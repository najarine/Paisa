package com.paisa.najarine.data.repository

import com.paisa.najarine.data.local.PaisaDatabase
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
            updatedAt = System.currentTimeMillis()
        )
        database.walletDao().insertWallet(wallet)
        return wallet
    }

    suspend fun updateWallet(wallet: WalletEntity) {
        database.walletDao().updateWallet(wallet)
    }

    suspend fun deleteWallet(walletId: String) {
        database.walletDao().deleteWallet(walletId)
    }

    suspend fun adjustBalance(walletId: String, amountDelta: Double) {
        database.walletDao().adjustBalance(walletId, amountDelta)
    }
}
