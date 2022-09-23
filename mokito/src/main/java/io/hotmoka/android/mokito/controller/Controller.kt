package io.hotmoka.android.mokito.controller

import android.util.Base64
import android.util.Log
import androidx.preference.PreferenceManager
import io.hotmoka.android.mokito.MVC
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.model.Accounts
import io.hotmoka.android.mokito.model.Faucet
import io.hotmoka.android.mokito.model.OwnerTokens
import io.hotmoka.android.remote.AndroidRemoteNode
import io.hotmoka.beans.Coin
import io.hotmoka.beans.references.TransactionReference
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest
import io.hotmoka.beans.requests.TransactionRequest
import io.hotmoka.beans.signatures.CodeSignature
import io.hotmoka.beans.signatures.MethodSignature
import io.hotmoka.beans.signatures.NonVoidMethodSignature
import io.hotmoka.beans.types.BasicTypes
import io.hotmoka.beans.updates.Update
import io.hotmoka.beans.values.*
import io.hotmoka.crypto.BIP39Words
import io.hotmoka.crypto.Base58
import io.hotmoka.crypto.Entropy
import io.hotmoka.crypto.SignatureAlgorithmForTransactionRequests
import io.hotmoka.helpers.AccountCreationHelper
import io.hotmoka.helpers.SendCoinsHelper
import io.hotmoka.remote.RemoteNodeConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.security.KeyPair
import java.security.PublicKey
import java.util.concurrent.atomic.AtomicInteger

class Controller(private val mvc: MVC) {
    private var node: AndroidRemoteNode = AndroidRemoteNode()
    private var takamakaCode: TransactionReference? = null
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val signatureAlgorithmOfNewAccounts = SignatureAlgorithmForTransactionRequests.ed25519()
    private val working = AtomicInteger(0)

    companion object {
        private const val TAG = "Controller"
        @Suppress("ObjectPropertyName")
        private val _100_000 = BigInteger.valueOf(100_000L)
        private val IERC20View = "io.takamaka.code.tokens.IERC20View";
    }

    fun isWorking() : Boolean {
        return working.get() > 0
    }

    private fun ensureConnected() {
        if (!node.isConnected())
            connect()

        try {
            mvc.openFileInput("accounts.txt").bufferedReader().useLines { lines ->
                val all = lines.fold("") { some, text ->
                    "$some\n$text"
                }
                Log.d(TAG, all)
            }
        }
        catch (e: Exception) {
            Log.d(TAG, "no accounts.txt", e)
        }
    }

    private fun connect() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mvc)
        var url = sharedPreferences.getString("url", mvc.getString(R.string.default_server))

        if (url!!.startsWith("http://"))
            url = url.substring("http://".length)

        val config = RemoteNodeConfig.Builder()
            .setURL(url)
            .setWebSockets(sharedPreferences.getBoolean("webSockets", false))
            .build()

        try {
            node.connect(config)
        }
        catch (t: Throwable) {
            Log.d(TAG, "connection to $url failed")
            mainScope.launch {
                mvc.view?.notifyUser(mvc.getString(R.string.connection_failed, config.url))
            }
        }
        takamakaCode = node.takamakaCode

        mainScope.launch {
            mvc.view?.notifyUser(mvc.getString(R.string.connected, config.url))
        }
    }

    fun requestReconnect() {
        safeRunAsIO {
            // we clear the model, since the new node might contain different data
            mvc.model.clear()
            node.disconnect()
            connect()
        }
    }

    fun requestStateOf(reference: StorageReference) {
        safeRunAsIO {
            ensureConnected()
            val state = node.getState(reference)
            mvc.model.setState(reference, state.toArray { arrayOfNulls<Update>(it) })
        }
    }

    fun requestOwnerTokensOf(reference: StorageReference) {
        safeRunAsIO {
            ensureConnected()
            val size = getErc20Size(reference)
            val ownerTokens = Array<OwnerTokens>(size) { i -> getOwnerTokens(reference, i) }
            mvc.model.setErc20OwnerTokens(reference, ownerTokens)
        }
    }

    /**
     * Yields the pair owner/amount for the ith owner of the given ERC20 contract.
     *
     * @param erc20Token the storage reference of the contract in blockchain
     * @param the index of the owner (from 0 to token size minus 1)
     * @return the pair owner/amount
     */
    private fun getOwnerTokens(erc20Token: StorageReference, i: Int): OwnerTokens {
        // si modifichi il lato destro della riga seguente in modo da chiamare erc20Token.select(i)
        // ci si ispiri a quanto scritto dentro getErc20Size() più sotto
        val owner = erc20Token

        // si modifichi il lato destro
        // della riga seguente in modo da chiamare erc20Token.balanceOf(owner)
        // in modo simile a quanto fatto sopra; si noti che il risultato sarà una storage
        // reference di un oggetto in blockchain di tipo UnsignedBigInteger
        val amount = null

        // si modifichi il lato destro della riga seguente in modo da chiamare amount.toBigInteger()
        // in modo simile a quanto fatto sopra
        val amountAsBigInteger = BigInteger.valueOf(42L)
        return OwnerTokens(owner, amountAsBigInteger)
    }

    fun requestStateOfManifest() {
        safeRunAsIO {
            ensureConnected()
            requestStateOf(getManifestCached())
        }
    }

    fun requestAccounts() {
        safeRunAsIO {
            ensureConnected()
            mvc.model.setAccounts(reloadAccounts())
        }
    }

    fun requestDelete(account: Account) {
        safeRunAsIO {
            ensureConnected()
            val accounts = mvc.model.getAccounts() ?: reloadAccounts()
            accounts.delete(account)
            accounts.writeIntoInternalStorage(mvc)
            mainScope.launch { mvc.view?.onAccountDeleted(account) }
            mvc.model.setAccounts(accounts)
        }
    }

    fun requestReplace(old: Account, new: Account, passwordOld: String) {
        safeRunAsIO {
            checkPassword(old, passwordOld)
            ensureConnected()
            var newAccount = new
            new.reference?.let { newReference ->
                val balance = getBalance(newReference)
                newAccount = Account(newReference, new.name, new.entropy, new.publicKey, balance, true, new.coin)
                checkThatRemotePublicKeyMatches(newAccount)
            }
            val accounts = mvc.model.getAccounts() ?: reloadAccounts()
            accounts.delete(old)
            accounts.add(newAccount)
            accounts.writeIntoInternalStorage(mvc)
            mainScope.launch { mvc.view?.onAccountReplaced(old, newAccount) }
            mvc.model.setAccounts(accounts)
        }
    }

    fun requestNewAccountFromFaucet(name: String, passwordOfNewAccount: String, balance: BigInteger) {
        createNewAccount({ publicKey ->
            AccountCreationHelper(node)
                .fromFaucet(
                    signatureAlgorithmOfNewAccounts,
                    publicKey,
                    balance,
                    BigInteger.ZERO
                ) { }
        }, name, passwordOfNewAccount, balance)
    }

    fun requestNewAccountFromAnotherAccount(payer: Account, passwordOfPayer: String, name: String, passwordOfNewAccount: String, balance: BigInteger) {
        createNewAccount({ publicKey ->
            checkPassword(payer, passwordOfPayer)
            val keysOfPayer = getKeysOf(payer, passwordOfPayer)

            AccountCreationHelper(node)
                .fromPayer(
                    payer.reference,
                    keysOfPayer,
                    signatureAlgorithmOfNewAccounts,
                    publicKey,
                    balance,
                    BigInteger.ZERO,
                    false,
                    {},
                    {}
                )
        }, name, passwordOfNewAccount, balance)
    }

    fun requestBip39Words(account: Account) {
        safeRunAsIO {
            val acc = io.hotmoka.crypto.Account(account.entropy, account.reference)
            val bip39 = acc.bip39Words()

            mainScope.launch { mvc.view?.onBip39Available(account, bip39) }
        }
    }

    fun requestImportAccountFromBip39Words(name: String, mnemonic: Array<String>, password: String) {
        safeRunAsIO {
            val acc = BIP39Words.of(mnemonic).toAccount()
            ensureConnected()
            val balance = getBalance(acc.reference)
            val keys = acc.keys(password, signatureAlgorithmOfNewAccounts)
            val importedAccount = Account(acc.reference, name, acc, publicKeyBase64Encoded(keys), balance, true, Coin.PANAREA)
            checkThatRemotePublicKeyMatches(importedAccount)

            val accounts = mvc.model.getAccounts() ?: reloadAccounts()
            accounts.add(importedAccount)
            accounts.writeIntoInternalStorage(mvc)
            mainScope.launch { mvc.view?.onAccountImported(importedAccount) }
            mvc.model.setAccounts(accounts)
        }
    }

    fun requestNewKeyPair(password: String) {
        safeRunAsIO {
            val entropy = Entropy()
            val keys = entropy.keys(password, signatureAlgorithmOfNewAccounts)
            val publicKeyBase58 = publicKeyBase58Encoded(keys)
            val publicKeyBase64 = publicKeyBase64Encoded(keys)
            Log.d(TAG, "created public key $publicKeyBase58")

            // it is not a fully functional account yet, since it misses the reference
            val newAccount = Account(null, publicKeyBase58, entropy, publicKeyBase64, BigInteger.ZERO, false, Coin.PANAREA)
            ensureConnected()
            val accounts = mvc.model.getAccounts() ?: reloadAccounts()
            accounts.add(newAccount)
            accounts.writeIntoInternalStorage(mvc)
            mvc.model.setAccounts(accounts)
        }
    }

    /**
     * Request a payment to an existing account.
     *
     * @param payer the paying account
     * @param destination the recipient of the payment
     * @param amount to amount to transfer to {@code destination}
     * @param passwordOfPayer the password of {@code payer}
     */
    fun requestPayment(payer: Account, destination: StorageReference, amount: BigInteger, passwordOfPayer: String) {
        safeRunAsIO {
            checkPassword(payer, passwordOfPayer)
            val keys = getKeysOf(payer, passwordOfPayer)
            ensureConnected()
            var savedRequests: Array<TransactionRequest<*>>? = null
            SendCoinsHelper(node).fromPayer(payer.reference, keys, destination, amount, BigInteger.ZERO, {}, {
                requests -> savedRequests = requests
            })

            Log.d(TAG, "paid $amount to account $destination")

            // we reload the accounts, since the payer will see its balance decrease
            val accounts = reloadAccounts()
            mvc.model.setAccounts(accounts)

            mainScope.launch {
                mvc.view?.onPaymentCompleted(
                    payer,
                    destination,
                    null,
                    amount,
                    false,
                    toTransactions(savedRequests)
                )
            }
        }
    }

    private fun toTransactions(requests: Array<TransactionRequest<*>>?): List<TransactionReference> {
        requests?.let {
            return it.map { request -> request.reference }
        }

        return emptyList()
    }

    /*private fun notifyGasConsumption(requests: Array<TransactionRequest<*>>) {
        val counter = GasCounter(node, *requests)
        Log.d(TAG, "total: " + counter.total)
        Log.d(TAG, "forCPU: " + counter.forCPU)
        Log.d(TAG, "forRAM: " + counter.forRAM)
        Log.d(TAG, "forStorage: " + counter.forStorage)
        Log.d(TAG, "forPenalty: " + counter.forPenalty)
    }*/

    /**
     * Request a payment to a public key.
     *
     * @param payer the paying account
     * @param publicKey the Base58-encoded public key of the recipient of the payment
     * @param amount to amount to transfer to {@code publicKey}
     * @param anonymous if true, the payment occurs by creating an account in the account ledger
     *                  of the Hotmoka node, for {@code publicKey}. Otherwise, a new account gets
     *                  created, with {@code publicKey} as public key
     * @param password the password of {@code payer}
     */
    fun requestPaymentToPublicKey(payer: Account, publicKey: String, amount: BigInteger, anonymous: Boolean, password: String) {
        safeRunAsIO {
            checkPassword(payer, password)
            val keysOfPayer = getKeysOf(payer, password)
            ensureConnected()
            var savedRequests: Array<TransactionRequest<*>>? = null
            val destination = AccountCreationHelper(node).fromPayer(
                payer.reference,
                keysOfPayer,
                signatureAlgorithmOfNewAccounts,
                signatureAlgorithmOfNewAccounts.publicKeyFromEncoding(Base58.decode(publicKey)),
                amount,
                BigInteger.ZERO,
                anonymous,
                {},
                {
                   requests -> savedRequests = requests
                }
            )

            if (anonymous)
                Log.d(TAG, "paid $amount anonymously to key $publicKey [destination is $destination]")
            else
                Log.d(TAG, "paid $amount to key $publicKey [destination is $destination]")

            // we reload the accounts, since the payer will see its balance decrease
            val accounts = reloadAccounts()
            mvc.model.setAccounts(accounts)
            mainScope.launch { mvc.view?.onPaymentCompleted(
                payer,
                destination,
                publicKey,
                amount,
                anonymous,
                toTransactions(savedRequests)
            ) }
        }
    }

    fun requestPaymentFromFaucet(faucet: Faucet, destination: StorageReference, amount: BigInteger) {
        safeRunAsIO {
            ensureConnected()
            var savedRequests: Array<TransactionRequest<*>>? = null
            SendCoinsHelper(node).fromFaucet(destination, amount, BigInteger.ZERO, {}, {
                requests -> savedRequests = requests
            })
            Log.d(TAG, "paid $amount from ${faucet.name} to account $destination")

            // we reload the accounts, since the payer will see its balance decrease
            val accounts = reloadAccounts()
            mvc.model.setAccounts(accounts)
            mainScope.launch { mvc.view?.onPaymentCompleted(
                faucet,
                destination,
                null,
                amount,
                false,
                toTransactions(savedRequests)
            ) }
        }
    }

    private fun getKeysOf(account: Account, password: String): KeyPair {
        return account.entropy.keys(password, signatureAlgorithmOfNewAccounts)
    }

    private fun publicKeyBase64Encoded(keys: KeyPair): String {
        return Base64.encodeToString(signatureAlgorithmOfNewAccounts.encodingOf(keys.public), Base64.NO_WRAP)
    }

    private fun publicKeyBase58Encoded(keys: KeyPair): String {
        return Base58.encode(signatureAlgorithmOfNewAccounts.encodingOf(keys.public))
    }

    /**
     * Checks that the reference of the given account is actually an account object in the node
     * with the same public key as the account.
     *
     * @param account the account to check
     * @throws IllegalArgumentException if the two public keys do not match
     */
    private fun checkThatRemotePublicKeyMatches(account: Account) {
        account.reference?.let {
            if (getPublicKey(it) != account.publicKey)
                throw IllegalArgumentException("The public key of the account does not match its entropy")
        }
    }

    /**
     * Checks that the given string is actually the password of the given account.
     *
     * @param account the account whose password is being checked
     * @param password the proposed password of {@code account}
     * @throws IllegalArgumentException if the password is incorrect
     */
    private fun checkPassword(account: Account, password: String) {
        if (!passwordIsCorrect(account, password))
            throw IllegalArgumentException("Incorrect password!")
    }

    fun passwordIsCorrect(account: Account, password: String): Boolean {
        return publicKeyBase64Encoded(getKeysOf(account, password)) == account.publicKey
    }

    private fun reloadAccounts(): Accounts {
        return Accounts(mvc, getFaucet(), getMaxFaucet(), this::getBalance, this::getReferenceFromAccountsLedger)
    }

    private fun createNewAccount(creator: (PublicKey) -> StorageReference, name: String, password: String, balance: BigInteger) {
        safeRunAsIO {
            ensureConnected()
            val entropy = Entropy()
            val keys = entropy.keys(password, signatureAlgorithmOfNewAccounts)

            // create an account with the public key
            val reference = creator(keys.public)
            Log.d(TAG, "created new account $reference")

            // currently, the progressive number of the created accounts will be #0,
            // but we better check against future unexpected changes in the server's behavior
            if (reference.progressive.signum() != 0)
                throw IllegalStateException("I can only deal with new accounts whose progressive number is 0")

            // we force a reload of the accounts, so that their balances reflect the changes;
            // this is important, in particular, to update the balance of the payer
            val accounts = reloadAccounts()
            val newAccount = Account(reference, name, entropy, publicKeyBase64Encoded(keys), balance, true, Coin.PANAREA)
            accounts.add(newAccount)
            accounts.writeIntoInternalStorage(mvc)
            mainScope.launch { mvc.view?.onAccountCreated(newAccount) }
            mvc.model.setAccounts(accounts)
        }
    }

    /**
     * Yields the faucet of the Hotmoka node, if the node allows it.
     *
     * @return the faucet, if any
     */
    private fun getFaucet(): StorageReference? {
        val manifest = getManifestCached()
        val hasFaucet = (node.runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest(
            manifest, _100_000, takamakaCode, MethodSignature.ALLOWS_UNSIGNED_FAUCET, manifest
        )) as BooleanValue).value

        return if (hasFaucet)
            getGameteCached()
        else
            null
    }

    private fun getMaxFaucet(): BigInteger {
        val manifest = getManifestCached()
        return (node.runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest(
            manifest, _100_000, takamakaCode, MethodSignature.GET_MAX_FAUCET, getGameteCached()
        )) as BigIntegerValue).value
    }

    private fun getBalance(reference: StorageReference): BigInteger {
        return (node.runInstanceMethodCallTransaction(
            InstanceMethodCallTransactionRequest(
                reference, _100_000, takamakaCode, MethodSignature.BALANCE, reference
            )
        ) as BigIntegerValue).value
    }

    private fun getErc20Size(reference: StorageReference): Int {
        // we use the manifest as caller, since the call is free of charge
        val manifest = getManifestCached()

        return (node.runInstanceMethodCallTransaction(
            InstanceMethodCallTransactionRequest(
                manifest, _100_000, takamakaCode, NonVoidMethodSignature(
                    IERC20View,
                    "size",
                    BasicTypes.INT
                ), reference
            )
        ) as IntValue).value
    }

    private fun getReferenceFromAccountsLedger(publicKey: String): StorageReference? {
        Log.d(TAG, "looking in the ledger for $publicKey")
        val manifest = getManifestCached()
        val ledger = getAccountsLedgerCached()
        val result = (node.runInstanceMethodCallTransaction(
            InstanceMethodCallTransactionRequest(
                manifest, _100_000, takamakaCode,
                CodeSignature.GET_FROM_ACCOUNTS_LEDGER,
                ledger,
                StringValue(publicKey)
            )
        ))

        return if (result is StorageReference)
            result
        else
            null
    }

    private fun getPublicKey(reference: StorageReference): String {
        return (node.runInstanceMethodCallTransaction(
            InstanceMethodCallTransactionRequest(
                reference, _100_000, takamakaCode, MethodSignature.PUBLIC_KEY, reference
            )
        ) as StringValue).value
    }

    private fun safeRunAsIO(task: () -> Unit) {
        working.incrementAndGet()
        mainScope.launch { mvc.view?.onBackgroundStart() }

        ioScope.launch {
            try {
                task.invoke()
                working.decrementAndGet()
                mainScope.launch { mvc.view?.onBackgroundEnd() }
            }
            catch (t: Throwable) {
                working.decrementAndGet()
                mainScope.launch {
                    mvc.view?.onBackgroundEnd()
                    mvc.view?.notifyException(t)
                }
            }
        }
    }

    private fun getManifestCached(): StorageReference {
        mvc.model.getManifest()?.let {
            return it
        }

        val manifest = node.manifest
        mvc.model.setManifest(manifest)
        return manifest
    }

    private fun getGameteCached(): StorageReference {
        mvc.model.getGamete()?.let {
            return it
        }

        val manifest = getManifestCached()
        val gamete = node.runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest(
            manifest, _100_000, takamakaCode, MethodSignature.GET_GAMETE, manifest
        )) as StorageReference

        mvc.model.setGamete(gamete)
        return gamete
    }

    private fun getAccountsLedgerCached(): StorageReference {
        mvc.model.getAccountsLedger()?.let {
            return it
        }

        val manifest = getManifestCached()
        val accountsLedger = node.runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest(
            manifest, _100_000, takamakaCode, MethodSignature.GET_ACCOUNTS_LEDGER, manifest
        )) as StorageReference

        mvc.model.setAccountsLedger(accountsLedger)
        return accountsLedger
    }
}