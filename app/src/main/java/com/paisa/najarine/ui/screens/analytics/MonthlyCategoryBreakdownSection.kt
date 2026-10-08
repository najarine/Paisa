package com.paisa.najarine.ui.screens.analytics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.core.graphics.toColorInt
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.data.local.CategoryEntity
import com.paisa.najarine.data.local.TransactionEntity
import com.paisa.najarine.ui.theme.*
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Data model for a category's monthly expenditure item.
 */
data class CategorySpendingItem(
    val categoryId: String,
    val categoryName: String,
    val color: Color,
    val totalAmount: Double,
    val percentageOfTotal: Double, // 0.0 to 1.0
    val transactionCount: Int,
    val previousMonthAmount: Double = 0.0,
    val monthOverMonthChangePct: Double? = null,
    val healthWarning: String? = null
)

data class MonthOption(
    val label: String,
    val fullLabel: String,
    val startMillis: Long,
    val endMillis: Long,
    val prevStartMillis: Long,
    val prevEndMillis: Long
)

object MonthlyCategoryBreakdownHelper {

    fun getRecentMonthOptions(referenceTime: Long = System.currentTimeMillis()): List<MonthOption> {
        val bnMonths = listOf(
            "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
            "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
        )

        return (0..2).map { offset ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = referenceTime
                add(Calendar.MONTH, -offset)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = cal.timeInMillis
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, maxDay)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis

            // Previous month for comparison
            val prevCal = Calendar.getInstance().apply {
                timeInMillis = start
                add(Calendar.MONTH, -1)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val prevStart = prevCal.timeInMillis
            val prevMaxDay = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            prevCal.set(Calendar.DAY_OF_MONTH, prevMaxDay)
            prevCal.set(Calendar.HOUR_OF_DAY, 23)
            prevCal.set(Calendar.MINUTE, 59)
            prevCal.set(Calendar.SECOND, 59)
            prevCal.set(Calendar.MILLISECOND, 999)
            val prevEnd = prevCal.timeInMillis

            val monthIndex = cal.get(Calendar.MONTH)
            val year = cal.get(Calendar.YEAR)
            val label = when (offset) {
                0 -> "চলতি মাস (${bnMonths[monthIndex]})"
                1 -> "গত মাস (${bnMonths[monthIndex]})"
                else -> bnMonths[monthIndex]
            }
            val fullLabel = "${bnMonths[monthIndex]} $year"

            MonthOption(label, fullLabel, start, end, prevStart, prevEnd)
        }
    }

    fun computeMonthlyCategorySpending(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        monthOption: MonthOption
    ): List<CategorySpendingItem> {
        val catMapByName = categories.associateBy { it.name.lowercase() }
        val catMapById = categories.associateBy { it.id.lowercase() }

        // Current month expenses
        val currentTxs = transactions.filter {
            it.type == "EXPENSE" && it.dateMillis in monthOption.startMillis..monthOption.endMillis
        }
        val prevTxs = transactions.filter {
            it.type == "EXPENSE" && it.dateMillis in monthOption.prevStartMillis..monthOption.prevEndMillis
        }

        val totalMonthExpense = currentTxs.sumOf { it.amount }
        if (totalMonthExpense <= 0.0) return emptyList()

        val prevMap = prevTxs.groupBy { it.category.lowercase() }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val palette = listOf(
            Color(0xFFEF4444), // Crimson
            Color(0xFFF97316), // Orange
            Color(0xFF0D9488), // Teal
            Color(0xFF2563EB), // Blue
            Color(0xFF8B5CF6), // Purple
            Color(0xFFEC4899), // Pink
            Color(0xFF10B981), // Emerald
            Color(0xFFF59E0B), // Amber
            Color(0xFF6366F1), // Indigo
            Color(0xFF14B8A6)  // Light Teal
        )

        return currentTxs.groupBy { it.category }
            .entries
            .toList()
            .mapIndexed { index, entry ->
                val catKey = entry.key
                val txList = entry.value
                val amount = txList.sumOf { it.amount }
                val catObj = catMapByName[catKey.lowercase()] ?: catMapById[catKey.lowercase()]
                val name = catObj?.name ?: if (catKey.isBlank()) "সাধারণ খরচ" else catKey
                val colorHex = catObj?.colorHex

                val parsedColor = if (!colorHex.isNullOrBlank()) {
                    try {
                        Color(colorHex.toColorInt())
                    } catch (_: Exception) {
                        palette[index % palette.size]
                    }
                } else {
                    palette[index % palette.size]
                }

                val pct = if (totalMonthExpense > 0) amount / totalMonthExpense else 0.0
                val prevAmt = prevMap[catKey.lowercase()] ?: 0.0
                val changePct = if (prevAmt > 0) ((amount - prevAmt) / prevAmt) * 100.0 else null

                // Financial health warning: if single category takes > 35% of total monthly spend
                val warning = if (pct >= 0.35 && totalMonthExpense > 5000.0) {
                    "বাজেটের ${(pct * 100).toInt()}% দখল করেছে (উচ্চ ব্যয় সতর্কতা)"
                } else null

                CategorySpendingItem(
                    categoryId = catObj?.id ?: catKey,
                    categoryName = name,
                    color = parsedColor,
                    totalAmount = amount,
                    percentageOfTotal = pct,
                    transactionCount = txList.size,
                    previousMonthAmount = prevAmt,
                    monthOverMonthChangePct = changePct,
                    healthWarning = warning
                )
            }
            .sortedByDescending { it.totalAmount }
    }
}

/**
 * Recharts-inspired Monthly Category Spending Breakdown visualization section.
 * Features an interactive Donut Chart, Category Comparison Bars, Month Selector,
 * and Financial Health Warnings.
 */
@Composable
fun MonthlyCategoryBreakdownSection(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    modifier: Modifier = Modifier
) {
    val currencyFormat = remember {
        NumberFormat.getNumberInstance(Locale.forLanguageTag("bn-BD")).apply {
            maximumFractionDigits = 0
        }
    }

    val monthOptions = remember { MonthlyCategoryBreakdownHelper.getRecentMonthOptions() }
    var selectedMonthIndex by remember { mutableIntStateOf(0) }
    val currentMonthOption = monthOptions.getOrElse(selectedMonthIndex) { monthOptions[0] }

    val categoryBreakdown = remember(transactions, categories, currentMonthOption) {
        MonthlyCategoryBreakdownHelper.computeMonthlyCategorySpending(transactions, categories, currentMonthOption)
    }

    val totalMonthExpense = remember(categoryBreakdown) {
        categoryBreakdown.sumOf { it.totalAmount }
    }

    // Selected category slice for interactive inspection
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val activeCategory = categoryBreakdown.getOrNull(selectedCategoryIndex)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_category_breakdown_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = PaisaSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "মাসিক খাতওয়ারি ব্যয় বিশ্লেষণ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = PaisaTextPrimary
                        )
                        Text(
                            text = "ক্যাটাগরি ভিত্তিক ভিজ্যুয়ালাইজেশন ও বাজেট স্বাস্থ্য",
                            fontSize = 11.sp,
                            color = PaisaTextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PaisaSurfaceVariant
                ) {
                    Text(
                        text = "${categoryBreakdown.size} টি খাত",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PaisaTealPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Month Selector Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(monthOptions.indices.toList()) { idx ->
                    val opt = monthOptions[idx]
                    val isSelected = idx == selectedMonthIndex
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedMonthIndex = idx
                            selectedCategoryIndex = 0
                        },
                        label = { Text(opt.label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PaisaTealPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (categoryBreakdown.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Category,
                            contentDescription = null,
                            tint = PaisaTextTertiary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${currentMonthOption.fullLabel}-এ কোনো ব্যয়ের লেনদেন নেই",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = PaisaTextSecondary
                        )
                        Text(
                            text = "খরচের লেনদেন যুক্ত করলে এখানে স্বয়ংক্রিয় পাই ও বার চার্ট প্রদর্শিত হবে।",
                            fontSize = 11.sp,
                            color = PaisaTextTertiary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Interactive Donut Chart (Recharts Pie aesthetic)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Donut Canvas
                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        CategoryDonutChart(
                            items = categoryBreakdown,
                            selectedIndex = selectedCategoryIndex,
                            onSelectIndex = { selectedCategoryIndex = it },
                            modifier = Modifier.size(175.dp)
                        )

                        // Center Content inside Donut
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = activeCategory?.categoryName ?: "মোট ব্যয়",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PaisaTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "৳${currencyFormat.format(activeCategory?.totalAmount ?: totalMonthExpense)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = activeCategory?.color ?: PaisaTextPrimary
                            )
                            if (activeCategory != null) {
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", activeCategory.percentageOfTotal * 100),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PaisaTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Top 3-4 Categories Legend with Tap Selection
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        categoryBreakdown.take(4).forEachIndexed { index, item ->
                            val isSelected = index == selectedCategoryIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) PaisaSurfaceVariant else Color.Transparent)
                                    .clickable { selectedCategoryIndex = index }
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(item.color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.categoryName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = PaisaTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = String.format(Locale.US, "%.0f%%", item.percentageOfTotal * 100),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PaisaTextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Highlighted Details for the Selected Category
                if (activeCategory != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(activeCategory.color.copy(alpha = 0.08f))
                            .border(1.dp, activeCategory.color.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(activeCategory.color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = activeCategory.categoryName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = PaisaTextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${activeCategory.transactionCount} টি এন্ট্রি)",
                                        fontSize = 11.sp,
                                        color = PaisaTextSecondary
                                    )
                                }

                                Text(
                                    text = "৳${currencyFormat.format(activeCategory.totalAmount)}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = activeCategory.color
                                )
                            }

                            // Health warning or MoM change
                            if (activeCategory.healthWarning != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = activeCategory.healthWarning,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFB45309)
                                    )
                                }
                            } else if (activeCategory.monthOverMonthChangePct != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                val chg = activeCategory.monthOverMonthChangePct
                                val isMore = chg > 0.0
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isMore) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = if (isMore) PaisaExpenseRed else PaisaIncomeGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isMore) "গত মাসের তুলনায় +${String.format(Locale.US, "%.1f%%", chg)} বৃদ্ধি" else "গত মাসের তুলনায় ${String.format(Locale.US, "%.1f%%", -chg)} সাশ্রয়",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isMore) PaisaExpenseRed else PaisaIncomeGreen
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Breakdown Progress Bars for All Categories
                Text(
                    text = "সকল খাতের খরচের অনুপাত:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = PaisaTextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                categoryBreakdown.forEachIndexed { index, item ->
                    val isSelected = index == selectedCategoryIndex
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedCategoryIndex = index }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(item.color)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.categoryName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = PaisaTextPrimary
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "৳${currencyFormat.format(item.totalAmount)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PaisaTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = String.format(Locale.US, "(%.1f%%)", item.percentageOfTotal * 100),
                                    fontSize = 11.sp,
                                    color = PaisaTextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LinearProgressIndicator(
                            progress = { item.percentageOfTotal.toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = item.color,
                            trackColor = PaisaSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Custom Canvas Donut Chart with touch interaction and animated segments.
 */
@Composable
private fun CategoryDonutChart(
    items: List<CategorySpendingItem>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    Box(
        modifier = modifier
            .testTag("category_donut_canvas")
            .pointerInput(items) {
                detectTapGestures { offset ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = offset.x - center.x
                    val dy = offset.y - center.y
                    var angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    if (angle < 0) angle += 360f

                    // Start angle in drawArc is usually 270 (top)
                    var relAngle = (angle - 270f)
                    if (relAngle < 0) relAngle += 360f

                    var currentAngle = 0f
                    for (i in items.indices) {
                        val sweep = (items[i].percentageOfTotal * 360f).toFloat()
                        if (relAngle in currentAngle..(currentAngle + sweep)) {
                            onSelectIndex(i)
                            break
                        }
                        currentAngle += sweep
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 24.dp.toPx()
            val diameter = size.minDimension - strokeWidth - 8.dp.toPx()
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            )

            var startAngle = 270f // Start from the top

            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                val sweepAngle = (item.percentageOfTotal * 360f).toFloat().coerceAtLeast(1f)

                val effectiveStroke = if (isSelected) strokeWidth + 6.dp.toPx() else strokeWidth
                val effectiveColor = if (isSelected) item.color else item.color.copy(alpha = 0.85f)

                drawArc(
                    color = effectiveColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle - 2f, // Small gap between slices
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = effectiveStroke, cap = StrokeCap.Round)
                )

                startAngle += sweepAngle
            }
        }
    }
}
