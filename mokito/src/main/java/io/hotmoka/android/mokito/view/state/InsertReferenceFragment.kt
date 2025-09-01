package io.hotmoka.android.mokito.view.state

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.databinding.FragmentInsertReferenceBinding
import io.hotmoka.android.mokito.view.AbstractFragment
import io.hotmoka.android.mokito.view.state.InsertReferenceFragmentDirections.toShowState
import io.hotmoka.node.StorageValues

/**
 * A fragment used to insert the storage reference of an object
 * that exists in the store of a Hotmoka node.
 */
class InsertReferenceFragment : AbstractFragment<FragmentInsertReferenceBinding>() {

    companion object {
        const val TAG = "InsertReferenceFragment"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setBinding(FragmentInsertReferenceBinding.inflate(inflater, container, false))

        binding.showState.setOnClickListener {
            try {
                navigate(toShowState(StorageValues.reference(binding.reference.text.toString())))
            }
            catch (e: IllegalArgumentException) {
                notifyUser(getString(R.string.storage_reference_constraints))
                Log.w(TAG, "Illegal storage reference: ${e.message}")
            }
        }

        return binding.root
    }
}