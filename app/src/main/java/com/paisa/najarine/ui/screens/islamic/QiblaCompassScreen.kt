package com.paisa.najarine.ui.screens.islamic

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.ui.PaisaViewModel
import com.paisa.najarine.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaCompassScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userLocation by viewModel.userLocation.collectAsState()
    val isDetectingLocation by viewModel.isDetectingLocation.collectAsState()

    val currentLat = userLocation?.latitude ?: 23.8103
    val currentLng = userLocation?.longitude ?: 90.4125
    val locationName = userLocation?.displayName ?: "ঢাকা, বাংলাদেশ"

    val qiblaBearing = remember(currentLat, currentLng) {
        viewModel.islamicRepo.calculateQiblaBearing(currentLat, currentLng)
    }
    val distanceToKaaba = remember(currentLat, currentLng) {
        viewModel.islamicRepo.calculateDistanceToKaaba(currentLat, currentLng)
    }

    var currentAzimuth by remember { mutableStateOf(0f) }
    var hasCompassSensor by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        @Suppress("DEPRECATION")
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)

        hasCompassSensor = rotationSensor != null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val azimuthInRadians = orientation[0]
                    val azimuthInDegrees = Math.toDegrees(azimuthInRadians.toDouble()).toFloat()
                    currentAzimuth = (azimuthInDegrees + 360f) % 360f
                } else {
                    @Suppress("DEPRECATION")
                    if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
                        currentAzimuth = (event.values[0] + 360f) % 360f
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor != null) {
            sensorManager?.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    val animatedRotation by animateFloatAsState(
        targetValue = -currentAzimuth,
        animationSpec = tween(durationMillis = 200),
        label = "compassRotation"
    )

    val relativeQiblaAngle = (qiblaBearing - currentAzimuth + 360f) % 360f
    val isFacingQibla = relativeQiblaAngle in 355f..360f || relativeQiblaAngle in 0f..5f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "কিবলা কম্পাস (Qibla Compass)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.detectLocationAndRefreshPrayerTimings(context) { msg ->
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        if (isDetectingLocation) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = PaisaTealPrimary)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = "GPS রিফ্রেশ", tint = PaisaTealPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaisaSurface)
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(PaisaBackground)
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Location and Bearing Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("বর্তমান অবস্থান (GPS)", fontSize = 12.sp, color = PaisaTextSecondary)
                            Text(locationName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PaisaTextPrimary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text("স্থানাঙ্ক: ${String.format(java.util.Locale.US, "%.2f°, %.2f°", currentLat, currentLng)}", fontSize = 11.sp, color = PaisaTextTertiary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isFacingQibla) PaisaIncomeGreen else PaisaTealPrimary)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (isFacingQibla) "কিবলার অভিমুখী ✓" else "কিবলা: ${qiblaBearing.roundToInt()}°",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "পবিত্র কাবা শরীফের দিক: উত্তর থেকে ${String.format(java.util.Locale.US, "%.1f°", qiblaBearing)} • দূরত্ব: প্রায় ${java.text.NumberFormat.getNumberInstance().format(distanceToKaaba)} কিমি",
                        style = MaterialTheme.typography.bodySmall,
                        color = PaisaTextSecondary
                    )
                }
            }

            // Beautiful Circular Compass Canvas
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(CircleShape)
                    .background(PaisaSurfaceVariant)
                    .border(3.dp, if (isFacingQibla) PaisaIncomeGreen else PaisaTealPrimary.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Rotating Dial
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(animatedRotation),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.minDimension / 2 - 20.dp.toPx()

                        // Outer ring
                        drawCircle(
                            color = Color.LightGray.copy(alpha = 0.4f),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    // Cardinal Directions (N, E, S, W)
                    Text("N", modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp), fontWeight = FontWeight.Bold, color = PaisaExpenseRed, fontSize = 16.sp)
                    Text("S", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp), fontWeight = FontWeight.Bold, color = PaisaTextSecondary, fontSize = 16.sp)
                    Text("E", modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp), fontWeight = FontWeight.Bold, color = PaisaTextSecondary, fontSize = 16.sp)
                    Text("W", modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp), fontWeight = FontWeight.Bold, color = PaisaTextSecondary, fontSize = 16.sp)

                    // Kaaba Direction Marker on Dial
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(qiblaBearing),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mosque,
                                contentDescription = "কিবলা",
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(24.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF15803D))
                            )
                        }
                    }
                }

                // Stationary Compass Center Needle
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "দিকনির্দেশক",
                        tint = if (isFacingQibla) PaisaIncomeGreen else PaisaTealPrimary,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${currentAzimuth.roundToInt()}°",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = PaisaTextPrimary
                    )
                }
            }

            // Calibration & Guidance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = PaisaTealPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (hasCompassSensor) {
                            "সঠিক কিবলা পেতে ফোনটিকে অনুভূমিকভাবে সমতল রাখুন এবং প্রয়োজনে ৮ (8) আকারের মতো ঘুরিয়ে ক্যালিব্রেট করুন।"
                        } else {
                            "আপনার ডিভাইসে ডিজিটাল ম্যাগনেটিক সেন্সর অনুপস্থিত থাকলে ঢাকার কিবলা কোণ (২৬২° পশ্চিম) অনুযায়ী মুখ করুন।"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        color = PaisaTextSecondary
                    )
                }
            }
        }
    }
}
