package com.ecomap.socio.presentation.ui.main

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
import com.ecomap.socio.data.model.Business
import com.ecomap.socio.presentation.ui.onboarding.businessCategories
import com.ecomap.socio.presentation.viewmodel.BusinessViewModel
import com.ecomap.socio.ui.theme.NuColors
import com.ecomap.socio.utils.ImageConfig
import com.ecomap.socio.utils.UiState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onNavigateToOnboarding: (String) -> Unit = {},
    onNavigateToDashboard: (String) -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    viewModel: BusinessViewModel = hiltViewModel()
) {
    val businesses by viewModel.businesses.collectAsState()
    val businessesState by viewModel.businessesState.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val isProUser by viewModel.isProUser.collectAsState()
    val isProStatusChecked by viewModel.isProStatusChecked.collectAsState()
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }

    val filteredBusinesses = remember(businesses, searchQuery) {
        if (searchQuery.isBlank()) {
            businesses
        } else {
            businesses.filter { business ->
                business.businessName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    var showUpgradeDialog by remember { mutableStateOf(false) }
    var showApprovalDialog by remember { mutableStateOf(false) }
    var showPendingDialog by remember { mutableStateOf(false) }
    var showRejectedDialog by remember { mutableStateOf(false) }
    var showDisabledDialog by remember { mutableStateOf(false) }
    var showBlockedBusinessDialog by remember { mutableStateOf(false) }
    var showManageBusinessSheet by remember { mutableStateOf(false) }
    var approvedBusiness by remember { mutableStateOf<Business?>(null) }
    var rejectedBusinessName by remember { mutableStateOf("") }
    var showTopMenu by remember { mutableStateOf(false) }
    var showBusinessPhotoDialog by remember { mutableStateOf(false) }
    var selectedBusinessForPhoto by remember { mutableStateOf<Business?>(null) }

    // Launchers para foto del negocio
    val cameraLauncherBusiness = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val file = File(context.cacheDir, "business_camera_photo.jpg")
            if (file.exists() && selectedBusinessForPhoto != null) {
                viewModel.uploadBusinessAvatar(file, selectedBusinessForPhoto!!.id)
            }
        }
    }

    val cameraPermissionLauncherBusiness = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val photoFile = File(context.cacheDir, "business_camera_photo.jpg")
            val photoUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                photoFile
            )
            cameraLauncherBusiness.launch(photoUri)
        } else {
            android.widget.Toast.makeText(
                context,
                "Permiso de cámara denegado",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    val galleryLauncherBusiness = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val file = File(context.cacheDir, "business_avatar_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            if (selectedBusinessForPhoto != null) {
                viewModel.uploadBusinessAvatar(file, selectedBusinessForPhoto!!.id)
            }
        }
    }

    val shownApprovalModals = remember { mutableSetOf<String>() }

    LaunchedEffect(businesses) {
        if (!showApprovalDialog) {
            val recentlyApproved = businesses.firstOrNull { business ->
                business.verificationStatus == "approved" &&
                        business.approvedAt != null &&
                        !business.approvalSeen &&
                        !shownApprovalModals.contains(business.id)
            }
            if (recentlyApproved != null) {
                approvedBusiness = recentlyApproved
                showApprovalDialog = true
                shownApprovalModals.add(recentlyApproved.id)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadAllUserBusinesses()
        viewModel.checkProStatus()
        viewModel.subscribeToBusinessChanges()
    }

    // Dialogs modernos
    if (showUpgradeDialog) {
        ModernUpgradeDialog(
            onDismiss = { showUpgradeDialog = false },
            onUpgrade = {
                showUpgradeDialog = false
                onNavigateToSubscription()
            }
        )
    }

    if (showApprovalDialog && approvedBusiness != null) {
        ModernApprovalDialog(
            businessName = approvedBusiness!!.businessName,
            onDismiss = {
                val businessId = approvedBusiness!!.id
                shownApprovalModals.add(businessId)
                showApprovalDialog = false
                approvedBusiness = null
                viewModel.markApprovalAsSeen(businessId)
            }
        )
    }

    if (showPendingDialog) {
        ModernPendingDialog(
            onDismiss = { showPendingDialog = false }
        )
    }

    if (showRejectedDialog) {
        ModernRejectedDialog(
            businessName = rejectedBusinessName,
            onDismiss = { showRejectedDialog = false }
        )
    }

    if (showDisabledDialog) {
        ModernDisabledDialog(
            onDismiss = { showDisabledDialog = false },
            onManage = {
                showDisabledDialog = false
                showManageBusinessSheet = true
            }
        )
    }

    if (showBlockedBusinessDialog) {
        ModernBlockedBusinessDialog(
            onDismiss = { showBlockedBusinessDialog = false },
            onUpgrade = {
                showBlockedBusinessDialog = false
                onNavigateToSubscription()
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color(0xFFFAFAFA),
        topBar = {
            ModernTopBar(
                searchQuery = searchQuery,
                isFocused = isFocused,
                onSearchQueryChange = { searchQuery = it },
                onFocusChange = { isFocused = it },
                onClearSearch = {
                    searchQuery = ""
                    keyboardController?.hide()
                    focusManager.clearFocus()
                },
                onMenuClick = onOpenDrawer,
                onMoreClick = { showTopMenu = true },
                showTopMenu = showTopMenu,
                onDismissMenu = { showTopMenu = false },
                onManageClick = {
                    showTopMenu = false
                    showManageBusinessSheet = true
                }
            )
        },
        floatingActionButton = {
            ModernFAB(
                businesses = businesses,
                isProUser = isProUser,
                isProStatusChecked = isProStatusChecked,
                currentUserId = currentUserId,
                onAddBusiness = { onNavigateToOnboarding(currentUserId!!) },
                onShowUpgrade = { showUpgradeDialog = true }
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (businessesState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF1F1F1F),
                            strokeWidth = 2.dp
                        )
                    }
                }

                is UiState.Error -> {
                    ModernErrorState(
                        message = (businessesState as UiState.Error).message
                    )
                }

                is UiState.Success, UiState.Idle -> {
                    if (businesses.isEmpty()) {
                        ModernEmptyState()
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .imePadding(),
                            // El fondo de la lista deja aire para la barra de navegación
                            // y para que el FAB no tape la última tarjeta al hacer scroll.
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                top = 16.dp,
                                bottom = 16.dp + 88.dp +
                                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (filteredBusinesses.isEmpty() && searchQuery.isNotEmpty()) {
                                item {
                                    ModernNoResultsState()
                                }
                            }

                            items(filteredBusinesses) { business ->
                                ModernBusinessCard(
                                    business = business,
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        when {
                                            business.verificationStatus == "approved" && !business.isActive && !isProUser -> {
                                                // Negocio bloqueado por plan FREE
                                                showBlockedBusinessDialog = true
                                            }
                                            business.verificationStatus == "approved" && !business.isActive && isProUser -> {
                                                // Negocio deshabilitado manualmente
                                                showDisabledDialog = true
                                            }
                                            business.verificationStatus == "approved" && business.isActive -> {
                                                onNavigateToDashboard(business.id)
                                            }
                                            business.verificationStatus == "pending" -> {
                                                showPendingDialog = true
                                            }
                                            business.verificationStatus == "rejected" -> {
                                                showRejectedDialog = true
                                                rejectedBusinessName = business.businessName
                                            }
                                        }
                                    },
                                    onChangePhoto = {
                                        selectedBusinessForPhoto = business
                                        showBusinessPhotoDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showManageBusinessSheet) {
        ManageBusinessBottomSheet(
            businesses = businesses,
            onDeactivateBusiness = { businessId ->
                viewModel.deactivateBusiness(businessId)
            },
            onReactivateBusiness = { businessId ->
                viewModel.reactivateBusiness(businessId)
            },
            onDismiss = { showManageBusinessSheet = false }
        )
    }

    if (showBusinessPhotoDialog && selectedBusinessForPhoto != null) {
        ModernPhotoDialog(
            businessName = selectedBusinessForPhoto?.businessName ?: "",
            onDismiss = { showBusinessPhotoDialog = false },
            onCamera = {
                showBusinessPhotoDialog = false
                cameraPermissionLauncherBusiness.launch(android.Manifest.permission.CAMERA)
            },
            onGallery = {
                showBusinessPhotoDialog = false
                galleryLauncherBusiness.launch("image/*")
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernTopBar(
    searchQuery: String,
    isFocused: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onClearSearch: () -> Unit,
    onMenuClick: () -> Unit,
    onMoreClick: () -> Unit,
    showTopMenu: Boolean,
    onDismissMenu: () -> Unit,
    onManageClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .background(Color.White)
            .shadow(2.dp)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Mis Negocios",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F1F1F)
                )
            },
            navigationIcon = {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Menú",
                        tint = Color(0xFF1F1F1F)
                    )
                }
            },
            actions = {
                Box {
                    IconButton(onClick = onMoreClick) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Más opciones",
                            tint = Color(0xFF1F1F1F)
                        )
                    }

                    DropdownMenu(
                        expanded = showTopMenu,
                        onDismissRequest = onDismissMenu
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = Color(0xFFFFFFFF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        "Gestionar negocios",
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFFFFFFF)
                                    )
                                }
                            },
                            onClick = onManageClick
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )

        // Buscador mejorado
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F5F5)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        onFocusChange(focusState.isFocused)
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 15.sp,
                    color = Color(0xFF1F1F1F)
                ),
                cursorBrush = SolidColor(Color(0xFF1F1F1F)),
                decorationBox = { innerTextField ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF999999),
                            modifier = Modifier.size(20.dp)
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "Buscar negocios...",
                                    fontSize = 15.sp,
                                    color = Color(0xFF999999)
                                )
                            }
                            innerTextField()
                        }

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = onClearSearch,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Limpiar",
                                    tint = Color(0xFF999999),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ModernBusinessCard(
    business: Business,
    onClick: () -> Unit,
    onChangePhoto: () -> Unit
) {
    val context = LocalContext.current
    val category = businessCategories.find { it.id == business.businessType }
    val icon = category?.icon ?: Icons.Default.Store

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar con key para actualización
            Box(
                modifier = Modifier.size(64.dp)
            ) {
                key(business.avatarUrl) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .clickable { onChangePhoto() }
                            .background(Color(0xFFF5F5F5)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!business.avatarUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = business.avatarUrl,
                                imageLoader = ImageConfig.getImageLoader(context),
                                contentDescription = "Foto de ${business.businessName}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color(0xFF666666),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Badge de cámara
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F1F1F))
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Cambiar foto",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = business.businessName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F1F1F),
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Badge de estado moderno
                    ModernStatusBadge(
                        verificationStatus = business.verificationStatus,
                        isActive = business.isActive
                    )
                }

                Text(
                    text = business.address,
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
private fun ModernStatusBadge(
    verificationStatus: String,
    isActive: Boolean
) {
    val (icon, text, backgroundColor, textColor) = when {
        verificationStatus == "approved" && !isActive -> {
            Quadruple(
                Icons.Default.Block,
                "Deshabilitado",
                Color(0xFFFFEBEE),
                Color(0xFFE53935)
            )
        }
        verificationStatus == "approved" && isActive -> {
            Quadruple(
                Icons.Default.CheckCircle,
                "Activo",
                Color(0xFFE8F5E9),
                Color(0xFF4CAF50)
            )
        }
        verificationStatus == "pending" -> {
            Quadruple(
                Icons.Default.Schedule,
                "Pendiente",
                Color(0xFFFFF9E6),
                Color(0xFFFFA000)
            )
        }
        else -> {
            Quadruple(
                Icons.Default.Cancel,
                "Rechazado",
                Color(0xFFFFEBEE),
                Color(0xFFE53935)
            )
        }
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = text,
                fontSize = 11.sp,
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ModernFAB(
    businesses: List<Business>,
    isProUser: Boolean,
    isProStatusChecked: Boolean,
    currentUserId: String?,
    onAddBusiness: () -> Unit,
    onShowUpgrade: () -> Unit
) {
    val hasBusiness = businesses.isNotEmpty()

    FloatingActionButton(
        onClick = {
            if (!isProStatusChecked) return@FloatingActionButton

            if (!isProUser && hasBusiness) {
                onShowUpgrade()
            } else {
                if (currentUserId != null) {
                    onAddBusiness()
                }
            }
        },
        // El Scaffold usa contentWindowInsets = WindowInsets(0,0,0,0), así que sin
        // esto el FAB queda tapado por la barra de navegación del sistema.
        modifier = Modifier
            .navigationBarsPadding()
            .padding(bottom = 8.dp),
        containerColor = Color(0xFF1F1F1F),
        contentColor = Color.White,
        shape = RoundedCornerShape(18.dp),
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        )
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Agregar negocio",
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun ModernEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Store,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = Color(0xFFCCCCCC)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No tienes negocios",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F1F1F)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Toca el botón + para agregar\ntu primer negocio",
            fontSize = 15.sp,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun ModernNoResultsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFCCCCCC)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No se encontraron resultados",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1F1F1F)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Intenta con otro término",
            fontSize = 14.sp,
            color = Color(0xFF999999)
        )
    }
}

@Composable
private fun ModernErrorState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFE53935)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Error al cargar",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F1F1F)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center
        )
    }
}

// Dialogs modernos
@Composable
private fun ModernUpgradeDialog(
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit
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
                    .background(Color(0xFFFFF9E6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFA000),
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = "Hazte PRO",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F1F1F),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "Con la versión PRO puedes agregar negocios ilimitados, acceder a estadísticas avanzadas y mucho más.",
                fontSize = 15.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onUpgrade,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1F1F1F)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Ver Planes", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancelar", fontSize = 15.sp, color = Color(0xFF666666))
            }
        }
    )
}

@Composable
private fun ModernApprovalDialog(
    businessName: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(48.dp)
                )
            }
        },
        title = {
            Text(
                text = "¡Felicidades!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F1F1F),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = businessName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4CAF50),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Tu negocio ha sido aprobado y ya está visible para tus clientes.",
                    fontSize = 15.sp,
                    color = Color(0xFF1F1F1F),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Text(
                    text = "¡Comienza a publicar tus productos!",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("¡Comenzar!", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun ModernPendingDialog(
    onDismiss: () -> Unit
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
                    .background(Color(0xFFFFF9E6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color(0xFFFFA000),
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = "En Revisión",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F1F1F),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "Tu negocio está siendo revisado por nuestro equipo. Te notificaremos cuando sea aprobado.",
                fontSize = 15.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1F1F1F)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Entendido", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun ModernRejectedDialog(
    businessName: String,
    onDismiss: () -> Unit
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
                    Icons.Default.Cancel,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = "Negocio Rechazado",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F1F1F),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = businessName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE53935),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "No cumplió con los requisitos de verificación.",
                    fontSize = 15.sp,
                    color = Color(0xFF1F1F1F),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Text(
                    text = "Puedes crear un nuevo negocio o contactar a soporte.",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1F1F1F)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Entendido", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun ModernDisabledDialog(
    onDismiss: () -> Unit,
    onManage: () -> Unit
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
                    Icons.Default.Block,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = "Negocio Deshabilitado",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F1F1F),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Este negocio está desactivado y no es visible para los clientes.",
                    fontSize = 15.sp,
                    color = Color(0xFF1F1F1F),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Text(
                    text = "Para reactivarlo, ve a 'Gestionar Negocios'.",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onManage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1F1F1F)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Ir a Gestionar", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancelar", fontSize = 15.sp, color = Color(0xFF666666))
            }
        }
    )
}

@Composable
private fun ModernPhotoDialog(
    businessName: String,
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                "Cambiar foto de $businessName",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F1F1F)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Opción Cámara
                Card(
                    onClick = onCamera,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1F1F1F)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                "Tomar foto",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = Color(0xFF1F1F1F)
                            )
                            Text(
                                "Usar la cámara",
                                fontSize = 13.sp,
                                color = Color(0xFF999999)
                            )
                        }
                    }
                }

                // Opción Galería
                Card(
                    onClick = onGallery,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1F1F1F)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                "Elegir de galería",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = Color(0xFF1F1F1F)
                            )
                            Text(
                                "Seleccionar foto",
                                fontSize = 13.sp,
                                color = Color(0xFF999999)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar", color = Color(0xFF666666), fontWeight = FontWeight.Medium)
            }
        }
    )
}

@Composable
private fun ModernBlockedBusinessDialog(
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit
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
                    .background(Color(0xFFFFF9E6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFFFFA000),
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = "Negocio Bloqueado",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F1F1F),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Este negocio requiere una suscripción PRO.",
                    fontSize = 15.sp,
                    color = Color(0xFF1F1F1F),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Text(
                    text = "Actualiza tu plan para administrar múltiples negocios y acceder a todas las funciones premium.",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onUpgrade,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFA000)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text("Actualizar a PRO", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancelar", fontSize = 15.sp, color = Color(0xFF666666))
            }
        }
    )
}

// Helper class
private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)