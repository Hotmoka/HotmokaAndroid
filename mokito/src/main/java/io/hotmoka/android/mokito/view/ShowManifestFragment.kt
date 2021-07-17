package io.hotmoka.android.mokito.view

import io.hotmoka.beans.values.StorageReference

/**
 * A fragment used to show the state of the manifest of a Hotmoka node.
 */
class ShowManifestFragment : ShowStateFragment() {

    override fun showOrRequestState() {
        val manifest = getModel().getManifest()
        if (manifest != null)
            showOrRequestStateOf(manifest)
        else
            getController().requestStateOfManifest()
    }

    override fun isRequestedObject(reference: StorageReference): Boolean {
        return reference == getModel().getManifest()
    }
}