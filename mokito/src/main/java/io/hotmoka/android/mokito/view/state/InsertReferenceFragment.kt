package io.hotmoka.android.mokito.view.state

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.hotmoka.android.mokito.databinding.FragmentInsertReferenceBinding
import io.hotmoka.android.mokito.view.AbstractFragment
import io.hotmoka.android.mokito.view.state.InsertReferenceFragmentDirections.toShowState
import io.hotmoka.beans.values.StorageReference
import java.lang.IllegalArgumentException

/**
 * A fragment used to insert the storage reference of an object
 * that exists in the store of a Hotmoka node.
 */
class InsertReferenceFragment : AbstractFragment<FragmentInsertReferenceBinding>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setBinding(FragmentInsertReferenceBinding.inflate(inflater, container, false))

        binding.showState.setOnClickListener {
            try {
                navigate(toShowState(validateStorageReference(binding.reference.text.toString())))
            }
            catch (e: IllegalArgumentException) {
                notifyException(e)
            }
        }

        return binding.root
    }
}