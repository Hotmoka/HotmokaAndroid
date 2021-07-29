package io.hotmoka.android.mokito.model

import android.os.Parcel
import android.os.Parcelable
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

/**
 * An account of the user of the application.
 */
open class Account: Comparable<Account>, Parcelable {

    /**
     * The reference of the account in the store of the Hotmoka node.
     * This might be missing if we are waiting for somebody else
     * to create the account and store it in the accounts ledger of the manifest of the node.
     */
    val reference: StorageReference?

    /**
     * The name given to the account. This is not contained in the store of the Hotmoka
     * node, but only maintained locally, in the internal storage of the mobile application,
     * for ease of reference to the account.
     */
    val name: String

    /**
     * The account of the account in the storage of the Hotmoka node.
     */
    val balance: BigInteger

    /**
     * The entropy that was used to generate the key pair of the account. Only the
     * public key of the key pair is stored in the Hotmoka node. Note that this entropy
     * is useless if the associated password is not known, that, merged with this entropy,
     * allows one to reconstruct the key pair. The password is not stored anywhere, it should
     * be remembered by the user.
     */
    private val entropy: ByteArray

    constructor(reference: StorageReference?, name: String, entropy: ByteArray, balance: BigInteger) {
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

        if (reference != null) {
            this.reference = reference
            this.balance = getBalance(reference)
        }
        else {
            this.reference = null
            this.balance = BigInteger.ZERO
        }

        if (name != null)
            this.name = name
        else
            throw IllegalStateException("missing name tag in account")

        if (entropy != null)
            this.entropy = entropy
        else
            throw IllegalStateException("missing entropy tag in account")
    }

    protected constructor(parcel: Parcel) {
        this.reference = parcel.readSerializable() as StorageReference
        this.name = parcel.readString()!!
        this.balance = parcel.readSerializable() as BigInteger
        this.entropy = ByteArray(parcel.readInt())
        parcel.readByteArray(this.entropy)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeByte(0)
        writeToParcelInternal(out, flags)
    }

    protected fun writeToParcelInternal(out: Parcel, flags: Int) {
        out.writeSerializable(reference)
        out.writeString(name)
        out.writeSerializable(balance)
        out.writeInt(entropy.size)
        out.writeByteArray(entropy)
    }

    companion object {

        @Suppress("unused") @JvmField
        val CREATOR = object : Parcelable.Creator<Account?> {

            override fun createFromParcel(parcel: Parcel): Account {
                val start = parcel.readByte()
                if (start == 0.toByte())
                    return Account(parcel)
                else
                    return Faucet(parcel)
            }

            override fun newArray(size: Int): Array<Account?> {
                return arrayOfNulls(size)
            }
        }
    }

    fun setName(newName: String): Account {
        return Account(reference, newName, entropy.clone(), balance)
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

    open fun writeWith(serializer: XmlSerializer) {
        serializer.startTag(null, "account")

        reference?.let {
            serializer.startTag(null, "reference")
            serializer.startTag(null, "transaction")
            serializer.text(Hex.toHexString(reference.transaction.hashAsBytes))
            serializer.endTag(null, "transaction")
            serializer.startTag(null, "progressive")
            serializer.text(reference.progressive.toString())
            serializer.endTag(null, "progressive")
            serializer.endTag(null, "reference")
        }

        serializer.startTag(null, "name")
        serializer.text(name)
        serializer.endTag(null, "name")

        serializer.startTag(null, "entropy")
        serializer.text(Hex.toHexString(entropy))
        serializer.endTag(null, "entropy")

        serializer.endTag(null, "account")
    }

    override fun compareTo(other: Account): Int {
        val diff = compareEntropies(entropy, other.entropy)
        if (diff == 0)
            return 0

        val diff2 = name.compareTo(other.name)
        return if (diff2 != 0)
            diff2
        else
            diff
    }

    private fun compareEntropies(entropy1: ByteArray, entropy2: ByteArray): Int {
        var diff: Int = entropy1.size - entropy2.size
        if (diff != 0)
            return diff

        for (pos in entropy1.indices) {
           diff = entropy1[pos] - entropy2[pos]
           if (diff != 0)
               return diff
        }

        return 0
    }

    override fun equals(other: Any?): Boolean {
        return other is Account && entropy.contentEquals(other.entropy)
    }

    override fun hashCode(): Int {
        return entropy.contentHashCode()
    }

    fun getEntropy(): ByteArray {
        return entropy.clone()
    }
}