package com.paisa.najarine.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.auth.AuthState
import com.paisa.najarine.ui.theme.*

@Composable
fun LoginScreen(
    authState: AuthState,
    onSignInClick: () -> Unit,
    onCancelLoading: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PaisaBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Paisa Icon Logo
            com.paisa.najarine.ui.components.PaisaLogoBadge(
                size = 80.dp,
                fontSize = 42.sp,
                cornerRadius = 24.dp,
                borderWidth = 2.dp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Paisa",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = PaisaTextPrimary
            )

            Text(
                text = "আপনার আর্থিক ভবিষ্যৎ ও ইসলামিক সম্পদ ব্যবস্থাপনার নির্ভরযোগ্য সঙ্গী",
                style = MaterialTheme.typography.bodyMedium,
                color = PaisaTextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Main Authentication Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "আপনার অ্যাকাউন্টে প্রবেশ করুন",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PaisaTextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "আপনার আর্থিক তথ্য শতভাগ সুরক্ষিত ও এনক্রিপ্ট করা। Google অ্যাকাউন্ট দিয়ে সহজে প্রবেশ করুন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = PaisaTextSecondary,
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    when (authState) {
                        is AuthState.SigningIn,
                        is AuthState.GoogleAccountSelector,
                        is AuthState.AuthenticatingWithFirebase -> {
                            val statusLabel = when (authState) {
                                is AuthState.GoogleAccountSelector -> "গুগল অ্যাকাউন্ট নির্বাচন করা হচ্ছে..."
                                is AuthState.AuthenticatingWithFirebase -> "অ্যাকাউন্ট যাচাই করা হচ্ছে..."
                                else -> "গুগল সার্ভিসের সাথে সংযোগ করা হচ্ছে..."
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = PaisaTealPrimary,
                                    modifier = Modifier.size(40.dp),
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    text = statusLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PaisaTextPrimary,
                                    textAlign = TextAlign.Center
                                )

                                OutlinedButton(
                                    onClick = onCancelLoading,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("বাতিল করুন", fontSize = 12.sp)
                                }
                            }
                        }

                        is AuthState.Timeout -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = PaisaGoldAmber.copy(alpha = 0.12f)),
                                border = BorderStroke(1.dp, PaisaGoldAmber.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Schedule, contentDescription = null, tint = PaisaGoldAmber, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("সময়সীমা অতিক্রম করেছে", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PaisaTextPrimary)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "সংযোগ করতে অতিরিক্ত সময় লেগেছে। দয়া করে ইন্টারনেট সংযোগ চেক করে আবার চেষ্টা করুন।",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PaisaTextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = onSignInClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("আবার চেষ্টা করুন", fontWeight = FontWeight.Bold)
                            }
                        }

                        is AuthState.Error -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = PaisaExpenseLight),
                                border = BorderStroke(1.dp, PaisaExpenseRed.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = PaisaExpenseRed, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(authState.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PaisaExpenseRed)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = authState.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PaisaTextPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = onSignInClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary)
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Google দিয়ে পুনরায় চেষ্টা করুন", fontWeight = FontWeight.Bold)
                            }
                        }

                        else -> {
                            // Primary Clean Google Sign-In Action
                            Button(
                                onClick = onSignInClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PaisaTealPrimary,
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "Google Sign In",
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Google দিয়ে চালিয়ে যান",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
