package io.hotmoka.android.mokito

import android.app.Application
import io.hotmoka.android.mokito.controller.Controller
import io.hotmoka.android.mokito.model.Model
import io.hotmoka.android.mokito.view.View

class MVC: Application() {
    val model = Model(this)
    var view: View? = null // dynamically set to the current fragment
    val controller = Controller(this)
}