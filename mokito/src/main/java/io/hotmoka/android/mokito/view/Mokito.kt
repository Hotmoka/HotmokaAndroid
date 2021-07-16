package io.hotmoka.android.mokito.view

import android.os.Bundle
import android.view.Menu
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.appcompat.app.AppCompatActivity
import io.hotmoka.android.mokito.MVC
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.databinding.MokitoBinding

class Mokito : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = MokitoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMokito.toolbar)

        // passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow, R.id.insert_reference
            ), binding.drawerLayout
        )

        val navController = findNavController(R.id.nav_host_fragment_content_mokito)
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.mokito, menu)
        return true
    }

    override fun getApplicationContext(): MVC {
        return super.getApplicationContext() as MVC
    }

    override fun onStart() {
        super.onStart()
        applicationContext.view = this
    }

    override fun onStop() {
        applicationContext.view = null
        super.onStop()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_mokito)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}