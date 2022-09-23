package io.hotmoka.android.mokito.view.tokens

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import android.view.View
import android.widget.ImageView
import android.widget.TextView
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

    override fun onStart() { //qui si richiede l'indirizzo del contratto
        super.onStart()
        getController().requestOwnerTokensOf(reference) //get quello che inserisco
    }

    private inner class OwnerTokens { //cosa fa: dichiara reference e amount, che sono le cose che voglio estrapolare e mettere nelle cards
        val reference: StorageReference
        val amount: BigInteger

        //COS'E' QUESTO constructor? Perchè non c'è fun davanti?
        constructor(reference: StorageReference, amount: BigInteger) { //cosa fa: "inizializza" reference e amount (regola del codice)
            this.reference = reference;
            this.amount = amount;
        }
    }

    private inner class RecyclerAdapter: RecyclerView.Adapter<ShowTokenFragment.RecyclerAdapter.ViewHolder>() {  //Metodo RecyclerView.Adapter praticamente e viewholder
        private var state = emptyArray<OwnerTokens>() //Dichiarazione di state che sarà quante cards si creeranno

        fun setOwnerTokens(state: Array<OwnerTokens>) { //costruttore
            this.state = state
            notifyDataSetChanged() //?
        }

        private inner class ViewHolder(val binding: TokenOwnerCardBinding) : RecyclerView.ViewHolder(binding.root) { //passa alla card i dati
            //DEVO ESTRAPOLARE L'INDIRIZZO DEL POSSESSORE E QUANTI TOKEN POSSIEDE
            /*ESEMPIO TEORICO
            var itemTitle: TextView
            var itemDetail: TextView

            init{
                itemTitle = itemView.findViewById(R.id.owner)
                itemDetail = itemView.findViewById(R.id.amount)
            }
            Fine ESEMPIO*/
            fun bindToClassTag(update: ClassTag) {  //Prende le info che mi servono da stampare: owner e amount. Forse incorretta
                //reference. token
                //reference. transaction
                binding.owner.text = getString(R.string.class_description, update.clazz.toString())
                binding.amount.text = getString(R.string.jar_description, update.jar.toString())
                binding.card.isClickable = false
                binding.card.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.class_tag))
            }
            private fun orderTokenOwners(update: UpdateOfField) {
            //Fare funzione che ordina per amount
            }
            private fun valueToPrint(update: UpdateOfField): String {
                //Funzione che prende il valore da stampare
                return if (update is UpdateOfString)
                    "\"${update.value}\""
                else
                    update.value.toString()
            }
        }
        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ShowTokenFragment.RecyclerAdapter.ViewHolder { //mi displaya le mie robe
            //non fare aggiunte
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
            viewHolder.binding.owner.text = ownerTokens.reference.toString() //mette l'indirizzo dell'owner
            viewHolder.binding.amount.text = ownerTokens.amount.toString() //mette quanti token possiede

        }

        override fun getItemCount(): Int { //Questo metodo mi dice quanti items sto passando al mio ViewHolder: già a posto così
            return state.size  //Ritorna il numero di items che ho
        }
    }


}
