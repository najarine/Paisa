package com.paisa.najarine.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.ui.theme.*

@Composable
fun GenericFeatureDialog(
    title: String,
    subtitle: String,
    initialValue: String = "",
    label: String = "বিবরণ বা পরিমাণ",
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var textInput by remember { mutableStateOf(initialValue) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(subtitle, fontSize = 12.sp, color = PaisaTextSecondary)
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text(label) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting,
                onClick = {
                    if (isSubmitting) return@Button
                    isSubmitting = true
                    onSave(textInput)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isSubmitting) "সংরক্ষণ হচ্ছে..." else "সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSubmitting,
                onClick = onDismiss
            ) {
                Text("বাতিল", color = PaisaTextSecondary)
            }
        }
    )
}
