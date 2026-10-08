package com.paisa.najarine

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.paisa.najarine.data.local.PaisaDatabase
import com.paisa.najarine.data.local.TransactionEntity
import com.paisa.najarine.data.local.WalletEntity
import com.paisa.najarine.data.repository.TransactionRepository
import com.paisa.najarine.data.repository.WalletRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PaisaOfflineArchitectureTest {

    private lateinit var database: PaisaDatabase
    private lateinit var walletRepo: WalletRepository
    private lateinit var txRepo: TransactionRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PaisaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        walletRepo = WalletRepository(database)
        txRepo = TransactionRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testOfflineTransactionCreationAndOutboxQueue() = runBlocking {
        // Create initial wallet
        val wallet = walletRepo.createWallet(
            workspaceId = "ws_test",
            name = "Bkash Main",
            type = "MFS",
            institutionId = "bkash",
            accountNumber = "01700000000",
            initialBalance = 1000.0
        )

        // Record income offline
        val tx = txRepo.recordIncome(
            workspaceId = "ws_test",
            walletId = wallet.id,
            amount = 500.0,
            category = "Salary",
            note = "Freelance bonus"
        )

        // 1. Verify Room is local source of truth
        val updatedWallet = walletRepo.getWalletById(wallet.id)
        assertNotNull(updatedWallet)
        assertEquals(1500.0, updatedWallet!!.balance, 0.001)

        val retrievedTx = database.transactionDao().getTransactionById(tx.id)
        assertNotNull(retrievedTx)
        assertEquals(500.0, retrievedTx!!.amount, 0.001)

        // 2. Verify durable sync outbox queue entries were created
        val pendingOutbox = database.syncOutboxDao().getAllPending()
        assertTrue("Outbox should have recorded mutations", pendingOutbox.isNotEmpty())
        assertTrue(pendingOutbox.any { it.entityId == tx.id && it.action == "UPSERT" && it.entityType == "TRANSACTION" })
        assertTrue(pendingOutbox.any { it.entityId == wallet.id && it.action == "UPSERT" && it.entityType == "WALLET" })
    }

    @Test
    fun testOfflineDeletionAndTombstone() = runBlocking {
        val wallet = walletRepo.createWallet(
            workspaceId = "ws_test",
            name = "Cash",
            type = "CASH",
            institutionId = "cash",
            accountNumber = "",
            initialBalance = 2000.0
        )

        val expenseTx = txRepo.recordExpense(
            workspaceId = "ws_test",
            walletId = wallet.id,
            amount = 300.0,
            category = "Food",
            fee = 0.0
        )

        assertEquals(1700.0, walletRepo.getWalletById(wallet.id)!!.balance, 0.001)

        // Delete transaction offline
        txRepo.deleteTransaction(expenseTx)

        // 1. Verify balance reversed atomically
        val finalWallet = walletRepo.getWalletById(wallet.id)!!
        assertEquals(2000.0, finalWallet.balance, 0.001)

        // 2. Verify transaction is removed from Room
        assertNull(database.transactionDao().getTransactionById(expenseTx.id))

        // 3. Verify tombstone created to prevent resurrection
        assertTrue(database.tombstoneDao().isTombstoned(expenseTx.id))

        // 4. Verify outbox delete operation enqueued
        val outbox = database.syncOutboxDao().getAllPending()
        assertTrue(outbox.any { it.entityId == expenseTx.id && it.action == "DELETE" && it.entityType == "TRANSACTION" })
    }

    @Test
    fun testTransferAtomicityAcrossBothWallets() = runBlocking {
        val sourceWallet = walletRepo.createWallet(
            workspaceId = "ws_test",
            name = "Bank",
            type = "BANK",
            institutionId = "ibbl",
            accountNumber = "123",
            initialBalance = 10000.0
        )
        val destWallet = walletRepo.createWallet(
            workspaceId = "ws_test",
            name = "Nagad",
            type = "MFS",
            institutionId = "nagad",
            accountNumber = "456",
            initialBalance = 1000.0
        )

        val transferTx = txRepo.recordTransfer(
            workspaceId = "ws_test",
            sourceWalletId = sourceWallet.id,
            destinationWalletId = destWallet.id,
            amount = 2500.0,
            fee = 15.0,
            note = "Bank to Nagad"
        )

        val updatedSource = walletRepo.getWalletById(sourceWallet.id)!!
        val updatedDest = walletRepo.getWalletById(destWallet.id)!!

        // Source = 10000 - (2500 + 15) = 7485
        assertEquals(7485.0, updatedSource.balance, 0.001)
        // Dest = 1000 + 2500 = 3500
        assertEquals(3500.0, updatedDest.balance, 0.001)

        // Reversal upon delete
        txRepo.deleteTransaction(transferTx)
        val revertedSource = walletRepo.getWalletById(sourceWallet.id)!!
        val revertedDest = walletRepo.getWalletById(destWallet.id)!!

        assertEquals(10000.0, revertedSource.balance, 0.001)
        assertEquals(1000.0, revertedDest.balance, 0.001)
    }

    @Test
    fun testEditTransactionAtomicityAndDifferentialBalance() = runBlocking {
        val wallet = walletRepo.createWallet(
            workspaceId = "ws_test",
            name = "Primary",
            type = "BANK",
            institutionId = "brac",
            accountNumber = "",
            initialBalance = 5000.0
        )

        val initialTx = txRepo.recordExpense(
            workspaceId = "ws_test",
            walletId = wallet.id,
            amount = 1000.0,
            category = "Shopping"
        )
        assertEquals(4000.0, walletRepo.getWalletById(wallet.id)!!.balance, 0.001)

        // Edit amount from 1000 to 1500
        val editedTx = initialTx.copy(amount = 1500.0)
        txRepo.editTransaction(initialTx, editedTx)

        // Balance should be 5000 - 1500 = 3500
        assertEquals(3500.0, walletRepo.getWalletById(wallet.id)!!.balance, 0.001)
        assertEquals(1500.0, database.transactionDao().getTransactionById(initialTx.id)!!.amount, 0.001)
    }

    @Test
    fun testCloudLocalConflictHandlingTombstonesAndTimestamps() = runBlocking {
        val txId = UUID.randomUUID().toString()

        // Insert tombstone indicating user deleted txId locally
        database.tombstoneDao().insertTombstone(
            com.paisa.najarine.data.local.TombstoneEntity(
                entityId = txId,
                entityType = "TRANSACTION",
                deletedAt = System.currentTimeMillis()
            )
        )

        // Check that isTombstoned returns true so snapshot listeners will discard it
        assertTrue(database.tombstoneDao().isTombstoned(txId))

        // Create transaction with a newer local timestamp
        val wallet = walletRepo.createWallet(
            workspaceId = "ws_test",
            name = "Local Wallet",
            type = "CASH",
            institutionId = "cash",
            accountNumber = "",
            initialBalance = 500.0
        )
        val localTx = txRepo.recordIncome(
            workspaceId = "ws_test",
            walletId = wallet.id,
            amount = 100.0,
            category = "Gift",
            dateMillis = 1000L
        )

        // Verify local transaction has updatedAt assigned
        assertTrue(localTx.updatedAt > 0)
    }

    @Test
    fun testDuplicateRetryPreventionAndOutboxDrain() = runBlocking {
        val outboxItem1 = com.paisa.najarine.data.local.SyncOutboxEntity(
            id = "outbox_1",
            entityType = "TRANSACTION",
            entityId = "tx_101",
            action = "UPSERT",
            createdAt = System.currentTimeMillis()
        )
        database.syncOutboxDao().enqueue(outboxItem1)

        // Same ID re-enqueued should replace cleanly without duplicates
        database.syncOutboxDao().enqueue(outboxItem1)
        assertEquals(1, database.syncOutboxDao().getAllPending().size)

        // Drain / complete item
        database.syncOutboxDao().deleteById(outboxItem1.id)
        assertEquals(0, database.syncOutboxDao().getAllPending().size)
    }
}
