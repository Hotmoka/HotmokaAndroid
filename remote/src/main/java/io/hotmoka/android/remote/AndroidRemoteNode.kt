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
import io.hotmoka.beans.requests.*
import io.hotmoka.beans.responses.TransactionResponse
import io.hotmoka.beans.updates.ClassTag
import io.hotmoka.beans.updates.Update
import io.hotmoka.beans.values.StorageReference
import io.hotmoka.beans.values.StorageValue
import io.hotmoka.nodes.Node
import io.hotmoka.nodes.Node.CodeSupplier
import io.hotmoka.nodes.Node.Subscription
import io.hotmoka.nodes.Node.JarSupplier
import io.hotmoka.remote.RemoteNode
import io.hotmoka.remote.RemoteNodeConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.function.BiConsumer
import java.util.stream.Stream

class AndroidRemoteNode : Service(), Node {
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

    override fun close() {
        node?.close()
        stopSelf()
    }

    override fun getTakamakaCode(): TransactionReference {
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

    override fun getManifest(): StorageReference {
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

    override fun getNameOfSignatureAlgorithmForRequests(): String {
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

    override fun getClassTag(reference: StorageReference): ClassTag {
        return callSafely({ node -> node.getClassTag(reference) }, "getClassTag")
    }

    override fun getState(reference: StorageReference): Stream<Update> {
        return callSafely({ node -> node.getState(reference) }, "getState")
    }

    override fun getRequest(reference: TransactionReference): TransactionRequest<*> {
        return callSafely({ node -> node.getRequest(reference) }, "getRequest")
    }

    override fun getResponse(reference: TransactionReference): TransactionResponse {
        return callSafely({ node -> node.getResponse(reference) }, "getResponse")
    }

    override fun getPolledResponse(reference: TransactionReference): TransactionResponse {
        return callSafely({ node -> node.getPolledResponse(reference) }, "getPolledResponse")
    }

    override fun addJarStoreInitialTransaction(request: JarStoreInitialTransactionRequest): TransactionReference {
        return callSafely({ node -> node.addJarStoreInitialTransaction(request) }, "addJarStoreInitialTransaction")
    }

    override fun addGameteCreationTransaction(request: GameteCreationTransactionRequest): StorageReference {
        return callSafely({ node -> node.addGameteCreationTransaction(request) }, "addGameteCreationTransaction")
    }

    override fun addInitializationTransaction(request: InitializationTransactionRequest) {
        callSafely({ node -> node.addInitializationTransaction(request) }, "addInitializationTransaction")
    }

    override fun addJarStoreTransaction(request: JarStoreTransactionRequest): TransactionReference {
        return callSafely({ node -> node.addJarStoreTransaction(request) }, "addJarStoreTransaction")
    }

    override fun addConstructorCallTransaction(request: ConstructorCallTransactionRequest): StorageReference {
        return callSafely({ node -> node.addConstructorCallTransaction(request) }, "addConstructorCallTransaction")
    }

    override fun addInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequest): StorageValue {
        return callSafely({ node -> node.addInstanceMethodCallTransaction(request) }, "addInstanceMethodCallTransaction")
    }

    override fun addStaticMethodCallTransaction(request: StaticMethodCallTransactionRequest): StorageValue {
        return callSafely({ node -> node.addStaticMethodCallTransaction(request) }, "addStaticMethodCallTransaction")
    }

    override fun runInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequest): StorageValue {
        return callSafely({ node -> node.runInstanceMethodCallTransaction(request) }, "runInstanceMethodCallTransaction")
    }

    override fun runStaticMethodCallTransaction(request: StaticMethodCallTransactionRequest): StorageValue {
        return callSafely({ node -> node.runStaticMethodCallTransaction(request) }, "runStaticMethodCallTransaction")
    }

    override fun postJarStoreTransaction(request: JarStoreTransactionRequest): JarSupplier {
        return callSafely({ node -> node.postJarStoreTransaction(request) }, "postJarStoreTransaction")
    }

    override fun postConstructorCallTransaction(request: ConstructorCallTransactionRequest): CodeSupplier<StorageReference> {
        return callSafely({ node -> node.postConstructorCallTransaction(request) }, "postConstructorCallTransaction")
    }

    override fun postInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequest): CodeSupplier<StorageValue> {
        return callSafely({ node -> node.postInstanceMethodCallTransaction(request) }, "postInstanceMethodCallTransaction")
    }

    override fun postStaticMethodCallTransaction(request: StaticMethodCallTransactionRequest): CodeSupplier<StorageValue> {
        return callSafely({ node -> node.postStaticMethodCallTransaction(request) }, "postStaticMethodCallTransaction")
    }

    override fun subscribeToEvents(creator: StorageReference?, handler: BiConsumer<StorageReference, StorageReference>): Subscription {
        return callSafely({ node -> node.subscribeToEvents(creator, handler) }, "subscribeToEvents")
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