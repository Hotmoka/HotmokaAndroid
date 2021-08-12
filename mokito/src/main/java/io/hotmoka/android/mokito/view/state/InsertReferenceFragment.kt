package io.hotmoka.android.mokito.view.state

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.hotmoka.android.mokito.databinding.FragmentInsertReferenceBinding
import io.hotmoka.android.mokito.view.AbstractFragment

/**
 * A fragment used to insert the storage reference of an object
 * that exists in the store of a Hotmoka node.
 */
class InsertReferenceFragment : AbstractFragment<FragmentInsertReferenceBinding>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setBinding(FragmentInsertReferenceBinding.inflate(inflater, container, false))

        binding.showState.setOnClickListener {
            val reference = validateStorageReference(binding.reference.text.toString())
            reference?.let {
                findNavController().navigate(
                    InsertReferenceFragmentDirections.actionInsertReferenceToShowState(it)
                )
            }
        }

        return binding.root
    }
}