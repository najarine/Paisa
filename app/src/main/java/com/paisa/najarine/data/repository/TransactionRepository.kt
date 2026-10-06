package com.paisa.najarine.data.repository

import androidx.room.withTransaction
import com.paisa.najarine.data.local.PaisaDatabase
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
        tags: String = ""
    ) {
        val tx = TransactionEntity(
            id = UUID.randomUUID().toString(),
            workspaceId = workspaceId,
            walletId = walletId,
            toWalletId = null,
            type = "INCOME",
            amount = amount,
            category = category,
            note = note,
            dateMillis = dateMillis,
            tags = tags
        )
        database.withTransaction {
            database.transactionDao().insertTransaction(tx)
            database.walletDao().adjustBalance(walletId, amount)
        }
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
        receiptImageUri: String? = null
    ) {
        val tx = TransactionEntity(
            id = UUID.randomUUID().toString(),
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
            receiptImageUri = receiptImageUri
        )
        database.withTransaction {
            database.transactionDao().insertTransaction(tx)
            database.walletDao().adjustBalance(walletId, -(amount + fee))
        }
    }

    suspend fun recordTransfer(
        workspaceId: String,
        sourceWalletId: String,
        destinationWalletId: String,
        amount: Double,
        fee: Double = 0.0,
        note: String = "",
        dateMillis: Long = System.currentTimeMillis()
    ) {
        val tx = TransactionEntity(
            id = UUID.randomUUID().toString(),
            workspaceId = workspaceId,
            walletId = sourceWalletId,
            toWalletId = destinationWalletId,
            type = "TRANSFER",
            amount = amount,
            fee = fee,
            category = "Transfer",
            note = note,
            dateMillis = dateMillis
        )
        database.withTransaction {
            database.transactionDao().insertTransaction(tx)
            // Deterministic balance adjustments:
            // Transfers must not count as income or expense
            database.walletDao().adjustBalance(sourceWalletId, -(amount + fee))
            database.walletDao().adjustBalance(destinationWalletId, amount)
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        database.withTransaction {
            database.transactionDao().deleteTransaction(transaction.id)
            // Reverse balance effect
            when (transaction.type) {
                "INCOME" -> database.walletDao().adjustBalance(transaction.walletId, -transaction.amount)
                "EXPENSE" -> database.walletDao().adjustBalance(transaction.walletId, (transaction.amount + transaction.fee))
                "TRANSFER" -> {
                    database.walletDao().adjustBalance(transaction.walletId, (transaction.amount + transaction.fee))
                    transaction.toWalletId?.let { destId ->
                        database.walletDao().adjustBalance(destId, -transaction.amount)
                    }
                }
            }
        }
    }
}
