package io.hotmoka.android.mokito.controller

import io.hotmoka.android.mokito.MVC
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.beans.references.TransactionReference
import java.lang.IllegalArgumentException
import java.security.MessageDigest
import java.util.*

class Bip39(account: Account, mvc: MVC) {
    private val digest = MessageDigest.getInstance("SHA-256")
    private val words: MutableList<String> = mutableListOf()

    init {
        words(account, Bip39Words(mvc))
    }

    fun getWords(): List<String> {
        return words
    }

    private fun words(account: Account, dictionary: Bip39Words) {
        val reference = account.reference
        if (reference != null) {
            if (reference.progressive.signum() != 0)
                throw IllegalArgumentException("Can only translate into words accounts with progressive 0")

            words(account.getEntropy(), reference.transaction, dictionary)
        } else
            words(account.getEntropy(), dictionary)
    }

    private fun words(entropy: ByteArray, transaction: TransactionReference, dictionary: Bip39Words) {
        var data = entropy + transaction.hashAsBytes
        val sha256 = digest.digest(data)
        data = data + sha256[0] + sha256[1] // add checksum
        selectWordsFor(data, dictionary)
    }

    private fun words(entropy: ByteArray, dictionary: Bip39Words) {
        var data = entropy
        val sha256 = digest.digest(data)
        data += sha256[0] // add checksum
        selectWordsFor(data, dictionary)
    }

    private fun selectWordsFor(data: ByteArray, dictionary: Bip39Words) {
        val allWords = dictionary.getWords()

        // we transform the byte array into a sequence of bits
        val dataAsBits = BitSet.valueOf(data)

        // we take 11 bits at a time from data and use it as an index into allWords
        val last = data.size * 8 - 11
        for (pos in 0..last step 11) {
            // we select bits from pos (inclusive) to pos + 11 (exclusive)
            val window = dataAsBits.get(pos, pos + 11)
            // we interpret the selection as the index inside the dictionary
            val index = window.toLongArray()[0].toInt()
            // we add the index-th word from the dictionary
            words.add(allWords[index])
        }
    }
}