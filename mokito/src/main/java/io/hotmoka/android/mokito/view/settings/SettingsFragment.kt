package io.hotmoka.android.mokito.view.settings

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.model.Accounts
import io.hotmoka.android.mokito.model.OwnerTokens
import io.hotmoka.android.mokito.view.AbstractFragment
import io.hotmoka.android.mokito.view.Mokito
import io.hotmoka.android.mokito.view.View
import io.hotmoka.node.api.transactions.TransactionReference
import io.hotmoka.node.api.updates.Update
import io.hotmoka.node.api.values.StorageReference
import io.hotmoka.crypto.api.BIP39Mnemonic
import java.math.BigInteger

class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener, View {

    companion object {
        private const val TAG = "SettingsFragment"
    }

    override fun onStart() {
        super.onStart()
        context.applicationContext.view = this
        clearSubtitle()
    }

    override fun onStop() {
        context.applicationContext.view = null
        super.onStop()
    }

    override fun getContext(): Mokito {
        return super.getContext() as Mokito
    }

    override fun onAccountCreated(account: Account) {
        if (account.isKey()) {
            notifyUser(getString(R.string.key_created_toast, account.name))
            Log.i(TAG, "Created key ${account.name}")
        }
        else {
            notifyUser(getString(R.string.account_created_toast, account.name))
            Log.i(TAG, "Created account ${account.name}")
        }
    }

    override fun onAccountImported(account: Account) {
        notifyUser(getString(R.string.account_imported_toast, account.name))
        Log.i(TAG, "Imported account $account.name")
    }

    override fun onAccountDeleted(account: Account) {
        if (account.isKey()) {
            notifyUser(getString(R.string.key_deleted_toast, account.name))
            Log.i(TAG, "Deleted key $account.name")
        }
        else {
            notifyUser(getString(R.string.account_deleted_toast, account.name))
            Log.i(TAG, "Deleted account $account.name")
        }
    }

    override fun onAccountReplaced(old: Account, new: Account) {
        if (old.isKey()) {
            notifyUser(getString(R.string.key_replaced_toast, new.name))
            Log.i(TAG, "Replaced key $old.name with $new.name")
        }
        else {
            notifyUser(getString(R.string.account_replaced_toast, new.name))
            Log.i(TAG, "Replaced account $old.name with $new.name")
        }
    }

    @UiThread
    override fun onQRScanCancelled() {
        notifyUser(getString(R.string.qr_scan_cancelled))
        Log.i(TAG, "QR scan cancelled")
    }

    @UiThread
    override fun onQRScanAvailable(data: String) {
        notifyUser(getString(R.string.qr_scan_successful))
        Log.i(TAG, "QR scan available")
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
        Log.i(TAG, "Completed payment of $amount coins from $payer to $destination")
    }

    override fun notifyUser(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun clearSubtitle() {
        context.supportActionBar!!.subtitle = ""
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        findPreference<Preference>("url")?.onPreferenceChangeListener = this
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        if (preference.key == "url")
            context.applicationContext.controller.requestReconnect()

        return true
    }
}