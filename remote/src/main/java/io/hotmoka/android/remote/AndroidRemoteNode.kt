package io.hotmoka.android.remote

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import android.util.Log
import io.hotmoka.beans.InternalFailureException
import io.hotmoka.beans.references.TransactionReference
import io.hotmoka.beans.values.StorageReference
import io.hotmoka.beans.updates.ClassTag
import io.hotmoka.beans.updates.Update
import io.hotmoka.nodes.Node
import io.hotmoka.remote.RemoteNode
import io.hotmoka.remote.RemoteNodeConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.util.stream.Stream

class AndroidRemoteNode : Service() {
    private var node: Node? = null
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val mainScope = CoroutineScope(Dispatchers.Main)

    companion object {
        private const val TAG = "AndroidRemoteNode"

        fun of(
            context: Context,
            config: RemoteNodeConfig,
            onConnected: (AndroidRemoteNode) -> Unit,
            onDisconnected: () -> Unit = {}
        ) {
            val intent = Intent(context, AndroidRemoteNode::class.java)
            intent.putExtra("url", config.url)
            intent.putExtra("webSockets", config.webSockets)

            val myConnection = object : ServiceConnection {
                override fun onServiceConnected(className: ComponentName, service: IBinder) {
                    val binder = service as AndroidRemoteNode.MyLocalBinder
                    onConnected(binder.getService())
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    onDisconnected()
                }
            }

            context.bindService(intent, myConnection, BIND_AUTO_CREATE)
        }
    }

    override fun onBind(intent: Intent): IBinder {
        val url = intent.getStringExtra("url")
            ?: throw IllegalArgumentException("missing node url to bind to service")
        val websockets = intent.getBooleanExtra("webSockets", false)
        val config = RemoteNodeConfig.Builder().setURL(url).setWebSockets(websockets).build()

        ioScope.launch(Dispatchers.IO) {
            node = RemoteNode.of(config)
            Log.d(TAG, "created remote node of type " + node!!::class.simpleName)
        }

        return MyLocalBinder()
    }

    private inner class MyLocalBinder : Binder() {
        fun getService() : AndroidRemoteNode {
            return this@AndroidRemoteNode
        }
    }

    fun getTakamakaCode(): TransactionReference {
        return callSafely(Node::getTakamakaCode, "getTakamakaCode")
    }

    fun getTakamakaCode(onSuccess: (TransactionReference) -> Unit, onException: (Throwable) -> Unit) {
        ioScope.launch(Dispatchers.IO) {
            val result: TransactionReference

            try {
                result = getTakamakaCode()
            }
            catch (e: Throwable) {
                Log.d(TAG, "getTakamakaCode => failure: $e")
                mainScope.launch(Dispatchers.Main) { onException(e) }
                return@launch
            }

            mainScope.launch(Dispatchers.Main) { onSuccess(result) }
        }
    }

    fun getManifest(): StorageReference {
        return callSafely(Node::getManifest, "getManifest")
    }

    fun getManifest(onSuccess: (StorageReference) -> Unit, onException: (Throwable) -> Unit) {
        ioScope.launch(Dispatchers.IO) {
            val result: StorageReference

            try {
                result = getManifest()
            }
            catch (e: Throwable) {
                Log.d(TAG, "getManifest => failure: $e")
                mainScope.launch(Dispatchers.Main) { onException(e) }
                return@launch
            }

            mainScope.launch(Dispatchers.Main) { onSuccess(result) }
        }
    }

    fun getNameOfSignatureAlgorithmForRequests(): String {
        return callSafely(Node::getNameOfSignatureAlgorithmForRequests, "getNameOfSignatureAlgorithmForRequests")
    }

    fun getNameOfSignatureAlgorithmForRequests(onSuccess: (String) -> Unit, onException: (Throwable) -> Unit) {
        ioScope.launch(Dispatchers.IO) {
            val result: String

            try {
                result = getNameOfSignatureAlgorithmForRequests()
            }
            catch (e: Throwable) {
                Log.d(TAG, "getNameOfSignatureAlgorithmForRequests => failure: $e")
                mainScope.launch(Dispatchers.Main) { onException(e) }
                return@launch
            }

            mainScope.launch(Dispatchers.Main) { onSuccess(result) }
        }
    }

    fun getClassTag(reference: StorageReference): ClassTag {
        return callSafely({ node -> node.getClassTag(reference) }, "getClassTag")
    }

    fun getState(reference: StorageReference): Stream<Update> {
        return callSafely({ node -> node.getState(reference) }, "getState")
    }

    private fun <T> callSafely(task: (Node) -> T, name: String): T {
        node?.let {
            val result = task(it)
            if (result == null)
                throw InternalFailureException("unexpected null result")
            else {
                Log.d(TAG, "$name => success")
                return result
            }
        }

        throw IllegalStateException("remote node not connected")
    }
}