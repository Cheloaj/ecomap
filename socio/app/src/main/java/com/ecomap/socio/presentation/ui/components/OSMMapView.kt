package com.ecomap.socio.presentation.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ecomap.socio.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import com.ecomap.socio.ui.theme.NuColors

@Composable
fun OSMMapView(
    modifier: Modifier = Modifier,
    location: Location?,
    businessName: String? = null,
    isManualMode: Boolean = false,
    onLocationObtained: ((Double, Double, String) -> Unit)? = null,
    onConfirmLocation: (() -> Unit)? = null,
    onModeChange: ((Boolean) -> Unit)? = null // true = manual, false = automático
) {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var showGpsDisabledDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isOutsideMexico by remember { mutableStateOf(false) }
    var currentAddress by remember { mutableStateOf("") }
    var gpsAddress by remember { mutableStateOf("") } // Dirección fija del GPS
    var isExpanded by remember { mutableStateOf(false) }
    var hasObtainedGpsLocation by remember { mutableStateOf(false) }
    var gpsLocation by remember { mutableStateOf<Location?>(null) }

    // Función para validar si las coordenadas están dentro de México
    fun isInMexico(lat: Double, lon: Double): Boolean {
        return lat in 14.5..32.7 && lon in -117.1..-86.7
    }

    // Limpiar dirección y estado GPS al cambiar de modo
    LaunchedEffect(isManualMode) {
        if (!isManualMode) {
            // Cambio a automático: limpiar todo
            currentAddress = ""
            gpsAddress = ""
            hasObtainedGpsLocation = false
            gpsLocation = null
        } else {
            // Cambio a manual: limpiar datos GPS Y marcadores del mapa
            gpsAddress = ""
            hasObtainedGpsLocation = false
            gpsLocation = null

            // Limpiar marcadores GPS del mapa
            mapView?.overlays?.removeAll { it is Marker }
            mapView?.invalidate()
        }
    }

    // ✅ Función para verificar si el GPS está activado
    fun isGpsEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    // Launcher para permisos de ubicación
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val hasPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (hasPermission) {
            // ✅ Verificar si el GPS está activado
            if (!isGpsEnabled()) {
                println("❌ GPS está DESACTIVADO - Mostrando diálogo")
                showGpsDisabledDialog = true
                return@rememberLauncherForActivityResult
            }

            println("✅ GPS activado - Obteniendo ubicación")
            isLoadingLocation = true
            getCurrentLocation(
                context = context,
                onSuccess = { lat, lon ->
                    println("✅ Ubicación obtenida: $lat, $lon")
                    isLoadingLocation = false
                    hasObtainedGpsLocation = true // ✅ Marcar que se obtuvo ubicación GPS

                    // Guardar ubicación GPS para mostrar marcador
                    gpsLocation = Location("gps").apply {
                        latitude = lat
                        longitude = lon
                    }

                    // Geocodificación inversa - guardar en gpsAddress (fijo)
                    reverseGeocode(context, lat, lon) { address ->
                        gpsAddress = address
                        onLocationObtained?.invoke(lat, lon, address)
                    }
                },
                onFailure = {
                    println("❌ Error al obtener ubicación")
                    isLoadingLocation = false
                    hasObtainedGpsLocation = false
                    gpsLocation = null
                    gpsAddress = ""
                }
            )
        } else {
            println("❌ Permisos de ubicación DENEGADOS")
        }
    }

    // ✅ Diálogo GPS desactivado
    if (showGpsDisabledDialog) {
        AlertDialog(
            onDismissRequest = { showGpsDisabledDialog = false },
            icon = {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = NuColors.Error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "GPS Desactivado",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = NuColors.TextPrimary
                )
            },
            text = {
                Text(
                    text = "Para usar tu ubicación actual, necesitas activar el GPS en la configuración de tu dispositivo.",
                    fontSize = 16.sp,
                    color = NuColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGpsDisabledDialog = false
                        // Abrir configuración de ubicación
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NuColors.Primary),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("Activar GPS", color = Color.White)
                }
            }
        )
    }

    // Configurar osmdroid (solo una vez)
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", 0)
        )
        Configuration.getInstance().userAgentValue = context.packageName
    }

    // Diálogo de mapa expandido
    if (isExpanded) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { isExpanded = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(17.0)

                            if (location != null) {
                                val geoPoint = GeoPoint(location.latitude, location.longitude)
                                controller.setCenter(geoPoint)
                            } else {
                                controller.setCenter(GeoPoint(18.6435, -91.8260))
                            }
                        }
                    },
                    update = { map ->
                        map.overlays.removeAll { it is org.osmdroid.views.overlay.MapEventsOverlay }

                        // Si cambiamos a modo manual, limpiar TODOS los overlays (incluyendo marcadores GPS)
                        if (isManualMode) {
                            map.overlays.clear()
                        }

                        if (isManualMode) {
                            // Listener para modo manual
                            val mapListener = object : org.osmdroid.events.MapListener {
                                override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                                    val center = map.mapCenter as GeoPoint
                                    reverseGeocode(map.context, center.latitude, center.longitude) { address ->
                                        currentAddress = address
                                        onLocationObtained?.invoke(center.latitude, center.longitude, address)
                                    }
                                    return true
                                }

                                override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean {
                                    val center = map.mapCenter as GeoPoint
                                    reverseGeocode(map.context, center.latitude, center.longitude) { address ->
                                        currentAddress = address
                                        onLocationObtained?.invoke(center.latitude, center.longitude, address)
                                    }
                                    return false
                                }
                            }

                            map.addMapListener(mapListener)

                            // Invocar inmediatamente con el centro actual
                            val center = map.mapCenter as GeoPoint
                            reverseGeocode(map.context, center.latitude, center.longitude) { address ->
                                currentAddress = address
                                onLocationObtained?.invoke(center.latitude, center.longitude, address)
                            }
                        } else {
                            // Modo automático: Agregar marcador fijo en ubicación GPS
                            gpsLocation?.let {
                                val geoPoint = GeoPoint(it.latitude, it.longitude)
                                map.controller.animateTo(geoPoint)
                                addMarker(map, geoPoint, businessName)
                            }
                            map.invalidate()
                        }
                    }
                )

                // Botón cerrar pantalla completa
                FloatingActionButton(
                    onClick = { isExpanded = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(48.dp),
                    containerColor = Color.White,
                    contentColor = NuColors.Primary,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Salir pantalla completa",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Botones de modo (Automático/Manual) en mapa expandido
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp)
                        .fillMaxWidth(0.7f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón Automático
                    Button(
                        onClick = {
                            onModeChange?.invoke(false) // false = automático
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isManualMode) NuColors.Primary else Color.White.copy(alpha = 0.9f),
                            contentColor = if (!isManualMode) Color.White else NuColors.Primary
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Automático",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Botón Manual
                    Button(
                        onClick = {
                            onModeChange?.invoke(true) // true = manual
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isManualMode) NuColors.Primary else Color.White.copy(alpha = 0.9f),
                            contentColor = if (isManualMode) Color.White else NuColors.Primary
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Manual",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Pin fijo en centro (solo modo manual)
                // Modo automático usa marcador fijo en coordenadas GPS
                if (isManualMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (-32).dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_location_pin_apple),
                            contentDescription = "Pin de ubicación",
                            modifier = Modifier.size(64.dp),
                            tint = Color.Unspecified // Mantener colores originales del SVG
                        )
                    }
                }

                // FAB GPS (modo automático)
                if (!isManualMode) {
                    FloatingActionButton(
                        onClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(56.dp),
                        containerColor = NuColors.Primary,
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        if (isLoadingLocation) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Obtener mi ubicación"
                            )
                        }
                    }
                }

                // Botón confirmar con dirección (mapa expandido)
                val displayAddressExpanded = if (isManualMode) currentAddress else gpsAddress
                if (displayAddressExpanded.isNotEmpty() && (isManualMode || hasObtainedGpsLocation)) {
                    Surface(
                        onClick = {
                            // Validar si la ubicación está en México
                            val lat = if (isManualMode) {
                                mapView?.mapCenter?.latitude ?: 0.0
                            } else {
                                gpsLocation?.latitude ?: 0.0
                            }
                            val lon = if (isManualMode) {
                                mapView?.mapCenter?.longitude ?: 0.0
                            } else {
                                gpsLocation?.longitude ?: 0.0
                            }

                            if (isInMexico(lat, lon)) {
                                isOutsideMexico = false
                                showConfirmDialog = true
                            } else {
                                isOutsideMexico = true
                                showConfirmDialog = true
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = displayAddressExpanded,
                                fontSize = 13.sp,
                                color = Color(0xFF565656),
                                lineHeight = 18.sp,
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Confirmar ubicación",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NuColors.Primary
                                )
                            }
                        }
                    }
                }

                // Diálogo de confirmación en mapa expandido
                androidx.compose.animation.AnimatedVisibility(
                    visible = showConfirmDialog,
                    enter = androidx.compose.animation.slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = androidx.compose.animation.core.tween(
                            durationMillis = 400,
                            easing = androidx.compose.animation.core.FastOutSlowInEasing
                        )
                    ) + androidx.compose.animation.fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(300)
                    ),
                    exit = androidx.compose.animation.slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = androidx.compose.animation.core.tween(300)
                    ) + androidx.compose.animation.fadeOut(
                        animationSpec = androidx.compose.animation.core.tween(200)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            shadowElevation = 12.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (isOutsideMexico) {
                                    // Mostrar error: fuera de México
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = NuColors.Error
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "Ubicación fuera de México",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NuColors.TextPrimary
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Debes seleccionar una ubicación dentro de México para continuar.",
                                        fontSize = 14.sp,
                                        color = NuColors.TextSecondary,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Solo botón de cerrar
                                    Button(
                                        onClick = { showConfirmDialog = false },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NuColors.Error
                                        )
                                    ) {
                                        Text("Entendido", color = Color.White)
                                    }
                                } else {
                                    // Confirmación normal
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = NuColors.Primary
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "¿Confirmar ubicación?",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NuColors.TextPrimary
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = if (isManualMode) currentAddress else gpsAddress,
                                        fontSize = 14.sp,
                                        color = NuColors.TextSecondary,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Botón Cancelar
                                        OutlinedButton(
                                            onClick = { showConfirmDialog = false },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = NuColors.TextPrimary
                                            )
                                        ) {
                                            Text("Cancelar")
                                        }

                                        // Botón Confirmar
                                        Button(
                                            onClick = {
                                                showConfirmDialog = false
                                                isExpanded = false
                                                onConfirmLocation?.invoke()
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = NuColors.Primary
                                            )
                                        ) {
                                            Text("Confirmar", color = Color.White)
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

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(17.0)

                    // Posición inicial
                    if (location != null) {
                        val geoPoint = GeoPoint(location.latitude, location.longitude)
                        controller.setCenter(geoPoint)
                    } else {
                        // Ciudad del Carmen, Campeche por defecto
                        controller.setCenter(GeoPoint(18.6435, -91.8260))
                    }

                    mapView = this
                }
            },
            update = { map ->
                // Limpiar listeners anteriores
                map.overlays.removeAll { it is org.osmdroid.views.overlay.MapEventsOverlay }

                // Si cambiamos a modo manual, limpiar TODOS los overlays (incluyendo marcadores GPS)
                if (isManualMode) {
                    map.overlays.clear()
                }

                if (isManualMode) {
                    // Listener para modo manual
                    val mapEventsReceiver = object : org.osmdroid.events.MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            return false
                        }

                        override fun longPressHelper(p: GeoPoint?): Boolean {
                            return false
                        }
                    }

                    // Agregar listener de scroll
                    val mapListener = object : org.osmdroid.events.MapListener {
                        override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                            val center = map.mapCenter as GeoPoint
                            // Geocodificación inversa
                            reverseGeocode(map.context, center.latitude, center.longitude) { address ->
                                currentAddress = address
                                onLocationObtained?.invoke(center.latitude, center.longitude, address)
                            }
                            return true
                        }

                        override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean {
                            val center = map.mapCenter as GeoPoint
                            reverseGeocode(map.context, center.latitude, center.longitude) { address ->
                                currentAddress = address
                                onLocationObtained?.invoke(center.latitude, center.longitude, address)
                            }
                            return false
                        }
                    }

                    map.addMapListener(mapListener)

                    // Invocar inmediatamente con el centro actual (SOLO en modo manual)
                    val center = map.mapCenter as GeoPoint
                    reverseGeocode(map.context, center.latitude, center.longitude) { address ->
                        currentAddress = address
                        onLocationObtained?.invoke(center.latitude, center.longitude, address)
                    }
                } else {
                    // Modo automático: Agregar marcador fijo en ubicación GPS
                    gpsLocation?.let {
                        val geoPoint = GeoPoint(it.latitude, it.longitude)
                        map.controller.animateTo(geoPoint)
                        addMarker(map, geoPoint, businessName)
                    }
                    map.invalidate()
                }
            }
        )

        // Botón expandir mapa (esquina superior derecha)
        FloatingActionButton(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(48.dp),
            containerColor = Color.White,
            contentColor = NuColors.Primary,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = if (isExpanded) "Salir pantalla completa" else "Pantalla completa",
                modifier = Modifier.size(24.dp)
            )
        }

        // Pin fijo en centro (solo modo manual)
        // Modo automático usa marcador fijo en coordenadas GPS
        if (isManualMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-32).dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_location_pin_apple),
                    contentDescription = "Pin de ubicación",
                    modifier = Modifier.size(64.dp),
                    tint = Color.Unspecified // Mantener colores originales del SVG
                )
            }
        }

        // FAB CIRCULAR para obtener ubicación GPS (solo en modo automático, abajo derecha)
        if (!isManualMode) {
            FloatingActionButton(
                onClick = {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(56.dp),
                containerColor = NuColors.Primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                if (isLoadingLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Obtener mi ubicación"
                    )
                }
            }
        }

        // Botón confirmar con dirección
        // Manual: usar currentAddress (tiempo real)
        // Automático: usar gpsAddress (fijo)
        val displayAddress = if (isManualMode) currentAddress else gpsAddress
        if (displayAddress.isNotEmpty() && (isManualMode || hasObtainedGpsLocation)) {
            Surface(
                onClick = {
                    // Validar si la ubicación está en México
                    val lat = if (isManualMode) {
                        mapView?.mapCenter?.latitude ?: 0.0
                    } else {
                        gpsLocation?.latitude ?: 0.0
                    }
                    val lon = if (isManualMode) {
                        mapView?.mapCenter?.longitude ?: 0.0
                    } else {
                        gpsLocation?.longitude ?: 0.0
                    }

                    if (isInMexico(lat, lon)) {
                        isOutsideMexico = false
                        showConfirmDialog = true
                    } else {
                        isOutsideMexico = true
                        showConfirmDialog = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 64.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Dirección
                    Text(
                        text = displayAddress,
                        fontSize = 14.sp,
                        color = Color(0xFF565656),
                        lineHeight = 20.sp,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Botón confirmar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Confirmar ubicación",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NuColors.Primary
                        )
                    }
                }
            }
        }

        // Diálogo de confirmación (solo modo automático)
        androidx.compose.animation.AnimatedVisibility(
            visible = showConfirmDialog,
            enter = androidx.compose.animation.slideInVertically(
                initialOffsetY = { -it },
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 400,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                )
            ) + androidx.compose.animation.fadeIn(
                animationSpec = androidx.compose.animation.core.tween(300)
            ),
            exit = androidx.compose.animation.slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = androidx.compose.animation.core.tween(300)
            ) + androidx.compose.animation.fadeOut(
                animationSpec = androidx.compose.animation.core.tween(200)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 12.dp,
                    modifier = Modifier.padding(top = 40.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isOutsideMexico) {
                            // Mostrar error: fuera de México
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = NuColors.Error
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Ubicación fuera de México",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = NuColors.TextPrimary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Debes seleccionar una ubicación dentro de México para continuar.",
                                fontSize = 14.sp,
                                color = NuColors.TextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Solo botón de cerrar
                            Button(
                                onClick = { showConfirmDialog = false },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NuColors.Error
                                )
                            ) {
                                Text("Entendido", color = Color.White)
                            }
                        } else {
                            // Confirmación normal
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = NuColors.Primary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "¿Confirmar ubicación?",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = NuColors.TextPrimary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (isManualMode) currentAddress else gpsAddress,
                                fontSize = 14.sp,
                                color = NuColors.TextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Botón Cancelar
                                OutlinedButton(
                                    onClick = { showConfirmDialog = false },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = NuColors.TextPrimary
                                    )
                                ) {
                                    Text("Cancelar")
                                }

                                // Botón Confirmar
                                Button(
                                    onClick = {
                                        showConfirmDialog = false
                                        isExpanded = false
                                        onConfirmLocation?.invoke()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NuColors.Primary
                                    )
                                ) {
                                    Text("Confirmar", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun reverseGeocode(
    context: Context,
    latitude: Double,
    longitude: Double,
    onResult: (String) -> Unit
) {
    Thread {
        try {
            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            val addressText = if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                buildString {
                    val lines = mutableListOf<String>()

                    // 1. Nombre del lugar/establecimiento (si existe)
                    address.featureName?.let {
                        if (!it.matches(Regex("\\d+")) && it != address.thoroughfare) {
                            lines.add("📍 $it")
                        }
                    }

                    // 2. Calle y Número
                    val street = buildString {
                        address.thoroughfare?.let { append(it) }
                        address.subThoroughfare?.let {
                            if (isNotEmpty()) append(" ")
                            append("#$it")
                        }
                    }
                    if (street.isNotEmpty()) lines.add(street)

                    // 3. Colonia/Barrio
                    address.subLocality?.let {
                        lines.add("Col. $it")
                    }

                    // 4. Código Postal
                    address.postalCode?.let { lines.add("C.P. $it") }

                    // 5. Municipio (si está disponible)
                    address.subAdminArea?.let {
                        if (it != address.locality) {
                            lines.add("$it")
                        }
                    }

                    // 6. Ciudad
                    address.locality?.let { lines.add(it) }

                    // 7. Estado
                    address.adminArea?.let { lines.add(it) }

                    // 8. País
                    address.countryName?.let {
                        if (it.isNotEmpty()) lines.add(it)
                    }

                    // Unir con saltos de línea
                    append(lines.joinToString("\n"))
                }.ifEmpty { "Ubicación seleccionada" }
            } else {
                "Ubicación seleccionada"
            }

            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onResult(addressText)
            }
        } catch (e: Exception) {
            println("Error en geocodificación inversa: ${e.message}")
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onResult("Ubicación seleccionada")
            }
        }
    }.start()
}

private fun addMarker(mapView: MapView, geoPoint: GeoPoint, title: String?) {
    // Usar el pin personalizado estilo Apple
    val icon = androidx.core.content.ContextCompat.getDrawable(
        mapView.context,
        R.drawable.ic_location_pin_apple
    )

    val marker = Marker(mapView).apply {
        position = geoPoint
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        this.title = title ?: "Mi Negocio"
        this.icon = icon
    }
    mapView.overlays.add(marker)
}

private fun getCurrentLocation(
    context: android.content.Context,
    onSuccess: (Double, Double) -> Unit,
    onFailure: () -> Unit
) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    try {
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { location: Location? ->
            location?.let {
                onSuccess(it.latitude, it.longitude)
            } ?: onFailure()
        }.addOnFailureListener {
            onFailure()
        }
    } catch (e: SecurityException) {
        onFailure()
    }
}

