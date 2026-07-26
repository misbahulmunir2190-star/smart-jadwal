package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.HariEntity
import com.example.data.local.entity.JamEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HariJamScreen(
    hariList: List<HariEntity>,
    jamList: List<JamEntity>,
    activeRole: String,
    onToggleHari: (HariEntity) -> Unit,
    onSaveJam: (JamEntity, Boolean) -> Unit,
    onDeleteJam: (JamEntity) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Hari, 1: Jam Pelajaran
    var showJamDialog by remember { mutableStateOf(false) }
    var selectedJamForEdit by remember { mutableStateOf<JamEntity?>(null) }
    var jamToDelete by remember { mutableStateOf<JamEntity?>(null) }

    val isReadOnly = activeRole == "Kepala Madrasah"

    Scaffold(
        floatingActionButton = {
            if (!isReadOnly && selectedTab == 1) {
                FloatingActionButton(
                    onClick = {
                        selectedJamForEdit = null
                        showJamDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Slot Jam")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Pengaturan Hari (${hariList.count { it.isAktif }} Aktif)") },
                    icon = { Icon(Icons.Default.Today, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Jam Pelajaran (${jamList.size} Slot)") },
                    icon = { Icon(Icons.Default.AccessTime, contentDescription = null) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(hariList) { hari ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (hari.isAktif) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (hari.isAktif) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Hari ${hari.namaHari}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = if (hari.isAktif) "Status: Aktif Mengajar" else "Status: Libur / Non-Aktif",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (!isReadOnly) {
                                    Switch(
                                        checked = hari.isAktif,
                                        onCheckedChange = { onToggleHari(hari) }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(jamList) { jam ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (jam.isIstirahat || jam.isSholatJumat) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (jam.isIstirahat) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = jam.jpKode,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = "${jam.jamMulai} - ${jam.jamSelesai}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        val label = when {
                                            jam.isSholatJumat -> "Istirahat / Sholat Jumat"
                                            jam.isIstirahat -> "Istirahat Siswa"
                                            else -> "Jam Pelajaran Aktif"
                                        }
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (!isReadOnly) {
                                    Row {
                                        IconButton(onClick = {
                                            selectedJamForEdit = jam
                                            showJamDialog = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                                        }
                                        IconButton(onClick = { jamToDelete = jam }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
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

    if (showJamDialog) {
        JamFormDialog(
            jam = selectedJamForEdit,
            onDismiss = { showJamDialog = false },
            onSave = { updated, isEdit ->
                onSaveJam(updated, isEdit)
                showJamDialog = false
            }
        )
    }

    if (jamToDelete != null) {
        AlertDialog(
            onDismissRequest = { jamToDelete = null },
            title = { Text("Hapus Slot Jam") },
            text = { Text("Hapus slot jam ${jamToDelete?.jpKode}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        jamToDelete?.let { onDeleteJam(it) }
                        jamToDelete = null
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { jamToDelete = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun JamFormDialog(
    jam: JamEntity?,
    onDismiss: () -> Unit,
    onSave: (JamEntity, Boolean) -> Unit
) {
    val isEdit = jam != null
    var jpKode by remember { mutableStateOf(jam?.jpKode ?: "") }
    var jamMulai by remember { mutableStateOf(jam?.jamMulai ?: "") }
    var jamSelesai by remember { mutableStateOf(jam?.jamSelesai ?: "") }
    var isIstirahat by remember { mutableStateOf(jam?.isIstirahat ?: false) }
    var isSholatJumat by remember { mutableStateOf(jam?.isSholatJumat ?: false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (isEdit) "Edit Slot Jam" else "Tambah Slot Jam",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = jpKode,
                    onValueChange = { jpKode = it },
                    label = { Text("Kode JP (e.g. JP1, JP2, IST1) *") },
                    enabled = !isEdit,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = jamMulai,
                    onValueChange = { jamMulai = it },
                    label = { Text("Jam Mulai (e.g. 07.00) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = jamSelesai,
                    onValueChange = { jamSelesai = it },
                    label = { Text("Jam Selesai (e.g. 07.40) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isIstirahat, onCheckedChange = { isIstirahat = it })
                    Text("Slot Jam Istirahat")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isSholatJumat, onCheckedChange = { isSholatJumat = it })
                    Text("Slot Jam Sholat / Istirahat Jumat")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (jpKode.isNotBlank() && jamMulai.isNotBlank()) {
                                onSave(
                                    JamEntity(
                                        jpKode = jpKode.trim().uppercase(),
                                        jamMulai = jamMulai.trim(),
                                        jamSelesai = jamSelesai.trim(),
                                        isIstirahat = isIstirahat,
                                        isSholatJumat = isSholatJumat,
                                        urutan = 1
                                    ),
                                    isEdit
                                )
                            }
                        }
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}
