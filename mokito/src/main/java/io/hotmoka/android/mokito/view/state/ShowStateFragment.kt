package io.hotmoka.android.mokito.view.state

import android.os.Bundle
import android.view.*
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.R.*
import io.hotmoka.android.mokito.databinding.FragmentShowStateBinding
import io.hotmoka.android.mokito.view.AbstractFragment
import io.hotmoka.beans.updates.*
import io.hotmoka.beans.values.StorageReference
import java.lang.IllegalStateException
import kotlin.Comparator

/**
 * A fragment used to show the state of an object from its storage reference.
 */
open class ShowStateFragment : AbstractFragment() {
    private var _binding: FragmentShowStateBinding? = null
    private val binding get() = _binding!!
    private var adapter: RecyclerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentShowStateBinding.inflate(inflater, container, false)
        adapter = RecyclerAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.show_state, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_reload) {
            getShownReference()?.let { getController().requestStateOf(it) }
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        showOrRequestState()
    }

    protected open fun showOrRequestState() {
        getShownReference()?.let { showOrRequestStateOf(it) }
    }

    protected open fun getShownReference(): StorageReference? {
        arguments?.let {
            val args = ShowStateFragmentArgs.fromBundle(it)
            return StorageReference(args.reference)
        }

        return null
    }

    protected fun showOrRequestStateOf(reference: StorageReference) {
        val state = getModel().getState(reference)
        if (state != null)
            onStateChanged(reference, state)
        else
            getController().requestStateOf(reference)
    }

    override fun onStateChanged(reference: StorageReference, state: Array<Update>) {
        if (reference == getShownReference()) {
            setSubtitle(reference.toString())
            adapter?.setUpdates(state)
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

            return if (!field1IsInherited && field2IsInherited)
                -1
            else if (field1IsInherited && !field2IsInherited)
                1
            else
                update1.compareTo(update2)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class RecyclerAdapter: RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {
        private var tag: ClassTag? = null
        private var state = emptyArray<Update>()

        fun setUpdates(state: Array<Update>) {
            try {
                val tag = state.filterIsInstance<ClassTag>().first()
                state.sortWith(UpdateComparator(tag))
                this.state = state
                this.tag = tag
                notifyDataSetChanged()
            }
            catch (e: NoSuchElementException) {
                throw IllegalStateException("The server answered with a state missing a class tag")
            }
        }

        private inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val itemDescription: TextView = itemView.findViewById(R.id.item_description)
            val itemValue: TextView = itemView.findViewById(R.id.item_value)
            val itemArrow: ImageView = itemView.findViewById(R.id.item_arrow)
            val card: CardView = itemView.findViewById(R.id.update_card_view)
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
            val v = LayoutInflater.from(viewGroup.context)
                .inflate(layout.update_card_layout, viewGroup, false)

            return ViewHolder(v)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
            val update = state[i]

            if (update is ClassTag) {
                // class tag
                viewHolder.itemDescription.text = resources.getString(string.class_description, update.clazz.toString())
                viewHolder.itemValue.text = resources.getString(string.jar_description, update.jar.toString())
                viewHolder.card.isClickable = false
                viewHolder.card.setCardBackgroundColor(0xFFFFFFFF.toInt())
                viewHolder.itemArrow.visibility = View.GONE
            }
            else {
                val field = (update as UpdateOfField).field
                if (field.definingClass == tag?.clazz) {
                    // field in the same class
                    viewHolder.itemDescription.text = resources.getString(string.field_description,
                        field.name, field.type.toString())
                    viewHolder.itemValue.text = valueToPrint(update)
                    viewHolder.card.setCardBackgroundColor(0xFFE0E0E0.toInt())

                    if (update is UpdateOfStorage) {
                        viewHolder.card.isClickable = true
                        viewHolder.itemArrow.visibility = View.VISIBLE
                        viewHolder.card.setOnClickListener {
                            val action = ShowStateFragmentDirections.actionShowStateSelf()
                            action.reference = update.value.toString()
                            Navigation.findNavController(it).navigate(action)
                        }
                    }
                    else {
                        viewHolder.card.isClickable = false
                        viewHolder.itemArrow.visibility = View.GONE
                    }
                }
                else {
                    // field inherited from a superclass
                    viewHolder.itemDescription.text = resources.getString(string.field_inherited_description,
                        field.name, field.type.toString(), field.definingClass.toString())
                    viewHolder.itemValue.text = valueToPrint(update)
                    viewHolder.card.setCardBackgroundColor(0xFFC0C0C0.toInt())

                    if (update is UpdateOfStorage) {
                        viewHolder.card.isClickable = true
                        viewHolder.itemArrow.visibility = View.VISIBLE
                        viewHolder.card.setOnClickListener {
                            val action = ShowStateFragmentDirections.actionShowStateSelf()
                            action.reference = update.value.toString()
                            Navigation.findNavController(it).navigate(action)
                        }
                    }
                    else {
                        viewHolder.card.isClickable = false
                        viewHolder.itemArrow.visibility = View.GONE
                    }
                }
            }
        }

        private fun valueToPrint(update: UpdateOfField): String {
            return if (update is UpdateOfString)
                "\"${update.value}\""
            else
                update.value.toString()
        }

        override fun getItemCount(): Int {
            return state.size
        }
    }
}