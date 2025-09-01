package io.hotmoka.android.mokito.view

import android.widget.Toast
import androidx.annotation.UiThread
import androidx.fragment.app.DialogFragment
import io.hotmoka.android.mokito.controller.Controller
import io.hotmoka.android.mokito.model.Model

abstract class AbstractDialogFragment: DialogFragment() {

    override fun getContext(): Mokito {
        return super.getContext() as Mokito
    }

    protected fun getController(): Controller {
        return context.applicationContext.controller
    }

    protected fun getModel(): Model {
        return context.applicationContext.model
    }

    @UiThread protected fun notifyUser(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}