package com.paisa.najarine.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import com.paisa.najarine.ui.PaisaViewModel
import com.paisa.najarine.ui.components.PaisaLogoBadge
import com.paisa.najarine.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreAndAnalyticsScreen(
    viewModel: PaisaViewModel,
    onNavigateToFinancialAnalytics: () -> Unit = {},
    onNavigateToBudgetPlanning: () -> Unit = {},
    onNavigateToSavingsGoals: () -> Unit = {},
    onNavigateToBillsSubscriptions: () -> Unit = {},
    onNavigateToDenaPaona: () -> Unit = {},
    onNavigateToCreditCardEmi: () -> Unit = {},
    onNavigateToFdrDpsShonchoy: () -> Unit = {},
    onNavigateToGoldSilver: () -> Unit = {},
    onNavigateToStockMarket: () -> Unit = {},
    onNavigateToCryptoAssets: () -> Unit = {},
    onNavigateToCustomerLedger: () -> Unit = {},
    onNavigateToProjectInvoice: () -> Unit = {},
    onNavigateToMessManager: () -> Unit = {},
    onNavigateToBazarShodai: () -> Unit = {},
    onNavigateToCurrencyConverter: () -> Unit = {},
    onNavigateToTaskHabit: () -> Unit = {},
    onNavigateToUserProfile: () -> Unit = {},
    onOpenSyncCenter: () -> Unit = {},
    onOpenMasterSettings: () -> Unit = {},
    onOpenSupportDeveloper: () -> Unit = {},
    onNavigateToWallets: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToIslamic: () -> Unit = {},
    onNavigateToPrayerAdhan: () -> Unit = {},
    onNavigateToQibla: () -> Unit = {},
    onNavigateToQuran: () -> Unit = {},
    onNavigateToHadith: () -> Unit = {},
    onNavigateToZakat: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PaisaBackground),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
    ) {
        // Page Title Banner with PaisaLogoBadge
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PaisaLogoBadge(
                    size = 44.dp,
                    fontSize = 22.sp,
                    cornerRadius = 14.dp,
                    borderWidth = 1.5.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "আর্থিক হাব ও টুলস",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = PaisaTextPrimary
                    )
                    Text(
                        text = "আপনার ব্যক্তিগত আর্থিক ও হিসাব ব্যবস্থাপনার কেন্দ্র",
                        fontSize = 13.sp,
                        color = PaisaTextSecondary
                    )
                }
            }
        }

        // FEATURED: DETAILED FINANCIAL ANALYTICS HERO CARD
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { onNavigateToFinancialAnalytics() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PaisaTealContainer.copy(alpha = 0.6f)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(PaisaTealPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "বিশদ আর্থিক অ্যানালিটিক্স",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = PaisaTealDark
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(PaisaIncomeGreen.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "নতুন",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PaisaIncomeGreen
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "আর্থিক স্বাস্থ্য স্কোর, ক্যাটাগরি বিশ্লেষণ ও লিকুইডিটি রানওয়ে রিপোর্ট",
                                fontSize = 12.sp,
                                color = PaisaTextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = PaisaTealPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // FEATURED: MASTER SETTINGS & PREFERENCES CARD (Prominent and easy to find)
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { onOpenMasterSettings() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(PaisaSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = PaisaTealPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "মাস্টার সেটিংস ও নিয়ন্ত্রণ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = PaisaTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(PaisaTealPrimary.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "মাস্টার",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PaisaTealPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "বায়োমেট্রিক লক, ডার্ক থিম, ক্লাউড সিঙ্ক, মাজহাব ও নোটিফিকেশন সেটিংস",
                                fontSize = 12.sp,
                                color = PaisaTextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = PaisaTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // SECTION 1: হিসাব ও খতিয়ান (Ledger)
        item {
            Spacer(modifier = Modifier.height(14.dp))
            HubSectionContainer(
                title = "হিসাব ও খতিয়ান (Ledger & Planning)",
                description = "বাজেট, সঞ্চয় লক্ষ্য, রিকারিং বিল ও দেনা-পাওনা ট্র্যাকিং",
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                accentColor = PaisaTealPrimary
            ) {
                HubActionItem(
                    title = "সব লেনদেন (সকল হিসাব)",
                    subtitle = "আয়, ব্যয়, ট্রান্সফার ও ফিল্টার তালিকা",
                    icon = Icons.AutoMirrored.Filled.ListAlt,
                    onClick = onNavigateToTransactions
                )
                HubActionItem(
                    title = "ওয়ালেট ও ব্যাংক অ্যাকাউন্ট",
                    subtitle = "ক্যাশ, ব্যাংক, বিকাশ, নগদ ও কার্ড ব্যালেন্স",
                    icon = Icons.Default.AccountBalanceWallet,
                    onClick = onNavigateToWallets
                )
                HubActionItem(
                    title = "বাজেট পরিকল্পনা (Budget Planning)",
                    subtitle = "ক্যাটাগরি ভিত্তিক খরচের সীমা ও নিয়ন্ত্রণ",
                    icon = Icons.Default.PieChart,
                    onClick = onNavigateToBudgetPlanning
                )
                HubActionItem(
                    title = "সঞ্চয় লক্ষ্য (Savings Goals Vault)",
                    subtitle = "ভবিষ্যতের লক্ষ্য অনুযায়ী টাকা জমানো",
                    icon = Icons.Default.Savings,
                    onClick = onNavigateToSavingsGoals
                )
                HubActionItem(
                    title = "বিল ও সাবস্ক্রিপশন (Bills)",
                    subtitle = "বিদ্যুৎ, ওয়াইফাই ও মাসিক রিকারিং ফি পরিশোধ",
                    icon = Icons.Default.CalendarMonth,
                    onClick = onNavigateToBillsSubscriptions
                )
                HubActionItem(
                    title = "দেনা-পাওনা ও ঋণ (Dena-Paona)",
                    subtitle = "ধার দেওয়া ও নেওয়া (ব্যালেন্সের সাথে সমন্বয়কৃত)",
                    icon = Icons.Default.Handshake,
                    onClick = onNavigateToDenaPaona
                )
                HubActionItem(
                    title = "ক্রেডিট কার্ড ও EMI",
                    subtitle = "কার্ডের ঋণ ও মাসিক কিস্তি পরিশোধ",
                    icon = Icons.Default.CreditCard,
                    onClick = onNavigateToCreditCardEmi
                )
            }
        }

        // SECTION 2: সম্পদ ও বিনিয়োগ ব্যবস্থাপনা (Asset Portfolio)
        item {
            Spacer(modifier = Modifier.height(14.dp))
            HubSectionContainer(
                title = "সম্পদ ও বিনিয়োগ (Assets Portfolio)",
                description = "ফিক্সড ডিপোজিট, স্বর্ণ, শেয়ার বাজার ও ক্রিপ্টো সম্পদ",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                accentColor = Color(0xFF0284C7)
            ) {
                HubActionItem(
                    title = "FDR, DPS ও সঞ্চয়পত্র",
                    subtitle = "ব্যাংক মেয়াদী আমানত ও মুনাফা ট্র্যাকিং",
                    icon = Icons.Default.AccountBalance,
                    onClick = onNavigateToFdrDpsShonchoy
                )
                HubActionItem(
                    title = "স্বর্ণ ও রৌপ্য সম্পদ (Gold & Silver)",
                    subtitle = "গ্রাম ও ভরি অনুযায়ী লাইভ বাজারমূল্য",
                    icon = Icons.Default.MonetizationOn,
                    onClick = onNavigateToGoldSilver
                )
                HubActionItem(
                    title = "শেয়ার বাজার বিনিয়োগ (Stock Market)",
                    subtitle = "ডিএসই পোর্টফোলিও ও মিউচুয়াল ফান্ড",
                    icon = Icons.AutoMirrored.Filled.ShowChart,
                    onClick = onNavigateToStockMarket
                )
                HubActionItem(
                    title = "ডিজিটাল সম্পদ ও ক্রিপ্টো (Crypto)",
                    subtitle = "বিটকয়েন ও অল্টকয়েন লাইভ BDT কনভার্সন রেট",
                    icon = Icons.Default.CurrencyBitcoin,
                    onClick = onNavigateToCryptoAssets
                )
                HubActionItem(
                    title = "মুদ্রা রূপান্তর ও ফরেক্স (Currency Converter)",
                    subtitle = "লাইভ আন্তর্জাতিক এক্সচেঞ্জ রেট ও দ্রুত লেনদেন কনভার্সন",
                    icon = Icons.Default.CurrencyExchange,
                    onClick = onNavigateToCurrencyConverter
                )
            }
        }

        // SECTION 3: পেশাদার কাজ ও লেজার (Work & Professional)
        item {
            Spacer(modifier = Modifier.height(14.dp))
            HubSectionContainer(
                title = "কাজ ও পেশাদার হিসাব (Work & Business)",
                description = "কাস্টমার খতিয়ান, প্রজেক্ট ইনভয়েস ও আর্থিক টু-ডু লিস্ট",
                icon = Icons.Default.Work,
                accentColor = Color(0xFF7C3AED)
            ) {
                HubActionItem(
                    title = "ক্লায়েন্ট ও কাস্টমার লেজার (Khata)",
                    subtitle = "কাস্টমারের বাকি ও জমার ডিজিটাল হিসাব",
                    icon = Icons.Default.BusinessCenter,
                    onClick = onNavigateToCustomerLedger
                )
                HubActionItem(
                    title = "মেস ও হোস্টেল মিল ম্যানেজার",
                    subtitle = "সদস্য মিল শিট, বাজার খরচ ও মিল রেট হিসাব",
                    icon = Icons.Default.Restaurant,
                    onClick = onNavigateToMessManager
                )
                HubActionItem(
                    title = "বাজার সদাই তালিকা (Bazar & Grocery)",
                    subtitle = "সদাই চেকলিস্ট, দাম ও মেস বাজার পরিকল্পনা",
                    icon = Icons.Default.ShoppingCart,
                    onClick = onNavigateToBazarShodai
                )
                HubActionItem(
                    title = "প্রজেক্ট ও ইনভয়েস জেনারেশন",
                    subtitle = "ডিজিটাল মানি রিসিট ও বিল তৈরি",
                    icon = Icons.Default.Receipt,
                    onClick = onNavigateToProjectInvoice
                )
                HubActionItem(
                    title = "কাজ ও অভ্যাসের তালিকা (Habits & Tasks)",
                    subtitle = "আর্থিক রুটিন, দৈনন্দিন কাজ ও ট্র্যাকিং",
                    icon = Icons.Default.CheckCircle,
                    onClick = onNavigateToTaskHabit
                )
            }
        }

        // SECTION 4: প্রোফাইল ও ক্লাউড অটো-সিঙ্ক (Single User Account)
        item {
            Spacer(modifier = Modifier.height(14.dp))
            HubSectionContainer(
                title = "ব্যক্তিগত অ্যাকাউন্ট ও ক্লাউড সিঙ্ক",
                description = "গুগল অ্যাকাউন্ট প্রোফাইল ও ফায়ারবেস অটোমেটিক সিঙ্ক",
                icon = Icons.Default.Person,
                accentColor = Color(0xFF0D9488)
            ) {
                HubActionItem(
                    title = "আমার প্রোফাইল (User Profile)",
                    subtitle = "গুগল আইডি, যোগাযোগের তথ্য ও পার্সোনাল ডাটা",
                    icon = Icons.Default.Badge,
                    onClick = onNavigateToUserProfile
                )
                HubActionItem(
                    title = "অনলাইন ক্লাউড সিঙ্ক কেন্দ্র",
                    subtitle = "রিয়েলটাইম ফায়ারবেস স্টোরেজ ও অফলাইন ব্যাকআপ",
                    icon = Icons.Default.CloudDone,
                    onClick = onOpenSyncCenter
                )
                HubActionItem(
                    title = "মাস্টার সেটিংস ও নিরাপত্তা",
                    subtitle = "পিন কোড, বায়োমেট্রিক ও নোটিফিকেশন",
                    icon = Icons.Default.Security,
                    onClick = onOpenMasterSettings
                )
            }
        }

        // SECTION 5: ইসলামিক জীবন ও সম্পদ
        item {
            Spacer(modifier = Modifier.height(14.dp))
            HubSectionContainer(
                title = "ইসলামিক জীবন ও সম্পদ",
                description = "নামাজ, আযান, পূর্ণাঙ্গ ১১৪ সূরা কুরআন, হাদিস, যাকাত ও কিবলা",
                icon = Icons.Default.Mosque,
                accentColor = Color(0xFF15803D)
            ) {
                HubActionItem(
                    title = "আজকের ইসলামিক ড্যাশবোর্ড",
                    subtitle = "পরবর্তী নামাজ, দৈনিক আয়াত ও হাদিস",
                    icon = Icons.Default.Today,
                    onClick = onNavigateToIslamic
                )
                HubActionItem(
                    title = "আযান ও নামাজের সময়সূচি",
                    subtitle = "করাচি পদ্ধতি, হানাফী আসর ও ফজরের আযান",
                    icon = Icons.Default.NotificationsActive,
                    onClick = onNavigateToPrayerAdhan
                )
                HubActionItem(
                    title = "পবিত্র আল-কুরআন (১১৪টি সূরা)",
                    subtitle = "সূরাভিত্তিক পূর্ণাঙ্গ পাঠাগার, অর্থ ও তিলাওয়াত",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    onClick = onNavigateToQuran
                )
                HubActionItem(
                    title = "দৈনিক সহীহ হাদিস (প্রতি ঘণ্টায় রিফ্রেশ)",
                    subtitle = "সহীহ বুখারী ও মুসলিম ভিত্তিক লাইভ হাদিস",
                    icon = Icons.Default.AutoStories,
                    onClick = onNavigateToHadith
                )
                HubActionItem(
                    title = "যাকাত ও ফিতরা ক্যালকুলেটর",
                    subtitle = "নেসাব ভিত্তিক সুনির্দিষ্ট যাকাত ও ফিতরা হিসাব",
                    icon = Icons.Default.Calculate,
                    onClick = onNavigateToZakat
                )
                HubActionItem(
                    title = "কিবলা কম্পাস",
                    subtitle = "পবিত্র কাবার সঠিক দিকনির্দেশক (২৬২° পশ্চিম)",
                    icon = Icons.Default.Explore,
                    onClick = onNavigateToQibla
                )
            }
        }

        // SECTION 6: ডেভেলপার সহায়তা
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { onOpenSupportDeveloper() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PaisaTealContainer),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(PaisaTealPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "ডেভেলপারকে সহায়তা করুন",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = PaisaTealDark
                            )
                            Text(
                                text = "যোগাযোগ: najarine@gmail.com • অনুদান ও সহায়তা",
                                style = MaterialTheme.typography.bodySmall,
                                color = PaisaTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PaisaTealPrimary)
                }
            }
        }
    }
}

@Composable
fun HubSectionContainer(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = PaisaSurface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PaisaTextPrimary)
                    Text(description, fontSize = 11.sp, color = PaisaTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = PaisaBorder.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            content()
        }
    }
}

@Composable
fun HubActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = PaisaTealPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = PaisaTextPrimary)
                Text(subtitle, fontSize = 11.sp, color = PaisaTextSecondary)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PaisaTextTertiary, modifier = Modifier.size(18.dp))
    }
}
