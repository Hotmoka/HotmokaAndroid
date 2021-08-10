package io.hotmoka.android.mokito.view

import android.content.Context
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.widget.EditText

class PasswordVisibilityToggle @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : androidx.appcompat.widget.AppCompatImageButton(context, attrs) {

    fun controls(target: EditText) {
        setOnClickListener { togglePassword(target) }
    }

    private fun togglePassword(target: EditText) {
        isActivated = !isActivated
        if (isActivated)
            target.transformationMethod = HideReturnsTransformationMethod.getInstance()
        else
            target.transformationMethod = PasswordTransformationMethod.getInstance()
    }
}