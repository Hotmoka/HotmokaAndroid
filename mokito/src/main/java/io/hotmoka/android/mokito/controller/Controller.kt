package io.hotmoka.android.mokito.controller

import android.widget.Toast
import io.hotmoka.android.mokito.MVC
import io.hotmoka.android.mokito.Settings
import io.hotmoka.android.remote.AndroidRemoteNode
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
                { t: Throwable ->
                    mvc.view?.getContext().let {
                        Toast.makeText(it, t.toString(), Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }

    fun requestStateOf(reference: StorageReference) {
        safeRunAsIO {
            val state = node.getState(reference)
            mvc.model.setState(reference, state.toArray { arrayOfNulls<Update>(it) })
        }
    }

    fun requestStateOfManifest() {
        safeRunAsIO {
            requestStateOf(getManifestCached())
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