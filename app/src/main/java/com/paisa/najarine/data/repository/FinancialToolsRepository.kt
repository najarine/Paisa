package com.paisa.najarine.data.repository

import com.paisa.najarine.data.local.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class FinancialToolsRepository(private val database: PaisaDatabase) {

    // Budgets
    fun getBudgets(workspaceId: String): Flow<List<BudgetEntity>> =
        database.budgetDao().getBudgets(workspaceId)

    suspend fun saveBudget(budget: BudgetEntity) =
        database.budgetDao().insertBudget(budget)

    suspend fun deleteBudget(id: String) =
        database.budgetDao().deleteBudget(id)

    // Goals / Vaults
    fun getGoals(workspaceId: String): Flow<List<GoalVaultEntity>> =
        database.goalVaultDao().getGoals(workspaceId)

    suspend fun saveGoal(goal: GoalVaultEntity) =
        database.goalVaultDao().insertGoal(goal)

    suspend fun contributeToGoal(id: String, amount: Double) =
        database.goalVaultDao().contributeToGoal(id, amount)

    suspend fun deleteGoal(id: String) =
        database.goalVaultDao().deleteGoal(id)

    // Bills & Subscriptions
    fun getBills(workspaceId: String): Flow<List<BillSubscriptionEntity>> =
        database.billDao().getBills(workspaceId)

    suspend fun saveBill(bill: BillSubscriptionEntity) =
        database.billDao().insertBill(bill)

    suspend fun toggleBillPaid(id: String, isPaid: Boolean) =
        database.billDao().togglePaid(id, isPaid)

    suspend fun deleteBill(id: String) =
        database.billDao().deleteBill(id)

    // Debt / Dena-Pona
    fun getDebts(workspaceId: String): Flow<List<DebtEntity>> =
        database.debtDao().getDebts(workspaceId)

    suspend fun saveDebt(debt: DebtEntity) =
        database.debtDao().insertDebt(debt)

    suspend fun toggleDebtSettled(id: String, isSettled: Boolean) =
        database.debtDao().toggleSettled(id, isSettled)

    suspend fun deleteDebt(id: String) =
        database.debtDao().deleteDebt(id)

    // Customer Ledger (Baki-Joma)
    fun getCustomerLedger(workspaceId: String): Flow<List<CustomerLedgerEntity>> =
        database.customerLedgerDao().getLedgerEntries(workspaceId)

    suspend fun saveCustomerLedgerEntry(entry: CustomerLedgerEntity) =
        database.customerLedgerDao().insertEntry(entry)

    suspend fun deleteCustomerLedgerEntry(id: String) =
        database.customerLedgerDao().deleteEntry(id)

    // Mess Manager
    fun getMessEntries(workspaceId: String): Flow<List<MessEntryEntity>> =
        database.messDao().getMessEntries(workspaceId)

    suspend fun saveMessEntry(entry: MessEntryEntity) =
        database.messDao().insertEntry(entry)

    suspend fun deleteMessEntry(id: String) =
        database.messDao().deleteEntry(id)

    // Assets
    fun getAssets(workspaceId: String): Flow<List<AssetEntity>> =
        database.assetDao().getAssets(workspaceId)

    suspend fun saveAsset(asset: AssetEntity) =
        database.assetDao().insertAsset(asset)

    suspend fun deleteAsset(id: String) =
        database.assetDao().deleteAsset(id)
}
