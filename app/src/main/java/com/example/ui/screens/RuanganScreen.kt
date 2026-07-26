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
import com.example.data.local.entity.RuanganEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuanganScreen(
    ruanganList: List<RuanganEntity>,
    activeRole: String,
    onSaveRuangan: (RuanganEntity) -> Unit,
    onDeleteRuangan: (RuanganEntity) -> Unit
) {
    var showFormDialog by remember { mutableStateOf(false) }
    var selectedRuanganForEdit by remember { mutableStateOf<RuanganEntity?>(null) }
    var ruanganToDelete by remember { mutableStateOf<RuanganEntity?>(null) }

    val isReadOnly = activeRole == "Kepala Madrasah"

    Scaffold(
        floatingActionButton = {
            if (!isReadOnly) {
                FloatingActionButton(
                    onClick = {
                        selectedRuanganForEdit = null
                        showFormDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Ruangan")
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
                text = "DAFTAR RUANGAN MADRASAH (${ruanganList.size} Ruangan)",
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
                items(ruanganList, key = { it.id }) { r ->
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
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MeetingRoom,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = r.namaRuangan,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Jenis: ${r.jenisRuangan} • Kapasitas: ${r.kapasitas} Siswa",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!isReadOnly) {
                                IconButton(onClick = {
                                    selectedRuanganForEdit = r
                                    showFormDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { ruanganToDelete = r }) {
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
        RuanganFormDialog(
            ruangan = selectedRuanganForEdit,
            onDismiss = { showFormDialog = false },
            onSave = { updated ->
                onSaveRuangan(updated)
                showFormDialog = false
            }
        )
    }

    if (ruanganToDelete != null) {
        AlertDialog(
            onDismissRequest = { ruanganToDelete = null },
            title = { Text("Hapus Ruangan") },
            text = { Text("Apakah Anda yakin ingin menghapus ruangan ${ruanganToDelete?.namaRuangan}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        ruanganToDelete?.let { onDeleteRuangan(it) }
                        ruanganToDelete = null
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { ruanganToDelete = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun RuanganFormDialog(
    ruangan: RuanganEntity?,
    onDismiss: () -> Unit,
    onSave: (RuanganEntity) -> Unit
) {
    var namaRuangan by remember { mutableStateOf(ruangan?.namaRuangan ?: "") }
    var kapasitas by remember { mutableStateOf(ruangan?.kapasitas?.toString() ?: "36") }
    var jenisRuangan by remember { mutableStateOf(ruangan?.jenisRuangan ?: "Teori") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (ruangan == null) "Tambah Ruangan" else "Edit Ruangan",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = namaRuangan,
                    onValueChange = { namaRuangan = it },
                    label = { Text("Nama Ruangan *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = kapasitas,
                    onValueChange = { kapasitas = it },
                    label = { Text("Kapasitas Siswa *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = jenisRuangan,
                    onValueChange = { jenisRuangan = it },
                    label = { Text("Jenis Ruangan (Teori / Laboratorium / Lapangan / Aula)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (namaRuangan.isNotBlank()) {
                                onSave(
                                    RuanganEntity(
                                        id = ruangan?.id ?: 0L,
                                        namaRuangan = namaRuangan.trim(),
                                        kapasitas = kapasitas.toIntOrNull() ?: 36,
                                        jenisRuangan = jenisRuangan.ifBlank { "Teori" }
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
