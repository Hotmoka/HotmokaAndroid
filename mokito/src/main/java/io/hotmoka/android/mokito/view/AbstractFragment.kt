package io.hotmoka.android.mokito.view

import android.app.Activity
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.controller.Controller
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.model.Accounts
import io.hotmoka.android.mokito.model.Model
import io.hotmoka.beans.updates.Update
import io.hotmoka.beans.values.StorageReference
import io.hotmoka.crypto.BIP39Words
import io.hotmoka.crypto.Base58
import java.math.BigInteger

abstract class AbstractFragment<V: ViewBinding> : Fragment(), View {
    private var _binding: V? = null
    protected val binding get() = _binding!!

    companion object {
        const val TAG = "AbstractFragment"
    }

    protected fun setBinding(binding: V) {
        _binding = binding
    }

    override fun onStart() {
        super.onStart()
        context.applicationContext.view = this
        setSubtitle("")
    }

    override fun onStop() {
        context.applicationContext.view = null

        val inputMethodManager: InputMethodManager = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        if (inputMethodManager.isAcceptingText)
            context.currentFocus?.let {
                inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
            }

        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getContext(): Mokito {
        return super.getContext() as Mokito
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
            return StorageReference(s)
        }
        catch (t: Throwable) {
            throw IllegalArgumentException(getString(R.string.storage_reference_constraints))
        }
    }

    override fun onManifestChanged(manifest: StorageReference) {
    }

    override fun onStateChanged(reference: StorageReference, state: Array<Update>) {
    }

    override fun onAccountsChanged(accounts: Accounts) {
    }

    override fun onBip39Available(account: Account, bip39: BIP39Words) {
    }

    override fun onAccountCreated(account: Account) {
        notifyUser(getString(R.string.account_created_toast, account.name))
    }

    override fun onAccountImported(account: Account) {
        notifyUser(getString(R.string.account_imported_toast, account.name))
    }

    override fun onAccountDeleted(account: Account) {
        notifyUser(getString(R.string.account_deleted_toast, account.name))
    }

    override fun onAccountReplaced(old: Account, new: Account) {
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
        anonymous: Boolean
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