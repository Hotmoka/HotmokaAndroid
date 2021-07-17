package io.hotmoka.android.mokito

import android.app.Application
import io.hotmoka.android.mokito.controller.Controller
import io.hotmoka.android.mokito.model.Model
import io.hotmoka.android.mokito.view.View

class MVC: Application() {
    val model = Model(this)

    var view: View? = null
        set(value) {
            field = value

            if (value != null)
                controller.ensureConnected()
        }

    val controller = Controller(this)
}