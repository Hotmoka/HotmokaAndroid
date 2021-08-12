package io.hotmoka.android.mokito.view.accounts

import android.os.Bundle
import android.view.*
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.databinding.FragmentAccountsBinding
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.model.Accounts
import io.hotmoka.android.mokito.model.Faucet
import io.hotmoka.android.mokito.view.AbstractFragment
import java.math.BigInteger

class AccountsFragment : AbstractFragment<FragmentAccountsBinding>() {
    private lateinit var adapter: RecyclerAdapter

    companion object {
        @Suppress("ObjectPropertyName")
        private val _10exp21 = BigInteger.TEN.pow(21)
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
                findNavController().navigate(AccountsFragmentDirections.actionInsertAccount())
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

    override fun onAccountCreated(account: Account) {
        super.onAccountCreated(account)
        findNavController().navigate(AccountsFragmentDirections.actionShowAccount(account))
    }

    private inner class RecyclerAdapter: RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {
        private var accounts = emptyArray<Account>()

        fun setAccounts(accounts: Accounts) {
            this.accounts = accounts.getAll().toArray { i -> arrayOfNulls(i) }
            notifyDataSetChanged()
        }

        private inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val itemReference: TextView = itemView.findViewById(R.id.item_reference)
            val itemName: TextView = itemView.findViewById(R.id.item_name)
            val itemBalance: TextView = itemView.findViewById(R.id.item_balance)
            val deleteIcon: ImageView = itemView.findViewById(R.id.item_delete)
            val settingsIcon: ImageView = itemView.findViewById(R.id.item_settings)
            val newIcon: ImageView = itemView.findViewById(R.id.item_new)
            val receiveIcon: ImageView = itemView.findViewById(R.id.item_receive)
            val sendIcon: ImageView = itemView.findViewById(R.id.item_send)
            val card: CardView = itemView.findViewById(R.id.account_card_view)
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
            val v = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.account_card_layout, viewGroup, false)

            return ViewHolder(v)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
            val account = accounts[i]
            viewHolder.itemName.text = account.name
            viewHolder.itemReference.text = account.reference.toString()

            if (account.accessible) {
                val balance = account.balance
                val split = balance.divideAndRemainder(_10exp21)
                val mokas = split[0]
                val fraction = split[1]
                var fractionWithZeros = fraction.toString()
                while (fractionWithZeros.length < 21)
                    fractionWithZeros = "0$fractionWithZeros"

                viewHolder.itemBalance.text =
                    resources.getString(R.string.balance_description, mokas, fractionWithZeros)
            }
            else
                viewHolder.itemBalance.text = resources.getString(R.string.account_not_accessible)

            if (account is Faucet) {
                // the faucet cannot be edited, nor removed, nor used to send money
                viewHolder.deleteIcon.visibility = View.GONE
                viewHolder.sendIcon.visibility = View.GONE
                viewHolder.settingsIcon.visibility = View.GONE
            }
            else {
                viewHolder.deleteIcon.visibility = View.VISIBLE
                viewHolder.deleteIcon.setOnClickListener {
                    DeleteAccountConfirmationDialogFragment.show(this@AccountsFragment, account)
                }
                viewHolder.sendIcon.visibility = View.VISIBLE
                viewHolder.settingsIcon.visibility = View.VISIBLE
                viewHolder.settingsIcon.setOnClickListener {
                    findNavController().navigate(AccountsFragmentDirections.actionShowAccount(account))
                }
            }

            if (!account.accessible) {
                viewHolder.newIcon.visibility = View.GONE
                viewHolder.sendIcon.visibility = View.GONE
                viewHolder.receiveIcon.visibility = View.GONE
            }
            else {
                viewHolder.newIcon.visibility = View.VISIBLE
                viewHolder.receiveIcon.visibility = View.VISIBLE
                viewHolder.newIcon.setOnClickListener {
                    CreateAccountDialogFragment.show(this@AccountsFragment, account)
                }
            }
        }

        override fun getItemCount(): Int {
            return accounts.size
        }
    }
}