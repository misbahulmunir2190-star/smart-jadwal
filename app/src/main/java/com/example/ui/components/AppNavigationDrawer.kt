package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationDrawerContent(
    currentScreen: AppScreen,
    activeRole: String,
    namaMadrasah: String,
    tahunPelajaran: String,
    semester: String,
    onNavigate: (AppScreen) -> Unit,
    onCloseDrawer: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "Logo",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "SMART SCHEDULER",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(
                                text = namaMadrasah,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = activeRole,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            )
                        }

                        Text(
                            text = "$tahunPelajaran ($semester)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "MODUL NAVIGASI",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            AppScreen.entries.forEach { screen ->
                val isSelected = screen == currentScreen
                val icon = getIconForScreen(screen)

                NavigationDrawerItem(
                    label = {
                        Text(
                            text = screen.title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = screen.title
                        )
                    },
                    selected = isSelected,
                    onClick = {
                        onNavigate(screen)
                        onCloseDrawer()
                    },
                    modifier = Modifier.padding(vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun getIconForScreen(screen: AppScreen): ImageVector {
    return when (screen) {
        AppScreen.DASHBOARD -> Icons.Default.Home
        AppScreen.GURU -> Icons.Default.Person
        AppScreen.MAPEL -> Icons.Default.MenuBook
        AppScreen.KELAS -> Icons.Default.Class
        AppScreen.HARI -> Icons.Default.Today
        AppScreen.JAM -> Icons.Default.AccessTime
        AppScreen.RUANGAN -> Icons.Default.MeetingRoom
        AppScreen.JADWAL -> Icons.Default.CalendarMonth
        AppScreen.AI_SCHEDULER -> Icons.Default.AutoAwesome
        AppScreen.LAPORAN -> Icons.Default.Assessment
        AppScreen.PENGATURAN -> Icons.Default.Settings
    }
}
