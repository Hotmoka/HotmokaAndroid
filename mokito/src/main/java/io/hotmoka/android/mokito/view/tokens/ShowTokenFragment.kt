package io.hotmoka.android.mokito.view.tokens

import android.os.Bundle
import android.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.databinding.FragmentShowTokenBinding
import io.hotmoka.android.mokito.databinding.TokenOwnerCardBinding
import io.hotmoka.android.mokito.model.OwnerTokens
import io.hotmoka.android.mokito.view.AbstractFragment
import io.hotmoka.beans.api.values.StorageReference

/**
 * A fragment used to show the state of an ERC20 token in the store of the Hotmoka node.
 */

class ShowTokenFragment : AbstractFragment<FragmentShowTokenBinding>() {
    private lateinit var reference: StorageReference
    private lateinit var adapter: ShowTokenFragment.RecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        reference = ShowTokenFragmentArgs.fromBundle(requireArguments()).reference
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setBinding(FragmentShowTokenBinding.inflate(inflater, container, false))
        adapter = RecyclerAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
        return binding.root
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.show_token, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_reload)
            getController().requestOwnerTokensOf(reference)

        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()

        // when the fragment starts, it checks if the model contains information about
        // the required token contract; this might be because the contract was already
        // accessed previously; note that this is just an optimization to avoid
        // duplicated accesses to the contract (they are very slow)
        val ownerTokens = getModel().getErc20OwnerTokens(reference)
        if (ownerTokens == null)
            // never seen this contract before: it asks the controller to load it
            getController().requestOwnerTokensOf(reference)
        else
            // the contract has been already loaded previously: use the cached data
            onErc20Changed(reference, ownerTokens)
    }

    override fun onErc20Changed(reference: StorageReference, state: Array<OwnerTokens>) {
        if (reference == this.reference) {
            setSubtitle(reference.toString())
            adapter.setOwnerTokens(state)
        }
    }

    private inner class RecyclerAdapter: RecyclerView.Adapter<ShowTokenFragment.RecyclerAdapter.ViewHolder>() {
        private var state = emptyArray<OwnerTokens>()

        fun setOwnerTokens(state: Array<OwnerTokens>) {
            this.state = state
            notifyDataSetChanged() // require Android to update the recycler view
        }

        private inner class ViewHolder(val binding: TokenOwnerCardBinding) : RecyclerView.ViewHolder(binding.root) {
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ShowTokenFragment.RecyclerAdapter.ViewHolder {
            return ViewHolder(
                TokenOwnerCardBinding.inflate(
                    LayoutInflater.from(viewGroup.context),
                    viewGroup,
                    false
                )
            )
        }

        override fun onBindViewHolder(viewHolder: ShowTokenFragment.RecyclerAdapter.ViewHolder, i: Int) {
            val ownerTokens = state[i]
            viewHolder.binding.owner.text = ownerTokens.reference.toString().substring(0, 40) + "..."
            viewHolder.binding.amount.text = ownerTokens.amount.toString()
        }

        override fun getItemCount(): Int {
            return state.size
        }
    }
}