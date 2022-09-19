package io.hotmoka.android.mokito.view.tokens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.hotmoka.android.mokito.databinding.FragmentInsertTokenBinding
import io.hotmoka.android.mokito.view.AbstractFragment
import java.lang.IllegalArgumentException

class InsertTokenFragment : AbstractFragment<FragmentInsertTokenBinding>() {

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
                notifyException(e)
            }
        }

        return binding.root
    }
}
