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
import com.example.data.local.entity.GuruEntity
import com.example.data.local.entity.KelasEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KelasScreen(
    kelasList: List<KelasEntity>,
    guruList: List<GuruEntity>,
    activeRole: String,
    onSaveKelas: (KelasEntity) -> Unit,
    onDeleteKelas: (KelasEntity) -> Unit
) {
    var showFormDialog by remember { mutableStateOf(false) }
    var selectedKelasForEdit by remember { mutableStateOf<KelasEntity?>(null) }
    var kelasToDelete by remember { mutableStateOf<KelasEntity?>(null) }

    val isReadOnly = activeRole == "Kepala Madrasah"

    Scaffold(
        floatingActionButton = {
            if (!isReadOnly) {
                FloatingActionButton(
                    onClick = {
                        selectedKelasForEdit = null
                        showFormDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Kelas")
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
            Text(
                text = "DAFTAR ROMBEL KELAS MTs (${kelasList.size} Kelas)",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(kelasList, key = { it.id }) { kelas ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = kelas.namaKelas,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Kelas ${kelas.namaKelas} (Tingkat ${kelas.tingkat})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Wali Kelas: ${kelas.waliKelasNama}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Jumlah Siswa: ${kelas.jumlahSiswa} Orang",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            if (!isReadOnly) {
                                IconButton(onClick = {
                                    selectedKelasForEdit = kelas
                                    showFormDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { kelasToDelete = kelas }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFormDialog) {
        KelasFormDialog(
            kelas = selectedKelasForEdit,
            guruList = guruList,
            onDismiss = { showFormDialog = false },
            onSave = { updated ->
                onSaveKelas(updated)
                showFormDialog = false
            }
        )
    }

    if (kelasToDelete != null) {
        AlertDialog(
            onDismissRequest = { kelasToDelete = null },
            title = { Text("Hapus Rombel Kelas") },
            text = { Text("Apakah Anda yakin ingin menghapus Rombel ${kelasToDelete?.namaKelas}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        kelasToDelete?.let { onDeleteKelas(it) }
                        kelasToDelete = null
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { kelasToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KelasFormDialog(
    kelas: KelasEntity?,
    guruList: List<GuruEntity>,
    onDismiss: () -> Unit,
    onSave: (KelasEntity) -> Unit
) {
    var namaKelas by remember { mutableStateOf(kelas?.namaKelas ?: "") }
    var tingkat by remember { mutableStateOf(kelas?.tingkat?.toString() ?: "7") }
    var jumlahSiswa by remember { mutableStateOf(kelas?.jumlahSiswa?.toString() ?: "32") }

    var selectedGuru by remember { mutableStateOf(guruList.find { it.id == kelas?.waliKelasGuruId }) }
    var expandedGuruPicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (kelas == null) "Tambah Rombel Kelas" else "Edit Kelas",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = namaKelas,
                    onValueChange = { namaKelas = it },
                    label = { Text("Nama Kelas (e.g. 7A, 8B) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = tingkat,
                    onValueChange = { tingkat = it },
                    label = { Text("Tingkat (7 / 8 / 9) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = jumlahSiswa,
                    onValueChange = { jumlahSiswa = it },
                    label = { Text("Jumlah Siswa *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedGuruPicker,
                    onExpandedChange = { expandedGuruPicker = !expandedGuruPicker }
                ) {
                    OutlinedTextField(
                        value = selectedGuru?.nama ?: kelas?.waliKelasNama ?: "Pilih Wali Kelas",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Wali Kelas") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGuruPicker) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                    )

                    ExposedDropdownMenu(
                        expanded = expandedGuruPicker,
                        onDismissRequest = { expandedGuruPicker = false }
                    ) {
                        guruList.forEach { g ->
                            DropdownMenuItem(
                                text = { Text("${g.nama} (${g.mapelUtama})") },
                                onClick = {
                                    selectedGuru = g
                                    expandedGuruPicker = false
                                }
                            )
                        }
                    }
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
                            if (namaKelas.isNotBlank()) {
                                onSave(
                                    KelasEntity(
                                        id = kelas?.id ?: 0L,
                                        namaKelas = namaKelas.trim(),
                                        tingkat = tingkat.toIntOrNull() ?: 7,
                                        waliKelasGuruId = selectedGuru?.id ?: kelas?.waliKelasGuruId,
                                        waliKelasNama = selectedGuru?.nama ?: kelas?.waliKelasNama ?: "-",
                                        jumlahSiswa = jumlahSiswa.toIntOrNull() ?: 32
                                    )
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
