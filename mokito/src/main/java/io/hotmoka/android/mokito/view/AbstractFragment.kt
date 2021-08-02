package io.hotmoka.android.mokito.view

import android.app.Activity
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import io.hotmoka.android.mokito.controller.Controller
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.model.Accounts
import io.hotmoka.android.mokito.model.Model
import io.hotmoka.beans.updates.Update
import io.hotmoka.beans.values.StorageReference
import io.hotmoka.crypto.BIP39Words


abstract class AbstractFragment : Fragment(), View {

    companion object {
        const val TAG = "AbstractFragment"
    }

    override fun onStart() {
        super.onStart()
        context.applicationContext.view = this
        setSubtitle("")
    }

    override fun onStop() {
        context.applicationContext.view = null

        val inputMethodManager: InputMethodManager = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        if (inputMethodManager.isAcceptingText)
            context.currentFocus?.let {
                inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
            }

        super.onStop()
    }

    override fun getContext(): Mokito {
        return super.getContext() as Mokito
    }

    protected fun setSubtitle(subtitle: String) {
        context.supportActionBar!!.subtitle = subtitle
    }

    protected fun getController(): Controller {
        return context.applicationContext.controller
    }

    protected fun getModel(): Model {
        return context.applicationContext.model
    }

    override fun onManifestChanged(manifest: StorageReference) {
    }

    override fun onStateChanged(reference: StorageReference, state: Array<Update>) {
    }

    override fun onAccountsChanged(accounts: Accounts) {
    }

    override fun onAccountCreated(account: Account) {
    }

    override fun onBip39Available(account: Account, bip39: BIP39Words) {
    }

    protected fun notifyException(t: Throwable) {
        Log.d(TAG, "action failed with the following exception", t)
        Toast.makeText(context, t.toString(), Toast.LENGTH_LONG).show()
    }

    override fun notifyUser(message: String) {
        Log.d(TAG, message)
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}