package io.hotmoka.android.mokito.controller

import io.hotmoka.android.mokito.MVC
import java.io.BufferedReader
import java.io.InputStreamReader

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
    fun indexOf(word: String): Int {
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
}