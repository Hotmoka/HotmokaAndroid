package io.hotmoka.android.mokito.controller

import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager
import io.hotmoka.android.mokito.MVC
import io.hotmoka.android.mokito.R
import io.hotmoka.android.remote.AndroidRemoteNode
import io.hotmoka.beans.updates.Update
import io.hotmoka.beans.values.StorageReference
import io.hotmoka.crypto.SignatureAlgorithmForTransactionRequests
import io.hotmoka.remote.RemoteNodeConfig
import io.hotmoka.views.AccountCreationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import java.math.BigInteger

class Controller(private val mvc: MVC) {
    private var node: AndroidRemoteNode = AndroidRemoteNode()
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val mainScope = CoroutineScope(Dispatchers.Main)

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

    fun requestNewAccountFromFaucet() {
        safeRunAsIO {
            Log.d("Controller", "requested new account from faucet")
            ensureConnected()
            val signatureAlgorithmOfNewAccount = SignatureAlgorithmForTransactionRequests.mk("ed25519")
            val keys = signatureAlgorithmOfNewAccount.keyPair
            val balance = BigInteger.valueOf(123456789L)
            var account = AccountCreationHelper(node)
                .fromFaucet(signatureAlgorithmOfNewAccount,
                    keys.public, balance, BigInteger.ZERO) { _ -> {} }
            Log.d("Controller", "created new account $account")
            account = AccountCreationHelper(node).fromPayer(account, keys, signatureAlgorithmOfNewAccount, keys.public, BigInteger.ZERO, BigInteger.ZERO, false, { _ -> {} }, { _ -> {}})
            val publicKey = Base64.encodeToString(keys.public.encoded, Base64.DEFAULT)
            Log.d("Controller", publicKey)
            Log.d("Controller", "created new account $account")
            Log.d("Controller", "the account has length ${account.transaction.hashAsBytes.size}")
            mvc.model.addAccount(account)
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