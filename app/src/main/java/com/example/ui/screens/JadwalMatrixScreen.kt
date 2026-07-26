package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.*
import com.example.domain.validator.ScheduleConflict

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
    activeRole: String,
    onSaveJadwalSlot: (hariId: Int, jpKode: String, kelasId: Long, guruId: Long, mapelKode: String, ruanganId: Long) -> Unit,
    onClearJadwalSlot: (hariId: Int, jpKode: String, kelasId: Long) -> Unit,
    onClearAllJadwal: () -> Unit
) {
    var selectedKelasId by remember(kelasList) { mutableLongStateOf(kelasList.firstOrNull()?.id ?: 1L) }
    var selectedSlotCell by remember { mutableStateOf<SlotCellTarget?>(null) }
    var showConfirmClearAll by remember { mutableStateOf(false) }

    val activeHariList = remember(hariList) { hariList.filter { it.isAktif } }
    val activeJamList = remember(jamList) { jamList.sortedBy { it.urutan } }

    val selectedKelas = kelasList.find { it.id == selectedKelasId }
    val isReadOnly = activeRole == "Kepala Madrasah"

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
                    Column {
                        Text(
                            text = "MATRIKS JADWAL PELAJARAN",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "Pilih Rombel Kelas untuk melihat & mengedit jadwal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!isReadOnly) {
                        OutlinedButton(
                            onClick = { showConfirmClearAll = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                            label = { Text("Kelas ${k.namaKelas}") },
                            leadingIcon = if (k.id == selectedKelasId) {
                                { Icon(Icons.Default.Class, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Matrix Grid Table View
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
    var selectedGuru by remember { mutableStateOf(guruList.find { it.id == target.existingDetail?.guruId } ?: guruList.firstOrNull()) }
    var selectedRuangan by remember { mutableStateOf(ruanganList.find { it.id == target.existingDetail?.ruanganId } ?: ruanganList.firstOrNull()) }

    var expandedMapel by remember { mutableStateOf(false) }
    var expandedGuru by remember { mutableStateOf(false) }
    var expandedRuangan by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Set Jadwal Slot ${target.jpKode}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Hari ${target.namaHari} • Kelas ${target.namaKelas}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mapel Picker
                ExposedDropdownMenuBox(
                    expanded = expandedMapel,
                    onExpandedChange = { expandedMapel = !expandedMapel }
                ) {
                    OutlinedTextField(
                        value = selectedMapel?.let { "${it.kode} - ${it.namaMapel}" } ?: "Pilih Mapel",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Mata Pelajaran *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMapel) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedMapel,
                        onDismissRequest = { expandedMapel = false }
                    ) {
                        mapelList.forEach { m ->
                            DropdownMenuItem(
                                text = { Text("${m.kode} - ${m.namaMapel} (${m.jpPerMinggu} JP)") },
                                onClick = {
                                    selectedMapel = m
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
                        value = selectedGuru?.nama ?: "Pilih Guru",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Guru Pengampu *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGuru) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedGuru,
                        onDismissRequest = { expandedGuru = false }
                    ) {
                        guruList.forEach { g ->
                            DropdownMenuItem(
                                text = { Text("${g.nama} (${g.mapelUtama})") },
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
                        value = selectedRuangan?.namaRuangan ?: "Pilih Ruangan",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ruangan Kelas *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRuangan) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
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
                            }
                        ) {
                            Text("Simpan Slot")
                        }
                    }
                }
            }
        }
    }
}
