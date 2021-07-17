package io.hotmoka.android.mokito.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.hotmoka.android.mokito.databinding.FragmentShowManifestBinding
import io.hotmoka.beans.updates.Update
import io.hotmoka.beans.values.StorageReference

/**
 * A fragment used to show the state of the manifest of a Hotmoka node.
 */
class ShowManifestFragment : AbstractFragment() {
    private var _binding: FragmentShowManifestBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentShowManifestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        val manifest = getModel().getManifest()
        if (manifest != null) {
            val state = getModel().getState(manifest)
            if (state != null)
                onStateChanged(manifest, state)
            else
                getController().requestStateOf(manifest)
        }
        else
            getController().requestStateOfManifest()
    }

    override fun onStateChanged(reference: StorageReference, state: Array<Update>) {
        if (reference == getModel().getManifest()) {
            setSubtitle(reference.toString())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        setSubtitle("")
        _binding = null
    }
}