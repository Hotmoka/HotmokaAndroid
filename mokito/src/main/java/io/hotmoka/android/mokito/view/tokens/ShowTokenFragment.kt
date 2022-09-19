package io.hotmoka.android.mokito.view.tokens

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.databinding.FragmentShowTokenBinding
import io.hotmoka.android.mokito.databinding.TokenOwnerCardBinding
import io.hotmoka.android.mokito.databinding.UpdateCardBinding
import io.hotmoka.android.mokito.view.AbstractFragment
import io.hotmoka.android.mokito.view.state.ShowStateFragment
import io.hotmoka.android.mokito.view.state.ShowStateFragmentDirections
import io.hotmoka.android.mokito.view.tokens.ShowTokenFragmentArgs
import io.hotmoka.beans.updates.*
import io.hotmoka.beans.values.StorageReference
import java.math.BigInteger

/**
 * A fragment used to show the state of an object in the store of the Hotmoka node.
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

    override fun onStart() {
        super.onStart()
        getController().requestOwnerTokensOf(reference)
    }

    private inner class OwnerTokens {
        val reference: StorageReference
        val amount: BigInteger

        constructor(reference: StorageReference, amount: BigInteger) {
            this.reference = reference;
            this.amount = amount;
        }
    }

    private inner class RecyclerAdapter: RecyclerView.Adapter<ShowTokenFragment.RecyclerAdapter.ViewHolder>() {
        private var state = emptyArray<OwnerTokens>()

        fun setOwnerTokens(state: Array<OwnerTokens>) {
            this.state = state
            notifyDataSetChanged()
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
            viewHolder.binding.owner.text = ownerTokens.reference.toString()
            viewHolder.binding.amount.text = ownerTokens.amount.toString()
        }

        override fun getItemCount(): Int {
            return state.size
        }
    }
}
