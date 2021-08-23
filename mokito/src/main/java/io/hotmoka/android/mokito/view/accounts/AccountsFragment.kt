package io.hotmoka.android.mokito.view.accounts

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.databinding.AccountCardBinding
import io.hotmoka.android.mokito.databinding.FragmentAccountsBinding
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.model.Accounts
import io.hotmoka.android.mokito.model.Faucet
import io.hotmoka.android.mokito.view.AbstractFragment
import io.hotmoka.android.mokito.view.accounts.AccountsFragmentDirections.*
import io.hotmoka.beans.Coin
import java.math.BigDecimal
import java.math.BigInteger

class AccountsFragment : AbstractFragment<FragmentAccountsBinding>() {
    private lateinit var adapter: RecyclerAdapter

    companion object {
        @Suppress("ObjectPropertyName")
        private val _1000 = BigDecimal(1000)

        private val idsOfPluralsOfCoins = arrayOf(
            R.plurals.panareas, R.plurals.alicudis, R.plurals.filicudis, R.plurals.strombolis,
            R.plurals.vulcanos, R.plurals.salinas, R.plurals.liparis, R.plurals.mokas
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setBinding(FragmentAccountsBinding.inflate(inflater, container, false))
        adapter = RecyclerAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.accounts, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_reload -> {
                getController().requestAccounts()
                true
            }
            R.id.action_import -> {
                navigate(toImportAccount())
                true
            }
            R.id.action_create_key -> {
                CreateKeyDialogFragment.show(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        showOrRequestAccounts()
    }

    private fun showOrRequestAccounts() {
        val accounts = getModel().getAccounts()
        if (accounts != null)
            onAccountsChanged(accounts)
        else
            getController().requestAccounts()
    }

    override fun onAccountsChanged(accounts: Accounts) {
        adapter.setAccounts(accounts)
    }

    private inner class RecyclerAdapter: RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {
        private var accounts = emptyArray<Account>()

        @SuppressLint("NotifyDataSetChanged")
        fun setAccounts(accounts: Accounts) {
            this.accounts = accounts.getAll().toArray { i -> arrayOfNulls(i) }
            notifyDataSetChanged()
        }

        private inner class ViewHolder(val binding: AccountCardBinding): RecyclerView.ViewHolder(binding.root) {

            /**
             * Binds the view holder to an account that is actually a public key,
             * still waiting for the corresponding account object to be created.
             */
            private fun bindToKey(account: Account) {
                binding.card.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.key))
                binding.name.text = account.name
                binding.reference.text = getString(R.string.waiting_for_payment_to_this_key)
                binding.balance.visibility = GONE
                binding.newAccount.visibility = GONE
                binding.send.visibility = GONE
                receiveIsVisible(account)
                deleteIsVisible(account)
                settingsIsVisible(account)
            }

            /**
             * Binds the view holder to an account that is actually the faucet of the node.
             */
            private fun bindToFaucet(account: Account) {
                binding.card.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.faucet))
                binding.name.text = account.name
                binding.reference.text = account.reference.toString()
                balanceIsVisible(account)
                newIsVisible(account)
                sendIsVisible(account)
                receiveIsVisible(account)
                binding.delete.visibility = GONE
                binding.settings.visibility = GONE
            }

            /**
             * Binds the view holder to an account that is not the faucet and is accessible.
             */
            private fun bindToAccessible(account: Account) {
                binding.card.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.accessible_account))
                binding.name.text = account.name
                binding.reference.text = account.reference.toString()
                balanceIsVisible(account)
                newIsVisible(account)
                sendIsVisible(account)
                receiveIsVisible(account)
                deleteIsVisible(account)
                settingsIsVisible(account)
            }

            /**
             * Binds the view holder to an account that is not the faucet and is inaccessible.
             */
            private fun bindToInaccessible(account: Account) {
                binding.card.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.inaccessible_account))
                binding.name.text = account.name
                binding.reference.text = account.reference.toString()
                binding.balance.text = resources.getString(R.string.account_not_accessible)
                binding.balance.visibility = VISIBLE
                binding.newAccount.visibility = GONE
                binding.send.visibility = GONE
                binding.receive.visibility = GONE
                deleteIsVisible(account)
                settingsIsVisible(account)
            }

            private fun settingsIsVisible(account: Account) {
                binding.settings.visibility = VISIBLE
                binding.settings.setOnClickListener { navigate(toShowAccount(account)) }
            }

            private fun deleteIsVisible(account: Account) {
                binding.delete.visibility = VISIBLE
                binding.delete.setOnClickListener {
                    DeleteAccountConfirmationDialogFragment.show(this@AccountsFragment, account)
                }
            }

            private fun newIsVisible(account: Account) {
                binding.newAccount.visibility = VISIBLE
                binding.newAccount.setOnClickListener { navigate(toCreateNewAccount(account)) }
            }

            private fun balanceIsVisible(account: Account) {
                binding.balance.text = descriptionOfBalance(account.balance, account.coin)
                binding.balance.visibility = VISIBLE
            }

            private fun sendIsVisible(account: Account) {
                binding.send.visibility = VISIBLE
                binding.send.setOnClickListener { navigate(toSendCoins(account)) }
            }

            private fun receiveIsVisible(account: Account) {
                binding.receive.visibility = VISIBLE
                binding.receive.setOnClickListener { navigate(toReceiveCoins(account)) }
            }

            private fun descriptionOfBalance(balance: BigInteger, coin: Coin): String {
                var decimal = BigDecimal(balance)
                for (level in 0 until coin.ordinal)
                    decimal = decimal.divide(_1000)

                return resources.getQuantityString(
                    idsOfPluralsOfCoins[coin.ordinal],
                    if (decimal == BigDecimal.ONE) 1 else 10,
                    decimal.toPlainString()
                )
            }

            fun bindTo(account: Account) {
                when {
                    account is Faucet -> bindToFaucet(account)
                    account.isKey() -> bindToKey(account)
                    account.isAccessible -> bindToAccessible(account)
                    else -> bindToInaccessible(account)
                }
            }
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
            return ViewHolder(
                AccountCardBinding.inflate(
                    LayoutInflater.from(viewGroup.context),
                    viewGroup,
                    false
                )
            )
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
            viewHolder.bindTo(accounts[i])
        }

        override fun getItemCount(): Int {
            return accounts.size
        }
    }
}