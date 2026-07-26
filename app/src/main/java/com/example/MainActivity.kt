package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.repository.SchedulerRepository
import com.example.ui.*
import com.example.ui.components.AppNavigationDrawerContent
import com.example.ui.screens.*
import com.example.ui.theme.SmartSchedulerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SmartSchedulerTheme {
                SmartSchedulerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartSchedulerApp() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { SchedulerRepository(database) }
    val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(repository))

    val uiState by viewModel.uiState.collectAsState()
    val guruList by viewModel.guruList.collectAsState()
    val mapelList by viewModel.mapelList.collectAsState()
    val kelasList by viewModel.kelasList.collectAsState()
    val hariList by viewModel.hariList.collectAsState()
    val jamList by viewModel.jamList.collectAsState()
    val ruanganList by viewModel.ruanganList.collectAsState()
    val enrichedJadwalPair by viewModel.enrichedJadwalPair.collectAsState()
    val pengaturanList by viewModel.pengaturanList.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()

    val (jadwalDetails, conflicts) = enrichedJadwalPair

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val settingsMap = remember(pengaturanList) { pengaturanList.associate { it.key to it.value } }
    val namaMadrasah = settingsMap["nama_madrasah"] ?: "MTs Negeri 1 Model"
    val tahunPelajaran = settingsMap["tahun_pelajaran"] ?: "2025/2026"
    val semester = settingsMap["semester"] ?: "Ganjil"

    // Handle Snackbar messages
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                AppNavigationDrawerContent(
                    currentScreen = uiState.currentScreen,
                    activeRole = uiState.activeRole,
                    namaMadrasah = namaMadrasah,
                    tahunPelajaran = tahunPelajaran,
                    semester = semester,
                    onNavigate = { screen ->
                        viewModel.navigateTo(screen)
                    },
                    onCloseDrawer = {
                        coroutineScope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.currentScreen.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = namaMadrasah,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu Navigasi")
                        }
                    },
                    actions = {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = uiState.activeRole,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (uiState.currentScreen) {
                    AppScreen.DASHBOARD -> DashboardScreen(
                        guruList = guruList,
                        mapelList = mapelList,
                        kelasList = kelasList,
                        jadwalDetails = jadwalDetails,
                        conflicts = conflicts,
                        onNavigateTo = { viewModel.navigateTo(it) },
                        onRunAiScheduler = { viewModel.runAiScheduleGenerator("generate") }
                    )

                    AppScreen.GURU -> GuruScreen(
                        guruList = guruList,
                        searchQuery = uiState.guruSearchQuery,
                        filterStatus = uiState.guruFilterStatus,
                        activeRole = uiState.activeRole,
                        onSearchQueryChange = { viewModel.setGuruSearchQuery(it) },
                        onFilterStatusChange = { viewModel.setGuruFilterStatus(it) },
                        onSaveGuru = { viewModel.saveGuru(it) },
                        onDeleteGuru = { viewModel.deleteGuru(it) }
                    )

                    AppScreen.MAPEL -> MapelScreen(
                        mapelList = mapelList,
                        guruList = guruList,
                        searchQuery = uiState.mapelSearchQuery,
                        filterKelompok = uiState.mapelFilterKelompok,
                        activeRole = uiState.activeRole,
                        onSearchQueryChange = { viewModel.setMapelSearchQuery(it) },
                        onFilterKelompokChange = { viewModel.setMapelFilterKelompok(it) },
                        onSaveMapel = { m, isEdit -> viewModel.saveMapel(m, isEdit) },
                        onDeleteMapel = { viewModel.deleteMapel(it) }
                    )

                    AppScreen.KELAS -> KelasScreen(
                        kelasList = kelasList,
                        guruList = guruList,
                        activeRole = uiState.activeRole,
                        onSaveKelas = { viewModel.saveKelas(it) },
                        onDeleteKelas = { viewModel.deleteKelas(it) }
                    )

                    AppScreen.HARI, AppScreen.JAM -> HariJamScreen(
                        hariList = hariList,
                        jamList = jamList,
                        activeRole = uiState.activeRole,
                        onToggleHari = { viewModel.toggleHari(it) },
                        onSaveJam = { j, isEdit -> viewModel.saveJam(j, isEdit) },
                        onDeleteJam = { viewModel.deleteJam(it) }
                    )

                    AppScreen.RUANGAN -> RuanganScreen(
                        ruanganList = ruanganList,
                        activeRole = uiState.activeRole,
                        onSaveRuangan = { viewModel.saveRuangan(it) },
                        onDeleteRuangan = { viewModel.deleteRuangan(it) }
                    )

                    AppScreen.JADWAL -> JadwalMatrixScreen(
                        jadwalDetails = jadwalDetails,
                        conflicts = conflicts,
                        kelasList = kelasList,
                        guruList = guruList,
                        mapelList = mapelList,
                        hariList = hariList,
                        jamList = jamList,
                        ruanganList = ruanganList,
                        activeRole = uiState.activeRole,
                        onSaveJadwalSlot = { hId, jKode, kId, gId, mKode, rId ->
                            viewModel.saveJadwalSlot(hId, jKode, kId, gId, mKode, rId)
                        },
                        onClearJadwalSlot = { hId, jKode, kId ->
                            viewModel.clearJadwalSlot(hId, jKode, kId)
                        },
                        onClearAllJadwal = { viewModel.clearAllJadwal() }
                    )

                    AppScreen.AI_SCHEDULER -> AiSchedulerScreen(
                        guruList = guruList,
                        mapelList = mapelList,
                        kelasList = kelasList,
                        hariList = hariList,
                        jamList = jamList,
                        ruanganList = ruanganList,
                        conflicts = conflicts,
                        aiStage = uiState.aiStage,
                        isGenerating = uiState.isAiGenerating,
                        aiResult = uiState.aiResult,
                        onRunAiScheduler = { mode -> viewModel.runAiScheduleGenerator(mode) },
                        onApplyAiResult = { viewModel.applyAiResultToDatabase() }
                    )

                    AppScreen.LAPORAN -> LaporanScreen(
                        jadwalDetails = jadwalDetails,
                        guruList = guruList,
                        kelasList = kelasList,
                        mapelList = mapelList,
                        hariList = hariList,
                        ruanganList = ruanganList,
                        namaMadrasah = namaMadrasah,
                        tahunPelajaran = tahunPelajaran,
                        semester = semester
                    )

                    AppScreen.PENGATURAN -> PengaturanScreen(
                        pengaturanList = pengaturanList,
                        auditLogs = auditLogs,
                        activeRole = uiState.activeRole,
                        onRoleChange = { viewModel.setRole(it) },
                        onSavePengaturan = { key, valStr -> viewModel.savePengaturan(key, valStr) },
                        onResetSampleData = { viewModel.resetSampleData() }
                    )
                }
            }
        }
    }
}
