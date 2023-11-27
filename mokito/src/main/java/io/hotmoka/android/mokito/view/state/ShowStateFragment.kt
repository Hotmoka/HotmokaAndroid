package io.hotmoka.android.mokito.view.state

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.databinding.FragmentShowStateBinding
import io.hotmoka.android.mokito.databinding.UpdateCardBinding
import io.hotmoka.android.mokito.view.AbstractFragment
import io.hotmoka.android.mokito.view.state.ShowStateFragmentDirections.toShowState
import io.hotmoka.beans.updates.*
import io.hotmoka.beans.values.StorageReference

/**
 * A fragment used to show the state of an object in the store of the Hotmoka node.
 */
open class ShowStateFragment : AbstractFragment<FragmentShowStateBinding>() {
    private var reference: StorageReference? = null
    private lateinit var adapter: RecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        // the arguments might be missing if this fragment is actually a ShowManifestFragment;
        // in that case, reference will be set later, when the state of the manifest will be ready
        if (arguments != null)
            reference = ShowStateFragmentArgs.fromBundle(requireArguments()).reference
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setBinding(FragmentShowStateBinding.inflate(inflater, container, false))
        adapter = RecyclerAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
        return binding.root
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.show_state, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_reload)
            reference?.let {
                getController().requestStateOf(it)
                return true
            }

        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        showOrRequestStateOf(reference)
    }

    protected open fun showOrRequestStateOf(reference: StorageReference?) {
        if (reference == null) {
            val manifest = getModel().getManifest()
            if (manifest == null)
                getController().requestStateOfManifest()
            else
                showOrRequestStateOf(manifest)
        }
        else {
            val state = getModel().getState(reference)
            if (state != null)
                onStateChanged(reference, state)
            else
                getController().requestStateOf(reference)
        }
    }

    override fun onStateChanged(reference: StorageReference, state: Array<Update>) {
        if (this.reference == null && reference == getModel().getManifest())
            this.reference = reference

        if (reference == this.reference) {
            setSubtitle(reference.toString())
            adapter.setUpdates(state)
        }
    }

    private inner class UpdateComparator(val tag: ClassTag) : Comparator<Update> {
        override fun compare(update1: Update, update2: Update): Int {
            if (update1 is ClassTag)
                return -1 // there is at most a ClassTag in the state
            else if (update2 is ClassTag)
                return 1  // there is at most a ClassTag in the state

            val field1IsInherited = (update1 as UpdateOfField).field.definingClass != tag.clazz
            val field2IsInherited = (update2 as UpdateOfField).field.definingClass != tag.clazz

            return if (!field1IsInherited && field2IsInherited)
                -1
            else if (field1IsInherited && !field2IsInherited)
                1
            else
                update1.compareTo(update2)
        }
    }

    private inner class RecyclerAdapter: RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {
        private var tag: ClassTag? = null
        private var state = emptyArray<Update>()

        @SuppressLint("NotifyDataSetChanged")
        fun setUpdates(state: Array<Update>) {
            try {
                val tag = state.filterIsInstance<ClassTag>().first()
                state.sortWith(UpdateComparator(tag))
                this.state = state
                this.tag = tag
                notifyDataSetChanged()
            }
            catch (e: NoSuchElementException) {
                notifyUser(getString(R.string.missing_class_tag))
            }
        }

        private inner class ViewHolder(val binding: UpdateCardBinding) : RecyclerView.ViewHolder(binding.root) {

            fun bindToClassTag(update: ClassTag) {
                binding.description.text = getString(R.string.class_description, update.clazz.toString())
                binding.value.text = getString(R.string.jar_description, update.jar.toString())
                binding.card.isClickable = false
                binding.card.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.class_tag))
                binding.arrow.visibility = View.GONE
            }

            fun bindToFieldInTheSameClass(update: UpdateOfField) {
                binding.description.text = getString(R.string.field_description,
                    update.field.name, update.field.type.toString())
                binding.card.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.field_in_class))
                bindToFieldGeneric(update)
            }

            fun bindToFieldInheritedFromSuperclass(update: UpdateOfField) {
                binding.description.text = getString(R.string.field_inherited_description,
                    update.field.name, update.field.type.toString(), update.field.definingClass.toString())
                binding.card.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.field_inherited))
                bindToFieldGeneric(update)
            }

            private fun bindToFieldGeneric(update: UpdateOfField) {
                binding.value.text = valueToPrint(update)

                if (update is UpdateOfStorage) {
                    binding.arrow.visibility = View.VISIBLE
                    binding.arrow.setOnClickListener { navigate(toShowState(update.value)) }
                }
                else
                    binding.arrow.visibility = View.GONE
            }

            private fun valueToPrint(update: UpdateOfField): String {
                return if (update is UpdateOfString)
                    "\"${update.value}\""
                else
                    update.value.toString()
            }
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
            return ViewHolder(
                UpdateCardBinding.inflate(
                    LayoutInflater.from(viewGroup.context),
                    viewGroup,
                    false
                )
            )
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
            val update = state[i]

            when {
                update is ClassTag -> viewHolder.bindToClassTag(update)
                (update as UpdateOfField).field.definingClass == tag?.clazz
                    -> viewHolder.bindToFieldInTheSameClass(update)
                else -> viewHolder.bindToFieldInheritedFromSuperclass(update)
            }
        }

        override fun getItemCount(): Int {
            return state.size
        }
    }
}