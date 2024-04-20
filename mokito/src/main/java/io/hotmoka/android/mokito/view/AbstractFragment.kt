package io.hotmoka.android.mokito.view

import android.app.Activity
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.controller.Controller
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.model.Accounts
import io.hotmoka.android.mokito.model.Model
import io.hotmoka.android.mokito.model.OwnerTokens
import io.hotmoka.node.StorageValues
import io.hotmoka.node.api.transactions.TransactionReference
import io.hotmoka.node.api.updates.Update
import io.hotmoka.node.api.values.StorageReference
import io.hotmoka.crypto.api.BIP39Mnemonic
import io.hotmoka.crypto.Base58
import java.math.BigInteger

abstract class AbstractFragment<V: ViewBinding> : Fragment(), View {
    private var _binding: V? = null
    private var progressBar: ProgressBar? = null
    protected val binding get() = _binding!!

    companion object {
        const val TAG = "AbstractFragment"
    }

    protected fun setBinding(binding: V) {
        _binding = binding
        progressBar = binding.root.findViewById(R.id.progress_bar)
        if (!getController().isWorking())
            progressBar?.visibility = android.view.View.GONE
    }

    override fun onStart() {
        super.onStart()
        context.applicationContext.view = this
        setSubtitle("")
    }

    override fun onStop() {
        context.applicationContext.view = null
        closeKeyboard()
        super.onStop()
    }

    override fun onBackgroundStart() {
        progressBar?.visibility = android.view.View.VISIBLE
    }

    override fun onBackgroundEnd() {
        progressBar?.visibility = android.view.View.GONE
    }

    protected fun closeKeyboard() {
        val inputMethodManager: InputMethodManager =
            context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager

        if (inputMethodManager.isAcceptingText)
            context.currentFocus?.let {
                inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getContext(): Mokito {
        return super.getContext() as Mokito
    }

    protected fun navigate(where: NavDirections) {
        findNavController().navigate(where)
    }

    protected fun popBackStack() {
        findNavController().popBackStack()
    }

    protected fun setSubtitle(subtitle: String) {
        context.supportActionBar!!.subtitle = subtitle
    }

    protected fun getController(): Controller {
        return context.applicationContext.controller
    }

    protected fun getModel(): Model {
        return context.applicationContext.model
    }

    /**
     * Checks that the given string is syntactically a storage reference.
     *
     * @param s the string representation of the potential storage reference
     * @return the corresponding storage reference
     *
     */
    protected fun validateStorageReference(s: String): StorageReference {
        try {
            return StorageValues.reference(s)
        }
        catch (t: Throwable) {
            throw IllegalArgumentException(getString(R.string.storage_reference_constraints))
        }
    }

    override fun onManifestChanged(manifest: StorageReference) {
    }

    override fun onStateChanged(reference: StorageReference, state: Array<Update>) {
    }

    override fun onErc20Changed(reference: StorageReference, state: Array<OwnerTokens>) {
    }

    override fun onAccountsChanged(accounts: Accounts) {
    }

    override fun onBip39Available(account: Account, bip39: BIP39Mnemonic) {
    }

    override fun onAccountCreated(account: Account) {
        if (account.isKey())
            notifyUser(getString(R.string.key_created_toast, account.name))
        else
            notifyUser(getString(R.string.account_created_toast, account.name))
    }

    override fun onAccountImported(account: Account) {
        notifyUser(getString(R.string.account_imported_toast, account.name))
    }

    override fun onAccountDeleted(account: Account) {
        if (account.isKey())
            notifyUser(getString(R.string.key_deleted_toast, account.name))
        else
            notifyUser(getString(R.string.account_deleted_toast, account.name))
    }

    override fun onAccountReplaced(old: Account, new: Account) {
        if (old.isKey())
            notifyUser(getString(R.string.key_replaced_toast, new.name))
        else
            notifyUser(getString(R.string.account_replaced_toast, new.name))
    }

    override fun onQRScanCancelled() {
        Log.d(TAG, getString(R.string.qr_scan_cancelled))
        notifyUser(getString(R.string.qr_scan_cancelled))
    }

    override fun onQRScanAvailable(data: String) {
        Log.d(TAG, getString(R.string.qr_scan_successful))
        notifyUser(getString(R.string.qr_scan_successful))
    }

    override fun onPaymentCompleted(
        payer: Account,
        destination: StorageReference,
        publicKey: String?,
        amount: BigInteger,
        anonymous: Boolean,
        transactions: List<TransactionReference>
    ) {
        notifyUser(getString(R.string.payment_completed))
    }

    override fun notifyException(t: Throwable) {
        var t2: Throwable = t
        var cause = t2.cause
        while (cause != null) {
            t2 = cause
            cause = t2.cause
        }

        Log.d(TAG, "action failed with the following exception", t)
        Toast.makeText(context, t2.message, Toast.LENGTH_LONG).show()
    }

    override fun notifyUser(message: String) {
        Log.d(TAG, message)
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    protected fun looksLikePublicKey(s: String): Boolean {
        return try {
            return Base58.decode(s).size == 32 // ed25519 public keys are 32 bytes long
        } catch (e: java.lang.IllegalArgumentException) {
            false
        }
    }

    protected fun looksLikeStorageReference(s: String): Boolean {
        return try {
            validateStorageReference(s)
            true
        } catch (e: java.lang.IllegalArgumentException) {
            false
        }
    }
}