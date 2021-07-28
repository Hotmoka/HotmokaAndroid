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
import java.util.stream.Stream

class Accounts(mvc: MVC, faucet: StorageReference?, maxFaucet: BigInteger, getBalance: (StorageReference) -> BigInteger) {

    /**
     * The name of the file where the accounts are stored, in the internal storage of the app.
     */
    private val accountsFilename = "accounts.txt"

    /**
     * The accounts in this container.
     */
    private val accounts = TreeSet<Account>()

    init {
        try {
            mvc.openFileInput(accountsFilename).use {
                val parser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(it, null)
                parser.nextTag()
                readAccounts(parser, getBalance)
            }
        }
        catch (e: FileNotFoundException) {
            // this is fine: initially the file of the accounts is missing
            Log.d("Accounts", "no $accountsFilename")
        }

        faucet?.let { add(Faucet(faucet, maxFaucet, getBalance(faucet))) }
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

    /**
     * Adds the given account from this object.
     *
     * @param account the account to add
     */
    fun add(account: Account) {
        accounts.add(account)
    }

    /**
     * Removes the given account from this object.
     *
     * @param account the account to remove
     */
    fun delete(account: Account) {
        accounts.remove(account)
    }

    /**
     * Yields the accounts in this container, in increasing order.
     *
     * @return the accounts, in increasing order
     */
    fun getAll(): Stream<Account> {
        return accounts.stream()
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
            accounts.forEach { it.writeWith(serializer) }
            serializer.endTag(null, "accounts")

            serializer.endDocument()
            serializer.flush()
        }
    }
}