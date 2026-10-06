package com.paisa.najarine.ui.screens.accounting

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.ui.PaisaViewModel
import java.util.*

data class TaskUi(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val priority: String = "MEDIUM", // HIGH, MEDIUM, LOW
    val isCompleted: Boolean = false,
    val dueDate: String = "আজ"
)

data class HabitUi(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val streak: Int = 1,
    val isDoneToday: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskHabitScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: To-Do Tasks, 1: Habits

    val tasks by viewModel.tasks.collectAsState()
    val habits by viewModel.habits.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddHabitDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("কাজ ও অভ্যাসের তালিকা", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (selectedTab == 0) showAddTaskDialog = true else showAddHabitDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) showAddTaskDialog = true else showAddHabitDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("কাজের তালিকা (${tasks.count { !it.isCompleted }})")
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("অভ্যাস ট্র্যাকার (${habits.size})")
                        }
                    }
                )
            }

            if (selectedTab == 0) {
                // To-Do Tasks
                if (tasks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.TaskAlt, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("কোন কাজ নির্ধারিত নেই", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("দৈনন্দিন গুরুত্বপূর্ণ কাজের তালিকা ও টু-ডু নোট তৈরি করুন।", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { showAddTaskDialog = true }) {
                                Text("নতুন কাজ যুক্ত করুন")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(tasks, key = { it.id }) { task ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = task.isCompleted,
                                        onCheckedChange = { viewModel.toggleTask(task.id) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                            ),
                                            color = if (task.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text("সময়সীমা: ${task.dueDate}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }

                                    Surface(
                                        color = when (task.priority) {
                                            "HIGH" -> Color(0xFFFEE2E2)
                                            "MEDIUM" -> Color(0xFFFEF3C7)
                                            else -> Color(0xFFDCFCE7)
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = when (task.priority) {
                                                "HIGH" -> "জরুরি"
                                                "MEDIUM" -> "মাঝারি"
                                                else -> "সাধারণ"
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = when (task.priority) {
                                                "HIGH" -> Color(0xFF991B1B)
                                                "MEDIUM" -> Color(0xFF92400E)
                                                else -> Color(0xFF166534)
                                            }
                                        )
                                    }

                                    IconButton(onClick = { viewModel.deleteTask(task.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Habits
                if (habits.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFFF97316).copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("কোন অভ্যাস যুক্ত করা হয়নি", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("৫ ওয়াক্ত নামাজ, সকালের ব্যায়াম, বই পড়া বা দৈনিক হিসাব লেখার অভ্যাস গড়ে তুলুন।", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { showAddHabitDialog = true }) {
                                Text("নতুন অভ্যাস যুক্ত করুন")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(habits, key = { it.id }) { habit ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(habit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("ধারাবাহিকতা: ${habit.streak} দিন", style = MaterialTheme.typography.bodySmall, color = Color(0xFFEA580C), fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Button(
                                        onClick = { viewModel.toggleHabit(habit.id) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (habit.isDoneToday) Color(0xFF10B981) else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (habit.isDoneToday) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (habit.isDoneToday) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (habit.isDoneToday) "আজ সম্পন্ন" else "টিক দিন")
                                    }

                                    IconButton(onClick = { viewModel.deleteHabit(habit.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Task Dialog
    if (showAddTaskDialog) {
        var title by remember { mutableStateOf("") }
        var priority by remember { mutableStateOf("MEDIUM") }

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("নতুন কাজ যুক্ত করুন") },
            text = {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("কাজের বিবরণ") },
                        placeholder = { Text("যেমন: বিদ্যুৎ বিল পরিশোধ, মাসিক হিসাব ক্লোজ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("অগ্রাধিকার (Priority):", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = priority == "HIGH", onClick = { priority = "HIGH" }, label = { Text("জরুরি") })
                        FilterChip(selected = priority == "MEDIUM", onClick = { priority = "MEDIUM" }, label = { Text("মাঝারি") })
                        FilterChip(selected = priority == "LOW", onClick = { priority = "LOW" }, label = { Text("সাধারণ") })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            Toast.makeText(context, "কাজের বিবরণ লিখুন", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addTask(TaskUi(title = title, priority = priority))
                        showAddTaskDialog = false
                    }
                ) {
                    Text("যোগ করুন")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddTaskDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Add Habit Dialog
    if (showAddHabitDialog) {
        var name by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddHabitDialog = false },
            title = { Text("নতুন দৈনিক অভ্যাস") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("অভ্যাসের নাম") },
                        placeholder = { Text("যেমন: ফজরের নামাজ, ৩০ মিনিট বই পড়া") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            Toast.makeText(context, "অভ্যাসের নাম লিখুন", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addHabit(HabitUi(name = name))
                        showAddHabitDialog = false
                    }
                ) {
                    Text("যোগ করুন")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddHabitDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}
