package io.hotmoka.android.mokito.view.accounts

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.zxing.integration.android.IntentIntegrator
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.databinding.FragmentSendCoinsBinding
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.model.Faucet
import io.hotmoka.android.mokito.view.AbstractFragment
import io.hotmoka.helpers.Coin
import io.hotmoka.node.api.values.StorageReference
import java.math.BigInteger
import io.hotmoka.android.mokito.view.accounts.SendCoinsFragmentDirections.*
import io.hotmoka.node.api.transactions.TransactionReference

class SendCoinsFragment: AbstractFragment<FragmentSendCoinsBinding>() {
    private lateinit var payer: Account

    companion object {
        const val TAG = "SendCoinsFragment"
    }

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
        binding.accountPassword.hint = getString(R.string.payer_account_password, payer.name)
        binding.anonymousDescription.text = getString(R.string.anonymous_description, "500000")
        binding.amount.hint = getString(R.string.amount_to_pay)
        binding.heading.text =
            if (payer is Faucet)
                getString(R.string.payment_from_faucet_message)
            else
                getString(R.string.payment_message, payer.name)
        binding.destination.hint =
            if (payer is Faucet)
                getString(R.string.destination_account)
            else
                getString(R.string.destination_account_or_key)
        binding.qrCode.setOnClickListener { readQrCode() }
        binding.pay.setOnClickListener { pay() }

        if (payer is Faucet) {
            binding.accountPassword.visibility = View.GONE
            binding.anonymousDescription.visibility = View.GONE
            binding.anonymous.visibility = View.GONE
            binding.hideShowPassword.visibility = View.GONE
        }

        return binding.root
    }

    private fun readQrCode() {
        IntentIntegrator(context)
            .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            .setPrompt(getString(R.string.scan_qr_code))
            .setCameraId(0)
            .setBeepEnabled(true)
            .setBarcodeImageEnabled(false)
            .initiateScan()
    }

    override fun onQRScanAvailable(data: String) {
        val parts = data.split('&')
        if (parts.size != 3) {
            notifyUser(getString(R.string.wrong_qr_code_data))
            Log.w(TAG, "The QR code data contains an unexpected number of snippets: $parts.size instead of 3")
        }
        else {
            val anonymous: Boolean = if (parts[2] == "true")
                true
            else if (parts[2] == "false")
                false
            else {
                notifyUser(getString(R.string.wrong_qr_code_data))
                Log.w(TAG, "The QR code data does not report a Boolean as anonymous")
                return
            }

            binding.destination.setText(parts[0])
            binding.amount.setText(parts[1])
            binding.anonymous.isChecked = anonymous
            binding.coinType.setSelection(Coin.PANAREA.ordinal) // set unit to Panareas
        }
    }

    private fun pay() {
        val input = binding.destination.text.toString()
        val password = binding.accountPassword.text.toString()
        val amount: BigInteger

        try {
            amount = binding.coinType.asPanareas()
        }
        catch (e: NumberFormatException) {
            notifyUser(getString(R.string.illegal_amount_to_pay))
            Log.w(TAG, "Illegal amount to pay: $e")
            return
        }

        if (amount.signum() < 0) {
            notifyUser(getString(R.string.coins_cant_be_negative))
            Log.w(TAG, "Illegal negative amount to pay")
            return
        }

        val max = payer.maxPayment()
        if (amount.subtract(max).signum() > 0) {
            val maxPanareas = resources.getQuantityString(R.plurals.panareas, max.toInt(), max)
            notifyUser(getString(R.string.amount_too_high, payer.name, maxPanareas))
            Log.w(TAG, "The amount to pay is too big: $amount against a maximum of $max")
            return
        }

        if (payer !is Faucet && !getController().passwordIsCorrect(payer, password)) {
            notifyUser(getString(R.string.incorrect_password))
            Log.w(TAG, "Incorrect password")
            return
        }

        if (looksLikeStorageReference(input)) {
            // first we check if the destination looks like a storage reference
            val destination = validateStorageReference(input)
            payer.let {
                if (it is Faucet)
                    getController().requestPaymentFromFaucet(it, destination, amount)
                else
                    getController().requestPayment(it, destination, amount, password)
            }
        }
        else if (looksLikePublicKey(input))
            // otherwise, if might looks like a public key
            if (payer is Faucet) {
                notifyUser(getString(R.string.payment_to_key_not_implemented_for_faucet))
                Log.w(TAG, "Payment to key is not currently implemented for the faucet")
            }
            else
                getController().requestPaymentToPublicKey(
                    payer,
                    input,
                    amount,
                    binding.anonymous.isChecked,
                    password
                )
        else if (payer is Faucet) {
            notifyUser(getString(R.string.destination_syntax_for_faucet_error))
            Log.w(TAG, "The destination is not a storage reference")
        }
        else {
            notifyUser(getString(R.string.destination_syntax_error))
            Log.w(TAG, "The destination is not a storage reference nor a Base58-encoded key")
        }
    }

    override fun onPaymentCompleted(
        payer: Account,
        destination: StorageReference,
        publicKey: String?,
        amount: BigInteger,
        anonymous: Boolean,
        transactions: List<TransactionReference>
    ) {
        super.onPaymentCompleted(payer, destination, publicKey, amount, anonymous, transactions)
        if (payer == this.payer)
            // present a receipt to the user, who can share it if she wants
            navigate(toSentCoinsReceipt(payer, destination, publicKey, amount, anonymous, ArrayList(transactions)))
    }
}