package io.hotmoka.android.mokito.view

import android.app.Activity
import android.content.Context
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import io.hotmoka.android.mokito.R

class PasswordVisibilityToggle constructor(
    context: Context, attrs: AttributeSet
) : androidx.appcompat.widget.AppCompatImageButton(context, attrs) {

    private val controls: Int

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.PasswordVisibilityToggle,
            0, 0).apply {

            try {
                controls = getResourceId(R.styleable.PasswordVisibilityToggle_controls, -1)
            } finally {
                recycle()
            }
        }

        setOnClickListener { togglePassword() }
    }

    private fun togglePassword() {
        if (controls >= 0) {
            var parent = this.parent
            while (parent.parent is android.view.View)
                parent = parent.parent

            if (parent is android.view.View) {
                val target: TextView = parent.findViewById(controls)
                target.let {
                    isActivated = !isActivated
                    if (isActivated)
                        it.transformationMethod = HideReturnsTransformationMethod.getInstance()
                    else
                        it.transformationMethod = PasswordTransformationMethod.getInstance()
                }
            }
        }
    }
}