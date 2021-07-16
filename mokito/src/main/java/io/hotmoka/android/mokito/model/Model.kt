package io.hotmoka.android.mokito.model

import io.hotmoka.beans.updates.Update
import io.hotmoka.beans.values.StorageReference

class Model {

    /**
     * A map from a storage reference to its state, if has been already
     * computed for that storage reference. Since this is just a cache of
     * information stored in the remote Hotmoka node, this information might
     * be out of date or missing and the user will have the possibility to request a reload.
     */
    private val states: MutableMap<StorageReference, Array<Update>> = HashMap()

    /**
     * Clears all information contained in this model.
     */
    fun clear() {
        states.clear()
    }

    /**
     * Yields the state of the given object, if it has been already
     * fetched from the node. This is just a cache of
     * information stored in the remote Hotmoka node, hence this information might
     * be out of date or missing.
     *
     * @param reference the reference of the object
     * @return the state of {@code reference}
     */
    fun getState(reference: StorageReference): Array<Update>? {
        return states[reference]
    }

    /**
     * Puts in cache the state of the given object.
     *
     * @param reference the reference of the object
     * @param state the state of the object
     */
    fun setState(reference: StorageReference, state: Array<Update>) {
        states[reference] = state
    }
}