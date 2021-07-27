package io.hotmoka.android.mokito.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.databinding.FragmentAccountsBinding
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.model.Accounts
import io.hotmoka.android.mokito.model.Faucet
import java.math.BigInteger


class AccountsFragment : AbstractFragment() {
    private var _binding: FragmentAccountsBinding? = null
    private val binding get() = _binding!!
    private var adapter: RecyclerAdapter? = null
    private var dialog: AlertDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAccountsBinding.inflate(inflater, container, false)
        adapter = RecyclerAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
        return binding.root
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
        adapter?.setAccounts(accounts)
    }

    override fun askForConfirmationOfDeleting(account: Account) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.delete_question)
        builder.setMessage(getString(R.string.delete_confirmation, account.name))
        builder.setIcon(R.drawable.ic_hand)
        builder.setPositiveButton(R.string.delete) { _, _ ->
            dismissDialog()
            getController().requestConfirmedDelete(account)
        }
        builder.setNegativeButton(R.string.keep) { _, _ -> dismissDialog() }
        val dialog: AlertDialog = builder.create()
        dialog.show()
        this.dialog = dialog
    }

    private fun dismissDialog() {
        dialog?.dismiss()
        dialog = null
    }

    override fun onPause() {
        super.onPause()
        dismissDialog()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
            viewHolder.itemBalance.text = resources.getString(R.string.balance_description, account.balance.toString())

            if (account is Faucet) {
                // the faucet cannot be edited, nor removed, nor used to send money
                viewHolder.deleteIcon.visibility = View.GONE
                viewHolder.sendIcon.visibility = View.GONE
                viewHolder.settingsIcon.visibility = View.GONE
            }
            else {
                viewHolder.deleteIcon.visibility = View.VISIBLE
                viewHolder.deleteIcon.setOnClickListener { getController().requestDelete(account) }
                viewHolder.sendIcon.visibility = View.VISIBLE
                viewHolder.settingsIcon.visibility = View.VISIBLE
            }

            viewHolder.newIcon.setOnClickListener {
                getController().requestNewAccountFromFaucet("pippo", BigInteger.TEN)
            }
        }

        override fun getItemCount(): Int {
            return accounts.size
        }
    }
}