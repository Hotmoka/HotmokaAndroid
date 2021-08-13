package io.hotmoka.android.mokito.view.accounts

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.databinding.DialogFragmentSendCoinsBinding
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.view.AbstractDialogFragment
import io.hotmoka.views.AccountCreationHelper
import java.math.BigInteger

class SendCoinsDialogFragment: AbstractDialogFragment() {
    private lateinit var payer: Account
    private var _binding: DialogFragmentSendCoinsBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val TAG = "SendCoinsDialog"
        private const val PAYER_KEY = "payer"

        fun show(father: Fragment, payer: Account) {
            val dialog = SendCoinsDialogFragment()
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
        _binding = DialogFragmentSendCoinsBinding.inflate(layoutInflater)
        binding.hideShowPassword.controls(binding.accountPassword)
        binding.anonymousDescription.text = getString(R.string.anonymous_description, AccountCreationHelper.EXTRA_GAS_FOR_ANONYMOUS)
        binding.balance.hint = getString(R.string.amount_to_pay, payer.balance.toString())

        val dialog = AlertDialog.Builder(context)
            .setTitle(getString(R.string.pay_with, payer.name))
            .setIcon(R.drawable.ic_send)
            .setView(binding.root)
            .setMessage(getString(R.string.payment_message, payer.name))
            .setNegativeButton(R.string.dismiss) { _, _ -> }
            .setPositiveButton(R.string.pay) { _, _ ->
                val input = binding.destination.text.toString()
                val password = binding.accountPassword.text.toString()
                val amount = BigInteger(binding.balance.text.toString())

                if (binding.destination.text.contains('#')) {
                    val destination = validateStorageReference(input)
                    getController().requestPayment(
                        payer,
                        destination,
                        amount,
                        password
                    )
                }
                else {
                    getController().requestPaymentToPublicKey(
                        payer,
                        input,
                        amount,
                        binding.anonymous.isSelected,
                        password
                    )
                }
            }
            .create()

        dialog.setOnShowListener {
            // we initially disable the OK button: only when the input looks correct it will be enabled
            enableDoneIfOK()
        }

        binding.balance.addTextChangedListener(object: TextWatcher {
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

    private fun enableDoneIfOK() {
        var enable = false

        try {
            val balance = BigInteger(binding.balance.text.toString())
            enable = balance.subtract(payer.balance).signum() <= 0
        }
        catch (e: NumberFormatException) {
        }

        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = enable
    }
}