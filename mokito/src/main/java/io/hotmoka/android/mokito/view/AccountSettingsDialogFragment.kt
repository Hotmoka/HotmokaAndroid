package io.hotmoka.android.mokito.view

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.fragment.app.Fragment
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.model.Account

class AccountSettingsDialogFragment: AbstractDialogFragment() {
    private var account: Account? = null

    companion object {
        private const val TAG = "AccountSettingsDialog"
        private const val ACCOUNT_KEY = "account"

        fun show(father: Fragment, account: Account) {
            val dialog = AccountSettingsDialogFragment()
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
        builder.setTitle(R.string.account_settings)
        builder.setIcon(R.drawable.ic_settings)
        val view = layoutInflater.inflate(R.layout.dialog_fragment_account_settings, null)
        val accountNameEditText: EditText? = view?.findViewById(R.id.account_name)

        accountNameEditText?.let {
            account?.let { old ->
                accountNameEditText.setText(old.name)
                builder.setPositiveButton(R.string.done) { _, _ ->
                    val new = old.setName(accountNameEditText.text.toString())
                    if (old.name != new.name)
                        getController().requestReplace(old, new)
                }
            }
        }

        builder.setNegativeButton(R.string.cancel) { _, _ -> }
        builder.setView(view)

        return builder.create()
    }
}