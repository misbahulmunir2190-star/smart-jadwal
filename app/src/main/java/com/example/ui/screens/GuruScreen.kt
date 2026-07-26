package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.GuruEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuruScreen(
    guruList: List<GuruEntity>,
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

    val filteredList = remember(guruList, searchQuery, filterStatus) {
        guruList.filter { g ->
            val matchesSearch = g.nama.contains(searchQuery, ignoreCase = true) ||
                    g.nip.contains(searchQuery, ignoreCase = true) ||
                    g.mapelUtama.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (filterStatus) {
                "Semua" -> true
                "Sertifikasi" -> g.isSertifikasi
                else -> g.status.equals(filterStatus, ignoreCase = true)
            }
            matchesSearch && matchesFilter
        }
    }

    val isReadOnly = activeRole == "Kepala Madrasah"

    Scaffold(
        floatingActionButton = {
            if (!isReadOnly) {
                FloatingActionButton(
                    onClick = {
                        selectedGuruForEdit = null
                        showFormDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Guru")
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
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cari Guru berdasarkan Nama, NIP, Mapel...") },
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

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val statuses = listOf("Semua", "PNS", "PPPK", "GTT", "Honor", "Sertifikasi")
                statuses.forEach { st ->
                    FilterChip(
                        selected = filterStatus == st,
                        onClick = { onFilterStatusChange(st) },
                        label = { Text(st, style = MaterialTheme.typography.bodySmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Menampilkan ${filteredList.size} Guru",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { guru ->
                    GuruCardItem(
                        guru = guru,
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

    // Form Dialog
    if (showFormDialog) {
        GuruFormDialog(
            guru = selectedGuruForEdit,
            onDismiss = { showFormDialog = false },
            onSave = { updated ->
                onSaveGuru(updated)
                showFormDialog = false
            }
        )
    }

    // Detail Dialog
    if (selectedGuruForDetail != null) {
        GuruDetailDialog(
            guru = selectedGuruForDetail!!,
            onDismiss = { selectedGuruForDetail = null }
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
    isReadOnly: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = guru.nama,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (guru.isSertifikasi) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Sertifikasi",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = "NIP: ${guru.nip} • Status: ${guru.status}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SuggestionChip(
                        onClick = { },
                        label = { Text(guru.mapelUtama, style = MaterialTheme.typography.labelSmall) }
                    )
                    SuggestionChip(
                        onClick = { },
                        label = { Text("Max ${guru.maxJp} JP", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            if (!isReadOnly) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun GuruDetailDialog(
    guru: GuruEntity,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
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
                        text = "Detail Guru",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                DetailItem("Nama Lengkap", guru.nama)
                DetailItem("NIP", guru.nip)
                DetailItem("NUPTK", guru.nuptk)
                DetailItem("Jenis Kelamin", if (guru.jk == "L") "Laki-laki" else "Perempuan")
                DetailItem("Status Kepegawaian", guru.status)
                DetailItem("Mapel Utama", guru.mapelUtama)
                DetailItem("Beban Maksimal JP", "${guru.maxJp} JP / Minggu")
                DetailItem("Tugas Tambahan / Wali", guru.tugasTambahan)
                DetailItem("Sertifikasi Guru", if (guru.isSertifikasi) "Sudah Sertifikasi" else "Belum Sertifikasi")
                DetailItem("Email", guru.email.ifEmpty { "-" })
                DetailItem("No. HP / WhatsApp", guru.hp.ifEmpty { "-" })

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tutup")
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
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
private fun GuruFormDialog(
    guru: GuruEntity?,
    onDismiss: () -> Unit,
    onSave: (GuruEntity) -> Unit
) {
    var nama by remember { mutableStateOf(guru?.nama ?: "") }
    var nip by remember { mutableStateOf(guru?.nip ?: "") }
    var nuptk by remember { mutableStateOf(guru?.nuptk ?: "") }
    var jk by remember { mutableStateOf(guru?.jk ?: "L") }
    var status by remember { mutableStateOf(guru?.status ?: "PNS") }
    var mapelUtama by remember { mutableStateOf(guru?.mapelUtama ?: "") }
    var maxJp by remember { mutableStateOf(guru?.maxJp?.toString() ?: "24") }
    var tugasTambahan by remember { mutableStateOf(guru?.tugasTambahan ?: "") }
    var isSertifikasi by remember { mutableStateOf(guru?.isSertifikasi ?: false) }
    var email by remember { mutableStateOf(guru?.email ?: "") }
    var hp by remember { mutableStateOf(guru?.hp ?: "") }

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
                Text(
                    text = if (guru == null) "Tambah Guru Baru" else "Edit Data Guru",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text("Nama Guru Lengkap *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nip,
                    onValueChange = { nip = it },
                    label = { Text("NIP") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nuptk,
                    onValueChange = { nuptk = it },
                    label = { Text("NUPTK") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

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

                OutlinedTextField(
                    value = status,
                    onValueChange = { status = it },
                    label = { Text("Status Kepegawaian (PNS / PPPK / GTT / Honor)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = mapelUtama,
                    onValueChange = { mapelUtama = it },
                    label = { Text("Mapel Utama *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = maxJp,
                    onValueChange = { maxJp = it },
                    label = { Text("Maksimal Beban JP / Minggu *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = tugasTambahan,
                    onValueChange = { tugasTambahan = it },
                    label = { Text("Tugas Tambahan / Wali Kelas") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isSertifikasi,
                        onCheckedChange = { isSertifikasi = it }
                    )
                    Text("Sudah Lulus Sertifikasi Guru")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
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

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (nama.isNotBlank() && mapelUtama.isNotBlank()) {
                                onSave(
                                    GuruEntity(
                                        id = guru?.id ?: 0L,
                                        nama = nama,
                                        nip = nip.ifBlank { "-" },
                                        nuptk = nuptk.ifBlank { "-" },
                                        jk = jk,
                                        status = status.ifBlank { "GTT" },
                                        mapelUtama = mapelUtama,
                                        maxJp = maxJp.toIntOrNull() ?: 24,
                                        tugasTambahan = tugasTambahan.ifBlank { "-" },
                                        isSertifikasi = isSertifikasi,
                                        email = email,
                                        hp = hp
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
