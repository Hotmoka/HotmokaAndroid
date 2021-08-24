package io.hotmoka.android.mokito.view.accounts

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import io.hotmoka.android.mokito.R
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
        return AlertDialog.Builder(context)
            .setTitle(if (account.isKey()) R.string.delete_key_question else R.string.delete_account_question)
            .setIcon(R.drawable.ic_delete)
            .setMessage(
                getString(
                    if (account.isKey()) R.string.delete_key_confirmation_message else R.string.delete_confirmation_message,
                    account.name
                )
            )
            .setNegativeButton(R.string.dismiss) { _, _ -> }
            .setPositiveButton(R.string.delete) { _, _ -> getController().requestDelete(account) }
            .create()
    }
}