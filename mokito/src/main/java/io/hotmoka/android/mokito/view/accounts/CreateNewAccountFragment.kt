package io.hotmoka.android.mokito.view.accounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.databinding.FragmentCreateNewAccountBinding
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.model.Faucet
import io.hotmoka.android.mokito.view.AbstractFragment
import java.math.BigInteger

class CreateNewAccountFragment: AbstractFragment<FragmentCreateNewAccountBinding>() {
    private lateinit var payer: Account

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        payer = CreateNewAccountFragmentArgs.fromBundle(requireArguments()).payer
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setBinding(FragmentCreateNewAccountBinding.inflate(inflater, container, false))

        if (payer is Faucet) {
            binding.payerPassword.visibility = View.GONE
            binding.hideShowPayerPassword.visibility = View.GONE
        }

        binding.payerPassword.hint = getString(R.string.payer_account_password, payer.name)
        binding.heading.text = resources.getString(R.string.account_creation_message, payer.name)

        binding.createNewAccountButton.setOnClickListener {
            closeKeyboard()
            createNewAccount()
        }

        return binding.root
    }

    private fun createNewAccount() {
        try {
            val balanceOfNewAccount = binding.coinType.asPanareas()
            val max = maxAllowedForCreation()
            if (balanceOfNewAccount.subtract(max).signum() > 0) {
                val maxPanareas = resources.getQuantityString(R.plurals.panareas, max.toInt(), max)
                notifyUser(getString(R.string.not_enough_coins, payer.name, maxPanareas))
                return
            }

            if (payer is Faucet)
                getController().requestNewAccountFromFaucet(
                    binding.accountName.text.toString(),
                    binding.accountPassword.text.toString(),
                    balanceOfNewAccount
                )
            else {
                val passwordOfPayer = binding.payerPassword.text.toString()
                if (!getController().passwordIsCorrect(payer, passwordOfPayer)) {
                    notifyUser(getString(R.string.incorrect_password))
                    return
                }

                getController().requestNewAccountFromAnotherAccount(
                    payer,
                    binding.payerPassword.text.toString(),
                    binding.accountName.text.toString(),
                    binding.accountPassword.text.toString(),
                    balanceOfNewAccount
                )
            }
        }
        catch (e: java.lang.NumberFormatException) {
            notifyUser(getString(R.string.illegal_balance_for_new_account))
        }
    }

    override fun onAccountCreated(account: Account) {
        super.onAccountCreated(account)
        navigate(CreateNewAccountFragmentDirections.actionShowNewAccount(account))
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
}