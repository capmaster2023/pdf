package com.pdfpocket.lite.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.pdfpocket.lite.R
import com.pdfpocket.lite.features.about.AboutScreen
import com.pdfpocket.lite.features.about.PrivacyScreen
import com.pdfpocket.lite.features.files.FilesScreen
import com.pdfpocket.lite.features.home.HomeScreen
import com.pdfpocket.lite.features.ocr.OcrScreen
import com.pdfpocket.lite.features.scanner.ScannerScreen
import com.pdfpocket.lite.features.settings.SettingsScreen
import com.pdfpocket.lite.features.signature.SignatureScreen
import com.pdfpocket.lite.features.tools.ImagesToPdfScreen
import com.pdfpocket.lite.features.tools.ToolOperationScreen
import com.pdfpocket.lite.features.tools.ToolsScreen
import com.pdfpocket.lite.features.viewer.ViewerScreen

private data class NavItem(val route: String, val label: Int, val icon: ImageVector)

@Composable
fun AppNavigation(incomingUri: Uri?, onIncomingConsumed: () -> Unit) {
    val navController = rememberNavController()
    val items = listOf(
        NavItem(Routes.HOME, R.string.home, Icons.Default.Home),
        NavItem(Routes.FILES, R.string.documents, Icons.Default.Folder),
        NavItem(Routes.TOOLS, R.string.tools, Icons.Default.Build),
        NavItem(Routes.SETTINGS, R.string.settings, Icons.Default.Settings)
    )
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val showBottom = current in items.map { it.route }

    LaunchedEffect(incomingUri) {
        if (incomingUri != null) {
            navController.navigate(Routes.viewer(incomingUri.toString()))
            onIncomingConsumed()
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottom) {
                NavigationBar {
                    items.forEach { item ->
                        NavigationBarItem(
                            selected = current == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = stringResource(item.label)) },
                            label = { Text(stringResource(item.label)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = Routes.HOME, modifier = Modifier.padding(padding)) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenDocument = { navController.navigate(Routes.viewer(it.toString())) },
                    onScanner = { navController.navigate(Routes.SCANNER) },
                    onImages = { navController.navigate(Routes.IMAGES) },
                    onTool = { operation ->
                        if (operation == "signature") navController.navigate(Routes.SIGNATURE)
                        else navController.navigate(Routes.tool(operation))
                    }
                )
            }
            composable(Routes.FILES) { FilesScreen { navController.navigate(Routes.viewer(it.toString())) } }
            composable(Routes.TOOLS) {
                ToolsScreen(
                    onTool = { navController.navigate(Routes.tool(it)) },
                    onImages = { navController.navigate(Routes.IMAGES) },
                    onScanner = { navController.navigate(Routes.SCANNER) },
                    onOcr = { navController.navigate(Routes.OCR) },
                    onSignature = { navController.navigate(Routes.SIGNATURE) }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onPrivacy = { navController.navigate(Routes.PRIVACY) },
                    onAbout = { navController.navigate(Routes.ABOUT) }
                )
            }
            composable(Routes.VIEWER, arguments = listOf(navArgument("uri") { type = NavType.StringType })) {
                ViewerScreen { navController.popBackStack() }
            }
            composable(Routes.IMAGES) { ImagesToPdfScreen { navController.popBackStack() } }
            composable(Routes.SCANNER) { ScannerScreen { navController.popBackStack() } }
            composable(Routes.OCR) { OcrScreen { navController.popBackStack() } }
            composable(Routes.SIGNATURE) { SignatureScreen { navController.popBackStack() } }
            composable(Routes.TOOL_OPERATION, arguments = listOf(navArgument("operation") { type = NavType.StringType })) { entry ->
                ToolOperationScreen(entry.arguments?.getString("operation").orEmpty()) { navController.popBackStack() }
            }
            composable(Routes.PRIVACY) { PrivacyScreen { navController.popBackStack() } }
            composable(Routes.ABOUT) { AboutScreen { navController.popBackStack() } }
        }
    }
}
