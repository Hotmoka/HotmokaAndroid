package io.hotmoka.android.mokito.view.accounts

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import io.hotmoka.android.mokito.databinding.FragmentShowAccountBinding
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.view.AbstractFragment
import io.hotmoka.crypto.BIP39Words

class ShowAccountFragment : AbstractFragment<FragmentShowAccountBinding>() {
    private lateinit var account: Account

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        account = ShowAccountFragmentArgs.fromBundle(requireArguments()).account
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setBinding(FragmentShowAccountBinding.inflate(inflater, container, false))
        getController().requestBip39Words(account)
        binding.accountName.setText(account.name)
        binding.ok.setOnClickListener { editAccountIfNeeded() }
        return binding.root
    }

    private fun editAccountIfNeeded() {
        val new = account.setName(binding.accountName.text.toString())
        if (account.name != new.name)
            getController().requestReplace(account, new)

        findNavController().popBackStack()
    }

    override fun onBip39Available(account: Account, bip39: BIP39Words) {
        if (account == this.account) {
            binding.words.removeAllViews()

            var pos = 0
            var index = 0
            val wordsInRow = Array(4) { "" }
            for (word in bip39.stream()) {
                wordsInRow[pos] = word
                pos = (pos + 1) % 4
                if (pos == 0) {
                    addRow(index, wordsInRow)
                    index += 4
                }
            }

            // let us deal with a potential spare row of less than 4 words
            if (pos > 0) {
                do {
                    wordsInRow[pos] = ""
                    pos = (pos + 1) % 4
                } while (pos > 0)

                addRow(index, wordsInRow)
            }
        }
    }

    private fun addRow(index: Int, wordsInRow: Array<String>) {
        val scale = resources.displayMetrics.density
        val threeDP = (3 * scale + 0.5f).toInt()
        val row = TableRow(context)
        var cursor = index

        for (word in wordsInRow) {
            val textView = TextView(context)
            textView.text = "${++cursor}: $word"
            textView.gravity = Gravity.CENTER
            textView.setPadding(threeDP, threeDP, threeDP, threeDP)
            row.addView(textView)
        }

        binding.words.addView(row)
    }
}