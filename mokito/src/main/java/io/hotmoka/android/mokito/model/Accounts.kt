package io.hotmoka.android.mokito.model

import android.content.Context
import android.util.Log
import android.util.Xml
import io.hotmoka.android.mokito.MVC
import io.hotmoka.beans.values.StorageReference
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.IllegalStateException
import java.math.BigInteger
import java.util.*

class Accounts(mvc: MVC, faucet: StorageReference?, getBalance: (StorageReference) -> BigInteger) {

    /**
     * The name of the file where the accounts are stored, in the internal storage of the app.
     */
    private val accountsFilename = "accounts.txt"

    /**
     * The accounts in this container, bound to their storage reference
     * for simplifying their look-up.
     */
    private val accounts = HashMap<StorageReference, Account>()

    init {
        try {
            mvc.openFileInput(accountsFilename).use {
                val parser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(it, null)
                parser.nextTag()
                readAccounts(parser, getBalance)
                faucet?.let {
                    add(Faucet(faucet, getBalance(faucet)))
                }
            }
        }
        catch (e: FileNotFoundException) {
            // this is fine: initially the file of the accounts is missing
            Log.d("Accounts", "no $accountsFilename")
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readAccounts(parser: XmlPullParser, getBalance: (StorageReference) -> BigInteger) {
        parser.require(XmlPullParser.START_TAG, null, "accounts")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG)
                continue

            // starts by looking for the account tag
            if (parser.name == "account")
                add(Account(parser, getBalance))
            else
                skip(parser)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    fun add(account: Account) {
        if (accounts.containsKey(account.reference))
            throw IllegalStateException("account already existing")

        accounts[account.reference] = account
    }

    fun getAll(): Collection<Account> {
        return accounts.values
    }

    fun writeIntoInternalStorage(context: Context) {
        //val old = File(context.filesDir, accountsFilename)
        //old.delete()

        context.openFileOutput(accountsFilename, Context.MODE_PRIVATE).use { file ->
            val serializer = Xml.newSerializer()
            serializer.setOutput(file, "UTF-8")
            serializer.startDocument(null, true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

            serializer.startTag(null, "accounts")
            accounts.values.forEach { it.writeWith(serializer) }
            serializer.endTag(null, "accounts")

            serializer.endDocument()
            serializer.flush()
        }
    }
}