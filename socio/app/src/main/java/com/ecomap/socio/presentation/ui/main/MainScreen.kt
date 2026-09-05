package com.ecomap.socio.presentation.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.socio.presentation.ui.profile.ProfileScreen
import com.ecomap.socio.presentation.viewmodel.NotificationViewModel
import com.ecomap.socio.ui.theme.NuColors
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    onSignOut: () -> Unit,
    onNavigateToOnboarding: (String) -> Unit,
    onNavigateToDashboard: (String) -> Unit = {},
    onNavigateToForgotPassword: (String) -> Unit,
    onNavigateToUpgrade: () -> Unit
) {
    val notificationViewModel: NotificationViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        println("🚀 MainScreen: Iniciando NotificationViewModel después de login")
    }

    val pagerState = rememberPagerState(
        initialPage = 0, // Inicio por defecto
        pageCount = { 3 } // Inicio, Ofertas, Perfil
    )
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                currentPage = pagerState.currentPage,
                onNavigateToPage = { page ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(page)
                        drawerState.close()
                    }
                },
                onSignOut = {
                    coroutineScope.launch {
                        drawerState.close()
                    }
                    onSignOut()
                }
            )
        }
    ) {
        Scaffold(
            containerColor = NuColors.Background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                userScrollEnabled = false // Deshabilitar swipe lateral
            ) { page ->
                when (page) {
                    0 -> TodayScreen(
                        onNavigateToOnboarding = onNavigateToOnboarding,
                        onNavigateToDashboard = onNavigateToDashboard,
                        onNavigateToSubscription = onNavigateToUpgrade,
                        onOpenDrawer = {
                            coroutineScope.launch {
                                drawerState.open()
                            }
                        }
                    )
                    1 -> PlaceholderScreen(
                        title = "Ofertas",
                        onOpenDrawer = {
                            coroutineScope.launch {
                                drawerState.open()
                            }
                        }
                    )
                    2 -> ProfileScreen(
                        onSignOut = onSignOut,
                        onNavigateToForgotPassword = onNavigateToForgotPassword,
                        onNavigateToUpgrade = onNavigateToUpgrade,
                        onOpenDrawer = {
                            coroutineScope.launch {
                                drawerState.open()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerContent(
    currentPage: Int,
    onNavigateToPage: (Int) -> Unit,
    onSignOut: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = NuColors.Surface
    ) {
        // Header del Drawer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NuColors.Primary)
                .padding(24.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Store,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "EcoMap Socio",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Gestiona tus negocios",
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Opciones del menú
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Store, contentDescription = null) },
            label = { Text("Inicio") },
            selected = currentPage == 0,
            onClick = { onNavigateToPage(0) },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = NuColors.Primary.copy(alpha = 0.1f),
                selectedIconColor = NuColors.Primary,
                selectedTextColor = NuColors.Primary,
                unselectedIconColor = NuColors.TextSecondary,
                unselectedTextColor = NuColors.TextPrimary
            ),
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text("Perfil") },
            selected = currentPage == 2,
            onClick = { onNavigateToPage(2) },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = NuColors.Primary.copy(alpha = 0.1f),
                selectedIconColor = NuColors.Primary,
                selectedTextColor = NuColors.Primary,
                unselectedIconColor = NuColors.TextSecondary,
                unselectedTextColor = NuColors.TextPrimary
            ),
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            color = NuColors.TextSecondary.copy(alpha = 0.2f)
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Logout, contentDescription = null) },
            label = { Text("Cerrar sesión") },
            selected = false,
            onClick = onSignOut,
            colors = NavigationDrawerItemDefaults.colors(
                unselectedIconColor = NuColors.Error,
                unselectedTextColor = NuColors.Error
            ),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(
    title: String,
    onOpenDrawer: () -> Unit = {}
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        color = NuColors.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Menú",
                            tint = NuColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NuColors.Background)
            )
        },
        containerColor = NuColors.Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                text = "Próximamente",
                style = MaterialTheme.typography.headlineMedium,
                color = NuColors.TextSecondary
            )
        }
    }
}
