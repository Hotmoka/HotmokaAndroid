package io.hotmoka.android.mokito.view.manifest

import io.hotmoka.android.mokito.view.state.ShowStateFragment
import io.hotmoka.beans.values.StorageReference

/**
 * A fragment used to show the state of the manifest of a Hotmoka node.
 */
class ShowManifestFragment : ShowStateFragment() {

    override fun showOrRequestState() {
        val manifest = getShownReference()
        if (manifest != null)
            showOrRequestStateOf(manifest)
        else
            getController().requestStateOfManifest()
    }

    override fun getShownReference(): StorageReference? {
        return getModel().getManifest()
    }
}