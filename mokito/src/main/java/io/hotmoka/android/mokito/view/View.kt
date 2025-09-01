package io.hotmoka.android.mokito.view

import androidx.annotation.UiThread
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.model.Accounts
import io.hotmoka.android.mokito.model.OwnerTokens
import io.hotmoka.node.api.transactions.TransactionReference
import io.hotmoka.node.api.updates.Update
import io.hotmoka.node.api.values.StorageReference
import io.hotmoka.crypto.api.BIP39Mnemonic
import java.math.BigInteger

interface View {

    /**
     * Yields the Android context of the view.
     */
    fun getContext() : Mokito

    /**
     * Called whenever a background task has started.
     */
    @UiThread
    fun onBackgroundStart() {}

    /**
     * Called on the main thread whenever a background task has completed.
     */
    @UiThread
    fun onBackgroundEnd() {}

    /**
     * Called when the manifest has been changed.
     *
     * @param manifest the new value of the manifest
     */
    @UiThread
    fun onManifestChanged(manifest: StorageReference) {}

    /**
     * Called when the state of an object has been changed.
     *
     * @param reference the reference of the object
     * @param state the state of {@code reference} that has been changed
     */
    @UiThread
    fun onStateChanged(reference: StorageReference, state: Array<Update>) {}

    /**
     * Called when the state of an ERC20 contract has been changed.
     *
     * @param reference the reference to the ERC20 contract
     * @param state the pairs of owners/amount of tokens contained in the contract
     */
    @UiThread
    fun onErc20Changed(reference: StorageReference, state: Array<OwnerTokens>) {}

    /**
     * Called when the set of accounts has changed.
     *
     * @param accounts the updated set of accounts
     */
    @UiThread
    fun onAccountsChanged(accounts: Accounts) {}

    /**
     * Called when a new account has been created.
     *
     * @param account the new account
     */
    @UiThread
    fun onAccountCreated(account: Account)

    /**
     * Called when an account has been imported.
     *
     * @param account the imported account
     */
    @UiThread
    fun onAccountImported(account: Account)

    /**
     * Called when an account has been deleted.
     *
     * @param account the deleted account
     */
    @UiThread
    fun onAccountDeleted(account: Account)

    /**
     * Called when an account has been replaced with another.
     * For instance, its name has changed.
     *
     * @param old the old account
     * @param new the new account, that replaces {@code old}
     */
    @UiThread
    fun onAccountReplaced(old: Account, new: Account)

    /**
     * Called when the computation of the BIP39 words for an account
     * has been completed.
     *
     * @param account the account
     * @param bip39 the words computed for {@code account}
     */
    @UiThread
    fun onBip39Available(account: Account, bip39: BIP39Mnemonic) {}

    /**
     * Called when a QR code scan has been cancelled by the user.
     */
    @UiThread
    fun onQRScanCancelled()

    /**
     * Called when a QR code has been successfully performed.
     *
     * @param data the data read from the QR code
     */
    @UiThread
    fun onQRScanAvailable(data: String)

    /**
     * Called when a payment has been executed, on behalf of a paying account.
     *
     * @param payer the paying account
     * @param destination the recipient of the payment
     * @param publicKey the public key that has received the payment, if the payment was into a key
     * @param amount the amount of coins transferred to {@code destination}
     * @param anonymous true if and only if the transfer was an anonymous transfer to a key
     * @param transactions the transactions that have been used to perform the payment
     */
    @UiThread
    fun onPaymentCompleted(
        payer: Account,
        destination: StorageReference,
        publicKey: String?,
        amount: BigInteger,
        anonymous: Boolean,
        transactions: List<TransactionReference>
    )

    @UiThread
    fun notifyUser(message: String)
}