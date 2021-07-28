package io.hotmoka.android.mokito.controller

import android.util.Log
import android.widget.Toast
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
import io.hotmoka.crypto.SignatureAlgorithmForTransactionRequests
import io.hotmoka.crypto.internal.ED25519
import io.hotmoka.remote.RemoteNodeConfig
import io.hotmoka.views.AccountCreationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.IllegalStateException
import java.math.BigInteger
import java.security.SecureRandom

class Controller(private val mvc: MVC) {
    private var node: AndroidRemoteNode = AndroidRemoteNode()
    private var takamakaCode: TransactionReference? = null
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val signatureAlgorithmOfNewAccounts = SignatureAlgorithmForTransactionRequests.ed25519() as ED25519
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
            mvc.view?.getContext()?.let {
                Toast.makeText(it, it.getString(R.string.connected, config.url), Toast.LENGTH_SHORT).show()
            }
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
            mvc.model.setAccounts(Accounts(mvc, getFaucet(), this::getBalance))
        }
    }

    fun requestDelete(account: Account) {
        safeRunAsIO {
            ensureConnected()
            val accounts = mvc.model.getAccounts() ?: Accounts(mvc, getFaucet(), this::getBalance)
            accounts.delete(account)
            accounts.writeIntoInternalStorage(mvc)
            mvc.model.setAccounts(accounts)
        }
    }

    fun requestReplace(old: Account, new: Account) {
        safeRunAsIO {
            ensureConnected()
            val accounts = mvc.model.getAccounts() ?: Accounts(mvc, getFaucet(), this::getBalance)
            accounts.delete(old)
            accounts.add(new)
            accounts.writeIntoInternalStorage(mvc)
            mvc.model.setAccounts(accounts)
        }
    }

    fun requestNewAccountFromFaucet(password: String, balance: BigInteger) {
        safeRunAsIO {
            ensureConnected()

            // generate 128 bits of entropy
            val entropy = entropy(16)

            // compute the key pair for that entropy and the given password
            val keys = signatureAlgorithmOfNewAccounts.getKeyPair(entropy, password)

            // create an account with the public key, let the faucet pay for that
            val account = AccountCreationHelper(node)
                .fromFaucet(
                    signatureAlgorithmOfNewAccounts,
                    keys.public,
                    balance,
                    BigInteger.ZERO
                ) { }

            // currently, the progressive number of the created account will be #0,
            // but we better check against future unexpected changes in the server's behavior
            if (account.progressive.signum() != 0)
                throw IllegalStateException("I can only deal with new accounts whose progressive number is 0")

            Log.d("Controller", "created new account $account")

            val accounts = mvc.model.getAccounts() ?: Accounts(mvc, getFaucet(), this::getBalance)
            accounts.add(Account(account, "NO NAME", entropy, balance))
            accounts.writeIntoInternalStorage(mvc)

            /*mvc.openFileInput("accounts.txt").bufferedReader().useLines { lines ->
                val all = lines.fold("") { some, text ->
                    "$some\n$text"
                }
                Log.d("Model", all)
            }*/

            mvc.model.setAccounts(accounts)

            /*
            // the progressive number of the created account is always #0,
            // hence we do not consider it
            val digest = MessageDigest.getInstance("SHA-256")
            var data = entropy + account.transaction.hashAsBytes
            val sha256 = digest.digest(data)

            // we add a checksum
            data = data + sha256[0] + sha256[1]
            var total = Hex.toHexString(data)

            // the last four bits of sha256[1] are not considered
            total = total.substring(0, total.length - 1)

            Log.d("Controller", "In total: $total")
            Log.d(
                "Controller",
                "Length to store is ${total.length * 4} bits ie ${total.length * 4 / 11} words"
            )

             */
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
            node.runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest(
                manifest, BigInteger.valueOf(100_000L), takamakaCode, MethodSignature.GET_GAMETE, manifest
            )) as StorageReference
        else
            null
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
                    mvc.view?.let {
                        Toast.makeText(it.getContext(), t.toString(), Toast.LENGTH_LONG).show()
                    }
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
}