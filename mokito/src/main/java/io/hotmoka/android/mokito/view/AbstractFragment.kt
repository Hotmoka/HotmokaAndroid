package io.hotmoka.android.mokito.view

import android.widget.Toast
import androidx.fragment.app.Fragment
import io.hotmoka.android.mokito.controller.Controller
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.model.Accounts
import io.hotmoka.android.mokito.model.Model
import io.hotmoka.beans.updates.Update
import io.hotmoka.beans.values.StorageReference

abstract class AbstractFragment : Fragment(), View {

    override fun onStart() {
        super.onStart()
        context.applicationContext.view = this
        setSubtitle("")
    }

    override fun onStop() {
        context.applicationContext.view = null
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

    override fun askForConfirmationOfDeleting(account: Account) {
    }

    override fun askForEdit(account: Account) {
    }

    protected fun notifyException(t: Throwable) {
        notifyProblem(t.toString())
    }

    protected fun notifyProblem(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}