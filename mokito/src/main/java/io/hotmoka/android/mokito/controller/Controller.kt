package io.hotmoka.android.mokito.controller

import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager
import io.hotmoka.android.mokito.MVC
import io.hotmoka.android.mokito.R
import io.hotmoka.android.remote.AndroidRemoteNode
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest
import io.hotmoka.beans.requests.SignedTransactionRequest
import io.hotmoka.beans.signatures.CodeSignature
import io.hotmoka.beans.signatures.NonVoidMethodSignature
import io.hotmoka.beans.types.ClassType
import io.hotmoka.beans.updates.Update
import io.hotmoka.beans.values.BigIntegerValue
import io.hotmoka.beans.values.StorageReference
import io.hotmoka.beans.values.StringValue
import io.hotmoka.crypto.SignatureAlgorithmForTransactionRequests
import io.hotmoka.remote.RemoteNodeConfig
import io.hotmoka.views.GasHelper
import io.hotmoka.views.NonceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
            val publicKey = Base64.encodeToString(keys.public.encoded, Base64.DEFAULT)
            Log.d("Controller", publicKey)
            val _100_000 = BigInteger.valueOf(100_000L)
            val balance = BigInteger.valueOf(123456L)
            val manifest = node.manifest
            val takamakaCode = node.takamakaCode
            val chainId = (node.runInstanceMethodCallTransaction(
                InstanceMethodCallTransactionRequest(manifest, _100_000, takamakaCode,
                    CodeSignature.GET_CHAIN_ID, manifest)) as StringValue).value
            val gamete = node.runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest(manifest, _100_000, takamakaCode, CodeSignature.GET_GAMETE, manifest)) as StorageReference
            val methodName = "faucetED25519"
            val eoaType = ClassType(ClassType.EOA.name + "ed25519".uppercase())
            val gas = _100_000

            // we use an empty signature algorithm and an arbitrary key, since the faucet is unsigned
            val signature = SignatureAlgorithmForTransactionRequests.empty()
            val signer = SignedTransactionRequest.Signer.with(signature, signature.keyPair)
            val nonceHelper = NonceHelper(node)
            val gasHelper = GasHelper(node)
            val request = InstanceMethodCallTransactionRequest(signer, gamete, nonceHelper.getNonceOf(gamete), chainId, gas, gasHelper.gasPrice, takamakaCode,
                NonVoidMethodSignature(ClassType.GAMETE, methodName, eoaType, ClassType.BIG_INTEGER, ClassType.BIG_INTEGER, ClassType.STRING),
                gamete,
                BigIntegerValue(balance), BigIntegerValue(BigInteger.ZERO), StringValue(publicKey)
            )

            val account = node.addInstanceMethodCallTransaction(request) as StorageReference
            Log.d("Controller", "created new account $account")
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