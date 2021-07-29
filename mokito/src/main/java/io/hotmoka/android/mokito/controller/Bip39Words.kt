package io.hotmoka.android.mokito.controller

import io.hotmoka.android.mokito.MVC
import java.io.BufferedReader
import java.io.InputStreamReader

class Bip39Words(mvc: MVC) {
    private val words: List<String>

    companion object {
        private const val fileName = "bip39_words.txt"
    }

    init {
        val reader = BufferedReader(InputStreamReader(mvc.assets.open(fileName)))
        reader.use {
            words = reader.readLines()
        }
    }

    fun getWords(): List<String> {
        return words
    }
}