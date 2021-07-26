package io.hotmoka.android.mokito.model

import io.hotmoka.beans.references.LocalTransactionReference
import io.hotmoka.beans.references.TransactionReference
import io.hotmoka.beans.values.StorageReference
import org.bouncycastle.util.encoders.Hex
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlSerializer
import java.io.IOException
import java.lang.IllegalStateException
import java.math.BigInteger

open class Account {
    val reference: StorageReference
    val name: String
    private val entropy: ByteArray
    val balance: BigInteger

    constructor(reference: StorageReference, name: String, entropy: ByteArray, balance: BigInteger) {
        this.reference = reference
        this.name = name
        this.entropy = entropy
        this.balance = balance
    }

    constructor(parser: XmlPullParser, getBalance: (StorageReference) -> BigInteger) {
        parser.require(XmlPullParser.START_TAG, null, "account")

        var reference: StorageReference? = null
        var name: String? = null
        var entropy: ByteArray? = null

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG)
                continue

            when (parser.name) {
                "reference" -> reference = readReference(parser)
                "name" -> name = readName(parser)
                "entropy" -> entropy = readEntropy(parser)
                else -> skip(parser)
            }
        }

        if (reference != null)
            this.reference = reference
        else
            throw IllegalStateException("missing reference tag in account")

        if (name != null)
            this.name = name
        else
            throw IllegalStateException("missing name tag in account")

        if (entropy != null)
            this.entropy = entropy
        else
            throw IllegalStateException("missing entropy tag in account")

        this.balance = getBalance(reference)
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readName(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, null, "name")
        val name = readText(parser)
        parser.require(XmlPullParser.END_TAG, null, "name")
        return name
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readEntropy(parser: XmlPullParser): ByteArray {
        parser.require(XmlPullParser.START_TAG, null, "entropy")
        val entropy = readText(parser)
        parser.require(XmlPullParser.END_TAG, null, "entropy")
        return Hex.decode(entropy)
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readReference(parser: XmlPullParser): StorageReference {
        parser.require(XmlPullParser.START_TAG, null, "reference")

        var transaction: TransactionReference? = null
        var progressive: BigInteger? = null

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG)
                continue

            when (parser.name) {
                "transaction" -> transaction = readTransaction(parser)
                "progressive" -> progressive = readProgressive(parser)
                else -> skip(parser)
            }
        }

        if (transaction == null)
            throw IllegalStateException("missing transaction tag in account")

        if (progressive == null)
            throw IllegalStateException("missing name tag in account")

        return StorageReference(transaction, progressive)
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readTransaction(parser: XmlPullParser): TransactionReference {
        parser.require(XmlPullParser.START_TAG, null, "transaction")
        val transaction = readText(parser)
        parser.require(XmlPullParser.END_TAG, null, "transaction")
        return LocalTransactionReference(transaction)
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readProgressive(parser: XmlPullParser): BigInteger {
        parser.require(XmlPullParser.START_TAG, null, "progressive")
        val progressive = readText(parser)
        parser.require(XmlPullParser.END_TAG, null, "progressive")
        return BigInteger(progressive)
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
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

    fun writeWith(serializer: XmlSerializer) {
        serializer.startTag(null, "account")

        serializer.startTag(null, "reference")
        serializer.startTag(null, "transaction")
        serializer.text(Hex.toHexString(reference.transaction.hashAsBytes))
        serializer.endTag(null, "transaction")
        serializer.startTag(null, "progressive")
        serializer.text(reference.progressive.toString())
        serializer.endTag(null, "progressive")
        serializer.endTag(null, "reference")

        serializer.startTag(null, "name")
        serializer.text(name)
        serializer.endTag(null, "name")

        serializer.startTag(null, "entropy")
        serializer.text(Hex.toHexString(entropy))
        serializer.endTag(null, "entropy")

        serializer.endTag(null, "account")
    }
}