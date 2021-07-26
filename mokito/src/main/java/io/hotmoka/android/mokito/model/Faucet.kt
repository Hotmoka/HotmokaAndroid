package io.hotmoka.android.mokito.model

import io.hotmoka.beans.values.StorageReference
import java.math.BigInteger

class Faucet(reference: StorageReference, balance: BigInteger):
    Account(reference, "Faucet", ByteArray(0), balance) {
}