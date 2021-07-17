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
class ShowStateFragment : AbstractFragment() {
    private var _binding: FragmentShowStateBinding? = null
    private val binding get() = _binding!!
    private var reference: StorageReference? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentShowStateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        arguments?.let {
            val args = ShowStateFragmentArgs.fromBundle(it)
            reference = StorageReference(args.reference)
            reference?.let {
                val state = getModel().getState(it)
                if (state != null)
                    onStateChanged(it, state)
                else
                    getController().requestStateOf(it)
            }
        }
    }

    override fun onStateChanged(reference: StorageReference, state: Array<Update>) {
        if (reference == this.reference) {
            setSubtitle(reference.toString())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        setSubtitle("");
        _binding = null
    }
}