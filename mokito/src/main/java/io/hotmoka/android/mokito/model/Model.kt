package io.hotmoka.android.mokito.model

import android.util.Log
import io.hotmoka.android.mokito.MVC
import io.hotmoka.beans.api.updates.Update
import io.hotmoka.beans.api.values.StorageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Model(private val mvc: MVC) {

    private val mainScope = CoroutineScope(Dispatchers.Main)

    /**
     * The manifest of the Hotmoka node.
     */
    private var manifest: StorageReference? = null

    /**
     * The gamete of the Hotmoka node.
     */
    private var gamete: StorageReference? = null

    /**
     * The accounts ledger of the Hotmoka node.
     */
    private var accountsLedger: StorageReference? = null

    /**
     * The accounts of the user.
     */
    private var accounts: Accounts? = null

    /**
     * A map from a storage reference to its state, if has been already
     * computed for that storage reference. Since this is just a cache of
     * information stored in the remote Hotmoka node, this information might
     * be out of date or missing and the user will have the possibility to request a reload.
     */
    private val states: MutableMap<StorageReference, Array<Update>> = HashMap()

    /**
     * A map from the storage reference of an ERC20 contract to its pairs owner/amounts,
     * if has been already computed for that storage reference. Since this is just a cache of
     * information stored in the remote Hotmoka node, this information might
     * be out of date or missing and the user will have the possibility to request a reload.
     */
    private val erc20OwnerTokens: MutableMap<StorageReference, Array<OwnerTokens>> = HashMap()

    /**
     * Clears all information contained in this model.
     */
    fun clear() {
        Log.d("Model", "cleaning everything")
        manifest = null
        gamete = null
        accounts = null
        accountsLedger = null
        states.clear()
    }

    fun getManifest(): StorageReference? = manifest

    fun setManifest(manifest: StorageReference) {
        this.manifest = manifest

        mainScope.launch {
            mvc.view?.onManifestChanged(manifest)
        }
    }

    fun getGamete(): StorageReference? = gamete

    fun setGamete(gamete: StorageReference) {
        this.gamete = gamete
    }

    fun getAccounts(): Accounts? = accounts

    fun setAccounts(accounts: Accounts) {
        this.accounts = accounts

        /*mvc.openFileInput("accounts.txt").bufferedReader().useLines { lines ->
            val all = lines.fold("") { some, text ->
                "$some\n$text"
            }
            Log.d("Model", all)
        }*/

        mainScope.launch {
            mvc.view?.onAccountsChanged(accounts)
        }
    }

    fun getAccountsLedger(): StorageReference? = accountsLedger

    fun setAccountsLedger(accountsLedger: StorageReference) {
        this.accountsLedger = accountsLedger
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

        mainScope.launch {
            mvc.view?.onStateChanged(reference, state)
        }
    }

    /**
     * Yields the owner/amount pairs of the given ERC20 contract, if it has been already
     * fetched from the node. This is just a cache of
     * information stored in the remote Hotmoka node, hence this information might
     * be out of date or missing.
     *
     * @param reference the reference of the contract
     * @return the owner/amount pairs of {@code reference}
     */
    fun getErc20OwnerTokens(reference: StorageReference): Array<OwnerTokens>? {
        return erc20OwnerTokens[reference]
    }

    /**
     * Puts in cache the state of the given object.
     *
     * @param reference the reference of the object
     * @param ownerTokens the owner/amount pairs of {@code reference}
     */
    fun setErc20OwnerTokens(reference: StorageReference, ownerTokens: Array<OwnerTokens>) {
        erc20OwnerTokens[reference] = ownerTokens

        mainScope.launch {
            mvc.view?.onErc20Changed(reference, ownerTokens)
        }
    }
}