package io.hotmoka.android.mokito.view.accounts

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import io.hotmoka.android.mokito.controller.Bip39Dictionary
import io.hotmoka.android.mokito.databinding.FragmentImportAccountBinding
import io.hotmoka.android.mokito.view.AbstractFragment

class ImportAccountFragment : AbstractFragment() {
    private var _binding: FragmentImportAccountBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewsForWord: Array<AutoCompleteTextView>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentImportAccountBinding.inflate(inflater, container, false)
        getController().requestBip39Dictionary()
        binding.importAccount.setOnClickListener { performImport() }
        viewsForWord = arrayOf(
            binding.word1, binding.word2, binding.word3, binding.word4,
            binding.word5, binding.word6, binding.word7, binding.word8,
            binding.word9, binding.word10, binding.word11, binding.word12,
            binding.word13, binding.word14, binding.word15, binding.word16,
            binding.word17, binding.word18, binding.word19, binding.word20,
            binding.word21, binding.word22, binding.word23, binding.word24,
            binding.word25, binding.word26, binding.word27, binding.word28,
            binding.word29, binding.word30, binding.word31, binding.word32,
            binding.word33, binding.word34, binding.word35, binding.word36
        )
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onBip39DictionaryAvailable(dictionary: Bip39Dictionary) {
        val adapter = ArrayAdapter<String>(context, R.layout.simple_list_item_1, dictionary.getAllWords())
        for (viewForWord in viewsForWord)
            viewForWord.setAdapter(adapter)
    }

    private fun performImport() {
        val words = viewsForWord.map { textView -> textView.text.toString() }.toTypedArray()
        getController().requestImportAccountFromBip39Words(binding.accountName.text.toString(), words)
    }
}