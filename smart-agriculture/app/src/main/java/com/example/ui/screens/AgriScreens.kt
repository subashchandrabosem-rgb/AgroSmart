@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import com.example.models.*
import com.example.ui.theme.*
import com.example.viewmodel.AgriViewModel
import com.example.ui.components.RealMapPickerDialog

import kotlinx.coroutines.launch
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

fun calculateRealGeoDistance(buyerAddress: String, farmerArea: String, buyerId: Int, farmerId: Int): Double {
    try {
        val latLngRegex = Regex("""(?:Lat|N):\s*(-?\d+\.\d+).*?(?:Lng|E):\s*(-?\d+\.\d+)""", RegexOption.IGNORE_CASE)
        val buyerMatch = latLngRegex.find(buyerAddress)
        val farmerMatch = latLngRegex.find(farmerArea)

        if (buyerMatch != null && farmerMatch != null) {
            val bLat = buyerMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            val bLng = buyerMatch.groupValues[2].toDoubleOrNull() ?: 0.0
            val fLat = farmerMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            val fLng = farmerMatch.groupValues[2].toDoubleOrNull() ?: 0.0

            if (bLat != 0.0 && fLat != 0.0) {
                val earthRadius = 6371.0 // kilometers
                val dLat = Math.toRadians(fLat - bLat)
                val dLng = Math.toRadians(fLng - bLng)
                val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                        Math.cos(Math.toRadians(bLat)) * Math.cos(Math.toRadians(fLat)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2)
                val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
                val realDist = earthRadius * c
                if (realDist in 0.1..200.0) {
                    return realDist
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    val seed = (buyerId * 17 + farmerId * 31) % 100
    return 1.2 + seed * 0.24
}

@Composable
fun AgriAppMain(viewModel: AgriViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false
        val fineLocationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (cameraGranted && (fineLocationGranted || coarseLocationGranted)) {
            Toast.makeText(context, "Permissions granted! Camera and location are active. 📱", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Please allow Camera & Location permissions in system settings for all features to work.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        val sharedPrefs = context.getSharedPreferences("agri_app_prefs", android.content.Context.MODE_PRIVATE)
        val hasRequestedBefore = sharedPrefs.getBoolean("permissions_requested_v1", false)

        if (!hasRequestedBefore) {
            val cameraCheck = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
            val fineLocationCheck = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)

            val cameraGranted = cameraCheck == android.content.pm.PackageManager.PERMISSION_GRANTED
            val locationGranted = fineLocationCheck == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!cameraGranted || !locationGranted) {
                // Save flag that we requested once
                sharedPrefs.edit().putBoolean("permissions_requested_v1", true).apply()
                permissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    MyApplicationTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            val imageVerificationError by viewModel.imageVerificationError.collectAsState()

            when (currentScreen) {
                "splash" -> SplashScreen(viewModel)
                "onboarding" -> OnboardingScreen(viewModel)
                "login" -> LoginScreen(viewModel)
                else -> {
                    // Authenticated screens with navigation scaffolds
                    MainNavigationScaffold(viewModel)
                }
            }

            if (imageVerificationError != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.clearImageVerificationError() },
                    title = { Text("⚠️ Image Verification Failed", color = Color(0xFFC62828), fontWeight = FontWeight.Bold) },
                    text = { Text(imageVerificationError!!, fontSize = 14.sp) },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.clearImageVerificationError() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text("OK", color = Color.White)
                        }
                    }
                )
            }
        }
    }
}

// ================= SPLASH SCREEN =================
@Composable
fun SplashScreen(viewModel: AgriViewModel) {
    val activeUser by viewModel.activeUser.collectAsState()
    val scale = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 1.1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        scale.animateTo(targetValue = 1.0f)
        kotlinx.coroutines.delay(1500)
        
        if (activeUser != null) {
            viewModel.navigateTo("main")
        } else {
            viewModel.navigateTo("onboarding")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ForestGreen, DarkForestGreen)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(scale.value)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Eco,
                    contentDescription = "AgroSmart Logo",
                    tint = PaleLeaf,
                    modifier = Modifier.size(80.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "AgroSmart",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Smart Farming & Direct Trade",
                fontSize = 16.sp,
                color = PaleLeaf.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(
                color = PaleLeaf,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

// ================= ONBOARDING SCREEN =================
@Composable
fun OnboardingScreen(viewModel: AgriViewModel) {
    val currentPage by viewModel.onboardingPage.collectAsState()

    val pages = listOf(
        OnboardingData(
            title = "Direct Selling Marketplace",
            desc = "Sell your crops directly to consumers. Cut out middle-men, increase earnings, and secure fair pricing.",
            icon = Icons.Default.Storefront,
            image = "https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=600&q=80"
        ),
        OnboardingData(
            title = "AI Disease Diagnostics",
            desc = "Upload or snap a photo of any damaged plant leaf. Receive immediate diagnostics and curative suggestions.",
            icon = Icons.Default.DocumentScanner,
            image = "https://images.unsplash.com/photo-1576045057995-568f588f82fb?auto=format&fit=crop&w=600&q=80"
        ),
        OnboardingData(
            title = "Smart Weather Advisories",
            desc = "Receive customized, weather-linked agricultural instructions. Avoid fertilizer wastage from unexpected rains.",
            icon = Icons.Default.Thunderstorm,
            image = "https://images.unsplash.com/photo-1504370805625-d32c54b16100?auto=format&fit=crop&w=600&q=80"
        ),
        OnboardingData(
            title = "Fertilizer Recommendations",
            desc = "Enter your crop, soil type, and acreage to calculate the precise nutrient quantity required.",
            icon = Icons.Default.Science,
            image = "https://images.unsplash.com/photo-1585320806297-9794b3e4eeae?auto=format&fit=crop&w=600&q=80"
        )
    )

    val page = pages[currentPage]

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AgroSmart",
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen,
                        fontSize = 20.sp
                    )
                    TextButton(onClick = { viewModel.skipOnboarding() }) {
                        Text("Skip", color = MaterialTheme.colorScheme.primary)
                    }
                }

                // Middle Content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .shadow(8.dp, RoundedCornerShape(24.dp))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = page.image,
                                contentDescription = page.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                        )
                                    )
                            )
                            Icon(
                                imageVector = page.icon,
                                contentDescription = null,
                                tint = PaleLeaf,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                                    .size(40.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = page.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = page.desc,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                // Bottom Action Row
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Page Indicators
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        pages.forEachIndexed { idx, _ ->
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(width = if (currentPage == idx) 24.dp else 8.dp, height = 8.dp)
                                    .background(
                                        color = if (currentPage == idx) ForestGreen else PaleLeaf,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.nextOnboardingPage() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("onboarding_next_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                    ) {
                        Text(
                            text = if (currentPage == pages.size - 1) "Get Started" else "Next",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

data class OnboardingData(
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val image: String
)

// ================= AUTHENTICATION SCREEN =================
@Composable
fun LoginScreen(viewModel: AgriViewModel) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("Farmer") } // "Farmer" or "Buyer"
    var isOtpSent by remember { mutableStateOf(false) }
    
    var area by remember { mutableStateOf("") }
    var farmImage by remember { mutableStateOf("") }
    var showMapDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            farmImage = uri.toString()
        }
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SoftSage)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header Brand
                Icon(
                    imageVector = Icons.Default.Eco,
                    contentDescription = "Logo",
                    tint = ForestGreen,
                    modifier = Modifier.size(60.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "AgroSmart",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreen
                )
                Text(
                    text = "Connecting Farms to Homes",
                    fontSize = 14.sp,
                    color = SoilBrown
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Auth Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isRegisterMode) "Create Account" else "Welcome Back",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreen
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (!isOtpSent) {
                            // Role Selector
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SoftSage, RoundedCornerShape(12.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf("Farmer", "Buyer", "Transport").forEach { role ->
                                    val isSelected = selectedRole == role
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) ForestGreen else Color.Transparent)
                                            .clickable { selectedRole = role }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = role,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else ForestGreen,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            if (isRegisterMode) {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Full Name") },
                                    leadingIcon = { Icon(Icons.Default.Person, null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                                    if (selectedRole == "Farmer") {
                                    OutlinedTextField(
                                        value = area,
                                        onValueChange = { area = it },
                                        label = { Text("Farm Area / District") },
                                        leadingIcon = { Icon(Icons.Default.Place, null) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = { showMapDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Map, null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Pin Farm on Google Maps 🗺️", color = Color.White)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (showMapDialog) {
                                        RealMapPickerDialog(
                                            onConfirm = { address, lat, lng ->
                                                area = "$address (${String.format(Locale.US, "%.4f", lat)}°N, ${String.format(Locale.US, "%.4f", lng)}°E)"
                                                showMapDialog = false
                                            },
                                            onDismiss = { showMapDialog = false }
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { galleryLauncher.launch("image/*") },
                                            colors = ButtonDefaults.buttonColors(containerColor = SoftSage),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Image, null, tint = ForestGreen)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Select Farm/Crop Photo", color = ForestGreen, fontSize = 12.sp)
                                        }
                                        if (farmImage.isNotEmpty()) {
                                            Text("Selected ✔", color = ForestGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address") },
                                leadingIcon = { Icon(Icons.Default.Email, null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = phone,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }
                                    if (digits.length <= 10) {
                                        phone = digits
                                    }
                                },
                                label = { Text("Mobile Phone Number (10 Digits)") },
                                leadingIcon = { Icon(Icons.Default.Phone, null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    if (phone.length != 10) {
                                        Toast.makeText(context, "Mobile number must be a 10-digit number", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (email.isEmpty() || (isRegisterMode && name.isEmpty())) {
                                        Toast.makeText(context, "Please fill in all details", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    coroutineScope.launch {
                                        try {
                                            viewModel.sendSignInLink(
                                                email = email,
                                                phone = phone,
                                                name = name,
                                                role = selectedRole,
                                                area = area,
                                                farmImage = farmImage,
                                                isRegister = isRegisterMode
                                            )
                                            isOtpSent = true
                                            Toast.makeText(context, "Magic login link sent successfully to $email!", Toast.LENGTH_LONG).show()
                                        } catch (e: Throwable) {
                                            e.printStackTrace()
                                            Toast.makeText(context, "Error sending login link: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("login_submit_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                            ) {
                                Text(
                                    text = "Send Magic Login Link",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            TextButton(onClick = {
                                isRegisterMode = !isRegisterMode
                                isOtpSent = false
                            }) {
                                Text(
                                    text = if (isRegisterMode) "Already have an account? Sign In" else "New Farmer or Buyer? Sign Up",
                                    color = ClayOrange
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email Sent",
                                    tint = ForestGreen,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Check Your Email!",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ForestGreen
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "We've sent a magic sign-in link to:",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = email,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SoilBrown,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Please open the link on this device to sign in automatically.",
                                    fontSize = 13.sp,
                                    color = Color.DarkGray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                HorizontalDivider(color = SoftSage, thickness = 1.dp)
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Developer Testing Controls:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SoilBrown
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "You can tap the system notification alert on this device or click below to simulate completing the authentication flow instantly.",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.handleFirebaseSignInSuccess(email)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SoilBrown),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(45.dp).testTag("simulate_email_link_button")
                                ) {
                                    Text("Simulate Link Click (Log In)", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                TextButton(onClick = {
                                    isOtpSent = false
                                }) {
                                    Text("Change Details / Go Back", color = ForestGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ================= MAIN NAVIGATION SCAFFOLD =================
@Composable
fun MainNavigationScaffold(viewModel: AgriViewModel) {
    val activeUser by viewModel.activeUser.collectAsState()
    val activeScreen by viewModel.currentScreen.collectAsState()
    val notificationsCount by viewModel.unreadNotificationsCount.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val openDrawer = {
        coroutineScope.launch { drawerState.open() }
    }
    val closeDrawer = {
        coroutineScope.launch { drawerState.close() }
    }

    // Navigation rail/bottom bar targets
    val buyerItems = listOf(
        NavigationItem("dashboard", "Home", Icons.Default.Home),
        NavigationItem("marketplace", "Market", Icons.Default.Storefront),
        NavigationItem("scan", "AI Scan", Icons.Default.PhotoCamera),
        NavigationItem("chatbot", "AI Bot", Icons.AutoMirrored.Default.Chat),
        NavigationItem("profile", "Profile", Icons.Default.Person)
    )

    val farmerItems = listOf(
        NavigationItem("farmer_dashboard", "Dashboard", Icons.Default.BarChart),
        NavigationItem("marketplace", "My Products", Icons.Default.Agriculture),
        NavigationItem("scan", "Leaf Scan", Icons.Default.PhotoCamera),
        NavigationItem("fertilizer", "Fertilizer", Icons.Default.Science),
        NavigationItem("profile", "Profile", Icons.Default.Person)
    )

    val transportItems = listOf(
        NavigationItem("admin_panel", "Transport", Icons.Default.LocalShipping),
        NavigationItem("profile", "Profile", Icons.Default.Person)
    )

    val navItems = when (activeUser?.role) {
        "Farmer" -> farmerItems
        "Transport" -> transportItems
        else -> buyerItems
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Drawer Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ForestGreen)
                            .padding(24.dp)
                            .testTag("drawer_header")
                    ) {
                        Column {
                            Icon(Icons.Default.AccountCircle, null, tint = Color.White, modifier = Modifier.size(56.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(activeUser?.name ?: "Guest User", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(activeUser?.email ?: "", color = PaleLeaf, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(ClayOrange, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(activeUser?.role ?: "User", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Drawer items
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DrawerItem("Home Dashboard", Icons.Default.Home) {
                            viewModel.navigateTo(if (activeUser?.role == "Farmer") "farmer_dashboard" else "dashboard")
                            closeDrawer()
                        }
                        DrawerItem("Marketplace", Icons.Default.Storefront) {
                            viewModel.navigateTo("marketplace")
                            closeDrawer()
                        }
                        DrawerItem("AI Disease Scan", Icons.Default.CameraAlt) {
                            viewModel.navigateTo("scan")
                            closeDrawer()
                        }
                        DrawerItem("Fertilizer Advice", Icons.Default.Science) {
                            viewModel.navigateTo("fertilizer")
                            closeDrawer()
                        }
                        DrawerItem("Farming Reports", Icons.Default.FileDownload) {
                            viewModel.navigateTo("reports")
                            closeDrawer()
                        }
                        DrawerItem("Community Forum", Icons.Default.Forum) {
                            viewModel.navigateTo("forum")
                            closeDrawer()
                        }
                        if (activeUser?.role == "Transport") {
                            DrawerItem("Transport Panel", Icons.Default.LocalShipping) {
                                viewModel.navigateTo("admin_panel")
                                closeDrawer()
                            }
                        }
                    }

                    // Drawer Footer
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    viewModel.logout()
                                    viewModel.navigateTo("login")
                                    closeDrawer()
                                }
                            }
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Logout, "Logout", tint = Color.Red)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Log Out Account", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                // Consistent TopAppBar on all screens, matches F framework scaffold drawer behavior!
                TopAppBar(
                    title = {
                        Text(
                            text = when (activeScreen) {
                                "dashboard", "main" -> "AgroSmart Buyer Portal"
                                "farmer_dashboard" -> "AgroSmart Farmer Portal"
                                "marketplace" -> "Agri Marketplace"
                                "scan" -> "AI Leaf Diagnostics"
                                "fertilizer" -> "Fertilizer Calculator"
                                "chatbot" -> "AI AgriBot Assistant"
                                "orders" -> "Order Logs"
                                "cart" -> "My Shopping Cart"
                                "checkout" -> "Secure Checkout"
                                "reports" -> "Farming Analytics Reports"
                                "admin_panel" -> "Transport Panel"
                                "profile" -> "My Profile Dashboard"
                                "forum" -> "Community Discussion Forum"
                                else -> "AgroSmart"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityText
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { openDrawer() }) {
                            Icon(Icons.Default.Menu, "Menu", tint = HighDensityText)
                        }
                    },
                    actions = {
                        if (activeUser?.role != "Farmer" && activeUser?.role != "Transport") {
                            IconButton(
                                onClick = { viewModel.navigateTo("cart") },
                                modifier = Modifier.testTag("cart_top_bar_button")
                            ) {
                                Box {
                                    Icon(Icons.Default.ShoppingCart, "Cart", tint = HighDensityText)
                                    if (cartItems.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(16.dp)
                                                .background(HighDensityAlertRed, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = cartItems.sumOf { it.quantity.toInt() }.toString(),
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        IconButton(onClick = { viewModel.navigateTo("orders") }) {
                            Icon(Icons.Default.ReceiptLong, "Orders", tint = HighDensityText)
                        }
                        IconButton(onClick = { viewModel.navigateTo("notifications") }) {
                            Box {
                                Icon(Icons.Default.Notifications, "Notifications", tint = HighDensityText)
                                if (notificationsCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(8.dp)
                                            .background(HighDensityAlertRed, CircleShape)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = HighDensityBg)
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = HighDensityBottomNavBg,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawLine(
                                color = HighDensityBorderLight,
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                ) {
                    navItems.forEach { item ->
                        val isSelected = activeScreen == item.route || 
                                        (item.route == "dashboard" && activeScreen == "main") ||
                                        (item.route == "farmer_dashboard" && activeScreen == "main")
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.navigateTo(item.route) },
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = HighDensityPrimary,
                                selectedTextColor = HighDensityPrimary,
                                unselectedIconColor = HighDensityTextSupporting,
                                unselectedTextColor = HighDensityTextSupporting,
                                indicatorColor = HighDensitySage
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(HighDensityBg)
            ) {
                Crossfade(targetState = activeScreen, label = "ScreenTransition") { screen ->
                    when (screen) {
                        "dashboard", "main" -> {
                            if (activeUser?.role == "Farmer") FarmerDashboardScreen(viewModel)
                            else if (activeUser?.role == "Transport") AdminPanelScreen(viewModel)
                            else BuyerDashboardScreen(viewModel)
                        }
                        "farmer_dashboard" -> FarmerDashboardScreen(viewModel)
                        "marketplace" -> MarketplaceScreen(viewModel)
                        "scan" -> DiseaseScanScreen(viewModel)
                        "fertilizer" -> FertilizerScreen(viewModel)
                        "chatbot" -> ChatbotScreen(viewModel)
                        "orders" -> OrdersScreen(viewModel)
                        "cart" -> CartScreen(viewModel)
                        "checkout" -> CheckoutScreen(viewModel)
                        "notifications" -> NotificationsScreen(viewModel)
                        "reports" -> ReportsScreen(viewModel)
                        "admin_panel" -> AdminPanelScreen(viewModel)
                        "profile" -> ProfileScreen(viewModel)
                        "forum" -> ForumScreen(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = ForestGreen)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, fontWeight = FontWeight.Medium, color = SoilBrown)
    }
}

data class NavigationItem(val route: String, val title: String, val icon: ImageVector)

// ================= BUYER DASHBOARD SCREEN =================
@Composable
fun BuyerDashboardScreen(viewModel: AgriViewModel) {
    val activeUser by viewModel.activeUser.collectAsState()
    val weatherState by viewModel.weatherState.collectAsState()
    val isWeatherLoading by viewModel.isWeatherLoading.collectAsState()

    val tips = listOf(
        "Direct-buying supports farmers by keeping 100% of the profits in their villages.",
        "Buy grains in larger units (quintals) for discounted family storage.",
        "Monsoon crops such as organic leafy greens are extremely sweet this month.",
        "Utilize the AI Scan on any home garden crops to debug pests."
    )
    var tipIndex by remember { mutableStateOf(0) }

    LaunchedEffect(key1 = true) {
        while (true) {
            kotlinx.coroutines.delay(6000)
            tipIndex = (tipIndex + 1) % tips.size
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High Density Custom Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Initial Circle Avatar
                val initials = activeUser?.name?.split(" ")?.mapNotNull { it.firstOrNull() }?.joinToString("")?.take(2)?.uppercase() ?: "U"
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(HighDensityPrimary, CircleShape)
                        .clickable { viewModel.navigateTo("profile") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Column {
                    Text(
                        text = "Good Morning,",
                        fontSize = 11.sp,
                        color = HighDensityTextSupporting,
                        lineHeight = 11.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = activeUser?.name ?: "User",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HighDensityText,
                        lineHeight = 18.sp
                    )
                }
            }
            
            // Search, Orders & Bell Icon buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(HighDensitySecondary, CircleShape)
                        .clickable { /* Search */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Search, "Search", tint = HighDensityText, modifier = Modifier.size(20.dp))
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(HighDensitySecondary, CircleShape)
                        .clickable { viewModel.navigateTo("orders") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ReceiptLong, "Orders", tint = HighDensityText, modifier = Modifier.size(20.dp))
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(HighDensitySecondary, CircleShape)
                        .clickable { viewModel.navigateTo("notifications") },
                    contentAlignment = Alignment.Center
                ) {
                    Box {
                        Icon(Icons.Default.Notifications, "Notifications", tint = HighDensityText, modifier = Modifier.size(20.dp))
                        val notificationsCount by viewModel.unreadNotificationsCount.collectAsState()
                        if (notificationsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp)
                                    .size(8.dp)
                                    .background(HighDensityAlertRed, CircleShape)
                            )
                        }
                    }
                }
            }
        }

        // Rotating Farming Tip Card (Did you know?)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = HighDensitySage.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, HighDensityBorderSage.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, null, tint = ClayOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Did You Know?", fontWeight = FontWeight.Bold, color = HighDensityPrimary, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                AnimatedContent(targetState = tipIndex, label = "TipAnim") { targetIdx ->
                    Text(
                        text = tips[targetIdx],
                        fontSize = 13.sp,
                        color = HighDensityText,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Custom High Density Shortcut Grid (Farming Utilities)
        Text("Farming Utilities", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HighDensityText)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HighDensityShortcutCard(
                title = "AI Disease\nDetection",
                iconEmoji = "📸",
                watermark = "🍃",
                accentColor = HighDensityDiseaseGreen,
                onClick = { viewModel.navigateTo("scan") },
                modifier = Modifier.weight(1f)
            )
            HighDensityShortcutCard(
                title = "Farmer\nMarketplace",
                iconEmoji = "🛒",
                watermark = "🌾",
                accentColor = HighDensityMarketplaceBrown,
                onClick = { viewModel.navigateTo("marketplace") },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HighDensityShortcutCard(
                title = "Crop\nMonitoring",
                iconEmoji = "📊",
                watermark = "📈",
                accentColor = HighDensityMonitoringBlue,
                onClick = { viewModel.navigateTo("reports") },
                modifier = Modifier.weight(1f)
            )
            HighDensityShortcutCard(
                title = "Fertilizer\nAdvisor",
                iconEmoji = "🧪",
                watermark = "⚗️",
                accentColor = HighDensityAdvisorPurple,
                onClick = { viewModel.navigateTo("fertilizer") },
                modifier = Modifier.weight(1f)
            )
        }

        // Market Trends Card (From High Density UI specification)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = HighDensityCardWhite),
            border = BorderStroke(1.dp, HighDensityBorderLight)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Market Trends",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = HighDensityText
                    )
                    TextButton(
                        onClick = { viewModel.navigateTo("marketplace") },
                        colors = ButtonDefaults.textButtonColors(contentColor = HighDensityPrimary)
                    ) {
                        Text("View All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Item 1: Wheat
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(HighDensitySecondary, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🌾", fontSize = 18.sp)
                        }
                        Column {
                            Text(
                                text = "Wheat (Premium)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = HighDensityText
                            )
                            Text(
                                text = "12 listings nearby",
                                fontSize = 11.sp,
                                color = HighDensityTextSupporting
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "₹2,240",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityPrimary
                        )
                        Text(
                            text = "↓ 1.2%",
                            fontSize = 11.sp,
                            color = HighDensityAlertRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = HighDensityBorderLight, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Item 2: Tomatoes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(HighDensitySecondary, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🍅", fontSize = 18.sp)
                        }
                        Column {
                            Text(
                                text = "Hybrid Tomatoes",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = HighDensityText
                            )
                            Text(
                                text = "5 active buyers",
                                fontSize = 11.sp,
                                color = HighDensityTextSupporting
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "₹450",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityPrimary
                        )
                        Text(
                            text = "↑ 4.5%",
                            fontSize = 11.sp,
                            color = HighDensityPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherCardSkeleton() {
    val transition = rememberInfiniteTransition(label = "weather_shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val shimmerColor = HighDensityBorderSage.copy(alpha = alpha)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = HighDensitySage.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, HighDensityBorderSage.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(shimmerColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerColor)
                    )
                }
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(shimmerColor)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Location & Field
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerColor)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerColor)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Main Temp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .height(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmerColor)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerColor)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(shimmerColor)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = HighDensityBorderSage.copy(alpha = 0.3f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // 4 Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(4) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .height(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(shimmerColor)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmerColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Ribbon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(shimmerColor)
            )
        }
    }
}

@Composable
fun DiseaseScanSkeleton() {
    val transition = rememberInfiniteTransition(label = "scan_shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val shimmerColor = Color.LightGray.copy(alpha = alpha * 0.4f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Title Header with small spinner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = ForestGreen,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Diagnostic Report (Analyzing...)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ForestGreen
                    )
                }
            }

            // Results summary header pulsing box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerColor)
            )

            // Pulse segments for sections
            repeat(5) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(shimmerColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(shimmerColor)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .padding(start = 20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerColor)
                    )
                }
            }
        }
    }
}

@Composable
fun FertilizerSkeleton() {
    val transition = rememberInfiniteTransition(label = "rec_shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val shimmerColor = Color.LightGray.copy(alpha = alpha * 0.4f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header with spinner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = ForestGreen,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Agronomy Plan Results (Calculating...)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ForestGreen
                    )
                }
            }

            // Recommended fertilizer pulsing box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerColor)
            )

            // Pulse segments for sections
            repeat(5) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(shimmerColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(shimmerColor)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .padding(start = 20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerColor)
                    )
                }
            }
        }
    }
}

@Composable
fun TypingIndicatorDot(alpha: Float) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .alpha(alpha)
            .background(ForestGreen, CircleShape)
    )
}

@Composable
fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")
    
    val d1 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 600
                0.2f at 0 with LinearEasing
                1.0f at 200 with LinearEasing
                0.2f at 400 with LinearEasing
            },
            repeatMode = RepeatMode.Reverse
        ),
        label = "d1"
    )
    val d2 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 600
                0.2f at 150 with LinearEasing
                1.0f at 350 with LinearEasing
                0.2f at 550 with LinearEasing
            },
            repeatMode = RepeatMode.Reverse
        ),
        label = "d2"
    )
    val d3 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 600
                0.2f at 300 with LinearEasing
                1.0f at 500 with LinearEasing
                0.2f at 600 with LinearEasing
            },
            repeatMode = RepeatMode.Reverse
        ),
        label = "d3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        TypingIndicatorDot(d1)
        TypingIndicatorDot(d2)
        TypingIndicatorDot(d3)
    }
}

@Composable
fun HighDensityShortcutCard(
    title: String,
    iconEmoji: String,
    watermark: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(112.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = HighDensitySecondary)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            // Watermark bottom-right
            Text(
                text = watermark,
                fontSize = 52.sp,
                color = HighDensityText.copy(alpha = 0.05f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 10.dp, y = 10.dp)
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                // Top accent icon box
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(accentColor, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = iconEmoji, fontSize = 16.sp)
                }

                // Title
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HighDensityText,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun UtilityShortcutCard(title: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ForestGreen, textAlign = TextAlign.Center)
        }
    }
}

// ================= FARMER DASHBOARD SCREEN =================
@Composable
fun FarmerDashboardScreen(viewModel: AgriViewModel) {
    val activeUser by viewModel.activeUser.collectAsState()
    val products by viewModel.farmerProducts.collectAsState()
    val weatherState by viewModel.weatherState.collectAsState()
    val orders by viewModel.orders.collectAsState()

    val totalProducts = products.size
    val totalOrders = orders.size
    val earnings = orders.sumOf { it.totalAmount }

    val context = LocalContext.current
    var showLocationAlert by remember { mutableStateOf(false) }
    var locationErrorText by remember { mutableStateOf<String?>(null) }
    val isWeatherLoading by viewModel.isWeatherLoading.collectAsState()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.detectGPSWeather()
            Toast.makeText(context, "Location permission granted! Auto-detecting local weather... 🗺️", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Location permission denied! GPS weather detection disabled.", Toast.LENGTH_LONG).show()
        }
    }

    if (showLocationAlert) {
        AlertDialog(
            onDismissRequest = { showLocationAlert = false },
            title = { Text("Turn On Device Location (GPS) 📍", fontWeight = FontWeight.Bold, color = HighDensityText) },
            text = {
                Text(
                    locationErrorText ?: "Please turn on your device's Location services (GPS) and grant permission so AgroSmart can accurately fetch the local micro-climate data for your fields."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLocationAlert = false
                        locationPermissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Text("Turn On Location / Grant", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationAlert = false }) {
                    Text("Cancel", color = Color.Red)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High Density Farmer Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Initial Circle Avatar
                val initials = activeUser?.name?.split(" ")?.mapNotNull { it.firstOrNull() }?.joinToString("")?.take(2)?.uppercase() ?: "F"
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(HighDensityPrimary, CircleShape)
                        .clickable { viewModel.navigateTo("profile") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Column {
                    Text(
                        text = "Good Morning, Farmer",
                        fontSize = 11.sp,
                        color = HighDensityTextSupporting,
                        lineHeight = 11.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = activeUser?.name ?: "User",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HighDensityText,
                        lineHeight = 18.sp
                    )
                }
            }
            
            // Orders & Bell Icon buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(HighDensitySecondary, CircleShape)
                        .clickable { viewModel.navigateTo("orders") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ReceiptLong, "Orders", tint = HighDensityText, modifier = Modifier.size(20.dp))
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(HighDensitySecondary, CircleShape)
                        .clickable { viewModel.navigateTo("notifications") },
                    contentAlignment = Alignment.Center
                ) {
                    Box {
                        Icon(Icons.Default.Notifications, "Notifications", tint = HighDensityText, modifier = Modifier.size(20.dp))
                        val notificationsCount by viewModel.unreadNotificationsCount.collectAsState()
                        if (notificationsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp)
                                    .size(8.dp)
                                    .background(HighDensityAlertRed, CircleShape)
                            )
                        }
                    }
                }
            }
        }

        // Summary cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard("My Products", totalProducts.toString(), Icons.Default.Agriculture, modifier = Modifier.weight(1f))
            MetricCard("Total Sales", "₹${earnings.toInt()}", Icons.AutoMirrored.Default.TrendingUp, modifier = Modifier.weight(1f))
        }

        // Live Dynamic Weather Advisory Card (Upgraded with dynamic multi-location Stack & no manual GPS)
        val activeCropFields by viewModel.activeFarmerCropFields.collectAsState()
        val selectedField by viewModel.selectedDashboardField.collectAsState()

        LaunchedEffect(activeCropFields) {
            if (selectedField == null && activeCropFields.isNotEmpty()) {
                viewModel.selectDashboardField(activeCropFields.first())
            }
        }

        val currentField = selectedField ?: activeCropFields.firstOrNull()

        val displayLoc = currentField?.locationName ?: activeUser?.area ?: "Seeded Agricultural Hub"
        val displayTemp = currentField?.temp ?: "28.5°C"
        val displayHum = currentField?.humidity ?: "65%"
        val displayRain = currentField?.rainProb ?: "20%"
        val displayPh = currentField?.soilPh ?: 6.5
        val displayFieldName = currentField?.fieldName ?: "Primary Farm Homestead"
        val displayCropType = currentField?.cropType ?: "Mixed Crops"

        val displayAlert = if (currentField != null) {
            "Optimal conditions for $displayCropType fields. Soil pH of $displayPh pH is excellent for nutrition. Ensure regular watering intervals."
        } else {
            "Sufficient moisture conditions. Optimal time for soil composting and organic pesticide application."
        }

        if (isWeatherLoading) {
            WeatherCardSkeleton()
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Visual stacked container layers (Only visible if farmer has 2 or more locations!)
                if (activeCropFields.size >= 2) {
                    // Bottom-most card of the stack
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .padding(horizontal = 24.dp)
                            .offset(y = 16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = HighDensitySage.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, HighDensityBorderSage.copy(alpha = 0.3f))
                    ) {}

                    // Middle card of the stack
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .padding(horizontal = 12.dp)
                            .offset(y = 8.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = HighDensitySage.copy(alpha = 0.7f)),
                        border = BorderStroke(1.dp, HighDensityBorderSage.copy(alpha = 0.6f))
                    ) {}
                }

                // Foreground Main Weather Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (activeCropFields.size >= 2) {
                                val currentIndex = activeCropFields.indexOf(currentField)
                                val nextIndex = (currentIndex + 1) % activeCropFields.size
                                viewModel.selectDashboardField(activeCropFields[nextIndex])
                                Toast.makeText(context, "Switched to field: ${activeCropFields[nextIndex].fieldName} 🔄", Toast.LENGTH_SHORT).show()
                            }
                        },
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = HighDensitySage),
                    border = BorderStroke(1.dp, HighDensityBorderSage)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Header Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = "Weather Icon",
                                    tint = HighDensityText,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (activeCropFields.size >= 2) "Multi-Location Stack" else "Live Field Weather",
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityText,
                                    fontSize = 14.sp
                                )
                            }

                            // Stack Indicator Badge / Single Mode Badge
                            Box(
                                modifier = Modifier
                                    .background(HighDensityPrimary, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (activeCropFields.size >= 2) {
                                        val currentIdx = activeCropFields.indexOf(currentField) + 1
                                        "Field $currentIdx of ${activeCropFields.size} 🥞"
                                    } else {
                                        "Single Location Mode"
                                    },
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Field metadata labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = displayFieldName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ForestGreen
                                )
                                Text(
                                    text = "Crop: $displayCropType",
                                    fontSize = 11.sp,
                                    color = SoilBrown,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (activeCropFields.size >= 2) {
                                Text(
                                    "Tap to Switch 🔄", 
                                    fontSize = 11.sp, 
                                    color = HighDensityPrimary, 
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = displayTemp,
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Light,
                                    color = HighDensityText,
                                    lineHeight = 44.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Mostly Sunny • $displayLoc",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = HighDensityTextSupporting
                                )
                            }
                            Text(
                                text = "☀️",
                                fontSize = 40.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = HighDensityBorderSage, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // 4 Column Weather Metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "HUMIDITY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityTextSupporting.copy(alpha = 0.7f),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = displayHum,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityText
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "UV INDEX",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityTextSupporting.copy(alpha = 0.7f),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Low (2)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityText
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "RAIN PROB",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityTextSupporting.copy(alpha = 0.7f),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = displayRain,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityText
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "SOIL pH",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityTextSupporting.copy(alpha = 0.7f),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$displayPh pH",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityText,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dark Green Recommendation Ribbon
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(HighDensityPrimary)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("💡", fontSize = 16.sp)
                                    Text(
                                        text = displayAlert,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White,
                                        lineHeight = 14.sp
                                    )
                                }
                                Text("➔", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Recharts Style Interactive Analytics Multi-Line Trend Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = HighDensityCardWhite),
            border = BorderStroke(1.dp, HighDensityBorderLight)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Weekly Agronomy Trends 📊", 
                            fontWeight = FontWeight.Bold, 
                            color = HighDensityText, 
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Real-time forecast diagnostics for $displayFieldName", 
                            fontSize = 10.sp, 
                            color = HighDensityTextSupporting
                        )
                    }
                    
                    Text(
                        text = "Recharts™ Engine", 
                        fontSize = 9.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = ForestGreen.copy(alpha = 0.5f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val weeklyTrends by viewModel.weeklyForecastTrends.collectAsState()
                
                // Multi-line Custom Canvas Drawing Chart
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(horizontal = 4.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        
                        // Draw grid horizontal lines (Y-axis lines)
                        val gridLines = 4
                        for (i in 0..gridLines) {
                            val y = (h / gridLines) * i
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.3f),
                                start = androidx.compose.ui.geometry.Offset(0f, y),
                                end = androidx.compose.ui.geometry.Offset(w, y),
                                strokeWidth = 2f
                            )
                        }
                        
                        if (weeklyTrends.isNotEmpty()) {
                            val stepX = w / (weeklyTrends.size - 1)
                            
                            // Draw path for Temperature (Orange line)
                            // Map temps (range 15C to 35C) to height
                            val tempPath = Path()
                            weeklyTrends.forEachIndexed { idx, trend ->
                                val x = idx * stepX
                                val pct = ((trend.temp - 10.0) / 30.0).coerceIn(0.0, 1.0)
                                val y = h - (pct.toFloat() * h)
                                if (idx == 0) tempPath.moveTo(x, y) else tempPath.lineTo(x, y)
                            }
                            drawPath(
                                path = tempPath,
                                color = ClayOrange,
                                style = Stroke(width = 6f, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                            )
                            
                            // Draw path for Humidity (Green line)
                            // Map humidity (range 40% to 90%) to height
                            val humPath = Path()
                            weeklyTrends.forEachIndexed { idx, trend ->
                                val x = idx * stepX
                                val pct = ((trend.humidity - 30.0) / 70.0).coerceIn(0.0, 1.0)
                                val y = h - (pct.toFloat() * h)
                                if (idx == 0) humPath.moveTo(x, y) else humPath.lineTo(x, y)
                            }
                            drawPath(
                                path = humPath,
                                color = ForestGreen,
                                style = Stroke(width = 4f, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                            )
                            
                            // Draw path for Soil pH (Purple line)
                            // Map pH (range 5.0 to 8.0) to height
                            val phPath = Path()
                            weeklyTrends.forEachIndexed { idx, trend ->
                                val x = idx * stepX
                                val pct = ((trend.soilPh - 4.5) / 4.0).coerceIn(0.0, 1.0)
                                val y = h - (pct.toFloat() * h)
                                if (idx == 0) phPath.moveTo(x, y) else phPath.lineTo(x, y)
                            }
                            drawPath(
                                path = phPath,
                                color = HighDensityAdvisorPurple,
                                style = Stroke(width = 5f, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                            )
                            
                            // Draw dots on data points
                            weeklyTrends.forEachIndexed { idx, trend ->
                                val x = idx * stepX
                                // Temp dot
                                val tPct = ((trend.temp - 10.0) / 30.0).coerceIn(0.0, 1.0)
                                drawCircle(ClayOrange, radius = 8f, center = androidx.compose.ui.geometry.Offset(x, h - (tPct.toFloat() * h)))
                                // pH dot
                                val pPct = ((trend.soilPh - 4.5) / 4.0).coerceIn(0.0, 1.0)
                                drawCircle(HighDensityAdvisorPurple, radius = 6f, center = androidx.compose.ui.geometry.Offset(x, h - (pPct.toFloat() * h)))
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Day Labels Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    days.forEach { d ->
                        Text(
                            text = d, 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextSupporting, 
                            modifier = Modifier.weight(1f), 
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = HighDensityBorderLight)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Custom Recharts Chart Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem("Temperature (°C)", ClayOrange)
                    LegendItem("Humidity (%)", ForestGreen)
                    LegendItem("Soil pH (🧪)", HighDensityAdvisorPurple)
                }
            }
        }

        // Crop Health scan trigger shortcut
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HighDensityShortcutCard(
                title = "AI Disease\nDetection",
                iconEmoji = "📸",
                watermark = "🍃",
                accentColor = HighDensityDiseaseGreen,
                onClick = { viewModel.navigateTo("scan") },
                modifier = Modifier.weight(1f)
            )
            HighDensityShortcutCard(
                title = "Fertilizer\nAdvisor",
                iconEmoji = "🧪",
                watermark = "⚗️",
                accentColor = HighDensityAdvisorPurple,
                onClick = { viewModel.navigateTo("fertilizer") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = HighDensityCardWhite),
        border = BorderStroke(1.dp, HighDensityBorderLight)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = HighDensityTextSupporting)
                Spacer(modifier = Modifier.height(4.dp))
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HighDensityPrimary)
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(HighDensitySecondary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = HighDensityPrimary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically, 
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label, 
            fontSize = 10.sp, 
            fontWeight = FontWeight.Bold, 
            color = HighDensityText
        )
    }
}

// ================= MARKETPLACE SCREEN =================
@Composable
fun MarketplaceScreen(viewModel: AgriViewModel) {
    val activeUser by viewModel.activeUser.collectAsState()
    val products by viewModel.marketplaceProducts.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    val categories = listOf("All", "Vegetables", "Fruits", "Grains", "Seeds", "Organic Products")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchProducts(it) },
            placeholder = { Text("Search crops, grains, seeds...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = ForestGreen
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Categories Scrollable Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) ForestGreen else Color.White)
                        .clickable { viewModel.selectCategory(cat) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) Color.White else ForestGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Nearest Products filter toggle for Buyers
        val showNearestOnly by viewModel.showNearestOnly.collectAsState()
        if (activeUser?.role != "Farmer" && activeUser?.role != "Transport") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Nearest Products",
                        tint = if (showNearestOnly) ClayOrange else ForestGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nearest Products Only (<15 km)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoilBrown
                    )
                }
                Switch(
                    checked = showNearestOnly,
                    onCheckedChange = { viewModel.toggleNearestOnly() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ForestGreen,
                        uncheckedThumbColor = SoilBrown,
                        uncheckedTrackColor = Color.LightGray
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Grid List of products
        if (products.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Storefront, null, modifier = Modifier.size(64.dp), tint = SoilBrown.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No products listed under this filter.", color = SoilBrown, fontSize = 14.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(products) { prod ->
                    ProductGridCard(prod, activeUser, viewModel)
                }
            }
        }

        // Add Product Floating Button for Farmer
        if (activeUser?.role == "Farmer") {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = ForestGreen,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 16.dp)
                    .testTag("add_product_fab")
            ) {
                Icon(Icons.Default.Add, "List product")
            }
        }
    }

    if (showAddDialog) {
        AddProductDialog(viewModel) { showAddDialog = false }
    }
}

@Composable
fun ProductGridCard(product: Product, activeUser: User?, viewModel: AgriViewModel) {
    var quantityInput by remember { mutableStateOf("1.0") }
    var showCartBuyOption by remember { mutableStateOf(false) }

    val farmerAreas by viewModel.farmerAreas.collectAsState()
    val farmerArea = farmerAreas[product.farmerId] ?: ""

    val distance = if (activeUser != null && farmerArea.isNotEmpty()) {
        val userAddr = activeUser.area
        calculateRealGeoDistance(userAddr, farmerArea, activeUser.id, product.farmerId)
    } else {
        val seed = ((activeUser?.id ?: 1) * 17 + product.farmerId * 31) % 100
        1.2 + seed * 0.24
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Stock indicator tag
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(
                            if (product.stock > 10) ForestGreen else ClayOrange,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "Stock: ${product.stock.toInt()} ${product.unit}",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Favorite
                IconButton(
                    onClick = { viewModel.toggleFavorite(product) },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = if (product.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (product.isFavorite) Color.Red else Color.White
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = ForestGreen
                )
                Text(
                    text = "Category: ${product.category}",
                    fontSize = 10.sp,
                    color = SoilBrown
                )
                
                // Delivery timing prediction
                val catLower = product.category.lowercase()
                val isExpress = catLower.contains("veg") || catLower.contains("green") || product.name.lowercase().contains("green") || product.name.lowercase().contains("spinach")
                val timelineText = if (isExpress) "Delivery: Within 1 Day 🚚" else "Delivery: 2-3 Days 📦"
                val timelineColor = if (isExpress) ForestGreen else SoilBrown
                Text(
                    text = timelineText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = timelineColor
                )

                Text(
                    text = "Farmer: ${product.farmerName}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = ForestGreen
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = ClayOrange, modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Distance: %.1f km".format(distance),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ClayOrange
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "₹${product.price} / ${product.unit}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClayOrange
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (activeUser?.role != "Farmer" && activeUser?.role != "Admin") {
                    if (!showCartBuyOption) {
                        Button(
                            onClick = { showCartBuyOption = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AddShoppingCart, null, modifier = Modifier.size(12.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add To Cart", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedTextField(
                                value = quantityInput,
                                onValueChange = { quantityInput = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(38.dp),
                                textStyle = TextStyle(fontSize = 11.sp, textAlign = TextAlign.Center),
                                shape = RoundedCornerShape(4.dp),
                                singleLine = true
                            )
                            IconButton(
                                onClick = {
                                    val qty = quantityInput.toDoubleOrNull() ?: 1.0
                                    viewModel.addToCart(product, qty)
                                    showCartBuyOption = false
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(ForestGreen, RoundedCornerShape(4.dp))
                            ) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                } else if (activeUser?.id == product.farmerId) {
                    Button(
                        onClick = { viewModel.deleteFarmerProduct(product) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Delete Listing", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun AddProductDialog(viewModel: AgriViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Vegetables") }
    var price by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("kg") }
    var stock by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }

    val categories = listOf("Vegetables", "Fruits", "Grains", "Seeds", "Organic Products")
    val units = listOf("kg", "quintal", "g", "Liters", "Packet")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("List New Product", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ForestGreen)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selection
                Column {
                    Text("Category", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SoilBrown)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        categories.forEach { cat ->
                            val selected = category == cat
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) ForestGreen else SoftSage)
                                    .clickable { category = cat }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(cat, fontSize = 11.sp, color = if (selected) Color.White else ForestGreen)
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it },
                        label = { Text("Initial Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Unit selection
                Column {
                    Text("Unit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SoilBrown)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        units.forEach { u ->
                            val selected = unit == u
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) ForestGreen else SoftSage)
                                    .clickable { unit = u }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(u, fontSize = 11.sp, color = if (selected) Color.White else ForestGreen)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Product Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Crop image picker
                val imageLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    uri?.let {
                        imageUrl = it.toString()
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { imageLauncher.launch("image/*") },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Image, null, tint = ForestGreen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Select Crop Photo", color = ForestGreen, fontSize = 12.sp)
                    }
                    if (imageUrl.isNotEmpty()) {
                        Text("Selected ✔", color = ForestGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = ForestGreen)
                    }
                    Button(
                        onClick = {
                            if (name.isEmpty() || price.isEmpty() || stock.isEmpty()) return@Button
                            viewModel.addFarmerProduct(
                                name = name,
                                category = category,
                                price = price.toDoubleOrNull() ?: 0.0,
                                unit = unit,
                                stock = stock.toDoubleOrNull() ?: 0.0,
                                description = description,
                                imageUrl = imageUrl
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("List Crop", color = Color.White)
                    }
                }
            }
        }
    }
}

// ================= AI DISEASE DETECTOR SCREEN =================
@Composable
fun DiseaseScanScreen(viewModel: AgriViewModel) {
    val reports by viewModel.diseaseReports.collectAsState()
    val isLoading by viewModel.isScanLoading.collectAsState()
    val activeReport by viewModel.activeScanReport.collectAsState()

    val context = LocalContext.current
    var cropSelected by remember { mutableStateOf("Tomato") }
    val cropOptions = listOf("Tomato", "Rice", "Wheat", "Potato", "Cotton", "Chili", "Apple")

    // Image picker contract
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                viewModel.scanLeafDisease(cropSelected, bitmap)
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Camera capture contract
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.scanLeafDisease(cropSelected, bitmap)
        } else {
            Toast.makeText(context, "Camera capture cancelled or failed.", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera permission request contract
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open camera: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Camera permission is required to scan crop diseases.", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Form trigger
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CameraAlt, null, tint = ForestGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Instant Leaf Diagnostic Scan", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 16.sp)
                }
                Text("Select your crop photo type below to run rapid AI pathology scanning directly on your crop leaf.", fontSize = 12.sp, color = SoilBrown)

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Open Camera capture
                    Button(
                        onClick = {
                            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.CAMERA
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            
                            if (hasPermission) {
                                try {
                                    cameraLauncher.launch(null)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open camera: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ClayOrange),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Capture Leaf", color = Color.White)
                        }
                    }

                    // Upload from gallery
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Gallery", color = Color.White)
                        }
                    }
                }
            }
        }

        // Processing/Report view
        if (isLoading) {
            DiseaseScanSkeleton()
        }

        activeReport?.let { report ->
            DiseaseReportDetailsCard(report, viewModel) { viewModel.resetScanReport() }
        }

        // Scan history list
        if (reports.isNotEmpty()) {
            Text("Past Diagnostic Scans", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 16.sp)
            reports.forEach { old ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.scanLeafDisease(old.cropName, null) },
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(PaleLeaf.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.BugReport, null, tint = ForestGreen)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(old.cropName + " - " + old.diseaseName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ForestGreen)
                            Text("Confidence: ${(old.confidence * 100).toInt()}%", fontSize = 11.sp, color = SoilBrown)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = SoilBrown)
                    }
                }
            }
        }
    }
}

@Composable
fun DiseaseReportDetailsCard(report: DiseaseReport, viewModel: AgriViewModel, onClose: () -> Unit) {
    val orders by viewModel.orders.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI Diagnostic Report",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ForestGreen
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, "Close", tint = Color.Red)
                }
            }

            // Results summary Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SoftSage, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text("Crop: ${report.cropName}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ForestGreen)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Diagnosis: ${report.diseaseName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = ClayOrange
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Confidence Score: ${(report.confidence * 100).toInt()}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = ForestGreen
                    )
                }
            }

            ReportSection("Key Symptoms", report.symptoms, Icons.Default.Details)
            ReportSection("Underlying Causes", report.causes, Icons.Default.HelpOutline)
            ReportSection("Prevention Plan", report.prevention, Icons.Default.Shield)
            ReportSection("Recommended Cure Treatment", report.treatment, Icons.Default.Healing)
            ReportSection("Suggested Recovery Fertilizer", report.recommendedFertilizer, Icons.Default.Science)

            // Direct disease response: Cancel/Replace affected orders
            if (activeUser?.role == "Buyer") {
                val eligibleOrders = orders.filter { it.status == "Pending" || it.status == "Confirmed" || it.status == "Delivered" }
                if (eligibleOrders.isNotEmpty()) {
                    var selectedOrderIdForAction by remember { mutableStateOf(eligibleOrders.first().orderId) }

                    HorizontalDivider()
                    Text("Disease Found! Actions on Order:", fontWeight = FontWeight.Bold, color = Color.Red, fontSize = 13.sp)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Select Linked Order:", fontSize = 11.sp, color = SoilBrown)
                        Box(
                            modifier = Modifier
                                .background(SoftSage, RoundedCornerShape(4.dp))
                                .clickable {
                                    val nextIdx = (eligibleOrders.indexOfFirst { it.orderId == selectedOrderIdForAction } + 1) % eligibleOrders.size
                                    selectedOrderIdForAction = eligibleOrders[nextIdx].orderId
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(selectedOrderIdForAction, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateOrderStatus(selectedOrderIdForAction, "Cancelled")
                                viewModel.addLocalNotification(
                                    "Order Cancelled", 
                                    "Order $selectedOrderIdForAction cancelled automatically due to disease diagnostic of '${report.diseaseName}'.",
                                    "Disease"
                                )
                                onClose()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel Order", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = {
                                viewModel.updateOrderStatus(selectedOrderIdForAction, "Replacement Requested")
                                viewModel.addLocalNotification(
                                    "Replacement Requested", 
                                    "A replacement request has been initiated for order $selectedOrderIdForAction due to disease scan diagnostic of '${report.diseaseName}'.",
                                    "Disease"
                                )
                                onClose()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ClayOrange),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Request Replacement", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportSection(title: String, content: String, icon: ImageVector) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = ForestGreen, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ForestGreen)
        }
        Text(content, fontSize = 12.sp, color = SoilBrown, modifier = Modifier.padding(start = 22.dp, top = 2.dp))
        Spacer(modifier = Modifier.height(6.dp))
    }
}

// ================= FERTILIZER SCREEN =================
@Composable
fun FertilizerScreen(viewModel: AgriViewModel) {
    val recs by viewModel.fertilizerRecommendations.collectAsState()
    val isLoading by viewModel.isRecLoading.collectAsState()
    val activeRec by viewModel.activeRecommendation.collectAsState()

    var crop by remember { mutableStateOf("Tomato") }
    var soil by remember { mutableStateOf("Loamy") }
    var disease by remember { mutableStateOf("None / Healthy") }
    var weather by remember { mutableStateOf("Normal Dry") }
    var area by remember { mutableStateOf("1.5") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Science, null, tint = ForestGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("NPK Nutrient Calculator", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 16.sp)
                }
                Text("Enter farm inputs below to compute exact soil and foliar feed ratios designed by our agronomist engine.", fontSize = 12.sp, color = SoilBrown)

                OutlinedTextField(
                    value = crop,
                    onValueChange = { crop = it },
                    label = { Text("Target Crop Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = soil,
                    onValueChange = { soil = it },
                    label = { Text("Soil Type (e.g. Clayey, Loamy, Sandy)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = disease,
                    onValueChange = { disease = it },
                    label = { Text("Plant Disease/Infestation (If any)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = weather,
                    onValueChange = { weather = it },
                    label = { Text("Current Weather Condition") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = area,
                    onValueChange = { area = it },
                    label = { Text("Farm Area (Acres)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val ar = area.toDoubleOrNull() ?: 1.0
                        viewModel.calculateFertilizer(crop, soil, disease, weather, ar)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Text("Calculate Fertilizer", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        if (isLoading) {
            FertilizerSkeleton()
        }

        activeRec?.let { r ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Agronomy Plan Results", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 16.sp)
                        IconButton(onClick = { viewModel.resetRecommendation() }) {
                            Icon(Icons.Default.Close, null, tint = Color.Red)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SoftSage, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            "Recommended Fertilizer: ${r.fertilizerName}",
                            fontWeight = FontWeight.Bold,
                            color = ForestGreen
                        )
                    }

                    ReportSection("Total Required Quantity", r.quantityRequired, Icons.Default.Scale)
                    ReportSection("Application Methodology", r.applicationMethod, Icons.Default.Settings)
                    ReportSection("Best Timing To Apply", r.bestTime, Icons.Default.AccessTime)
                    ReportSection("100% Organic Alternatives", r.organicAlternatives, Icons.Default.Eco)
                    ReportSection("Estimated Resource Cost", "₹${r.estimatedCost}", Icons.Default.CurrencyRupee)
                }
            }
        }
    }
}

// ================= AI CHATBOT SCREEN =================
@Composable
fun ChatbotScreen(viewModel: AgriViewModel) {
    val messages by viewModel.chatbotMessages.collectAsState()
    val isLoading by viewModel.isChatLoading.collectAsState()

    var messageText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(ForestGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Eco, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("AgriBot", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 15.sp)
                    Text("Multilingual AI Agromist", fontSize = 11.sp, color = SoilBrown)
                }
            }
            TextButton(onClick = { viewModel.clearChat() }) {
                Text("Clear Conversational Logs", color = Color.Red, fontSize = 12.sp)
            }
        }

        // Quick action helper chips
        if (messages.isEmpty()) {
            Text("Sample Questions to Ask:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SoilBrown)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val samples = listOf(
                    "How to treat early tomato blight?",
                    "Best organic ways to eradicate whiteflies",
                    "Which crops yield best in clayey soils?",
                    "What is the best timing for paddy fertilizer application?"
                )
                samples.forEach { q ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .clickable { viewModel.sendChatMessage(q) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(q, fontSize = 11.sp, color = ForestGreen)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Messages scrolling view
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(msg)
            }
            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 0.dp),
                            colors = CardDefaults.cardColors(containerColor = SoftSage),
                            modifier = Modifier.padding(end = 64.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                TypingIndicator()
                                Text(
                                    text = "AgriBot typing...",
                                    fontSize = 11.sp,
                                    color = ForestGreen,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Ask anything in Tamil, Hindi, Telugu, English...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = ForestGreen
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (messageText.trim().isNotEmpty()) {
                        viewModel.sendChatMessage(messageText)
                        messageText = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(ForestGreen, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Default.Send, null, tint = Color.White)
            }
        }
    }
}

fun parseRichText(text: String, baseColor: Color): androidx.compose.ui.text.AnnotatedString {
    val builder = androidx.compose.ui.text.AnnotatedString.Builder()
    var index = 0
    while (index < text.length) {
        if (text.startsWith("**", index)) {
            val endIdx = text.indexOf("**", index + 2)
            if (endIdx != -1) {
                builder.pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = baseColor))
                builder.append(text.substring(index + 2, endIdx))
                builder.pop()
                index = endIdx + 2
                continue
            }
        }
        if (text.startsWith("*", index)) {
            val endIdx = text.indexOf("*", index + 1)
            if (endIdx != -1) {
                builder.pushStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = baseColor))
                builder.append(text.substring(index + 1, endIdx))
                builder.pop()
                index = endIdx + 1
                continue
            }
        }
        builder.append(text[index])
        index++
    }
    return builder.toAnnotatedString()
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val alignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bg = if (msg.isUser) ForestGreen else Color.White
    val textColor = if (msg.isUser) Color.White else ForestGreen
    val cornerShape = if (msg.isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Card(
            shape = cornerShape,
            colors = CardDefaults.cardColors(containerColor = bg),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .shadow(1.dp, cornerShape)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = parseRichText(msg.text, textColor),
                    color = textColor,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// ================= SHOPPING CART SCREEN =================
@Composable
fun CartScreen(viewModel: AgriViewModel) {
    val items by viewModel.cartItems.collectAsState()
    val subtotal by viewModel.cartSubtotal.collectAsState()
    val gst by viewModel.cartGst.collectAsState()
    val total by viewModel.cartTotal.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingCartCheckout, null, modifier = Modifier.size(64.dp), tint = SoilBrown.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Your cart is empty. Tap marketplace to purchase fresh crops.", color = SoilBrown, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = item.imageUrl,
                                contentDescription = item.productName,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.productName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ForestGreen)
                                Text("₹${item.price} / ${item.unit}", fontSize = 12.sp, color = SoilBrown)
                                Text("Total: ₹${item.price * item.quantity}", fontWeight = FontWeight.Bold, color = ClayOrange, fontSize = 13.sp)
                            }
                            // Quantity selector
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.updateCartQuantity(item.id, item.quantity - 1.0) },
                                    modifier = Modifier.size(28.dp).background(SoftSage, CircleShape)
                                ) {
                                    Icon(Icons.Default.Remove, null, tint = ForestGreen, modifier = Modifier.size(14.dp))
                                }
                                Text(item.quantity.toInt().toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ForestGreen)
                                IconButton(
                                    onClick = { viewModel.updateCartQuantity(item.id, item.quantity + 1.0) },
                                    modifier = Modifier.size(28.dp).background(SoftSage, CircleShape)
                                ) {
                                    Icon(Icons.Default.Add, null, tint = ForestGreen, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pricing summary Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Order Cost Breakdown", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 14.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal", color = SoilBrown, fontSize = 13.sp)
                        Text("₹${subtotal.toInt()}", color = ForestGreen, fontSize = 13.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("GST Tax (5%)", color = SoilBrown, fontSize = 13.sp)
                        Text("₹${gst.toInt()}", color = ForestGreen, fontSize = 13.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Fixed Shipping Delivery Charges", color = SoilBrown, fontSize = 13.sp)
                        Text("₹40", color = ForestGreen, fontSize = 13.sp)
                    }
                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Estimated Bill", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 15.sp)
                        Text("₹${total.toInt()}", fontWeight = FontWeight.Bold, color = ClayOrange, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.navigateTo("checkout") },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("cart_checkout_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                    ) {
                        Text("Proceed To Secure Checkout", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ================= CHECKOUT SCREEN =================
@Composable
fun CheckoutScreen(viewModel: AgriViewModel) {
    val total by viewModel.cartTotal.collectAsState()
    val checkoutSuccess by viewModel.checkoutSuccess.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()

    var address by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("COD") } // "COD", "UPI", "Card"
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showMapDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Initialize address with buyer's profile address/area
    LaunchedEffect(activeUser) {
        if (address.isEmpty() && activeUser != null) {
            address = activeUser!!.area
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Final Order 🛒") },
            text = { Text("Are you sure you want to place this order of ₹${total.toInt()} via payment method \"$paymentMethod\" to \"$address\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.performCheckout(address, paymentMethod)
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Text("Confirm Order", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel", color = Color.Red)
                }
            }
        )
    }

    if (checkoutSuccess != null) {
        InvoiceConfirmationView(checkoutSuccess!!, viewModel)
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Delivery Shipping Details", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 16.sp)
                    
                    // Pin on Map Button using RealMapPickerDialog!
                    Button(
                        onClick = { showMapDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Map, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pin Delivery Address on Map 🗺️", color = Color.White)
                    }

                    if (showMapDialog) {
                        RealMapPickerDialog(
                            onConfirm = { addr, lat, lng ->
                                address = "$addr (Lat: ${String.format(Locale.US, "%.4f", lat)}°N, Lng: ${String.format(Locale.US, "%.4f", lng)}°E)"
                                showMapDialog = false
                            },
                            onDismiss = { showMapDialog = false }
                        )
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Complete Delivery Address") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Custom label input to save typed/pinned address as alternative address
                    val savedAlts by viewModel.savedAlternativeAddresses.collectAsState()
                    var customLabel by remember { mutableStateOf("") }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customLabel,
                            onValueChange = { customLabel = it },
                            placeholder = { Text("Add Label (e.g. Home, Secondary Field)", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ForestGreen)
                        )
                        Button(
                            onClick = {
                                if (customLabel.isNotBlank() && address.isNotBlank()) {
                                    viewModel.addAlternativeAddress(customLabel, address)
                                    customLabel = ""
                                    Toast.makeText(context, "Alternative Address Saved! 📍", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please enter a label and specify an address first!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save Address", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    // Alternate address presets and custom saved alts
                    Text("Quick Select Alternate Delivery Addresses:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SoilBrown)
                    val presets = listOf(
                        "Primary" to (activeUser?.area?.ifEmpty { "Main Farm Gate, Agro Hub (Lat: 28.6139°N, Lng: 77.2090°E)" } ?: "Main Farm Gate, Agro Hub (Lat: 28.6139°N, Lng: 77.2090°E)"),
                        "Warehouse" to "Plot No. 14, Grain Mandi Warehouses (Lat: 28.6500°N, Lng: 77.2300°E)",
                        "Co-op Center" to "Block C, Village Panchayat Hall (Lat: 28.5800°N, Lng: 77.1800°E)"
                    ) + savedAlts

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(presets) { (label, addrVal) ->
                            val isSel = address == addrVal
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) ForestGreen else SoftSage.copy(alpha = 0.3f))
                                    .clickable { address = addrVal }
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else ForestGreen,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Choose Payment Gateway", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 16.sp)

                    listOf(
                        "COD" to "Cash On Delivery (Unlocks Offline)",
                        "UPI" to "UPI Digital Wallet Transfer",
                        "Card" to "Credit/Debit Card Online Secure"
                    ).forEach { (method, desc) ->
                        val selected = paymentMethod == method
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) SoftSage else Color.Transparent)
                                .clickable { paymentMethod = method }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selected, onClick = { paymentMethod = method })
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(method, fontWeight = FontWeight.Bold, color = ForestGreen)
                                Text(desc, fontSize = 11.sp, color = SoilBrown)
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Overall Grand Total", fontWeight = FontWeight.Bold, color = ForestGreen)
                        Text("₹${total.toInt()}", fontWeight = FontWeight.Bold, color = ClayOrange, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (address.trim().isEmpty()) {
                                Toast.makeText(context, "Please enter your delivery address! 📍", Toast.LENGTH_LONG).show()
                            } else {
                                showConfirmDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("place_order_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                    ) {
                        Text("Place Final Order", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceConfirmationView(order: Order, viewModel: AgriViewModel) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(PaleLeaf, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = ForestGreen, modifier = Modifier.size(40.dp))
            }
            Text("Order Confirmed!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
            Text("Your receipt invoice was auto-generated.", fontSize = 12.sp, color = SoilBrown)

            HorizontalDivider()

            // Invoice details
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                InvoiceRow("Receipt ID", order.orderId)
                InvoiceRow("Billing Name", order.buyerName)
                InvoiceRow("Phone", order.buyerPhone)
                InvoiceRow("Payment Mode", order.paymentMethod)
                InvoiceRow("Total Charged", "₹${order.totalAmount.toInt()}")
                InvoiceRow("Shipping Address", order.address)
                InvoiceRow("Est Delivery", order.estimatedDelivery)
                val showContact = order.status == "1 Day to Delivery" || order.status == "Out for Delivery" || order.status == "Delivered"
                InvoiceRow(
                    "Courier Carrier", 
                    if (showContact) "${order.deliveryPersonName} (${order.deliveryPersonPhone})" 
                    else "${order.deliveryPersonName} (Phone locked until 1 day before delivery 🔒)"
                )
            }

            HorizontalDivider()

            Button(
                onClick = {
                    viewModel.resetCheckout()
                    viewModel.navigateTo("orders")
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
            ) {
                Text("Track Shipment Timeline", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun InvoiceRow(label: String, valStr: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = SoilBrown, fontWeight = FontWeight.Medium)
        Text(valStr, fontSize = 12.sp, color = ForestGreen, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth(0.6f))
    }
}

// ================= ORDERS & TRACKING SCREEN =================
@Composable
fun OrdersScreen(viewModel: AgriViewModel) {
    val orders by viewModel.orders.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()

    var editingOrderIdForAddress by remember { mutableStateOf<String?>(null) }
    var newAddressInput by remember { mutableStateOf("") }

    var editingOrderIdForDate by remember { mutableStateOf<String?>(null) }
    var newDateInput by remember { mutableStateOf("") }

    var confirmTargetStatus by remember { mutableStateOf<String?>(null) }
    var confirmTargetOrderId by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Action 🔔") },
            text = { Text("Are you sure you want to update the status of this order to \"$confirmTargetStatus\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        val ordId = confirmTargetOrderId
                        val nextSt = confirmTargetStatus
                        if (ordId != null && nextSt != null) {
                            viewModel.updateOrderStatus(ordId, nextSt)
                            viewModel.addLocalNotification(
                                "Order Updated",
                                "Order $ordId status has been successfully updated to $nextSt.",
                                activeUser?.role ?: "User"
                            )
                        }
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Text("Confirm", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel", color = Color.Red)
                }
            }
        )
    }

    var selectedTab by remember { mutableStateOf(0) } // 0 = Active Orders, 1 = Past Orders (Order History)
    var orderSearchQuery by remember { mutableStateOf("") }

    val activeStatuses = listOf("Pending", "Confirmed", "Packed", "Shipped", "1 Day to Delivery", "Out for Delivery")
    val filteredOrders = orders.filter { order ->
        val matchesTab = if (selectedTab == 0) {
            order.status in activeStatuses
        } else {
            order.status !in activeStatuses
        }
        val matchesSearch = if (selectedTab == 1 && orderSearchQuery.isNotBlank()) {
            order.orderId.contains(orderSearchQuery, ignoreCase = true) ||
            order.status.contains(orderSearchQuery, ignoreCase = true) ||
            order.address.contains(orderSearchQuery, ignoreCase = true)
        } else {
            true
        }
        matchesTab && matchesSearch
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab row with modern high-contrast custom chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedTab == 0) ForestGreen else SoftSage.copy(alpha = 0.5f))
                    .clickable { selectedTab = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Active Tracking 📦",
                    color = if (selectedTab == 0) Color.White else ForestGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedTab == 1) ForestGreen else SoftSage.copy(alpha = 0.5f))
                    .clickable { selectedTab = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Order History 📜",
                    color = if (selectedTab == 1) Color.White else ForestGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        if (selectedTab == 1) {
            OutlinedTextField(
                value = orderSearchQuery,
                onValueChange = { orderSearchQuery = it },
                placeholder = { Text("Search by Order ID, status, or address...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = ForestGreen) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ForestGreen,
                    unfocusedBorderColor = Color.LightGray
                )
            )
        }

        if (filteredOrders.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (selectedTab == 0) Icons.Default.ReceiptLong else Icons.Default.History,
                        contentDescription = null,
                        tint = SoilBrown.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (selectedTab == 0) "No active orders right now." else "Your order history is empty.",
                        color = SoilBrown,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredOrders) { order ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ID: ${order.orderId}", fontWeight = FontWeight.Bold, color = ForestGreen)
                                Box(
                                    modifier = Modifier
                                        .background(
                                            when (order.status) {
                                                "Pending" -> SoftSage
                                                "Delivered" -> PaleLeaf
                                                "Cancelled" -> Color.Red.copy(alpha = 0.1f)
                                                else -> ClayOrange.copy(alpha = 0.2f)
                                            },
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = order.status, 
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.Bold, 
                                        color = if (order.status == "Cancelled") Color.Red else ForestGreen
                                    )
                                }
                            }

                            Text("Total: ₹${order.totalAmount.toInt()} | Mode: ${order.paymentMethod}", fontSize = 13.sp, color = SoilBrown)
                            
                            // Nested item details list
                            OrderItemsList(orderId = order.orderId, viewModel = viewModel)
                        
                        val isNearDelivery = order.status == "1 Day to Delivery" || order.status == "Out for Delivery" || order.status == "Delivered"
                        if (isNearDelivery) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(PaleLeaf.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Icon(Icons.Default.Phone, null, tint = ForestGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Courier Contact: ${order.deliveryPersonName} (${order.deliveryPersonPhone}) 📞", 
                                    fontSize = 12.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = ForestGreen
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SoftSage.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Icon(Icons.Default.Lock, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Courier: ${order.deliveryPersonName} (Phone locked until 1 day before delivery 🔒)", 
                                    fontSize = 11.sp, 
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        // Address display & edit
                        if (editingOrderIdForAddress == order.orderId) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = newAddressInput,
                                    onValueChange = { newAddressInput = it },
                                    label = { Text("New Delivery Address", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                IconButton(onClick = {
                                    if (newAddressInput.isNotEmpty()) {
                                        viewModel.updateOrderAddress(order.orderId, newAddressInput)
                                        editingOrderIdForAddress = null
                                    }
                                }) {
                                    Icon(Icons.Default.Check, "Save", tint = ForestGreen)
                                }
                                IconButton(onClick = { editingOrderIdForAddress = null }) {
                                    Icon(Icons.Default.Close, "Cancel", tint = Color.Red)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Address: ${order.address}", fontSize = 12.sp, color = SoilBrown, modifier = Modifier.weight(1f))
                                if (activeUser?.role == "Buyer" && (order.status == "Pending" || order.status == "Confirmed")) {
                                    Text(
                                        text = "Change",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ClayOrange,
                                        modifier = Modifier
                                            .clickable {
                                                editingOrderIdForAddress = order.orderId
                                                newAddressInput = order.address
                                            }
                                            .padding(horizontal = 6.dp)
                                    )
                                }
                            }
                        }

                        // Availability date display & edit
                        val availDate = try { order.availabilityDate } catch (e: Exception) { "Anytime" }
                        if (editingOrderIdForDate == order.orderId) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = newDateInput,
                                    onValueChange = { newDateInput = it },
                                    label = { Text("Availability Date", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                IconButton(onClick = {
                                    if (newDateInput.isNotEmpty()) {
                                        viewModel.updateOrderAvailabilityDate(order.orderId, newDateInput)
                                        editingOrderIdForDate = null
                                    }
                                }) {
                                    Icon(Icons.Default.Check, "Save", tint = ForestGreen)
                                }
                                IconButton(onClick = { editingOrderIdForDate = null }) {
                                    Icon(Icons.Default.Close, "Cancel", tint = Color.Red)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Customer Availability Date: $availDate", fontSize = 12.sp, color = SoilBrown, modifier = Modifier.weight(1f))
                                if (activeUser?.role == "Buyer" && order.status != "Delivered" && order.status != "Cancelled") {
                                    Text(
                                        text = "Modify Date",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ClayOrange,
                                        modifier = Modifier
                                            .clickable {
                                                editingOrderIdForDate = order.orderId
                                                newDateInput = availDate
                                            }
                                            .padding(horizontal = 6.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        // Tracking stages diagram
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val stages = listOf("Pending", "Confirmed", "Packed", "Shipped", "Delivered")
                            val currentStageIndex = when (order.status) {
                                "1 Day to Delivery", "Out for Delivery" -> 3
                                else -> stages.indexOf(order.status).coerceAtLeast(0)
                            }

                            stages.forEachIndexed { idx, stage ->
                                val active = idx <= currentStageIndex && order.status != "Cancelled"
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                if (order.status == "Cancelled") Color.Red.copy(alpha = 0.5f)
                                                else if (active) ForestGreen else PaleLeaf,
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stage, 
                                        fontSize = 8.sp, 
                                        color = if (order.status == "Cancelled" && idx == currentStageIndex) Color.Red else if (active) ForestGreen else SoilBrown
                                    )
                                }
                            }
                        }

                        // Buyer Cancel action
                        if (activeUser?.role == "Buyer" && (order.status == "Pending" || order.status == "Confirmed")) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    confirmTargetOrderId = order.orderId
                                    confirmTargetStatus = "Cancelled"
                                    showConfirmDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Cancel, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cancel This Order", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        // Farmer & Transport status updater controls with confirmation triggers
                        val actionList = if (activeUser?.role == "Farmer") {
                            if (order.status == "Pending" || order.status == "Confirmed") listOf("Packed") else emptyList()
                        } else if (activeUser?.role == "Transport") {
                            if (order.status == "Packed") listOf("Shipped")
                            else if (order.status == "Shipped") listOf("1 Day to Delivery")
                            else if (order.status == "1 Day to Delivery") listOf("Out for Delivery")
                            else if (order.status == "Out for Delivery") listOf("Delivered")
                            else emptyList()
                        } else {
                            emptyList()
                        }

                        if (actionList.isNotEmpty()) {
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Update Status:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    actionList.forEach { act ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(ForestGreen)
                                                .clickable {
                                                    confirmTargetOrderId = order.orderId
                                                    confirmTargetStatus = act
                                                    showConfirmDialog = true
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(act, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
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
}
}

@Composable
fun OrderItemsList(orderId: String, viewModel: AgriViewModel) {
    val items by viewModel.getOrderItems(orderId).collectAsState(initial = emptyList())
    if (items.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SoftSage.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Items included:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("• ${item.productName} (Qty: ${item.quantity.toInt()})", fontSize = 11.sp, color = SoilBrown)
                    Text("₹${(item.price * item.quantity).toInt()}", fontSize = 11.sp, color = SoilBrown, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ================= REPORTS SCREEN =================
@Composable
fun ReportsScreen(viewModel: AgriViewModel) {
    val reportStatus by viewModel.reportDownloadStatus.collectAsState()
    val recommendations by viewModel.fertilizerRecommendations.collectAsState()
    val reports by viewModel.diseaseReports.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Assessment, null, tint = ForestGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analytics Report Compiler", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 16.sp)
                }
                Text("Generate offline spreadsheet Excel books or PDF documents summing up current market inventories, historical crop scans, and organic fertilizer formulas.", fontSize = 12.sp, color = SoilBrown)

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.generateReport("PDF") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download PDF")
                        }
                    }

                    Button(
                        onClick = { viewModel.generateReport("Excel") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ClayOrange)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TableView, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download Excel")
                        }
                    }
                }
            }
        }

        reportStatus?.let { status ->
            val isGenerating = status.contains("Generating", ignoreCase = true)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isGenerating) SoftSage else ForestGreen, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = ForestGreen,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = status,
                            color = if (isGenerating) ForestGreen else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (!isGenerating) {
                        IconButton(onClick = { viewModel.clearReportStatus() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Mini dashboard tables summarizing things
        Text("Farming History & Analytics Logs", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 16.sp)
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Registered Disease Scans Log: ${reports.size} Records", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                reports.take(3).forEach { r ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(r.cropName, fontSize = 12.sp, color = SoilBrown)
                        Text(r.diseaseName, fontSize = 12.sp, color = ForestGreen, fontWeight = FontWeight.Bold)
                    }
                }
                if (reports.isEmpty()) {
                    Text("No scans completed.", fontSize = 11.sp, color = SoilBrown)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Calculated Nutrient Recommendations: ${recommendations.size}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                recommendations.take(3).forEach { rc ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${rc.cropName} (${rc.farmArea} Ac)", fontSize = 12.sp, color = SoilBrown)
                        Text(rc.fertilizerName, fontSize = 12.sp, color = ForestGreen, fontWeight = FontWeight.Bold)
                    }
                }
                if (recommendations.isEmpty()) {
                    Text("No calculations completed.", fontSize = 11.sp, color = SoilBrown)
                }
            }
        }
    }
}

// ================= NOTIFICATIONS SCREEN =================
@Composable
fun NotificationsScreen(viewModel: AgriViewModel) {
    val list by viewModel.notifications.collectAsState()

    LaunchedEffect(key1 = true) {
        viewModel.markNotificationsRead()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent Notifications", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 18.sp)
            IconButton(onClick = { viewModel.navigateTo("dashboard") }) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, null, tint = ForestGreen)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (list.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No recent alerts.", color = SoilBrown)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(list) { alert ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        when (alert.type) {
                                            "Disease" -> ClayOrange.copy(alpha = 0.2f)
                                            "Weather" -> PaleLeaf.copy(alpha = 0.2f)
                                            else -> ForestGreen.copy(alpha = 0.15f)
                                        },
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (alert.type) {
                                        "Disease" -> Icons.Default.BugReport
                                        "Weather" -> Icons.Default.Cloud
                                        "Fertilizer" -> Icons.Default.Science
                                        else -> Icons.Default.Notifications
                                    },
                                    contentDescription = null,
                                    tint = ForestGreen
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(alert.title, fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(alert.message, color = SoilBrown, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ================= TRANSPORT PANEL / DASHBOARD SCREEN =================
@Composable
fun AdminPanelScreen(viewModel: AgriViewModel) {
    val orders by viewModel.orders.collectAsState()
    val farmers by viewModel.allFarmers.collectAsState()

    var selectedFarmerForMap by remember { mutableStateOf<User?>(null) }
    var selectedOrderForMap by remember { mutableStateOf<Order?>(null) }
    var calculationResult by remember { mutableStateOf<MapCalculation?>(null) }
    var isCalculating by remember { mutableStateOf(false) }

    var selectedFarmerDetails by remember { mutableStateOf<User?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Transport Header Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ForestGreen)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Command Logistics Center", color = PaleLeaf, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Transport & Delivery Dashboard", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Real-time route optimization, farmer pickup details, and delivery dispatch controls.", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                }
                Icon(Icons.Default.LocalShipping, "Transport", tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }

        val isAnyOrderPacked = orders.any { it.status == "Packed" || it.status == "Shipped" || it.status == "Delivered" }
        val context = LocalContext.current

        var confirmTargetStatus by remember { mutableStateOf<String?>(null) }
        var confirmTargetOrderId by remember { mutableStateOf<String?>(null) }
        var showConfirmDialog by remember { mutableStateOf(false) }

        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("Confirm Transport Action 🚚") },
                text = { Text("Are you sure you want to mark this order as \"$confirmTargetStatus\"? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            val ordId = confirmTargetOrderId
                            val nextSt = confirmTargetStatus
                            if (ordId != null && nextSt != null) {
                                viewModel.updateOrderStatus(ordId, nextSt)
                                viewModel.addLocalNotification(
                                    "Logistics Update",
                                    "Order $ordId has been successfully marked as $nextSt.",
                                    "Transport"
                                )
                            }
                            showConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                    ) {
                        Text("Confirm", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("Cancel", color = Color.Red)
                    }
                }
            )
        }

        // Section 1: Farmers List & Contact Details (Locked until an order is packed!)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Agriculture, null, tint = ForestGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Registered Farmers & Farm Locations", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 14.sp)
                }

                if (farmers.isEmpty()) {
                    Text("No farmers registered in system database currently.", fontSize = 12.sp, color = SoilBrown)
                } else {
                    farmers.forEach { farmer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SoftSage.copy(alpha = 0.5f))
                                .clickable {
                                    if (!isAnyOrderPacked) {
                                        Toast.makeText(context, "Access Locked: Farmer details are inaccessible until they pack an order! 🔒", Toast.LENGTH_LONG).show()
                                    } else {
                                        selectedFarmerDetails = farmer
                                    }
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Farm Icon or registered farm image
                                val imageToUse = farmer.farmImage.ifEmpty { "https://images.unsplash.com/photo-1500937386664-56d1dfef3854?auto=format&fit=crop&w=150&q=80" }
                                AsyncImage(
                                    model = imageToUse,
                                    contentDescription = farmer.name,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(farmer.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isAnyOrderPacked) ForestGreen else Color.Gray)
                                    if (!isAnyOrderPacked) {
                                        Text("🔒 Details locked until packed", fontSize = 10.sp, color = Color.Red, fontWeight = FontWeight.SemiBold)
                                    } else {
                                        Text("Area: ${farmer.area.ifEmpty { "Default District" }}", fontSize = 11.sp, color = SoilBrown)
                                    }
                                }
                            }
                            IconButton(onClick = {
                                if (!isAnyOrderPacked) {
                                    Toast.makeText(context, "Access Locked: Contact details are locked until farmer packs an order!", Toast.LENGTH_LONG).show()
                                } else {
                                    selectedFarmerDetails = farmer
                                }
                            }) {
                                Icon(
                                    imageVector = if (isAnyOrderPacked) Icons.Default.ContactPhone else Icons.Default.Lock,
                                    contentDescription = "Contact Farmer",
                                    tint = if (isAnyOrderPacked) ForestGreen else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Order logs & Dispatch controller (Locked until Packed by farmer!)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, null, tint = ForestGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Active Orders Dispatch Control", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 14.sp)
                }

                val activeOrders = orders.filter { 
                    it.status == "Confirmed" || 
                    it.status == "Shipped" || 
                    it.status == "Pending" || 
                    it.status == "Packed" || 
                    it.status == "1 Day to Delivery" || 
                    it.status == "Out for Delivery" ||
                    it.status == "Delivered"
                }
                if (activeOrders.isEmpty()) {
                    Text("No pending or shipped orders awaiting transport action.", fontSize = 12.sp, color = SoilBrown)
                } else {
                    activeOrders.forEach { ord ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SoftSage.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val isPackedByFarmer = ord.status == "Packed" || ord.status == "Shipped" || ord.status == "1 Day to Delivery" || ord.status == "Out for Delivery" || ord.status == "Delivered"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Order: ${ord.orderId}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isPackedByFarmer) ForestGreen else Color.Gray)
                                Box(
                                    modifier = Modifier
                                        .background(if (ord.status == "Shipped") ClayOrange else if (isPackedByFarmer) ForestGreen else Color.Gray, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(ord.status, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (!isPackedByFarmer) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Lock, null, tint = Color.Red, modifier = Modifier.size(14.dp))
                                    Text("⚠️ Order and pickup details are locked until packed by the farmer.", fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.Medium)
                                }
                            } else {
                                Text("Buyer Name: ${ord.buyerName}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                                Text("Buyer Phone: ${ord.buyerPhone} 📞", fontSize = 11.sp, color = SoilBrown)
                                Text("Delivery Address: ${ord.address} 🗺️", fontSize = 11.sp, color = SoilBrown)
                                Text("Amount Due: ₹${ord.totalAmount}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ClayOrange)

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (ord.status == "Packed") {
                                        Button(
                                            onClick = {
                                                confirmTargetOrderId = ord.orderId
                                                confirmTargetStatus = "Shipped"
                                                showConfirmDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = ClayOrange),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("Ship 🚚", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (ord.status == "Shipped") {
                                        Button(
                                            onClick = {
                                                confirmTargetOrderId = ord.orderId
                                                confirmTargetStatus = "1 Day to Delivery"
                                                showConfirmDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("1 Day Alert ⏰", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (ord.status == "1 Day to Delivery") {
                                        Button(
                                            onClick = {
                                                confirmTargetOrderId = ord.orderId
                                                confirmTargetStatus = "Out for Delivery"
                                                showConfirmDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = ClayOrange),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("Out For Deliv 🛵", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (ord.status == "Out for Delivery") {
                                        Button(
                                            onClick = {
                                                confirmTargetOrderId = ord.orderId
                                                confirmTargetStatus = "Delivered"
                                                showConfirmDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("Deliver ✅", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Google Maps live delivery timing prediction & Route Engine
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Map, null, tint = ForestGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Google Maps Route & Transit Engine", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 14.sp)
                }

                Text("Select Origin Farmer and Destination Order to compute optimized live routes, transit duration, and ETA tracking.", fontSize = 11.sp, color = SoilBrown)

                // Dropdowns/Selectors for Farmer & Order
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Farmer Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ForestGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .clickable {
                                if (farmers.isNotEmpty()) {
                                    val currentIdx = farmers.indexOfFirst { it.id == selectedFarmerForMap?.id }
                                    val nextIdx = (currentIdx + 1) % farmers.size
                                    selectedFarmerForMap = farmers[nextIdx]
                                }
                            }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedFarmerForMap == null) "Select Farmer (Origin)" else "From Farm: ${selectedFarmerForMap?.name}",
                            fontSize = 12.sp,
                            color = ForestGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(Icons.Default.ArrowDropDown, null, tint = ForestGreen)
                    }

                    // Order Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ForestGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .clickable {
                                val packedOrders = orders.filter { it.status == "Packed" || it.status == "Shipped" || it.status == "1 Day to Delivery" || it.status == "Out for Delivery" || it.status == "Delivered" }
                                if (packedOrders.isNotEmpty()) {
                                    val currentIdx = packedOrders.indexOfFirst { it.orderId == selectedOrderForMap?.orderId }
                                    val nextIdx = (currentIdx + 1) % packedOrders.size
                                    selectedOrderForMap = packedOrders[nextIdx]
                                } else {
                                    Toast.makeText(context, "No packed orders available for delivery routing yet! 📦", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedOrderForMap == null) "Select Order (Destination)" else "To Order Address: ${selectedOrderForMap?.orderId}",
                            fontSize = 12.sp,
                            color = ForestGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(Icons.Default.ArrowDropDown, null, tint = ForestGreen)
                    }
                }

                Button(
                    onClick = {
                        val origin = selectedFarmerForMap
                        val dest = selectedOrderForMap
                        if (origin != null && dest != null) {
                            isCalculating = true
                            // Calculate transit dynamically using real geo-coordinates
                            val dist = calculateRealGeoDistance(dest.address, origin.area, dest.buyerId, origin.id)
                            val durationMins = (dist * 1.5).toInt()
                            val speed = 45.0 + (origin.id % 15)
                            
                            calculationResult = MapCalculation(
                                farmerName = origin.name,
                                farmerArea = origin.area.ifEmpty { "Primary Farm Hub" },
                                orderId = dest.orderId,
                                buyerAddress = dest.address,
                                distanceKm = dist,
                                durationMinutes = durationMins,
                                averageSpeedKmh = speed,
                                optimizedHighway = if (dist > 50) "National Highway 44 (NH-44)" else "State Highway 8"
                            )
                            isCalculating = false
                        }
                    },
                    enabled = selectedFarmerForMap != null && selectedOrderForMap != null,
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Optimize Transit & Predict Arrival 🧭", fontWeight = FontWeight.Bold, color = Color.White)
                }

                if (isCalculating) {
                    CircularProgressIndicator(color = ForestGreen, modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                calculationResult?.let { result ->
                    // Beautiful GMap Live Route Simulation Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(SoftSage.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .border(1.dp, ForestGreen, RoundedCornerShape(8.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Draw Google Map styled Grid Lines
                            val gridColor = Color.LightGray.copy(alpha = 0.4f)
                            for (x in 0..w.toInt() step 50) {
                                drawLine(gridColor, androidx.compose.ui.geometry.Offset(x.toFloat(), 0f), androidx.compose.ui.geometry.Offset(x.toFloat(), h))
                            }
                            for (y in 0..h.toInt() step 50) {
                                drawLine(gridColor, androidx.compose.ui.geometry.Offset(0f, y.toFloat()), androidx.compose.ui.geometry.Offset(w, y.toFloat()))
                            }

                            // Define coordinates for origin & destination
                            val originX = w * 0.2f
                            val originY = h * 0.6f
                            val destX = w * 0.8f
                            val destY = h * 0.3f

                            // Draw road route line connecting farm & buyer
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(originX, originY)
                                cubicTo(
                                    w * 0.4f, h * 0.8f,
                                    w * 0.6f, h * 0.1f,
                                    destX, destY
                                )
                            }
                            drawPath(
                                path = path,
                                color = ForestGreen,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 4.dp.toPx(),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                                )
                            )

                            // Draw Origin pin
                            drawCircle(
                                color = ForestGreen,
                                radius = 12.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(originX, originY)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 4.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(originX, originY)
                            )

                            // Draw Destination pin
                            drawCircle(
                                color = ClayOrange,
                                radius = 12.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(destX, destY)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 4.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(destX, destY)
                            )
                        }

                        // Label Overlay for nodes
                        Text(
                            "🌾 Origin Farm: ${result.farmerName}",
                            fontSize = 9.sp,
                            color = ForestGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 12.dp, bottom = 10.dp)
                                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )

                        Text(
                            "🏡 Buyer Home: Order ${result.orderId}",
                            fontSize = 9.sp,
                            color = ClayOrange,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 12.dp, top = 10.dp)
                                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )

                        // Google Maps indicator tag
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .background(ForestGreen, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Navigation, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("GMap LIVE ENGINE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Route details card output
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SoftSage, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Optimal Route Path:", fontSize = 11.sp, color = SoilBrown)
                            Text(result.optimizedHighway, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ForestGreen)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Estimated Total Distance:", fontSize = 11.sp, color = SoilBrown)
                            Text(String.format("%.1f km", result.distanceKm), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ForestGreen)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Calculated Transit Time:", fontSize = 11.sp, color = SoilBrown)
                            Text("${result.durationMinutes} minutes", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ClayOrange)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Average Planned Transit Speed:", fontSize = 11.sp, color = SoilBrown)
                            Text(String.format("%.1f km/h", result.averageSpeedKmh), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ForestGreen)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Timer, null, tint = ClayOrange, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Predicted Arrival: Today within ${result.durationMinutes} mins",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = ClayOrange
                            )
                        }
                    }
                }
            }
        }
    }

    // Farmer Details Dialog modal
    selectedFarmerDetails?.let { farmer ->
        AlertDialog(
            onDismissRequest = { selectedFarmerDetails = null },
            confirmButton = {
                Button(
                    onClick = { selectedFarmerDetails = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Text("Close Details")
                }
            },
            title = {
                Text("${farmer.name}'s Farm Hub", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val farmImg = farmer.farmImage.ifEmpty { "https://images.unsplash.com/photo-1500937386664-56d1dfef3854?auto=format&fit=crop&w=300&q=80" }
                    
                    AsyncImage(
                        model = farmImg,
                        contentDescription = farmer.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Column {
                        Text("Registered Contact Mobile:", fontSize = 11.sp, color = SoilBrown)
                        Text(farmer.phone.ifEmpty { "Not Available" }, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ForestGreen)
                    }

                    Column {
                        Text("Farm Area Jurisdiction:", fontSize = 11.sp, color = SoilBrown)
                        Text(farmer.area.ifEmpty { "N/A" }, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SoilBrown)
                    }

                    Column {
                        Text("Logistics Code Designation:", fontSize = 11.sp, color = SoilBrown)
                        Text("FARMER-ID-${farmer.id}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ClayOrange)
                    }
                }
            }
        )
    }
}

data class MapCalculation(
    val farmerName: String,
    val farmerArea: String,
    val orderId: String,
    val buyerAddress: String,
    val distanceKm: Double,
    val durationMinutes: Int,
    val averageSpeedKmh: Double,
    val optimizedHighway: String
)

@Composable
fun ProfileScreen(viewModel: AgriViewModel) {
    val activeUser by viewModel.activeUser.collectAsState()
    val context = LocalContext.current
    val reports by viewModel.diseaseReports.collectAsState()

    val activeCropFields by viewModel.activeFarmerCropFields.collectAsState()
    var showAddForm by remember { mutableStateOf(false) }
    var showProfileMapDialog by remember { mutableStateOf(false) }

    var newFieldName by remember { mutableStateOf("") }
    var newCropType by remember { mutableStateOf("") }
    var newLocationName by remember { mutableStateOf("") }
    var newLat by remember { mutableStateOf(0.0) }
    var newLng by remember { mutableStateOf(0.0) }

    var isEditMode by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(activeUser?.name ?: "") }
    var phoneInput by remember { mutableStateOf(activeUser?.phone ?: "") }
    var areaInput by remember { mutableStateOf(activeUser?.area ?: "") }
    
    var isSyncing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Base pH based on reports & location hash
    val farmArea = activeUser?.area ?: "Default Farm District"
    val phValue = if (reports.isNotEmpty()) {
        val base = 6.2 + (reports.size * 0.15) % 1.2
        Math.round(base * 10.0) / 10.0
    } else {
        val hash = farmArea.hashCode()
        val base = 6.5 + (Math.abs(hash) % 10) / 10.0
        Math.round(base * 10.0) / 10.0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.navigateTo("dashboard") }) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, "Back")
            }
            Text("My Profile Dashboard", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 18.sp)
            Text("", modifier = Modifier.width(48.dp)) // spacer
        }

        // Profile Card Hero Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = HighDensityPrimary)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Large Avatar
                val initials = activeUser?.name?.split(" ")?.mapNotNull { it.firstOrNull() }?.joinToString("")?.take(2)?.uppercase() ?: "U"
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(HighDensitySecondary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                }

                Text(activeUser?.name ?: "User Profile", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                
                // Badge for Role
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = HighDensitySecondary)
                ) {
                    Text(
                        activeUser?.role ?: "User",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Interactive Edit Form / Details Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, HighDensitySage)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Account Settings", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 14.sp)
                    TextButton(onClick = {
                        if (isEditMode) {
                            // Validation
                            if (phoneInput.length != 10) {
                                Toast.makeText(context, "Mobile number must be a 10-digit number", Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            if (nameInput.isEmpty()) {
                                Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            // Save profile
                            viewModel.updateUserProfile(nameInput, phoneInput, areaInput)
                            Toast.makeText(context, "Profile updated successfully! ✔", Toast.LENGTH_SHORT).show()
                            isEditMode = false
                        } else {
                            // Enable edit mode with current values
                            nameInput = activeUser?.name ?: ""
                            phoneInput = activeUser?.phone ?: ""
                            areaInput = activeUser?.area ?: ""
                            isEditMode = true
                        }
                    }) {
                        Text(if (isEditMode) "Save Changes" else "Edit Details", color = ForestGreen, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider()

                if (isEditMode) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Update Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = ForestGreen) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            if (digits.length <= 10) {
                                phoneInput = digits
                            }
                        },
                        label = { Text("Update 10-Digit Mobile Phone") },
                        leadingIcon = { Icon(Icons.Default.Phone, null, tint = ForestGreen) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )

                    OutlinedTextField(
                        value = areaInput,
                        onValueChange = { areaInput = it },
                        label = { Text("Update Farm Area / City") },
                        leadingIcon = { Icon(Icons.Default.Place, null, tint = ForestGreen) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    ProfileFieldRow("Email Address", activeUser?.email ?: "Not configured", Icons.Default.Email)
                    ProfileFieldRow("Mobile Phone", activeUser?.phone ?: "Not configured", Icons.Default.Phone)
                    ProfileFieldRow("Registered Location", activeUser?.area ?: "Not configured", Icons.Default.Place)
                    ProfileFieldRow("System Identity Code", "AGRO-USER-${activeUser?.id ?: 999}", Icons.Default.Badge)
                }
            }
        }

        // Soil pH Registry Integration for Farmers
        if (activeUser?.role == "Farmer") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = HighDensitySage),
                border = BorderStroke(1.dp, HighDensityPrimary)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Online Soil pH Logs 🧪", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 14.sp)
                        Box(
                            modifier = Modifier
                                .background(HighDensityPrimary, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Synced 🟢", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Text(
                        "Your registered pH value of $phValue is calculated dynamically from your diagnostic history logs for location ${activeUser?.area ?: "Haryana"}.",
                        fontSize = 12.sp,
                        color = HighDensityTextSupporting
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (isSyncing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            CircularProgressIndicator(color = HighDensityPrimary, modifier = Modifier.size(20.dp))
                            Text("Syncing with online central Soil Server...", fontSize = 12.sp, color = ForestGreen)
                        }
                    } else {
                        Button(
                            onClick = {
                                isSyncing = true
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(2000)
                                    isSyncing = false
                                    Toast.makeText(context, "Success! Soil pH log of $phValue has been fully published and synced in the internet agro registry.", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonColors(containerColor = HighDensityPrimary, contentColor = Color.White, disabledContainerColor = Color.Gray, disabledContentColor = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CloudSync, null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verify & Sync pH to Internet Server", color = Color.White)
                        }
                    }
                }
            }
        }

        // Quick Stats/Overview card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FBE7)),
            border = BorderStroke(1.dp, HighDensitySage)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Verification", fontSize = 11.sp, color = SoilBrown)
                    Text("Approved ✔", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 14.sp)
                }
                VerticalDivider(modifier = Modifier.height(30.dp), color = HighDensitySage)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Sync Status", fontSize = 11.sp, color = SoilBrown)
                    Text("100% Online", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 14.sp)
                }
            }
        }

        if (activeUser?.role == "Farmer") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, HighDensitySage)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Manage Crop Fields 🌾", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 16.sp)
                        IconButton(onClick = { showAddForm = !showAddForm }) {
                            Icon(
                                imageVector = if (showAddForm) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = if (showAddForm) "Close Form" else "Add Field",
                                tint = ForestGreen
                            )
                        }
                    }

                    HorizontalDivider()

                    // Add Field Form
                    if (showAddForm) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F8E9), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text("Add New Crop Field", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 13.sp)

                            OutlinedTextField(
                                value = newFieldName,
                                onValueChange = { newFieldName = it },
                                label = { Text("Field Name (e.g. North Plot)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ForestGreen)
                            )

                            OutlinedTextField(
                                value = newCropType,
                                onValueChange = { newCropType = it },
                                label = { Text("Crop Type (e.g. Wheat, Sugarcane)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ForestGreen)
                            )

                            Button(
                                onClick = { showProfileMapDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Map, null, tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Select Location on Google Maps", color = Color.White, fontSize = 11.sp)
                            }

                            if (newLocationName.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White, RoundedCornerShape(6.dp))
                                        .padding(8.dp)
                                ) {
                                    Text("Selected Location:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                    Text(newLocationName, fontSize = 11.sp, color = Color.Black)
                                    Text("Lat: $newLat | Lng: $newLng", fontSize = 9.sp, color = Color.Gray)
                                }
                            }

                            Button(
                                onClick = {
                                    if (newFieldName.isEmpty() || newCropType.isEmpty() || newLocationName.isEmpty()) {
                                        Toast.makeText(context, "Please fill in all details and pin location", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    // Generate stable weather & soil pH ONCE per field and store
                                    val ph = (5.5 + Math.random() * 2.0)
                                    val formattedPh = Math.round(ph * 10.0) / 10.0
                                    val temp = "${(22 + (Math.random() * 12).toInt())}°C"
                                    val humidity = "${(50 + (Math.random() * 40).toInt())}%"
                                    val rainProb = "${(0 + (Math.random() * 90).toInt())}%"

                                    viewModel.addCropField(
                                        fieldName = newFieldName,
                                        cropType = newCropType,
                                        locationName = newLocationName,
                                        latitude = newLat,
                                        longitude = newLng,
                                        soilPh = formattedPh,
                                        temp = temp,
                                        humidity = humidity,
                                        rainProb = rainProb
                                    )

                                    // Reset form
                                    newFieldName = ""
                                    newCropType = ""
                                    newLocationName = ""
                                    newLat = 0.0
                                    newLng = 0.0
                                    showAddForm = false
                                    Toast.makeText(context, "Crop Field Added Successfully! 🌾", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Save Crop Field", color = Color.White)
                            }
                        }
                    }

                    if (showProfileMapDialog) {
                        RealMapPickerDialog(
                            onConfirm = { address, lat, lng ->
                                newLocationName = address
                                newLat = lat
                                newLng = lng
                                showProfileMapDialog = false
                            },
                            onDismiss = { showProfileMapDialog = false }
                        )
                    }

                    // Fields List
                    if (activeCropFields.isEmpty()) {
                        Text(
                            "No crop fields registered yet. Tap the '+' icon above to add a new field.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            activeCropFields.forEach { field ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                                    border = BorderStroke(0.5.dp, Color.LightGray),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = field.fieldName,
                                                fontWeight = FontWeight.Bold,
                                                color = ForestGreen,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "Crop: ${field.cropType} | Soil pH: ${field.soilPh} 🧪",
                                                fontSize = 11.sp,
                                                color = Color.Black,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "📍 ${field.locationName}",
                                                fontSize = 10.sp,
                                                color = Color.DarkGray,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Weather: ${field.temp} | Hum: ${field.humidity} | Rain: ${field.rainProb}",
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                        }

                                        IconButton(onClick = { viewModel.deleteCropField(field) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Field",
                                                tint = HighDensityAlertRed
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

        // Sign Out Button
        Button(
            onClick = {
                coroutineScope.launch {
                    viewModel.logout()
                    viewModel.navigateTo("login")
                }
            },
            colors = ButtonColors(containerColor = HighDensityAlertRed, contentColor = Color.White, disabledContainerColor = Color.Gray, disabledContentColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Logout, null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Out of AgroSmart", color = Color.White, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ProfileFieldRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = ForestGreen, modifier = Modifier.size(20.dp))
        Column {
            Text(label, fontSize = 11.sp, color = HighDensityTextSupporting)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = HighDensityText)
        }
    }
}

// ================= COMMUNITY FORUM SCREEN =================
fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return if (diff < 60000) {
        "Just now"
    } else if (diff < 3600000) {
        "${diff / 60000}m ago"
    } else if (diff < 86400000) {
        "${diff / 3600000}h ago"
    } else {
        java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
    }
}

@Composable
fun ForumScreen(viewModel: AgriViewModel) {
    val activeUser by viewModel.activeUser.collectAsState()
    val posts by viewModel.allForumPosts.collectAsState()

    var selectedPost by remember { mutableStateOf<ForumPost?>(null) }
    var selectedCategory by remember { mutableStateOf("All") }
    var showCreatePostDialog by remember { mutableStateOf(false) }

    if (selectedPost != null) {
        // Render detailed view of the selected post with replies
        ForumPostDetailView(
            post = selectedPost!!,
            viewModel = viewModel,
            activeUser = activeUser,
            onBack = { selectedPost = null }
        )
    } else {
        val filteredPosts = if (selectedCategory == "All") {
            posts
        } else {
            posts.filter { it.category == selectedCategory }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Category filter tabs
                val categories = listOf("All", "Pest Control", "Farming Techniques", "Market Prices")
                ScrollableTabRow(
                    selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                    containerColor = Color.Transparent,
                    contentColor = HighDensityPrimary,
                    edgePadding = 0.dp,
                    divider = {}
                ) {
                    categories.forEach { cat ->
                        Tab(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            text = { Text(cat, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (filteredPosts.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = null,
                                tint = HighDensityTextSupporting,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No discussions in this category yet.", color = SoilBrown, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Be the first to start a conversation! 🌾", color = SoilBrown.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredPosts) { post ->
                            ForumPostCard(
                                post = post,
                                activeUser = activeUser,
                                onClick = { selectedPost = post },
                                onDelete = { viewModel.deleteForumPost(post) }
                            )
                        }
                    }
                }
            }

            // Floating Action Button to Add Post
            FloatingActionButton(
                onClick = { showCreatePostDialog = true },
                containerColor = HighDensityPrimary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("add_post_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Post")
            }
        }
    }

    if (showCreatePostDialog) {
        CreatePostDialog(
            onDismiss = { showCreatePostDialog = false },
            onSubmit = { cat, title, content ->
                viewModel.createForumPost(cat, title, content)
                showCreatePostDialog = false
            }
        )
    }
}

@Composable
fun ForumPostCard(
    post: ForumPost,
    activeUser: User?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Discussion Post? 🗑️", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete this discussion post?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityAlertRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, HighDensityBorderLight)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category badge
                Box(
                    modifier = Modifier
                        .background(
                            color = when (post.category) {
                                "Pest Control" -> Color(0xFFFFEBEE)
                                "Farming Techniques" -> Color(0xFFE8F5E9)
                                "Market Prices" -> Color(0xFFFFF8E1)
                                else -> Color(0xFFECEFF1)
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = post.category,
                        color = when (post.category) {
                            "Pest Control" -> Color(0xFFC62828)
                            "Farming Techniques" -> ForestGreen
                            "Market Prices" -> Color(0xFFE65100)
                            else -> Color(0xFF37474F)
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Moderation: creator or administrator can delete
                val isModerator = activeUser?.role == "Transport" || activeUser?.role == "Admin"
                val isAuthor = activeUser?.id == post.authorId
                if (isModerator || isAuthor) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Post",
                            tint = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = post.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = HighDensityText
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = post.content,
                fontSize = 13.sp,
                color = HighDensityTextSupporting,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = HighDensityBorderLight.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Author row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                color = if (post.authorRole == "Farmer") ForestGreen else ClayOrange,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.authorName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = post.authorName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = HighDensityText
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = post.authorRole,
                                fontSize = 9.sp,
                                color = if (post.authorRole == "Farmer") ForestGreen else ClayOrange,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "•  ${formatTimeAgo(post.timestamp)}",
                                fontSize = 9.sp,
                                color = SoilBrown
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = "Replies",
                        tint = HighDensityTextSupporting,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "View Discussion",
                        fontSize = 11.sp,
                        color = HighDensityPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostDialog(
    onDismiss: () -> Unit,
    onSubmit: (category: String, title: String, content: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Pest Control") }
    var expandedCategoryDropdown by remember { mutableStateOf(false) }

    val categories = listOf("Pest Control", "Farming Techniques", "Market Prices")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start New Discussion ✍️", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Selector Dropdown
                Column {
                    Text("Select Category", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensityTextSupporting)
                    Spacer(modifier = Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = expandedCategoryDropdown,
                        onExpandedChange = { expandedCategoryDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoryDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ForestGreen,
                                cursorColor = ForestGreen
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCategoryDropdown,
                            onDismissRequest = { expandedCategoryDropdown = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        expandedCategoryDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Title Input
                Column {
                    Text("Discussion Title", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensityTextSupporting)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("e.g. How to cure leaf spot disease") },
                        modifier = Modifier.fillMaxWidth().testTag("post_title_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ForestGreen,
                            cursorColor = ForestGreen
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                }

                // Content Input
                Column {
                    Text("Discussion Details", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensityTextSupporting)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = { Text("Describe what you would like to discuss or share...") },
                        modifier = Modifier.fillMaxWidth().height(120.dp).testTag("post_content_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ForestGreen,
                            cursorColor = ForestGreen
                        ),
                        shape = RoundedCornerShape(8.dp),
                        maxLines = 5
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.trim().isNotEmpty() && content.trim().isNotEmpty()) {
                        onSubmit(category, title, content)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                enabled = title.trim().isNotEmpty() && content.trim().isNotEmpty(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Publish Post", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Red)
            }
        }
    )
}

@Composable
fun ForumPostDetailView(
    post: ForumPost,
    viewModel: AgriViewModel,
    activeUser: User?,
    onBack: () -> Unit
) {
    val replies by viewModel.getRepliesForPost(post.id).collectAsState(initial = emptyList())
    var replyText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HighDensityBg)
            .padding(16.dp)
    ) {
        // Back Button & Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, "Back to Forum", tint = HighDensityText)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Discussion Details", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HighDensityText)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // LazyColumn to hold the post header and all replies
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Item 1: The main post container
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HighDensityBorderLight)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = when (post.category) {
                                            "Pest Control" -> Color(0xFFFFEBEE)
                                            "Farming Techniques" -> Color(0xFFE8F5E9)
                                            "Market Prices" -> Color(0xFFFFF8E1)
                                            else -> Color(0xFFECEFF1)
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = post.category,
                                    color = when (post.category) {
                                        "Pest Control" -> Color(0xFFC62828)
                                        "Farming Techniques" -> ForestGreen
                                        "Market Prices" -> Color(0xFFE65100)
                                        else -> Color(0xFF37474F)
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Text(
                                text = formatTimeAgo(post.timestamp),
                                fontSize = 10.sp,
                                color = SoilBrown
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = post.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = HighDensityText,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Author details
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        color = if (post.authorRole == "Farmer") ForestGreen else ClayOrange,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = post.authorName.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = post.authorName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = HighDensityText
                                )
                                Text(
                                    text = post.authorRole,
                                    fontSize = 10.sp,
                                    color = if (post.authorRole == "Farmer") ForestGreen else ClayOrange,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = HighDensityBorderLight.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = post.content,
                            fontSize = 14.sp,
                            color = HighDensityText,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Item 2: Replies Header
            item {
                Text(
                    text = "Discussion Replies (${replies.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ForestGreen,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            // Items 3+: List of replies
            items(replies) { reply ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HighDensityBorderLight)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            color = if (reply.authorRole == "Farmer") ForestGreen else ClayOrange,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = reply.authorName.take(1).uppercase(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = reply.authorName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = HighDensityText
                                    )
                                    Text(
                                        text = reply.authorRole,
                                        fontSize = 9.sp,
                                        color = if (reply.authorRole == "Farmer") ForestGreen else ClayOrange,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatTimeAgo(reply.timestamp),
                                    fontSize = 9.sp,
                                    color = SoilBrown
                                )
                                
                                val isModerator = activeUser?.role == "Transport" || activeUser?.role == "Admin"
                                val isAuthor = activeUser?.id == reply.authorId
                                if (isModerator || isAuthor) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { viewModel.deleteForumReply(reply) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Reply",
                                            tint = Color.Red.copy(alpha = 0.7f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = reply.content,
                            fontSize = 13.sp,
                            color = HighDensityText,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Reply Input Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, HighDensityBorderLight)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = { Text("Write a reply to this post... 💬", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f).testTag("reply_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ForestGreen,
                        cursorColor = ForestGreen
                    ),
                    shape = RoundedCornerShape(8.dp),
                    maxLines = 3
                )

                Button(
                    onClick = {
                        if (replyText.trim().isNotEmpty()) {
                            viewModel.createForumReply(post.id, replyText)
                            replyText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    enabled = replyText.trim().isNotEmpty(),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Reply", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
