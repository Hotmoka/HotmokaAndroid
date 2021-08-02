package io.hotmoka.android.mokito.view.accounts

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import java.math.BigInteger

class CreateAccountDialogFragment: AbstractDialogFragment() {
    private var payer: Account? = null
    private var _binding: DialogFragmentCreateAccountBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val TAG = "CreateAccountDialog"
        private const val PAYER_KEY = "payer"

        fun show(father: Fragment, payer: Account) {
            val dialog = CreateAccountDialogFragment()
            val args = Bundle()
            args.putParcelable(PAYER_KEY, payer)
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
        _binding = DialogFragmentCreateAccountBinding.inflate(layoutInflater)

        if (payer is Faucet) {
            binding.payerPassword.visibility = View.GONE
            binding.hideShowPayerPassword.visibility = View.GONE
        }
        else
            binding.hideShowPayerPassword.setOnClickListener { togglePassword(binding.hideShowPayerPassword, binding.payerPassword) }

        binding.hideShowNewAcccountPassword.setOnClickListener { togglePassword(binding.hideShowNewAcccountPassword, binding.accountPassword) }

        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.account_creation)
        builder.setIcon(R.drawable.ic_new)
        builder.setNegativeButton(R.string.cancel) { _, _ -> }
        builder.setView(binding.root)

        payer?.let {
            builder.setMessage(resources.getString(R.string.account_creation_message, it.name))
            binding.payerPassword.hint = resources.getString(R.string.payer_account_password, it.name)
            binding.accountBalance.hint = resources.getString(R.string.new_account_balance, maxAllowedForCreation().toString())
            if (it is Faucet)
                builder.setPositiveButton(R.string.done) { _, _ ->
                    getController().requestNewAccountFromFaucet(
                        binding.accountName.text.toString(),
                        binding.accountPassword.text.toString(),
                        BigInteger(binding.accountBalance.text.toString()))
                }
            else
                builder.setPositiveButton(R.string.done) { _, _ ->
                }
        }

        val dialog =  builder.create()
        dialog.setOnShowListener {
            // we initially disable the OK button: only when input looks correct it will be enabled
            enableDoneIfOK()
        }

        binding.accountBalance.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                enableDoneIfOK()
            }
        })

        return dialog
    }

    /**
     * Yields the maximum balance allowed for a new account. Normally, this is the
     * balance of the payer, but if the payer is the faucet, there is a smaller limit for it.
     */
    private fun maxAllowedForCreation(): BigInteger {
        payer?.let {
            return if (it is Faucet)
                // the faucet has a limit that is normally smaller than its balance
                it.maxFaucet.min(it.balance)
            else
                it.balance
        }

        return BigInteger.ZERO
    }

    private fun enableDoneIfOK() {
        var enable = false

        payer?.let {
            try {
                val balance = BigInteger(binding.accountBalance.text.toString())
                enable = balance.subtract(maxAllowedForCreation()).signum() <= 0
            } catch (e: NumberFormatException) {
            }
        }

        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = enable
    }
}