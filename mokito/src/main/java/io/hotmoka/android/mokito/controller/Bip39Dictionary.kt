package io.hotmoka.android.mokito.controller

import io.hotmoka.android.mokito.MVC
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.beans.references.LocalTransactionReference
import io.hotmoka.beans.values.StorageReference
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*
import kotlin.experimental.and

class Bip39Dictionary(mvc: MVC) {
    private val words: List<String>

    companion object {
        private const val fileName = "bip39_words.txt"
    }

    init {
        val reader = BufferedReader(InputStreamReader(mvc.assets.open(fileName)))
        reader.use { words = reader.readLines() }
    }

    /**
     * Yields the word at the given index inside this dictionary.
     *
     * @param index the index of the word, starting at 0
     * @return the word
     */
    fun getWord(index: Int): String {
        return words[index]
    }

    /**
     * Yields the position of a word inside this dictionary of words.
     *
     * @param word to word to search for
     * @return the position of {@code word} inside this dictionary, starting at 0;
     *         yields a negative number if {@code word} is not contained in this dictionary
     */
    private fun indexOf(word: String): Int {
        return words.binarySearch(word)   // BIP39 words are in alphabetical order
    }

    /**
     * Yields all words in this dictionary, in their order.
     *
     * @return the words
     */
    fun getAllWords(): Array<String> {
        return words.toTypedArray()
    }

    /**
     * Yields the account reconstructed from the given mnemonic words.
     *
     * @param name the name of the account
     * @param mnemonic the 36 mnemonic words
     * @param getBalance a function that retrieves the balance of accounts
     * @return the imported account
     */
    fun getAccount(name: String, mnemonic: Array<String>, getBalance: (StorageReference) -> BigInteger): Account {
        if (mnemonic.size != 36)
            throw java.lang.IllegalArgumentException("expected 36 mnemonic words rather than ${mnemonic.size}")

        // entropy + reference + checksum make 396 bits, so we add 4 bits to complete the last byte
        val bits = BitSet(400)
        var pos = 0
        for (word in mnemonic) {
            val index = indexOf(word)
            if (index < 0)
                throw IllegalArgumentException("$word is not a valid mnemonic word")

            // every word accounts for 11 bits
            for (bit in 0..10) {
                if (index and (1 shl bit) != 0)
                    bits.set(pos)

                pos++
            }
        }

        val data = bits.toByteArray()

        // the first 16 bytes are the entropy
        val entropy = data.slice(0..15).toByteArray()

        // the next 32 bytes are the transaction reference of the account reference
        val transaction = data.slice(16..47).toByteArray()

        // the final 2 bytes are the checksum
        val checksum = data.slice(48..49).toByteArray()

        val digest = MessageDigest.getInstance("SHA-256")
        val sha256 = digest.digest(entropy + transaction)

        // we must drop the highest 4 bits of the checksum, since they are not represented
        val checksumRecomputed: ByteArray = byteArrayOf(sha256[0], sha256[1].and(0x0f.toByte()))

        if (!checksum.contentEquals(checksumRecomputed))
            throw java.lang.IllegalArgumentException("illegal mnemonic phrase: checksum mismatch")

        // the progressive is assumed to be 0 for accounts represented in BIP39
        val reference = StorageReference(LocalTransactionReference(transaction), BigInteger.ZERO)

        return Account(reference, name, entropy, getBalance(reference))
    }
}