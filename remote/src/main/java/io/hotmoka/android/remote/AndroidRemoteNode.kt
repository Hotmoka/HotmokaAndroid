package io.hotmoka.android.remote

import android.util.Log
import io.hotmoka.closeables.OnCloseHandlersManagers
import io.hotmoka.closeables.api.OnCloseHandler
import io.hotmoka.node.api.nodes.NodeInfo
import io.hotmoka.node.api.transactions.TransactionReference
import io.hotmoka.node.api.requests.*
import io.hotmoka.node.api.responses.TransactionResponse
import io.hotmoka.node.api.updates.ClassTag
import io.hotmoka.node.api.updates.Update
import io.hotmoka.node.api.values.StorageReference
import io.hotmoka.node.api.values.StorageValue
import io.hotmoka.node.api.Node
import io.hotmoka.node.api.ConstructorFuture
import io.hotmoka.node.api.Subscription
import io.hotmoka.node.api.JarFuture
import io.hotmoka.node.api.MethodFuture
import io.hotmoka.node.api.nodes.ConsensusConfig
import io.hotmoka.node.remote.RemoteNodes
import io.hotmoka.node.remote.api.RemoteNode
import java.net.URI
import java.util.Objects
import java.util.Optional
import java.util.function.BiConsumer
import java.util.stream.Stream

class AndroidRemoteNode : Node {
    private var node: RemoteNode? = null
    private var uri: URI? = null
    private var manifest: StorageReference? = null
    private var takamakaCode: TransactionReference? = null
    private var manager = OnCloseHandlersManagers.create()

    companion object {
        private const val TAG = "AndroidRemoteNode"
    }

    override fun addOnCloseHandler(handler: OnCloseHandler?) {
        manager.addOnCloseHandler(handler)
    }

    override fun removeOnCloseHandler(handler: OnCloseHandler?) {
        manager.removeOnCloseHandler(handler)
    }

    fun connect(uri: URI, timeout: Int) {
        this.uri = uri

        with (RemoteNodes.of(uri, timeout)) {
            this@AndroidRemoteNode.node = this
            Log.i(TAG, "Connected to $uri")
        }
    }

    fun disconnect() {
        node?.let {
            close()
            Log.i(TAG, "Disconnected from $uri")
        }
    }

    fun isConnected() : Boolean {
        return node != null
    }

    override fun close() {
        manager.callCloseHandlers()
        val node = this.node
        this.node = null
        this.manifest = null
        this.takamakaCode = null
        node?.close()
    }

    override fun getTakamakaCode(): TransactionReference {
        var takamakaCode: TransactionReference? = this.takamakaCode
        if (takamakaCode == null) {
            takamakaCode = callSafely(Node::getTakamakaCode, "getTakamakaCode")
            this.takamakaCode = takamakaCode
        }

        return takamakaCode!!
    }

    override fun getManifest(): StorageReference {
        var manifest: StorageReference? = this.manifest
        if (manifest == null) {
            manifest = callSafely(Node::getManifest, "getManifest")
            this.manifest = manifest
        }

        return manifest!!
    }

    override fun getInfo(): NodeInfo {
        return callSafely(Node::getInfo, "getNodeInfo")
    }

    override fun getConfig(): ConsensusConfig<*, *> {
        return callSafely(Node::getConfig, "getConfig")
    }

    override fun getClassTag(reference: StorageReference): ClassTag {
        return callSafely({ node -> node.getClassTag(reference) }, "getClassTag")
    }

    override fun getState(reference: StorageReference): Stream<Update> {
        return callSafely({ node -> node.getState(reference) }, "getState")
    }

    override fun getIndex(reference: StorageReference): Stream<TransactionReference> {
        return callSafely({ node -> node.getIndex(reference) }, "getIndex")
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

    override fun addInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequest): Optional<StorageValue> {
        return callSafely({ node -> node.addInstanceMethodCallTransaction(request) }, "addInstanceMethodCallTransaction")
    }

    override fun addStaticMethodCallTransaction(request: StaticMethodCallTransactionRequest): Optional<StorageValue> {
        return callSafely({ node -> node.addStaticMethodCallTransaction(request) }, "addStaticMethodCallTransaction")
    }

    override fun runInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequest): Optional<StorageValue> {
        return callSafely({ node -> node.runInstanceMethodCallTransaction(request) }, "runInstanceMethodCallTransaction")
    }

    override fun runStaticMethodCallTransaction(request: StaticMethodCallTransactionRequest): Optional<StorageValue> {
        return callSafely({ node -> node.runStaticMethodCallTransaction(request) }, "runStaticMethodCallTransaction")
    }

    override fun postJarStoreTransaction(request: JarStoreTransactionRequest): JarFuture {
        return callSafely({ node -> node.postJarStoreTransaction(request) }, "postJarStoreTransaction")
    }

    override fun postConstructorCallTransaction(request: ConstructorCallTransactionRequest): ConstructorFuture {
        return callSafely({ node -> node.postConstructorCallTransaction(request) }, "postConstructorCallTransaction")
    }

    override fun postInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequest): MethodFuture {
        return callSafely({ node -> node.postInstanceMethodCallTransaction(request) }, "postInstanceMethodCallTransaction")
    }

    override fun postStaticMethodCallTransaction(request: StaticMethodCallTransactionRequest): MethodFuture {
        return callSafely({ node -> node.postStaticMethodCallTransaction(request) }, "postStaticMethodCallTransaction")
    }

    override fun subscribeToEvents(creator: StorageReference?, handler: BiConsumer<StorageReference, StorageReference>): Subscription {
        return callSafely({ node -> node.subscribeToEvents(creator, handler) }, "subscribeToEvents")
    }

    private fun <T> callSafely(task: (Node) -> T, name: String): T {
        node?.let {
            val result = Objects.requireNonNull(task(it))
            Log.d(TAG, "$name => success")
            return result
        }

        throw IllegalStateException("Remote node not connected")
    }
}