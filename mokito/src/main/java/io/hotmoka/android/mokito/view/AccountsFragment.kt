package io.hotmoka.android.mokito.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.hotmoka.android.mokito.databinding.FragmentAccountsBinding
import io.hotmoka.beans.values.StorageReference

class AccountsFragment : AbstractFragment() {
    private var _binding: FragmentAccountsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAccountsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        getController().requestNewAccountFromFaucet()
    }

    override fun onAccountCreated(account: StorageReference) {
        binding.newAccount.text = account.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}