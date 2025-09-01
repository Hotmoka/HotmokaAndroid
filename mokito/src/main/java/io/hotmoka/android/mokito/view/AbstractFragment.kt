package io.hotmoka.android.mokito.view

import android.app.Activity
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.UiThread
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
import io.hotmoka.crypto.Base58ConversionException
import java.math.BigInteger

abstract class AbstractFragment<V: ViewBinding> : Fragment(), View {
    private var _binding: V? = null
    private var progressBar: ProgressBar? = null
    protected val binding get() = _binding!!

    companion object {
        private const val TAG = "AbstractFragment"
    }

    @UiThread protected fun setBinding(binding: V) {
        _binding = binding
        progressBar = binding.root.findViewById(R.id.progress_bar)
        if (!getController().isWorking())
            progressBar?.visibility = android.view.View.GONE
    }

    @UiThread override fun onStart() {
        super.onStart()
        context.applicationContext.view = this
        setSubtitle("")
    }

    @UiThread override fun onStop() {
        context.applicationContext.view = null
        closeKeyboard()
        super.onStop()
    }

    @UiThread override fun onBackgroundStart() {
        progressBar?.visibility = android.view.View.VISIBLE
    }

    @UiThread override fun onBackgroundEnd() {
        progressBar?.visibility = android.view.View.GONE
    }

    @UiThread protected fun closeKeyboard() {
        val inputMethodManager: InputMethodManager =
            context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager

        if (inputMethodManager.isAcceptingText)
            context.currentFocus?.let {
                inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
            }
    }

    @UiThread override fun onDestroyView() {
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

    @UiThread override fun onAccountCreated(account: Account) {
        if (account.isKey()) {
            notifyUser(getString(R.string.key_created_toast, account.name))
            Log.i(TAG, "Created key ${account.name}")
        }
        else {
            notifyUser(getString(R.string.account_created_toast, account.name))
            Log.i(TAG, "Created account ${account.name}")
        }
    }

    @UiThread override fun onAccountImported(account: Account) {
        notifyUser(getString(R.string.account_imported_toast, account.name))
        Log.i(TAG, "Imported account ${account.name}")
    }

    @UiThread override fun onAccountDeleted(account: Account) {
        if (account.isKey()) {
            notifyUser(getString(R.string.key_deleted_toast, account.name))
            Log.i(TAG, "Deleted key ${account.name}")
        }
        else {
            notifyUser(getString(R.string.account_deleted_toast, account.name))
            Log.i(TAG, "Deleted account ${account.name}")
        }
    }

    @UiThread override fun onAccountReplaced(old: Account, new: Account) {
        if (old.isKey()) {
            notifyUser(getString(R.string.key_replaced_toast, new.name))
            Log.i(TAG, "Replaced key ${old.name} with ${new.name}")
        }
        else {
            notifyUser(getString(R.string.account_replaced_toast, new.name))
            Log.i(TAG, "Replaced account ${old.name} with ${new.name}")
        }
    }

    @UiThread override fun onQRScanCancelled() {
        notifyUser(getString(R.string.qr_scan_cancelled))
        Log.i(TAG, "QR scan cancelled")
    }

    @UiThread override fun onQRScanAvailable(data: String) {
        notifyUser(getString(R.string.qr_scan_successful))
        Log.i(TAG, "QR scan available")
    }

    @UiThread override fun onPaymentCompleted(
        payer: Account,
        destination: StorageReference,
        publicKey: String?,
        amount: BigInteger,
        anonymous: Boolean,
        transactions: List<TransactionReference>
    ) {
        notifyUser(getString(R.string.payment_completed))
        Log.i(TAG, "Completed payment of $amount coins from $payer to $destination")
    }

    @UiThread override fun notifyUser(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}