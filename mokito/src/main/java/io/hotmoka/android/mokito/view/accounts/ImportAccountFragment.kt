package io.hotmoka.android.mokito.view.accounts

import android.R
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.navigation.fragment.findNavController
import io.hotmoka.android.mokito.controller.Bip39Dictionary
import io.hotmoka.android.mokito.databinding.FragmentImportAccountBinding
import io.hotmoka.android.mokito.view.AbstractFragment

class ImportAccountFragment : AbstractFragment() {
    private var _binding: FragmentImportAccountBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentImportAccountBinding.inflate(inflater, container, false)
        getController().requestBip39Dictionary()
        binding.dismiss.setOnClickListener { findNavController().popBackStack() }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onBip39DictionaryAvailable(dictionary: Bip39Dictionary) {
        Log.d("InsertAccount", "dictionary computed!")
        val adapter = ArrayAdapter<String>(context, R.layout.simple_list_item_1, dictionary.getAllWords())
        binding.word1.setAdapter(adapter)
        binding.word2.setAdapter(adapter)
        binding.word3.setAdapter(adapter)
        binding.word4.setAdapter(adapter)
        binding.word5.setAdapter(adapter)
        binding.word6.setAdapter(adapter)
        binding.word7.setAdapter(adapter)
        binding.word8.setAdapter(adapter)
        binding.word9.setAdapter(adapter)
        binding.word10.setAdapter(adapter)
        binding.word11.setAdapter(adapter)
        binding.word12.setAdapter(adapter)
        binding.word13.setAdapter(adapter)
        binding.word14.setAdapter(adapter)
        binding.word15.setAdapter(adapter)
        binding.word16.setAdapter(adapter)
        binding.word17.setAdapter(adapter)
        binding.word18.setAdapter(adapter)
        binding.word19.setAdapter(adapter)
        binding.word20.setAdapter(adapter)
        binding.word21.setAdapter(adapter)
        binding.word22.setAdapter(adapter)
        binding.word23.setAdapter(adapter)
        binding.word24.setAdapter(adapter)
        binding.word25.setAdapter(adapter)
        binding.word26.setAdapter(adapter)
        binding.word27.setAdapter(adapter)
        binding.word28.setAdapter(adapter)
        binding.word29.setAdapter(adapter)
        binding.word30.setAdapter(adapter)
        binding.word31.setAdapter(adapter)
        binding.word32.setAdapter(adapter)
        binding.word33.setAdapter(adapter)
        binding.word34.setAdapter(adapter)
        binding.word35.setAdapter(adapter)
        binding.word36.setAdapter(adapter)
    }
}