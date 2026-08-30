package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.*
import com.example.domain.validator.ScheduleConflict
import com.example.util.PdfScheduleExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JadwalMatrixScreen(
    jadwalDetails: List<JadwalDetail>,
    conflicts: List<ScheduleConflict>,
    kelasList: List<KelasEntity>,
    guruList: List<GuruEntity>,
    mapelList: List<MapelEntity>,
    hariList: List<HariEntity>,
    jamList: List<JamEntity>,
    ruanganList: List<RuanganEntity>,
    pengaturanList: List<PengaturanEntity> = emptyList(),
    namaMadrasah: String = "MTs Negeri 1 Model",
    tahunPelajaran: String = "2025/2026",
    semester: String = "Ganjil",
    activeRole: String,
    onSaveJadwalSlot: (hariId: Int, jpKode: String, kelasId: Long, guruId: Long, mapelKode: String, ruanganId: Long) -> Unit,
    onClearJadwalSlot: (hariId: Int, jpKode: String, kelasId: Long) -> Unit,
    onClearAllJadwal: () -> Unit,
    onAutoFixConflicts: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedKelasId by remember(kelasList) { mutableLongStateOf(kelasList.firstOrNull()?.id ?: 1L) }
    var selectedSlotCell by remember { mutableStateOf<SlotCellTarget?>(null) }
    var showConfirmClearAll by remember { mutableStateOf(false) }
    var showPrintDialog by remember { mutableStateOf(false) }

    val settingsMap = remember(pengaturanList, namaMadrasah, tahunPelajaran, semester) {
        val map = pengaturanList.associate { it.key to it.value }.toMutableMap()
        map["nama_madrasah"] = namaMadrasah
        map["tahun_pelajaran"] = tahunPelajaran
        map["semester"] = semester
        map
    }

    val activeHariList = remember(hariList) { hariList.filter { it.isAktif } }
    val activeJamList = remember(jamList) { jamList.sortedBy { it.urutan } }

    val selectedKelas = kelasList.find { it.id == selectedKelasId }
    val isReadOnly = activeRole == "Kepala Madrasah"

    var viewMode by remember { mutableIntStateOf(0) } // 0 = Mode List Harian (Mobile Friendly), 1 = Mode Tabel Matriks (Grid)
    var selectedHariId by remember(activeHariList) { mutableIntStateOf(activeHariList.firstOrNull()?.id ?: 1) }

    val jadwalSlotMap = remember(jadwalDetails, selectedKelasId) {
        jadwalDetails.filter { it.kelasId == selectedKelasId }
            .associateBy { "${it.hariId}_${it.jpKode}" }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "JADWAL PELAJARAN",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "Kelas ${selectedKelas?.namaKelas ?: ""} • $tahunPelajaran ($semester)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { showPrintDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "Cetak PDF", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF", style = MaterialTheme.typography.labelMedium)
                        }

                        if (!isReadOnly) {
                            IconButton(
                                onClick = { showConfirmClearAll = true },
                                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Reset Semua")
                            }
                        }
                    }
                }

                // Conflict Banner Alert with One-Click Resolution Button
                if (conflicts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Bentrok",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Terdeteksi ${conflicts.size} Bentrok Jadwal",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = conflicts.firstOrNull()?.message ?: "Terdapat jadwal guru / kelas yang bertabrakan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            if (!isReadOnly) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onAutoFixConflicts,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Perbaiki Semua Bentrok Otomatis (100% Bebas Bentrok)",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Rombel Selector Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    kelasList.forEach { k ->
                        FilterChip(
                            selected = k.id == selectedKelasId,
                            onClick = { selectedKelasId = k.id },
                            label = { Text("Kelas ${k.namaKelas}", fontWeight = if (k.id == selectedKelasId) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = if (k.id == selectedKelasId) {
                                { Icon(Icons.Default.Class, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // View Mode Switcher: Mobile List vs Grid Table
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = viewMode == 0,
                        onClick = { viewMode = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {
                            if (viewMode == 0) {
                                Icon(Icons.Default.ViewAgenda, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    ) {
                        Text("List Harian (Mobile)", style = MaterialTheme.typography.labelMedium)
                    }
                    SegmentedButton(
                        selected = viewMode == 1,
                        onClick = { viewMode = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {
                            if (viewMode == 1) {
                                Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    ) {
                        Text("Matriks Tabel (Grid)", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    ) { innerPadding ->
        if (viewMode == 0) {
            // MODE 0: LIST HARIAN (Sangat Nyaman di HP)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Hari Tabs
                if (activeHariList.isNotEmpty()) {
                    val currentTabIndex = activeHariList.indexOfFirst { it.id == selectedHariId }
                        .let { if (it >= 0) it else 0 }
                        .coerceIn(0, activeHariList.size - 1)

                    ScrollableTabRow(
                        selectedTabIndex = currentTabIndex,
                        edgePadding = 16.dp,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        activeHariList.forEach { hari ->
                            val filledCount = activeJamList.count { jam ->
                                !jam.isIstirahat && !jam.isSholatJumat && jadwalSlotMap.containsKey("${hari.id}_${jam.jpKode}")
                            }
                            Tab(
                                selected = hari.id == selectedHariId,
                                onClick = { selectedHariId = hari.id },
                                text = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = hari.namaHari,
                                            fontWeight = if (hari.id == selectedHariId) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = "$filledCount JP",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (hari.id == selectedHariId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                // Daily JP List
                val selectedHari = activeHariList.find { it.id == selectedHariId } ?: activeHariList.firstOrNull()
                val currentHariName = selectedHari?.namaHari ?: "Hari"

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(activeJamList, key = { it.jpKode }) { jam ->
                        val slotKey = "${selectedHariId}_${jam.jpKode}"
                        val detail = jadwalSlotMap[slotKey]
                        val isNonTeaching = jam.isIstirahat || jam.isSholatJumat

                        if (isNonTeaching) {
                            // Non teaching banner
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (jam.isSholatJumat) Icons.Default.Mosque else Icons.Default.FreeBreakfast,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (jam.isSholatJumat) "Sholat Jum'at" else "Istirahat",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        )
                                        Text(
                                            text = "${jam.jamMulai} - ${jam.jamSelesai}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        } else if (detail != null) {
                            // Filled Slot Card
                            val hexColor = try {
                                Color(android.graphics.Color.parseColor(detail.warnaHex))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = hexColor.copy(alpha = 0.18f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(hexColor)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${jam.jpKode} • ${jam.jamMulai} - ${jam.jamSelesai}",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = hexColor
                                                    )
                                                )
                                            }
                                        }

                                        if (!isReadOnly) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        selectedSlotCell = SlotCellTarget(
                                                            hariId = selectedHariId,
                                                            namaHari = currentHariName,
                                                            jpKode = jam.jpKode,
                                                            kelasId = selectedKelasId,
                                                            namaKelas = selectedKelas?.namaKelas ?: "",
                                                            existingDetail = detail
                                                        )
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Ubah Slot",
                                                        modifier = Modifier.size(18.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        onClearJadwalSlot(selectedHariId, jam.jpKode, selectedKelasId)
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Hapus Slot",
                                                        modifier = Modifier.size(18.dp),
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = "${detail.mapelKode} - ${detail.namaMapel}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = detail.namaGuru,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = detail.namaRuangan,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }

                                    if (detail.hasConflict) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = detail.conflictMessages.firstOrNull() ?: "Terdeteksi bentrok jadwal",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Empty Slot Card (Easy 1-tap assign)
                            OutlinedCard(
                                onClick = {
                                    if (!isReadOnly) {
                                        selectedSlotCell = SlotCellTarget(
                                            hariId = selectedHariId,
                                            namaHari = currentHariName,
                                            jpKode = jam.jpKode,
                                            kelasId = selectedKelasId,
                                            namaKelas = selectedKelas?.namaKelas ?: "",
                                            existingDetail = null
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${jam.jpKode} (${jam.jamMulai} - ${jam.jamSelesai})",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isReadOnly) "Slot Kosong" else "Ketuk untuk Memilih Mapel & Guru",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isReadOnly) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    if (!isReadOnly) {
                                        FilledTonalIconButton(
                                            onClick = {
                                                selectedSlotCell = SlotCellTarget(
                                                    hariId = selectedHariId,
                                                    namaHari = currentHariName,
                                                    jpKode = jam.jpKode,
                                                    kelasId = selectedKelasId,
                                                    namaKelas = selectedKelas?.namaKelas ?: "",
                                                    existingDetail = null
                                                )
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Isi Slot")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // MODE 1: MATRIKS TABEL (Grid View untuk Tablet/Landscape)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState())
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Header Row: Days
                        Row {
                            // Top Left Corner (Time Column Header)
                            Box(
                                modifier = Modifier
                                    .size(width = 80.dp, height = 44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "JAM / HARI",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Hari Columns
                            activeHariList.forEach { h ->
                                Box(
                                    modifier = Modifier
                                        .size(width = 140.dp, height = 44.dp)
                                        .padding(start = 6.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = h.namaHari.uppercase(),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Rows: Time Slots x Days
                        activeJamList.forEach { jam ->
                            Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                // Left Time Column
                                Box(
                                    modifier = Modifier
                                        .size(width = 80.dp, height = 76.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (jam.isIstirahat || jam.isSholatJumat)
                                                MaterialTheme.colorScheme.tertiaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = jam.jpKode,
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${jam.jamMulai}\n${jam.jamSelesai}",
                                            style = MaterialTheme.typography.labelSmall,
                                            textAlign = TextAlign.Center,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Day Cells
                                activeHariList.forEach { hari ->
                                    val slotKey = "${hari.id}_${jam.jpKode}"
                                    val detail = jadwalSlotMap[slotKey]

                                    MatrixCell(
                                        detail = detail,
                                        isIstirahat = jam.isIstirahat || jam.isSholatJumat,
                                        isReadOnly = isReadOnly,
                                        onClick = {
                                            if (!jam.isIstirahat && !jam.isSholatJumat && !isReadOnly) {
                                                selectedSlotCell = SlotCellTarget(
                                                    hariId = hari.id,
                                                    namaHari = hari.namaHari,
                                                    jpKode = jam.jpKode,
                                                    kelasId = selectedKelasId,
                                                    namaKelas = selectedKelas?.namaKelas ?: "",
                                                    existingDetail = detail
                                                )
                                            }
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

    // Cell Slot Editor Dialog
    if (selectedSlotCell != null) {
        SlotEditorDialog(
            target = selectedSlotCell!!,
            guruList = guruList,
            mapelList = mapelList,
            ruanganList = ruanganList,
            onDismiss = { selectedSlotCell = null },
            onSave = { hId, jKode, kId, gId, mKode, rId ->
                onSaveJadwalSlot(hId, jKode, kId, gId, mKode, rId)
                selectedSlotCell = null
            },
            onClear = { hId, jKode, kId ->
                onClearJadwalSlot(hId, jKode, kId)
                selectedSlotCell = null
            }
        )
    }

    if (showConfirmClearAll) {
        AlertDialog(
            onDismissRequest = { showConfirmClearAll = false },
            title = { Text("Kosongkan Seluruh Jadwal") },
            text = { Text("Apakah Anda yakin ingin mengosongkan seluruh tabel jadwal di semua kelas?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAllJadwal()
                        showConfirmClearAll = false
                    }
                ) {
                    Text("Kosongkan", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearAll = false }) { Text("Batal") }
            }
        )
    }

    if (showPrintDialog) {
        Dialog(onDismissRequest = { showPrintDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CETAK JADWAL PELAJARAN",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = "Pilih format dokumen siap cetak PDF",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showPrintDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Option 1: Jadwal Kelas Ini (Landscape A4)
                    Surface(
                        onClick = {
                            showPrintDialog = false
                            if (selectedKelas != null) {
                                val file = PdfScheduleExporter.exportJadwalKelasLandscapePdf(
                                    context = context,
                                    kelas = selectedKelas,
                                    jadwalDetails = jadwalDetails,
                                    mapelList = mapelList,
                                    guruList = guruList,
                                    hariList = hariList,
                                    jamList = jamList,
                                    settingsMap = settingsMap
                                )
                                PdfScheduleExporter.openOrSharePdf(
                                    context,
                                    file,
                                    "Jadwal Pelajaran Kelas ${selectedKelas.namaKelas} (Landscape)"
                                )
                            } else {
                                Toast.makeText(context, "Pilih kelas terlebih dahulu", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Class,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Cetak Kelas ${selectedKelas?.namaKelas ?: ""} (Landscape)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = "LANDSCAPE",
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                                Text(
                                    text = "Format A4 Horizontal lebar, nama mapel & guru jelas",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 2: Matriks Master Jadwal Induk (Landscape A4)
                    Surface(
                        onClick = {
                            showPrintDialog = false
                            Toast.makeText(context, "Menyiapkan Matriks Master Landscape...", Toast.LENGTH_SHORT).show()
                            val file = PdfScheduleExporter.exportMasterMatrixPdf(
                                context = context,
                                kelasList = kelasList,
                                jadwalDetails = jadwalDetails,
                                mapelList = mapelList,
                                guruList = guruList,
                                hariList = hariList,
                                jamList = jamList,
                                settingsMap = settingsMap
                            )
                            PdfScheduleExporter.openOrSharePdf(context, file, "Matriks Master Jadwal Induk (Landscape)")
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ViewModule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Matriks Master Induk (Landscape)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = "LANDSCAPE",
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                                Text(
                                    text = "Tabel grid master semua rombel kelas & JP mingguan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 3: Jadwal Kelas Ini (Portrait A4)
                    Surface(
                        onClick = {
                            showPrintDialog = false
                            if (selectedKelas != null) {
                                val file = PdfScheduleExporter.exportJadwalKelasPdf(
                                    context = context,
                                    kelas = selectedKelas,
                                    jadwalDetails = jadwalDetails,
                                    mapelList = mapelList,
                                    guruList = guruList,
                                    hariList = hariList,
                                    jamList = jamList,
                                    settingsMap = settingsMap
                                )
                                PdfScheduleExporter.openOrSharePdf(
                                    context,
                                    file,
                                    "Jadwal Pelajaran Kelas ${selectedKelas.namaKelas} (Portrait)"
                                )
                            } else {
                                Toast.makeText(context, "Pilih kelas terlebih dahulu", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Cetak Kelas ${selectedKelas?.namaKelas ?: ""} (Portrait Standar)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Format A4 Vertikal ringkas + tabel kode mapel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrixCell(
    detail: JadwalDetail?,
    isIstirahat: Boolean,
    isReadOnly: Boolean,
    onClick: () -> Unit
) {
    val hexColor = try {
        if (detail != null) Color(android.graphics.Color.parseColor(detail.warnaHex)) else Color.Transparent
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val isConflict = detail?.hasConflict == true

    Box(
        modifier = Modifier
            .size(width = 140.dp, height = 76.dp)
            .padding(start = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isIstirahat -> MaterialTheme.colorScheme.surfaceVariant
                    detail != null -> hexColor.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surface
                }
            )
            .border(
                width = if (isConflict) 2.dp else 1.dp,
                color = when {
                    isConflict -> MaterialTheme.colorScheme.error
                    detail != null -> hexColor
                    else -> MaterialTheme.colorScheme.outlineVariant
                },
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = !isIstirahat && !isReadOnly, onClick = onClick)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isIstirahat) {
            Text(
                text = "ISTIRAHAT",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.outline
            )
        } else if (detail != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${detail.mapelKode} - ${detail.namaMapel}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = hexColor
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isConflict) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Bentrok",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Text(
                    text = detail.namaGuru,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = detail.namaRuangan,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1
                )
            }
        } else {
            Text(
                text = "+ Kosong",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            )
        }
    }
}

data class SlotCellTarget(
    val hariId: Int,
    val namaHari: String,
    val jpKode: String,
    val kelasId: Long,
    val namaKelas: String,
    val existingDetail: JadwalDetail?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotEditorDialog(
    target: SlotCellTarget,
    guruList: List<GuruEntity>,
    mapelList: List<MapelEntity>,
    ruanganList: List<RuanganEntity>,
    onDismiss: () -> Unit,
    onSave: (hariId: Int, jpKode: String, kelasId: Long, guruId: Long, mapelKode: String, ruanganId: Long) -> Unit,
    onClear: (hariId: Int, jpKode: String, kelasId: Long) -> Unit
) {
    var selectedMapel by remember { mutableStateOf(mapelList.find { it.kode == target.existingDetail?.mapelKode } ?: mapelList.firstOrNull()) }
    var selectedGuru by remember {
        mutableStateOf(
            guruList.find { it.id == target.existingDetail?.guruId }
                ?: (selectedMapel?.let { m ->
                    guruList.find { g ->
                        g.mapelUtama.contains(m.namaMapel, ignoreCase = true) ||
                        g.mapelUtama.contains(m.kode, ignoreCase = true)
                    }
                } ?: guruList.firstOrNull())
        )
    }
    var selectedRuangan by remember {
        mutableStateOf(
            ruanganList.find { it.id == target.existingDetail?.ruanganId }
                ?: ruanganList.find { it.namaRuangan.contains(target.namaKelas, ignoreCase = true) }
                ?: ruanganList.firstOrNull()
        )
    }

    var expandedMapel by remember { mutableStateOf(false) }
    var expandedGuru by remember { mutableStateOf(false) }
    var expandedRuangan by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Set Jadwal ${target.jpKode}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Hari ${target.namaHari} • Kelas ${target.namaKelas}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mapel Picker
                ExposedDropdownMenuBox(
                    expanded = expandedMapel,
                    onExpandedChange = { expandedMapel = !expandedMapel }
                ) {
                    OutlinedTextField(
                        value = selectedMapel?.let { "[${it.kode}] ${it.namaMapel}" } ?: "Pilih Mapel",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Mata Pelajaran *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMapel) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = expandedMapel,
                        onDismissRequest = { expandedMapel = false }
                    ) {
                        mapelList.forEach { m ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("[${m.kode}] ${m.namaMapel}", fontWeight = FontWeight.Bold)
                                        Text("${m.kelompok} • ${m.jpPerMinggu} JP/minggu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    selectedMapel = m
                                    // Auto select guru matching this mapel
                                    val matchGuru = guruList.find { g ->
                                        g.mapelUtama.contains(m.namaMapel, ignoreCase = true) ||
                                        g.mapelUtama.contains(m.kode, ignoreCase = true)
                                    }
                                    if (matchGuru != null) {
                                        selectedGuru = matchGuru
                                    }
                                    expandedMapel = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Guru Picker
                ExposedDropdownMenuBox(
                    expanded = expandedGuru,
                    onExpandedChange = { expandedGuru = !expandedGuru }
                ) {
                    OutlinedTextField(
                        value = selectedGuru?.let { "${it.nama} (${it.mapelUtama})" } ?: "Pilih Guru",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Guru Pengampu *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGuru) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = expandedGuru,
                        onDismissRequest = { expandedGuru = false }
                    ) {
                        guruList.forEach { g ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(g.nama, fontWeight = FontWeight.Bold)
                                        Text("${g.mapelUtama} • ${g.status} (Maks ${g.maxJp} JP)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    selectedGuru = g
                                    expandedGuru = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Ruangan Picker
                ExposedDropdownMenuBox(
                    expanded = expandedRuangan,
                    onExpandedChange = { expandedRuangan = !expandedRuangan }
                ) {
                    OutlinedTextField(
                        value = selectedRuangan?.let { "${it.namaRuangan} (${it.jenisRuangan})" } ?: "Pilih Ruangan",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ruangan Kelas *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRuangan) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = expandedRuangan,
                        onDismissRequest = { expandedRuangan = false }
                    ) {
                        ruanganList.forEach { r ->
                            DropdownMenuItem(
                                text = { Text("${r.namaRuangan} (${r.jenisRuangan})") },
                                onClick = {
                                    selectedRuangan = r
                                    expandedRuangan = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (target.existingDetail != null) {
                        TextButton(
                            onClick = {
                                onClear(target.hariId, target.jpKode, target.kelasId)
                            }
                        ) {
                            Text("Kosongkan Slot", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row {
                        TextButton(onClick = onDismiss) { Text("Batal") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (selectedMapel != null && selectedGuru != null && selectedRuangan != null) {
                                    onSave(
                                        target.hariId,
                                        target.jpKode,
                                        target.kelasId,
                                        selectedGuru!!.id,
                                        selectedMapel!!.kode,
                                        selectedRuangan!!.id
                                    )
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Simpan Slot")
                        }
                    }
                }
            }
        }
    }
}
