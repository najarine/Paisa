package com.paisa.najarine.ui.screens.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.data.local.TransactionEntity
import com.paisa.najarine.ui.theme.*
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

/**
 * Data representation for a single month's spending data point.
 */
data class MonthlySpendingPoint(
    val monthOffset: Int,             // 5 down to 0 (0 is current month)
    val monthLabel: String,           // Short name, e.g. "মে", "জুন"
    val monthFullLabel: String,       // Full label, e.g. "মে ২০২৬"
    val totalExpense: Double,
    val totalIncome: Double,
    val transactionCount: Int,
    val percentChangeFromPrevious: Double? = null // Change vs prior month
)

/**
 * Aggregated trends summary over 6 months.
 */
data class SixMonthSpendingSummary(
    val points: List<MonthlySpendingPoint>,
    val totalSpend: Double,
    val averageMonthlySpend: Double,
    val highestMonth: MonthlySpendingPoint?,
    val lowestMonth: MonthlySpendingPoint?,
    val currentVsPreviousChangePct: Double?
)

object MonthlySpendingTrendsCalculator {
    private val BN_MONTHS_SHORT = listOf(
        "জানু", "ফেব্রু", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টে", "অক্টো", "নভে", "ডিসে"
    )
    private val BN_MONTHS_FULL = listOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    fun calculateSixMonthTrends(
        transactions: List<TransactionEntity>,
        referenceTimeMillis: Long = System.currentTimeMillis()
    ): SixMonthSpendingSummary {
        val rawPoints = (5 downTo 0).map { offset ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = referenceTimeMillis
                add(Calendar.MONTH, -offset)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startMillis = cal.timeInMillis

            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, maxDay)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val endMillis = cal.timeInMillis

            val monthIdx = cal.get(Calendar.MONTH)
            val year = cal.get(Calendar.YEAR)
            val monthShort = BN_MONTHS_SHORT.getOrElse(monthIdx) { "মাস" }
            val monthFull = "${BN_MONTHS_FULL.getOrElse(monthIdx) { "" }} $year"

            val monthExpenses = transactions.filter {
                it.type == "EXPENSE" && it.dateMillis in startMillis..endMillis
            }
            val monthIncomes = transactions.filter {
                it.type == "INCOME" && it.dateMillis in startMillis..endMillis
            }

            val totalExp = monthExpenses.sumOf { it.amount }
            val totalInc = monthIncomes.sumOf { it.amount }

            MonthlySpendingPoint(
                monthOffset = offset,
                monthLabel = monthShort,
                monthFullLabel = monthFull,
                totalExpense = totalExp,
                totalIncome = totalInc,
                transactionCount = monthExpenses.size
            )
        }

        // Calculate month-over-month percentage changes
        val points = rawPoints.mapIndexed { idx, pt ->
            if (idx > 0) {
                val prevExp = rawPoints[idx - 1].totalExpense
                val pct = if (prevExp > 0.0) {
                    ((pt.totalExpense - prevExp) / prevExp) * 100.0
                } else if (pt.totalExpense > 0.0) {
                    100.0
                } else {
                    0.0
                }
                pt.copy(percentChangeFromPrevious = pct)
            } else {
                pt
            }
        }

        val totalSpend = points.sumOf { it.totalExpense }
        val averageSpend = if (points.isNotEmpty()) totalSpend / points.size else 0.0
        val highestMonth = points.maxByOrNull { it.totalExpense }
        val lowestMonth = points.filter { it.totalExpense > 0 }.minByOrNull { it.totalExpense }
            ?: points.minByOrNull { it.totalExpense }

        val currentVsPreviousChangePct = points.lastOrNull()?.percentChangeFromPrevious

        return SixMonthSpendingSummary(
            points = points,
            totalSpend = totalSpend,
            averageMonthlySpend = averageSpend,
            highestMonth = highestMonth,
            lowestMonth = lowestMonth,
            currentVsPreviousChangePct = currentVsPreviousChangePct
        )
    }
}

/**
 * Modern, interactive "Monthly Spending Trends" section using a line chart
 * to visualize transaction history over the past six months.
 */
@Composable
fun MonthlySpendingTrendsSection(
    transactions: List<TransactionEntity>,
    modifier: Modifier = Modifier
) {
    val currencyFormat = remember {
        NumberFormat.getNumberInstance(Locale("bn", "BD")).apply {
            maximumFractionDigits = 0
        }
    }

    val summary = remember(transactions) {
        MonthlySpendingTrendsCalculator.calculateSixMonthTrends(transactions)
    }

    // Default selection is the current month (index 5)
    var selectedIndex by remember { mutableIntStateOf(5) }
    val selectedPoint = summary.points.getOrNull(selectedIndex) ?: summary.points.lastOrNull()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_spending_trends_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = PaisaSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header Row
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
                            .background(PaisaExpenseLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "মাসিক খরচের ট্রেন্ড",
                            tint = PaisaExpenseRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "মাসিক ব্যয়ের ট্রেন্ড (Spending Trends)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = PaisaTextPrimary
                        )
                        Text(
                            text = "বিগত ৬ মাসের খরচের গতিবিধি ও তুলনামূলক ইতিহাস",
                            fontSize = 11.sp,
                            color = PaisaTextSecondary
                        )
                    }
                }

                // 6 Months Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PaisaSurfaceVariant,
                    border = BorderStroke(1.dp, PaisaBorderSubtle)
                ) {
                    Text(
                        text = "৬ মাস",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PaisaTealPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Highlighted Detail Card for Selected Month
            if (selectedPoint != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PaisaBackground)
                        .border(1.dp, PaisaBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = PaisaTealPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedPoint.monthFullLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PaisaTextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "৳${currencyFormat.format(selectedPoint.totalExpense)}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (selectedPoint.totalExpense > 0) PaisaExpenseRed else PaisaTextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "(${selectedPoint.transactionCount} টি এন্ট্রি)",
                                    fontSize = 11.sp,
                                    color = PaisaTextTertiary,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                        }

                        // Month-over-month trend change pill
                        val changePct = selectedPoint.percentChangeFromPrevious
                        if (changePct != null && selectedPoint.monthOffset < 5) {
                            val isHigher = changePct > 0.05
                            val isLower = changePct < -0.05
                            val badgeBg = when {
                                isHigher -> PaisaExpenseLight
                                isLower -> PaisaIncomeLight
                                else -> PaisaSurfaceVariant
                            }
                            val badgeColor = when {
                                isHigher -> PaisaExpenseRed
                                isLower -> PaisaIncomeGreen
                                else -> PaisaTextSecondary
                            }
                            val changeText = when {
                                isHigher -> String.format(Locale.US, "+%.1f%% ব্যয় বৃদ্ধি", changePct)
                                isLower -> String.format(Locale.US, "%.1f%% সাশ্রয়", -changePct)
                                else -> "অপরিবর্তিত"
                            }
                            val changeIcon = when {
                                isHigher -> Icons.Default.TrendingUp
                                isLower -> Icons.Default.TrendingDown
                                else -> Icons.Default.ShowChart
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = badgeBg,
                                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = changeIcon,
                                        contentDescription = null,
                                        tint = badgeColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = changeText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PaisaSurfaceVariant
                            ) {
                                Text(
                                    text = "প্রথম রেকর্ড",
                                    fontSize = 11.sp,
                                    color = PaisaTextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // The Interactive Line Chart Canvas
            SpendingTrendsLineChart(
                points = summary.points,
                selectedIndex = selectedIndex,
                onSelectIndex = { selectedIndex = it },
                currencyFormat = currencyFormat,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Month Selector Chips underneath chart
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                summary.points.forEachIndexed { index, pt ->
                    val isSelected = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PaisaExpenseRed else PaisaSurfaceVariant)
                            .clickable { selectedIndex = index }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pt.monthLabel,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else PaisaTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6-Month Summary Metrics (Average, Peak, Total)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Monthly Average
                TrendStatPill(
                    title = "মাসিক গড় ব্যয়",
                    value = "৳${currencyFormat.format(summary.averageMonthlySpend)}",
                    accentColor = PaisaTealPrimary,
                    modifier = Modifier.weight(1f)
                )

                // Highest Month
                val peakName = summary.highestMonth?.monthLabel ?: "-"
                val peakVal = summary.highestMonth?.totalExpense ?: 0.0
                TrendStatPill(
                    title = "সর্বোচ্চ ব্যয় ($peakName)",
                    value = "৳${currencyFormat.format(peakVal)}",
                    accentColor = PaisaExpenseRed,
                    modifier = Modifier.weight(1f)
                )

                // Total 6-Month Spend
                TrendStatPill(
                    title = "৬ মাসের মোট",
                    value = "৳${currencyFormat.format(summary.totalSpend)}",
                    accentColor = PaisaTextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Custom Canvas Line Chart with gradient fill, gridlines, glowing dots and touch handling.
 */
@Composable
private fun SpendingTrendsLineChart(
    points: List<MonthlySpendingPoint>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val maxSpend = points.maxOfOrNull { it.totalExpense } ?: 0.0
    // Dynamic ceiling so chart doesn't flatten when all values are 0
    val yCeiling = if (maxSpend > 0) maxSpend * 1.25 else 10000.0

    Box(
        modifier = modifier
            .testTag("spending_trends_canvas_box")
            .pointerInput(points) {
                detectTapGestures { offset ->
                    val width = size.width
                    val padLeft = 45f
                    val padRight = 20f
                    val chartWidth = width - padLeft - padRight
                    val stepX = chartWidth / (points.size - 1).coerceAtLeast(1)

                    val relativeX = (offset.x - padLeft).coerceIn(0f, chartWidth)
                    val nearestIndex = ((relativeX + (stepX / 2f)) / stepX).toInt().coerceIn(0, points.size - 1)
                    onSelectIndex(nearestIndex)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val padLeft = 50.dp.toPx()
            val padRight = 16.dp.toPx()
            val padTop = 16.dp.toPx()
            val padBottom = 22.dp.toPx()

            val chartWidth = size.width - padLeft - padRight
            val chartHeight = size.height - padTop - padBottom

            // 1. Draw horizontal grid lines (0%, 50%, 100% of ceiling)
            val gridSteps = 3
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

            for (i in 0..gridSteps) {
                val ratio = i / gridSteps.toFloat()
                val y = padTop + chartHeight * (1f - ratio)

                drawLine(
                    color = PaisaBorderSubtle,
                    start = Offset(padLeft, y),
                    end = Offset(size.width - padRight, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
            }

            // Calculate coordinate points (X, Y) for each month
            val numPoints = points.size
            val stepX = if (numPoints > 1) chartWidth / (numPoints - 1) else chartWidth

            val coords = points.mapIndexed { i, pt ->
                val x = padLeft + i * stepX
                val normalizedY = (pt.totalExpense / yCeiling).coerceIn(0.0, 1.0).toFloat()
                val y = padTop + chartHeight * (1f - normalizedY)
                Offset(x, y)
            }

            // 2. Draw smooth curved path and filled gradient
            if (coords.isNotEmpty()) {
                val linePath = Path()
                val fillPath = Path()

                linePath.moveTo(coords[0].x, coords[0].y)
                fillPath.moveTo(coords[0].x, padTop + chartHeight)
                fillPath.lineTo(coords[0].x, coords[0].y)

                for (i in 0 until coords.size - 1) {
                    val p0 = coords[i]
                    val p1 = coords[i + 1]

                    // Cubic Bezier curve control points for fluid curvature
                    val controlX1 = p0.x + (p1.x - p0.x) / 2f
                    val controlY1 = p0.y
                    val controlX2 = p0.x + (p1.x - p0.x) / 2f
                    val controlY2 = p1.y

                    linePath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                    fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                }

                // Close the fill path along the bottom
                fillPath.lineTo(coords.last().x, padTop + chartHeight)
                fillPath.close()

                // Draw gradient underneath curve
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            PaisaExpenseRed.copy(alpha = 0.28f),
                            PaisaExpenseRed.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        startY = padTop,
                        endY = padTop + chartHeight
                    )
                )

                // Draw main stroke line
                drawPath(
                    path = linePath,
                    color = PaisaExpenseRed,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // 3. Draw vertical indicator line for selected month
            if (selectedIndex in coords.indices) {
                val selectedCoord = coords[selectedIndex]
                drawLine(
                    color = PaisaExpenseRed.copy(alpha = 0.45f),
                    start = Offset(selectedCoord.x, padTop),
                    end = Offset(selectedCoord.x, padTop + chartHeight),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = dashEffect
                )
            }

            // 4. Draw data points (halo + dot)
            coords.forEachIndexed { i, coord ->
                val isSelected = i == selectedIndex

                if (isSelected) {
                    // Outer glow halo
                    drawCircle(
                        color = PaisaExpenseRed.copy(alpha = 0.2f),
                        radius = 12.dp.toPx(),
                        center = coord
                    )
                    // White border ring
                    drawCircle(
                        color = Color.White,
                        radius = 7.dp.toPx(),
                        center = coord
                    )
                    // Inner bright dot
                    drawCircle(
                        color = PaisaExpenseRed,
                        radius = 5.dp.toPx(),
                        center = coord
                    )
                } else {
                    // Regular dot: white fill with red border
                    drawCircle(
                        color = Color.White,
                        radius = 5.dp.toPx(),
                        center = coord
                    )
                    drawCircle(
                        color = PaisaExpenseRed,
                        radius = 5.dp.toPx(),
                        center = coord,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendStatPill(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PaisaBackground)
            .border(1.dp, PaisaBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 10.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 10.sp,
                color = PaisaTextSecondary,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1
            )
        }
    }
}
