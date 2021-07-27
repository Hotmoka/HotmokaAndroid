package io.hotmoka.android.mokito.view

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.model.Account

class DeleteAccountConfirmationDialogFragment: AbstractDialogFragment() {
    private var account: Account? = null

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
        account = arguments?.getParcelable(ACCOUNT_KEY)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.delete_question)
        builder.setIcon(R.drawable.ic_hand)

        account?.let {
            builder.setMessage(getString(R.string.delete_confirmation, it.name))
            builder.setPositiveButton(R.string.delete) { _, _ -> getController().requestConfirmedDelete(it) }
        }

        builder.setNegativeButton(R.string.keep) { _,_ -> }
        return builder.create()
    }
}