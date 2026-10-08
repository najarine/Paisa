package com.paisa.najarine.data.repository

import androidx.room.withTransaction
import com.paisa.najarine.data.local.PaisaDatabase
import com.paisa.najarine.data.local.SyncOutboxEntity
import com.paisa.najarine.data.local.TombstoneEntity
import com.paisa.najarine.data.local.WalletEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class WalletRepository(private val database: PaisaDatabase) {

    fun getWallets(workspaceId: String): Flow<List<WalletEntity>> =
        database.walletDao().getWalletsByWorkspace(workspaceId)

    suspend fun getWalletById(id: String): WalletEntity? =
        database.walletDao().getWalletById(id)

    suspend fun createWallet(
        workspaceId: String,
        name: String,
        type: String,
        institutionId: String,
        accountNumber: String,
        initialBalance: Double,
        creditLimit: Double = 0.0,
        currencyCode: String = "BDT",
        colorHex: String = "#0D9488",
        isExcludedFromTotal: Boolean = false,
        note: String = ""
    ): WalletEntity {
        val now = System.currentTimeMillis()
        val wallet = WalletEntity(
            id = UUID.randomUUID().toString(),
            workspaceId = workspaceId,
            name = name,
            type = type,
            institutionId = institutionId,
            accountNumber = accountNumber,
            balance = initialBalance,
            creditLimit = creditLimit,
            currencyCode = currencyCode,
            colorHex = colorHex,
            isExcludedFromTotal = isExcludedFromTotal,
            note = note,
            updatedAt = now
        )
        database.withTransaction {
            database.walletDao().insertWallet(wallet)
            // Remove any old tombstone if reusing ID
            database.tombstoneDao().deleteTombstone(wallet.id)
            // Enqueue outbox mutation
            database.syncOutboxDao().enqueue(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "WALLET",
                    entityId = wallet.id,
                    action = "UPSERT",
                    payload = "",
                    createdAt = now
                )
            )
        }
        return wallet
    }

    suspend fun updateWallet(wallet: WalletEntity) {
        val now = System.currentTimeMillis()
        val updated = wallet.copy(updatedAt = now)
        database.withTransaction {
            database.walletDao().updateWallet(updated)
            database.syncOutboxDao().enqueue(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "WALLET",
                    entityId = updated.id,
                    action = "UPSERT",
                    payload = "",
                    createdAt = now
                )
            )
        }
    }

    suspend fun deleteWallet(walletId: String) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            database.walletDao().deleteWallet(walletId)
            // Insert tombstone so incoming snapshots cannot re-create this wallet
            database.tombstoneDao().insertTombstone(
                TombstoneEntity(
                    entityId = walletId,
                    entityType = "WALLET",
                    deletedAt = now
                )
            )
            // Enqueue outbox delete
            database.syncOutboxDao().enqueue(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "WALLET",
                    entityId = walletId,
                    action = "DELETE",
                    payload = "",
                    createdAt = now
                )
            )
        }
    }

    suspend fun adjustBalance(walletId: String, amountDelta: Double) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            database.walletDao().adjustBalance(walletId, amountDelta, now)
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
    }
}

