package io.hotmoka.android.mokito.model

import android.os.Parcel
import io.hotmoka.beans.values.StorageReference
import org.xmlpull.v1.XmlSerializer
import java.math.BigInteger

class Faucet: Account {
    val maxFaucet: BigInteger

    constructor(reference: StorageReference, maxFaucet: BigInteger, balance: BigInteger, accessible: Boolean):
            super(reference, "Faucet", ByteArray(0), "", balance, accessible) {
        this.maxFaucet = maxFaucet
    }

    constructor(parcel: Parcel): super(parcel) {
        this.maxFaucet = parcel.readSerializable() as BigInteger
    }

    override fun writeWith(serializer: XmlSerializer) {
        // the faucet is not dumped into the XML file
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeByte(1)
        writeToParcelInternal(out)
        out.writeSerializable(maxFaucet)
    }

    override fun maxPayment(): BigInteger {
        return maxFaucet.min(super.maxPayment())
    }
}