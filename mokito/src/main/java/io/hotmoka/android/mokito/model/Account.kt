package io.hotmoka.android.mokito.model

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import io.hotmoka.node.StorageValues
import io.hotmoka.node.TransactionReferences
import io.hotmoka.helpers.Coin
import io.hotmoka.node.api.transactions.TransactionReference
import io.hotmoka.node.api.values.StorageReference
import io.hotmoka.crypto.Entropies
import io.hotmoka.crypto.api.Entropy
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
     * to create the account and bind it to the entropy.
     */
    val reference: StorageReference?

    /**
     * The name given to the account. This is not contained in the store of the Hotmoka
     * node, but only maintained locally, in the internal storage of the mobile application,
     * for ease of reference to the account.
     */
    val name: String

    /**
     * The Base64 encoded public key of the account, derived from the entropy and the password.
     */
    val publicKey: String

    /**
     * The preferred coin level for the representation of the balance of this account.
     */
    val coin: Coin

    /**
     * The balance of the account in the storage of the Hotmoka node, in Panareas.
     */
    val balance: BigInteger

    /**
     * True if and only if the account could be accessed and its balance could be retrieved.
     */
    val isAccessible: Boolean

    /**
     * The entropy that was used to generate the key pair of the account. Note that this entropy
     * is useless if the associated password is not known, that, merged with this entropy,
     * allows one to reconstruct the key pair. The password is not stored anywhere, it should
     * be remembered by the user.
     */
    val entropy: Entropy

    constructor(reference: StorageReference?, name: String, entropy: Entropy, publicKey: String, balance: BigInteger, isAccessible: Boolean, coin: Coin) {
        this.reference = reference
        this.name = name
        this.entropy = entropy
        this.publicKey = publicKey
        this.balance = balance
        this.isAccessible = isAccessible
        this.coin = coin
    }

    constructor(parser: XmlPullParser, getBalance: (StorageReference) -> BigInteger, getReferenceFromAccountsLedger: (String) -> StorageReference?) {
        parser.require(XmlPullParser.START_TAG, null, "account")

        var reference: StorageReference? = null
        var name: String? = null
        var entropy: Entropy? = null
        var publicKey: String? = null
        var coin: String? = null

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG)
                continue

            when (parser.name) {
                "reference" -> reference = readReference(parser)
                "name" -> name = readName(parser)
                "entropy" -> entropy = readEntropy(parser)
                "publicKey" -> publicKey = readPublicKey(parser)
                "coin" -> coin = readCoin(parser)
                else -> skip(parser)
            }
        }

        this.name = name ?: throw IOException("Missing name in account")
        this.entropy = entropy ?: throw IOException("Missing entropy in account")
        this.publicKey = publicKey ?: throw IOException("Missing public key in account")
        this.coin = if (coin != null)
            Coin.valueOf(coin)
            else throw IOException("Missing coin unit in account")

        // the reference is null if the account is still a key waiting for the
        // corresponding account to be created; in that case, we consult the
        // accounts ledger of the node to see if that account has been created:
        // we use the public key as identifier
        if (reference == null)
            try {
                reference = getReferenceFromAccountsLedger(publicKey)
            }
            catch (e: Exception) {
                Log.w("Account", "Cannot find $publicKey in the accounts ledger")
            }

        this.reference = reference

        if (reference != null) {
            var balance: BigInteger
            var isAccessible: Boolean

            try {
                balance = getBalance(reference)
                isAccessible = true
            }
            catch (e: Exception) {
                balance = BigInteger.ZERO
                isAccessible = false
                Log.w("Account", "Cannot access the balance of account $reference: $e")
            }

            this.balance = balance
            this.isAccessible = isAccessible
        }
        else {
            this.balance = BigInteger.ZERO
            this.isAccessible = false
        }
    }

    protected constructor(parcel: Parcel) {
        this.reference = parcel.readSerializable() as StorageReference
        this.name = parcel.readString()!!
        this.balance = parcel.readSerializable() as BigInteger
        this.isAccessible = parcel.readByte() != 0.toByte()
        this.publicKey = parcel.readString()!!
        val entropy = ByteArray(parcel.readInt())
        parcel.readByteArray(entropy)
        this.entropy = Entropies.of(entropy)
        this.coin = Coin.valueOf(parcel.readString()!!)
    }

    /**
     * Determines if this account is just a key, waiting for the creation of
     * a corresponding account.
     */
    fun isKey(): Boolean {
        return reference == null
    }

    /**
     * Yields the maximum amount that can be spent with this account.
     *
     * @return the maximum amount
     */
    open fun maxPayment(): BigInteger {
        return balance
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeByte(0)
        writeToParcelInternal(out)
    }

    protected fun writeToParcelInternal(out: Parcel) {
        out.writeSerializable(reference)
        out.writeString(name)
        out.writeSerializable(balance)
        out.writeByte(if (isAccessible) 1.toByte() else 0.toByte())
        out.writeString(publicKey)
        out.writeInt(entropy.length())
        out.writeByteArray(entropy.entropyAsBytes)
        out.writeString(coin.name)
    }

    companion object {

        @Suppress("unused") @JvmField
        val CREATOR = object : Parcelable.Creator<Account?> {

            override fun createFromParcel(parcel: Parcel): Account {
                val start = parcel.readByte()
                return if (start == 0.toByte())
                    Account(parcel)
                else
                    Faucet(parcel)
            }

            override fun newArray(size: Int): Array<Account?> {
                return arrayOfNulls(size)
            }
        }
    }

    fun setName(newName: String): Account {
        return Account(reference, newName, entropy, publicKey, balance, isAccessible, coin)
    }

    fun setReference(newReference: StorageReference): Account {
        return Account(newReference, name, entropy, publicKey, balance, isAccessible, coin)
    }

    fun setCoin(newCoin: Coin): Account {
        return Account(reference, name, entropy, publicKey, balance, isAccessible, newCoin)
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readName(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, null, "name")
        val name = readText(parser)
        parser.require(XmlPullParser.END_TAG, null, "name")
        return name
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readCoin(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, null, "coin")
        val name = readText(parser)
        parser.require(XmlPullParser.END_TAG, null, "coin")
        return name
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readPublicKey(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, null, "publicKey")
        val publicKey = readText(parser)
        parser.require(XmlPullParser.END_TAG, null, "publicKey")
        return publicKey
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readEntropy(parser: XmlPullParser): Entropy {
        parser.require(XmlPullParser.START_TAG, null, "entropy")
        val entropy = readText(parser)
        parser.require(XmlPullParser.END_TAG, null, "entropy")
        return Entropies.of(Hex.decode(entropy))
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

        return StorageValues.reference(
            transaction ?: throw IOException("Missing transaction tag in account"),
            progressive ?: throw IOException("Missing progressive tag in account")
        )
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readTransaction(parser: XmlPullParser): TransactionReference {
        parser.require(XmlPullParser.START_TAG, null, "transaction")
        val transaction = readText(parser)
        parser.require(XmlPullParser.END_TAG, null, "transaction")
        return TransactionReferences.of(transaction)
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
        if (parser.eventType != XmlPullParser.START_TAG)
            throw IllegalStateException("skip() was meant to be called on a START XML tag")

        var depth = 1
        do {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
        while (depth != 0)
    }

    open fun writeWith(serializer: XmlSerializer) {
        serializer.startTag(null, "account")

        reference?.let {
            serializer.startTag(null, "reference")
            serializer.startTag(null, "transaction")
            serializer.text(Hex.toHexString(reference.transaction.hash))
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
        serializer.text(entropy.toString())
        serializer.endTag(null, "entropy")

        serializer.startTag(null, "publicKey")
        serializer.text(publicKey)
        serializer.endTag(null, "publicKey")

        serializer.startTag(null, "coin")
        serializer.text(coin.name)
        serializer.endTag(null, "coin")

        serializer.endTag(null, "account")
    }

    override fun compareTo(other: Account): Int {
        val diff = entropy.compareTo(other.entropy)
        if (diff == 0)
            return 0

        val diff2 = name.compareTo(other.name)
        return if (diff2 != 0)
            diff2
        else
            diff
    }

    override fun equals(other: Any?): Boolean {
        return other is Account && entropy == other.entropy
    }

    override fun hashCode(): Int {
        return entropy.hashCode()
    }
}