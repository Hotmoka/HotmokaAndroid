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
import android.view.MenuInflater
import androidx.appcompat.widget.PopupMenu

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
        if (accounts.getAll().allMatch { account -> account is Faucet }) {
            // if there are no accounts but the faucet, we create a quick
            // link for the creation of a new key, as a hint to the user
            binding.createNewKey.visibility = VISIBLE
            binding.createNewKey.setOnClickListener { CreateKeyDialogFragment.show(this) }
        }
        else
            binding.createNewKey.visibility = GONE

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
                setCardBackground(R.color.key)
                binding.name.text = account.name
                binding.reference.text = getString(R.string.waiting_for_payment_to_this_key)
                binding.balance.visibility = GONE
                binding.menuButton.setOnClickListener { createMenuForKey(account) }
            }

            private fun createMenuForKey(account: Account) {
                val popup = PopupMenu(context, binding.menuButton)
                popup.menuInflater.inflate(R.menu.key_actions, popup.menu)
                popup.setOnMenuItemClickListener{ item -> clickListenerForKey(item, account) }
                popup.show()
            }

            private fun clickListenerForKey(item: MenuItem, account: Account): Boolean {
                return when (item.itemId) {
                    R.id.action_receive_into_key -> {
                        navigate(toReceiveCoins(account))
                        true
                    }
                    R.id.action_show_or_edit_key -> {
                        navigate(toShowAccount(account))
                        true
                    }
                    R.id.action_delete_key -> {
                        DeleteAccountConfirmationDialogFragment.show(this@AccountsFragment, account)
                        true
                    }
                    else -> false
                }
            }

            /**
             * Binds the view holder to an account that is actually the faucet of the node.
             */
            private fun bindToFaucet(account: Account) {
                setCardBackground(R.color.faucet)
                binding.name.text = account.name
                binding.reference.text = account.reference.toString()
                balanceIsVisible(account)
                binding.menuButton.setOnClickListener { createMenuForFaucet(account) }
            }

            private fun createMenuForFaucet(account: Account) {
                val popup = PopupMenu(context, binding.menuButton)
                popup.menuInflater.inflate(R.menu.faucet_actions, popup.menu)
                popup.setOnMenuItemClickListener{ item -> clickListenerForFaucet(item, account) }
                popup.show()
            }

            private fun clickListenerForFaucet(item: MenuItem, account: Account): Boolean {
                return when (item.itemId) {
                    R.id.action_new_account_from_faucet -> {
                        navigate(toCreateNewAccount(account))
                        true
                    }
                    R.id.action_receive_into_faucet -> {
                        navigate(toReceiveCoins(account))
                        true
                    }
                    R.id.action_send_from_faucet -> {
                        navigate(toSendCoins(account))
                        true
                    }
                    else -> false
                }
            }

            /**
             * Binds the view holder to an account that is not the faucet and is accessible.
             */
            private fun bindToAccessible(account: Account) {
                setCardBackground(R.color.accessible_account)
                binding.name.text = account.name
                binding.reference.text = account.reference.toString()
                balanceIsVisible(account)
                binding.menuButton.setOnClickListener { createMenuForAccessible(account) }
            }

            private fun createMenuForAccessible(account: Account) {
                val popup = PopupMenu(context, binding.menuButton)
                popup.menuInflater.inflate(R.menu.accessible_account_actions, popup.menu)
                popup.setOnMenuItemClickListener{ item -> clickListenerForAccessible(item, account) }
                popup.show()
            }

            private fun clickListenerForAccessible(item: MenuItem, account: Account): Boolean {
                return when (item.itemId) {
                    R.id.action_new_account_from_account -> {
                        navigate(toCreateNewAccount(account))
                        true
                    }
                    R.id.action_receive_into_account -> {
                        navigate(toReceiveCoins(account))
                        true
                    }
                    R.id.action_send_from_account -> {
                        navigate(toSendCoins(account))
                        true
                    }
                    R.id.action_show_or_edit_account -> {
                        navigate(toShowAccount(account))
                        true
                    }
                    R.id.action_delete_account -> {
                        DeleteAccountConfirmationDialogFragment.show(this@AccountsFragment, account)
                        true
                    }
                    else -> false
                }
            }

            /**
             * Binds the view holder to an account that is not the faucet and is inaccessible.
             */
            private fun bindToInaccessible(account: Account) {
                setCardBackground(R.color.inaccessible_account)
                binding.name.text = account.name
                binding.reference.text = account.reference.toString()
                binding.balance.text = resources.getString(R.string.account_not_accessible)
                binding.balance.visibility = GONE
                binding.menuButton.setOnClickListener { createMenuForInaccessible(account) }
            }

            private fun createMenuForInaccessible(account: Account) {
                val popup = PopupMenu(context, binding.menuButton)
                popup.menuInflater.inflate(R.menu.inaccessible_account_actions, popup.menu)
                popup.setOnMenuItemClickListener{ item -> clickListenerForInaccessible(item, account) }
                popup.show()
            }

            private fun clickListenerForInaccessible(item: MenuItem, account: Account): Boolean {
                return when (item.itemId) {
                    R.id.action_show_or_edit_account -> {
                        navigate(toShowAccount(account))
                        true
                    }
                    R.id.action_delete_account -> {
                        DeleteAccountConfirmationDialogFragment.show(this@AccountsFragment, account)
                        true
                    }
                    else -> false
                }
            }

            private fun setCardBackground(color: Int) {
                binding.card.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, color))
            }

            private fun balanceIsVisible(account: Account) {
                binding.balance.text = descriptionOfBalance(account.balance, account.coin)
                binding.balance.visibility = VISIBLE
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