package io.hotmoka.android.mokito.view.accounts

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.databinding.DialogFragmentDeleteAccountConfirmationBinding
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.view.AbstractDialogFragment

class DeleteAccountConfirmationDialogFragment: AbstractDialogFragment() {
    private lateinit var account: Account

    companion object {
        private const val TAG = "DeleteAccountConfirmationDialog"
        private const val ACCOUNT_KEY = "account"

        fun show(father: Fragment, account: Account) {
            val dialog = DeleteAccountConfirmationDialogFragment()
            val args = Bundle()
            args.putParcelable(ACCOUNT_KEY, account)
            dialog.arguments = args
            dialog.show(father.childFragmentManager, TAG)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        account = arguments?.getParcelable(ACCOUNT_KEY)!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogFragmentDeleteAccountConfirmationBinding.inflate(layoutInflater)
        binding.hideShowPassword.controls(binding.accountPassword)

        val builder = AlertDialog.Builder(context)
            .setTitle(R.string.delete_question)
            .setIcon(R.drawable.ic_delete)
            .setView(binding.root)
            .setNegativeButton(R.string.keep) { _, _ -> }

        if (account.isKey())
            builder.setMessage(getString(R.string.delete_key_confirmation_message, account.name))
        else
            builder.setMessage(getString(R.string.delete_confirmation_message, account.name))

        builder.setPositiveButton(R.string.delete) { _, _ ->
            getController().requestDelete(account, binding.accountPassword.text.toString())
        }

        return builder.create()
    }
}