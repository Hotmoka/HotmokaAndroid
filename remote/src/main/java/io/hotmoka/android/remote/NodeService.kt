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
import io.hotmoka.nodes.Node
import io.hotmoka.remote.RemoteNode
import io.hotmoka.remote.RemoteNodeConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException

class NodeService : Service() {
    private val TAG = "NodeService"
    private var node: Node? = null
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val mainScope = CoroutineScope(Dispatchers.Main)

    companion object {

        fun bind(
            context: Context,
            config: RemoteNodeConfig,
            onConnected: (NodeService) -> Unit,
            onDisconnected: () -> Unit = {}
        ) {
            val intent = Intent(context, NodeService::class.java)
            intent.putExtra("url", config.url)
            intent.putExtra("webSockets", config.webSockets)

            val myConnection = object : ServiceConnection {
                override fun onServiceConnected(className: ComponentName, service: IBinder) {
                    val binder = service as NodeService.MyLocalBinder
                    onConnected(binder.getService())
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    onDisconnected
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
            node = RemoteNode.of(config);
            Log.d(TAG, "created remote node of type " + node!!::class.simpleName)
        }

        return MyLocalBinder()
    }

    private inner class MyLocalBinder : Binder() {
        fun getService() : NodeService {
            return this@NodeService
        }
    }

    fun getTakamakaCode(onSuccess: (TransactionReference) -> Unit, onException: (Throwable) -> Unit) {
        ioScope.launch(Dispatchers.IO) {
            try {
                val result = node?.takamakaCode
                if (result == null)
                    throw InternalFailureException("unexpected null result")
                else {
                    Log.d(TAG, "getTakamakaCode => success")
                    mainScope.launch(Dispatchers.Main) {
                        onSuccess(result);
                    }
                }
            }
            catch (e: Throwable) {
                Log.d(TAG, "getTakamakaCode => failure: $e")
                mainScope.launch(Dispatchers.Main) {
                    onException(e)
                }
            }
        }
    }
}