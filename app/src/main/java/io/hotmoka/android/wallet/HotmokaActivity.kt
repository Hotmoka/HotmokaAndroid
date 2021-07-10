package io.hotmoka.android.wallet

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.hotmoka.android.remote.NodeService
import io.hotmoka.remote.RemoteNodeConfig
import kotlinx.android.synthetic.main.activity_main.*

class HotmokaActivity : AppCompatActivity() {
    var nodeService: NodeService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val config = RemoteNodeConfig.Builder().setURL("panarea.hotmoka.io").build()
        NodeService.bind(this, config, { nodeService = it }, { nodeService = null })
    }

    fun getTakamakaCode(view: View) {
        nodeService?.getTakamakaCode({ textView.text = it.toString() }, ::notifyException);
    }

    private fun notifyException(t: Throwable) {
        Toast.makeText(this, t.toString(), Toast.LENGTH_LONG).show()
    }
}