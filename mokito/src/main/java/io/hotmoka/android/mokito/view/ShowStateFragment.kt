package io.hotmoka.android.mokito.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.hotmoka.android.mokito.databinding.FragmentShowStateBinding
import io.hotmoka.beans.updates.Update
import io.hotmoka.beans.values.StorageReference

/**
 * A fragment used to show the state of an object from its storage reference.
 */
open class ShowStateFragment : AbstractFragment() {
    private var _binding: FragmentShowStateBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentShowStateBinding.inflate(inflater, container, false)
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
        if (isRequestedObject(reference))
            setSubtitle(reference.toString())
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
        setSubtitle("");
        _binding = null
    }
}