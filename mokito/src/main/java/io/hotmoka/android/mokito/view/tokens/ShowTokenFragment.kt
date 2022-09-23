package io.hotmoka.android.mokito.view.tokens

import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.hotmoka.android.mokito.databinding.FragmentShowTokenBinding
import io.hotmoka.android.mokito.databinding.TokenOwnerCardBinding
import io.hotmoka.android.mokito.model.OwnerTokens
import io.hotmoka.android.mokito.view.AbstractFragment
import io.hotmoka.beans.values.StorageReference

/**
 * A fragment used to show the state of an object in the store of the Hotmoka node.
 */

class ShowTokenFragment : AbstractFragment<FragmentShowTokenBinding>() { //nome della classe
    private lateinit var reference: StorageReference
    private lateinit var adapter: ShowTokenFragment.RecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) { //?
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        reference = ShowTokenFragmentArgs.fromBundle(requireArguments()).reference
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View { //?
        setBinding(FragmentShowTokenBinding.inflate(inflater, container, false))
        adapter = RecyclerAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
        return binding.root
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

    override fun onErc20Changed(reference: StorageReference, ownerTokens: Array<OwnerTokens>) {
        if (reference == this.reference) {
            setSubtitle(reference.toString())
            adapter.setOwnerTokens(ownerTokens)
        }
    }

    private inner class RecyclerAdapter: RecyclerView.Adapter<ShowTokenFragment.RecyclerAdapter.ViewHolder>() {  //Metodo RecyclerView.Adapter praticamente e viewholder
        private var state = emptyArray<OwnerTokens>() //Dichiarazione di state che sarà quante cards si creeranno

        fun setOwnerTokens(state: Array<OwnerTokens>) {
            this.state = state
            notifyDataSetChanged() // it asks Android to update the recycler view
        }

        private inner class ViewHolder(val binding: TokenOwnerCardBinding) : RecyclerView.ViewHolder(binding.root) {
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ShowTokenFragment.RecyclerAdapter.ViewHolder { //mi displaya le mie robe
            return ViewHolder(
                TokenOwnerCardBinding.inflate(
                    LayoutInflater.from(viewGroup.context),
                    viewGroup,
                    false
                )
            )
        }

        override fun onBindViewHolder(viewHolder: ShowTokenFragment.RecyclerAdapter.ViewHolder, i: Int) { //updata i date sulle cards
            val ownerTokens = state[i]
            viewHolder.binding.owner.text = ownerTokens.reference.toString().substring(0, 40) + "..." // mette l'indirizzo dell'owner
            viewHolder.binding.amount.text = ownerTokens.amount.toString() // mette quanti token possiede
        }

        override fun getItemCount(): Int { //Questo metodo mi dice quanti items sto passando al mio ViewHolder: già a posto così
            return state.size  //Ritorna il numero di items che ho
        }
    }
}