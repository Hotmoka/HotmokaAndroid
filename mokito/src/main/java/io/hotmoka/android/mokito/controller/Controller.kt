package io.hotmoka.android.mokito.controller

import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager
import io.hotmoka.android.mokito.MVC
import io.hotmoka.android.mokito.R
import io.hotmoka.android.remote.AndroidRemoteNode
import io.hotmoka.beans.updates.Update
import io.hotmoka.beans.values.StorageReference
import io.hotmoka.crypto.SignatureAlgorithmForTransactionRequests
import io.hotmoka.crypto.internal.ED25519
import io.hotmoka.remote.RemoteNodeConfig
import io.hotmoka.views.AccountCreationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bouncycastle.util.encoders.Hex
import java.lang.IllegalStateException
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

class Controller(private val mvc: MVC) {
    private var node: AndroidRemoteNode = AndroidRemoteNode()
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
        val entropy = ByteArray(16)
        random.nextBytes(entropy)
        return entropy
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

            mvc.model.addAccount(account, entropy)

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
        }
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