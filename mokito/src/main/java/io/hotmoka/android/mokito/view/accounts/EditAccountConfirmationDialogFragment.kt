package io.hotmoka.android.mokito.view.accounts

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.databinding.DialogFragmentEditAccountConfirmationBinding
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.view.AbstractDialogFragment

class EditAccountConfirmationDialogFragment: AbstractDialogFragment() {
    private lateinit var old: Account
    private lateinit var new: Account

    companion object {
        private const val TAG = "EditAccountConfirmationDialog"
        private const val ACCOUNT_OLD_KEY = "old"
        private const val ACCOUNT_NEW_KEY = "new"

        fun show(father: Fragment, old: Account, new: Account) {
            val dialog = EditAccountConfirmationDialogFragment()
            val args = Bundle()
            args.putParcelable(ACCOUNT_OLD_KEY, old)
            args.putParcelable(ACCOUNT_NEW_KEY, new)
            dialog.arguments = args
            dialog.show(father.childFragmentManager, TAG)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        old = arguments?.getParcelable(ACCOUNT_OLD_KEY)!!
        new = arguments?.getParcelable(ACCOUNT_NEW_KEY)!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogFragmentEditAccountConfirmationBinding.inflate(layoutInflater)

        val builder = AlertDialog.Builder(context)
            .setTitle(if (old.isKey()) R.string.edit_key_question else R.string.edit_account_question)
            .setIcon(R.drawable.ic_edit)
            .setView(binding.root)
            .setNegativeButton(R.string.dismiss) { _, _ -> }
            .setPositiveButton(R.string.edit) {
                _, _ -> getController().requestReplace(old, new, binding.accountPassword.text.toString())
            }

        return builder.create()
    }
}