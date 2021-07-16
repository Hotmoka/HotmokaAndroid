package io.hotmoka.android.mokito.controller

import android.widget.Toast
import io.hotmoka.android.mokito.MVC
import io.hotmoka.android.mokito.Settings
import io.hotmoka.android.remote.AndroidRemoteNode
import io.hotmoka.beans.references.TransactionReference
import io.hotmoka.beans.responses.TransactionResponse
import io.hotmoka.beans.values.StorageReference
import io.hotmoka.remote.RemoteNodeConfig

class Controller(private val mvc: MVC) {
    private val node: AndroidRemoteNode = AndroidRemoteNode()

    fun ensureConnected() {
        if (!node.isConnected()) {
            val config = RemoteNodeConfig.Builder()
                .setURL(Settings.url)
                .setWebSockets(Settings.webSockets)
                .build()

            node.connect(config,
                {
                    Toast.makeText(mvc.view, "Connected to ${config.url}", Toast.LENGTH_SHORT).show()
                },
                {
                    Toast.makeText(mvc.view, it.toString(), Toast.LENGTH_LONG).show()
                }
            )
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