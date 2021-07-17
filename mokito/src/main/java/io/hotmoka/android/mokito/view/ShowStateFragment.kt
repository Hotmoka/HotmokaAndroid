package io.hotmoka.android.mokito.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.hotmoka.android.mokito.R.*
import io.hotmoka.android.mokito.databinding.FragmentShowStateBinding
import io.hotmoka.beans.updates.*
import io.hotmoka.beans.values.StorageReference
import kotlin.Comparator

/**
 * A fragment used to show the state of an object from its storage reference.
 */
open class ShowStateFragment : AbstractFragment() {
    private var _binding: FragmentShowStateBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentShowStateBinding.inflate(inflater, container, false)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        showOrRequestState()
    }

    protected open fun showOrRequestState() {
        arguments?.let {
            val args = ShowStateFragmentArgs.fromBundle(it)
            showOrRequestStateOf(StorageReference(args.reference))
        }
    }

    protected fun showOrRequestStateOf(reference: StorageReference) {
        val state = getModel().getState(reference)
        if (state != null)
            onStateChanged(reference, state)
        else
            getController().requestStateOf(reference)
    }

    override fun onStateChanged(reference: StorageReference, state: Array<Update>) {
        if (isRequestedObject(reference)) {
            setSubtitle(reference.toString())
            showState(state)
        }
    }

    private inner class UpdateComparator(val tag: ClassTag) : Comparator<Update> {
        override fun compare(update1: Update, update2: Update): Int {
            if (update1 is ClassTag)
                return -1 // there is at most a ClassTag in the state
            else if (update2 is ClassTag)
                return 1  // there is at most a ClassTag in the state

            val updateOfField1 = update1 as UpdateOfField
            val updateOfField2 = update2 as UpdateOfField

            val field1IsInherited = updateOfField1.field.definingClass != tag.clazz
            val field2IsInherited = updateOfField2.field.definingClass != tag.clazz

            if (!field1IsInherited && field2IsInherited)
                return -1
            else if (field1IsInherited && !field2IsInherited)
                return 1
            else
                return update1.compareTo(update2)
        }
    }

    private fun showState(state: Array<Update>) {
        val tag = state.filterIsInstance<ClassTag>().first()
        state.sortWith(UpdateComparator(tag))
        binding.recyclerView.adapter = RecyclerAdapter(state, tag)
        binding.recyclerView.adapter?.notifyDataSetChanged()
    }

    protected open fun isRequestedObject(reference: StorageReference): Boolean {
        arguments?.let {
            val args = ShowStateFragmentArgs.fromBundle(it)
            return reference == StorageReference(args.reference);
        }

        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class RecyclerAdapter(val state: Array<Update>, val tag: ClassTag) : RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val itemDescription: TextView = itemView.findViewById(id.item_description)
            val itemValue: TextView = itemView.findViewById(id.item_value)
            val card: CardView = itemView.findViewById(id.card_view)
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
            val v = LayoutInflater.from(viewGroup.context)
                .inflate(layout.card_layout, viewGroup, false)

            return ViewHolder(v)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
            val update = state[i]

            if (update is ClassTag) {
                // class tag
                viewHolder.itemDescription.text = "class ${update.clazz}"
                viewHolder.itemValue.text = "from jar at ${update.jar}"
                viewHolder.card.isClickable = false
                viewHolder.card.setCardBackgroundColor(0xFFFF5722.toInt())
            }
            else {
                val field = (update as UpdateOfField).field
                if (field.definingClass == tag.clazz) {
                    // field in the same class
                    viewHolder.itemDescription.text = "${field.name}: ${field.type}"
                    viewHolder.itemValue.text = valueToPrint(update)
                    viewHolder.card.setCardBackgroundColor(0xFFCDDC39.toInt())

                    if (update is UpdateOfStorage) {
                        viewHolder.card.isClickable = true
                        viewHolder.card.setOnClickListener {
                            val action: ShowStateFragmentDirections.ActionShowStateSelf =
                                ShowStateFragmentDirections.actionShowStateSelf()
                            action.reference = update.value.toString()
                            Navigation.findNavController(it).navigate(action)
                        }
                    }
                    else
                        viewHolder.card.isClickable = false
                }
                else {
                    // field inherited from a superclass
                    viewHolder.itemDescription.text =
                        "${field.name}: ${field.type} (inherited from ${field.definingClass})"
                    viewHolder.itemValue.text = valueToPrint(update)
                    viewHolder.card.setCardBackgroundColor(0xFF00BCD4.toInt())

                    if (update is UpdateOfStorage) {
                        viewHolder.card.isClickable = true
                        viewHolder.card.setOnClickListener {
                            val action: ShowStateFragmentDirections.ActionShowStateSelf =
                                ShowStateFragmentDirections.actionShowStateSelf()
                            action.reference = update.value.toString()
                            Navigation.findNavController(it).navigate(action)
                        }
                    }
                    else
                        viewHolder.card.isClickable = false
                }
            }
        }

        private fun valueToPrint(update: UpdateOfField): String {
            if (update is UpdateOfString)
                return "\"${update.value}\"";
            else
                return update.value.toString();
        }

        override fun getItemCount(): Int {
            return state.size
        }
    }
}