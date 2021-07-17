package io.hotmoka.android.mokito.controller

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import io.hotmoka.android.mokito.MVC
import io.hotmoka.android.mokito.Settings
import io.hotmoka.android.remote.AndroidRemoteNode
import io.hotmoka.beans.references.TransactionReference
import io.hotmoka.beans.responses.TransactionResponse
import io.hotmoka.beans.updates.Update
import io.hotmoka.beans.values.StorageReference
import io.hotmoka.remote.RemoteNodeConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Controller(private val mvc: MVC) {
    private val node: AndroidRemoteNode = AndroidRemoteNode()
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val mainScope = CoroutineScope(Dispatchers.Main)

    fun ensureConnected() {
        if (!node.isConnected()) {
            val config = RemoteNodeConfig.Builder()
                .setURL(Settings.url)
                .setWebSockets(Settings.webSockets)
                .build()

            node.connect(config,
                {
                    mvc.view?.getContext().let {
                        Toast.makeText(it, "Connected to ${config.url}", Toast.LENGTH_SHORT).show()
                    }
                },
                { t:Throwable ->
                    mvc.view?.getContext().let {
                        Toast.makeText(it, t.toString(), Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }

    fun loadManifest() {

        ioScope.launch {
            mvc.model.setManifest(node.manifest)
        }
    }

    fun requestStateOf(reference: StorageReference) {
        ioScope.launch {
            val state = node.getState(reference)
            mvc.model.setState(reference, state.toArray { arrayOfNulls<Update>(it) })
        }
    }

    fun requestStateOfManifest() {
        ioScope.launch {
            val manifest = node.manifest
            mvc.model.setManifest(manifest)
            val state = node.getState(manifest)
            mvc.model.setState(manifest, state.toArray { arrayOfNulls<Update>(it) })
        }
    }

    fun getTakamakaCode(): TransactionReference {
        return node.takamakaCode
    }

    fun getManifest(): StorageReference {
        return node.manifest
    }

    fun getNameOfSignatureAlgorithmForRequests(): String {
        return node.nameOfSignatureAlgorithmForRequests
    }

    fun getResponse(reference: TransactionReference): TransactionResponse {
        return node.getResponse(reference)
    }
}