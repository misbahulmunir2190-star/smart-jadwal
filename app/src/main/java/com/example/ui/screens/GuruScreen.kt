package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.GuruEntity
import com.example.data.local.entity.JadwalDetail
import com.example.data.local.entity.MapelEntity
import com.example.domain.model.AnalisisSertifikasi
import com.example.domain.model.SertifikasiAnalyzer
import com.example.domain.model.TugasTambahanCatalog
import com.example.domain.model.TugasTambahanOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuruScreen(
    guruList: List<GuruEntity>,
    mapelList: List<MapelEntity> = emptyList(),
    jadwalDetails: List<JadwalDetail> = emptyList(),
    searchQuery: String,
    filterStatus: String,
    activeRole: String,
    onSearchQueryChange: (String) -> Unit,
    onFilterStatusChange: (String) -> Unit,
    onSaveGuru: (GuruEntity) -> Unit,
    onDeleteGuru: (GuruEntity) -> Unit
) {
    var showFormDialog by remember { mutableStateOf(false) }
    var selectedGuruForEdit by remember { mutableStateOf<GuruEntity?>(null) }
    var selectedGuruForDetail by remember { mutableStateOf<GuruEntity?>(null) }
    var guruToDelete by remember { mutableStateOf<GuruEntity?>(null) }
    var showCertificationSummaryDialog by remember { mutableStateOf(false) }

    // Pre-calculate analysis for all teachers
    val guruAnalysisMap = remember(guruList, jadwalDetails) {
        guruList.associate { guru ->
            guru.id to SertifikasiAnalyzer.hitungKelayakan(guru, jadwalDetails)
        }
    }

    val totalSertifikasi = remember(guruList) { guruList.count { it.isSertifikasi } }
    val totalLayak = remember(guruList, guruAnalysisMap) {
        guruList.count { it.isSertifikasi && (guruAnalysisMap[it.id]?.isLayak == true) }
    }
    val totalKurangJp = remember(guruList, guruAnalysisMap) {
        guruList.count { it.isSertifikasi && (guruAnalysisMap[it.id]?.isLayak == false) }
    }

    val filteredList = remember(guruList, searchQuery, filterStatus, guruAnalysisMap) {
        guruList.filter { g ->
            val matchesSearch = g.nama.contains(searchQuery, ignoreCase = true) ||
                    g.nip.contains(searchQuery, ignoreCase = true) ||
                    g.mapelUtama.contains(searchQuery, ignoreCase = true) ||
                    g.mapelTambahan.contains(searchQuery, ignoreCase = true) ||
                    g.tugasTambahan.contains(searchQuery, ignoreCase = true)

            val analysis = guruAnalysisMap[g.id]
            val matchesFilter = when (filterStatus) {
                "Semua" -> true
                "Sertifikasi" -> g.isSertifikasi
                "Non-Sertifikasi" -> !g.isSertifikasi
                "Layak 24 JP" -> g.isSertifikasi && analysis?.isLayak == true
                "Kurang JP (<24)" -> g.isSertifikasi && analysis?.isLayak == false
                else -> g.status.equals(filterStatus, ignoreCase = true)
            }
            matchesSearch && matchesFilter
        }
    }

    val isReadOnly = activeRole == "Kepala Madrasah"

    Scaffold(
        floatingActionButton = {
            if (!isReadOnly) {
                ExtendedFloatingActionButton(
                    onClick = {
                        selectedGuruForEdit = null
                        showFormDialog = true
                    },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("Tambah Pendidik") },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Analytics Card: Kelayakan Sertifikasi 24 JP
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCertificationSummaryDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Analisis Kelayakan Sertifikasi (24 JP)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "PMA 89/2021",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Total Guru
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${guruList.size}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Total Guru",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        // Layak 24 JP
                        Surface(
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$totalLayak / $totalSertifikasi",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = "Layak TPG (>=24 JP)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF1B5E20),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Kurang Jam
                        Surface(
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (totalKurangJp > 0) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$totalKurangJp Guru",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (totalKurangJp > 0) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (totalKurangJp > 0) "Kurang Jam" else "Semua Terpenuhi",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (totalKurangJp > 0) Color(0xFFB71C1C) else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cari nama, NIP, mapel utama, mapel tambahan...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    "Semua",
                    "Layak 24 JP",
                    "Kurang JP (<24)",
                    "Sertifikasi",
                    "Non-Sertifikasi",
                    "PNS",
                    "PPPK",
                    "Non PNS"
                )
                filters.forEach { st ->
                    FilterChip(
                        selected = filterStatus == st,
                        onClick = { onFilterStatusChange(st) },
                        label = { Text(st, style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = if (st == "Layak 24 JP" && filterStatus == st) {
                            { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else if (st == "Kurang JP (<24)" && filterStatus == st) {
                            { Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Menampilkan ${filteredList.size} Pendidik",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                Text(
                    text = "Klik kartu untuk detail beban",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { guru ->
                    val analysis = guruAnalysisMap[guru.id] ?: SertifikasiAnalyzer.hitungKelayakan(guru, jadwalDetails)
                    GuruCardItem(
                        guru = guru,
                        analysis = analysis,
                        isReadOnly = isReadOnly,
                        onClick = { selectedGuruForDetail = guru },
                        onEdit = {
                            selectedGuruForEdit = guru
                            showFormDialog = true
                        },
                        onDelete = { guruToDelete = guru }
                    )
                }
            }
        }
    }

    // Form Dialog (Tambah & Edit Guru + Mapel Tambahan + Tugas Tambahan Ekuivalensi)
    if (showFormDialog) {
        GuruFormDialog(
            guru = selectedGuruForEdit,
            availableMapelList = mapelList,
            onDismiss = { showFormDialog = false },
            onSave = { updated ->
                onSaveGuru(updated)
                showFormDialog = false
            }
        )
    }

    // Detail & Certification Analysis Dialog
    if (selectedGuruForDetail != null) {
        val guru = selectedGuruForDetail!!
        val analysis = guruAnalysisMap[guru.id] ?: SertifikasiAnalyzer.hitungKelayakan(guru, jadwalDetails)
        val guruScheduleSlots = jadwalDetails.filter { it.guruId == guru.id }

        GuruDetailDialog(
            guru = guru,
            analysis = analysis,
            scheduleSlots = guruScheduleSlots,
            onDismiss = { selectedGuruForDetail = null },
            onEdit = {
                selectedGuruForDetail = null
                selectedGuruForEdit = guru
                showFormDialog = true
            }
        )
    }

    // Summary Dialog
    if (showCertificationSummaryDialog) {
        CertificationSummaryDialog(
            guruList = guruList,
            analysisMap = guruAnalysisMap,
            onDismiss = { showCertificationSummaryDialog = false }
        )
    }

    // Confirm Delete Dialog
    if (guruToDelete != null) {
        AlertDialog(
            onDismissRequest = { guruToDelete = null },
            title = { Text("Hapus Data Guru") },
            text = { Text("Apakah Anda yakin ingin menghapus data guru ${guruToDelete?.nama}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        guruToDelete?.let { onDeleteGuru(it) }
                        guruToDelete = null
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { guruToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun GuruCardItem(
    guru: GuruEntity,
    analysis: AnalisisSertifikasi,
    isReadOnly: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (guru.isSertifikasi) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.secondaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (guru.jk == "P") Icons.Default.Face else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (guru.isSertifikasi) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = guru.nama,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (guru.isSertifikasi) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Sertifikasi",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = "NIP: ${guru.nip} • ${guru.status}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!isReadOnly) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subjects & Duties Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Mapel Utama
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${guru.mapelUtama} (Utama)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Mapel Tambahan (if any)
                if (guru.mapelTambahan.isNotBlank()) {
                    val addMapels = guru.mapelTambahan.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    addMapels.forEach { addm ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+ $addm",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                // Tugas Tambahan Ekuivalensi
                if (guru.tugasTambahan.isNotBlank() && guru.tugasTambahan != "-") {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFFF3E0)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AssignmentInd, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFE65100))
                            Spacer(modifier = Modifier.width(4.dp))
                            val eqText = if (guru.ekuivalensiJp > 0) " (+${guru.ekuivalensiJp} JP)" else ""
                            Text(
                                text = "${guru.tugasTambahan}$eqText",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Workload & 24 JP Progress Meter
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Beban: ${analysis.jpTatapMuka} JP Tatap Muka + ${analysis.jpEkuivalensi} JP Ekuivalen",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            !guru.isSertifikasi -> MaterialTheme.colorScheme.surfaceVariant
                            analysis.isLayak -> Color(0xFFE8F5E9)
                            else -> Color(0xFFFFEBEE)
                        }
                    ) {
                        Text(
                            text = when {
                                !guru.isSertifikasi -> "Non-Sertifikasi (${analysis.totalJpBeban} JP)"
                                analysis.isLayak -> "✅ Layak (${analysis.totalJpBeban}/24 JP)"
                                else -> "⚠️ Kurang ${analysis.kekuranganJp} JP"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = when {
                                !guru.isSertifikasi -> MaterialTheme.colorScheme.outline
                                analysis.isLayak -> Color(0xFF2E7D32)
                                else -> Color(0xFFC62828)
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { (analysis.totalJpBeban.toFloat() / 24f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = when {
                        !guru.isSertifikasi -> MaterialTheme.colorScheme.primary
                        analysis.isLayak -> Color(0xFF2E7D32)
                        else -> Color(0xFFE53935)
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GuruFormDialog(
    guru: GuruEntity?,
    availableMapelList: List<MapelEntity>,
    onDismiss: () -> Unit,
    onSave: (GuruEntity) -> Unit
) {
    var nama by remember { mutableStateOf(guru?.nama ?: "") }
    var nip by remember { mutableStateOf(guru?.nip?.takeIf { it != "-" } ?: "") }
    var nuptk by remember { mutableStateOf(guru?.nuptk?.takeIf { it != "-" } ?: "") }
    var jk by remember { mutableStateOf(guru?.jk ?: "L") }
    var status by remember { mutableStateOf(guru?.status ?: "Non PNS") }
    var mapelUtama by remember { mutableStateOf(guru?.mapelUtama ?: "") }

    // Mapel Tambahan list state
    var selectedMapelTambahanList by remember {
        mutableStateOf<List<String>>(
            guru?.mapelTambahan?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        )
    }
    var customMapelTambahanInput by remember { mutableStateOf("") }

    var maxJp by remember { mutableStateOf(guru?.maxJp?.toString() ?: "24") }

    // Tugas Tambahan selection
    var selectedTugasOption by remember {
        mutableStateOf(
            TugasTambahanCatalog.findOptionByName(guru?.tugasTambahan ?: "") ?:
            TugasTambahanCatalog.RECOGNIZED_OPTIONS.last()
        )
    }
    var customTugasTambahanNama by remember { mutableStateOf(guru?.tugasTambahan ?: "") }
    var ekuivalensiJpInput by remember { mutableStateOf(guru?.ekuivalensiJp?.toString() ?: "0") }
    var isCustomTugas by remember { mutableStateOf(selectedTugasOption.id == "NON_TUGAS" && (guru?.tugasTambahan?.isNotBlank() == true && guru.tugasTambahan != "-")) }

    var isSertifikasi by remember { mutableStateOf(guru?.isSertifikasi ?: true) }
    var email by remember { mutableStateOf(guru?.email ?: "") }
    var hp by remember { mutableStateOf(guru?.hp?.takeIf { it != "-" } ?: "") }

    var isTugasDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
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
                    Text(
                        text = if (guru == null) "Tambah Guru & Mapel" else "Edit Guru & Mapel Tambahan",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }

                Text(
                    text = "Konfigurasi mapel utama, mapel tambahan, dan tugas tambahan ekuivalen Simpatika",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // SECTION 1: Identitas
                Text("1. Identitas Guru", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text("Nama Lengkap & Gelar *") },
                    placeholder = { Text("Contoh: DESY ARIANI PUTRI, S.Pd") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nip,
                        onValueChange = { nip = it },
                        label = { Text("NIP") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = nuptk,
                        onValueChange = { nuptk = it },
                        label = { Text("NUPTK") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Jenis Kelamin", style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = jk == "L", onClick = { jk = "L" })
                    Text("Laki-laki")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = jk == "P", onClick = { jk = "P" })
                    Text("Perempuan")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Status Kepegawaian Chips
                Text("Status Kepegawaian", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val statusOptions = listOf("Non PNS", "PNS", "PPPK", "GTT", "Honorer", "Guru Tetap Yayasan")
                    statusOptions.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 2: MAPEL UTAMA & MAPEL TAMBAHAN
                Text("2. Mata Pelajaran yang Diampu", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Mapel tambahan akan otomatis disinkronkan ke AI saat generate jadwal untuk pemenuhan 24 JP.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Mapel Utama Selector
                OutlinedTextField(
                    value = mapelUtama,
                    onValueChange = { mapelUtama = it },
                    label = { Text("Mapel Utama *") },
                    placeholder = { Text("Pilih / Ketik Mapel Utama") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                    }
                )

                // Quick suggestions for Mapel Utama
                if (availableMapelList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Saran Mapel Utama:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        items(availableMapelList) { mapel ->
                            SuggestionChip(
                                onClick = { mapelUtama = mapel.namaMapel },
                                label = { Text(mapel.namaMapel, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mapel Tambahan Multi-Select Chips
                Text("Mapel Tambahan (Selain Mapel Utama):", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                Spacer(modifier = Modifier.height(4.dp))

                if (availableMapelList.isNotEmpty()) {
                    Text("Pilih mapel tambahan yang sanggup diajarkan:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(4.dp))
                    // Grid of chips from available mapel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableMapelList.filter { !it.namaMapel.equals(mapelUtama, ignoreCase = true) }.forEach { mapel ->
                            val isSelected = selectedMapelTambahanList.contains(mapel.namaMapel)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val current = selectedMapelTambahanList.toMutableList()
                                    if (isSelected) {
                                        current.remove(mapel.namaMapel)
                                    } else {
                                        current.add(mapel.namaMapel)
                                    }
                                    selectedMapelTambahanList = current
                                },
                                label = { Text(mapel.namaMapel, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Custom Mapel Tambahan Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = customMapelTambahanInput,
                        onValueChange = { customMapelTambahanInput = it },
                        placeholder = { Text("Ketik mapel tambahan lain...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val trimmed = customMapelTambahanInput.trim()
                            if (trimmed.isNotBlank() && !selectedMapelTambahanList.contains(trimmed)) {
                                selectedMapelTambahanList = selectedMapelTambahanList + trimmed
                                customMapelTambahanInput = ""
                            }
                        },
                        enabled = customMapelTambahanInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah")
                    }
                }

                // Display selected mapel tambahan tags
                if (selectedMapelTambahanList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Mapel Tambahan Terpilih:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        selectedMapelTambahanList.forEach { item ->
                            InputChip(
                                selected = true,
                                onClick = {
                                    selectedMapelTambahanList = selectedMapelTambahanList.filter { it != item }
                                },
                                label = { Text(item, style = MaterialTheme.typography.labelSmall) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "Hapus", modifier = Modifier.size(14.dp))
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 3: TUGAS TAMBAHAN YANG DIAKUI & EKUIVALENSI 24 JP
                Text("3. Tugas Tambahan & Ekuivalensi JP", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Sesuai regulasi Simpatika / Kemenag untuk pemenuhan beban kerja 24 JP/minggu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Tugas Tambahan Dropdown
                ExposedDropdownMenuBox(
                    expanded = isTugasDropdownExpanded,
                    onExpandedChange = { isTugasDropdownExpanded = !isTugasDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = if (isCustomTugas) "Tugas Tambahan Lainnya (Manual)" else selectedTugasOption.nama,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Pilih Tugas Tambahan yang Diakui *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTugasDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = isTugasDropdownExpanded,
                        onDismissRequest = { isTugasDropdownExpanded = false }
                    ) {
                        TugasTambahanCatalog.RECOGNIZED_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(option.nama, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                        Text(
                                            text = "${option.deskripsi} • Kategori: ${option.kategori}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                },
                                onClick = {
                                    selectedTugasOption = option
                                    isCustomTugas = false
                                    customTugasTambahanNama = option.nama
                                    ekuivalensiJpInput = option.ekuivalensiJp.toString()
                                    isTugasDropdownExpanded = false
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = {
                                Text("✍️ Tugas Tambahan Lainnya (Input Manual & JP Khusus)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            },
                            onClick = {
                                isCustomTugas = true
                                isTugasDropdownExpanded = false
                            }
                        )
                    }
                }

                if (isCustomTugas) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customTugasTambahanNama,
                        onValueChange = { customTugasTambahanNama = it },
                        label = { Text("Nama Tugas Tambahan") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ekuivalensiJpInput,
                        onValueChange = { ekuivalensiJpInput = it },
                        label = { Text("Ekuivalensi JP Diakui") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        supportingText = { Text("Diakui dalam Simpatika") }
                    )

                    OutlinedTextField(
                        value = maxJp,
                        onValueChange = { maxJp = it },
                        label = { Text("Beban Maksimal JP") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        supportingText = { Text("Target mingguan") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 4: SERTIFIKASI & KONTAK
                Text("4. Status Sertifikasi & Kontak", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sertifikasi Pendidik (TPG)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text("Membutuhkan minimal 24 JP beban kerja", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = isSertifikasi,
                            onCheckedChange = { isSertifikasi = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Madrasah / Pribadi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = hp,
                    onValueChange = { hp = it },
                    label = { Text("No. HP / WhatsApp") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (nama.isNotBlank() && mapelUtama.isNotBlank()) {
                                val tugasFinal = if (isCustomTugas) customTugasTambahanNama.ifBlank { "-" } else selectedTugasOption.nama
                                val mapelTambahanFinal = selectedMapelTambahanList.joinToString(", ")
                                val eqFinal = ekuivalensiJpInput.toIntOrNull() ?: 0

                                onSave(
                                    GuruEntity(
                                        id = guru?.id ?: 0L,
                                        nama = nama.trim(),
                                        nip = nip.ifBlank { "-" }.trim(),
                                        nuptk = nuptk.ifBlank { "-" }.trim(),
                                        jk = jk,
                                        status = status.ifBlank { "Non PNS" },
                                        mapelUtama = mapelUtama.trim(),
                                        mapelTambahan = mapelTambahanFinal,
                                        maxJp = maxJp.toIntOrNull() ?: 24,
                                        tugasTambahan = tugasFinal,
                                        ekuivalensiJp = eqFinal,
                                        isSertifikasi = isSertifikasi,
                                        email = email.trim(),
                                        hp = hp.ifBlank { "-" }.trim()
                                    )
                                )
                            }
                        }
                    ) {
                        Text("Simpan Data Pendidik")
                    }
                }
            }
        }
    }
}

@Composable
private fun GuruDetailDialog(
    guru: GuruEntity,
    analysis: AnalisisSertifikasi,
    scheduleSlots: List<JadwalDetail>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
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
                    Column {
                        Text(
                            text = guru.nama,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "NIP: ${guru.nip} • Status: ${guru.status}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // ANALISIS SERTIFIKASI 24 JP HIGHLIGHT
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            !guru.isSertifikasi -> MaterialTheme.colorScheme.surfaceVariant
                            analysis.isLayak -> Color(0xFFE8F5E9)
                            else -> Color(0xFFFFEBEE)
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Status Sertifikasi & Beban Kerja",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = when {
                                    !guru.isSertifikasi -> MaterialTheme.colorScheme.outline
                                    analysis.isLayak -> Color(0xFF2E7D32)
                                    else -> Color(0xFFC62828)
                                }
                            )

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when {
                                    !guru.isSertifikasi -> MaterialTheme.colorScheme.outline
                                    analysis.isLayak -> Color(0xFF2E7D32)
                                    else -> Color(0xFFC62828)
                                }
                            ) {
                                Text(
                                    text = if (analysis.isLayak) "MEMENUHI 24 JP" else if (guru.isSertifikasi) "KURANG JAM" else "NON-SERTIFIKASI",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Breakdown Formula
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "${analysis.jpTatapMuka} JP", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text(text = "Tatap Muka", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Text(text = "+", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.CenterVertically))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "${analysis.jpEkuivalensi} JP", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text(text = "Ekuivalensi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Text(text = "=", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.CenterVertically))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${analysis.totalJpBeban} / 24 JP",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (analysis.isLayak) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                                Text(text = "Total Beban", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Rekomendasi Simpatika: ${analysis.rekomendasi}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Detail Profil
                DetailItem("Mata Pelajaran Utama", guru.mapelUtama)
                DetailItem("Mata Pelajaran Tambahan", guru.mapelTambahan.ifBlank { "(Tidak ada mapel tambahan)" })
                DetailItem("Tugas Tambahan Diakui", "${guru.tugasTambahan} (Ekuivalensi ${guru.ekuivalensiJp} JP)")
                DetailItem("NUPTK", guru.nuptk)
                DetailItem("Beban Maksimal Target", "${guru.maxJp} JP / Minggu")
                DetailItem("Email", guru.email.ifEmpty { "-" })
                DetailItem("No. HP / WA", guru.hp.ifEmpty { "-" })

                Spacer(modifier = Modifier.height(12.dp))

                // Rincian Jadwal Mengajar Tatap Muka
                Text(
                    text = "Jadwal Mengajar Terjadwal (${scheduleSlots.size} JP):",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (scheduleSlots.isEmpty()) {
                    Text(
                        text = "Belum ada jadwal yang dialokasikan. Generate jadwal baru untuk melihat alokasi kelas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    val groupedByClass = scheduleSlots.groupBy { it.namaKelas }
                    groupedByClass.forEach { (kelas, slots) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Kelas $kelas: ${slots.firstOrNull()?.namaMapel ?: "-"}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                )
                                Text(
                                    text = "${slots.size} JP",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Guru")
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tutup")
                    }
                }
            }
        }
    }
}

@Composable
private fun CertificationSummaryDialog(
    guruList: List<GuruEntity>,
    analysisMap: Map<Long, AnalisisSertifikasi>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
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
                    Text(
                        text = "Rekapitulasi Sertifikasi (24 JP)",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }

                Text(
                    text = "Standar Pemenuhan Beban Kerja Guru Madrasah (PMA No. 89 Tahun 2021)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                guruList.filter { it.isSertifikasi }.forEach { guru ->
                    val analysis = analysisMap[guru.id] ?: SertifikasiAnalyzer.hitungKelayakan(guru, emptyList())
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (analysis.isLayak) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = guru.nama,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${analysis.totalJpBeban} / 24 JP",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (analysis.isLayak) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Mapel: ${guru.mapelUtama} • Tugas: ${guru.tugasTambahan} (+${guru.ekuivalensiJp} JP)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tutup Rekapitulasi")
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        )
    }
}
