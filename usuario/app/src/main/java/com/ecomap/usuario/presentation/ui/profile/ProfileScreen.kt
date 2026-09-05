package com.ecomap.usuario.presentation.ui.profile

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ecomap.usuario.utils.ImageConfig
import com.ecomap.usuario.presentation.viewmodel.AuthViewModel
import com.ecomap.usuario.ui.theme.AppleColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWelcome: () -> Unit,
    onNavigateToHelp: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToMyComplaints: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showForgetDeviceDialog by remember { mutableStateOf(false) }
    val hasBiometricCredentials by remember {
        mutableStateOf(viewModel.isBiometricEnabled())
    }

    val userDataViewModel: com.ecomap.usuario.presentation.viewmodel.UserDataViewModel = hiltViewModel()
    val favorites by userDataViewModel.favoritesState.collectAsState()
    val history by userDataViewModel.historyState.collectAsState()
    val userPreferences by userDataViewModel.userPreferences.collectAsState()

    // 🔄 CRITICAL: Recargar preferencias al entrar a la pantalla
    LaunchedEffect(Unit) {
        userDataViewModel.loadUserPreferences()
    }

    Scaffold(
        containerColor = Color(0xFFFAFAFA),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Mi Perfil",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = Color(0xFF1F1F1F)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color(0xFF1F1F1F)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            tint = Color(0xFFE53935)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header con avatar mejorado
            item {
                ModernProfileHeader(
                    userName = userPreferences?.displayName ?: currentUser?.email?.substringBefore("@") ?: "Usuario",
                    userEmail = currentUser?.email ?: "",
                    avatarUrl = userPreferences?.avatarUrl,
                    onEditClick = onNavigateToEditProfile
                )
            }

            // Estadísticas modernas
            item {
                ModernStatsSection(
                    favoritesCount = favorites.size,
                    historyCount = history.size
                )
            }

            // Sección de cuenta
            item {
                SectionHeader("CUENTA")
            }

            item {
                ModernMenuItem(
                    icon = Icons.Default.Person,
                    title = "Editar perfil",
                    subtitle = "Actualiza tu información",
                    iconColor = Color(0xFF2196F3),
                    onClick = onNavigateToEditProfile
                )
            }

            item {
                ModernMenuItem(
                    icon = Icons.Default.Favorite,
                    title = "Mis favoritos",
                    subtitle = "Negocios que te gustan",
                    iconColor = Color(0xFFE91E63),
                    onClick = onNavigateToFavorites
                )
            }

            item {
                ModernMenuItem(
                    icon = Icons.Default.History,
                    title = "Historial",
                    subtitle = "Visitados recientemente",
                    iconColor = Color(0xFF9C27B0),
                    onClick = onNavigateToHistory
                )
            }

            item {
                ModernMenuItem(
                    icon = Icons.Default.Report,
                    title = "Mis Reportes",
                    subtitle = "Ver mis quejas de productos",
                    iconColor = Color(0xFFFF9800),
                    onClick = onNavigateToMyComplaints
                )
            }

            if (hasBiometricCredentials) {
                item {
                    ModernMenuItem(
                        icon = Icons.Default.Fingerprint,
                        title = "Olvidar dispositivo",
                        subtitle = "Eliminar credenciales",
                        iconColor = Color(0xFFE53935),
                        onClick = { showForgetDeviceDialog = true }
                    )
                }
            }

            // Sección de preferencias
            item {
                SectionHeader("PREFERENCIAS")
            }

            item {
                ModernMenuItem(
                    icon = Icons.Default.Notifications,
                    title = "Notificaciones",
                    subtitle = "Gestiona alertas",
                    iconColor = Color(0xFFFF9800),
                    onClick = onNavigateToNotifications
                )
            }

            item {
                ModernMenuItem(
                    icon = Icons.Default.LocationOn,
                    title = "Ubicación",
                    subtitle = "Configurar ubicación",
                    iconColor = Color(0xFF4CAF50),
                    onClick = { /* TODO */ }
                )
            }

            // Sección de soporte
            item {
                SectionHeader("SOPORTE")
            }

            item {
                ModernMenuItem(
                    icon = Icons.Default.Help,
                    title = "Ayuda",
                    subtitle = "Preguntas frecuentes",
                    iconColor = Color(0xFF2196F3),
                    onClick = onNavigateToHelp
                )
            }

            item {
                ModernMenuItem(
                    icon = Icons.Default.Info,
                    title = "Acerca de",
                    subtitle = "Versión 1.0.0",
                    iconColor = Color(0xFF666666),
                    onClick = onNavigateToAbout
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Dialogs modernos
    if (showLogoutDialog) {
        ModernLogoutDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                viewModel.signOut()
                showLogoutDialog = false
                onNavigateToWelcome()
            }
        )
    }

    if (showForgetDeviceDialog) {
        ModernForgetDeviceDialog(
            onDismiss = { showForgetDeviceDialog = false },
            onConfirm = {
                viewModel.clearBiometricCredentials()
                showForgetDeviceDialog = false
            }
        )
    }
}

@Composable
private fun ModernProfileHeader(
    userName: String,
    userEmail: String,
    avatarUrl: String?,
    onEditClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar mejorado con refresco automático
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
            ) {
                // 🔄 Key para forzar recomposición cuando cambia avatarUrl
                key(avatarUrl) {
                    if (!avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            imageLoader = ImageConfig.getImageLoader(context),
                            contentDescription = "Foto de perfil",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF667EEA),
                                            Color(0xFF764BA2)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userName.firstOrNull()?.uppercase() ?: "U",
                                fontSize = 36.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Nombre
            Text(
                text = userName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F1F1F)
            )

            // Email
            Text(
                text = userEmail,
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )

            // Botón editar
            OutlinedButton(
                onClick = onEditClick,
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.5.dp
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF1F1F1F)
                )
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Editar perfil",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ModernStatsSection(
    favoritesCount: Int,
    historyCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ModernStatItem(
                value = favoritesCount.toString(),
                label = "Favoritos",
                color = Color(0xFFE91E63)
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(50.dp)
                    .background(Color(0xFFF0F0F0))
            )

            ModernStatItem(
                value = historyCount.toString(),
                label = "Visitados",
                color = Color(0xFF2196F3)
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(50.dp)
                    .background(Color(0xFFF0F0F0))
            )

            ModernStatItem(
                value = "0",
                label = "Reseñas",
                color = Color(0xFFFF9800)
            )
        }
    }
}

@Composable
private fun ModernStatItem(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF999999)
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF999999),
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}

@Composable
private fun ModernMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1F1F1F)
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFF999999)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF999999),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ModernLogoutDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFEBEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                "Cerrar sesión",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF1F1F1F),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                "¿Estás seguro de que deseas cerrar sesión?",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Cerrar sesión",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Cancelar",
                    color = Color(0xFF666666),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

@Composable
private fun ModernForgetDeviceDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFEBEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                "Olvidar este dispositivo",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF1F1F1F),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                "Se eliminarán las credenciales guardadas y tendrás que iniciar sesión manualmente la próxima vez.\n\n¿Deseas continuar?",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Olvidar",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Cancelar",
                    color = Color(0xFF666666),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}