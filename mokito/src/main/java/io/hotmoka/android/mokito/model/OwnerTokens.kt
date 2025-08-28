package io.hotmoka.android.mokito.model;

import java.math.BigInteger;

import io.hotmoka.node.api.values.StorageReference;

class OwnerTokens {
    val reference: StorageReference
    val amount: BigInteger

    constructor(reference: StorageReference, amount: BigInteger) {
        this.reference = reference;
        this.amount = amount;
    }
}