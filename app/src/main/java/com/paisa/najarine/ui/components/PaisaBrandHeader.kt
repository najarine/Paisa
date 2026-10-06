package com.paisa.najarine.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.paisa.najarine.sync.SyncState
import com.paisa.najarine.sync.SyncStatus
import com.paisa.najarine.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

@Composable
fun PaisaBrandHeader(
    accountOwnerName: String = "Paisa Account",
    accountOwnerEmail: String = "",
    userPhotoUrl: String? = null,
    syncStatus: SyncStatus? = null,
    onAccountClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onLockClick: () -> Unit = {},
    onSyncClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        color = PaisaSurface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Stylized Paisa Brand Logo & User Information
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onProfileClick() }
                        .padding(vertical = 4.dp)
                ) {
                    // Stylized Brand Icon (matches Login screen everywhere)
                    PaisaLogoBadge(
                        size = 38.dp,
                        fontSize = 20.sp,
                        cornerRadius = 12.dp,
                        borderWidth = 1.5.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = accountOwnerName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PaisaTextPrimary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PaisaTealPrimary.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ব্যক্তিগত",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PaisaTealPrimary,
                                    maxLines = 1
                                )
                            }
                        }

                        // Compact Cloud Sync State Indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { onSyncClick() }
                                .padding(top = 2.dp)
                        ) {
                            val (syncText, syncColor) = when (syncStatus?.state) {
                                SyncState.SYNCING -> "সিঙ্ক হচ্ছে..." to PaisaTealPrimary
                                SyncState.OFFLINE -> "অফলাইন মোড" to PaisaTextSecondary
                                SyncState.ERROR -> "সিঙ্ক সমস্যা" to PaisaExpenseRed
                                else -> "ক্লাউডে সিঙ্কড" to PaisaIncomeGreen
                            }

                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(syncColor)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = syncText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = PaisaTextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Right: User Profile Avatar with Google Profile Picture
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val resolvedPhotoUrl = userPhotoUrl ?: FirebaseAuth.getInstance().currentUser?.photoUrl?.toString()

                    IconButton(
                        onClick = onProfileClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PaisaTealContainer)
                            .border(1.5.dp, PaisaBorder, CircleShape)
                    ) {
                        if (!resolvedPhotoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = resolvedPhotoUrl,
                                contentDescription = "গুগল প্রোফাইল ছবি",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "প্রোফাইল",
                                tint = PaisaTealPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
