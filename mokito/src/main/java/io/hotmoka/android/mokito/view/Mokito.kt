package io.hotmoka.android.mokito.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.zxing.integration.android.IntentIntegrator
import io.hotmoka.android.mokito.BuildConfig
import io.hotmoka.android.mokito.MVC
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.databinding.MokitoBinding

class Mokito : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private var pendingScanCancelled: Boolean = false
    private var pendingScan: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = MokitoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMokito.toolbar)

        // passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_accounts, R.id.insert_reference,
                R.id.show_manifest, R.id.settings
            ), binding.drawerLayout
        )

        val navController = findNavController(R.id.nav_host_fragment_content_mokito)
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)
    }

    override fun getApplicationContext(): MVC {
        return super.getApplicationContext() as MVC
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_mokito)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            result.contents?.let { pendingScan = it }
            if (result.contents == null)
                pendingScanCancelled = true
        } else
            super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onStart() {
        super.onStart()
        Log.d("Mokito", "view = ${applicationContext.view}")

        // we can notify the view only when the activity has been fully waken up and its view
        // reconstructed, hence inside onStart()

        if (pendingScanCancelled) {
            pendingScanCancelled = false
            applicationContext.view?.onQRScanCancelled()
        }

        pendingScan?.let {
            pendingScan = null
            applicationContext.view?.onQRScanAvailable(it)
        }
    }
}