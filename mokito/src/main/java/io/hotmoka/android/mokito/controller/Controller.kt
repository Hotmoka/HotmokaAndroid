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
import io.hotmoka.crypto.BIP39Mnemonics
import io.hotmoka.crypto.Base58
import io.hotmoka.crypto.Entropies
import io.hotmoka.crypto.HashingAlgorithms
import io.hotmoka.crypto.SignatureAlgorithms
import io.hotmoka.helpers.AccountCreationHelpers
import io.hotmoka.helpers.Coin
import io.hotmoka.helpers.SendCoinsHelpers
import io.hotmoka.helpers.UnexpectedValueException
import io.hotmoka.helpers.UnexpectedVoidMethodException
import io.hotmoka.node.MethodSignatures
import io.hotmoka.node.StorageTypes
import io.hotmoka.node.StorageValues
import io.hotmoka.node.TransactionReferences
import io.hotmoka.node.TransactionRequests
import io.hotmoka.node.api.requests.TransactionRequest
import io.hotmoka.node.api.transactions.TransactionReference
import io.hotmoka.node.api.updates.Update
import io.hotmoka.node.api.values.StorageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.net.URI
import java.security.KeyPair
import java.security.PublicKey
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

class Controller(private val mvc: MVC) {
    private var node: AndroidRemoteNode = AndroidRemoteNode()
    private var takamakaCode: TransactionReference? = null
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val signatureAlgorithmOfNewAccounts = SignatureAlgorithms.ed25519()
    private val working = AtomicInteger(0)

    companion object {
        private const val TAG = "Controller"
        private val _100_000 = BigInteger.valueOf(100_000L)
        private val IERC20View = StorageTypes.classNamed("io.takamaka.code.tokens.IERC20View")
    }

    fun isWorking(): Boolean {
        return working.get() > 0
    }

    private fun ensureConnected() {
        if (!node.isConnected())
            connect()

        /*try {
            mvc.openFileInput("accounts.txt").bufferedReader().useLines { lines ->
                val all = lines.fold("") { some, text ->
                    "$some\n$text"
                }
                Log.d(TAG, all)
            }
        } catch (e: Exception) {
            Log.w(TAG, "I could not read the accounts.txt file: " + e.message)
        }*/
    }

    private fun connect() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mvc)
        val uri = sharedPreferences.getString("url", mvc.getString(R.string.default_server))

        try {
            node.connect(URI.create(uri), 200_000)
            takamakaCode = node.takamakaCode
            mainScope.launch { mvc.view?.notifyUser(mvc.getString(R.string.connected, uri)) }
        } catch (t: Throwable) {
            Log.w(TAG, "Connection to $uri failed: " + t.message)
            mainScope.launch { mvc.view?.notifyUser(mvc.getString(R.string.connection_failed, uri)) }
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
            // we operate on a snapshot, to avoid race conditions
            val snapshot = getERC20SnapshotOf(reference)
            val size = getErc20Size(snapshot)
            val ownerTokens = Array(size) { i -> getOwnerTokens(snapshot, i) }
            mvc.model.setErc20OwnerTokens(reference, ownerTokens)
        }
    }

    /**
     * Yields the pair owner/amount for the ith owner of the given ERC20 contract.
     *
     * @param erc20Token the storage reference of the contract in blockchain
     * @param i the index of the owner (from 0 to token size minus 1)
     * @return the pair owner/amount
     */
    private fun getOwnerTokens(erc20Token: StorageReference, i: Int): OwnerTokens {
        // call owner = erc20Token.select(i)
        val select = MethodSignatures.ofNonVoid(
            IERC20View,
            "select",
            StorageTypes.CONTRACT,
            StorageTypes.INT
        )
        val owner = node.runInstanceMethodCallTransaction(
            TransactionRequests.instanceViewMethodCall(
                getManifestCached(), _100_000, takamakaCode, select,
                erc20Token,
                StorageValues.intOf(i)
            )
        )
            .orElseThrow { UnexpectedVoidMethodException(select) }
            .asReturnedReference(select, ::UnexpectedValueException)

        // call amount = erc20Token.balanceOf(owner)
        // the resulting amount will be an UnsignedBigInteger
        val balanceOf = MethodSignatures.ofNonVoid(
            IERC20View,
            "balanceOf",
            StorageTypes.UNSIGNED_BIG_INTEGER,
            StorageTypes.CONTRACT
        )
        val amount = node.runInstanceMethodCallTransaction(
            TransactionRequests.instanceViewMethodCall(
                getManifestCached(), _100_000, takamakaCode, balanceOf,
                erc20Token,
                owner
            )
        )
            .orElseThrow { UnexpectedVoidMethodException(balanceOf) }
            .asReturnedReference(balanceOf, ::UnexpectedValueException)

        // call amountAsBigInteger = amount.toBigInteger()
        // in order to extract the BigInteger inside amount
        val toBigInteger = MethodSignatures.ofNonVoid(
            StorageTypes.UNSIGNED_BIG_INTEGER,
            "toBigInteger",
            StorageTypes.BIG_INTEGER
        )
        val amountAsBigInteger = node.runInstanceMethodCallTransaction(
            TransactionRequests.instanceViewMethodCall(
                getManifestCached(), _100_000, takamakaCode, toBigInteger, amount
            )
        )
            .orElseThrow { UnexpectedVoidMethodException(toBigInteger) }
            .asReturnedBigInteger(toBigInteger, ::UnexpectedValueException)

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
                newAccount = Account(
                    newReference,
                    new.name,
                    new.entropy,
                    new.publicKey,
                    balance,
                    true,
                    new.coin
                )
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

    fun requestNewAccountFromFaucet(
        name: String,
        passwordOfNewAccount: String,
        balance: BigInteger
    ) {
        createNewAccount({ publicKey ->
            AccountCreationHelpers.of(node)
                .paidByFaucet(
                    signatureAlgorithmOfNewAccounts,
                    publicKey,
                    balance,
                ) { }
        }, name, passwordOfNewAccount, balance)
    }

    fun requestNewAccountFromAnotherAccount(
        payer: Account,
        passwordOfPayer: String,
        name: String,
        passwordOfNewAccount: String,
        balance: BigInteger
    ) {
        createNewAccount({ publicKey ->
            checkPassword(payer, passwordOfPayer)
            val keysOfPayer = getKeysOf(payer, passwordOfPayer)

            AccountCreationHelpers.of(node)
                .paidBy(
                    payer.reference,
                    keysOfPayer,
                    signatureAlgorithmOfNewAccounts,
                    publicKey,
                    balance,
                    {},
                    {}
                )
        }, name, passwordOfNewAccount, balance)
    }

    fun requestBip39Words(account: Account) {
        safeRunAsIO {
            val acc = io.hotmoka.node.Accounts.of(account.entropy, account.reference)
            val bip39 = acc.bip39Words()

            mainScope.launch { mvc.view?.onBip39Available(account, bip39) }
        }
    }

    fun requestImportAccountFromBip39Words(
        name: String,
        mnemonic: Array<String>,
        password: String
    ) {
        safeRunAsIO {
            val acc = BIP39Mnemonics.of(mnemonic).toAccount { entropy, bytes ->
                io.hotmoka.node.Accounts.of(
                    entropy,
                    bytes
                )
            }
            ensureConnected()
            val balance = getBalance(acc.reference)
            val keys = acc.keys(password, signatureAlgorithmOfNewAccounts)
            val importedAccount = Account(
                acc.reference,
                name,
                acc,
                publicKeyBase64Encoded(keys),
                balance,
                true,
                Coin.PANAREA
            )
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
            val entropy = Entropies.random()
            val keys = entropy.keys(password, signatureAlgorithmOfNewAccounts)
            val publicKeyBase58 = publicKeyBase58Encoded(keys)
            val publicKeyBase64 = publicKeyBase64Encoded(keys)
            Log.d(TAG, "Created public key $publicKeyBase58")

            // it is not a fully functional account yet, since it misses the reference
            val newAccount = Account(
                null,
                publicKeyBase58,
                entropy,
                publicKeyBase64,
                BigInteger.ZERO,
                false,
                Coin.PANAREA
            )
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
    fun requestPayment(
        payer: Account,
        destination: StorageReference,
        amount: BigInteger,
        passwordOfPayer: String
    ) {
        safeRunAsIO {
            checkPassword(payer, passwordOfPayer)
            val keys = getKeysOf(payer, passwordOfPayer)
            ensureConnected()
            var savedRequests: Array<TransactionRequest<*>>? = null
            SendCoinsHelpers.of(node)
                .sendFromPayer(payer.reference, keys, destination, amount, {}, { requests ->
                    savedRequests = requests
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
        val hasher = HashingAlgorithms.sha256().getHasher(TransactionRequest<*>::toByteArray)
        requests?.let {
            return it.map { request -> TransactionReferences.of(hasher.hash(request)) }
        }

        return emptyList()
    }

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
    fun requestPaymentToPublicKey(
        payer: Account,
        publicKey: String,
        amount: BigInteger,
        anonymous: Boolean,
        password: String
    ) {
        safeRunAsIO {
            checkPassword(payer, password)
            val keysOfPayer = getKeysOf(payer, password)
            ensureConnected()
            var transactions: List<TransactionReference>? = null
            val destination: StorageReference

            if (anonymous) {
                destination = AccountCreationHelpers.of(node).paidToLedgerBy(
                    payer.reference,
                    keysOfPayer,
                    signatureAlgorithmOfNewAccounts.publicKeyFromEncoding(
                        Base58.fromBase58String(publicKey)
                    ),
                    amount,
                    {},
                    { requests -> transactions = toTransactions(requests) }
                )
                Log.i(TAG, "Paid $amount anonymously to key $publicKey [destination is $destination]")
            } else {
                destination = AccountCreationHelpers.of(node).paidBy(
                    payer.reference,
                    keysOfPayer,
                    signatureAlgorithmOfNewAccounts,
                    signatureAlgorithmOfNewAccounts.publicKeyFromEncoding(
                        Base58.fromBase58String(publicKey)
                    ),
                    amount,
                    {},
                    { requests -> transactions = toTransactions(requests) }
                )
                Log.i(TAG, "Paid $amount to key $publicKey [destination is $destination]")
            }

            // we reload the accounts, since the payer will see its balance decrease
            val accounts = reloadAccounts()
            mvc.model.setAccounts(accounts)
            mainScope.launch {
                mvc.view?.onPaymentCompleted(
                    payer,
                    destination,
                    publicKey,
                    amount,
                    anonymous,
                    transactions!!
                )
            }
        }
    }

    fun requestPaymentFromFaucet(
        faucet: Faucet,
        destination: StorageReference,
        amount: BigInteger
    ) {
        safeRunAsIO {
            ensureConnected()
            var transactions: List<TransactionReference>? = null

            SendCoinsHelpers.of(node).sendFromFaucet(destination, amount, {}, { requests ->
                transactions = toTransactions(requests)
            })
            Log.i(TAG, "Paid $amount from ${faucet.name} to account $destination")

            // we reload the accounts, since the payer will see its balance decrease
            val accounts = reloadAccounts()
            mvc.model.setAccounts(accounts)

            mainScope.launch {
                mvc.view?.onPaymentCompleted(
                    faucet,
                    destination,
                    null,
                    amount,
                    false,
                    transactions!!
                )
            }
        }
    }

    private fun getKeysOf(account: Account, password: String): KeyPair {
        return account.entropy.keys(password, signatureAlgorithmOfNewAccounts)
    }

    private fun publicKeyBase64Encoded(keys: KeyPair): String {
        return Base64.encodeToString(
            signatureAlgorithmOfNewAccounts.encodingOf(keys.public),
            Base64.NO_WRAP
        )
    }

    private fun publicKeyBase58Encoded(keys: KeyPair): String {
        return Base58.toBase58String(signatureAlgorithmOfNewAccounts.encodingOf(keys.public))
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
        return Accounts(
            mvc,
            getFaucet(),
            getMaxFaucet(),
            this::getBalance,
            this::getReferenceFromAccountsLedger
        )
    }

    private fun createNewAccount(
        creator: (PublicKey) -> StorageReference,
        name: String,
        password: String,
        balance: BigInteger
    ) {
        safeRunAsIO {
            ensureConnected()
            val entropy = Entropies.random()
            val keys = entropy.keys(password, signatureAlgorithmOfNewAccounts)

            // create an account with the public key
            val reference = creator(keys.public)
            Log.i(TAG, "Created new account $reference")

            // currently, the progressive number of the created accounts will be #0,
            // but we better check against future unexpected changes in the server's behavior
            if (reference.progressive.signum() != 0)
                throw IllegalStateException("I can only deal with new accounts whose progressive number is 0")

            // we force a reload of the accounts, so that their balances reflect the changes;
            // this is important, in particular, to update the balance of the payer
            val accounts = reloadAccounts()
            val newAccount = Account(
                reference,
                name,
                entropy,
                publicKeyBase64Encoded(keys),
                balance,
                true,
                Coin.PANAREA
            )
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
        val hasFaucet = node.runInstanceMethodCallTransaction(
            TransactionRequests.instanceViewMethodCall(
                manifest, _100_000, takamakaCode, MethodSignatures.ALLOWS_UNSIGNED_FAUCET, manifest
            )
        )
            .orElseThrow { UnexpectedVoidMethodException(MethodSignatures.ALLOWS_UNSIGNED_FAUCET) }
            .asReturnedBoolean(MethodSignatures.ALLOWS_UNSIGNED_FAUCET, ::UnexpectedValueException)

        return if (hasFaucet)
            getGameteCached()
        else
            null
    }

    private fun getMaxFaucet(): BigInteger {
        val manifest = getManifestCached()

        return node.runInstanceMethodCallTransaction(
            TransactionRequests.instanceViewMethodCall(
                manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_FAUCET, getGameteCached()
            )
        )
            .orElseThrow { UnexpectedVoidMethodException(MethodSignatures.GET_MAX_FAUCET) }
            .asReturnedBigInteger(MethodSignatures.GET_MAX_FAUCET, ::UnexpectedValueException)
    }

    private fun getBalance(reference: StorageReference): BigInteger {
        val manifest = getManifestCached()

        return node.runInstanceMethodCallTransaction(
            TransactionRequests.instanceViewMethodCall(
                manifest, _100_000, takamakaCode, MethodSignatures.BALANCE, reference
            )
        )
            .orElseThrow { UnexpectedVoidMethodException(MethodSignatures.BALANCE) }
            .asReturnedBigInteger(MethodSignatures.BALANCE, ::UnexpectedValueException)
    }

    private fun getERC20SnapshotOf(reference: StorageReference): StorageReference {
        // we use the manifest as caller, since the call is free of charge
        val manifest = getManifestCached()

        val erc20ViewSnapshot = MethodSignatures.ofNonVoid(
            IERC20View,
            "snapshot",
            IERC20View
        )

        return node.runInstanceMethodCallTransaction(
            TransactionRequests.instanceViewMethodCall(
                manifest, _100_000, takamakaCode, erc20ViewSnapshot, reference
            )
        )
            .orElseThrow { UnexpectedVoidMethodException(erc20ViewSnapshot) }
            .asReturnedReference(erc20ViewSnapshot, ::UnexpectedValueException)
    }

    private fun getErc20Size(reference: StorageReference): Int {
        // we use the manifest as caller, since the call is free of charge
        val manifest = getManifestCached()

        val erc20ViewSize = MethodSignatures.ofNonVoid(
            IERC20View,
            "size",
            StorageTypes.INT
        )

        return node.runInstanceMethodCallTransaction(
            TransactionRequests.instanceViewMethodCall(
                manifest, _100_000, takamakaCode, erc20ViewSize, reference
            )
        )
            .orElseThrow { UnexpectedVoidMethodException(erc20ViewSize) }
            .asReturnedInt(erc20ViewSize, ::UnexpectedValueException)
    }

    private fun getReferenceFromAccountsLedger(publicKey: String): StorageReference? {
        Log.d(TAG, "Looking in the accounts ledger for the account bound to $publicKey")
        val manifest = getManifestCached()
        val ledger = getAccountsLedgerCached()

        val result = node.runInstanceMethodCallTransaction(
            TransactionRequests.instanceViewMethodCall(
                manifest, _100_000, takamakaCode,
                MethodSignatures.GET_FROM_ACCOUNTS_LEDGER,
                ledger,
                StorageValues.stringOf(publicKey)
            )
        )
            .orElseThrow { UnexpectedVoidMethodException(MethodSignatures.GET_FROM_ACCOUNTS_LEDGER) }
            as? StorageReference // it might also be a NullValue, that as? transforms into null

        Log.d(TAG, "Found $result bound in the accounts ledger to $publicKey")

        return result
    }

    private fun getPublicKey(reference: StorageReference): String {
        return node.runInstanceMethodCallTransaction(
            TransactionRequests.instanceViewMethodCall(
                reference, _100_000, takamakaCode, MethodSignatures.PUBLIC_KEY, reference
            )
        )
            .orElseThrow { UnexpectedVoidMethodException(MethodSignatures.PUBLIC_KEY) }
            .asReturnedString(MethodSignatures.PUBLIC_KEY, ::UnexpectedValueException)
    }

    private fun safeRunAsIO(task: () -> Unit) {
        working.incrementAndGet()
        mainScope.launch { mvc.view?.onBackgroundStart() }

        ioScope.launch {
            try {
                task.invoke()
            }
            catch (t: TimeoutException) {
                Log.w(TAG, "The operation timed-out")
                mainScope.launch { mvc.view?.notifyUser(mvc.getString(R.string.operation_timeout)) }
            }
            catch (t: Throwable) {
                Log.w(TAG, "Background IO action failed", t)
                mainScope.launch { mvc.view?.notifyUser(t.toString()) }
            }
            finally {
                working.decrementAndGet()
                mainScope.launch { mvc.view?.onBackgroundEnd() }
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
        val gamete = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall(
            manifest, _100_000, takamakaCode, MethodSignatures.GET_GAMETE, manifest
        ))
            .orElseThrow { UnexpectedVoidMethodException(MethodSignatures.GET_GAMETE) }
            .asReturnedReference(MethodSignatures.GET_GAMETE, ::UnexpectedValueException)

        mvc.model.setGamete(gamete)
        return gamete
    }

    private fun getAccountsLedgerCached(): StorageReference {
        mvc.model.getAccountsLedger()?.let {
            return it
        }

        val manifest = getManifestCached()
        val accountsLedger = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall(
            manifest, _100_000, takamakaCode, MethodSignatures.GET_ACCOUNTS_LEDGER, manifest
        ))
            .orElseThrow { UnexpectedVoidMethodException(MethodSignatures.GET_ACCOUNTS_LEDGER) }
            .asReturnedReference(MethodSignatures.GET_ACCOUNTS_LEDGER, ::UnexpectedValueException)

        mvc.model.setAccountsLedger(accountsLedger)
        return accountsLedger
    }
}