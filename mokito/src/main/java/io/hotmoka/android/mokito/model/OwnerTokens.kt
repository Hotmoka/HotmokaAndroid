package io.hotmoka.android.mokito.model;

import java.math.BigInteger;

import io.hotmoka.node.api.values.StorageReference;

class OwnerTokens { //cosa fa: dichiara reference e amount, che sono le cose che voglio estrapolare e mettere nelle cards
    val reference: StorageReference
    val amount: BigInteger

    // un costruttore è il modo in cui si costruisce un oggetto quando si scrive new OwnerTokens(....). E' un caso particolare di funzione
    constructor(reference: StorageReference, amount: BigInteger) { //cosa fa: "inizializza" reference e amount (regola del codice)
        this.reference = reference;
        this.amount = amount;
    }
}