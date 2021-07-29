package io.hotmoka.android.mokito.view.accounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.hotmoka.android.mokito.databinding.FragmentAccountCreatedBinding
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.view.AbstractFragment

class AccountCreatedFragment : AbstractFragment() {
    private var _binding: FragmentAccountCreatedBinding? = null
    private val binding get() = _binding!!
    private lateinit var newAccount: Account

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        newAccount = AccountCreatedFragmentArgs.fromBundle(requireArguments()).newAccount
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAccountCreatedBinding.inflate(inflater, container, false)
        showNewAccount()

        binding.dismiss.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showNewAccount() {
        binding.accountName.text = newAccount.name
    }
}