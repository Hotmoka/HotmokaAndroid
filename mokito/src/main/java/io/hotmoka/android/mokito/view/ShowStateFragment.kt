package io.hotmoka.android.mokito.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.hotmoka.android.mokito.databinding.FragmentShowStateBinding

/**
 * A fragment used to show the state of an object from its storage reference.
 */
class ShowStateFragment : AbstractFragment() {
    private var _binding: FragmentShowStateBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentShowStateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        arguments?.let {
            val args = ShowStateFragmentArgs.fromBundle(it)
            Log.d("ShowStateFragment", args.reference)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}