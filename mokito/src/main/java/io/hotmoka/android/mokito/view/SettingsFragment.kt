package io.hotmoka.android.mokito.view

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.model.Accounts
import io.hotmoka.beans.updates.Update
import io.hotmoka.beans.values.StorageReference

class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener, View {

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

    override fun onManifestChanged(manifest: StorageReference) {
    }

    override fun onStateChanged(reference: StorageReference, state: Array<Update>) {
    }

    override fun onAccountsChanged(accounts: Accounts) {
    }

    override fun askForConfirmationOfDeleting(account: Account) {
    }

    override fun askForEdit(account: Account) {
    }

    private fun clearSubtitle() {
        context.supportActionBar!!.subtitle = ""
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        findPreference<Preference>("url")?.onPreferenceChangeListener = this
        findPreference<Preference>("webSockets")?.onPreferenceChangeListener = this
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        if (preference.key == "url" || preference.key == "webSockets")
            context.applicationContext.controller.requestReconnect()

        return true
    }
}