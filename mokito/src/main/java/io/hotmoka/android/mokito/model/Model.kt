package io.hotmoka.android.mokito.model

import android.util.Log
import io.hotmoka.android.mokito.MVC
import io.hotmoka.node.api.updates.Update
import io.hotmoka.node.api.values.StorageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The model of the application. It holds the data manipulated by the application.
 */
class Model(private val mvc: MVC) {

    private val mainScope = CoroutineScope(Dispatchers.Main)

    /**
     * The manifest of the Hotmoka node, if any.
     */
    private var manifest: StorageReference? = null

    /**
     * The gamete of the Hotmoka node, if any.
     */
    private var gamete: StorageReference? = null

    /**
     * The accounts ledger of the Hotmoka node, if any.
     */
    private var accountsLedger: StorageReference? = null

    /**
     * The accounts of the user, if any.
     */
    private var accounts: Accounts? = null

    /**
     * A map from a storage reference to its state, if it has been already
     * computed for that storage reference. Since this is just a cache of
     * information stored in the remote Hotmoka node, this information might
     * be out of date or missing and that's why the user has the possibility to request a reload.
     */
    private val states: MutableMap<StorageReference, Array<Update>> = HashMap()

    /**
     * A map from the storage reference of an ERC20 contract to its pairs owner/amounts,
     * if it has been already computed for that storage reference. Since this is just a cache of
     * information stored in the remote Hotmoka node, this information might
     * be out of date or missing and that's why the user has the possibility to request a reload.
     */
    private val erc20OwnerTokens: MutableMap<StorageReference, Array<OwnerTokens>> = HashMap()

    /**
     * Clears all information contained in this model.
     */
    fun clear() {
        manifest = null
        gamete = null
        accounts = null
        accountsLedger = null
        states.clear()
        erc20OwnerTokens.clear()
        Log.d("Model", "cleaned everything")
    }

    fun getManifest(): StorageReference? = manifest

    fun setManifest(manifest: StorageReference) {
        this.manifest = manifest
        mainScope.launch { mvc.view?.onManifestChanged(manifest) }
    }

    fun getGamete(): StorageReference? = gamete

    fun setGamete(gamete: StorageReference) {
        this.gamete = gamete
    }

    fun getAccounts(): Accounts? = accounts

    fun setAccounts(accounts: Accounts) {
        this.accounts = accounts
        mainScope.launch { mvc.view?.onAccountsChanged(accounts) }
    }

    fun getAccountsLedger(): StorageReference? = accountsLedger

    fun setAccountsLedger(accountsLedger: StorageReference) {
        this.accountsLedger = accountsLedger
    }

    /**
     * Yields the state of the given object, if it has been already fetched from the node.
     * This is just a cache of information stored in the remote Hotmoka node, hence it might
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
        mainScope.launch { mvc.view?.onStateChanged(reference, state) }
    }

    /**
     * Yields the owner/amount pairs of the given ERC20 contract, if it has been already
     * fetched from the node. This is just a cache of information stored in the remote Hotmoka node,
     * hence it might be out of date or missing.
     *
     * @param erc20Contract the reference of the ERC20 contract
     * @return the owner/amount pairs of {@code erc20Contract}
     */
    fun getErc20OwnerTokens(erc20Contract: StorageReference): Array<OwnerTokens>? {
        return erc20OwnerTokens[erc20Contract]
    }

    /**
     * Puts in cache the state of the given ERC20 contract.
     *
     * @param erc20Contract the reference of the ERC20 contract
     * @param ownerTokens the owner/amount pairs of {@code erc20Contract}
     */
    fun setErc20OwnerTokens(erc20Contract: StorageReference, ownerTokens: Array<OwnerTokens>) {
        erc20OwnerTokens[erc20Contract] = ownerTokens
        mainScope.launch { mvc.view?.onErc20Changed(erc20Contract, ownerTokens) }
    }
}