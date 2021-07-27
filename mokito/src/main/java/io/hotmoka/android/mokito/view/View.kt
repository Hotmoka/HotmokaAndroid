package io.hotmoka.android.mokito.view

import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.model.Accounts
import io.hotmoka.beans.updates.Update
import io.hotmoka.beans.values.StorageReference

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

    fun onAccountsChanged(accounts: Accounts)

    /**
     * The view must ask to the user if she really wants to delete the given account.
     *
     * @param account the account to delete
     */
    fun askForConfirmationOfDeleting(account: Account)

    /**
     * The view must open a dialog that allows one to edit the properties of the given account.
     *
     * @param account the account to edit
     */
    fun askForEdit(account: Account)
}