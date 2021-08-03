package io.hotmoka.android.mokito.controller

import android.util.Log
import androidx.preference.PreferenceManager
import io.hotmoka.android.mokito.MVC
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.model.Accounts
import io.hotmoka.android.remote.AndroidRemoteNode
import io.hotmoka.beans.references.TransactionReference
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest
import io.hotmoka.beans.signatures.MethodSignature
import io.hotmoka.beans.updates.Update
import io.hotmoka.beans.values.BigIntegerValue
import io.hotmoka.beans.values.BooleanValue
import io.hotmoka.beans.values.StorageReference
import io.hotmoka.crypto.BIP39Dictionary
import io.hotmoka.crypto.BIP39Words
import io.hotmoka.crypto.SignatureAlgorithmForTransactionRequests
import io.hotmoka.remote.RemoteNodeConfig
import io.hotmoka.views.AccountCreationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.security.SecureRandom

class Controller(private val mvc: MVC) {
    private var node: AndroidRemoteNode = AndroidRemoteNode()
    private var takamakaCode: TransactionReference? = null
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val signatureAlgorithmOfNewAccounts = SignatureAlgorithmForTransactionRequests.ed25519()
    private val random = SecureRandom()

    private fun ensureConnected() {
        if (!node.isConnected())
            connect()
    }

    private fun connect() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mvc)
        var url = sharedPreferences.getString("url", "panarea.hotmoka.io")
        if (url!!.startsWith("http://"))
            url = url.substring("http://".length)

        val config = RemoteNodeConfig.Builder()
            .setURL(url)
            .setWebSockets(sharedPreferences.getBoolean("webSockets", false))
            .build()

        node.connect(config)
        takamakaCode = node.takamakaCode

        mainScope.launch {
            mvc.view?.notifyUser(mvc.getString(R.string.connected, config.url))
        }
    }

    fun requestReconnect() {
        safeRunAsIO {
            // we clear the model, since the new node might contain different data
            mvc.model.clear()

            node.disconnect()
            connect()
        }
    }

    fun requestStateOf(reference: StorageReference) {
        safeRunAsIO {
            ensureConnected()
            val state = node.getState(reference)
            mvc.model.setState(reference, state.toArray { arrayOfNulls<Update>(it) })
        }
    }

    fun requestStateOfManifest() {
        safeRunAsIO {
            ensureConnected()
            requestStateOf(getManifestCached())
        }
    }

    /**
     * Creates a random array of the given amount of bytes.
     */
    private fun entropy(bytes: Int): ByteArray {
        val entropy = ByteArray(bytes)
        random.nextBytes(entropy)
        return entropy
    }

    fun requestAccounts() {
        safeRunAsIO {
            ensureConnected()
            mvc.model.setAccounts(reloadAccounts())
        }
    }

    private fun reloadAccounts(): Accounts {
        return Accounts(mvc, getFaucet(), getMaxFaucet(), this::getBalance)
    }

    fun requestDelete(account: Account) {
        safeRunAsIO {
            ensureConnected()
            val accounts = mvc.model.getAccounts() ?: reloadAccounts()
            accounts.delete(account)
            accounts.writeIntoInternalStorage(mvc)
            mvc.model.setAccounts(accounts)
        }
    }

    fun requestReplace(old: Account, new: Account) {
        safeRunAsIO {
            ensureConnected()
            val accounts = mvc.model.getAccounts() ?: reloadAccounts()
            accounts.delete(old)
            accounts.add(new)
            accounts.writeIntoInternalStorage(mvc)
            mvc.model.setAccounts(accounts)
        }
    }

    fun requestNewAccountFromFaucet(name: String, password: String, balance: BigInteger) {
        safeRunAsIO {
            ensureConnected()

            // generate 128 bits of entropy
            val entropy = entropy(16)

            // compute the key pair for that entropy and the given password
            val keys = signatureAlgorithmOfNewAccounts.getKeyPair(entropy, BIP39Dictionary.ENGLISH_DICTIONARY, password)

            // create an account with the public key, let the faucet pay for that
            val reference = AccountCreationHelper(node)
                .fromFaucet(
                    signatureAlgorithmOfNewAccounts,
                    keys.public,
                    balance,
                    BigInteger.ZERO
                ) { }

            // currently, the progressive number of the created account will be #0,
            // but we better check against future unexpected changes in the server's behavior
            if (reference.progressive.signum() != 0)
                throw IllegalStateException("I can only deal with new accounts whose progressive number is 0")

            Log.d("Controller", "created new account $reference")

            // we force a reload of the accounts, so that their balances reflect the changes;
            // this is important, in particular, to update the balance of the payer
            val accounts = reloadAccounts()
            val newAccount = Account(reference, name, entropy, balance)
            accounts.add(newAccount)
            accounts.writeIntoInternalStorage(mvc)

            /*mvc.openFileInput("accounts.txt").bufferedReader().useLines { lines ->
                val all = lines.fold("") { some, text ->
                    "$some\n$text"
                }
                Log.d("Controller", all)
            }*/

            mainScope.launch { mvc.view?.onAccountCreated(newAccount) }
            mvc.model.setAccounts(accounts)
        }
    }

    fun requestBip39Words(account: Account) {
        safeRunAsIO {
            val acc = io.hotmoka.crypto.Account(account.getEntropy(), account.reference)
            val bip39 = BIP39Words.of(acc, BIP39Dictionary.ENGLISH_DICTIONARY)

            mainScope.launch {
                mvc.view?.onBip39Available(account, bip39)
            }
        }
    }

    fun requestImportAccountFromBip39Words(name: String, mnemonic: Array<String>) {
        safeRunAsIO {
            val acc = BIP39Words.of(mnemonic, BIP39Dictionary.ENGLISH_DICTIONARY).toAccount()
            ensureConnected()
            val importedAccount = Account(acc.reference, name, acc.entropy, getBalance(acc.reference))
            val accounts = mvc.model.getAccounts() ?: reloadAccounts()
            accounts.add(importedAccount)
            accounts.writeIntoInternalStorage(mvc)
            mainScope.launch {
                mvc.view?.onAccountImported(importedAccount)
            }
            mvc.model.setAccounts(accounts)
        }
    }

    /**
     * Yields the account reconstructed from the given mnemonic words.
     *
     * @param name the name of the account
     * @param mnemonic the 36 mnemonic words
     * @param getBalance a function that retrieves the balance of accounts
     * @return the reconstructed account
     */
    private fun getAccount(name: String, mnemonic: Array<String>, getBalance: (StorageReference) -> BigInteger): Account {
        val acc = BIP39Words.of(mnemonic, BIP39Dictionary.ENGLISH_DICTIONARY).toAccount()
        return Account(acc.reference, name, acc.entropy, getBalance(acc.reference))
    }

    /**
     * Yields the faucet of the Hotmoka node, if the node allows it.
     *
     * @return the faucet, if any
     */
    private fun getFaucet(): StorageReference? {
        val manifest = getManifestCached()
        val hasFaucet = (node.runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest(
            manifest, BigInteger.valueOf(100_000L), takamakaCode, MethodSignature.ALLOWS_UNSIGNED_FAUCET, manifest
        )) as BooleanValue).value

        return if (hasFaucet)
            getGameteCached()
        else
            null
    }

    private fun getMaxFaucet(): BigInteger {
        val manifest = getManifestCached()
        return (node.runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest(
            manifest, BigInteger.valueOf(100_000L), takamakaCode, MethodSignature.GET_MAX_FAUCET, getGameteCached()
        )) as BigIntegerValue).value
    }

    private fun getBalance(reference: StorageReference): BigInteger {
        return (node.runInstanceMethodCallTransaction(
            InstanceMethodCallTransactionRequest(
                reference, BigInteger.valueOf(100_000L), takamakaCode, MethodSignature.BALANCE, reference
            )
        ) as BigIntegerValue).value
    }

    private fun safeRunAsIO(task: () -> Unit) {
        ioScope.launch {
            try {
                task.invoke()
            }
            catch (t: Throwable) {
                // if something goes wrong, we inform the user
                mainScope.launch {
                    mvc.view?.notifyUser(t.toString())
                }
            }
        }
    }

    private fun getManifestCached(): StorageReference {
        mvc.model.getManifest()?.let {
            return it
        }

        val manifest = node.manifest
        mvc.model.setManifest(manifest)
        return manifest
    }

    private fun getGameteCached(): StorageReference {
        mvc.model.getGamete()?.let {
            return it
        }

        val manifest = getManifestCached()
        val gamete = node.runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest(
            manifest, BigInteger.valueOf(100_000L), takamakaCode, MethodSignature.GET_GAMETE, manifest
        )) as StorageReference

        mvc.model.setGamete(gamete)
        return gamete
    }
}