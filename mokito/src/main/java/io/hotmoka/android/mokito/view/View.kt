package io.hotmoka.android.mokito.view

import io.hotmoka.beans.updates.Update
import io.hotmoka.beans.values.StorageReference
import java.util.stream.Stream

interface View {

    /**
     * Yields the Android context of the view.
     */
    fun getContext() : Mokito

    /**
     * Called on the main thread when the manifest has been changed.
     *
     * @param manifest the new value of the manifest
     */
    fun onManifestChanged(manifest: StorageReference)

    /**
     * Called on the main thread when the state of an object has been changed.
     *
     * @param reference the reference of the object
     * @param state the state of {@code reference} that has been changed
     */
    fun onStateChanged(reference: StorageReference, state: Array<Update>)
}