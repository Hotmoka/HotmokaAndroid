package io.hotmoka.android.mokito.view.accounts

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.databinding.DialogFragmentCreateKeyBinding
import io.hotmoka.android.mokito.view.AbstractDialogFragment

class CreateKeyDialogFragment: AbstractDialogFragment() {

    companion object {
        fun show(father: Fragment) {
            CreateKeyDialogFragment().show(father.childFragmentManager, "CreateKeyDialog")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogFragmentCreateKeyBinding.inflate(layoutInflater)

        return AlertDialog.Builder(context)
            .setTitle(R.string.action_create_key)
            .setIcon(R.drawable.ic_new)
            .setMessage(getString(R.string.description_new_key))
            .setPositiveButton(R.string.create) { _, _ -> getController().requestNewKeyPair(binding.accountPassword.text.toString()) }
            .setNegativeButton(R.string.dismiss) { _,_ -> }
            .setView(binding.root)
            .create()
    }
}