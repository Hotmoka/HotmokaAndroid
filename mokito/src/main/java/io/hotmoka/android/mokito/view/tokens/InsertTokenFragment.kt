package io.hotmoka.android.mokito.view.tokens

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.databinding.FragmentInsertTokenBinding
import io.hotmoka.android.mokito.view.AbstractFragment

class InsertTokenFragment : AbstractFragment<FragmentInsertTokenBinding>() {

    private companion object {
        private const val TAG = "InsertTokenFragment"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setBinding(FragmentInsertTokenBinding.inflate(inflater, container, false))

        binding.showToken.setOnClickListener {
            try {
                navigate(
                    InsertTokenFragmentDirections.toShowToken(
                        validateStorageReference(
                            binding.reference.text.toString()
                        )
                    )
                )
            }
            catch (e: IllegalArgumentException) {
                notifyUser(getString(R.string.storage_reference_constraints))
                Log.w(TAG, "Illegal storage reference: $e")
            }
        }

        return binding.root
    }
}
