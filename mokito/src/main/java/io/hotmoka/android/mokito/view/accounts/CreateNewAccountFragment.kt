package io.hotmoka.android.mokito.view.accounts

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.databinding.FragmentCreateNewAccountBinding
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.model.Faucet
import io.hotmoka.android.mokito.view.AbstractFragment
import io.hotmoka.android.mokito.view.accounts.CreateNewAccountFragmentDirections.toShowAccount
import java.math.BigInteger

class CreateNewAccountFragment: AbstractFragment<FragmentCreateNewAccountBinding>() {
    private lateinit var payer: Account

    companion object {
        const val TAG = "CreateNewAccountFragment"
    }

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

            val max = payer.maxPayment()
            if (balanceOfNewAccount.subtract(max).signum() > 0) {
                val maxPanareas = resources.getQuantityString(R.plurals.panareas, if (max == BigInteger.ONE) 1 else 10, max)
                notifyUser(getString(R.string.amount_too_high, payer.name, maxPanareas))
                if (payer is Faucet)
                    Log.i(TAG, "Payment of $balanceOfNewAccount coins from the faucet rejected: request is too high")
                else
                    Log.i(TAG, "Payment of $balanceOfNewAccount coins from $payer rejected: not enough funds")

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
                    Log.i(TAG, "Creation of new account rejected: incorrect password")
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
            Log.w(TAG, "Illegal balance for new account: $e")
        }
    }

    override fun onAccountCreated(account: Account) {
        super.onAccountCreated(account)
        navigate(toShowAccount(account))
    }
}