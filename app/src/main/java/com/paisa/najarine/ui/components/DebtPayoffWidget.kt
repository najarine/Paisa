package com.paisa.najarine.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.data.local.DebtEntity
import com.paisa.najarine.data.local.GoalVaultEntity
import com.paisa.najarine.data.local.TransactionEntity
import com.paisa.najarine.data.local.WalletEntity
import com.paisa.najarine.ui.screens.accounting.displayCleanName
import com.paisa.najarine.ui.screens.accounting.isDebtPayoff
import com.paisa.najarine.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil
import kotlin.math.max

/**
 * Representation of an individual liability for the Payoff Widget.
 */
data class IndividualLiabilityItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val totalAmount: Double,
    val paidAmount: Double,
    val remainingAmount: Double,
    val progress: Float,
    val dueDateMillis: Long,
    val isCreditCard: Boolean,
    val priorityTag: String,
    val projectedPayoffMonths: Int
)

/**
 * Payoff Projection Summary.
 */
data class DebtPayoffProjection(
    val totalOriginalLiability: Double,
    val totalPaidSoFar: Double,
    val totalRemainingLiability: Double,
    val overallProgress: Float,
    val estimatedMonthlyCapacity: Double,
    val projectedMonthsToDebtFree: Int,
    val projectedDebtFreeDateLabel: String,
    val liabilities: List<IndividualLiabilityItem>
)

object DebtPayoffCalculator {
    private val BN_MONTHS = listOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    fun calculateProjection(
        debts: List<DebtEntity>,
        goals: List<GoalVaultEntity>,
        wallets: List<WalletEntity>,
        transactions: List<TransactionEntity>
    ): DebtPayoffProjection {
        val activeDebts = debts.filter { it.type == "DENA" && !it.isSettled }
        val debtGoals = goals.filter { it.isDebtPayoff }

        // Negative credit card balances represent active short-term card liabilities
        val cardDebts = wallets.filter { it.type == "CARD" && it.balance < 0 }.map { wallet ->
            val liabilityAmt = -wallet.balance
            IndividualLiabilityItem(
                id = "card_${wallet.id}",
                title = wallet.name,
                subtitle = "ক্রেডিট কার্ড বকেয়া",
                totalAmount = liabilityAmt,
                paidAmount = 0.0,
                remainingAmount = liabilityAmt,
                progress = 0f,
                dueDateMillis = System.currentTimeMillis() + (25L * 24 * 60 * 60 * 1000), // ~25 days billing cycle
                isCreditCard = true,
                priorityTag = "উচ্চ অগ্রাধিকার",
                projectedPayoffMonths = 1
            )
        }

        // Map personal debts and match with corresponding goal vaults if any
        val personalDebts = activeDebts.map { debt ->
            val matchedGoal = debtGoals.find { goal ->
                val clean = goal.displayCleanName.lowercase()
                val person = debt.personName.lowercase()
                clean.contains(person) || goal.targetAmount == debt.amount
            }
            val paidAmt = matchedGoal?.currentAmount ?: 0.0
            val remainingAmt = (debt.amount - paidAmt).coerceAtLeast(0.0)
            val progress = if (debt.amount > 0) ((paidAmt / debt.amount).toFloat()).coerceIn(0f, 1f) else 1f

            val daysRemaining = ((debt.dueDateMillis - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
            val priority = when {
                daysRemaining in 0..7 -> "জরুরি পরিশোধ"
                daysRemaining < 0 -> "মেয়াদোত্তীর্ণ"
                debt.amount < 10000.0 -> "স্নোবল (দ্রুত মুক্তি)"
                else -> "নিয়মিত"
            }

            IndividualLiabilityItem(
                id = debt.id,
                title = "${debt.personName} এর ঋণ",
                subtitle = if (debt.phoneNumber.isNotBlank()) debt.phoneNumber else "ব্যক্তিগত দেনা",
                totalAmount = debt.amount,
                paidAmount = paidAmt,
                remainingAmount = remainingAmt,
                progress = progress,
                dueDateMillis = debt.dueDateMillis,
                isCreditCard = false,
                priorityTag = priority,
                projectedPayoffMonths = 0
            )
        }

        val allLiabilities = (cardDebts + personalDebts).sortedBy { it.remainingAmount }

        val totalOriginal = allLiabilities.sumOf { it.totalAmount }
        val totalPaid = max(allLiabilities.sumOf { it.paidAmount }, debtGoals.sumOf { it.currentAmount })

        val totalRemaining = (totalOriginal - totalPaid).coerceAtLeast(0.0)
        val overallProgress = if (totalOriginal > 0) ((totalPaid / totalOriginal).toFloat()).coerceIn(0f, 1f) else 1f

        // Estimate user's monthly savings capacity from past 90 days of transactions
        val ninetyDaysAgo = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000)
        val recentTxs = transactions.filter { it.dateMillis >= ninetyDaysAgo }
        val recentIncome = recentTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
        val recentExpense = recentTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val netQuarterlySavings = (recentIncome - recentExpense).coerceAtLeast(0.0)
        val calculatedMonthlyCapacity = if (netQuarterlySavings > 0) {
            max(netQuarterlySavings / 3.0, 3000.0)
        } else {
            5000.0 // Reasonable default capacity for projection
        }

        val projectedMonths = if (calculatedMonthlyCapacity > 0 && totalRemaining > 0) {
            ceil(totalRemaining / calculatedMonthlyCapacity).toInt().coerceAtLeast(1)
        } else {
            0
        }

        val cal = Calendar.getInstance().apply {
            add(Calendar.MONTH, projectedMonths)
        }
        val targetMonthName = BN_MONTHS.getOrElse(cal.get(Calendar.MONTH)) { "" }
        val targetYear = cal.get(Calendar.YEAR)
        val dateLabel = if (projectedMonths > 0) {
            "$targetMonthName $targetYear ($projectedMonths মাসে)"
        } else {
            "সম্পূর্ণ ঋণমুক্ত"
        }

        // Update projected months for individual items
        val enrichedLiabilities = allLiabilities.map { item ->
            val months = if (calculatedMonthlyCapacity > 0 && item.remainingAmount > 0) {
                ceil(item.remainingAmount / (calculatedMonthlyCapacity * 0.7)).toInt().coerceAtLeast(1)
            } else 1
            item.copy(projectedPayoffMonths = months)
        }

        return DebtPayoffProjection(
            totalOriginalLiability = totalOriginal,
            totalPaidSoFar = totalPaid,
            totalRemainingLiability = totalRemaining,
            overallProgress = overallProgress,
            estimatedMonthlyCapacity = calculatedMonthlyCapacity,
            projectedMonthsToDebtFree = projectedMonths,
            projectedDebtFreeDateLabel = dateLabel,
            liabilities = enrichedLiabilities
        )
    }
}

/**
 * Modern Debt Payoff Dashboard Widget for HomeScreen and Reports.
 */
@Composable
fun DebtPayoffWidget(
    debts: List<DebtEntity>,
    goals: List<GoalVaultEntity>,
    wallets: List<WalletEntity>,
    transactions: List<TransactionEntity>,
    onNavigateToDebtGoals: () -> Unit,
    onNavigateToDenaPaona: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormat = remember {
        NumberFormat.getNumberInstance(Locale("bn", "BD")).apply {
            maximumFractionDigits = 0
        }
    }

    val projection = remember(debts, goals, wallets, transactions) {
        DebtPayoffCalculator.calculateProjection(debts, goals, wallets, transactions)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = projection.overallProgress,
        label = "debt_progress"
    )

    var isExpanded by remember { mutableStateOf(false) }

    // If completely debt-free and no liabilities exist
    if (projection.totalRemainingLiability <= 0.0 && projection.liabilities.isEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("debt_payoff_widget_free"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
            border = BorderStroke(1.dp, Color(0xFFBBF7D0))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "ঋণমুক্ত",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "আলহামদুলিল্লাহ! আপনি সম্পূর্ণ ঋণমুক্ত",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF166534)
                        )
                        Text(
                            text = "আপনার কোনো সক্রিয় দেনা বা কার্ডের বকেয়া নেই।",
                            fontSize = 11.sp,
                            color = Color(0xFF15803D)
                        )
                    }
                }

                Surface(
                    onClick = onNavigateToDebtGoals,
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFF86EFAC))
                ) {
                    Text(
                        text = "পরিকল্পনা",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF166534),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
        return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("debt_payoff_widget"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = PaisaSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // 1. Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PaisaExpenseLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingDown,
                            contentDescription = "ঋণ পরিশোধ",
                            tint = PaisaExpenseRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "দায়মুক্তি ও ঋণ পরিশোধ ড্যাশবোর্ড",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = PaisaTextPrimary
                        )
                        Text(
                            text = "Debt Payoff Tracker & Projections",
                            fontSize = 11.sp,
                            color = PaisaTextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Text(
                        text = "${(projection.overallProgress * 100).toInt()}% পরিশোধিত",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PaisaExpenseRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Metrics Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "মোট দেনা (Liability)",
                        fontSize = 11.sp,
                        color = PaisaTextSecondary
                    )
                    Text(
                        text = "৳ ${currencyFormat.format(projection.totalOriginalLiability)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PaisaExpenseRed
                    )
                }

                Column {
                    Text(
                        text = "পরিশোধ অগ্রগতি",
                        fontSize = 11.sp,
                        color = PaisaTextSecondary
                    )
                    Text(
                        text = "৳ ${currencyFormat.format(projection.totalPaidSoFar)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PaisaIncomeGreen
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "অবশিষ্ট দায়",
                        fontSize = 11.sp,
                        color = PaisaTextSecondary
                    )
                    Text(
                        text = "৳ ${currencyFormat.format(projection.totalRemainingLiability)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = PaisaTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = PaisaIncomeGreen,
                trackColor = PaisaSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Calculated Payoff Projection Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0E7FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventAvailable,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "প্রক্ষেপিত ঋণমুক্তি সময়কাল (Projection)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                            Text(
                                text = projection.projectedDebtFreeDateLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF4F46E5)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "মাসিক সাশ্রয়",
                            fontSize = 10.sp,
                            color = PaisaTextSecondary
                        )
                        Text(
                            text = "৳ ${currencyFormat.format(projection.estimatedMonthlyCapacity)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PaisaTextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Individual Liabilities Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "পৃথক দায়ের বিবরণ (${projection.liabilities.size} টি)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = PaisaTextPrimary
                )

                Text(
                    text = if (isExpanded) "সংক্ষিপ্ত করুন ▲" else "সবগুলো দেখুন (${projection.liabilities.size}) ▼",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PaisaTealPrimary,
                    modifier = Modifier.clickable { isExpanded = !isExpanded }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Horizontal scrolling cards when collapsed, full list when expanded
            if (!isExpanded) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(projection.liabilities, key = { it.id }) { item ->
                        IndividualLiabilityCard(
                            item = item,
                            currencyFormat = currencyFormat,
                            onItemClick = onNavigateToDebtGoals,
                            modifier = Modifier.width(220.dp)
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    projection.liabilities.forEach { item ->
                        IndividualLiabilityCard(
                            item = item,
                            currencyFormat = currencyFormat,
                            onItemClick = onNavigateToDebtGoals,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Action Buttons Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onNavigateToDebtGoals,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary)
                ) {
                    Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Payoff লক্ষ্যসমূহ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onNavigateToDenaPaona,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Handshake, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("দেনা-পাওনা খাতা", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun IndividualLiabilityCard(
    item: IndividualLiabilityItem,
    currencyFormat: NumberFormat,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(item.dueDateMillis) {
        SimpleDateFormat("dd MMM yyyy", Locale("bn", "BD")).format(Date(item.dueDateMillis))
    }

    Surface(
        onClick = onItemClick,
        shape = RoundedCornerShape(14.dp),
        color = PaisaSurfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, PaisaBorderSubtle),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (item.isCreditCard) Color(0xFFFEE2E2) else Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (item.isCreditCard) Icons.Default.CreditCard else Icons.Default.Person,
                            contentDescription = null,
                            tint = if (item.isCreditCard) Color(0xFFDC2626) else Color(0xFFD97706),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = PaisaTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (item.priorityTag) {
                        "জরুরি পরিশোধ", "মেয়াদোত্তীর্ণ" -> Color(0xFFFEE2E2)
                        "উচ্চ অগ্রাধিকার" -> Color(0xFFFFEDD5)
                        else -> Color(0xFFE0E7FF)
                    }
                ) {
                    Text(
                        text = item.priorityTag,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (item.priorityTag) {
                            "জরুরি পরিশোধ", "মেয়াদোত্তীর্ণ" -> Color(0xFF991B1B)
                            "উচ্চ অগ্রাধিকার" -> Color(0xFFC2410C)
                            else -> Color(0xFF3730A3)
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("অবশিষ্ট ব্যালেন্স", fontSize = 10.sp, color = PaisaTextSecondary)
                    Text(
                        text = "৳ ${currencyFormat.format(item.remainingAmount)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = PaisaExpenseRed
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("মোট দায়", fontSize = 10.sp, color = PaisaTextSecondary)
                    Text(
                        text = "৳ ${currencyFormat.format(item.totalAmount)}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = PaisaTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { item.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = PaisaIncomeGreen,
                trackColor = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "মেয়াদ: $dateStr",
                    fontSize = 9.sp,
                    color = PaisaTextSecondary
                )
                Text(
                    text = if (item.projectedPayoffMonths > 0) "~${item.projectedPayoffMonths} মাসে মুক্তি" else "পরিশোধিত",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4F46E5)
                )
            }
        }
    }
}
