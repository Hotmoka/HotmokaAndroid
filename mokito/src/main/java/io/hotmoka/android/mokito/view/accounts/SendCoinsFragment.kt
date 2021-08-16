package io.hotmoka.android.mokito.view.accounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.databinding.FragmentSendCoinsBinding
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.view.AbstractFragment
import io.hotmoka.beans.values.StorageReference
import io.hotmoka.crypto.Base58
import io.hotmoka.views.AccountCreationHelper
import java.lang.IllegalArgumentException
import java.math.BigInteger

class SendCoinsFragment: AbstractFragment<FragmentSendCoinsBinding>() {
    private lateinit var payer: Account

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        payer = SendCoinsFragmentArgs.fromBundle(requireArguments()).payer
    }

    override fun onStart() {
        super.onStart()
        setSubtitle(getString(R.string.pay_with, payer.name))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setBinding(FragmentSendCoinsBinding.inflate(inflater, container, false))
        binding.hideShowPassword.controls(binding.accountPassword)
        binding.anonymousDescription.text = getString(R.string.anonymous_description, AccountCreationHelper.EXTRA_GAS_FOR_ANONYMOUS)
        binding.balance.hint = getString(R.string.amount_to_pay, payer.balance.toString())
        binding.heading.text = getString(R.string.payment_message, payer.name)
        binding.pay.setOnClickListener { pay() }

        return binding.root
    }

    private fun pay() {
        val input = binding.destination.text.toString()
        val password = binding.accountPassword.text.toString()
        val amount: BigInteger

        try {
            amount = BigInteger(binding.balance.text.toString())
        }
        catch (e: NumberFormatException) {
            notifyUser(getString(R.string.illegal_amount_to_pay))
            return
        }

        if (amount.subtract(payer.balance).signum() > 0) {
            notifyUser(getString(R.string.not_enough_coins, payer.name))
            return
        }

        if (!getController().passwordIsCorrect(payer, password)) {
            notifyUser(getString(R.string.incorrect_password))
            return
        }

        if (looksLikeStorageReference(input)) {
            // first we check if the destination looks like a storage reference
            val destination = validateStorageReference(input)
            getController().requestPayment(
                payer,
                destination,
                amount,
                password
            )
            findNavController().popBackStack()
        }
        else if (looksLikePublicKey(input)) {
            // otherwise, if might looks like a public key
            getController().requestPaymentToPublicKey(
                payer,
                input,
                amount,
                binding.anonymous.isChecked,
                password
            )
            findNavController().popBackStack()
        }
        else
            notifyUser(getString(R.string.destination_syntax_error))
    }

    private fun looksLikePublicKey(s: String): Boolean {
        return try {
            return Base58.decode(s).size == 32 // ed25519 public keys are 32 bytes long
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun looksLikeStorageReference(s: String): Boolean {
        return try {
            validateStorageReference(s)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    override fun onPaymentCompleted(
        payer: Account,
        destination: StorageReference,
        amount: BigInteger,
        anonymous: Boolean
    ) {
        super.onPaymentCompleted(payer, destination, amount, anonymous)
        if (!anonymous) {
            // present a receipt to the user, that can be shared if she wants
        }
    }
}