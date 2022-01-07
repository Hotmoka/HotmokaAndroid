package io.hotmoka.android.remote

import android.util.Log
import io.hotmoka.beans.InternalFailureException
import io.hotmoka.beans.nodes.NodeInfo
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
import java.util.function.BiConsumer
import java.util.stream.Stream

class AndroidRemoteNode : Node {
    private var node: Node? = null
    private var config: RemoteNodeConfig? = null
    private var manifest: StorageReference? = null
    private var takamakaCode: TransactionReference? = null

    companion object {
        private const val TAG = "AndroidRemoteNode"
    }

    fun connect(config: RemoteNodeConfig) {
        this.config = config
        with (RemoteNode.of(config)) {
            this@AndroidRemoteNode.node = this
            Log.d(TAG, "connected to ${config.url} through a ${this::class.simpleName}")
        }
    }

    fun disconnect() {
        node?.let {
            close()
            Log.d(TAG, "disconnected from ${config?.url}")
        }
    }

    fun isConnected() : Boolean {
        return node != null
    }

    override fun close() {
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

    override fun getNodeInfo(): NodeInfo {
        return callSafely(Node::getNodeInfo, "getNodeInfo")
    }

    override fun getNameOfSignatureAlgorithmForRequests(): String {
        return callSafely(Node::getNameOfSignatureAlgorithmForRequests, "getNameOfSignatureAlgorithmForRequests")
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

    override fun addInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequest): StorageValue? {
        return callSafelyAcceptsNull({ node -> node.addInstanceMethodCallTransaction(request) }, "addInstanceMethodCallTransaction")
    }

    override fun addStaticMethodCallTransaction(request: StaticMethodCallTransactionRequest): StorageValue? {
        return callSafelyAcceptsNull({ node -> node.addStaticMethodCallTransaction(request) }, "addStaticMethodCallTransaction")
    }

    override fun runInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequest): StorageValue? {
        return callSafelyAcceptsNull({ node -> node.runInstanceMethodCallTransaction(request) }, "runInstanceMethodCallTransaction")
    }

    override fun runStaticMethodCallTransaction(request: StaticMethodCallTransactionRequest): StorageValue? {
        return callSafelyAcceptsNull({ node -> node.runStaticMethodCallTransaction(request) }, "runStaticMethodCallTransaction")
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

    private fun <T> callSafelyAcceptsNull(task: (Node) -> T?, name: String): T? {
        node?.let {
            val result = task(it)
            Log.d(TAG, "$name => success")
            return result
        }

        throw IllegalStateException("remote node not connected")
    }
}