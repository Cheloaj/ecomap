package com.ecomap.usuario.presentation.ui.map

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ecomap.usuario.data.model.Business
import com.ecomap.usuario.presentation.viewmodel.BusinessUiState
import com.ecomap.usuario.presentation.viewmodel.BusinessViewModel
import com.ecomap.usuario.ui.theme.AppleColors
import com.ecomap.usuario.ui.theme.NuColors
import com.ecomap.usuario.utils.ImageConfig
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    onBusinessClick: (Business) -> Unit,
    onOpenDrawer: () -> Unit,
    onNavigateToReportProduct: () -> Unit = {},
    onNavigateToCommunityProducts: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    viewModel: BusinessViewModel = hiltViewModel(),
    productViewModel: com.ecomap.usuario.presentation.viewmodel.ProductViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val businessState by viewModel.businessState.collectAsState()
    val communityProducts by productViewModel.communityProducts.collectAsState()

    // ViewModel de usuario para obtener avatar y nombre
    val userDataViewModel: com.ecomap.usuario.presentation.viewmodel.UserDataViewModel = hiltViewModel()
    val userPreferences by userDataViewModel.userPreferences.collectAsState()

    var selectedClusterBusinesses by remember { mutableStateOf<List<Business>?>(null) }
    var selectedCommunityProduct by remember { mutableStateOf<com.ecomap.usuario.data.model.Product?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var showBusinesses by remember { mutableStateOf(true) }
    var showCommunityProducts by remember { mutableStateOf(true) }

    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    val coroutineScope = rememberCoroutineScope()
    val locationManager = remember {
        context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
    }

    // 🔄 CRITICAL: Cargar preferencias del usuario al inicio
    LaunchedEffect(Unit) {
        userDataViewModel.loadUserPreferences()

        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            tileDownloadThreads = 4.toShort()
            tileDownloadMaxQueueSize = 40.toShort()
            osmdroidBasePath = context.getExternalFilesDir(null)
            osmdroidTileCache = context.getExternalFilesDir("osmdroid/tiles")
            isDebugMode = false
            expirationOverrideDuration = 604800000L
        }
        productViewModel.loadCommunityProducts()
    }

    LaunchedEffect(showBusinesses, showCommunityProducts, businessState, communityProducts) {
        val map = mapView ?: return@LaunchedEffect

        try {
            map.overlays.clear()

            if (showBusinesses && businessState is BusinessUiState.Success) {
                val state = businessState as BusinessUiState.Success
                val clusterer = MarkerClusterer(
                    context = context,
                    businesses = state.businesses,
                    onBusinessClick = { business -> onBusinessClick(business) },
                    onClusterClick = { businesses -> selectedClusterBusinesses = businesses }
                )
                map.overlays.add(clusterer)
            }

            if (showCommunityProducts && communityProducts is com.ecomap.usuario.presentation.viewmodel.CommunityProductsUiState.Success) {
                val products = (communityProducts as com.ecomap.usuario.presentation.viewmodel.CommunityProductsUiState.Success).products
                products.forEach { product ->
                    if (product.latitude != null && product.longitude != null) {
                        try {
                            val marker = org.osmdroid.views.overlay.Marker(map)
                            marker.position = GeoPoint(product.latitude, product.longitude)
                            marker.title = "🛒 ${product.name}"
                            marker.snippet = "$${product.price} / ${product.unit}"
                            marker.icon = androidx.core.content.ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)
                            marker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
                            marker.setOnMarkerClickListener { _, _ ->
                                selectedCommunityProduct = product
                                true
                            }
                            map.overlays.add(marker)
                        } catch (e: Exception) {
                            android.util.Log.e("MapScreen", "Error creando marker para producto ${product.name}: ${e.message}")
                        }
                    }
                }
            }

            map.invalidate()
        } catch (e: Exception) {
            android.util.Log.e("MapScreen", "Error actualizando overlays del mapa: ${e.message}")
            e.printStackTrace()
        }
    }

    Scaffold(
        containerColor = Color(0xFFFAFAFA),
        topBar = {
            ModernMapTopBar(
                userName = userPreferences?.displayName ?: "Usuario",
                avatarUrl = userPreferences?.avatarUrl,
                onMenuClick = onOpenDrawer,
                onSearchClick = onNavigateToSearch
            )
        },
        floatingActionButton = {
            ModernFABColumn(
                onCommunityProductsClick = onNavigateToCommunityProducts,
                onReportProductClick = onNavigateToReportProduct,
                onLocationClick = {
                    if (locationPermissionState.status.isGranted) {
                        try {
                            val location = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                                ?: locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)

                            location?.let {
                                val userLocation = GeoPoint(it.latitude, it.longitude)
                                mapView?.controller?.animateTo(userLocation)
                                mapView?.controller?.setZoom(16.0)
                            } ?: run {
                                android.widget.Toast.makeText(
                                    context,
                                    "No se pudo obtener la ubicación",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: SecurityException) {
                            android.widget.Toast.makeText(
                                context,
                                "Error al obtener ubicación",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        locationPermissionState.launchPermissionRequest()
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = businessState) {
                is BusinessUiState.Loading -> {
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
                is BusinessUiState.Success -> {
                    AndroidView(
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                controller.setZoom(13.0)
                                controller.setCenter(GeoPoint(18.6465, -91.8323))

                                if (showBusinesses) {
                                    val clusterer = MarkerClusterer(
                                        context = ctx,
                                        businesses = state.businesses,
                                        onBusinessClick = { business -> onBusinessClick(business) },
                                        onClusterClick = { businesses -> selectedClusterBusinesses = businesses }
                                    )
                                    overlays.add(clusterer)
                                }

                                if (showCommunityProducts && communityProducts is com.ecomap.usuario.presentation.viewmodel.CommunityProductsUiState.Success) {
                                    val products = (communityProducts as com.ecomap.usuario.presentation.viewmodel.CommunityProductsUiState.Success).products
                                    products.forEach { product ->
                                        if (product.latitude != null && product.longitude != null) {
                                            val marker = org.osmdroid.views.overlay.Marker(this)
                                            marker.position = GeoPoint(product.latitude, product.longitude)
                                            marker.title = "🛒 ${product.name}"
                                            marker.snippet = "$${product.price} / ${product.unit}"
                                            marker.icon = androidx.core.content.ContextCompat.getDrawable(ctx, android.R.drawable.ic_menu_mylocation)
                                            marker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
                                            overlays.add(marker)
                                        }
                                    }
                                }

                                mapView = this
                            }
                        },
                        update = { mv ->
                            try {
                                mv.overlays.clear()

                                if (showBusinesses) {
                                    val clusterer = MarkerClusterer(
                                        context = context,
                                        businesses = state.businesses,
                                        onBusinessClick = { business -> onBusinessClick(business) },
                                        onClusterClick = { businesses -> selectedClusterBusinesses = businesses }
                                    )
                                    mv.overlays.add(clusterer)
                                }

                                if (showCommunityProducts && communityProducts is com.ecomap.usuario.presentation.viewmodel.CommunityProductsUiState.Success) {
                                    val products = (communityProducts as com.ecomap.usuario.presentation.viewmodel.CommunityProductsUiState.Success).products
                                    products.forEach { product ->
                                        if (product.latitude != null && product.longitude != null) {
                                            try {
                                                val marker = org.osmdroid.views.overlay.Marker(mv)
                                                marker.position = GeoPoint(product.latitude, product.longitude)
                                                marker.title = "🛒 ${product.name}"
                                                marker.snippet = "$${product.price} / ${product.unit}"
                                                marker.icon = androidx.core.content.ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)
                                                marker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
                                                mv.overlays.add(marker)
                                            } catch (e: Exception) {
                                                android.util.Log.e("MapScreen", "Error en update creando marker: ${e.message}")
                                            }
                                        }
                                    }
                                }

                                mv.invalidate()
                            } catch (e: Exception) {
                                android.util.Log.e("MapScreen", "Error en update del mapa: ${e.message}")
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is BusinessUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.padding(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFEBEE)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = state.message,
                                modifier = Modifier.padding(24.dp),
                                color = Color(0xFFE53935),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Filtros modernos
            ModernFilterChips(
                showBusinesses = showBusinesses,
                showCommunityProducts = showCommunityProducts,
                onBusinessesToggle = { showBusinesses = !showBusinesses },
                onProductsToggle = { showCommunityProducts = !showCommunityProducts }
            )
        }
    }

    // Bottom sheets
    selectedClusterBusinesses?.let { businesses ->
        ModernClusterBottomSheet(
            businesses = businesses,
            onDismiss = { selectedClusterBusinesses = null },
            onBusinessClick = { business ->
                selectedClusterBusinesses = null
                coroutineScope.launch {
                    delay(350)
                    onBusinessClick(business)
                }
            }
        )
    }

    selectedCommunityProduct?.let { product ->
        CommunityProductBottomSheet(
            product = product,
            onDismiss = { selectedCommunityProduct = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernMapTopBar(
    userName: String,
    avatarUrl: String?,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    val context = LocalContext.current

    TopAppBar(
        title = {

            Text(
                text = "Explora negocios cerca",
                fontSize = 20.sp,
                color = Color(0xFF000000)
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
            IconButton(onClick = onSearchClick) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = Color(0xFF1F1F1F)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        )
    )
}

@Composable
private fun ModernFABColumn(
    onCommunityProductsClick: () -> Unit,
    onReportProductClick: () -> Unit,
    onLocationClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.End
    ) {
        SmallFloatingActionButton(
            onClick = onCommunityProductsClick,
            containerColor = Color(0xFF00BCD4),
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.People, contentDescription = "Productos comunitarios")
        }

        SmallFloatingActionButton(
            onClick = onReportProductClick,
            containerColor = Color(0xFFFFA000),
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.AddBox, contentDescription = "Publicar producto")
        }

        FloatingActionButton(
            onClick = onLocationClick,
            containerColor = Color(0xFF1F1F1F),
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Mi ubicación")
        }
    }
}

@Composable
private fun ModernFilterChips(
    showBusinesses: Boolean,
    showCommunityProducts: Boolean,
    onBusinessesToggle: () -> Unit,
    onProductsToggle: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Row(
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = showBusinesses,
                onClick = onBusinessesToggle,
                label = { Text("Negocios", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Store,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF9C27B0),
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White,
                    containerColor = Color.White
                )
            )

            FilterChip(
                selected = showCommunityProducts,
                onClick = onProductsToggle,
                label = { Text("Productos", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF00BCD4),
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White,
                    containerColor = Color.White
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernClusterBottomSheet(
    businesses: List<Business>,
    onDismiss: () -> Unit,
    onBusinessClick: (Business) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Deja aire para la barra de navegación del sistema
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            Text(
                text = "${businesses.size} negocios en esta área",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F1F1F),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(businesses) { business ->
                    Card(
                        onClick = { onBusinessClick(business) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFAFAFA)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50).copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Store,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = business.businessName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1F1F1F)
                                )
                                Text(
                                    text = business.businessType,
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
            }
        }
    }
}