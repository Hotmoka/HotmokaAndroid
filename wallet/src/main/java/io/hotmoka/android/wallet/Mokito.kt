package io.hotmoka.android.wallet

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.hotmoka.android.remote.AndroidRemoteNode
import io.hotmoka.remote.RemoteNodeConfig
import kotlinx.android.synthetic.main.activity_main.*

class Mokito : AppCompatActivity() {
    private var node: AndroidRemoteNode? = null

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

    private fun notifyException(t: Throwable) {
        Toast.makeText(this, t.toString(), Toast.LENGTH_LONG).show()
    }
}