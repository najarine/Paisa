package com.paisa.najarine.ui.screens.profile

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.core.content.edit
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.paisa.najarine.ui.PaisaViewModel
import com.paisa.najarine.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }

    val googleName = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Paisa User"
    val googleEmail = currentUser?.email ?: ""
    val photoUrl = currentUser?.photoUrl?.toString() ?: ""

    // SharedPreferences or ViewModel backed user profile fields
    val prefs = remember { context.getSharedPreferences("user_profile_prefs", android.content.Context.MODE_PRIVATE) }

    var phoneNumber by remember { mutableStateOf(prefs.getString("phone", "") ?: "") }
    var address by remember { mutableStateOf(prefs.getString("address", "") ?: "") }
    var occupation by remember { mutableStateOf(prefs.getString("occupation", "") ?: "") }
    var currency by remember { mutableStateOf(prefs.getString("currency", "BDT (৳)") ?: "BDT (৳)") }
    var emergencyContact by remember { mutableStateOf(prefs.getString("emergency_contact", "") ?: "") }
    var bioNote by remember { mutableStateOf(prefs.getString("bio_note", "") ?: "") }

    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ব্যবহারকারী প্রোফাইল", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            isSaving = true
                            prefs.edit {
                                putString("phone", phoneNumber)
                                putString("address", address)
                                putString("occupation", occupation)
                                putString("currency", currency)
                                putString("emergency_contact", emergencyContact)
                                putString("bio_note", bioNote)
                            }

                            // Auto sync to Firebase
                            val profileData = mapOf(
                                "phoneNumber" to phoneNumber,
                                "address" to address,
                                "occupation" to occupation,
                                "currency" to currency,
                                "emergencyContact" to emergencyContact,
                                "bioNote" to bioNote,
                                "updatedAt" to System.currentTimeMillis()
                            )
                            viewModel.syncManager.autoSyncUserProfile(profileData)

                            isSaving = false
                            Toast.makeText(context, "প্রোফাইল সফলভাবে সংরক্ষিত হয়েছে!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("সংরক্ষণ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Google Account Profile Card (Locked/Verified)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            .border(2.dp, PaisaBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUrl.isNotBlank()) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            com.paisa.najarine.ui.components.PaisaLogoBadge(
                                size = 80.dp,
                                fontSize = 42.sp,
                                cornerRadius = 40.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = googleName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = googleEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = Color(0xFFE0F2FE),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF0369A1))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "গুগল অ্যাকাউন্ট তথ্য (সুরক্ষিত ও অপরিবর্তনযোগ্য)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF0369A1),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ব্যক্তিগত ও আর্থিক অতিরিক্ত তথ্য:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "এই তথ্যগুলো শুধুমাত্র আপনার ব্যক্তিগত ডিভাইস ও ক্লাউডে নিরাপদভাবে সংরক্ষিত থাকে।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("মোবাইল নম্বর") },
                placeholder = { Text("+880 1XXXXXXXXX") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = occupation,
                onValueChange = { occupation = it },
                label = { Text("পেশা / কর্মসংস্থান") },
                placeholder = { Text("যেমন: সফটওয়্যার ইঞ্জিনিয়ার, ব্যবসায়ী") },
                leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("ঠিকানা / শহর") },
                placeholder = { Text("যেমন: ধানমন্ডি, ঢাকা") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = currency,
                onValueChange = { currency = it },
                label = { Text("প্রধান হিসাব মুদ্রা (Currency)") },
                leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = emergencyContact,
                onValueChange = { emergencyContact = it },
                label = { Text("জরুরি যোগাযোগ নম্বর") },
                placeholder = { Text("পরিবারের সদস্যের নম্বর") },
                leadingIcon = { Icon(Icons.Default.ContactPhone, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = bioNote,
                onValueChange = { bioNote = it },
                label = { Text("ব্যক্তিগত নোট বা লক্ষ্য") },
                placeholder = { Text("আপনার আর্থিক দর্শন বা সংকল্প লিখুন...") },
                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    prefs.edit {
                        putString("phone", phoneNumber)
                        putString("address", address)
                        putString("occupation", occupation)
                        putString("currency", currency)
                        putString("emergency_contact", emergencyContact)
                        putString("bio_note", bioNote)
                    }

                    val profileData = mapOf(
                        "phoneNumber" to phoneNumber,
                        "address" to address,
                        "occupation" to occupation,
                        "currency" to currency,
                        "emergencyContact" to emergencyContact,
                        "bioNote" to bioNote,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    viewModel.syncManager.autoSyncUserProfile(profileData)
                    Toast.makeText(context, "প্রোফাইল সংরক্ষিত হয়েছে!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("তথ্য আপডেট করুন")
            }

            Spacer(modifier = Modifier.height(16.dp))

            var showSignOutDialog by remember { mutableStateOf(false) }

            OutlinedButton(
                onClick = { showSignOutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("অ্যাকাউন্ট থেকে লগআউট / সাইন আউট করুন", fontWeight = FontWeight.Bold)
            }

            if (showSignOutDialog) {
                AlertDialog(
                    onDismissRequest = { showSignOutDialog = false },
                    icon = { Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    title = { Text("সাইন আউট নিশ্চিতকরণ", fontWeight = FontWeight.Bold) },
                    text = { Text("আপনার সংরক্ষিত ডাটা ক্লাউডে নিরাপদে রয়েছে। আপনি কি নিশ্চিত যে অ্যাকাউন্ট থেকে সাইন আউট করতে চান?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showSignOutDialog = false
                                viewModel.signOut()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("হ্যাঁ, সাইন আউট করুন")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSignOutDialog = false }) {
                            Text("বাতিল")
                        }
                    }
                )
            }
        }
    }
}
