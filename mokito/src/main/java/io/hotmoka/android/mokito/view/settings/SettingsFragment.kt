package io.hotmoka.android.mokito.view.settings

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.view.Mokito
import io.hotmoka.android.mokito.view.View
import io.hotmoka.node.api.transactions.TransactionReference
import io.hotmoka.node.api.values.StorageReference
import java.math.BigInteger

class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener, View {

    companion object {
        private const val TAG = "SettingsFragment"
    }

    @UiThread override fun onStart() {
        super.onStart()
        context.applicationContext.view = this
        context.supportActionBar!!.subtitle = ""
    }

    @UiThread override fun onStop() {
        context.applicationContext.view = null
        super.onStop()
    }

    override fun getContext(): Mokito {
        return super.getContext() as Mokito
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
        Log.i(TAG, "Imported account $account.name")
    }

    @UiThread override fun onAccountDeleted(account: Account) {
        if (account.isKey()) {
            notifyUser(getString(R.string.key_deleted_toast, account.name))
            Log.i(TAG, "Deleted key $account.name")
        }
        else {
            notifyUser(getString(R.string.account_deleted_toast, account.name))
            Log.i(TAG, "Deleted account $account.name")
        }
    }

    @UiThread override fun onAccountReplaced(old: Account, new: Account) {
        if (old.isKey()) {
            notifyUser(getString(R.string.key_replaced_toast, new.name))
            Log.i(TAG, "Replaced key $old.name with $new.name")
        }
        else {
            notifyUser(getString(R.string.account_replaced_toast, new.name))
            Log.i(TAG, "Replaced account $old.name with $new.name")
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

    @UiThread override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        findPreference<Preference>("url")?.onPreferenceChangeListener = this
    }

    @UiThread override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        if (preference.key == "url") {
            context.applicationContext.controller.requestReconnect()
            return true
        }
        else
            return false
    }
}