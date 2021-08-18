package io.hotmoka.android.mokito.view.accounts

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.Fragment
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.databinding.DialogFragmentCreateAccountBinding
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.model.Faucet
import io.hotmoka.android.mokito.view.AbstractDialogFragment
import java.math.BigInteger

class CreateAccountDialogFragment: AbstractDialogFragment() {
    private lateinit var payer: Account
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
        payer = arguments?.getParcelable(PAYER_KEY)!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogFragmentCreateAccountBinding.inflate(layoutInflater)

        if (payer is Faucet) {
            binding.payerPassword.visibility = View.GONE
            binding.hideShowPayerPassword.visibility = View.GONE
        }

        binding.payerPassword.hint = getString(R.string.payer_account_password, payer.name)
        binding.accountBalance.hint = getString(
            R.string.new_account_balance,
            maxAllowedForCreation().toString()
        )

        val builder = AlertDialog.Builder(context)
            .setTitle(R.string.account_creation)
            .setIcon(R.drawable.ic_new)
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .setView(binding.root)
            .setMessage(resources.getString(R.string.account_creation_message, payer.name))

        builder.setPositiveButton(R.string.done) { _, _ ->
            if (payer is Faucet)
                getController().requestNewAccountFromFaucet(
                    binding.accountName.text.toString(),
                    binding.accountPassword.text.toString(),
                    BigInteger(binding.accountBalance.text.toString())
                )
            else
                getController().requestNewAccountFromAnotherAccount(
                    payer,
                    binding.payerPassword.text.toString(),
                    binding.accountName.text.toString(),
                    binding.accountPassword.text.toString(),
                    BigInteger(binding.accountBalance.text.toString())
                )
        }

        val dialog =  builder.create()
        dialog.setOnShowListener {
            // we initially disable the OK button: only when the input looks correct it will be enabled
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
        payer.let {
            return if (it is Faucet)
                // the faucet has a limit that is normally smaller than its balance
                it.maxFaucet.min(it.balance)
            else
                it.balance
        }
    }

    private fun enableDoneIfOK() {
        var enable = false

        try {
            val balance = BigInteger(binding.accountBalance.text.toString())
            enable = balance.subtract(maxAllowedForCreation()).signum() <= 0
        }
        catch (e: NumberFormatException) {
        }

        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = enable
    }
}