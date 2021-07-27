package io.hotmoka.android.mokito.view

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import io.hotmoka.android.mokito.MVC
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.model.Account

class DeleteAccountConfirmationDialogFragment: DialogFragment() {
    private var account: Account? = null

    companion object {
        private const val TAG = "DeleteAccountConfirmationDialog"

        fun show(father: Fragment, account: Account) {
            val dialog = DeleteAccountConfirmationDialogFragment()
            val args = Bundle()
            args.putParcelable("account", account)
            dialog.arguments = args
            dialog.show(father.childFragmentManager, TAG)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        account = arguments?.getParcelable("account")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.delete_question)
        builder.setIcon(R.drawable.ic_hand)

        account?.let {
            builder.setMessage(getString(R.string.delete_confirmation, it.name))
            builder.setPositiveButton(R.string.delete) { _, _ ->
                (context.applicationContext as MVC).controller.requestConfirmedDelete(it)
            }
        }

        builder.setNegativeButton(R.string.keep) { _,_ -> }
        return builder.create()
    }
}