package io.hotmoka.android.mokito.view

import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.model.Accounts
import io.hotmoka.beans.updates.Update
import io.hotmoka.beans.values.StorageReference
import io.hotmoka.crypto.BIP39Words

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

    /**
     * Called when the set of accounts has changed.
     *
     * @param accounts the updated set of accounts
     */
    fun onAccountsChanged(accounts: Accounts)

    /**
     * Called when a new account has been created.
     *
     * @param account the new account
     */
    fun onAccountCreated(account: Account)

    /**
     * Called when an account has been imported.
     *
     * @param account the imported account
     */
    fun onAccountImported(account: Account)

    /**
     * Called when an account has been deleted.
     *
     * @param account the deleted account
     */
    fun onAccountDeleted(account: Account)

    /**
     * Called when an account has been replaced with another.
     * For instance, its name has changed.
     *
     * @param old the old account
     * @param new the new account, that replaces {@code old}
     */
    fun onAccountReplaced(old: Account, new: Account)

    /**
     * Called when the computation of the BIP39 words for an account
     * has been completed.
     *
     * @param account the account
     * @param bip39 the words computed for {@code account}
     */
    fun onBip39Available(account: Account, bip39: BIP39Words)

    fun notifyException(t: Throwable)

    fun notifyUser(message: String)
}