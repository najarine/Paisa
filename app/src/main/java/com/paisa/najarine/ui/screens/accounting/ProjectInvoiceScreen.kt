package com.paisa.najarine.ui.screens.accounting

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.ui.PaisaViewModel
import java.text.SimpleDateFormat
import java.util.*

data class InvoiceItemUi(
    val id: String = UUID.randomUUID().toString(),
    val invoiceNo: String,
    val clientName: String,
    val projectName: String,
    val totalAmount: Double,
    val status: String, // "PAID", "PENDING", "OVERDUE"
    val issueDateMillis: Long = System.currentTimeMillis(),
    val dueDateMillis: Long = System.currentTimeMillis() + (14L * 24 * 60 * 60 * 1000)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectInvoiceScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current

    // Invoices list stored in ViewModel/SharedPreferences
    val invoices by viewModel.invoices.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }

    val totalBilled = invoices.sumOf { it.totalAmount }
    val totalCollected = invoices.filter { it.status == "PAID" }.sumOf { it.totalAmount }
    val totalPending = totalBilled - totalCollected

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("প্রজেক্ট ও ইনভয়েস জেনারেশন", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "ইনভয়েস তৈরি করুন")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "ইনভয়েস তৈরি করুন")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Invoice Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ইনভয়েস ও বিলিং সারাংশ", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("মোট ইনভয়েস", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("৳ ${String.format(Locale.US, "%,.0f", totalBilled)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("আদায়কৃত", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("৳ ${String.format(Locale.US, "%,.0f", totalCollected)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                        Column {
                            Text("অপেক্ষমাণ (পাওনা)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("৳ ${String.format(Locale.US, "%,.0f", totalPending)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        }
                    }
                }
            }

            if (invoices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("কোন ইনভয়েস তৈরি করা হয়নি", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("প্রজেক্টের কাজের বিলিং ইনভয়েস তৈরি করুন এবং ক্লায়েন্টের সাথে শেয়ার করুন।", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("প্রথম ইনভয়েস তৈরি করুন")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(invoices, key = { it.id }) { inv ->
                        val isPaid = inv.status == "PAID"
                        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(inv.issueDateMillis))
                        val dueStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(inv.dueDateMillis))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(inv.projectName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("ক্লায়েন্ট: ${inv.clientName} | ইনভয়েস #${inv.invoiceNo}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    Surface(
                                        color = if (isPaid) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = if (isPaid) "পরিশোধিত" else "বকেয়া",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isPaid) Color(0xFF166534) else Color(0xFF92400E),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("বিল পরিমাণ", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Text("৳ ${String.format(Locale.US, "%,.0f", inv.totalAmount)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                val shareText = "ইনভয়েস #${inv.invoiceNo}\nপ্রজেক্ট: ${inv.projectName}\nক্লায়েন্ট: ${inv.clientName}\nমোট বিল: ৳ ${inv.totalAmount}\nপরিশোধের মেয়াদ: $dueStr\nস্ট্যাটাস: ${if (isPaid) "পরিশোধিত" else "বকেয়া"}\n\nPaisa অ্যাপ থেকে প্রেরিত।"
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_SUBJECT, "ইনভয়েস #${inv.invoiceNo}")
                                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "শেয়ার করুন"))
                                            }
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("শেয়ার")
                                        }

                                        if (!isPaid) {
                                            Button(
                                                onClick = {
                                                    viewModel.markInvoicePaid(inv.id)
                                                    Toast.makeText(context, "ইনভয়েস পরিশোধিত চিহ্নিত করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                            ) {
                                                Text("পেইড")
                                            }
                                        }

                                        IconButton(onClick = { viewModel.deleteInvoice(inv.id) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "মুছে ফেলুন", tint = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var clientName by remember { mutableStateOf("") }
        var projectName by remember { mutableStateOf("") }
        var invoiceNo by remember { mutableStateOf("INV-" + (1000 + invoices.size + 1)) }
        var amount by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("নতুন ইনভয়েস তৈরি") },
            text = {
                Column {
                    OutlinedTextField(
                        value = invoiceNo,
                        onValueChange = { invoiceNo = it },
                        label = { Text("ইনভয়েস নম্বর") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        label = { Text("প্রজেক্টের নাম / কাজের বিবরণ") },
                        placeholder = { Text("যেমন: ওয়েবসাইট ডেভেলপমেন্ট, লোগো ডিজাইন") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("ক্লায়েন্টের নাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("মোট বিলের পরিমাণ (৳)") },
                        placeholder = { Text("যেমন: 25000") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val total = amount.toDoubleOrNull() ?: 0.0
                        if (projectName.isBlank() || clientName.isBlank() || total <= 0) {
                            Toast.makeText(context, "সকল তথ্য সঠিকভাবে পূরণ করুন", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val item = InvoiceItemUi(
                            invoiceNo = invoiceNo,
                            clientName = clientName,
                            projectName = projectName,
                            totalAmount = total,
                            status = "PENDING"
                        )
                        viewModel.addInvoice(item)
                        showCreateDialog = false
                        Toast.makeText(context, "ইনভয়েস সফলভাবে তৈরি হয়েছে!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("তৈরি করুন")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}
