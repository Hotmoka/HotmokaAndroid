package io.hotmoka.android.mokito.model

import io.hotmoka.beans.values.StorageReference
import org.xmlpull.v1.XmlSerializer
import java.math.BigInteger

class Faucet(reference: StorageReference, balance: BigInteger):
    Account(reference, "Faucet", ByteArray(0), balance) {

    override fun writeWith(serializer: XmlSerializer) {
        // the faucet is not dumped into the XML file
    }
}