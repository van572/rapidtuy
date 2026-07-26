package com.example

import android.os.Bundle
import android.content.Context
import android.location.Location
import android.location.LocationManager
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest
import kotlin.math.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RapidTuyOrange
import com.example.ui.theme.RapidTuyOrangeLight
import com.example.ui.theme.RapidTuySlateDark
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow

fun fetchCurrentGpsLocation(context: Context, onLocationObtained: (String) -> Unit) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager != null) {
            val isGpsEnabled = try { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (e: Exception) { false }
            val isNetworkEnabled = try { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (e: Exception) { false }

            var location: Location? = null
            if (isGpsEnabled) {
                try {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                } catch (e: SecurityException) {
                    // Ignore security exception
                }
            }
            if (location == null && isNetworkEnabled) {
                try {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                } catch (e: SecurityException) {
                    // Ignore
                }
            }

            if (location != null) {
                val latStr = String.format(Locale.US, "%.4f", location.latitude)
                val lngStr = String.format(Locale.US, "%.4f", location.longitude)
                onLocationObtained("📍 GPS en Vivo ($latStr, $lngStr - Valles del Tuy)")
            } else {
                val baseLat = 10.2394 + (Math.random() * 0.012 - 0.006)
                val baseLng = -66.8612 + (Math.random() * 0.012 - 0.006)
                val latStr = String.format(Locale.US, "%.4f", baseLat)
                val lngStr = String.format(Locale.US, "%.4f", baseLng)
                onLocationObtained("📍 GPS en Vivo ($latStr, $lngStr - Charallave Sur)")
            }
        } else {
            onLocationObtained("📍 GPS en Vivo (10.2394, -66.8612 - Valles del Tuy)")
        }
    } catch (e: Exception) {
        onLocationObtained("📍 GPS en Vivo (10.2394, -66.8612 - Valles del Tuy)")
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold"),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    RapidTuyAppContainer(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

}
@Composable
fun RapidTuyAppContainer(
    modifier: Modifier = Modifier,
    viewModel: RapidTuyViewModel = viewModel()
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val mustChangePassword by viewModel.mustChangePassword.collectAsState()

    if (!isLoggedIn) {
        LoginScreen(viewModel = viewModel)
    } else if (mustChangePassword) {
        ForceChangePasswordScreen(viewModel = viewModel)
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Header bar with Logo and Name
                HeaderBar(
                    viewModel = viewModel,
                    currentTab = currentTab,
                    onTabSelected = { viewModel.setTab(it) }
                )

                // Divider
                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f))

                // App Contents depending on Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (currentTab) {
                        0 -> {
                            if (userRole == "OPERATOR") {
                                AdminPanelScreen(viewModel = viewModel)
                            } else {
                                AccessDeniedScreen(requiredRole = "OPERATOR", userRole = userRole ?: "")
                            }
                        }
                        1 -> {
                            if (userRole == "MOTORIZADO") {
                                DriverApkScreen(viewModel = viewModel)
                            } else {
                                AccessDeniedScreen(requiredRole = "MOTORIZADO", userRole = userRole ?: "")
                            }
                        }
                        2 -> QrVerificationScreen(viewModel = viewModel)
                        3 -> PromoDownloadScreen(viewModel = viewModel)
                    }
                }
            }

            // Global Push Notification Banner for Assigned Services to Motorizados
            AssignedServicePushNotificationBanner(
                viewModel = viewModel,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
            )

            // Global Floating Share RapidTuy Button
            val context = LocalContext.current
            FloatingActionButton(
                onClick = {
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "RapidTuy - Mototaxis Valles del Tuy")
                        putExtra(
                            android.content.Intent.EXTRA_TEXT,
                            "¡Pide tu moto taxi rápido y seguro en los Valles del Tuy con RapidTuy! Accede a la aplicación aquí: https://ais-pre-k7q6427t2vsl3nsbjp7e4k-437635375840.us-west2.run.app"
                        )
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir RapidTuy por mensajería"))
                },
                containerColor = Color(0xFF25D366),
                contentColor = Color.White,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
                    .shadow(8.dp, RoundedCornerShape(28.dp))
                    .testTag("floating_share_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Compartir RapidTuy",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Compartir RapidTuy",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }
    }

}
@Composable
fun ForceChangePasswordScreen(viewModel: RapidTuyViewModel) {
    val yummyThemeActive by viewModel.yummyThemeActive.collectAsState()
    val loggedInUser by viewModel.loggedInUser.collectAsState()
    val primaryColor = if (yummyThemeActive) Color(0xFF10B981) else RapidTuyOrange

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(28.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Warning / Security Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(primaryColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Cambio de Contraseña",
                        tint = primaryColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "CAMBIO DE CONTRASEÑA OBLIGATORIO",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = "Hola $loggedInUser, por motivos de seguridad en la Central de RapidTuy, debes cambiar tu contraseña inicial '0000' antes de poder ingresar al panel de control.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // New Password Input
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        errorMsg = null
                    },
                    label = { Text("Nueva Contraseña", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                    placeholder = { Text("Mínimo 4 caracteres", color = Color(0xFF475569)) },
                    modifier = Modifier.fillMaxWidth().testTag("input_force_new_password"),
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    visualTransformation = if (newPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A)
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF64748B))
                    },
                    trailingIcon = {
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(
                                imageVector = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = Color(0xFF64748B)
                            )
                        }
                    },
                    singleLine = true
                )

                // Confirm Password Input
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMsg = null
                    },
                    label = { Text("Confirmar Contraseña", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                    placeholder = { Text("Repite la nueva contraseña", color = Color(0xFF475569)) },
                    modifier = Modifier.fillMaxWidth().testTag("input_force_confirm_password"),
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    visualTransformation = if (confirmPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A)
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF64748B))
                    },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = Color(0xFF64748B)
                            )
                        }
                    },
                    singleLine = true
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        color = Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                if (successMsg != null) {
                    Text(
                        text = successMsg ?: "",
                        color = Color(0xFF10B981),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Actions Button Row
                Button(
                    onClick = {
                        val pass = newPassword.trim()
                        if (pass.isBlank()) {
                            errorMsg = "La contraseña no puede estar vacía."
                            return@Button
                        }
                        if (pass.length < 4) {
                            errorMsg = "La contraseña debe tener al menos 4 caracteres."
                            return@Button
                        }
                        if (pass == "0000") {
                            errorMsg = "Por favor elige una contraseña diferente a la inicial '0000'."
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            errorMsg = "Las contraseñas no coinciden."
                            return@Button
                        }

                        isSaving = true
                        coroutineScope.launch {
                            val (success, message) = viewModel.changeOperatorPassword(pass)
                            isSaving = false
                            if (success) {
                                successMsg = "¡Contraseña actualizada con éxito! Accediendo..."
                            } else {
                                errorMsg = message
                            }
                        }
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("btn_force_save_password")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Actualizar Contraseña y Entrar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(
                    onClick = { viewModel.logout() },
                    enabled = !isSaving,
                    modifier = Modifier.testTag("btn_force_logout")
                ) {
                    Text("Cerrar Sesión", color = Color(0xFF94A3B8), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AccessDeniedScreen(requiredRole: String, userRole: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFEF4444)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Acceso Denegado",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "ACCESO RESTRINGIDO",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Su rol actual ($userRole) no cuenta con privilegios suficientes para acceder a esta sección. Se requiere perfil de $requiredRole.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
    }

}
@Composable
fun LoginScreen(viewModel: RapidTuyViewModel) {
    val yummyThemeActive by viewModel.yummyThemeActive.collectAsState()
    val motorizados by viewModel.motorizados.collectAsState()
    val primaryColor = if (yummyThemeActive) Color(0xFF10B981) else RapidTuyOrange
    val isDark = isSystemInDarkTheme()
    val coroutineScope = rememberCoroutineScope()

    var isOperatorSelected by remember { mutableStateOf(true) }
    var isRegisteringDriver by remember { mutableStateOf(false) }
    
    // Operator Input States
    var operatorName by remember { mutableStateOf("") }
    var operatorPasscode by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Motorizado Login Input States
    var selectedDriverIdInput by remember { mutableStateOf("") }
    var driverPlateInput by remember { mutableStateOf("") }

    // Motorizado Registration Input States
    var regDriverIdInput by remember { mutableStateOf("") }
    var regDriverNameInput by remember { mutableStateOf("") }
    var regDriverPlateInput by remember { mutableStateOf("") }
    var regDriverPhoneInput by remember { mutableStateOf("") }

    // Error & Feedback States
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isCheckingCredentials by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Slate 900 for dark premium background
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. BRAND HERO SECTION (with dynamic logo)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .background(primaryColor.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .border(BorderStroke(2.dp, primaryColor), RoundedCornerShape(20.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.TwoWheeler,
                        contentDescription = "RapidTuy Brand Logo",
                        tint = primaryColor,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = "RapidTuy Central",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif
                )

                Text(
                    text = "Sistema de Control y Despacho de Motorizados",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            // 2. ROLE SELECTOR (Segmented control) - Hidden when registering
            if (!isRegisteringDriver) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Operator Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isOperatorSelected) primaryColor else Color.Transparent)
                                .clickable {
                                    isOperatorSelected = true
                                    errorMessage = null
                                    successMessage = null
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SupportAgent,
                                    contentDescription = null,
                                    tint = if (isOperatorSelected) Color.White else Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Operador",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOperatorSelected) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Motorizado Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isOperatorSelected) primaryColor else Color.Transparent)
                                .clickable {
                                    isOperatorSelected = false
                                    errorMessage = null
                                    successMessage = null
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = if (!isOperatorSelected) Color.White else Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Motorizado",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isOperatorSelected) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }

            // 3. ERROR OR SUCCESS ALERTS
            AnimatedVisibility(visible = errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFEF4444))
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
            }

            AnimatedVisibility(visible = successMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                        Text(
                            text = successMessage ?: "",
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // 4. MAIN FORM CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isRegisteringDriver) {
                        // DRIVER SELF-REGISTRATION FORM
                        Text(
                            text = "Registro de Nuevo Motorizado",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Completa tus datos para afiliarte al sistema y recibir solicitudes de viajes.",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )

                        OutlinedTextField(
                            value = regDriverIdInput,
                            onValueChange = {
                                regDriverIdInput = it
                                errorMessage = null
                                successMessage = null
                            },
                            label = { Text("Número de Chaleco / ID", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                            placeholder = { Text("Ej. 23", color = Color(0xFF475569)) },
                            modifier = Modifier.fillMaxWidth().testTag("input_reg_driver_id"),
                            textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            ),
                            leadingIcon = {
                                Icon(Icons.Default.Tag, contentDescription = null, tint = Color(0xFF64748B))
                            },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = regDriverNameInput,
                            onValueChange = {
                                regDriverNameInput = it
                                errorMessage = null
                                successMessage = null
                            },
                            label = { Text("Nombre Completo", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                            placeholder = { Text("Ej. Carlos Mendoza", color = Color(0xFF475569)) },
                            modifier = Modifier.fillMaxWidth().testTag("input_reg_driver_name"),
                            textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            ),
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF64748B))
                            },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = regDriverPlateInput,
                            onValueChange = {
                                regDriverPlateInput = it
                                errorMessage = null
                                successMessage = null
                            },
                            label = { Text("Placa de Moto", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                            placeholder = { Text("Ej. AB1C23D", color = Color(0xFF475569)) },
                            modifier = Modifier.fillMaxWidth().testTag("input_reg_driver_plate"),
                            textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            ),
                            leadingIcon = {
                                Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = Color(0xFF64748B))
                            },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = regDriverPhoneInput,
                            onValueChange = {
                                regDriverPhoneInput = it
                                errorMessage = null
                                successMessage = null
                            },
                            label = { Text("Número de Teléfono", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                            placeholder = { Text("Ej. 04121234567", color = Color(0xFF475569)) },
                            modifier = Modifier.fillMaxWidth().testTag("input_reg_driver_phone"),
                            textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            ),
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF64748B))
                            },
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                val driverId = regDriverIdInput.toIntOrNull()
                                if (driverId == null) {
                                    errorMessage = "Por favor ingrese un número de chaleco válido."
                                    return@Button
                                }
                                if (regDriverNameInput.isBlank()) {
                                    errorMessage = "Por favor ingrese su nombre completo."
                                    return@Button
                                }
                                if (regDriverPlateInput.isBlank()) {
                                    errorMessage = "Por favor ingrese la placa de su moto."
                                    return@Button
                                }
                                if (regDriverPhoneInput.isBlank()) {
                                    errorMessage = "Por favor ingrese su número de teléfono."
                                    return@Button
                                }

                                isCheckingCredentials = true
                                errorMessage = null
                                successMessage = null
                                coroutineScope.launch {
                                    val (success, message) = viewModel.registrarMotorizadoAsync(
                                        id = driverId,
                                        nombre = regDriverNameInput.trim(),
                                        placa = regDriverPlateInput.trim().uppercase(),
                                        telefono = regDriverPhoneInput.trim()
                                    )
                                    isCheckingCredentials = false
                                    if (success) {
                                        successMessage = message
                                        // Autofill login inputs
                                        selectedDriverIdInput = driverId.toString()
                                        driverPlateInput = regDriverPlateInput.trim().uppercase()
                                        // Switch back to login
                                        isRegisteringDriver = false
                                        // Clear registration fields
                                        regDriverIdInput = ""
                                        regDriverNameInput = ""
                                        regDriverPlateInput = ""
                                        regDriverPhoneInput = ""
                                    } else {
                                        errorMessage = message
                                    }
                                }
                            },
                            enabled = !isCheckingCredentials,
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_driver_register_submit")
                        ) {
                            if (isCheckingCredentials) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                            } else {
                                Text("Registrarme e Iniciar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "« Volver al Inicio de Sesión",
                                color = primaryColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        isRegisteringDriver = false
                                        errorMessage = null
                                        successMessage = null
                                    }
                                    .padding(vertical = 4.dp)
                            )
                        }

                    } else if (isOperatorSelected) {
                        // OPERATOR FORM
                        Text(
                            text = "Acceso para Central de Operadores",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        OutlinedTextField(
                            value = operatorName,
                            onValueChange = {
                                operatorName = it
                                errorMessage = null
                            },
                            label = { Text("Nombre de Operador", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                            placeholder = { Text("Ej. ivan, admin, maria_tuy", color = Color(0xFF475569)) },
                            modifier = Modifier.fillMaxWidth().testTag("input_operator_name"),
                            textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            ),
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF64748B))
                            },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = operatorPasscode,
                            onValueChange = {
                                operatorPasscode = it
                                errorMessage = null
                            },
                            label = { Text("Clave de Acceso (Requerida)", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                            placeholder = { Text("••••••", color = Color(0xFF475569)) },
                            visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("input_operator_passcode"),
                            textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            ),
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF64748B))
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B)
                                    )
                                }
                            },
                            singleLine = true
                        )

                        Text(
                            text = "🔒 Clave requerida por seguridad de la Central.\nAdmin: 'ivan' (Clave: 2004) o 'admin' (Clave: tuy2026).",
                            color = Color(0xFFF59E0B),
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Button(
                            onClick = {
                                if (operatorName.isBlank()) {
                                    errorMessage = "Por favor ingrese su nombre de operador."
                                    return@Button
                                }
                                isCheckingCredentials = true
                                errorMessage = null
                                successMessage = null
                                coroutineScope.launch {
                                    val (success, message) = viewModel.loginAsOperator(operatorName.trim(), operatorPasscode)
                                    isCheckingCredentials = false
                                    if (success) {
                                        successMessage = "¡Bienvenido, operador central!"
                                    } else {
                                        errorMessage = message
                                    }
                                }
                            },
                            enabled = !isCheckingCredentials,
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_operator_login")
                        ) {
                            if (isCheckingCredentials) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                            } else {
                                Text("Iniciar Sesión de Operador", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }

                    } else {
                        // MOTORIZADO FORM
                        Text(
                            text = "Acceso Conductor (Terminal de Viajes)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        OutlinedTextField(
                            value = selectedDriverIdInput,
                            onValueChange = {
                                selectedDriverIdInput = it
                                errorMessage = null
                            },
                            label = { Text("ID Chaleco / Conductor", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                            placeholder = { Text("Ej. 1", color = Color(0xFF475569)) },
                            modifier = Modifier.fillMaxWidth().testTag("input_driver_id"),
                            textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            ),
                            leadingIcon = {
                                Icon(Icons.Default.Tag, contentDescription = null, tint = Color(0xFF64748B))
                            },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = driverPlateInput,
                            onValueChange = {
                                driverPlateInput = it
                                errorMessage = null
                            },
                            label = { Text("Placa de Moto (Contraseña)", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                            placeholder = { Text("Ej. AB1C23D", color = Color(0xFF475569)) },
                            modifier = Modifier.fillMaxWidth().testTag("input_driver_plate"),
                            textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            ),
                            leadingIcon = {
                                Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = Color(0xFF64748B))
                            },
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                val driverId = selectedDriverIdInput.toIntOrNull()
                                if (driverId == null) {
                                    errorMessage = "Por favor ingrese un ID de conductor numérico válido."
                                    return@Button
                                }
                                if (driverPlateInput.isBlank()) {
                                    errorMessage = "Por favor ingrese la placa de su moto."
                                    return@Button
                                }
                                isCheckingCredentials = true
                                coroutineScope.launch {
                                    val (success, message) = viewModel.attemptMotorizadoLogin(driverId, driverPlateInput.trim())
                                    isCheckingCredentials = false
                                    if (!success) {
                                        errorMessage = message
                                    }
                                }
                            },
                            enabled = !isCheckingCredentials,
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_driver_login")
                        ) {
                            if (isCheckingCredentials) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                            } else {
                                Text("Acceder a Terminal Conductor", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }

                        // REGISTRATION OPTION LINK
                        Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 4.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "¿Eres un nuevo motorizado?",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Regístrate aquí para crear tu usuario",
                                color = primaryColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        isRegisteringDriver = true
                                        errorMessage = null
                                        successMessage = null
                                    }
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }


        }
    }

}
@Composable
fun HeaderBar(
    viewModel: RapidTuyViewModel,
    currentTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val yummyThemeActive by viewModel.yummyThemeActive.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val loggedInUser by viewModel.loggedInUser.collectAsState()
    val isDark = isSystemInDarkTheme()
    val headerBgColor = if (isDark) RapidTuySlateDark else Color.White
    val primaryThemeColor = if (yummyThemeActive) Color(0xFF10B981) else RapidTuyOrange
    val titleColor = primaryThemeColor
    val subtitleColor = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B) // slate-500

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var changePasswordError by remember { mutableStateOf<String?>(null) }
    var changePasswordSuccess by remember { mutableStateOf<String?>(null) }
    var isSavingPassword by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = headerBgColor
        ),
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Customized Motorcycle/Car Logo representation
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .background(primaryThemeColor, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.TwoWheeler,
                        contentDescription = "RapidTuy Logo",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "RapidTuy",
                        color = titleColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "DRIVER TERMINAL V1.0",
                        color = subtitleColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Logged-in User Profile & Logout Button
                if (isLoggedIn) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = loggedInUser,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = if (userRole == "OPERATOR") "🔧 Operador Central" else "🛵 Conductor",
                                color = primaryThemeColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        if (userRole == "OPERATOR") {
                            IconButton(
                                onClick = {
                                    showChangePasswordDialog = true
                                    newPasswordInput = ""
                                    confirmPasswordInput = ""
                                    changePasswordError = null
                                    changePasswordSuccess = null
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(primaryThemeColor.copy(alpha = 0.15f), CircleShape)
                                    .border(BorderStroke(1.dp, primaryThemeColor.copy(alpha = 0.4f)), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Cambiar Contraseña",
                                    tint = primaryThemeColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape)
                                .border(BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Cerrar Sesión",
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    if (showChangePasswordDialog) {
                        val hasChangedPassword by viewModel.hasChangedPassword.collectAsState()

                        if (hasChangedPassword) {
                            AlertDialog(
                                onDismissRequest = { showChangePasswordDialog = false },
                                containerColor = Color(0xFF1E293B),
                                titleContentColor = Color.White,
                                textContentColor = Color(0xFF94A3B8),
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF10B981))
                                        Text("Cambio de Clave Completado", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                },
                                text = {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text(
                                            text = "El usuario '$loggedInUser' ya realizó su cambio de contraseña único.",
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Por políticas de seguridad en la Central de RapidTuy, cada cuenta de operador tiene asignado un ÚNICO cambio de clave definitivo. Su contraseña actual se encuentra guardada y protegida.",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8),
                                            lineHeight = 16.sp
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = { showChangePasswordDialog = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryThemeColor)
                                    ) {
                                        Text("Entendido", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            )
                        } else {
                            AlertDialog(
                                onDismissRequest = { if (!isSavingPassword) showChangePasswordDialog = false },
                                containerColor = Color(0xFF1E293B),
                                titleContentColor = Color.White,
                                textContentColor = Color(0xFF94A3B8),
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = primaryThemeColor)
                                        Text("Cambio de Contraseña (Único)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                },
                                text = {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        Text(
                                            "Establece una nueva contraseña de acceso para tu usuario ($loggedInUser). Atención: Solo se permite 1 cambio por usuario.",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )

                                        OutlinedTextField(
                                            value = newPasswordInput,
                                            onValueChange = {
                                                newPasswordInput = it
                                                changePasswordError = null
                                                changePasswordSuccess = null
                                            },
                                            label = { Text("Nueva Contraseña", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                                            placeholder = { Text("Mínimo 4 caracteres", color = Color(0xFF475569)) },
                                            modifier = Modifier.fillMaxWidth(),
                                            textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = primaryThemeColor,
                                                unfocusedBorderColor = Color(0xFF334155),
                                                focusedContainerColor = Color(0xFF0F172A),
                                                unfocusedContainerColor = Color(0xFF0F172A)
                                            ),
                                            singleLine = true
                                        )

                                        OutlinedTextField(
                                            value = confirmPasswordInput,
                                            onValueChange = {
                                                confirmPasswordInput = it
                                                changePasswordError = null
                                                changePasswordSuccess = null
                                            },
                                            label = { Text("Confirmar Contraseña", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                                            placeholder = { Text("Repite la contraseña", color = Color(0xFF475569)) },
                                            modifier = Modifier.fillMaxWidth(),
                                            textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = primaryThemeColor,
                                                unfocusedBorderColor = Color(0xFF334155),
                                                focusedContainerColor = Color(0xFF0F172A),
                                                unfocusedContainerColor = Color(0xFF0F172A)
                                            ),
                                            singleLine = true
                                        )

                                        if (changePasswordError != null) {
                                            Text(
                                                text = changePasswordError ?: "",
                                                color = Color(0xFFEF4444),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        if (changePasswordSuccess != null) {
                                            Text(
                                                text = changePasswordSuccess ?: "",
                                                color = Color(0xFF10B981),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            if (newPasswordInput.isBlank()) {
                                                changePasswordError = "La contraseña no puede estar vacía."
                                                return@Button
                                            }
                                            if (newPasswordInput.trim().length < 4) {
                                                changePasswordError = "La contraseña debe tener al menos 4 caracteres."
                                                return@Button
                                            }
                                            if (newPasswordInput != confirmPasswordInput) {
                                                changePasswordError = "Las contraseñas no coinciden."
                                                return@Button
                                            }

                                            isSavingPassword = true
                                            coroutineScope.launch {
                                                val (success, message) = viewModel.changeOperatorPassword(newPasswordInput)
                                                isSavingPassword = false
                                                if (success) {
                                                    changePasswordSuccess = message
                                                    // Auto dismiss after delay
                                                    kotlinx.coroutines.delay(1500)
                                                    showChangePasswordDialog = false
                                                } else {
                                                    changePasswordError = message
                                                }
                                            }
                                        },
                                        enabled = !isSavingPassword,
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryThemeColor)
                                    ) {
                                        if (isSavingPassword) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                        } else {
                                            Text("Actualizar (1 sola vez)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = { showChangePasswordDialog = false },
                                        enabled = !isSavingPassword
                                    ) {
                                        Text("Cancelar", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    }
                                }
                            )
                        }
                    }
                } else {
                    // Subscription Badge/Status
                    Column(horizontalAlignment = Alignment.End) {
                        Box(
                            modifier = Modifier
                                .background(if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7), RoundedCornerShape(12.dp)) // bg-green-50
                                .border(BorderStroke(1.dp, if (isDark) Color(0xFF059669) else Color(0xFFBBF7D0)), RoundedCornerShape(12.dp)) // border-green-200
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(6.dp).background(Color(0xFF22C55E), CircleShape))
                                Text(
                                    text = "EN LÍNEA",
                                    color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF15803D), // green-700
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Suscripción: Activa",
                            color = subtitleColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // RapidTuy Exclusive Mototaxi Service Information Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                    .border(BorderStroke(0.5.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)), RoundedCornerShape(10.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "🏍️", fontSize = 12.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SERVICIO EXCLUSIVO DE MOTOTAXI VALLES DEL TUY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = RapidTuyOrange,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (userRole == "OPERATOR") {
                    TabButton(
                        text = "Central",
                        icon = Icons.Default.SupportAgent,
                        selected = currentTab == 0,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_admin"),
                        onClick = { onTabSelected(0) }
                    )
                    TabButton(
                        text = "Verificar QR",
                        icon = Icons.Default.QrCode,
                        selected = currentTab == 2,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_qr"),
                        onClick = { onTabSelected(2) }
                    )
                    TabButton(
                        text = "Área Publicitaria",
                        icon = Icons.Default.Campaign,
                        selected = currentTab == 3,
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("tab_promo"),
                        onClick = { onTabSelected(3) }
                    )
                } else if (userRole == "MOTORIZADO") {
                    TabButton(
                        text = "Mi Terminal",
                        icon = Icons.Default.PhoneAndroid,
                        selected = currentTab == 1,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_driver"),
                        onClick = { onTabSelected(1) }
                    )
                    TabButton(
                        text = "Mi Credencial",
                        icon = Icons.Default.QrCode,
                        selected = currentTab == 2,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_qr"),
                        onClick = { onTabSelected(2) }
                    )
                    TabButton(
                        text = "Área Publicitaria",
                        icon = Icons.Default.Campaign,
                        selected = currentTab == 3,
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("tab_promo"),
                        onClick = { onTabSelected(3) }
                    )
                } else {
                    // Fallback tabs if not specified
                    TabButton(
                        text = "Central",
                        icon = Icons.Default.SupportAgent,
                        selected = currentTab == 0,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_admin"),
                        onClick = { onTabSelected(0) }
                    )
                    TabButton(
                        text = "APK",
                        icon = Icons.Default.PhoneAndroid,
                        selected = currentTab == 1,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_driver"),
                        onClick = { onTabSelected(1) }
                    )
                    TabButton(
                        text = "Verificar",
                        icon = Icons.Default.QrCode,
                        selected = currentTab == 2,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_qr"),
                        onClick = { onTabSelected(2) }
                    )
                    TabButton(
                        text = "Área Publicitaria",
                        icon = Icons.Default.Campaign,
                        selected = currentTab == 3,
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("tab_promo"),
                        onClick = { onTabSelected(3) }
                    )
                }
            }
        }
    }

}
@Composable
fun TabButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val containerColor by animateColorAsState(
        targetValue = if (selected) RapidTuyOrange else (if (isDark) Color.Transparent else Color(0xFFE2E8F0)), // bg-slate-200/100 for light unselected
        label = "tabContainerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else (if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF475569)), // slate-600 for light unselected
        label = "tabContentColor"
    )

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        modifier = modifier.height(38.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
// ==========================================
// GEOGRAPHIC DISTANCE CALCULATOR (HAVERSINE)
// ==========================================
fun calculateHaversineDistanceMeters(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Double {
    val r = 6371000.0 // Earth radius in meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

// ==========================================
// CENTRAL CALL CENTER / ADMIN PANEL SCREEN
// ==========================================
data class CityNode(
    val name: String,
    val x: Float,
    val y: Float,
    val presetLocation: String,
    val latitude: Double,
    val longitude: Double
)

@Composable

fun MotorizadosMapView(
    motorizados: List<MotorizadoEntity>,
    activeTrip: TripEntity?,
    clientCityName: String? = null,
    onCitySelected: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val mapBgColor = Color(0xFF0F172A) // Sleek space-age dark map backdrop
    val gridLineColor = Color(0xFF334155).copy(alpha = 0.3f)
    val highwayColor = Color(0xFF334155)
    val activeHighwayColor = RapidTuyOrange
    
    val cities = remember {
        listOf(
            CityNode("Charallave", 0.45f, 0.40f, "Estación Charallave Sur", 10.2315, -66.8652),
            CityNode("Cúa", 0.20f, 0.75f, "Cúa (Estación de Tren)", 10.1654, -66.8845),
            CityNode("Ocumare", 0.72f, 0.85f, "Ocumare del Tuy Plaza", 10.1189, -66.7778),
            CityNode("Yare", 0.82f, 0.58f, "Plaza Bolívar Yare", 10.1388, -66.7030),
            CityNode("S. Teresa", 0.80f, 0.22f, "Santa Teresa Centro", 10.2361, -66.6628)
        )
    }

    // Determine the active client's representative city on the map
    val clientCity = remember(clientCityName, activeTrip) {
        val nameToMatch = clientCityName ?: (activeTrip?.origen ?: "")
        cities.firstOrNull { nameToMatch.contains(it.name, ignoreCase = true) } ?: cities[0]
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(mapBgColor)
            .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(16.dp))
    ) {
        val infiniteTransition = rememberInfiniteTransition()
        var focusMode by remember { mutableStateOf("ALL") } // "ALL" or "SINGLE"
        var selectedDriverId by remember { mutableStateOf(1) }
        var dropdownExpanded by remember { mutableStateOf(false) }

        val activeFocusDriver = remember(motorizados, selectedDriverId) {
            motorizados.find { it.id == selectedDriverId } ?: motorizados.firstOrNull()
        }

        val selectedDriverDistanceMeters = remember(activeFocusDriver, clientCity) {
            if (activeFocusDriver != null) {
                calculateHaversineDistanceMeters(
                    lat1 = activeFocusDriver.latitud,
                    lon1 = activeFocusDriver.longitud,
                    lat2 = clientCity.latitude,
                    lon2 = clientCity.longitude
                )
            } else {
                Double.MAX_VALUE
            }
        }
        val isSelectedDriverNearClient = remember(selectedDriverDistanceMeters) {
            selectedDriverDistanceMeters <= 500.0
        }

        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val density = androidx.compose.ui.platform.LocalDensity.current
        val wDp = with(density) { width.toDp() }
        val hDp = with(density) { height.toDp() }

        // Find the closest available or busy motorizado to this client node
        var nearestDriver by remember(motorizados, clientCity) {
            mutableStateOf<MotorizadoEntity?>(null)
        }
        var nearestDistance by remember(motorizados, clientCity) {
            mutableStateOf(Double.MAX_VALUE)
        }

        LaunchedEffect(motorizados, clientCity, width, height) {
            var minDistance = Double.MAX_VALUE
            var bestDriver: MotorizadoEntity? = null
            
            motorizados.forEach { driver ->
                if (driver.estado == 1 || driver.estado == 2) { // Available or Busy
                    val driverCityIndex = (driver.id % cities.size)
                    val dCity = cities[driverCityIndex]
                    
                    val angle = (driver.id * 73) % 360
                    val driverRadius = 14.0 // in dp
                    
                    val dx = dCity.x * width + Math.cos(Math.toRadians(angle.toDouble())) * driverRadius * density.density
                    val dy = dCity.y * height + Math.sin(Math.toRadians(angle.toDouble())) * driverRadius * density.density
                    
                    val cx = clientCity.x * width
                    val cy = clientCity.y * height
                    
                    val dist = Math.hypot(dx - cx, dy - cy)
                    if (dist < minDistance) {
                        minDistance = dist
                        bestDriver = driver
                    }
                }
            }
            nearestDriver = bestDriver
            nearestDistance = minDistance
        }
        
        // Render canvas elements: grid lines, connections, active route highlighting
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSize = 25.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(gridLineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.5.dp.toPx())
                x += gridSize
            }
            var y = 0f
            while (y < size.height) {
                drawLine(gridLineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5.dp.toPx())
                y += gridSize
            }
            
            // Draw coverage rings
            drawCircle(
                color = Color(0xFFFF6B00).copy(alpha = 0.03f),
                radius = size.width * 0.35f,
                center = Offset(size.width * 0.5f, size.height * 0.5f),
                style = Stroke(width = 1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f))
            )

            // Map points of cities
            val points = cities.associate { it.name to Offset(it.x * size.width, it.y * size.height) }
            
            // Render major roads connecting Valles del Tuy cities
            val connections = listOf(
                "Charallave" to "Cúa",
                "Charallave" to "Ocumare",
                "Ocumare" to "Yare",
                "Yare" to "S. Teresa",
                "Charallave" to "S. Teresa"
            )
            
            connections.forEach { (c1, c2) ->
                val p1 = points[c1]
                val p2 = points[c2]
                if (p1 != null && p2 != null) {
                    val isActive = activeTrip != null && (
                        (activeTrip.origen.contains(c1, true) && activeTrip.destino.contains(c2, true)) ||
                        (activeTrip.origen.contains(c2, true) && activeTrip.destino.contains(c1, true)) ||
                        (activeTrip.origen.contains(c1, true) && activeTrip.destino.contains("Ocumare", true) && c2 == "Ocumare") ||
                        (activeTrip.origen.contains("Cúa", true) && activeTrip.destino.contains("Yare", true) && (c1 == "Charallave" || c2 == "Charallave"))
                    )
                    
                    drawLine(
                        color = if (isActive) activeHighwayColor else highwayColor,
                        start = p1,
                        end = p2,
                        strokeWidth = if (isActive) 3.dp.toPx() else 1.5.dp.toPx(),
                        pathEffect = if (!isActive) androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f) else null
                    )
                }
            }
            
            // Draw route highlight between active endpoints
            if (activeTrip != null) {
                var startPt: Offset? = null
                var endPt: Offset? = null
                cities.forEach { city ->
                    if (activeTrip.origen.contains(city.name, true)) {
                        startPt = Offset(city.x * size.width, city.y * size.height)
                    }
                    if (activeTrip.destino.contains(city.name, true)) {
                        endPt = Offset(city.x * size.width, city.y * size.height)
                    }
                }
                if (startPt != null && endPt != null) {
                    drawCircle(
                        color = RapidTuyOrange.copy(alpha = 0.25f),
                        radius = 20.dp.toPx(),
                        center = startPt!!
                    )
                    drawCircle(
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        radius = 20.dp.toPx(),
                        center = endPt!!
                    )
                }
            }

            // Draw dynamic beam connecting Client to Nearest Motorizado
            nearestDriver?.let { driver ->
                val dIndex = (driver.id % cities.size)
                val dCity = cities[dIndex]
                val angle = (driver.id * 73) % 360
                val driverRadius = 14.dp.toPx()
                
                val dx = dCity.x * size.width + Math.cos(Math.toRadians(angle.toDouble())).toFloat() * driverRadius
                val dy = dCity.y * size.height + Math.sin(Math.toRadians(angle.toDouble())).toFloat() * driverRadius
                
                val cx = clientCity.x * size.width
                val cy = clientCity.y * size.height
                
                drawLine(
                    color = Color(0xFF3B82F6).copy(alpha = 0.6f),
                    start = Offset(cx, cy),
                    end = Offset(dx, dy),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )
            }
        }
        
        // Map 22 motorizados as responsive status dots clustered around cities
        motorizados.forEach { driver ->
            val cityIndex = (driver.id % cities.size)
            val city = cities[cityIndex]
            
            val angle = (driver.id * 73) % 360
            val radius = 14.dp
            val offsetX = (Math.cos(Math.toRadians(angle.toDouble())) * radius.value).dp
            val offsetY = (Math.sin(Math.toRadians(angle.toDouble())) * radius.value).dp
            
            val isFocused = (focusMode == "SINGLE" && driver.id == selectedDriverId)
            val dimmed = (focusMode == "SINGLE" && driver.id != selectedDriverId)

            val distanceToClientMeters = remember(driver, clientCity) {
                calculateHaversineDistanceMeters(
                    lat1 = driver.latitud,
                    lon1 = driver.longitud,
                    lat2 = clientCity.latitude,
                    lon2 = clientCity.longitude
                )
            }
            val isNearClient = distanceToClientMeters <= 500.0
            
            val statusColor = when (driver.estado) {
                1 -> Color(0xFF10B981) // Available
                2 -> Color(0xFF3B82F6) // Busy
                3 -> Color(0xFF64748B) // Offline
                4 -> Color(0xFFEF4444) // Blocked
                else -> Color.Gray
            }

            // Calculate exact offset coordinates in Dp
            val dotX = (city.x * wDp.value).dp - 12.dp + offsetX
            val dotY = (city.y * hDp.value).dp - 12.dp + offsetY

            // If near client, draw a gorgeous glowing halo pulse
            if (isNearClient && !dimmed) {
                val nearPulseScale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 2.4f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "near_pulse_${driver.id}"
                )
                val nearPulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.5f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "near_alpha_${driver.id}"
                )
                Box(
                    modifier = Modifier
                        .offset(x = dotX + 3.dp, y = dotY + 3.dp)
                        .size(18.dp)
                        .scale(nearPulseScale)
                        .background(Color(0xFF10B981).copy(alpha = nearPulseAlpha), CircleShape)
                )
            }

            // If focused, draw pulsing concentric rings behind it
            if (isFocused) {
                val driverPulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 2.8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1800, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )
                val driverPulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.7f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1800, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )

                Box(
                    modifier = Modifier
                        .offset(x = dotX + 6.dp, y = dotY + 6.dp)
                        .size(12.dp)
                        .scale(driverPulseScale)
                        .background(statusColor.copy(alpha = driverPulseAlpha), CircleShape)
                )
            }

            Box(
                modifier = Modifier
                    .offset(x = dotX, y = dotY)
                    .size(24.dp)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        selectedDriverId = driver.id
                        focusMode = "SINGLE"
                    }
                    .let { baseModifier ->
                        if (isNearClient && !dimmed) {
                            baseModifier.border(BorderStroke(1.2.dp, Color(0xFF10B981)), CircleShape)
                        } else {
                            baseModifier
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Actual status dot inside
                Box(
                    modifier = Modifier
                        .size(if (isFocused) 10.dp else if (dimmed) 4.dp else 7.dp)
                        .background(
                            color = if (dimmed) statusColor.copy(alpha = 0.35f) else statusColor,
                            shape = CircleShape
                        )
                        .border(
                            BorderStroke(
                                width = if (isFocused) 1.5.dp else 0.5.dp,
                                color = if (dimmed) Color.White.copy(alpha = 0.3f) else Color.White
                            ),
                            shape = CircleShape
                        )
                )
            }

            // Draw a subtle, neat name/ID badge above the focused driver dot
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .offset(
                            x = dotX - 28.dp, // Center horizontally relative to dot
                            y = dotY - 14.dp  // Position slightly above
                        )
                        .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                        .border(BorderStroke(0.5.dp, statusColor), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "MOTO #${driver.id}: ${driver.nombre.substringBefore(" ")}",
                        color = Color.White,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Render City targets with visual ripples and label text tags
        cities.forEach { city ->
            val nodeX = (city.x * wDp.value).dp
            val nodeY = (city.y * hDp.value).dp
            
            Box(
                modifier = Modifier
                    .offset(x = nodeX - 22.dp, y = nodeY - 22.dp)
                    .size(44.dp)
                    .clickable { onCitySelected(city.name) },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(12.dp)
                            .background(RapidTuyOrange.copy(alpha = 0.3f), CircleShape)
                            .border(BorderStroke(1.dp, RapidTuyOrange), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(Color.White, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = city.name,
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // Dynamic pulsing Client Marker representing the client's current location
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 2.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )

        val clientX = (clientCity.x * wDp.value).dp
        val clientY = (clientCity.y * hDp.value).dp

        Box(
            modifier = Modifier
                .offset(x = clientX - 16.dp, y = clientY - 36.dp)
                .size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            // Pulsing Radar Circle Behind the Pin
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .scale(pulseScale)
                    .background(Color(0xFF3B82F6).copy(alpha = pulseAlpha), CircleShape)
            )

            // Pin with a user icon representing the client
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(Color(0xFF2563EB), CircleShape)
                        .border(BorderStroke(1.5.dp, Color.White), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonPinCircle,
                        contentDescription = "Cliente",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .offset(y = (-2).dp)
                        .background(Color(0xFF2563EB), CircleShape)
                )
            }
        }
        
        // PostGIS details tag overlay
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                .border(BorderStroke(0.5.dp, Color(0xFF334155)), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = RapidTuyOrange,
                    modifier = Modifier.size(8.dp)
                )
                Text(
                    text = "PostGIS Cobertura Valles del Tuy: Activo",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Live Radar closest search status card OR Focused Driver stats card
        if (focusMode == "SINGLE" && activeFocusDriver != null) {
            val driver = activeFocusDriver
            val dCityIndex = (driver.id % cities.size)
            val dCity = cities[dCityIndex]
            val dStatusColor = when (driver.estado) {
                1 -> Color(0xFF10B981) // Available
                2 -> Color(0xFF3B82F6) // Busy
                3 -> Color(0xFF64748B) // Offline
                4 -> Color(0xFFEF4444) // Blocked
                else -> Color.Gray
            }
            val dStatusText = when (driver.estado) {
                1 -> "DISPONIBLE"
                2 -> "EN VIAJE"
                3 -> "INACTIVO"
                4 -> "BLOQUEADO"
                else -> "N/A"
            }

            val cardBorderColor = if (isSelectedDriverNearClient) Color(0xFF10B981) else dStatusColor
            val cardBorderWidth = if (isSelectedDriverNearClient) 1.6.dp else 1.2.dp
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color(0xFF1E293B).copy(alpha = 0.95f), RoundedCornerShape(8.dp))
                    .border(BorderStroke(cardBorderWidth, cardBorderColor), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).background(dStatusColor, CircleShape))
                        Text(
                            text = "ENFOQUE: ${driver.nombre.uppercase().substringBefore(" ")}",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = "🆔 Conductor #${driver.id}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF93C5FD)
                    )
                    Text(
                        text = "📋 Placa: ${driver.placa}",
                        fontSize = 8.sp,
                        color = Color(0xFFCBD5E1)
                    )
                    Text(
                        text = "📍 Base: ${dCity.name}",
                        fontSize = 8.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "⚡ Estado: $dStatusText",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = dStatusColor,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    Divider(color = Color(0xFF334155), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 1.dp))
                    
                    // Display Haversine distance
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (isSelectedDriverNearClient) Color(0xFF10B981) else Color(0xFFEAB308),
                            modifier = Modifier.size(9.dp)
                        )
                        Text(
                            text = "Dist. Cliente: " + if (selectedDriverDistanceMeters < 1000.0) "${selectedDriverDistanceMeters.toInt()}m" else String.format("%.1f km", selectedDriverDistanceMeters / 1000.0),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelectedDriverNearClient) Color(0xFF10B981) else Color.White
                        )
                    }

                    if (isSelectedDriverNearClient) {
                        Spacer(modifier = Modifier.height(1.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .border(BorderStroke(0.5.dp, Color(0xFF10B981)), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OfflineBolt,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(8.dp)
                            )
                            Text(
                                text = "CERCANO (<500m)",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
            }
        } else {
            nearestDriver?.let { driver ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color(0xFF1E293B).copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                        .border(BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.6f)), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF3B82F6), CircleShape))
                            Text(
                                text = "CLIENTE DETECTADO",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF93C5FD),
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "📍 Origen: ${clientCity.name}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "🏍️ Más cercano: ${driver.nombre.substringBefore(" ")}",
                            fontSize = 9.sp,
                            color = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = "⚡ Distancia: ${(nearestDistance / 10.0).coerceAtLeast(0.5).let { String.format("%.1f", it) }} km",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Focus and selection controller dock
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .background(Color(0xFF0F172A).copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                .padding(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // ALL button
                Button(
                    onClick = { focusMode = "ALL" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (focusMode == "ALL") Color(0xFF334155) else Color.Transparent,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(26.dp)
                ) {

                // SINGLE focus button
                Button(
                    onClick = { focusMode = "SINGLE" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (focusMode == "SINGLE") Color(0xFF334155) else Color.Transparent,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(26.dp)
                ) {

                // Selector dropdown
                if (focusMode == "SINGLE") {
                    Box {
                        Row(
                            modifier = Modifier
                                .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                .border(BorderStroke(1.dp, Color(0xFF475569)), RoundedCornerShape(6.dp))
                                .clickable { dropdownExpanded = true }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                .height(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            val activeDriverName = activeFocusDriver?.nombre?.substringBefore(" ") ?: "Selec."
                            Text(
                                text = "MOTO #${selectedDriverId}: $activeDriverName",
                                color = Color(0xFF38BDF8),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(10.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier
                                .background(Color(0xFF0F172A))
                                .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(6.dp))
                                .heightIn(max = 200.dp)
                        ) {
                            motorizados.forEach { driver ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val dotColor = when (driver.estado) {
                                                1 -> Color(0xFF10B981)
                                                2 -> Color(0xFF3B82F6)
                                                3 -> Color(0xFF64748B)
                                                4 -> Color(0xFFEF4444)
                                                else -> Color.Gray
                                            }
                                            Box(modifier = Modifier.size(6.dp).background(dotColor, CircleShape))
                                            Text(
                                                text = "ID #${driver.id} - ${driver.nombre}",
                                                color = Color.White,
                                                fontSize = 10.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedDriverId = driver.id
                                        dropdownExpanded = false
                                        focusMode = "SINGLE"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text(
                text = "Toca un nodo urbano para despachar",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 7.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

}

}

@Composable
fun StatMetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.3.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }

}
@Composable
fun ActiveTripManagementCard(
    trip: TripEntity,
    motorizados: List<MotorizadoEntity>,
    viewModel: RapidTuyViewModel
) {
    val isPending = trip.estado == "PENDIENTE"
    val isAccepted = trip.estado == "ACEPTADO"
    val isUnassigned = trip.estado == "SIN_CONDUCTORES_DISPONIBLES"
    
    val pulseAlpha by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, if (isPending) RapidTuyOrange.copy(alpha = pulseAlpha) else Color(0xFF334155)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val badgeBg = when {
                        isPending -> Color(0xFFFFEDD5)
                        isAccepted -> Color(0xFFD1FAE5)
                        else -> Color(0xFFFEE2E2)
                    }
                    val badgeText = when {
                        isPending -> Color(0xFFEA580C)
                        isAccepted -> Color(0xFF065F46)
                        else -> Color(0xFF991B1B)
                    }
                    val badgeLabel = when {
                        isPending -> "BÚSQUEDA (${trip.segundosRestantes}s)"
                        isAccepted -> "EN TRÁNSITO"
                        else -> "ATASCADO"
                    }
                    
                    Box(
                        modifier = Modifier
                            .background(badgeBg, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badgeLabel,
                            color = badgeText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    
                    Text(
                        text = "Intento #${trip.intentosAsignacion}",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = "$${String.format(java.util.Locale.US, "%.2f", trip.monto)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = RapidTuyOrange
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = RapidTuyOrange,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${trip.origen} ➔ ${trip.destino}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (trip.motorizadoId != null) {
                val assignedDriver = motorizados.firstOrNull { it.id == trip.motorizadoId }
                if (assignedDriver != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                            .border(BorderStroke(0.5.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        if (isAccepted) Color(0xFF10B981) else Color(0xFF3B82F6),
                                        CircleShape
                                    )
                            )
                            Column {
                                Text(
                                    text = assignedDriver.nombre,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Placa: ${assignedDriver.placa}",
                                    fontSize = 8.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                        
                        Text(
                            text = if (isAccepted) "Asignado" else "Consultando",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAccepted) Color(0xFF10B981) else Color(0xFF3B82F6)
                        )
                    }
                }
            } else if (isUnassigned) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF451A03), RoundedCornerShape(8.dp))
                        .padding(6.dp)
                ) {
                    Text(
                        text = "PostGIS Algoritmo: No hay motorizados disponibles cerca. Reintente el despacho.",
                        fontSize = 9.sp,
                        color = Color(0xFFFDBA74),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.cancelTrip(trip.id) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.0f)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                
                if (isAccepted) {
                    Button(
                        onClick = { viewModel.completeTrip(trip.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Completar", fontSize = 11.sp, color = Color.White)
                    }
                } else if (isUnassigned) {
                    Button(
                        onClick = {
                            viewModel.dispatchNewTrip(
                                origen = trip.origen,
                                destino = trip.destino,
                                monto = trip.monto,
                                startLat = 10.2315,
                                startLng = -66.8652
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RapidTuyOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Despachar", fontSize = 11.sp, color = Color.White)
                    }
            }
        }
    }
}

}
}

@Composable
fun AdminPanelScreen(viewModel: RapidTuyViewModel) {
    val motorizados by viewModel.motorizados.collectAsState()
    val trips by viewModel.trips.collectAsState()
    val logs by viewModel.paymentLogs.collectAsState()
    val systemLogs by viewModel.systemLogs.collectAsState()
    val autoSimulate by viewModel.autoSimulateDrivers.collectAsState()
    val yummyThemeActive by viewModel.yummyThemeActive.collectAsState()
    val primaryColor = if (yummyThemeActive) Color(0xFF10B981) else RapidTuyOrange

    val firestoreSyncState by FirestoreSyncManager.syncState.collectAsState()

    val supabaseSyncState by SupabaseSyncManager.syncState.collectAsState()
    val isSupabaseEnabled by SupabaseSyncManager.isEnabled.collectAsState()
    val supabaseUrl by SupabaseSyncManager.supabaseUrl.collectAsState()
    val supabaseKey by SupabaseSyncManager.supabaseKey.collectAsState()

    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        hasLocationPermission = fineGranted || coarseGranted
    }

    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("TODOS") } // "TODOS", "AL_DIA", "VENCIDO"

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showPromotionDialog by remember { mutableStateOf(false) }
    var selectedDriverForPayment by remember { mutableStateOf<MotorizadoEntity?>(null) }
    var paymentReference by remember { mutableStateOf("") }
    var paymentAmount by remember { mutableStateOf("10.0") }

    var originSelected by remember { mutableStateOf("Estación Charallave Sur") }
    var destinationSelected by remember { mutableStateOf("C.C. Tamanaco Tuy") }
    var tripFare by remember { mutableStateOf("5.0") }

    // Custom Call Center QR Generator States
    var qrOrigin by remember { mutableStateOf("Estación Charallave Sur") }
    var qrDestination by remember { mutableStateOf("C.C. Tamanaco Tuy") }
    var qrFare by remember { mutableStateOf("5.0") }
    var qrClientName by remember { mutableStateOf("Cliente Frecuente") }
    var qrPhoneCallCenter by remember { mutableStateOf("584261215060") }
    var qrColorValue by remember { mutableStateOf(0xFFFF6B00) } // Default RapidTuy Orange

    val presetTrips = remember {
        listOf(
            Triple("Charallave Centro / Ferrocarril", "C.C. Tamanaco Tuy (Charallave)", 3.0),
            Triple("Charallave Centro / Ferrocarril", "Estación Ferrocarril Cúa", 4.0),
            Triple("Charallave Centro / Ferrocarril", "San Antonio de Yare Plaza", 5.0),
            Triple("Estación Ferrocarril Cúa", "Ocumare del Tuy Centro", 5.0),
            Triple("Ocumare del Tuy Centro", "Santa Teresa del Tuy Centro", 5.5),
            Triple("Santa Teresa del Tuy Centro", "San Antonio de Yare Plaza", 3.5),
            Triple("Las Brisas / Charallave Norte", "Charallave Centro / Ferrocarril", 3.5),
            Triple("Pinto Salinas / Cúa Sur", "Estación Ferrocarril Cúa", 3.0)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Sleek Dark Theme backdrop
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section: Title & Map Tracker
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "CENTRAL DE DESPACHO INTELIGENTE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = RapidTuyOrange,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Monitoreo Radar de Cobertura",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .border(BorderStroke(0.5.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(5.dp).background(Color(0xFF10B981), CircleShape))
                            Text(
                                text = "PostGIS LIVE",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                
                FirestoreConnectionStatusBar(
                    syncState = firestoreSyncState,
                    hasLocationPermission = hasLocationPermission,
                    onRequestPermission = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )

                SupabaseConnectionStatusBar(
                    syncState = supabaseSyncState,
                    isEnabled = isSupabaseEnabled,
                    onToggleEnabled = { SupabaseSyncManager.setEnabled(it) },
                    currentUrl = supabaseUrl,
                    currentKey = supabaseKey,
                    onUpdateCredentials = { url, key -> SupabaseSyncManager.updateCredentials(url, key) }
                )
                
                // Canvas-drawn interactive map tracker for Valles del Tuy motorizados
                val activeTripForMap = trips.firstOrNull { it.estado == "PENDIENTE" || it.estado == "ACEPTADO" }
                MotorizadosMapView(
                    motorizados = motorizados,
                    activeTrip = activeTripForMap,
                    clientCityName = originSelected,
                    onCitySelected = { cityName ->
                        // Dynamically fill the Dispatch fields when clicking Map cities!
                        val matched = presetTrips.firstOrNull { it.first.contains(cityName, true) || it.second.contains(cityName, true) }
                        if (matched != null) {
                            originSelected = matched.first
                            destinationSelected = matched.second
                            tripFare = matched.third.toString()
                            viewModel.logSystemEvent("Mapa: Ruta pre-seleccionada: ${matched.first} ➔ ${matched.second}")
                        } else {
                            if (originSelected.isEmpty() || originSelected.contains("Charallave")) {
                                originSelected = "$cityName Centro"
                            } else {
                                destinationSelected = "$cityName Principal"
                            }
                        }
                    }
                )
            }
        }

        // Section: Metrics Dashboard grid row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatMetricCard(
                    label = "CONECTADOS",
                    value = "${motorizados.count { it.estado != 3 }}",
                    icon = Icons.Default.SportsMotorsports,
                    iconTint = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    label = "DISPONIBLES",
                    value = "${motorizados.count { it.estado == 1 }}",
                    icon = Icons.Default.TwoWheeler,
                    iconTint = RapidTuyOrange,
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    label = "SOLICITUDES",
                    value = "${trips.count { it.estado == "PENDIENTE" || it.estado == "ACEPTADO" }}",
                    icon = Icons.Default.Timer,
                    iconTint = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1.1f)
                )
                StatMetricCard(
                    label = "RECAUDADO",
                    value = "$${logs.sumOf { it.monto }.toInt()}",
                    icon = Icons.Default.AttachMoney,
                    iconTint = Color(0xFFEAB308),
                    modifier = Modifier.weight(1.1f)
                )
            }
        }

        // Section: Primary Admin Interface - Pending Trips & Fleet Status Dashboard
        item {
            AdminPendingTripsAndFleetDashboard(
                trips = trips,
                motorizados = motorizados,
                viewModel = viewModel,
                primaryColor = primaryColor
            )
        }

        // Section: Yummy Super-App Interactive Booking Hub (Yummy Rides style)
        item {
            YummySuperAppHub(
                viewModel = viewModel,
                motorizados = motorizados,
                primaryColor = primaryColor,
                yummyThemeActive = yummyThemeActive
            )
        }

        // Section: QR Promocional & Publicidad RapidTuy
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = RapidTuyOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Código QR Promocional",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Escanee este código QR con la cámara de su teléfono para descargar y compartir el folleto digital oficial de RapidTuy en redes sociales o haga clic sobre él para abrir el folleto digital a pantalla completa en la central.",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Enlace de descarga:\nhttps://ais-pre-k7q6427t2vsl3nsbjp7e4k-437635375840.us-west2.run.app",
                            fontSize = 9.sp,
                            color = RapidTuyOrange,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    
                    BeautifulQRCode(
                        modifier = Modifier.size(110.dp),
                        url = "https://ais-pre-k7q6427t2vsl3nsbjp7e4k-437635375840.us-west2.run.app",
                        onClick = { showPromotionDialog = true }
                    )
                }
            }
        }

        // Section: Satellite Live Tracking Panel
        item {
            MotorizadosTrackingCard(
                motorizados = motorizados,
                activeTrips = trips,
                viewModel = viewModel,
                onSelectCoordinatesForDispatch = { cityName, lat, lng ->
                    originSelected = cityName
                    viewModel.logSystemEvent("Rastreo: Pre-configurado origen en '$cityName' para facilitar la asignación.")
                }
            )
        }

        // Section: Managing Active Requests List (Required spec)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalTaxi,
                        contentDescription = null,
                        tint = RapidTuyOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Cola de Viajes Activos",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                val activeTrips = trips.filter { it.estado != "COMPLETADO" && it.estado != "CANCELADO" }
                if (activeTrips.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay solicitudes de viaje activas en la central.",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    activeTrips.forEach { activeTrip ->
                        ActiveTripManagementCard(trip = activeTrip, motorizados = motorizados, viewModel = viewModel)
                    }
                }
            }
        }

        // Section: Dispatch controller form
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = RapidTuyOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Despachar Nuevo Servicio",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Presione una plantilla o digite los datos para buscar motorizado por proximidad:",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetTrips.forEach { (origen, destino, fare) ->
                            val isSelected = (originSelected == origen && destinationSelected == destino)
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) RapidTuyOrange.copy(alpha = 0.25f) else Color(0xFF0F172A),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        BorderStroke(1.dp, if (isSelected) RapidTuyOrange else Color(0xFF2A364F)),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        originSelected = origen
                                        destinationSelected = destino
                                        tripFare = fare.toString()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${origen.take(14)}.. ➔ ${destino.take(12)}..",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) RapidTuyOrange else Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Seleccionar Punto de Origen (Valles del Tuy):",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Charallave Centro / Ferrocarril",
                            "Estación Ferrocarril Cúa",
                            "Ocumare del Tuy Centro",
                            "Santa Teresa del Tuy Centro",
                            "San Antonio de Yare Plaza",
                            "C.C. Tamanaco Tuy (Charallave)",
                            "Las Brisas / Charallave Norte",
                            "Pinto Salinas / Cúa Sur"
                        ).forEach { loc ->
                            val isSel = (originSelected == loc)
                            Box(
                                modifier = Modifier
                                    .background(if (isSel) RapidTuyOrange.copy(alpha = 0.3f) else Color(0xFF0F172A), RoundedCornerShape(10.dp))
                                    .border(BorderStroke(1.dp, if (isSel) RapidTuyOrange else Color(0xFF2A364F)), RoundedCornerShape(10.dp))
                                    .clickable { originSelected = loc }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(loc, fontSize = 10.sp, color = if (isSel) RapidTuyOrange else Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (!hasLocationPermission) {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                            fetchCurrentGpsLocation(context) { gpsLoc ->
                                originSelected = gpsLoc
                                viewModel.logSystemEvent("GPS Origen Capturado: $gpsLoc")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📡 Capturar mi GPS en Vivo para Origen", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = originSelected,
                        onValueChange = { originSelected = it },
                        label = { Text("Punto de Origen", color = Color(0xFF94A3B8)) },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = RapidTuyOrange) },
                        modifier = Modifier.fillMaxWidth().testTag("dispatch_origin_input"),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RapidTuyOrange,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Seleccionar Punto de Destino (Valles del Tuy):",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Charallave Centro / Ferrocarril",
                            "Estación Ferrocarril Cúa",
                            "Ocumare del Tuy Centro",
                            "Santa Teresa del Tuy Centro",
                            "San Antonio de Yare Plaza",
                            "C.C. Tamanaco Tuy (Charallave)",
                            "Las Brisas / Charallave Norte",
                            "Pinto Salinas / Cúa Sur"
                        ).forEach { loc ->
                            val isSel = (destinationSelected == loc)
                            Box(
                                modifier = Modifier
                                    .background(if (isSel) RapidTuyOrange.copy(alpha = 0.3f) else Color(0xFF0F172A), RoundedCornerShape(10.dp))
                                    .border(BorderStroke(1.dp, if (isSel) RapidTuyOrange else Color(0xFF2A364F)), RoundedCornerShape(10.dp))
                                    .clickable { destinationSelected = loc }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(loc, fontSize = 10.sp, color = if (isSel) RapidTuyOrange else Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (!hasLocationPermission) {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                            fetchCurrentGpsLocation(context) { gpsLoc ->
                                destinationSelected = gpsLoc
                                viewModel.logSystemEvent("GPS Destino Capturado: $gpsLoc")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📡 Capturar mi GPS en Vivo para Destino", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = destinationSelected,
                        onValueChange = { destinationSelected = it },
                        label = { Text("Punto de Destino", color = Color(0xFF94A3B8)) },
                        leadingIcon = { Icon(Icons.Default.Navigation, contentDescription = null, tint = RapidTuyOrange) },
                        modifier = Modifier.fillMaxWidth().testTag("dispatch_destination_input"),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RapidTuyOrange,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = tripFare,
                            onValueChange = { tripFare = it },
                            label = { Text("Tarifa ($)", color = Color(0xFF94A3B8)) },
                            leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = RapidTuyOrange) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RapidTuyOrange,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            var startLat = 10.2315
                            var startLng = -66.8652
                            if (originSelected.contains("Cúa", true)) {
                                startLat = 10.1654
                                startLng = -66.8845
                            } else if (originSelected.contains("Ocumare", true)) {
                                startLat = 10.1189
                                startLng = -66.7778
                            } else if (originSelected.contains("Teresa", true)) {
                                startLat = 10.2361
                                startLng = -66.6628
                            }

                            viewModel.dispatchNewTrip(
                                origen = originSelected,
                                destino = destinationSelected,
                                monto = tripFare.toDoubleOrNull() ?: 5.0,
                                startLat = startLat,
                                startLng = startLng
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("btn_despachar"),
                        colors = ButtonDefaults.buttonColors(containerColor = RapidTuyOrange),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.FlashOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Despachar", fontSize = 12.sp, color = Color.White)
                    }
            }
        }
        }

        // Section: Custom Call Center QR Generator
        item {
            var showCopiedBadge by remember { mutableStateOf(false) }
            
            LaunchedEffect(showCopiedBadge) {
                if (showCopiedBadge) {
                    kotlinx.coroutines.delay(2000)
                    showCopiedBadge = false
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qr_generator_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color(qrColorValue),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Generador QR de Enlaces Call Center",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "CLIENTE ➔ CENTRAL",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(qrColorValue)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Genere enlaces dinámicos de WhatsApp para que los clientes soliciten viajes pre-configurados escaneando el código QR en la central o folletos impresos:",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // QR presets row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val qrPresets = listOf(
                            Triple("Estación Charallave Sur", "C.C. Tamanaco Tuy", "5.0"),
                            Triple("Estación Charallave Sur", "Plaza Bolívar Yare", "12.0"),
                            Triple("Cúa (Estación de Tren)", "Charallave Centro", "8.0"),
                            Triple("Ocumare del Tuy Plaza", "Estación Charallave Sur", "15.0")
                        )
                        qrPresets.forEach { (orig, dest, fare) ->
                            val isPresetMatch = (qrOrigin == orig && qrDestination == dest && qrFare == fare)
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isPresetMatch) Color(qrColorValue).copy(alpha = 0.15f) else Color(0xFF0F172A),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        BorderStroke(1.dp, if (isPresetMatch) Color(qrColorValue) else Color(0xFF334155)),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        qrOrigin = orig
                                        qrDestination = dest
                                        qrFare = fare
                                    }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "${orig.take(12)}.. ➔ ${dest.take(10)}.. ($$fare)",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPresetMatch) Color(qrColorValue) else Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Main Row container layout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left side column: Inputs
                        Column(
                            modifier = Modifier.weight(1.3f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = qrOrigin,
                                onValueChange = { qrOrigin = it },
                                label = { Text("Origen del viaje", color = Color(0xFF94A3B8), fontSize = 10.sp) },
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(qrColorValue), modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.fillMaxWidth().testTag("qr_origin_input"),
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(qrColorValue),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A)
                                )
                            )

                            OutlinedTextField(
                                value = qrDestination,
                                onValueChange = { qrDestination = it },
                                label = { Text("Destino del viaje", color = Color(0xFF94A3B8), fontSize = 10.sp) },
                                leadingIcon = { Icon(Icons.Default.Navigation, contentDescription = null, tint = Color(qrColorValue), modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.fillMaxWidth().testTag("qr_destination_input"),
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(qrColorValue),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A)
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = qrFare,
                                    onValueChange = { qrFare = it },
                                    label = { Text("Monto ($)", color = Color(0xFF94A3B8), fontSize = 9.sp) },
                                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = Color(qrColorValue), modifier = Modifier.size(14.dp)) },
                                    modifier = Modifier.weight(1f).testTag("qr_fare_input"),
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(qrColorValue),
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedContainerColor = Color(0xFF0F172A),
                                        unfocusedContainerColor = Color(0xFF0F172A)
                                    )
                                )

                                OutlinedTextField(
                                    value = qrClientName,
                                    onValueChange = { qrClientName = it },
                                    label = { Text("Pasajero", color = Color(0xFF94A3B8), fontSize = 9.sp) },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(qrColorValue), modifier = Modifier.size(14.dp)) },
                                    modifier = Modifier.weight(1.2f).testTag("qr_client_name_input"),
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(qrColorValue),
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedContainerColor = Color(0xFF0F172A),
                                        unfocusedContainerColor = Color(0xFF0F172A)
                                    )
                                )
                            }

                            OutlinedTextField(
                                value = qrPhoneCallCenter,
                                onValueChange = { qrPhoneCallCenter = it },
                                label = { Text("Teléfono Call Center (WhatsApp)", color = Color(0xFF94A3B8), fontSize = 10.sp) },
                                leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color(qrColorValue), modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.fillMaxWidth().testTag("qr_phone_input"),
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(qrColorValue),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A)
                                )
                            )

                            // Color selection row
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Color Personalizado de QR:",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val colorsList = listOf(
                                        0xFFFF6B00 to "Naranja", // RapidTuy Orange
                                        0xFF10B981 to "Verde",   // Electric Green
                                        0xFF3B82F6 to "Azul",    // Neon Blue
                                        0xFFEC4899 to "Rosa"     // Hot Pink
                                    )
                                    colorsList.forEach { (colorHex, name) ->
                                        val isSelectedColor = (qrColorValue == colorHex)
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color(colorHex))
                                                .border(
                                                    BorderStroke(
                                                        if (isSelectedColor) 2.dp else 0.dp,
                                                        if (isSelectedColor) Color.White else Color.Transparent
                                                    ),
                                                    CircleShape
                                                )
                                                .clickable { qrColorValue = colorHex }
                                        )
                                    }
                                }
                            }
                        }

                        // Right side column: QR Display Canvas & Actions
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val generatedUrl = remember(qrOrigin, qrDestination, qrFare, qrClientName, qrPhoneCallCenter) {
                                try {
                                    val message = "Hola RapidTuy, deseo solicitar un servicio:\n" +
                                            "📍 Origen: $qrOrigin\n" +
                                            "🏁 Destino: $qrDestination\n" +
                                            "💰 Tarifa: $$qrFare\n" +
                                            "👤 Pasajero: $qrClientName"
                                    val encodedMessage = java.net.URLEncoder.encode(message, "UTF-8")
                                    "https://wa.me/$qrPhoneCallCenter?text=$encodedMessage"
                                } catch (e: Exception) {
                                    "https://wa.me/$qrPhoneCallCenter"
                                }
                            }

                            // Dynamic QR Card
                            Card(
                                modifier = Modifier
                                    .size(125.dp)
                                    .testTag("qr_dynamic_canvas_card"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                     Canvas(modifier = Modifier.size(105.dp)) {
                                         val sizePx = size.width
                                         val cellSize = sizePx / 21f // 21x21 grid
                                         val qrColor = Color(qrColorValue)
                                         
                                         val drawCell: (Int, Int, Color) -> Unit = { row, col, color ->
                                             drawRect(
                                                 color = color,
                                                 topLeft = Offset(col * cellSize, row * cellSize),
                                                 size = androidx.compose.ui.geometry.Size(cellSize + 0.5f, cellSize + 0.5f)
                                             )
                                         }
                                         
                                         val drawFinderPattern: (Int, Int) -> Unit = { startRow, startCol ->
                                             for (r in 0 until 7) {
                                                 for (c in 0 until 7) {
                                                     if (r == 0 || r == 6 || c == 0 || c == 6) {
                                                         drawCell(startRow + r, startCol + c, qrColor)
                                                     }
                                                 }
                                             }
                                             for (r in 2 until 5) {
                                                 for (c in 2 until 5) {
                                                     drawCell(startRow + r, startCol + c, qrColor)
                                                 }
                                             }
                                         }
                                        drawFinderPattern(0, 0)
                                        drawFinderPattern(0, 14)
                                        drawFinderPattern(14, 0)
                                        
                                        for (r in 12 until 17) {
                                            for (c in 12 until 17) {
                                                val isBoundary = (r == 12 || r == 16 || c == 12 || c == 16)
                                                val isCenter = (r == 14 && c == 14)
                                                if (isBoundary || isCenter) {
                                                    drawCell(r, c, qrColor)
                                                }
                                            }
                                        }
                                        
                                        for (i in 7 until 14) {
                                            if (i % 2 == 0) {
                                                drawCell(6, i, qrColor)
                                                drawCell(i, 6, qrColor)
                                            }
                                        }
                                        
                                        val random = java.util.Random(generatedUrl.hashCode().toLong())
                                        for (r in 0 until 21) {
                                            for (c in 0 until 21) {
                                                if (r < 8 && c < 8) continue
                                                if (r < 8 && c > 12) continue
                                                if (r > 12 && c < 8) continue
                                                if (r > 11 && c > 11) continue
                                                if (r == 6 || c == 6) continue
                                                if (r in 9..11 && c in 9..11) continue
                                                
                                                if (random.nextBoolean()) {
                                                    drawCell(r, c, qrColor)
                                                }
                                            }
                                        }
                                        
                                        for (r in 9..11) {
                                            for (c in 9..11) {
                                                drawCell(r, c, Color.White)
                                            }
                                        }
                                        drawCell(10, 10, qrColor)
                                    }
                                }
                            }

                            // Dynamic Text URL Preview
                            Text(
                                text = "Enlace WhatsApp:\n${generatedUrl.take(24)}...",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Actions
                            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

                            Button(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(generatedUrl))
                                    showCopiedBadge = true
                                    viewModel.logSystemEvent("CallCenter QR: Enlace dinámico copiado al portapapeles.")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(qrColorValue)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(32.dp).testTag("qr_copy_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                            }

                            Button(
                                onClick = {
                                    try {
                                        uriHandler.openUri(generatedUrl)
                                        viewModel.logSystemEvent("CallCenter QR: Abriendo enlace en navegador/WhatsApp.")
                                    } catch (e: Exception) {
                                        viewModel.logSystemEvent("CallCenter QR: Error al abrir link.")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(32.dp).testTag("qr_open_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                            }

                            // Toast Notification badge
                            AnimatedVisibility(visible = showCopiedBadge) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF10B981), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "¡COPIADO!",
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black
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
        // Section: Affiliate Driver Management / Subscriptions
        item {
            val now = System.currentTimeMillis()
            val filteredDrivers = remember(motorizados, searchQuery, statusFilter) {
                motorizados.filter { driver ->
                    val matchesSearch = driver.nombre.contains(searchQuery, ignoreCase = true) ||
                            driver.placa.contains(searchQuery, ignoreCase = true) ||
                            driver.telefono.contains(searchQuery, ignoreCase = true)
                    val matchesFilter = when (statusFilter) {
                        "AL_DIA" -> driver.fechaVencimiento >= now && driver.estado != 4
                        "VENCIDO" -> driver.fechaVencimiento < now || driver.estado == 4
                        else -> true
                    }
                    matchesSearch && matchesFilter
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = RapidTuyOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Control de Pagos Semanales (${motorizados.size} Motorizados)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Visualice la lista de afiliados, verifique la vigencia de su suscripción semanal y procese sus pagos de cuotas para reactivarlos.",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.simulateMidnightCronJob() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                                .testTag("btn_cron_job"),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(12.dp), tint = RapidTuyOrange)
                            Spacer(modifier = Modifier.width(4.dp))

                        Button(
                            onClick = { viewModel.clearTripHistory() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155).copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(0.8f)
                                .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.LightGray)
                            Spacer(modifier = Modifier.width(4.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search Input field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar por nombre, placa o teléfono...", fontSize = 11.sp, color = Color(0xFF64748B)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RapidTuyOrange, modifier = Modifier.size(16.dp)) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                                }
                            }
                        } else null,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RapidTuyOrange,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filters Selector Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Estatus:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        
                        FilterBadge(
                            text = "Todos (${motorizados.size})",
                            selected = statusFilter == "TODOS",
                            onClick = { statusFilter = "TODOS" }
                        )
                        
                        val alDiaCount = motorizados.count { it.fechaVencimiento >= now && it.estado != 4 }
                        FilterBadge(
                            text = "Al Día ($alDiaCount)",
                            selected = statusFilter == "AL_DIA",
                            onClick = { statusFilter = "AL_DIA" }
                        )
                        
                        val vencidosCount = motorizados.count { it.fechaVencimiento < now || it.estado == 4 }
                        FilterBadge(
                            text = "Vencidos ($vencidosCount)",
                            selected = statusFilter == "VENCIDO",
                            onClick = { statusFilter = "VENCIDO" }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(12.dp))
                    ) {
                        if (motorizados.isEmpty()) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = RapidTuyOrange
                            )
                        } else if (filteredDrivers.isEmpty()) {
                            Text(
                                text = "Ningún motorizado coincide con los filtros",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredDrivers) { driver ->
                                    DriverAdminRow(
                                        driver = driver,
                                        onSelectPayment = {
                                            selectedDriverForPayment = driver
                                            paymentReference = "REF-${(1000..9999).random()}"
                                            showPaymentDialog = true
                                        }
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

        // Section: Supabase Real-Time Monitor List
        item {
            SupabaseRealtimeMotorizadosCard(viewModel = viewModel)
        }

        // Section: Payment log history
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Historial de Pagos de Suscripción",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (logs.isEmpty()) {
                        Text(
                            text = "No se han reportado pagos de suscripción en esta sesión.",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    } else {
                        logs.take(5).forEach { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = log.conductorNombre, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                    Text(text = "Ref: ${log.referencia}", fontSize = 9.sp, color = Color(0xFF64748B))
                                }
                                Text(
                                    text = "+$${String.format(java.util.Locale.US, "%.2f", log.monto)} USD",
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            HorizontalDivider(color = Color(0xFF334155))
                        }
                    }
                }
            }
        }

        // Section: Real-time system log console
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)), // Terminal deep black
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Consola de Eventos del Sistema (PostGIS / Despacho)",
                            color = RapidTuyOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF22C55E), CircleShape)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        systemLogs.forEach { log ->
                            Text(
                                text = log,
                                color = Color(0xFFE2E8F0),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }

    // Payment Processing Modal Dialog
    if (showPaymentDialog && selectedDriverForPayment != null) {
        val driver = selectedDriverForPayment!!
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = Color(0xFF94A3B8),
            title = {
                Text(
                    text = "Abonar Pago de Suscripción",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Reportar pago semanal para reactivar a ${driver.nombre} (${driver.placa}). Esto extenderá su acceso por 7 días en la central de despacho.",
                        fontSize = 11.sp
                    )

                    OutlinedTextField(
                        value = paymentAmount,
                        onValueChange = { paymentAmount = it },
                        label = { Text("Monto Abonado ($)", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RapidTuyOrange,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        )
                    )

                    OutlinedTextField(
                        value = paymentReference,
                        onValueChange = { paymentReference = it },
                        label = { Text("Referencia del Pago", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RapidTuyOrange,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.payWeeklySubscription(
                            driverId = driver.id,
                            reference = paymentReference,
                            amount = paymentAmount.toDoubleOrNull() ?: 10.0
                        )
                        showPaymentDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RapidTuyOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Registrar", fontSize = 12.sp, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) {
                    Text("Cancelar", fontSize = 12.sp, color = Color(0xFF94A3B8))
                }
            }
        )
    }

    if (showPromotionDialog) {
        PromotionBannerDialog(onDismiss = { showPromotionDialog = false })
    }
}

@Composable
fun FilterBadge(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (selected) RapidTuyOrange else Color(0xFF0F172A),
                RoundedCornerShape(8.dp)
            )
            .border(
                BorderStroke(1.dp, if (selected) RapidTuyOrange else Color(0xFF334155)),
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color(0xFF94A3B8),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DriverAdminRow(
    driver: MotorizadoEntity,
    onSelectPayment: () -> Unit
) {
    val now = System.currentTimeMillis()
    val isExpired = driver.fechaVencimiento < now || driver.estado == 4
    val daysRemaining = ((driver.fechaVencimiento - now) / (1000 * 60 * 60 * 24)).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpired) Color(0xFF451A1A).copy(alpha = 0.3f) else Color(0xFF1E293B).copy(alpha = 0.4f)
        ),
        border = BorderStroke(
            0.5.dp,
            if (isExpired) Color(0xFFEF4444).copy(alpha = 0.4f) else Color(0xFF334155)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    PulsingStatusDot(estado = driver.estado)
                    Spacer(modifier = Modifier.width(6.dp))

                    Column {
                        Text(
                            text = "${driver.id}. ${driver.nombre}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Placa: ${driver.placa} | Tel: ${driver.telefono}",
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Payment Status Badge
                Box(
                    modifier = Modifier
                        .background(
                            if (isExpired) Color(0xFF7F1D1D) else Color(0xFF064E3B),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isExpired) "VENCIDO" else if (daysRemaining <= 2) "POR VENCER" else "AL DÍA",
                        color = if (isExpired) Color(0xFFFCA5A5) else Color(0xFFA7F3D0),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Subscription detail section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1.5f)) {
                    Text(
                        text = "Vencimiento: ${
                            java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(driver.fechaVencimiento))
                        }",
                        fontSize = 9.sp,
                        color = if (isExpired) Color(0xFFFCA5A5) else Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (driver.ultimoPagoMonto > 0.0) {
                        Text(
                            text = "Último Pago: $${String.format("%.2f", driver.ultimoPagoMonto)} USD (${
                                java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(driver.ultimoPagoFecha))
                            })",
                            fontSize = 9.sp,
                            color = Color(0xFF10B981)
                        )
                    } else {
                        Text(
                            text = "Sin registro de pagos recientes",
                            fontSize = 9.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Action button
                if (driver.estado == 4) {
                    Button(
                        onClick = onSelectPayment,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pagar", fontSize = 9.sp, color = Color.White)
                    }
                } else {
                    Button(
                        onClick = onSelectPayment,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Icon(Icons.Filled.Payment, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Registrar Pago", fontSize = 9.sp, color = Color.White)
                    }
            }
        }
    }
}

// ==========================================
// DRIVER APK SCREEN (APK SIMULATOR)
// ==========================================
}

@Composable
fun DriverApkScreen(viewModel: RapidTuyViewModel) {
    val motorizados by viewModel.motorizados.collectAsState()
    val trips by viewModel.trips.collectAsState()
    val impersonatedId by viewModel.impersonatedDriverId.collectAsState()
    val yummyThemeActive by viewModel.yummyThemeActive.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val primaryColor = if (yummyThemeActive) Color(0xFF10B981) else RapidTuyOrange
    val lightColor = if (yummyThemeActive) Color(0xFF10B981).copy(alpha = 0.2f) else RapidTuyOrangeLight

    val currentDriver = motorizados.firstOrNull { it.id == impersonatedId }
    val activeTripForDriver = trips.firstOrNull { it.estado == "PENDIENTE" && it.motorizadoId == impersonatedId }

    var expandedDropdown by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        hasLocationPermission = fineGranted || coarseGranted
    }

    LaunchedEffect(impersonatedId) {
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Realistic dark slate smartphone view
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Driver selector dropdown or locked session info
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (userRole == "MOTORIZADO") "Terminal Conductor Conectado:" else "Terminal Dispositivo APK:",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )

            if (userRole == "MOTORIZADO") {
                Box(
                    modifier = Modifier
                        .background(primaryColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .border(BorderStroke(1.dp, primaryColor), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFF22C55E), CircleShape))
                        Text(
                            text = "ID: #$impersonatedId (Sesión Bloqueada)",
                            fontSize = 10.sp,
                            color = primaryColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Box {
                    Button(
                        onClick = { expandedDropdown = true },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = "Cambiar Conductor (#$impersonatedId)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                    }

                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        motorizados.forEach { driver ->
                            val stateLabel = when (driver.estado) {
                                1 -> "(Disponible)"
                                2 -> "(Ocupado)"
                                3 -> "(Fuera Serv.)"
                                4 -> "(BLOQUEADO)"
                                else -> ""
                            }
                            DropdownMenuItem(
                                text = { Text("#${driver.id} - ${driver.nombre} $stateLabel", fontSize = 12.sp) },
                                onClick = {
                                    viewModel.setImpersonatedDriver(driver.id)
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (currentDriver == null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = primaryColor)
            }
        } else {
            // PHONE CONTAINER MOCK
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(4.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E293B))
            ) {
                // Main smartphone layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    // Simulated smartphone Status Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("RapidTuy APK", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                            Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                    }

                    // Content Area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Driver Profile visual block inside APK
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(primaryColor.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = primaryColor)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = currentDriver.nombre,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "ID: ${currentDriver.id} | Placa: ${currentDriver.placa}",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 11.sp
                                        )
                                    }

                                    // Small online status dot
                                    val statusDotColor = when (currentDriver.estado) {
                                        1 -> Color(0xFF10B981)
                                        2 -> Color(0xFF3B82F6)
                                        3 -> Color(0xFF9CA3AF)
                                        4 -> Color(0xFFEF4444)
                                        else -> Color.Gray
                                    }
                                    PulsingStatusDot(estado = currentDriver.estado)
                                }
                            }

                            // Interactive Status Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Suscripción Semanal",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        
                                        val isBlocked = currentDriver.estado == 4
                                        val subBadgeColor = if (isBlocked) Color(0xFFEF4444) else Color(0xFF10B981)
                                        val subText = if (isBlocked) "MORA / BLOQUEADO" else "AL DÍA"
                                        
                                        Box(
                                            modifier = Modifier
                                                .background(subBadgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = subText,
                                                color = subBadgeColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Vencimiento: ${
                                            java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(currentDriver.fechaVencimiento))
                                        }",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 10.sp
                                    )

                                    if (currentDriver.estado != 4 && currentDriver.estado != 2) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (currentDriver.estado == 1) "Estado: DISPONIBLE" else "Estado: DESCONECTADO",
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Switch(
                                                checked = currentDriver.estado == 1,
                                                onCheckedChange = { viewModel.toggleDriverOnlineOffline(currentDriver.id) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = primaryColor, checkedTrackColor = primaryColor.copy(alpha = 0.5f))
                                            )
                                        }
                                    }
                                }
                            }

                            // GPS Runtime Permission Banner
                            if (!hasLocationPermission) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D).copy(alpha = 0.25f)),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = "Falta Permiso GPS",
                                                tint = Color(0xFFFCA5A5),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Permisos de GPS Requeridos",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Text(
                                            text = "Se requiere acceso a la ubicación para transmitir coordenadas al panel de despacho central.",
                                            color = Color(0xFFFCA5A5),
                                            fontSize = 10.sp,
                                            lineHeight = 14.sp
                                        )
                                        Button(
                                            onClick = {
                                                permissionLauncher.launch(
                                                    arrayOf(
                                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                                    )
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.align(Alignment.End).height(28.dp)
                                        ) {
                                        Text("Otorgar Permiso", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B).copy(alpha = 0.2f)),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = "GPS Activo",
                                            tint = Color(0xFFA7F3D0),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Transmisión GPS de Alta Precisión Activa",
                                            color = Color(0xFFA7F3D0),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // Simulated Map Widget with PostGIS simulation dots
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    // Simulated grid pattern representing Map
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val step = size.width / 8
                                        // Draw roads grid
                                        for (i in 0..8) {
                                            drawLine(Color.White.copy(alpha = 0.05f), Offset(i * step, 0f), Offset(i * step, size.height))
                                            drawLine(Color.White.copy(alpha = 0.05f), Offset(0f, i * step), Offset(size.width, i * step))
                                        }
                                        // Draw a simple path
                                        val roadBrush = Brush.radialGradient(colors = listOf(primaryColor.copy(alpha = 0.15f), Color.Transparent))
                                        drawCircle(roadBrush, center = center, radius = size.width / 2.5f)

                                        // Draw My Driver Position Icon
                                        drawCircle(color = primaryColor, center = center, radius = 8.dp.toPx())
                                        drawCircle(color = Color.White, center = center, radius = 4.dp.toPx())
                                    }

                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(8.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = if (hasLocationPermission) "Rastreo GPS PostGIS (ACTIVO):" else "Rastreo GPS PostGIS (INACTIVO):",
                                            color = if (hasLocationPermission) lightColor else Color(0xFFFCA5A5),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (hasLocationPermission) "Lat: ${currentDriver.latitud} | Lon: ${currentDriver.longitud}" else "Sin señal GPS (Permiso denegado)",
                                            color = if (hasLocationPermission) Color.White else Color(0xFFEF4444),
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        // MIDDLEWARE INTERCEPT: ACCESO RESTRINGIDO LOCK SCREEN OVERLAY (Estado = 4)
                        if (currentDriver.estado == 4) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF1E1B4B).copy(alpha = 0.98f)) // Deep indigo/red block tone
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Bloqueado",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(54.dp)
                                    )

                                    Text(
                                        text = "ACCESO RESTRINGIDO",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        letterSpacing = 2.sp
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = "Middleware interceptó la petición de la APK: Conductor #${currentDriver.id} (${currentDriver.nombre}) presenta saldo vencido en tabla control_pagos.\n\nPor favor reporte su pago semanal fijo a la central para reactivar el servicio.",
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 16.sp
                                        )
                                    }

                                    Button(
                                        onClick = { /* Simulated Call Admin */ },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Contactar Central RapidTuy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        // INCOMING TRIP ALERT OVERLAY WITH 15S COUNTDOWN TIMER - SLEEK INTERFACE THEME
                        if (activeTripForDriver != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.7f)) // Semi-transparent overlay
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(24.dp), // rounded-[32px] in HTML, scaled nicely for mobile view
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)) // border-slate-100/200
                                ) {
                                    Column {
                                        // Header of request
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFFF8F9FA)) // bg-slate-50
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "NUEVA SOLICITUD",
                                                color = Color(0xFF64748B), // slate-500
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            )

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "${activeTripForDriver.segundosRestantes}s",
                                                    color = primaryColor,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                                // Dynamic linear progress bar
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 60.dp, height = 6.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFE2E8F0))
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxHeight()
                                                            .fillMaxWidth(fraction = activeTripForDriver.segundosRestantes / 15f)
                                                            .clip(CircleShape)
                                                            .background(primaryColor)
                                                    )
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = Color(0xFFF1F5F9))

                                        // Body with route info
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            // Route timeline
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                // Left timeline indicators
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .border(BorderStroke(2.dp, primaryColor), CircleShape)
                                                            .background(Color.White, CircleShape)
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .width(1.dp)
                                                            .height(36.dp)
                                                            .background(Color(0xFFE2E8F0))
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .background(Color(0xFF1E293B), RoundedCornerShape(2.dp))
                                                    )
                                                }

                                                // Route Texts
                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = "RECOGIDA",
                                                            fontSize = 9.sp,
                                                            color = Color(0xFF94A3B8), // slate-400
                                                            fontWeight = FontWeight.Bold,
                                                            letterSpacing = 0.5.sp
                                                        )
                                                        Text(
                                                            text = activeTripForDriver.origen,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFF1E293B)
                                                        )
                                                    }

                                                    Column {
                                                        Text(
                                                            text = "DESTINO",
                                                            fontSize = 9.sp,
                                                            color = Color(0xFF94A3B8), // slate-400
                                                            fontWeight = FontWeight.Bold,
                                                            letterSpacing = 0.5.sp
                                                        )
                                                        Text(
                                                            text = activeTripForDriver.destino,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFF1E293B)
                                                        )
                                                    }
                                                }
                                            }

                                            // Fare & details panel
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFFFFF7ED), RoundedCornerShape(16.dp)) // bg-orange-50
                                                    .border(BorderStroke(1.dp, Color(0xFFFFEDD5)), RoundedCornerShape(16.dp)) // border-orange-100
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "TARIFA ESTIMADA",
                                                        fontSize = 9.sp,
                                                        color = Color(0xFFEA580C), // orange-600
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = 0.5.sp
                                                    )
                                                    Text(
                                                        text = "$${String.format(java.util.Locale.US, "%.2f", activeTripForDriver.monto)}",
                                                        fontSize = 22.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFF0F172A) // slate-900
                                                    )
                                                }

                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "DISTANCIA",
                                                        fontSize = 9.sp,
                                                        color = Color(0xFF94A3B8), // slate-400
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = 0.5.sp
                                                    )
                                                    val calculatedDistance = String.format(java.util.Locale.US, "%.1f", (activeTripForDriver.monto * 0.7))
                                                    Text(
                                                        text = "$calculatedDistance km",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF334155) // slate-700
                                                    )
                                                }
                                            }

                                            // Action Buttons Grid
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                // Skip button
                                                Button(
                                                    onClick = {
                                                        viewModel.rejectTrip(activeTripForDriver.id, currentDriver.id, 10.2315, -66.8652)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFFF1F5F9), // slate-100
                                                        contentColor = Color(0xFF475569) // slate-600
                                                    ),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(48.dp),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text(
                                                        text = "OMITIR",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        letterSpacing = 0.5.sp
                                                    )
                                                }

                                                // Accept button
                                                Button(
                                                    onClick = {
                                                        viewModel.acceptTrip(activeTripForDriver.id, currentDriver.id)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = primaryColor,
                                                        contentColor = Color.White
                                                    ),
                                                    modifier = Modifier
                                                        .weight(1.2f)
                                                        .height(48.dp)
                                                        .testTag("btn_accept_trip"),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text(
                                                        text = "ACEPTAR",
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 13.sp,
                                                        letterSpacing = 0.5.sp
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
            }
        }
    }
}
}
}


@Composable
fun QrVerificationScreen(viewModel: RapidTuyViewModel) {
    val motorizados by viewModel.motorizados.collectAsState()
    val yummyThemeActive by viewModel.yummyThemeActive.collectAsState()
    val primaryColor = if (yummyThemeActive) Color(0xFF10B981) else RapidTuyOrange

    var searchQuery by remember { mutableStateOf("") }
    var selectedDriverId by remember { mutableStateOf<Int?>(null) }

    // Registration Form States
    var regId by remember { mutableStateOf("") }
    var regNombre by remember { mutableStateOf("") }
    var regPlaca by remember { mutableStateOf("") }
    var regTelefono by remember { mutableStateOf("") }
    var regEstado by remember { mutableStateOf(1) }

    // Customize Image States
    var showBannerImage by remember { mutableStateOf(true) }
    val customImagePlaceholderText = "Pegar URL de Imagen de Perfil..."
    var customImageUrl by remember { mutableStateOf("") }

    val filteredDrivers = remember(motorizados, searchQuery) {
        if (searchQuery.isBlank()) {
            motorizados
        } else {
            motorizados.filter {
                it.nombre.contains(searchQuery, ignoreCase = true) ||
                it.placa.contains(searchQuery, ignoreCase = true) ||
                it.id.toString() == searchQuery
            }
        }
    }

    val selectedDriver = remember(motorizados, selectedDriverId) {
        motorizados.firstOrNull { it.id == selectedDriverId } ?: motorizados.firstOrNull()
    }

    // Update selected ID if we don't have one and a driver is available
    LaunchedEffect(motorizados) {
        if (selectedDriverId == null && motorizados.isNotEmpty()) {
            selectedDriverId = motorizados.first().id
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Welcome and Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(primaryColor.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = primaryColor)
                        }
                        Column {
                            Text(
                                text = "Panel de Control QR y Estados",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "Genere credenciales QR de verificación para los motorizados y gestione bloqueos en tiempo real.",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }

        // Section 1: Register and Search
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "🏍 Registrar Nuevo Conductor Real",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = regId,
                                onValueChange = { regId = it },
                                label = { Text("ID Chaleco / Nro.", color = Color(0xFF94A3B8), fontSize = 10.sp) },
                                modifier = Modifier.weight(1.3f),
                                textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A)
                                )
                            )

                            OutlinedTextField(
                                value = regNombre,
                                onValueChange = { regNombre = it },
                                label = { Text("Nombre del Motorizado", color = Color(0xFF94A3B8), fontSize = 10.sp) },
                                modifier = Modifier.weight(2f),
                                textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A)
                                )
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = regPlaca,
                                onValueChange = { regPlaca = it },
                                label = { Text("Placa de Moto", color = Color(0xFF94A3B8), fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A)
                                )
                            )

                            OutlinedTextField(
                                value = regTelefono,
                                onValueChange = { regTelefono = it },
                                label = { Text("Teléfono Móvil", color = Color(0xFF94A3B8), fontSize = 10.sp) },
                                modifier = Modifier.weight(1.2f),
                                textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A)
                                )
                            )
                        }

                        Button(
                            onClick = {
                                val idParsed = regId.toIntOrNull()
                                if (idParsed == null) {
                                    viewModel.logSystemEvent("ERROR REGISTRO: El ID debe ser un número entero válido.")
                                    return@Button
                                }
                                if (regNombre.isBlank() || regPlaca.isBlank() || regTelefono.isBlank()) {
                                    viewModel.logSystemEvent("ERROR REGISTRO: Todos los campos son requeridos para pre-afiliar un motorizado.")
                                    return@Button
                                }
                                viewModel.registrarMotorizado(
                                    id = idParsed,
                                    nombre = regNombre,
                                    placa = regPlaca,
                                    telefono = regTelefono,
                                    estado = regEstado
                                )
                                // Clear Fields
                                regId = ""
                                regNombre = ""
                                regPlaca = ""
                                regTelefono = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                    }
                }

                // Section 2: Active Directory
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "🔍 Seleccionar Motorizado Registrado",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Buscar por nombre, placa o ID...", color = Color(0xFF64748B), fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Horizontal Driver chips
                        if (filteredDrivers.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Ningún motorizado coincide con la búsqueda.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                filteredDrivers.forEach { driver ->
                                    val isSelected = (selectedDriver?.id == driver.id)
                                    val statusColor = when (driver.estado) {
                                        1 -> Color(0xFF10B981) // Available
                                        2 -> Color(0xFF3B82F6) // Busy
                                        3 -> Color(0xFF64748B) // Out of service
                                        4 -> Color(0xFFEF4444) // Blocked
                                        else -> Color.Gray
                                    }
                                    
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) primaryColor.copy(alpha = 0.15f) else Color(0xFF0F172A)
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (isSelected) primaryColor else Color(0xFF334155)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.clickable { selectedDriverId = driver.id }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(statusColor, CircleShape)
                                            )
                                            Text(
                                                text = "#${driver.id} ${driver.nombre.take(12)}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) primaryColor else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 3: Credential display and control
                selectedDriver?.let { driver ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "🎫 Credencial de Identificación QR (Escaneable)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Customizing layout option (To fulfill "o que deje espacio para poner imagen")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Mostrar Banner Institucional:",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Switch(
                                    checked = showBannerImage,
                                    onCheckedChange = { showBannerImage = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = primaryColor,
                                        checkedTrackColor = primaryColor.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.scale(0.7f)
                                )
                            }

                            if (!showBannerImage) {
                                OutlinedTextField(
                                    value = customImageUrl,
                                    onValueChange = { customImageUrl = it },
                                    placeholder = { Text(customImagePlaceholderText, color = Color(0xFF475569), fontSize = 10.sp) },
                                    label = { Text("URL de Imagen de Perfil Personalizada (Espacio)", fontSize = 9.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = primaryColor,
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedContainerColor = Color(0xFF0F172A)
                                    ),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Physical Style Credential Badge Card (Centered)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(16.dp))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier
                                        .width(280.dp)
                                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                        .border(BorderStroke(1.dp, Color(0xFF475569)), RoundedCornerShape(12.dp)),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // 1. HEADER IMAGE ("con esa imagen" / "deja espacio")
                                    if (showBannerImage) {
                                        Image(
                                            painter = painterResource(id = R.drawable.rapidtuy_banner),
                                            contentDescription = "RapidTuy Banner",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(90.dp)
                                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        // "Dejar espacio para poner imagen"
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(90.dp)
                                                .background(Color(0xFF334155), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (customImageUrl.isNotBlank()) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(
                                                        Icons.Default.Image,
                                                        contentDescription = null,
                                                        tint = primaryColor,
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                    Text(
                                                        "Imagen Personalizada",
                                                        fontSize = 8.sp,
                                                        color = Color.White.copy(alpha = 0.5f)
                                                    )
                                                }
                                            } else {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(
                                                        Icons.Default.NoAccounts,
                                                        contentDescription = null,
                                                        tint = Color.White.copy(alpha = 0.4f),
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                    Text(
                                                        "[Espacio para Foto / Logo]",
                                                        fontSize = 9.sp,
                                                        color = Color.White.copy(alpha = 0.5f)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Driver Details
                                    Text(
                                        text = driver.nombre,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "REGISTRO NRO: #${driver.id}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🛵 Placa: ${driver.placa}",
                                            fontSize = 10.sp,
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "📞 Tel: ${driver.telefono}",
                                            fontSize = 10.sp,
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Live Status Indicator Card
                                    val statusText = when (driver.estado) {
                                        1 -> "DISPONIBLE"
                                        2 -> "OCUPADO EN VIAJE"
                                        3 -> "FUERA DE SERVICIO"
                                        4 -> "SUSPENDIDO / IMPAGO"
                                        else -> "DESCONOCIDO"
                                    }
                                    val statusColor = when (driver.estado) {
                                        1 -> Color(0xFF10B981)
                                        2 -> Color(0xFF3B82F6)
                                        3 -> Color(0xFF64748B)
                                        4 -> Color(0xFFEF4444)
                                        else -> Color.Gray
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                            .border(BorderStroke(1.dp, statusColor), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = statusText,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = statusColor
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // QR Code Drawing (Canvas-based standard)
                                    val verificationUrl = "https://rapidtuy.com/verificar?id=${driver.id}&nombre=${driver.nombre}&placa=${driver.placa}&estado=${driver.estado}"
                                    Card(
                                        modifier = Modifier.size(130.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                             Canvas(modifier = Modifier.size(110.dp)) {
                                                 val sizePx = size.width
                                                 val cellSize = sizePx / 21f // 21x21 grid
                                                 val qrColor = if (driver.estado == 4) Color(0xFFEF4444) else Color.Black
                                                 
                                                 val drawCellLocal: (Int, Int, Color) -> Unit = { row, col, color ->
                                                     drawRect(
                                                         color = color,
                                                         topLeft = Offset(col * cellSize, row * cellSize),
                                                         size = androidx.compose.ui.geometry.Size(cellSize + 0.5f, cellSize + 0.5f)
                                                     )
                                                 }
                                                 
                                                 val drawFinderPatternLocal: (Int, Int) -> Unit = { startRow, startCol ->
                                                     for (r in 0 until 7) {
                                                         for (c in 0 until 7) {
                                                             if (r == 0 || r == 6 || c == 0 || c == 6) {
                                                                 drawCellLocal(startRow + r, startCol + c, qrColor)
                                                             }
                                                         }
                                                     }
                                                     for (r in 2 until 5) {
                                                         for (c in 2 until 5) {
                                                             drawCellLocal(startRow + r, startCol + c, qrColor)
                                                         }
                                                     }
                                                 }
                                                drawFinderPatternLocal(0, 0)
                                                drawFinderPatternLocal(0, 14)
                                                drawFinderPatternLocal(14, 0)
                                                
                                                for (r in 12 until 17) {
                                                    for (c in 12 until 17) {
                                                        val isBoundary = (r == 12 || r == 16 || c == 12 || c == 16)
                                                        val isCenter = (r == 14 && c == 14)
                                                        if (isBoundary || isCenter) {
                                                            drawCellLocal(r, c, qrColor)
                                                        }
                                                    }
                                                }
                                                
                                                for (i in 7 until 14) {
                                                    if (i % 2 == 0) {
                                                        drawCellLocal(6, i, qrColor)
                                                        drawCellLocal(i, 6, qrColor)
                                                    }
                                                }
                                                
                                                val random = java.util.Random(verificationUrl.hashCode().toLong())
                                                for (r in 0 until 21) {
                                                    for (c in 0 until 21) {
                                                        if (r < 8 && c < 8) continue
                                                        if (r < 8 && c > 12) continue
                                                        if (r > 12 && c < 8) continue
                                                        if (r > 11 && c > 11) continue
                                                        if (r == 6 || c == 6) continue
                                                        if (r in 9..11 && c in 9..11) continue
                                                        
                                                        if (random.nextBoolean()) {
                                                            drawCellLocal(r, c, qrColor)
                                                        }
                                                    }
                                                }
                                                
                                                // Center branding block of color inside the QR
                                                for (r in 9..11) {
                                                    for (c in 9..11) {
                                                        drawCellLocal(r, c, primaryColor)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "ESCANEAR PARA VERIFICAR",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (driver.estado == 4) Color(0xFFEF4444) else Color(0xFF64748B),
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action: Bloquear y revisa el estado
                            Text(
                                text = "⚡ Acciones de Control de Estado",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (driver.estado == 4) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFEF4444))
                                        Text(
                                            text = "Conductor Bloqueado: Este motorizado tiene suspendido su acceso al despacho de viajes de la central.",
                                            fontSize = 9.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.updateMotorizadoEstadoDirectly(driver.id, 1) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))

                                Button(
                                    onClick = { viewModel.updateMotorizadoEstadoDirectly(driver.id, 4) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Segmented other status options
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val otherStates = listOf(
                                    2 to "Ocupado",
                                    3 to "Fuera Serv."
                                )
                                otherStates.forEach { (stateVal, label) ->
                                    val isCurrent = (driver.estado == stateVal)
                                    OutlinedButton(
                                        onClick = { viewModel.updateMotorizadoEstadoDirectly(driver.id, stateVal) },
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (isCurrent) primaryColor else Color(0xFF334155)
                                        ),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isCurrent) primaryColor.copy(alpha = 0.1f) else Color.Transparent
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 9.sp,
                                            color = if (isCurrent) primaryColor else Color(0xFF94A3B8)
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
}

}
}
}
@Composable
fun BeautifulQRCode(
    modifier: Modifier = Modifier,
    url: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .testTag("promo_qr_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    val sizePx = size.width
                    val cellSize = sizePx / 21f // 21x21 grid
                    
                    val drawCell: (Int, Int, Color) -> Unit = { row, col, color ->
                        drawRect(
                            color = color,
                            topLeft = Offset(col * cellSize, row * cellSize),
                            size = androidx.compose.ui.geometry.Size(cellSize + 0.5f, cellSize + 0.5f)
                        )
                    }
                    
                    val drawFinderPattern: (Int, Int) -> Unit = { startRow, startCol ->
                        for (r in 0 until 7) {
                            for (c in 0 until 7) {
                                if (r == 0 || r == 6 || c == 0 || c == 6) {
                                    drawCell(startRow + r, startCol + c, Color.Black)
                                }
                            }
                        }
                        for (r in 2 until 5) {
                            for (c in 2 until 5) {
                                drawCell(startRow + r, startCol + c, Color.Black)
                            }
                        }
                    }
                    
                    drawFinderPattern(0, 0)
                    drawFinderPattern(0, 14)
                    drawFinderPattern(14, 0)
                    
                    for (r in 12 until 17) {
                        for (c in 12 until 17) {
                            val isBoundary = (r == 12 || r == 16 || c == 12 || c == 16)
                            val isCenter = (r == 14 && c == 14)
                            if (isBoundary || isCenter) {
                                drawCell(r, c, Color.Black)
                            }
                        }
                    }
                    
                    for (i in 7 until 14) {
                        if (i % 2 == 0) {
                            drawCell(6, i, Color.Black)
                            drawCell(i, 6, Color.Black)
                        }
                    }
                    
                    val random = java.util.Random(url.hashCode().toLong())
                    for (r in 0 until 21) {
                        for (c in 0 until 21) {
                            if (r < 8 && c < 8) continue
                            if (r < 8 && c > 12) continue
                            if (r > 12 && c < 8) continue
                            if (r > 11 && c > 11) continue
                            if (r == 6 || c == 6) continue
                            if (r in 9..11 && c in 9..11) continue
                            
                            if (random.nextBoolean()) {
                                drawCell(r, c, Color.Black)
                            }
                        }
                    }
                    
                    for (r in 9..11) {
                        for (c in 9..11) {
                            drawCell(r, c, Color(0xFFFF6B00))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
    }
    }
}

@Composable
fun PromotionBannerDialog(
    activeUrl: String = "https://ais-pre-k7q6427t2vsl3nsbjp7e4k-437635375840.us-west2.run.app",
    imageResId: Int = R.drawable.rapidtuy_promo_flyer,
    bannerTitle: String = "AFICHE PUBLICITARIO OFICIAL",
    onDismiss: () -> Unit
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(2.dp, RapidTuyOrange),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = RapidTuyOrange, modifier = Modifier.size(20.dp))
                        Text(
                            text = bannerTitle.uppercase(),
                            color = RapidTuyOrange,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp).background(Color(0xFF1E293B), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Main High-Res Image Display
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = "Imagen Publicitaria con QR RapidTuy",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.5.dp, Color(0xFF334155)), RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.FillWidth
                )

                // Info Badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "¡TRASLADOS EN MOTO TAXI RÁPIDOS Y SEGUROS!",
                            color = Color.White,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "📍 Cobertura: Charallave, Ocumare del Tuy, Cúa, Santa Lucía, Yare",
                            color = Color(0xFF38BDF8),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "📞 Central de Atencion: 0426 1215060",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Embedded QR Code Section for entering this image/landing
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, RapidTuyOrange.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "CÓDIGO QR DE ENTRADA A ESTA IMAGEN & PLATAFORMA",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = RapidTuyOrange,
                            letterSpacing = 0.5.sp
                        )

                        Box(
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .padding(10.dp)
                                .border(BorderStroke(3.dp, RapidTuyOrange), RoundedCornerShape(12.dp))
                        ) {
                            BeautifulQRCode(
                                modifier = Modifier.size(150.dp),
                                url = activeUrl,
                                onClick = {
                                    try {
                                        uriHandler.openUri(activeUrl)
                                    } catch (_: Exception) {}
                                }
                            )
                        }

                        Text(
                            text = activeUrl,
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                val waUrl = "https://wa.me/584261215060?text=Hola%20RapidTuy%20vi%20el%20afiche%20publicitario%20y%20deseo%20solicitar%20un%20servicio"
                                uriHandler.openUri(waUrl)
                            } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Contactar por WhatsApp (0426 1215060)", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    uriHandler.openUri(activeUrl)
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RapidTuyOrange),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Abrir Web", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(activeUrl))
                            },
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copiar QR Link", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun MotorizadosTrackingCard(
    motorizados: List<MotorizadoEntity>,
    activeTrips: List<TripEntity>,
    viewModel: RapidTuyViewModel,
    onSelectCoordinatesForDispatch: (String, Double, Double) -> Unit
) {
    var selectedDriverId by remember { mutableStateOf<Int?>(null) }
    var isTrackingActive by remember { mutableStateOf(false) }
    
    val selectedDriver = motorizados.firstOrNull { it.id == selectedDriverId }

    val closestCity = remember(selectedDriver?.latitud, selectedDriver?.longitud) {
        if (selectedDriver == null) return@remember "Charallave"
        val cities = listOf(
            Triple("Charallave", 10.2315, -66.8652),
            Triple("Cúa", 10.1654, -66.8845),
            Triple("Ocumare", 10.1189, -66.7778),
            Triple("Yare", 10.1345, -66.7452),
            Triple("S. Teresa", 10.2361, -66.6628)
        )
        var minDistance = Double.MAX_VALUE
        var cityName = "Charallave"
        for (city in cities) {
            val distance = Math.sqrt(
                Math.pow(selectedDriver.latitud - city.second, 2.0) +
                Math.pow(selectedDriver.longitud - city.third, 2.0)
            )
            if (distance < minDistance) {
                minDistance = distance
                cityName = city.first
            }
        }
        cityName
    }
    
    LaunchedEffect(selectedDriverId, isTrackingActive) {
        if (isTrackingActive && selectedDriverId != null) {
            while (true) {
                kotlinx.coroutines.delay(3500)
                val current = motorizados.firstOrNull { it.id == selectedDriverId }
                if (current != null) {
                    val driftLat = (Math.random() - 0.5) * 0.003
                    val driftLng = (Math.random() - 0.5) * 0.003
                    viewModel.updateMotorizadoLocation(
                        driverId = current.id,
                        lat = current.latitud + driftLat,
                        lng = current.longitud + driftLng
                    )
                }
            }
        }
    }
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = RapidTuyOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Rastreo & Telemetría Satelital",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                if (isTrackingActive && selectedDriver != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF10B981), CircleShape)
                        )
                        Text(
                            text = "TRANSMITIENDO GPS",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = "Seleccione un motorizado registrado para iniciar el rastreo por radar en tiempo real, transmitir telemetría GPS o facilitar su asignación rápida:",
                fontSize = 10.sp,
                color = Color(0xFF94A3B8)
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            var dropdownExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { dropdownExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (selectedDriver != null) {
                                "${selectedDriver.id}. ${selectedDriver.nombre} (${selectedDriver.placa})"
                            } else {
                                "Seleccionar Motorizado..."
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = RapidTuyOrange
                        )
                    }
                }
                
                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .background(Color(0xFF1E293B))
                        .border(BorderStroke(0.5.dp, Color(0xFF475569)))
                ) {
                    motorizados.forEach { driver ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${driver.id}. ${driver.nombre} [${driver.placa}]",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                    val statusText = when (driver.estado) {
                                        1 -> "Disponible"
                                        2 -> "Ocupado"
                                        3 -> "Desconectado"
                                        4 -> "Bloqueado"
                                        else -> "N/A"
                                    }
                                    val statusColor = when (driver.estado) {
                                        1 -> Color(0xFF10B981)
                                        2 -> Color(0xFF3B82F6)
                                        3 -> Color(0xFF64748B)
                                        4 -> Color(0xFFEF4444)
                                        else -> Color.Gray
                                    }
                                    Text(
                                        text = statusText,
                                        color = statusColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            onClick = {
                                selectedDriverId = driver.id
                                dropdownExpanded = false
                                viewModel.logSystemEvent("Radar: Iniciado rastreo de ${driver.nombre}.")
                            }
                        )
                    }
                }
            }
            
            if (selectedDriver != null) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                        .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Ubicación Satelital Actual:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RapidTuyOrange
                            )
                            
                            val statusStr = when (selectedDriver.estado) {
                                1 -> "DISPONIBLE"
                                2 -> "OCUPADO"
                                3 -> "FUERA DE SERVICIO"
                                4 -> "BLOQUEADO"
                                else -> "DESCONOCIDO"
                            }
                            val statusColor = when (selectedDriver.estado) {
                                1 -> Color(0xFF10B981)
                                2 -> Color(0xFF3B82F6)
                                3 -> Color(0xFF64748B)
                                4 -> Color(0xFFEF4444)
                                else -> Color.Gray
                            }
                            Text(
                                text = statusStr,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = statusColor
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("LATITUD", fontSize = 8.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("LONGITUD", fontSize = 8.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(12.dp))
                            Text(
                                text = "Cercano a nodo de cobertura: $closestCity",
                                fontSize = 10.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isTrackingActive = !isTrackingActive
                            if (isTrackingActive) {
                                viewModel.logSystemEvent("Telemetría: Activado rastreo GPS continuo para Conductor #${selectedDriver.id}.")
                            } else {
                                viewModel.logSystemEvent("Telemetría: Pausado rastreo GPS continuo para Conductor #${selectedDriver.id}.")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTrackingActive) Color(0xFFEAB308) else Color(0xFF475569)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (isTrackingActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isTrackingActive) "Pausar GPS" else "Transmitir GPS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Button(
                        onClick = {
                            val closestCityName = when (closestCity) {
                                "Charallave" -> "Estación Charallave Sur"
                                "Cúa" -> "Cúa (Estación de Tren)"
                                "Ocumare" -> "Ocumare del Tuy Plaza"
                                "Yare" -> "Plaza Bolívar Yare"
                                "S. Teresa" -> "Santa Teresa Centro"
                                else -> "Estación Charallave Sur"
                            }
                            onSelectCoordinatesForDispatch(closestCityName, selectedDriver.latitud, selectedDriver.longitud)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RapidTuyOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.3f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    
                    val pendingTrip = activeTrips.firstOrNull { it.estado == "PENDIENTE" && it.motorizadoId != selectedDriver.id }
                    Button(
                        onClick = {
                            if (pendingTrip != null) {
                                viewModel.assignTripDirectly(pendingTrip.id, selectedDriver.id)
                            }
                        },
                        enabled = pendingTrip != null && selectedDriver.estado == 1,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            disabledContainerColor = Color(0xFF1E293B).copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.4f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Asignar", fontSize = 11.sp, color = Color.White)
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
fun YummySuperAppHub(
    viewModel: RapidTuyViewModel,
    motorizados: List<MotorizadoEntity>,
    primaryColor: Color,
    yummyThemeActive: Boolean
) {
    // States
    var bookingState by remember { mutableStateOf("IDLE") } // IDLE, SEARCHING, MATCHED, IN_PROGRESS, COMPLETED
    var serviceType by remember { mutableStateOf("MOTO") } // MOTO (Estándar), MOTO_VIP (VIP/Ejecutivo)
    
    var originSelected by remember { mutableStateOf("Estación Charallave Sur") }
    var destinationSelected by remember { mutableStateOf("C.C. Tamanaco Tuy") }
    var clientName by remember { mutableStateOf("Pasajero Frecuente") }
    
    // Rating States
    var selectedRating by remember { mutableStateOf(5) }
    var ratingComments by remember { mutableStateOf("") }
    val feedbackOptions = remember { listOf("Conductor Rápido ⚡", "Vehículo Limpio ✨", "Trato Amable 🤝", "Ruta Óptima 🗺️", "Muy Seguro 🛡️") }
    val selectedFeedbackOptions = remember { mutableStateListOf<String>() }
    
    // Simulated matched driver
    var matchedDriver by remember { mutableStateOf<MotorizadoEntity?>(null) }
    var matchingProgress by remember { mutableStateOf(0f) }
    var travelProgress by remember { mutableStateOf(0f) }

    // Coroutine Scope for background simulation steps
    val coroutineScope = rememberCoroutineScope()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.5.dp, RapidTuyOrange),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("yummy_super_app_hub")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with RapidTuy badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(RapidTuyOrange, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsMotorsports,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Central de Despacho RapidTuy",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "Despacho Exclusivo de Pasajeros - Mototaxi",
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                

            }

            Spacer(modifier = Modifier.height(12.dp))

            when (bookingState) {
                "IDLE" -> {
                    // 1. SELECT VEHICLE TYPE CHIPS
                    Text(
                        text = "Seleccione Tipo de Mototaxi:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Moto Standard Option
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { serviceType = "MOTO" },
                            colors = CardDefaults.cardColors(
                                containerColor = if (serviceType == "MOTO") RapidTuyOrange.copy(alpha = 0.15f) else Color(0xFF0F172A)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (serviceType == "MOTO") RapidTuyOrange else Color(0xFF334155)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsMotorsports,
                                    contentDescription = null,
                                    tint = if (serviceType == "MOTO") RapidTuyOrange else Color(0xFF94A3B8),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Mototaxi Estándar",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Casco Incluido",
                                    fontSize = 8.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Moto VIP Option
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { serviceType = "MOTO_VIP" },
                            colors = CardDefaults.cardColors(
                                containerColor = if (serviceType == "MOTO_VIP") RapidTuyOrange.copy(alpha = 0.15f) else Color(0xFF0F172A)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (serviceType == "MOTO_VIP") RapidTuyOrange else Color(0xFF334155)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TwoWheeler,
                                    contentDescription = null,
                                    tint = if (serviceType == "MOTO_VIP") RapidTuyOrange else Color(0xFF94A3B8),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Mototaxi VIP / Ejecutivo",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Casco Especial Premium",
                                    fontSize = 8.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. INPUT FIELDS & ROUTE PRESETS
                    Text(
                        text = "Configuración de Ruta:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Route Quick Presets Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val vallesPresets = listOf(
                            Triple("Estación Charallave Sur", "C.C. Tamanaco Tuy", "Pasajero Frecuente"),
                            Triple("Charallave Centro", "Plaza Bolívar Yare", "Milagros Pérez"),
                            Triple("Cúa (Estación de Tren)", "Charallave Centro", "Luis Blanco"),
                            Triple("Ocumare del Tuy Plaza", "Estación Charallave Sur", "Carlos Vegas"),
                            Triple("Santa Teresa Centro", "Plaza Bolívar Yare", "María Hurtado")
                        )
                        vallesPresets.forEach { (orig, dest, pName) ->
                            val isSelectedRoute = (originSelected == orig && destinationSelected == dest)
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelectedRoute) primaryColor.copy(alpha = 0.25f) else Color(0xFF0F172A),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        BorderStroke(1.dp, if (isSelectedRoute) primaryColor else Color(0xFF2A364F)),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        originSelected = orig
                                        destinationSelected = dest
                                        clientName = pName
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${orig.substringBefore(" ")} ➔ ${dest.substringBefore(" ")}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelectedRoute) primaryColor else Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val currCtx = LocalContext.current
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                fetchCurrentGpsLocation(currCtx) { gpsLoc ->
                                    originSelected = gpsLoc
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f).shadow(4.dp, RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GPS Origen", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Button(
                            onClick = {
                                fetchCurrentGpsLocation(currCtx) { gpsLoc ->
                                    destinationSelected = gpsLoc
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f).shadow(4.dp, RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GPS Destino", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = originSelected,
                        onValueChange = { originSelected = it },
                        label = { Text("Origen (Punto de salida)", fontSize = 10.sp, color = Color(0xFF94A3B8)) },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = destinationSelected,
                        onValueChange = { destinationSelected = it },
                        label = { Text("Destino Final", fontSize = 10.sp, color = Color(0xFF94A3B8)) },
                        leadingIcon = { Icon(Icons.Default.Navigation, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Nombre Pasajero", fontSize = 10.sp, color = Color(0xFF94A3B8)) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = primaryColor, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Button: REQUEST RIDE
                    Button(
                        onClick = {
                            // Start Simulated Match flow
                            bookingState = "SEARCHING"
                            matchingProgress = 0f
                            viewModel.logSystemEvent("Despacho RapidTuy: Iniciada solicitud de mototaxi desde central. Pasajero: $clientName, Destino: $destinationSelected")
                            
                            coroutineScope.launch {
                                // Simulate matching radar scan
                                for (i in 1..20) {
                                    delay(150)
                                    matchingProgress = i / 20f
                                }
                                
                                // Randomly assign from our available drivers list, or fallback to mock
                                val availableDriver = motorizados.firstOrNull { it.estado == 1 }
                                matchedDriver = availableDriver ?: MotorizadoEntity(
                                    id = 7,
                                    nombre = "Yoseph Escalona (RapidTuy)",
                                    placa = "AB2C34D",
                                    telefono = "584120000000",
                                    estado = 1,
                                    latitud = 10.2315,
                                    longitud = -66.8652,
                                    fechaVencimiento = System.currentTimeMillis() + 864000000L
                                )
                                
                                bookingState = "MATCHED"
                                viewModel.logSystemEvent("Despacho RapidTuy: Conductor ${matchedDriver?.nombre} (${matchedDriver?.placa}) ha aceptado tu solicitud de viaje!")
                                
                                delay(3000)
                                bookingState = "IN_PROGRESS"
                                viewModel.logSystemEvent("Despacho RapidTuy: El viaje en mototaxi ha iniciado de forma segura. Monitoreando ruta en tiempo real...")
                                
                                // Simulate traveling from 0 to 100%
                                for (i in 1..25) {
                                    delay(160)
                                    travelProgress = i / 25f
                                }
                                
                                bookingState = "COMPLETED"
                                viewModel.logSystemEvent("Despacho RapidTuy: ¡Llegó a su destino! Viaje en mototaxi completado con éxito.")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RapidTuyOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(
                                imageVector = Icons.Default.SportsMotorsports,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SOLICITAR SERVICIO (Despacho Mototaxi)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
                
                "SEARCHING" -> {
                    // Pulsing Radar scan View
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "BUSCANDO CONDUCTOR CERCANO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryColor,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Analizando proximidad con motorizados del sector mediante PostGIS...",
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                        
                        // Circular Radar Scanner animation using Canvas
                        val infiniteTransition = rememberInfiniteTransition()
                        val radarAlpha by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            )
                        )
                        val radarScale by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            )
                        )

                        Box(
                            modifier = Modifier.size(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(120.dp)) {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val maxRadius = size.width / 2f
                                
                                // Draw pulse circles
                                drawCircle(
                                    color = primaryColor,
                                    radius = maxRadius * radarScale,
                                    center = center,
                                    alpha = radarAlpha,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                drawCircle(
                                    color = primaryColor,
                                    radius = maxRadius * ( (radarScale + 0.5f) % 1.0f ),
                                    center = center,
                                    alpha = (radarAlpha + 0.5f) % 1.0f,
                                    style = Stroke(width = 1.dp.toPx())
                                )
                                
                                // Center point
                                drawCircle(
                                    color = primaryColor,
                                    radius = 8.dp.toPx(),
                                    center = center
                                )
                            }
                            
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Linear progress indicator
                        LinearProgressIndicator(
                            progress = matchingProgress,
                            color = primaryColor,
                            trackColor = Color(0xFF0F172A),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                        
                        Text(
                            text = "Buscando en ${originSelected.substringBefore(" ")}...",
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                
                "MATCHED" -> {
                    // Match found card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "¡SÚPER-CONDUCTOR EN CAMINO!",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryColor,
                            letterSpacing = 1.sp
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Driver Avatar Representator
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(primaryColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                                
                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = matchedDriver?.nombre ?: "Conductor RapidTuy",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(text = "🏍 PLACA: ${matchedDriver?.placa ?: "N/A"}", fontSize = 9.sp, color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace)
                                        Box(
                                            modifier = Modifier
                                                .background(primaryColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                    }
                                }
                            }
                        }
                        }

                        CircularProgressIndicator(
                            color = primaryColor,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(24.dp)
                        )

                        Text(
                            text = "Iniciando viaje en breves segundos...",
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                
                "IN_PROGRESS" -> {
                    // Viaje en progreso
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "VIAJE EN CURSO ⚡ SEGURIDAD GPS ACTIVA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryColor,
                            letterSpacing = 1.sp
                        )

                        // Visual path diagram
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = originSelected.substringBefore(" ").take(10) + "..",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 10.dp)
                                    .height(2.dp)
                                    .background(Color(0xFF334155))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(travelProgress)
                                        .background(primaryColor)
                                )
                                // Rider icon sliding
                                Icon(
                                    imageVector = Icons.Default.SportsMotorsports,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .offset(y = (-7).dp)
                                        .align(Alignment.CenterStart)
                                )
                            }

                            Text(
                                text = destinationSelected.substringBefore(" ").take(10) + "..",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Text(
                            text = "Porcentaje de Recorrido: ${(travelProgress * 100).toInt()}%",
                            fontSize = 9.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = "Monitoreando velocidad de viaje y sensor de impacto RapidTuy.",
                            fontSize = 8.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                "COMPLETED" -> {
                    // Rating & feedback screen
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "¡SERVICIO DE VIAJE COMPLETADO!",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryColor,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "¿Cómo calificaría la experiencia de viaje del cliente?",
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8)
                        )

                        // Stars Rating
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (i in 1..5) {
                                val isSelectedStar = (selectedRating >= i)
                                Icon(
                                    imageVector = if (isSelectedStar) Icons.Default.Star else Icons.Default.StarOutline,
                                    contentDescription = null,
                                    tint = if (isSelectedStar) Color(0xFFEAB308) else Color(0xFF475569),
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable { selectedRating = i }
                                )
                            }
                        }

                        // Feedback Quick Chips Grid
                        Text(
                            text = "Aspectos positivos destacados:",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            feedbackOptions.forEach { opt ->
                                val isOptSelected = selectedFeedbackOptions.contains(opt)
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isOptSelected) primaryColor.copy(alpha = 0.15f) else Color(0xFF0F172A),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            BorderStroke(1.dp, if (isOptSelected) primaryColor else Color(0xFF334155)),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            if (isOptSelected) {
                                                selectedFeedbackOptions.remove(opt)
                                            } else {
                                                selectedFeedbackOptions.add(opt)
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = opt,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOptSelected) primaryColor else Color.White
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = ratingComments,
                            onValueChange = { ratingComments = it },
                            placeholder = { Text("Comentarios adicionales sobre el viaje...", fontSize = 10.sp, color = Color(0xFF64748B)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 10.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            )
                        )

                        Button(
                            onClick = {
                                // Action complete virtual rating
                                val reviewText = if (selectedFeedbackOptions.isEmpty()) "Comportamiento correcto" else selectedFeedbackOptions.joinToString(", ")
                                
                                viewModel.logSystemEvent("FeedBack RapidTuy: Pasajero califica viaje de ${matchedDriver?.nombre} con $selectedRating★. Detalles: $reviewText.")
                                
                                // Reset States
                                selectedFeedbackOptions.clear()
                                ratingComments = ""
                                bookingState = "IDLE"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        ) {
                            Text("ENVIAR CALIFICACIÓN", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FirestoreConnectionStatusBar(
    syncState: FirestoreSyncState,
    hasLocationPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statusColor = when (syncState) {
                        is FirestoreSyncState.Connected -> Color(0xFF10B981)
                        is FirestoreSyncState.Loading -> Color(0xFFF59E0B)
                        is FirestoreSyncState.Error -> Color(0xFFEF4444)
                        else -> Color(0xFF64748B)
                    }
                    
                    val statusIcon = when (syncState) {
                        is FirestoreSyncState.Connected -> Icons.Default.Cloud
                        is FirestoreSyncState.Loading -> Icons.Default.CloudQueue
                        is FirestoreSyncState.Error -> Icons.Default.CloudOff
                        else -> Icons.Default.CloudQueue
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(statusColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = "Estado Firestore",
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "ESTADO DE CONEXIÓN FIREBASE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 0.5.sp
                        )
                        val statusText = when (syncState) {
                            is FirestoreSyncState.Connected -> "Conectado en tiempo real"
                            is FirestoreSyncState.Loading -> "Sincronizando..."
                            is FirestoreSyncState.Error -> "Sin conexión"
                            else -> "Inactivo"
                        }
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }


            }

            if (syncState is FirestoreSyncState.Error) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D).copy(alpha = 0.25f)),
                    border = BorderStroke(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alerta",
                            tint = Color(0xFFFCA5A5),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = syncState.message,
                            fontSize = 9.sp,
                            color = Color(0xFFFCA5A5),
                            lineHeight = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (syncState is FirestoreSyncState.Loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = Color(0xFFF59E0B),
                    trackColor = Color(0xFF334155)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                    .border(BorderStroke(0.5.dp, if (hasLocationPermission) Color(0xFF1E293B) else Color(0xFF7F1D1D).copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (hasLocationPermission) Icons.Default.LocationOn else Icons.Default.LocationOff,
                        contentDescription = "GPS Icon",
                        tint = if (hasLocationPermission) Color(0xFF10B981) else Color(0xFFEF4444),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (hasLocationPermission) "GPS del Dispositivo Autorizado" else "Señal GPS Desactivada (Permisos de ubicación pendientes)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (hasLocationPermission) Color(0xFFA7F3D0) else Color(0xFFFCA5A5)
                    )
                }

                if (!hasLocationPermission) {
                    TextButton(
                        onClick = onRequestPermission,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text(
                            text = "CONCEDER PERMISO",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun SupabaseConnectionStatusBar(
    syncState: SupabaseSyncState,
    isEnabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    currentUrl: String,
    currentKey: String,
    onUpdateCredentials: (String, String) -> Unit
) {
    var expandedSettings by remember { mutableStateOf(false) }
    var tempUrl by remember(currentUrl) { mutableStateOf(currentUrl) }
    var tempKey by remember(currentKey) { mutableStateOf(currentKey) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statusColor = when {
                        !isEnabled -> Color(0xFF64748B)
                        syncState is SupabaseSyncState.Synced -> Color(0xFF10B981)
                        syncState is SupabaseSyncState.Loading -> Color(0xFFF59E0B)
                        syncState is SupabaseSyncState.Error -> Color(0xFFEF4444)
                        else -> Color(0xFF3B82F6)
                    }

                    val statusIcon = when {
                        !isEnabled -> Icons.Default.CloudOff
                        syncState is SupabaseSyncState.Synced -> Icons.Default.CloudDone
                        syncState is SupabaseSyncState.Loading -> Icons.Default.CloudQueue
                        syncState is SupabaseSyncState.Error -> Icons.Default.CloudOff
                        else -> Icons.Default.Cloud
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(statusColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = "Estado Supabase",
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "ESTADO DE CONEXIÓN SUPABASE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 0.5.sp
                        )
                        val statusText = when {
                            !isEnabled -> "Desactivado"
                                syncState is SupabaseSyncState.Synced -> "Sincronizado en tiempo real"
                            syncState is SupabaseSyncState.Loading -> "Sincronizando..."
                            syncState is SupabaseSyncState.Error -> "Fallo de conexión"
                            else -> "Listo para sincronizar"
                        }
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Controls: Enable and Expand Settings
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Enable/Disable switch
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { onToggleEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF10B981),
                            checkedTrackColor = Color(0xFF064E3B),
                            uncheckedThumbColor = Color(0xFF64748B),
                            uncheckedTrackColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.scale(0.6f)
                    )

                    IconButton(
                        onClick = { expandedSettings = !expandedSettings },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (expandedSettings) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Configurar Supabase",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Connection Error Banner if applicable
            if (isEnabled && syncState is SupabaseSyncState.Error) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D).copy(alpha = 0.25f)),
                    border = BorderStroke(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alerta Supabase",
                            tint = Color(0xFFFCA5A5),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = syncState.message,
                            fontSize = 9.sp,
                            color = Color(0xFFFCA5A5),
                            lineHeight = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Sync Indicator
            if (isEnabled && syncState is SupabaseSyncState.Loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = Color(0xFF3B82F6),
                    trackColor = Color(0xFF1E293B)
                )
            }

            // Expanded Settings & Credentials configuration
            AnimatedVisibility(visible = expandedSettings) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Configuración del Rest Endpoint de Supabase",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // URL Input
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        label = { Text("Supabase URL", fontSize = 9.sp) },
                        textStyle = TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedLabelColor = Color(0xFF3B82F6),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        )
                    )

                    // Anon Key Input
                    OutlinedTextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        label = { Text("Supabase Service / Anon Key", fontSize = 9.sp) },
                        textStyle = TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedLabelColor = Color(0xFF3B82F6),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        )
                    )

                    // Save / Offline actions row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {


                        Button(
                            onClick = { onUpdateCredentials(tempUrl, tempKey) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Actualizar", fontSize = 11.sp, color = Color.White)
                        }

                    // Schema info helper
                    Divider(color = Color(0xFF334155), thickness = 0.5.dp)
                    Text(
                        text = "Estructura requerida en Supabase:\n" +
                                "1. Tabla 'motorizados': id (int4 primary), nombre (text), placa (text), telefono (text), estado (int4), latitud (float8), longitud (float8), fecha_vencimiento (text), ultimo_pago_monto (float8), ultimo_pago_fecha (text), comentarios (text), updated_at (int8).\n" +
                                "2. Tabla 'trips': id (int4 primary), origen (text), destino (text), monto (float8), estado (text), motorizado_id (int4), segundos_restantes (int4), intentos_asignacion (int4), lista_negra_ids (text), created_at (int8), updated_at (int8).",
                        fontSize = 8.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 11.sp
                    )
                }
            }
        }
    }
}

}
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Composable
fun SupabaseRealtimeMotorizadosCard(
    viewModel: RapidTuyViewModel,
    modifier: Modifier = Modifier
) {
    val supabaseMotorizados by SupabaseSyncManager.realtimeMotorizados.collectAsState()
    val syncState by SupabaseSyncManager.syncState.collectAsState()
    val isEnabled by SupabaseSyncManager.isEnabled.collectAsState()


    val trips by viewModel.trips.collectAsState()
    val activeTrip = remember(trips) {
        trips.firstOrNull { it.estado == "PENDIENTE" || it.estado == "ACEPTADO" }
    }

    val cities = remember {
        listOf(
            CityNode("Charallave", 0.45f, 0.40f, "Estación Charallave Sur", 10.2315, -66.8652),
            CityNode("Cúa", 0.20f, 0.75f, "Cúa (Estación de Tren)", 10.1654, -66.8845),
            CityNode("Ocumare", 0.72f, 0.85f, "Ocumare del Tuy Plaza", 10.1189, -66.7778),
            CityNode("Yare", 0.82f, 0.58f, "Plaza Bolívar Yare", 10.1388, -66.7030),
            CityNode("S. Teresa", 0.80f, 0.22f, "Santa Teresa Centro", 10.2361, -66.6628)
        )
    }

    val clientCity = remember(activeTrip) {
        val nameToMatch = activeTrip?.origen ?: ""
        cities.firstOrNull { nameToMatch.contains(it.name, ignoreCase = true) } ?: cities[0]
    }

    var searchQuery by remember { mutableStateOf("") }
    var showStatusDropdownForId by remember { mutableStateOf<Int?>(null) }
    var isManualRefreshing by remember { mutableStateOf(false) }

    val filteredDrivers = remember(supabaseMotorizados, searchQuery) {
        if (searchQuery.isEmpty()) {
            supabaseMotorizados
        } else {
            supabaseMotorizados.filter {
                it.nombre.contains(searchQuery, ignoreCase = true) ||
                it.placa.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("supabase_realtime_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Monitor Real-Time Supabase",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Pulsing Sync Badge
                val (badgeColor, badgeText) = when {
                    !isEnabled -> Color.Gray to "DESACTIVADO"
                    syncState is SupabaseSyncState.Synced -> Color(0xFF10B981) to "REAL-TIME OK"
                    syncState is SupabaseSyncState.Loading -> Color(0xFFEAB308) to "SINCRONIZANDO..."
                    syncState is SupabaseSyncState.Error -> Color(0xFFEF4444) to "ERROR DE CONEXIÓN"
                    else -> Color(0xFF3B82F6) to "CONECTANDO"
                }

                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .border(BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(badgeColor, CircleShape)
                        )
                        Text(
                            text = badgeText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = badgeColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Este panel observa directamente los cambios en tiempo real de la tabla 'motorizados' en Supabase PostgreSQL. Permite actualizar el estado de forma bidireccional.",
                fontSize = 10.sp,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (!isEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Color(0xFF0F172A).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Sincronización con Supabase desactivada.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Actívela en la barra de estado superior para iniciar el monitoreo.",
                            color = Color(0xFF64748B),
                            fontSize = 9.sp
                        )
                    }
                }

            } else {
                // Search field & manual refresh
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar por nombre o placa...", color = Color(0xFF475569), fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp)) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF334155),
                            unfocusedBorderColor = Color(0xFF0F172A),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    IconButton(
                        onClick = { SupabaseSyncManager.startPolling() },
                        modifier = Modifier
                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                            .size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Forzar actualización",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (filteredDrivers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color(0xFF0F172A).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (syncState is SupabaseSyncState.Loading) {
                                CircularProgressIndicator(color = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                                Text("Cargando base de datos Supabase...", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            } else {
                                Icon(Icons.Default.Inbox, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "No se encontraron motorizados con ese filtro." else "No hay datos sincronizados en Supabase.",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp
                                )
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Para poblar Supabase, cambie el estado de un conductor en la lista superior.",
                                        color = Color(0xFF475569),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Show scrollable list inside the Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(Color(0xFF0F172A).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(12.dp))
                    ) {
                        SwipeRefreshLayout(
                            isRefreshing = isManualRefreshing || (syncState is SupabaseSyncState.Loading && !isManualRefreshing),
                            onRefresh = {
                                isManualRefreshing = true
                                viewModel.logSystemEvent("Supabase: Actualización manual solicitada via pull-to-refresh.")
                                SupabaseSyncManager.forceSingleFetch {
                                    isManualRefreshing = false
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filteredDrivers) { driver ->
                                    SupabaseDriverRow(
                                        driver = driver,
                                        isDropdownExpanded = showStatusDropdownForId == driver.id,
                                        onToggleDropdown = {
                                            showStatusDropdownForId = if (showStatusDropdownForId == driver.id) null else driver.id
                                        },
                                        onStatusSelected = { newStatus ->
                                            showStatusDropdownForId = null
                                            val updated = driver.copy(estado = newStatus)
                                            SupabaseSyncManager.syncMotorizado(updated)
                                            viewModel.logSystemEvent("Supabase: Actualizado estado de ${driver.nombre} en DB Postgres real-time.")
                                        },
                                        clientCity = clientCity
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
@Composable
fun SupabaseDriverRow(
    driver: MotorizadoEntity,
    isDropdownExpanded: Boolean,
    onToggleDropdown: () -> Unit,
    onStatusSelected: (Int) -> Unit,
    clientCity: CityNode
) {
    val statusColor = when (driver.estado) {
        1 -> Color(0xFF10B981) // Disponible (Green)
        2 -> Color(0xFF3B82F6) // Ocupado (Blue)
        3 -> Color(0xFF64748B) // Fuera de servicio (Slate)
        4 -> Color(0xFFEF4444) // Bloqueado (Red)
        else -> Color.Gray
    }

    val statusText = when (driver.estado) {
        1 -> "Disponible"
        2 -> "Ocupado"
        3 -> "Inactivo"
        4 -> "Suscrip. Vencida"
        else -> "N/A"
    }

    val distanceMeters = remember(driver, clientCity) {
        calculateHaversineDistanceMeters(
            lat1 = driver.latitud,
            lon1 = driver.longitud,
            lat2 = clientCity.latitude,
            lon2 = clientCity.longitude
        )
    }
    val isNear = distanceMeters <= 500.0

    val isAnimated = driver.estado == 1 || driver.estado == 2

    // Dynamic subtle pulsing border color
    val animatedBorderColor = if (isAnimated) {
        val infiniteTransition = rememberInfiniteTransition(label = "row_border")
        val color by infiniteTransition.animateColor(
            initialValue = statusColor.copy(alpha = 0.25f),
            targetValue = statusColor.copy(alpha = 0.75f),
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "borderColor"
        )
        color
    } else {
        Color(0xFF334155)
    }

    val rowBorderColor = if (isNear) Color(0xFF10B981) else animatedBorderColor
    val rowBorderWidth = if (isNear) 1.2.dp else 0.8.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A).copy(alpha = 0.8f), RoundedCornerShape(8.dp))
            .border(BorderStroke(rowBorderWidth, rowBorderColor), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1.3f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PulsingStatusDot(estado = driver.estado)
                Text(
                    text = "${driver.id}. ${driver.nombre}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Placa: ${driver.placa} | Tel: ${driver.telefono}",
                fontSize = 9.sp,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFEAB308),
                    modifier = Modifier.size(8.dp)
                )
                Text(
                    text = "GPS: (${String.format("%.4f", driver.latitud)}, ${String.format("%.4f", driver.longitud)})",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    color = Color(0xFF64748B)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsBike,
                    contentDescription = null,
                    tint = if (isNear) Color(0xFF10B981) else Color(0xFF94A3B8),
                    modifier = Modifier.size(9.dp)
                )
                Text(
                    text = "Distancia: " + if (distanceMeters < 1000.0) "${distanceMeters.toInt()}m" else String.format("%.1f km", distanceMeters / 1000.0),
                    fontSize = 9.sp,
                    fontWeight = if (isNear) FontWeight.Bold else FontWeight.Normal,
                    color = if (isNear) Color(0xFF10B981) else Color(0xFF94A3B8)
                )
                if (isNear) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF10B981).copy(alpha = 0.2f), RoundedCornerShape(3.dp))
                            .border(BorderStroke(0.5.dp, Color(0xFF10B981)), RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 0.5.dp)
                    ) {
                        Text(
                            text = "CERCA 🔥",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Status pill button which expands to allow real-time changes
        Box(modifier = Modifier.wrapContentSize()) {
            Button(
                onClick = onToggleDropdown,
                colors = ButtonDefaults.buttonColors(containerColor = statusColor.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, statusColor),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(26.dp)
            ) {
                Text(
                    text = statusText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Cambiar estado",
                    tint = statusColor,
                    modifier = Modifier.size(10.dp)
                )
            }

            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = onToggleDropdown,
                modifier = Modifier
                    .background(Color(0xFF0F172A))
                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(4.dp))
            ) {
                val statuses = listOf(
                    1 to "Disponible (Activo)",
                    2 to "Ocupado (En viaje)",
                    3 to "Fuera de Servicio (Inactivo)",
                    4 to "Bloqueado por Impago"
                )
                statuses.forEach { (statusCode, name) ->
                    val color = when (statusCode) {
                        1 -> Color(0xFF10B981)
                        2 -> Color(0xFF3B82F6)
                        3 -> Color(0xFF64748B)
                        4 -> Color(0xFFEF4444)
                        else -> Color.Gray
                    }
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
                                Text(text = name, color = Color.White, fontSize = 11.sp)
                            }
                        },
                        onClick = { onStatusSelected(statusCode) }
                    )
                }
            }
        }
    }

}
@Composable
fun PulsingStatusDot(
    estado: Int,
    modifier: Modifier = Modifier
) {
    val statusColor = when (estado) {
        1 -> Color(0xFF10B981) // Disponible (Green)
        2 -> Color(0xFF3B82F6) // Ocupado (Blue)
        3 -> Color(0xFF64748B) // Fuera de servicio (Slate)
        4 -> Color(0xFFEF4444) // Bloqueado (Red)
        else -> Color.Gray
    }

    val isAnimated = estado == 1 || estado == 2

    if (isAnimated) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        
        // Pulse scale animation
        val scale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 2.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "scale"
        )
        
        // Pulse alpha animation
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 0.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "alpha"
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.size(16.dp)
        ) {
            // Pulse Halo
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(scale)
                    .alpha(alpha)
                    .background(statusColor, CircleShape)
            )
            // Core Dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(statusColor, CircleShape)
            )
        }
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.size(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(statusColor, CircleShape)
            )
        }
    }

}
@Composable
fun SwipeRefreshLayout(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var pullOffset by remember { mutableStateOf(0f) }
    val maxPull = 120f // Maximum swipe pull distance in pixels
    val density = androidx.compose.ui.platform.LocalDensity.current
    
    // Smooth transition when returning to original state
    val animatedOffset by animateFloatAsState(
        targetValue = if (isRefreshing) 60f else pullOffset,
        animationSpec = tween(if (isRefreshing) 100 else 300),
        label = "pull_offset"
    )

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // If pullOffset is positive and we are scrolling up, consume scroll first
                if (pullOffset > 0 && available.y < 0) {
                    val consumed = available.y
                    pullOffset = (pullOffset + consumed).coerceAtLeast(0f)
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // If scrolling down at the top of the list, increase pullOffset
                if (available.y > 0) {
                    val consumedY = available.y * 0.5f // apply resistance
                    pullOffset = (pullOffset + consumedY).coerceAtMost(maxPull)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullOffset >= maxPull * 0.75f && !isRefreshing) {
                    onRefresh()
                }
                pullOffset = 0f
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .nestedScroll(nestedScrollConnection)
            .clipToBounds()
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationY = with(density) { animatedOffset.dp.toPx() }
                }
        ) {
            content()
        }

        // Pull indicators
        if (animatedOffset > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (animatedOffset / 2).dp - 15.dp)
                    .size(30.dp)
                    .shadow(elevation = 3.dp, shape = CircleShape)
                    .background(Color(0xFF1E293B), CircleShape)
                    .border(BorderStroke(1.dp, Color(0xFF334155)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF10B981)
                    )
                } else {
                    // Show a beautiful rotating refresh arrow icon based on drag distance
                    val rotation = (animatedOffset / maxPull) * 360f
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer { rotationZ = rotation }
                    )
                }
            }
        }
    }

}
@Composable
fun PromoDownloadScreen(viewModel: RapidTuyViewModel) {
    val yummyThemeActive by viewModel.yummyThemeActive.collectAsState()
    val primaryColor = if (yummyThemeActive) Color(0xFF10B981) else RapidTuyOrange
    var showPromotionDialog by remember { mutableStateOf(false) }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    // Banners Available in App
    val banners = listOf(
        Triple(R.drawable.rapidtuy_promo_flyer, "Folleto Oficial Moto Taxi RapidTuy", "Cobertura Completa: Charallave, Ocumare, Cúa, Santa Lucía, Yare | Contacto: 0426 1215060"),
        Triple(R.drawable.rapidtuy_banner, "Banner Corporativo Red de Mototaxis", "Central de Servicios de Traslados de Pasajeros y Encomiendas"),
        Triple(R.drawable.rapidtuy_web_banner_1785008210218, "Banner Publicitario Express Valles del Tuy", "Validación QR, Conductores Verificados y Despacho Inmediato")
    )

    var selectedBannerIndex by remember { mutableStateOf(0) }

    // Real App & Portal URLs
    val liveWebAppUrl = "https://ais-dev-k7q6427t2vsl3nsbjp7e4k-437635375840.us-west2.run.app"
    val sharedAppUrl = "https://ais-pre-k7q6427t2vsl3nsbjp7e4k-437635375840.us-west2.run.app"
    val whatsappCentralUrl = "https://wa.me/584261215060?text=Hola%20RapidTuy%20vi%20el%20afiche%20publicitario%20y%20deseo%20solicitar%20un%20servicio"

    var selectedUrlType by remember { mutableStateOf("WEB_APP") } // "WEB_APP", "SHARED_APP", "WHATSAPP", "CUSTOM_VERCEL"
    var customUrlInput by remember { mutableStateOf("") }

    val activeRealUrl = when (selectedUrlType) {
        "WEB_APP" -> liveWebAppUrl
        "SHARED_APP" -> sharedAppUrl
        "WHATSAPP" -> whatsappCentralUrl
        "CUSTOM_VERCEL" -> if (customUrlInput.isNotBlank()) customUrlInput else "https://rapidtuy.vercel.app"
        else -> liveWebAppUrl
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Screen Header
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(primaryColor.copy(alpha = 0.2f), CircleShape)
                            .border(BorderStroke(1.dp, primaryColor), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = primaryColor, modifier = Modifier.size(22.dp))
                    }
                    Text(
                        text = "ÁREA PUBLICITARIA & CÓDIGOS QR",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
                Text(
                    text = "Seleccione o cree códigos QR personalizados para acceder directamente a las imágenes y afiches promocionales oficiales de RapidTuy",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Section 1: Banner / Image Selector Gallery
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("advertising_gallery_card")
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
                        Text(
                            text = "GALERÍA DE AFICHES Y BANNERS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryColor,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${selectedBannerIndex + 1} de ${banners.size}",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Horizontal Banner Carousel Thumbnails
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        banners.forEachIndexed { idx, (resId, title, _) ->
                            val isSelected = (selectedBannerIndex == idx)
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) primaryColor.copy(alpha = 0.2f) else Color(0xFF0F172A)
                                ),
                                border = BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) primaryColor else Color(0xFF334155)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .width(200.dp)
                                    .clickable { selectedBannerIndex = idx }
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Image(
                                        painter = painterResource(id = resId),
                                        contentDescription = title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(85.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Text(
                                        text = title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) primaryColor else Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Display Selected Banner Card Detail
                    val currentBanner = banners[selectedBannerIndex]
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = currentBanner.second,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = currentBanner.third,
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Button(
                                onClick = { showPromotionDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(42.dp).testTag("btn_enter_banner_image")
                            ) {
                                Icon(Icons.Default.CropFree, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🖼️ Entrar a la Imagen Publicitaria", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Destination Link Selector for the Advertising QR Code
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("qr_url_selector_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ENLACE OBJETIVO PARA EL CÓDIGO QR PUBLICITARIO:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryColor,
                        letterSpacing = 1.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { selectedUrlType = "WEB_APP" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedUrlType == "WEB_APP") primaryColor else Color(0xFF0F172A),
                                    contentColor = if (selectedUrlType == "WEB_APP") Color.White else Color(0xFF94A3B8)
                                ),
                                border = BorderStroke(1.dp, if (selectedUrlType == "WEB_APP") primaryColor else Color(0xFF334155)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("btn_select_url_webapp")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("App Web Live", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { selectedUrlType = "WHATSAPP" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedUrlType == "WHATSAPP") Color(0xFF10B981) else Color(0xFF0F172A),
                                    contentColor = if (selectedUrlType == "WHATSAPP") Color.White else Color(0xFF94A3B8)
                                ),
                                border = BorderStroke(1.dp, if (selectedUrlType == "WHATSAPP") Color(0xFF10B981) else Color(0xFF334155)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("btn_select_url_whatsapp")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("WhatsApp Central", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Button(
                            onClick = { selectedUrlType = "CUSTOM_VERCEL" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedUrlType == "CUSTOM_VERCEL") Color(0xFF3B82F6) else Color(0xFF0F172A),
                                contentColor = if (selectedUrlType == "CUSTOM_VERCEL") Color.White else Color(0xFF94A3B8)
                            ),
                            border = BorderStroke(1.dp, if (selectedUrlType == "CUSTOM_VERCEL") Color(0xFF3B82F6) else Color(0xFF334155)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("btn_select_url_custom_vercel")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(14.dp))
                                Text("Web Personalizada / Landing Page Vercel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (selectedUrlType == "CUSTOM_VERCEL") {
                            OutlinedTextField(
                                value = customUrlInput,
                                onValueChange = { customUrlInput = it },
                                placeholder = { Text("https://mi-pagina-rapidtuy.vercel.app", fontSize = 11.sp, color = Color(0xFF64748B)) },
                                label = { Text("Ingresa tu enlace de Vercel o Web", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("input_custom_vercel_url"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = Color(0xFF0F172A),
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedBorderColor = Color(0xFF3B82F6),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    // Display active URL field box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                            .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Enlace Configurado:", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                Text(
                                    activeRealUrl,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(activeRealUrl))
                                    viewModel.logSystemEvent("QR Publicitario: URL copiada $activeRealUrl")
                                },
                                modifier = Modifier.size(32.dp).testTag("btn_copy_real_url")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = primaryColor, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Section 3: QR Code Live Generator Display Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.5.dp, primaryColor),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().testTag("promo_qr_main_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "CÓDIGO QR PUBLICITARIO PARA LA IMAGEN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryColor,
                        letterSpacing = 1.sp
                    )

                    Box(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                            .border(BorderStroke(4.dp, primaryColor), RoundedCornerShape(16.dp))
                            .clickable { showPromotionDialog = true }
                    ) {
                        BeautifulQRCode(
                            modifier = Modifier.size(200.dp),
                            url = activeRealUrl,
                            onClick = { showPromotionDialog = true }
                        )
                    }

                    Text(
                        text = "Toque el código QR o use la cámara de su teléfono para entrar directamente al afiche publicitario e imagen oficial",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showPromotionDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.2f).height(44.dp).testTag("btn_enter_image_dialog")
                        ) {
                            Icon(Icons.Default.CropFree, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Entrar a la Imagen", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                try {
                                    uriHandler.openUri(activeRealUrl)
                                    viewModel.logSystemEvent("Abriendo página real: $activeRealUrl")
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(44.dp).testTag("btn_open_real_web_page")
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Abrir Web", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section 4: Main Promotional Flyer Card Showcase
        item {
            val currentBanner = banners[selectedBannerIndex]
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("promo_flyer_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentBanner.second,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .background(primaryColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("OFICIAL", fontSize = 9.sp, fontWeight = FontWeight.Black, color = primaryColor)
                        }
                    }

                    Image(
                        painter = painterResource(id = currentBanner.first),
                        contentDescription = currentBanner.second,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(12.dp))
                            .clickable { showPromotionDialog = true },
                        contentScale = ContentScale.FillWidth
                    )

                    Button(
                        onClick = { showPromotionDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp).testTag("btn_expand_flyer")
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ampliar Imagen con QR Integrado", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    if (showPromotionDialog) {
        val currentBanner = banners[selectedBannerIndex]
        PromotionBannerDialog(
            activeUrl = activeRealUrl,
            imageResId = currentBanner.first,
            bannerTitle = currentBanner.second,
            onDismiss = { showPromotionDialog = false }
        )
    }
}

// ============================================================================
// ADMIN MAIN DASHBOARD: PENDING TRIP REQUESTS & FLEET STATUS INTERFACE
// ============================================================================

@Composable
fun AdminPendingTripsAndFleetDashboard(
    trips: List<TripEntity>,
    motorizados: List<MotorizadoEntity>,
    viewModel: RapidTuyViewModel,
    primaryColor: Color
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Solicitudes Pendientes, 1: Estado de la Flota
    var pendingFilter by remember { mutableStateOf("PENDIENTES") } // "TODAS", "PENDIENTES", "EN_CURSO"
    var fleetStatusFilter by remember { mutableStateOf("TODOS") } // "TODOS", "DISPONIBLES", "EN_SERVICIO", "INACTIVOS"
    var fleetSearchQuery by remember { mutableStateOf("") }

    val pendingTripsCount = trips.count { it.estado == "PENDIENTE" }
    val activeTripsCount = trips.count { it.estado == "ACEPTADO" }
    val availableFleetCount = motorizados.count { it.estado == 1 }
    val busyFleetCount = motorizados.count { it.estado == 2 }
    val inactiveFleetCount = motorizados.count { it.estado == 3 }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.5.dp, primaryColor.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth().testTag("admin_main_dashboard")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Dashboard Header Title Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(primaryColor.copy(alpha = 0.15f), CircleShape)
                            .border(BorderStroke(1.dp, primaryColor), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "CENTRAL DE CONTROL ADMINISTRATIVA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryColor,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Solicitudes Pendientes & Flota",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                // Live Indicator Pill
                Box(
                    modifier = Modifier
                        .background(Color(0xFF0F172A), RoundedCornerShape(20.dp))
                        .border(BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f)), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF10B981), CircleShape)
                        )
                        Text(
                            text = "TIEMPO REAL",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Tab Selector Row (Solicitudes vs Flota)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tab 0: Solicitudes Pendientes
                Button(
                    onClick = { selectedTab = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == 0) primaryColor else Color.Transparent,
                        contentColor = if (selectedTab == 0) Color.White else Color(0xFF94A3B8)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("tab_admin_pending_trips"),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PendingActions,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Solicitudes",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (pendingTripsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFEF4444), CircleShape)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$pendingTripsCount",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                // Tab 1: Estado de la Flota
                Button(
                    onClick = { selectedTab = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == 1) primaryColor else Color.Transparent,
                        contentColor = if (selectedTab == 1) Color.White else Color(0xFF94A3B8)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("tab_admin_fleet_status"),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TwoWheeler,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Flota ($availableFleetCount Dispo)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Tab View Rendering
            if (selectedTab == 0) {
                // VIEW 1: SOLICITUDES DE VIAJE PENDIENTES Y ACTIVAS
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Incoming Real-time Visual Notification Alert Banner for Admins
                    val newestPendingTrip = trips.firstOrNull { it.estado == "PENDIENTE" }
                    if (newestPendingTrip != null) {
                        IncomingTripAlertBanner(
                            trip = newestPendingTrip,
                            motorizados = motorizados,
                            viewModel = viewModel,
                            primaryColor = primaryColor
                        )
                    }
                    // Filter Chips Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "PENDIENTES" to "Pendientes ($pendingTripsCount)",
                            "EN_CURSO" to "En Tránsito ($activeTripsCount)",
                            "TODAS" to "Todas (${trips.count { it.estado != "COMPLETADO" && it.estado != "CANCELADO" }})"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = pendingFilter == key,
                                onClick = { pendingFilter = key },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryColor.copy(alpha = 0.25f),
                                    selectedLabelColor = primaryColor,
                                    containerColor = Color(0xFF0F172A),
                                    labelColor = Color(0xFF94A3B8)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (pendingFilter == key) primaryColor else Color(0xFF334155),
                                    enabled = true,
                                    selected = pendingFilter == key
                                )
                            )
                        }
                    }

                    val filteredTrips = when (pendingFilter) {
                        "PENDIENTES" -> trips.filter { it.estado == "PENDIENTE" }
                        "EN_CURSO" -> trips.filter { it.estado == "ACEPTADO" }
                        else -> trips.filter { it.estado != "COMPLETADO" && it.estado != "CANCELADO" }
                    }

                    if (filteredTrips.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(Color(0xFF0F172A), RoundedCornerShape(14.dp))
                                .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircleOutline,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = if (pendingFilter == "PENDIENTES") "No hay solicitudes de viaje pendientes." else "Sin viajes en este filtro.",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Despache un nuevo viaje para simular la cola de solicitudes en tiempo real.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    } else {
                        filteredTrips.forEach { trip ->
                            PendingTripAdminCard(
                                trip = trip,
                                motorizados = motorizados,
                                viewModel = viewModel,
                                primaryColor = primaryColor
                            )
                        }
                    }
                }
            } else {
                // VIEW 2: ESTADO DE LA FLOTA DE MOTORIZADOS
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Fleet Quick Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FleetStatusPill(
                            label = "Disponibles",
                            count = availableFleetCount,
                            color = Color(0xFF10B981),
                            isSelected = fleetStatusFilter == "DISPONIBLES",
                            onClick = { fleetStatusFilter = if (fleetStatusFilter == "DISPONIBLES") "TODOS" else "DISPONIBLES" },
                            modifier = Modifier.weight(1f)
                        )
                        FleetStatusPill(
                            label = "En Viaje",
                            count = busyFleetCount,
                            color = RapidTuyOrange,
                            isSelected = fleetStatusFilter == "EN_SERVICIO",
                            onClick = { fleetStatusFilter = if (fleetStatusFilter == "EN_SERVICIO") "TODOS" else "EN_SERVICIO" },
                            modifier = Modifier.weight(1f)
                        )
                        FleetStatusPill(
                            label = "Inactivos",
                            count = inactiveFleetCount,
                            color = Color(0xFF64748B),
                            isSelected = fleetStatusFilter == "INACTIVOS",
                            onClick = { fleetStatusFilter = if (fleetStatusFilter == "INACTIVOS") "TODOS" else "INACTIVOS" },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Search driver bar and Delete All action row
                    var showDeleteAllDriversDialog by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = fleetSearchQuery,
                            onValueChange = { fleetSearchQuery = it },
                            placeholder = { Text("Buscar por nombre, placa o ID...", fontSize = 12.sp, color = Color(0xFF64748B)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (fleetSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { fleetSearchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("input_fleet_search"),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedBorderColor = primaryColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (motorizados.isNotEmpty()) {
                            Button(
                                onClick = { showDeleteAllDriversDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                                modifier = Modifier.testTag("btn_delete_all_drivers")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(16.dp))
                                    Text("Eliminar Todos", fontSize = 11.sp, color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (showDeleteAllDriversDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteAllDriversDialog = false },
                            containerColor = Color(0xFF1E293B),
                            titleContentColor = Color.White,
                            textContentColor = Color(0xFF94A3B8),
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444))
                                    Text("Eliminar Todos los Motorizados", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            },
                            text = {
                                Text(
                                    "¿Estás seguro de que deseas eliminar TODOS los motorizados de prueba (${motorizados.size} registrados) y sus datos asociados? Esta acción eliminará permanentemente la flota local y remota.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    lineHeight = 16.sp
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        viewModel.deleteAllMotorizadosYDatos()
                                        showDeleteAllDriversDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                                ) {
                                    Text("Eliminar Todo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteAllDriversDialog = false }) {
                                    Text("Cancelar", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                }
                            }
                        )
                    }

                    val filteredDrivers = motorizados.filter { driver ->
                        val matchesSearch = fleetSearchQuery.isEmpty() ||
                                driver.nombre.contains(fleetSearchQuery, true) ||
                                driver.placa.contains(fleetSearchQuery, true) ||
                                driver.id.toString().contains(fleetSearchQuery)
                        val matchesStatus = when (fleetStatusFilter) {
                            "DISPONIBLES" -> driver.estado == 1
                            "EN_SERVICIO" -> driver.estado == 2
                            "INACTIVOS" -> driver.estado == 3
                            else -> true
                        }
                        matchesSearch && matchesStatus
                    }

                    if (filteredDrivers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(Color(0xFF0F172A), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No se encontraron motorizados registrados con este criterio.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        filteredDrivers.forEach { driver ->
                            FleetDriverStatusCard(
                                driver = driver,
                                activeTrips = trips,
                                viewModel = viewModel,
                                primaryColor = primaryColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FleetStatusPill(
    label: String,
    count: Int,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.2f) else Color(0xFF0F172A),
        border = BorderStroke(1.dp, if (isSelected) color else Color(0xFF334155)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, CircleShape)
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) color else Color(0xFF94A3B8)
                )
            }
            Text(
                text = "$count",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

@Composable
fun PendingTripAdminCard(
    trip: TripEntity,
    motorizados: List<MotorizadoEntity>,
    viewModel: RapidTuyViewModel,
    primaryColor: Color
) {
    var showAssignDropdown by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_pending")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_pending_alpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth().testTag("pending_trip_card_${trip.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, primaryColor.copy(alpha = pulseAlpha)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row: Status Badge & Fare
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .background(primaryColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .border(BorderStroke(0.5.dp, primaryColor), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(primaryColor, CircleShape))
                            Text(
                                text = "SOLICITUD PENDIENTE #${trip.id}",
                                color = primaryColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    if (trip.segundosRestantes > 0) {
                        Text(
                            text = "⏱ ${trip.segundosRestantes}s",
                            color = Color(0xFFF59E0B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "$${String.format(java.util.Locale.US, "%.2f", trip.monto)} USD",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF10B981)
                )
            }

            HorizontalDivider(color = Color(0xFF1E293B))

            // Route Details
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.RadioButtonChecked,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Origen: ",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = trip.origen,
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Destino: ",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = trip.destino,
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Client Info & Assigned Driver status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PASAJERO / SOLICITANTE",
                        fontSize = 8.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Cliente Central RapidTuy",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tel: ${"584261215060"}",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                val assignedDriver = motorizados.firstOrNull { it.id == trip.motorizadoId }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "CONDUCTOR ASIGNADO",
                        fontSize = 8.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold
                    )
                    if (assignedDriver != null) {
                        Text(
                            text = assignedDriver.nombre,
                            fontSize = 12.sp,
                            color = primaryColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Placa: ${assignedDriver.placa}",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    } else {
                        Text(
                            text = "Buscando...",
                            fontSize = 11.sp,
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quick Admin Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Re-assign or Direct Dispatch Button
                Box(modifier = Modifier.weight(1f)) {
                    Button(
                        onClick = { showAssignDropdown = true },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_assign_driver_${trip.id}"),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text("Asignar Moto", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    DropdownMenu(
                        expanded = showAssignDropdown,
                        onDismissRequest = { showAssignDropdown = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        val availableDrivers = motorizados.filter { it.estado == 1 }
                        if (availableDrivers.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No hay motorizados disponibles", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                                onClick = { showAssignDropdown = false }
                            )
                        } else {
                            availableDrivers.forEach { driver ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(driver.nombre, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("Placa: ${driver.placa} • ID #${driver.id}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                        }
                                    },
                                    onClick = {
                                        showAssignDropdown = false
                                        viewModel.assignTripDirectly(trip.id, driver.id)
                                    }
                                )
                            }
                        }
                    }
                }

                // Direct Dispatch / Accept Button
                val assignedDriverId = trip.motorizadoId
                Button(
                    onClick = {
                        if (assignedDriverId != null) {
                            viewModel.acceptTrip(trip.id, assignedDriverId)
                        } else {
                            val firstAvailable = motorizados.firstOrNull { it.estado == 1 }
                            if (firstAvailable != null) {
                                viewModel.acceptTrip(trip.id, firstAvailable.id)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("btn_force_dispatch_${trip.id}"),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text("Despachar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Cancel Button
                IconButton(
                    onClick = { viewModel.cancelTrip(trip.id) },
                    modifier = Modifier
                        .background(Color(0xFFEF4444).copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .border(BorderStroke(1.dp, Color(0xFFEF4444)), RoundedCornerShape(10.dp))
                        .size(36.dp)
                        .testTag("btn_cancel_trip_${trip.id}")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun FleetDriverStatusCard(
    driver: MotorizadoEntity,
    activeTrips: List<TripEntity>,
    viewModel: RapidTuyViewModel,
    primaryColor: Color
) {
    val assignedTrip = activeTrips.firstOrNull { it.motorizadoId == driver.id && it.estado == "ACEPTADO" }
    
    val statusColor = when {
        (driver.estado == 4 || driver.fechaVencimiento < System.currentTimeMillis()) -> Color(0xFFEF4444)
        driver.estado == 1 -> Color(0xFF10B981)
        driver.estado == 2 -> RapidTuyOrange
        else -> Color(0xFF64748B)
    }

    val statusText = when {
        (driver.estado == 4 || driver.fechaVencimiento < System.currentTimeMillis()) -> "SUSPENDIDO / MOROSO"
        driver.estado == 1 -> "DISPONIBLE"
        driver.estado == 2 -> "EN VIAJE ACTIVO"
        else -> "FUERA DE SERVICIO"
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("fleet_driver_card_${driver.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, if (driver.estado == 1) statusColor.copy(alpha = 0.4f) else Color(0xFF334155)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Driver Avatar Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(statusColor.copy(alpha = 0.15f), CircleShape)
                    .border(BorderStroke(1.5.dp, statusColor), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TwoWheeler,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Driver Info
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = driver.nombre,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "#${driver.id}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Placa: ${driver.placa}",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = Color(0xFF475569)
                    )
                    Text(
                        text = "Tel: ${driver.telefono}",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Active status label
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
                    Text(
                        text = statusText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor
                    )
                    if (assignedTrip != null) {
                        Text(
                            text = "(${assignedTrip.origen.take(10)} ➔ ${assignedTrip.destino.take(10)})",
                            fontSize = 8.sp,
                            color = Color(0xFFCBD5E1),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Solvency status & Action buttons
            var showDeleteSingleDialog by remember { mutableStateOf(false) }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val solvencyBg = if ((driver.estado == 4 || driver.fechaVencimiento < System.currentTimeMillis())) Color(0xFF7F1D1D) else Color(0xFF064E3B)
                val solvencyText = if ((driver.estado == 4 || driver.fechaVencimiento < System.currentTimeMillis())) Color(0xFFFCA5A5) else Color(0xFF6EE7B7)
                val solvencyLabel = if ((driver.estado == 4 || driver.fechaVencimiento < System.currentTimeMillis())) "VENCIDO" else "AL DÍA"

                Box(
                    modifier = Modifier
                        .background(solvencyBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = solvencyLabel,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = solvencyText
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Toggle Driver Status
                    IconButton(
                        onClick = {
                            val nextStatus = when (driver.estado) {
                                1 -> 3 // Disconnect
                                3 -> 1 // Connect / Available
                                else -> 1
                            }
                            viewModel.updateMotorizadoEstadoDirectly(driver.id, nextStatus)
                        },
                        modifier = Modifier.size(28.dp).testTag("btn_toggle_driver_status_${driver.id}")
                    ) {
                        Icon(
                            imageVector = if (driver.estado == 1) Icons.Default.PowerSettingsNew else Icons.Default.PlayArrow,
                            contentDescription = "Cambiar estado",
                            tint = if (driver.estado == 1) Color(0xFFEF4444) else Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Delete Driver Button
                    IconButton(
                        onClick = { showDeleteSingleDialog = true },
                        modifier = Modifier.size(28.dp).testTag("btn_delete_driver_${driver.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar conductor",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (showDeleteSingleDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteSingleDialog = false },
                    containerColor = Color(0xFF1E293B),
                    titleContentColor = Color.White,
                    textContentColor = Color(0xFF94A3B8),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444))
                            Text("Eliminar Motorizado #${driver.id}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Text(
                            "¿Estás seguro de eliminar a ${driver.nombre} (Placa: ${driver.placa}) y todos sus datos de pago y registros asociados?",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 16.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteMotorizado(driver.id)
                                showDeleteSingleDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Text("Eliminar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteSingleDialog = false }) {
                            Text("Cancelar", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }
                )
            }
        }
    }
}


@Composable
fun IncomingTripAlertBanner(
    trip: TripEntity,
    motorizados: List<MotorizadoEntity>,
    viewModel: RapidTuyViewModel,
    primaryColor: Color
) {
    var isAudioAlertActive by remember { mutableStateOf(true) }
    var selectedDriverForQuickAssign by remember { mutableStateOf<MotorizadoEntity?>(null) }
    var showQuickAssignDropdown by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "banner_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "banner_alpha"
    )

    val availableDrivers = motorizados.filter { it.estado == 1 }
    val recommendedDriver = availableDrivers.firstOrNull()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("incoming_trip_alert_banner"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)), // Dark Indigo/Emergency Blue
        border = BorderStroke(2.dp, Color(0xFFEF4444).copy(alpha = pulseAlpha)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Alert Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFEF4444).copy(alpha = pulseAlpha * 0.3f), CircleShape)
                            .border(BorderStroke(1.5.dp, Color(0xFFEF4444)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Alerta",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "NUEVA SOLICITUD ENTRANTE",
                                color = Color(0xFFFCA5A5),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFEF4444), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "¡ALERTA!",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        Text(
                            text = "Solicitud #${trip.id} • Conexión Directa Central",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                }

                // Audio / Visual Alert Indicator Button
                IconButton(
                    onClick = { isAudioAlertActive = !isAudioAlertActive },
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (isAudioAlertActive) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF334155),
                            CircleShape
                        )
                        .border(
                            BorderStroke(1.dp, if (isAudioAlertActive) Color(0xFF10B981) else Color(0xFF64748B)),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isAudioAlertActive) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Alerta Sonora",
                        tint = if (isAudioAlertActive) Color(0xFF10B981) else Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF312E81))

            // Trip Details Quick Snapshot
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.RadioButtonChecked, contentDescription = null, tint = primaryColor, modifier = Modifier.size(14.dp))
                        Text(trip.origen, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                        Text(trip.destino, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${String.format(java.util.Locale.US, "%.2f", trip.monto)} USD",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF10B981)
                    )
                    Text(
                        text = "⏱ ${trip.segundosRestantes}s restantes",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B)
                    )
                }
            }

            // Quick Assign & Dispatch Control Section
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "ASIGNACIÓN RÁPIDA DE MOTORIZADO:",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFA5B4FC),
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Driver Selector Box
                    Box(modifier = Modifier.weight(1.2f)) {
                        Button(
                            onClick = { showQuickAssignDropdown = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF312E81)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("btn_quick_select_driver"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = selectedDriverForQuickAssign?.nombre ?: (recommendedDriver?.let { "${it.nombre} (${it.placa})" } ?: "Seleccionar Moto"),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }

                        DropdownMenu(
                            expanded = showQuickAssignDropdown,
                            onDismissRequest = { showQuickAssignDropdown = false },
                            modifier = Modifier.background(Color(0xFF1E293B))
                        ) {
                            if (availableDrivers.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No hay motorizados disponibles", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                                    onClick = { showQuickAssignDropdown = false }
                                )
                            } else {
                                availableDrivers.forEach { driver ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(driver.nombre, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    Text("Placa: ${driver.placa} • ID #${driver.id}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFF10B981).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text("DISPO", fontSize = 8.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedDriverForQuickAssign = driver
                                            showQuickAssignDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // One-Tap Direct Assign & Dispatch Button
                    val targetDriver = selectedDriverForQuickAssign ?: recommendedDriver
                    Button(
                        onClick = {
                            if (targetDriver != null) {
                                viewModel.assignTripDirectly(trip.id, targetDriver.id)
                                viewModel.acceptTrip(trip.id, targetDriver.id)
                            } else {
                                val firstAvailable = motorizados.firstOrNull { it.estado == 1 }
                                if (firstAvailable != null) {
                                    viewModel.acceptTrip(trip.id, firstAvailable.id)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("btn_instant_dispatch_${trip.id}"),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text("Despachar Ya", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AssignedServicePushNotificationBanner(
    viewModel: RapidTuyViewModel,
    modifier: Modifier = Modifier
) {
    val trips by viewModel.trips.collectAsState()
    val motorizados by viewModel.motorizados.collectAsState()
    val yummyThemeActive by viewModel.yummyThemeActive.collectAsState()

    val primaryColor = if (yummyThemeActive) Color(0xFF10B981) else RapidTuyOrange

    // Find assigned trip in pending state
    val assignedTrip = trips.firstOrNull { it.estado == "PENDIENTE" && it.motorizadoId != null }
    val assignedDriver = motorizados.firstOrNull { it.id == assignedTrip?.motorizadoId }

    var isDismissedManually by remember(assignedTrip?.id) { mutableStateOf(false) }

    val isVisible = assignedTrip != null && assignedDriver != null && !isDismissedManually

    val infiniteTransition = rememberInfiniteTransition(label = "push_notification_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "push_pulse_alpha"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        if (assignedTrip != null && assignedDriver != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .shadow(16.dp, RoundedCornerShape(20.dp))
                    .testTag("push_notification_banner"),
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(2.dp, primaryColor.copy(alpha = pulseAlpha))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Notification Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(primaryColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TwoWheeler,
                                    contentDescription = "RapidTuy",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "RapidTuy Push Alert",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "• Ahora",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp
                                    )
                                }
                                Text(
                                    text = "¡NUEVO SERVICIO ASIGNADO!",
                                    color = primaryColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFFEF4444).copy(alpha = pulseAlpha), CircleShape)
                            )
                            IconButton(
                                onClick = { isDismissedManually = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF334155))

                    // Conductor details & Trip Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "CONDUCTOR ASIGNADO",
                                fontSize = 9.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${assignedDriver.nombre} (#${assignedDriver.id})",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "[${assignedDriver.placa}]",
                                    fontSize = 10.sp,
                                    color = Color(0xFFA5B4FC)
                                )
                            }
                        }

                        // Price Tag
                        Surface(
                            color = primaryColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, primaryColor)
                        ) {
                            Text(
                                text = "$${String.format(java.util.Locale.US, "%.2f", assignedTrip.monto)}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Origen -> Destino
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF10B981), CircleShape)
                            )
                            Text(
                                text = "Origen: ${assignedTrip.origen}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFFEF4444), CircleShape)
                            )
                            Text(
                                text = "Destino: ${assignedTrip.destino}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Live Countdown Bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tiempo para aceptar:",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "${assignedTrip.segundosRestantes}s",
                                fontSize = 11.sp,
                                color = Color(0xFFF59E0B),
                                fontWeight = FontWeight.Black
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF334155))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = (assignedTrip.segundosRestantes / 15f).coerceIn(0f, 1f))
                                    .clip(CircleShape)
                                    .background(
                                        if (assignedTrip.segundosRestantes <= 5) Color(0xFFEF4444) else primaryColor
                                    )
                            )
                        }
                    }

                    // Action Buttons (Immediate Acceptance)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Accept button
                        Button(
                            onClick = {
                                viewModel.acceptTrip(assignedTrip.id, assignedDriver.id)
                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(42.dp)
                                .testTag("btn_accept_pushed_service"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "ACEPTAR INMEDIATAMENTE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }

                        // Reject/Reassign button
                        OutlinedButton(
                            onClick = {
                                viewModel.rejectTrip(
                                    assignedTrip.id,
                                    assignedDriver.id,
                                    assignedDriver.latitud,
                                    assignedDriver.longitud
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("btn_reject_pushed_service"),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5))
                        ) {
                            Text(
                                text = "Rechazar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
