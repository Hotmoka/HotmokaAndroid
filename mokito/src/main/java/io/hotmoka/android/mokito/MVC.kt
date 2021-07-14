package io.hotmoka.android.mokito

import io.hotmoka.android.mokito.controller.Controller
import io.hotmoka.android.mokito.model.Model
import io.hotmoka.android.mokito.view.Mokito

class MVC {
    companion object {
        private val model: Model = Model()
        var view: Mokito? = null
        var controller: Controller? = null
    }
}