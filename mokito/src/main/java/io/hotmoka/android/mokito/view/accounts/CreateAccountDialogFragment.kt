package io.hotmoka.android.mokito.view.accounts

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import androidx.fragment.app.Fragment
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.databinding.DialogFragmentCreateAccountBinding
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.model.Faucet
import io.hotmoka.android.mokito.view.AbstractDialogFragment

class CreateAccountDialogFragment: AbstractDialogFragment() {
    private var payer: Account? = null

    companion object {
        private const val TAG = "CreateAccountDialog"
        private const val PAYER_KEY = "payer"

        fun show(father: Fragment, account: Account) {
            val dialog = CreateAccountDialogFragment()
            val args = Bundle()
            args.putParcelable(PAYER_KEY, account)
            dialog.arguments = args
            dialog.show(father.childFragmentManager, TAG)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        payer = arguments?.getParcelable(PAYER_KEY)
    }

    private fun togglePassword(button: View, passwordField: EditText) {
        button.isActivated = !button.isActivated
        if (button.isActivated)
            passwordField.transformationMethod = HideReturnsTransformationMethod.getInstance()
        else
            passwordField.transformationMethod = PasswordTransformationMethod.getInstance()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = DialogFragmentCreateAccountBinding.inflate(layoutInflater)

        if (payer is Faucet) {
            view.payerPassword.visibility = View.GONE
            view.hideShowPayerPassword.visibility = View.GONE
        }
        else
            view.hideShowPayerPassword.setOnClickListener { togglePassword(view.hideShowPayerPassword, view.payerPassword) }

        view.hideShowNewAcccountPassword.setOnClickListener { togglePassword(view.hideShowNewAcccountPassword, view.accountPassword) }

        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.account_creation)
        builder.setIcon(R.drawable.ic_new)
        builder.setNegativeButton(R.string.cancel) { _, _ -> }
        builder.setView(view.root)

        payer?.let {
            builder.setMessage(resources.getString(R.string.account_creation_message, it.name))
            view.payerPassword.hint = resources.getString(R.string.payer_account_password, it.name)
            builder.setPositiveButton(R.string.done) { _, _ -> }
        }

        return builder.create()
    }
}