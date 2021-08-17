package io.hotmoka.android.mokito.view.accounts

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.databinding.FragmentSentCoinsReceiptBinding
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.model.Faucet
import io.hotmoka.android.mokito.view.AbstractFragment

class SentCoinsReceiptFragment: AbstractFragment<FragmentSentCoinsReceiptBinding>() {
    private lateinit var payer: Account

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        payer = SentCoinsReceiptFragmentArgs.fromBundle(requireArguments()).payer
    }

    override fun onStart() {
        super.onStart()
        setSubtitle(getString(R.string.paid_with, payer.name))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setBinding(FragmentSentCoinsReceiptBinding.inflate(inflater, container, false))
        val args = SentCoinsReceiptFragmentArgs.fromBundle(requireArguments())
        val payerReference = payer.reference.toString()
        val destination = args.destination.toString()
        val publicKey = args.publicKey
        val amount = resources.getQuantityString(R.plurals.panareas, args.amount.toInt(), args.amount)
        val anonymous = args.anonymous

        if (payer is Faucet)
            binding.message.text = getString(R.string.coins_sent_from_faucet_receipt, payerReference, destination, amount)
        else if (publicKey != null)
            if (anonymous)
                binding.message.text = getString(R.string.coins_sent_from_payer_to_public_key_anonymously_receipt, payerReference, destination, publicKey, amount)
            else
                binding.message.text = getString(R.string.coins_sent_from_payer_to_public_key_receipt, payerReference, destination, publicKey, amount)
        else
            binding.message.text = getString(R.string.coins_sent_from_payer_receipt, payerReference, destination, amount)

        binding.share.setOnClickListener { share() }

        return binding.root
    }

    private fun share() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject))
        intent.putExtra(Intent.EXTRA_TEXT, binding.message.text.toString())
        startActivity(Intent.createChooser(intent, getString(R.string.share_using)))
    }
}