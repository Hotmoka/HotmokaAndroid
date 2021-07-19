package io.hotmoka.android.mokito.controller

import android.widget.Toast
import androidx.preference.PreferenceManager
import io.hotmoka.android.mokito.MVC
import io.hotmoka.android.mokito.R
import io.hotmoka.android.remote.AndroidRemoteNode
import io.hotmoka.beans.updates.Update
import io.hotmoka.beans.values.StorageReference
import io.hotmoka.remote.RemoteNodeConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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