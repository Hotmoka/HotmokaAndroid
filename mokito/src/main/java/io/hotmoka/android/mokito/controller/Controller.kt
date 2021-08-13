package io.hotmoka.android.mokito.controller

import android.util.Base64
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
import io.hotmoka.beans.values.StringValue
import io.hotmoka.crypto.BIP39Dictionary
import io.hotmoka.crypto.BIP39Words
import io.hotmoka.crypto.Base58
import io.hotmoka.crypto.SignatureAlgorithmForTransactionRequests
import io.hotmoka.remote.RemoteNodeConfig
import io.hotmoka.views.AccountCreationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException
import java.math.BigInteger
import java.security.KeyPair
import java.security.PublicKey
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

        try {
            mvc.openFileInput("accounts.txt").bufferedReader().useLines { lines ->
                val all = lines.fold("") { some, text ->
                    "$some\n$text"
                }
                Log.d("Controller", all)
            }
        }
        catch (e: Exception) {
            Log.d("Controller", "no accounts.txt", e)
        }
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

    fun requestAccounts() {
        safeRunAsIO {
            ensureConnected()
            mvc.model.setAccounts(reloadAccounts())
        }
    }

    fun requestDelete(account: Account, password: String) {
        safeRunAsIO {
            checkPassword(account, password)
            ensureConnected()
            val accounts = mvc.model.getAccounts() ?: reloadAccounts()
            accounts.delete(account)
            accounts.writeIntoInternalStorage(mvc)
            mainScope.launch { mvc.view?.onAccountDeleted(account) }
            mvc.model.setAccounts(accounts)
        }
    }

    fun requestReplace(old: Account, new: Account, passwordOld: String) {
        safeRunAsIO {
            checkPassword(old, passwordOld)
            ensureConnected()
            var newAccount = new
            new.reference?.let { newReference ->
                val balance = getBalance(newReference)
                newAccount = Account(newReference, new.name, new.getEntropy(), new.publicKey, balance, true)
                checkThatRemotePublicKeyMatches(newAccount)
            }
            val accounts = mvc.model.getAccounts() ?: reloadAccounts()
            accounts.delete(old)
            accounts.add(newAccount)
            accounts.writeIntoInternalStorage(mvc)
            mainScope.launch { mvc.view?.onAccountReplaced(old, newAccount) }
            mvc.model.setAccounts(accounts)
        }
    }

    fun requestNewAccountFromFaucet(name: String, passwordOfNewAccount: String, balance: BigInteger) {
        createNewAccount({ publicKey ->
            AccountCreationHelper(node)
                .fromFaucet(
                    signatureAlgorithmOfNewAccounts,
                    publicKey,
                    balance,
                    BigInteger.ZERO
                ) { }
        }, name, passwordOfNewAccount, balance)
    }

    fun requestNewAccountFromAnotherAccount(payer: Account, passwordOfPayer: String, name: String, passwordOfNewAccount: String, balance: BigInteger) {
        createNewAccount({ publicKey ->

            checkPassword(payer, passwordOfPayer)

            val keysOfPayer = signatureAlgorithmOfNewAccounts.getKeyPair(
                payer.getEntropy(),
                BIP39Dictionary.ENGLISH_DICTIONARY,
                passwordOfPayer
            )

            AccountCreationHelper(node)
                .fromPayer(
                    payer.reference,
                    keysOfPayer,
                    signatureAlgorithmOfNewAccounts,
                    publicKey,
                    balance,
                    BigInteger.ZERO,
                    false,
                    {},
                    {}
                )
        }, name, passwordOfNewAccount, balance)
    }

    fun requestBip39Words(account: Account) {
        safeRunAsIO {
            val acc = io.hotmoka.crypto.Account(account.getEntropy(), account.reference)
            val bip39 = BIP39Words.of(acc, BIP39Dictionary.ENGLISH_DICTIONARY)

            mainScope.launch { mvc.view?.onBip39Available(account, bip39) }
        }
    }

    fun requestImportAccountFromBip39Words(name: String, mnemonic: Array<String>, password: String) {
        safeRunAsIO {
            val acc = BIP39Words.of(mnemonic, BIP39Dictionary.ENGLISH_DICTIONARY).toAccount()
            ensureConnected()

            val balance = getBalance(acc.reference)
            val keys = signatureAlgorithmOfNewAccounts.getKeyPair(acc.entropy, BIP39Dictionary.ENGLISH_DICTIONARY, password)
            val importedAccount = Account(acc.reference, name, acc.entropy, publicKeyBase64Encoded(keys), balance, true)
            checkThatRemotePublicKeyMatches(importedAccount)

            val accounts = mvc.model.getAccounts() ?: reloadAccounts()
            accounts.add(importedAccount)
            accounts.writeIntoInternalStorage(mvc)
            mainScope.launch { mvc.view?.onAccountImported(importedAccount) }
            mvc.model.setAccounts(accounts)
        }
    }

    fun requestNewKeyPair(password: String) {
        safeRunAsIO {
            val entropy = ByteArray(16) // 128 bits of entropy
            random.nextBytes(entropy)
            val keys = signatureAlgorithmOfNewAccounts.getKeyPair(entropy, BIP39Dictionary.ENGLISH_DICTIONARY, password)
            val publicKeyBase58 = publicKeyBase58Encoded(keys)
            val publicKeyBase64 = publicKeyBase64Encoded(keys)
            Log.d("Controller", "created public key $publicKeyBase58")

            // it is not a fully functional account yet, since it misses the reference
            val newAccount = Account(null, publicKeyBase58, entropy, publicKeyBase64, BigInteger.ZERO, false)
            val accounts = mvc.model.getAccounts() ?: reloadAccounts()
            accounts.add(newAccount)
            accounts.writeIntoInternalStorage(mvc)
            mvc.model.setAccounts(accounts)
        }
    }

    private fun publicKeyBase64Encoded(keys: KeyPair): String {
        return Base64.encodeToString(signatureAlgorithmOfNewAccounts.encodingOf(keys.public), Base64.NO_WRAP)
    }

    private fun publicKeyBase58Encoded(keys: KeyPair): String {
        return Base58.encode(signatureAlgorithmOfNewAccounts.encodingOf(keys.public))
    }

    /**
     * Checks that the reference of the given account is actually an account object in the node
     * with the same public key as the account.
     *
     * @param account the account to check
     * @throws IllegalArgumentException if the two public keys do not match
     */
    private fun checkThatRemotePublicKeyMatches(account: Account) {
        account.reference?.let {
            if (getPublicKey(it) != account.publicKey)
                throw IllegalArgumentException("The public key of the account does not match its entropy")
        }
    }

    /**
     * Checks that the given string is actually the password of the given account.
     *
     * @param account the account whose password is being checked
     * @param password the proposed password of {@code account}
     * @throws IllegalArgumentException if the password is incorrect
     */
    private fun checkPassword(account: Account, password: String) {
        val keys = signatureAlgorithmOfNewAccounts.getKeyPair(account.getEntropy(), BIP39Dictionary.ENGLISH_DICTIONARY, password)
        if (publicKeyBase64Encoded(keys) != account.publicKey)
            throw IllegalArgumentException("Incorrect password!")
    }

    private fun reloadAccounts(): Accounts {
        return Accounts(mvc, getFaucet(), getMaxFaucet(), this::getBalance)
    }

    private fun createNewAccount(creator: (PublicKey) -> StorageReference, name: String, password: String, balance: BigInteger) {
        safeRunAsIO {
            ensureConnected()
            val entropy = ByteArray(16) // 128 bits of entropy
            random.nextBytes(entropy)
            val keys = signatureAlgorithmOfNewAccounts.getKeyPair(entropy, BIP39Dictionary.ENGLISH_DICTIONARY, password)

            // create an account with the public key
            val reference = creator(keys.public)
            Log.d("Controller", "created new account $reference")

            // currently, the progressive number of the created accounts will be #0,
            // but we better check against future unexpected changes in the server's behavior
            if (reference.progressive.signum() != 0)
                throw IllegalStateException("I can only deal with new accounts whose progressive number is 0")

            // we force a reload of the accounts, so that their balances reflect the changes;
            // this is important, in particular, to update the balance of the payer
            val accounts = reloadAccounts()
            val newAccount = Account(reference, name, entropy, publicKeyBase64Encoded(keys), balance, true)
            accounts.add(newAccount)
            accounts.writeIntoInternalStorage(mvc)
            mainScope.launch { mvc.view?.onAccountCreated(newAccount) }
            mvc.model.setAccounts(accounts)
        }
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

    private fun getPublicKey(reference: StorageReference): String {
        return (node.runInstanceMethodCallTransaction(
            InstanceMethodCallTransactionRequest(
                reference, BigInteger.valueOf(100_000L), takamakaCode, MethodSignature.PUBLIC_KEY, reference
            )
        ) as StringValue).value
    }

    private fun safeRunAsIO(task: () -> Unit) {
        ioScope.launch {
            try {
                task.invoke()
            }
            catch (t: Throwable) {
                // if something goes wrong, we inform the user
                mainScope.launch {
                    mvc.view?.notifyException(t)
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