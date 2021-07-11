package io.hotmoka.android.mokito

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.hotmoka.android.remote.AndroidRemoteNode
import io.hotmoka.beans.updates.Update
import io.hotmoka.beans.values.StorageReference
import io.hotmoka.remote.RemoteNodeConfig
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.IllegalStateException
import java.util.stream.Collectors
import java.util.stream.Stream

class Mokito : AppCompatActivity() {
    private var node: AndroidRemoteNode? = null
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val mainScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val config = RemoteNodeConfig.Builder().setURL("panarea.hotmoka.io").build()
        AndroidRemoteNode.of(this, config, { node = it }, { node = null })
    }

    fun getTakamakaCode(view: View) {
        node?.getTakamakaCode({ textView.text = it.toString() }, ::notifyException);
    }

    fun getManifest(view: View) {
        node?.getManifest({ textView.text = it.toString() }, ::notifyException);
    }

    fun getNameOfSignatureAlgorithmForRequests(view: View) {
        node?.getNameOfSignatureAlgorithmForRequests({ textView.text = it }, ::notifyException);
    }

    fun complex(view: View) {
        node?.let { it ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ioScope.launch(Dispatchers.IO) {
                    try {
                        val state: Stream<Update>? = node?.getState(it.getManifest())
                        mainScope.launch {
                            textView.text = state
                                ?.map(Update::toString) // only API >= 24
                                ?.collect(Collectors.joining())
                        }
                    } catch (t: Throwable) {
                        mainScope.launch { notifyException(t) }
                    }
                }
            }
            else
                notifyException(IllegalStateException("this function is only available on Android API >= 24"))
        }
    }

    private fun notifyException(t: Throwable) {
        Toast.makeText(this, t.toString(), Toast.LENGTH_LONG).show()
    }
}