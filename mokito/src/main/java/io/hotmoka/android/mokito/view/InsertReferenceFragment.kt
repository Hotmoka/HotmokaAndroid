package io.hotmoka.android.mokito.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import io.hotmoka.android.mokito.databinding.FragmentInsertReferenceBinding
import io.hotmoka.beans.values.StorageReference

/**
 * A fragment used to insert the storage reference of an object
 * that exists in the store of a Hotmoka node.
 */
class InsertReferenceFragment : AbstractFragment() {
    private var _binding: FragmentInsertReferenceBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInsertReferenceBinding.inflate(inflater, container, false)

        binding.showState.setOnClickListener {
            val input = binding.reference.text.toString()

            // let us validate the user input
            try {
                StorageReference(input)
            }
            catch (t: Throwable) {
                notifyProblem("A storage reference should consist of 64 hex digits followed by # and by a progressive number")
                return@setOnClickListener
            }

            val action: InsertReferenceFragmentDirections.ActionInsertReferenceToShowState =
                InsertReferenceFragmentDirections.actionInsertReferenceToShowState()

            action.reference = input
            Navigation.findNavController(it).navigate(action)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}