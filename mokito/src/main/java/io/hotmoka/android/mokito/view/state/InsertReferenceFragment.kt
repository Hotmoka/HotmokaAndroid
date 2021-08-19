package io.hotmoka.android.mokito.view.state

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.hotmoka.android.mokito.databinding.FragmentInsertReferenceBinding
import io.hotmoka.android.mokito.view.AbstractFragment
import io.hotmoka.android.mokito.view.state.InsertReferenceFragmentDirections.toShowState

/**
 * A fragment used to insert the storage reference of an object
 * that exists in the store of a Hotmoka node.
 */
class InsertReferenceFragment : AbstractFragment<FragmentInsertReferenceBinding>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setBinding(FragmentInsertReferenceBinding.inflate(inflater, container, false))

        binding.showState.setOnClickListener {
            val reference = validateStorageReference(binding.reference.text.toString())
            navigate(toShowState(reference))
        }

        return binding.root
    }
}