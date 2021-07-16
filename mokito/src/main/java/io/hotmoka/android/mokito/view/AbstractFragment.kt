package io.hotmoka.android.mokito.view

import androidx.fragment.app.Fragment

abstract class AbstractFragment : Fragment() {

    override fun getContext(): Mokito {
        return super.getContext() as Mokito
    }
}