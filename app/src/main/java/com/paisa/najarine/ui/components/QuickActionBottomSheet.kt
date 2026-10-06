package com.paisa.najarine.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionBottomSheet(
    onDismiss: () -> Unit,
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onTransfer: () -> Unit,
    onScanReceipt: () -> Unit,
    onVoiceEntry: () -> Unit,
    onAddWallet: () -> Unit,
    onAddBudget: () -> Unit,
    onAddGoal: () -> Unit,
    onAddDebt: () -> Unit,
    onAddPartner: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        LazyColumn(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Title (Matches Reference Snapshot 1: "Quick Action")
            item {
                Text(
                    text = "Quick Action (দ্রুত অ্যাকশন)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = PaisaTextPrimary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // 1. Add Expense (Red Circle Arrow Down)
            item {
                QuickActionRow(
                    title = "Add Expense (খরচ যোগ করুন)",
                    subtitle = "Log daily spend, bills, or purchases",
                    icon = Icons.Default.ArrowDownward,
                    iconBg = Color(0xFFFEE2E2), // Light Red
                    iconTint = Color(0xFFDC2626), // Red
                    onClick = {
                        onDismiss()
                        onAddExpense()
                    }
                )
            }

            // 2. Add Income (Green Circle Arrow Up)
            item {
                QuickActionRow(
                    title = "Add Income (আয় যোগ করুন)",
                    subtitle = "Record salary, freelance, or profit",
                    icon = Icons.Default.ArrowUpward,
                    iconBg = Color(0xFFDCFCE7), // Light Green
                    iconTint = Color(0xFF16A34A), // Green
                    onClick = {
                        onDismiss()
                        onAddIncome()
                    }
                )
            }

            // 3. Transfer Money (Blue Circle Double Arrow)
            item {
                QuickActionRow(
                    title = "Transfer Money (টাকা ট্রান্সফার)",
                    subtitle = "Move funds between your Wallets",
                    icon = Icons.Default.SyncAlt,
                    iconBg = Color(0xFFE0F2FE), // Light Blue
                    iconTint = Color(0xFF0284C7), // Blue
                    onClick = {
                        onDismiss()
                        onTransfer()
                    }
                )
            }

            // 4. Scan Receipt (Mint Green Scan Icon)
            item {
                QuickActionRow(
                    title = "Scan Receipt (রশিদ স্ক্যান)",
                    subtitle = "Draft transaction from camera/receipt OCR",
                    icon = Icons.Default.DocumentScanner,
                    iconBg = Color(0xFFD1FAE5), // Mint
                    iconTint = Color(0xFF059669), // Emerald
                    onClick = {
                        onDismiss()
                        onScanReceipt()
                    }
                )
            }

            // 5. Voice Entry (Amber/Yellow Circle Mic Icon)
            item {
                QuickActionRow(
                    title = "Voice Entry (ভয়েস এন্ট্রি)",
                    subtitle = "Speak in Bangla or English to prepare draft",
                    icon = Icons.Default.Mic,
                    iconBg = Color(0xFFFEF3C7), // Light Yellow
                    iconTint = Color(0xFFD97706), // Amber
                    onClick = {
                        onDismiss()
                        onVoiceEntry()
                    }
                )
            }

            // Divider before secondary creations
            item {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = Color(0xFFE5E7EB))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "অন্যান্য সংযোজন (Create Containers & Goals)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = PaisaTextSecondary
                )
            }

            // Secondary Creation Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CreationChip(
                        label = "+ ওয়ালেট",
                        icon = Icons.Default.AccountBalanceWallet,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onDismiss()
                            onAddWallet()
                        }
                    )
                    CreationChip(
                        label = "+ বাজেট",
                        icon = Icons.Default.PieChart,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onDismiss()
                            onAddBudget()
                        }
                    )
                    CreationChip(
                        label = "+ লক্ষ্য",
                        icon = Icons.Default.Savings,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onDismiss()
                            onAddGoal()
                        }
                    )
                    CreationChip(
                        label = "+ পার্টনার",
                        icon = Icons.Default.PersonAdd,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onDismiss()
                            onAddPartner()
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
fun QuickActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = PaisaTextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = PaisaTextSecondary
            )
        }
    }
}

@Composable
fun CreationChip(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF3F4F6),
        modifier = modifier.height(38.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = PaisaTealPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PaisaTextPrimary)
        }
    }
}
