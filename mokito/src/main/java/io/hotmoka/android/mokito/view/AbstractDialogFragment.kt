package io.hotmoka.android.mokito.view

import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import io.hotmoka.android.mokito.controller.Controller
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.model.Accounts
import io.hotmoka.android.mokito.model.Model
import io.hotmoka.beans.updates.Update
import io.hotmoka.beans.values.StorageReference

abstract class AbstractDialogFragment : DialogFragment() {

    override fun getContext(): Mokito {
        return super.getContext() as Mokito
    }

    protected fun getController(): Controller {
        return context.applicationContext.controller
    }

    protected fun getModel(): Model {
        return context.applicationContext.model
    }

    protected fun notifyException(t: Throwable) {
        notifyProblem(t.toString())
    }

    protected fun notifyProblem(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}