package io.hotmoka.android.mokito.view.accounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.databinding.FragmentSendCoinsFromFaucetBinding
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.model.Faucet
import io.hotmoka.android.mokito.view.AbstractFragment
import io.hotmoka.beans.values.StorageReference
import java.math.BigInteger

class SendCoinsFromFaucetFragment: AbstractFragment<FragmentSendCoinsFromFaucetBinding>() {
    private lateinit var payer: Faucet
    private lateinit var max: BigInteger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        payer = SendCoinsFromFaucetFragmentArgs.fromBundle(requireArguments()).payer
    }

    override fun onStart() {
        super.onStart()
        setSubtitle(getString(R.string.pay_with, payer.name))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setBinding(FragmentSendCoinsFromFaucetBinding.inflate(inflater, container, false))
        max = payer.maxFaucet.min(payer.balance)
        binding.balance.hint = getString(R.string.amount_to_pay, max.toString())
        binding.heading.text = getString(R.string.payment_from_faucet_message)
        binding.pay.setOnClickListener { pay() }

        return binding.root
    }

    private fun pay() {
        val input = binding.destination.text.toString()
        val amount: BigInteger

        try {
            amount = BigInteger(binding.balance.text.toString())
        }
        catch (e: NumberFormatException) {
            notifyUser(getString(R.string.illegal_amount_to_pay))
            return
        }

        if (amount.subtract(max).signum() > 0) {
            notifyUser(getString(R.string.too_much_from_faucet, max))
            return
        }

        if (looksLikeStorageReference(input)) {
            // first we check if the destination looks like a storage reference
            val destination = validateStorageReference(input)
            getController().requestPaymentFromFaucet(
                payer,
                destination,
                amount
            )
        }
        else
            notifyUser(getString(R.string.destination_syntax_for_faucet_error))
    }

    override fun onPaymentCompleted(
        payer: Account,
        destination: StorageReference,
        publicKey: String?,
        amount: BigInteger,
        anonymous: Boolean
    ) {
        super.onPaymentCompleted(payer, destination, publicKey, amount, anonymous)
        if (payer == this.payer)
            // present a receipt to the user, that can be shared if she wants
            findNavController().navigate(
                SendCoinsFromFaucetFragmentDirections.actionSendCoinsFromFaucetToSentCoinsReceipt(
                    payer,
                    destination,
                    publicKey,
                    amount,
                    anonymous
                )
            )
    }
}