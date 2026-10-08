package com.paisa.najarine.data.repository

import androidx.room.withTransaction
import com.paisa.najarine.data.local.PaisaDatabase
import com.paisa.najarine.data.local.SyncOutboxEntity
import com.paisa.najarine.data.local.TombstoneEntity
import com.paisa.najarine.data.local.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class TransactionRepository(private val database: PaisaDatabase) {

    fun getTransactions(workspaceId: String): Flow<List<TransactionEntity>> =
        database.transactionDao().getTransactionsByWorkspace(workspaceId)

    fun getTransactionsForWallet(walletId: String): Flow<List<TransactionEntity>> =
        database.transactionDao().getTransactionsByWallet(walletId)

    suspend fun recordIncome(
        workspaceId: String,
        walletId: String,
        amount: Double,
        category: String,
        note: String = "",
        dateMillis: Long = System.currentTimeMillis(),
        tags: String = "",
        id: String = UUID.randomUUID().toString()
    ): TransactionEntity {
        val now = System.currentTimeMillis()
        val tx = TransactionEntity(
            id = id,
            workspaceId = workspaceId,
            walletId = walletId,
            toWalletId = null,
            type = "INCOME",
            amount = amount,
            category = category,
            note = note,
            dateMillis = dateMillis,
            tags = tags,
            updatedAt = now
        )
        database.withTransaction {
            database.transactionDao().insertTransaction(tx)
            database.walletDao().adjustBalance(walletId, amount, now)
            database.tombstoneDao().deleteTombstone(tx.id)

            // Enqueue outbox operations atomically
            database.syncOutboxDao().enqueue(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "TRANSACTION",
                    entityId = tx.id,
                    action = "UPSERT",
                    payload = "",
                    createdAt = now
                )
            )
            database.syncOutboxDao().enqueue(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "WALLET",
                    entityId = walletId,
                    action = "UPSERT",
                    payload = "",
                    createdAt = now
                )
            )
        }
        return tx
    }

    suspend fun recordExpense(
        workspaceId: String,
        walletId: String,
        amount: Double,
        category: String,
        fee: Double = 0.0,
        note: String = "",
        dateMillis: Long = System.currentTimeMillis(),
        tags: String = "",
        receiptImageUri: String? = null,
        id: String = UUID.randomUUID().toString()
    ): TransactionEntity {
        val now = System.currentTimeMillis()
        val tx = TransactionEntity(
            id = id,
            workspaceId = workspaceId,
            walletId = walletId,
            toWalletId = null,
            type = "EXPENSE",
            amount = amount,
            fee = fee,
            category = category,
            note = note,
            dateMillis = dateMillis,
            tags = tags,
            receiptImageUri = receiptImageUri,
            updatedAt = now
        )
        database.withTransaction {
            database.transactionDao().insertTransaction(tx)
            database.walletDao().adjustBalance(walletId, -(amount + fee), now)
            database.tombstoneDao().deleteTombstone(tx.id)

            database.syncOutboxDao().enqueue(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "TRANSACTION",
                    entityId = tx.id,
                    action = "UPSERT",
                    payload = "",
                    createdAt = now
                )
            )
            database.syncOutboxDao().enqueue(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "WALLET",
                    entityId = walletId,
                    action = "UPSERT",
                    payload = "",
                    createdAt = now
                )
            )
        }
        return tx
    }

    suspend fun recordTransfer(
        workspaceId: String,
        sourceWalletId: String,
        destinationWalletId: String,
        amount: Double,
        fee: Double = 0.0,
        note: String = "",
        dateMillis: Long = System.currentTimeMillis(),
        id: String = UUID.randomUUID().toString()
    ): TransactionEntity {
        val now = System.currentTimeMillis()
        val tx = TransactionEntity(
            id = id,
            workspaceId = workspaceId,
            walletId = sourceWalletId,
            toWalletId = destinationWalletId,
            type = "TRANSFER",
            amount = amount,
            fee = fee,
            category = "Transfer",
            note = note,
            dateMillis = dateMillis,
            updatedAt = now
        )
        database.withTransaction {
            database.transactionDao().insertTransaction(tx)
            database.walletDao().adjustBalance(sourceWalletId, -(amount + fee), now)
            database.walletDao().adjustBalance(destinationWalletId, amount, now)
            database.tombstoneDao().deleteTombstone(tx.id)

            database.syncOutboxDao().enqueue(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "TRANSACTION",
                    entityId = tx.id,
                    action = "UPSERT",
                    payload = "",
                    createdAt = now
                )
            )
            database.syncOutboxDao().enqueue(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "WALLET",
                    entityId = sourceWalletId,
                    action = "UPSERT",
                    payload = "",
                    createdAt = now
                )
            )
            database.syncOutboxDao().enqueue(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "WALLET",
                    entityId = destinationWalletId,
                    action = "UPSERT",
                    payload = "",
                    createdAt = now
                )
            )
        }
        return tx
    }

    suspend fun editTransaction(
        oldTx: TransactionEntity,
        newTx: TransactionEntity
    ) {
        val now = System.currentTimeMillis()
        val updatedTx = newTx.copy(updatedAt = now)

        database.withTransaction {
            // 1. Revert previous transaction impact on old wallets
            when (oldTx.type) {
                "INCOME" -> database.walletDao().adjustBalance(oldTx.walletId, -oldTx.amount, now)
                "EXPENSE" -> database.walletDao().adjustBalance(oldTx.walletId, (oldTx.amount + oldTx.fee), now)
                "TRANSFER" -> {
                    database.walletDao().adjustBalance(oldTx.walletId, (oldTx.amount + oldTx.fee), now)
                    oldTx.toWalletId?.let { destId ->
                        database.walletDao().adjustBalance(destId, -oldTx.amount, now)
                    }
                }
            }

            // 2. Apply new transaction impact on new wallets
            when (updatedTx.type) {
                "INCOME" -> database.walletDao().adjustBalance(updatedTx.walletId, updatedTx.amount, now)
                "EXPENSE" -> database.walletDao().adjustBalance(updatedTx.walletId, -(updatedTx.amount + updatedTx.fee), now)
                "TRANSFER" -> {
                    database.walletDao().adjustBalance(updatedTx.walletId, -(updatedTx.amount + updatedTx.fee), now)
                    updatedTx.toWalletId?.let { destId ->
                        database.walletDao().adjustBalance(destId, updatedTx.amount, now)
                    }
                }
            }

            // 3. Update transaction record
            database.transactionDao().insertTransaction(updatedTx)

            // 4. Enqueue sync outbox mutations
            database.syncOutboxDao().enqueue(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "TRANSACTION",
                    entityId = updatedTx.id,
                    action = "UPSERT",
                    payload = "",
                    createdAt = now
                )
            )
            val affectedWallets = setOfNotNull(oldTx.walletId, oldTx.toWalletId, updatedTx.walletId, updatedTx.toWalletId)
            for (wId in affectedWallets) {
                database.syncOutboxDao().enqueue(
                    SyncOutboxEntity(
                        id = UUID.randomUUID().toString(),
                        entityType = "WALLET",
                        entityId = wId,
                        action = "UPSERT",
                        payload = "",
                        createdAt = now
                    )
                )
            }
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            database.transactionDao().deleteTransaction(transaction.id)

            // Reverse balance effect atomically
            when (transaction.type) {
                "INCOME" -> database.walletDao().adjustBalance(transaction.walletId, -transaction.amount, now)
                "EXPENSE" -> database.walletDao().adjustBalance(transaction.walletId, (transaction.amount + transaction.fee), now)
                "TRANSFER" -> {
                    database.walletDao().adjustBalance(transaction.walletId, (transaction.amount + transaction.fee), now)
                    transaction.toWalletId?.let { destId ->
                        database.walletDao().adjustBalance(destId, -transaction.amount, now)
                    }
                }
            }

            // Insert tombstone so incoming snapshots cannot re-create this transaction
            database.tombstoneDao().insertTombstone(
                TombstoneEntity(
                    entityId = transaction.id,
                    entityType = "TRANSACTION",
                    deletedAt = now
                )
            )

            // Enqueue outbox delete
            database.syncOutboxDao().enqueue(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "TRANSACTION",
                    entityId = transaction.id,
                    action = "DELETE",
                    payload = "",
                    createdAt = now
                )
            )

            // Enqueue wallet balance sync
            database.syncOutboxDao().enqueue(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "WALLET",
                    entityId = transaction.walletId,
                    action = "UPSERT",
                    payload = "",
                    createdAt = now
                )
            )
            transaction.toWalletId?.let { destId ->
                database.syncOutboxDao().enqueue(
                    SyncOutboxEntity(
                        id = UUID.randomUUID().toString(),
                        entityType = "WALLET",
                        entityId = destId,
                        action = "UPSERT",
                        payload = "",
                        createdAt = now
                    )
                )
            }
        }
    }
}

