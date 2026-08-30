package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.local.entity.MapelEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapelScreen(
    mapelList: List<MapelEntity>,
    guruList: List<GuruEntity>,
    searchQuery: String,
    filterKelompok: String,
    activeRole: String,
    onSearchQueryChange: (String) -> Unit,
    onFilterKelompokChange: (String) -> Unit,
    onSaveMapel: (MapelEntity, Boolean) -> Unit,
    onDeleteMapel: (MapelEntity) -> Unit
) {
    var showFormDialog by remember { mutableStateOf(false) }
    var selectedMapelForEdit by remember { mutableStateOf<MapelEntity?>(null) }
    var mapelToDelete by remember { mutableStateOf<MapelEntity?>(null) }

    val filteredList = remember(mapelList, searchQuery, filterKelompok) {
        mapelList.filter { m ->
            val matchesSearch = m.namaMapel.contains(searchQuery, ignoreCase = true) ||
                    m.kode.contains(searchQuery, ignoreCase = true)

            val matchesFilter = if (filterKelompok == "Semua") true else m.kelompok.equals(filterKelompok, ignoreCase = true)
            matchesSearch && matchesFilter
        }
    }

    val isReadOnly = activeRole == "Kepala Madrasah"

    Scaffold(
        floatingActionButton = {
            if (!isReadOnly) {
                FloatingActionButton(
                    onClick = {
                        selectedMapelForEdit = null
                        showFormDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Mapel")
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cari Mapel berdasarkan Kode atau Nama...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Kelompok Filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val kelompokList = listOf("Semua", "Kelompok A", "Kelompok B", "Muatan Lokal")
                kelompokList.forEach { klp ->
                    FilterChip(
                        selected = filterKelompok == klp,
                        onClick = { onFilterKelompokChange(klp) },
                        label = { Text(klp, style = MaterialTheme.typography.bodySmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.kode }) { mapel ->
                    val relatedGuruCount = guruList.count { g ->
                        g.mapelUtama.contains(mapel.namaMapel, ignoreCase = true) ||
                                g.mapelUtama.contains(mapel.kode, ignoreCase = true)
                    }

                    MapelCardItem(
                        mapel = mapel,
                        guruCount = relatedGuruCount,
                        isReadOnly = isReadOnly,
                        onEdit = {
                            selectedMapelForEdit = mapel
                            showFormDialog = true
                        },
                        onDelete = { mapelToDelete = mapel }
                    )
                }
            }
        }
    }

    if (showFormDialog) {
        MapelFormDialog(
            mapel = selectedMapelForEdit,
            onDismiss = { showFormDialog = false },
            onSave = { m, isEdit ->
                onSaveMapel(m, isEdit)
                showFormDialog = false
            }
        )
    }

    if (mapelToDelete != null) {
        AlertDialog(
            onDismissRequest = { mapelToDelete = null },
            title = { Text("Hapus Mata Pelajaran") },
            text = { Text("Apakah Anda yakin ingin menghapus mapel ${mapelToDelete?.namaMapel} (${mapelToDelete?.kode})?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        mapelToDelete?.let { onDeleteMapel(it) }
                        mapelToDelete = null
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { mapelToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun MapelCardItem(
    mapel: MapelEntity,
    guruCount: Int,
    isReadOnly: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val hexColor = try {
        Color(android.graphics.Color.parseColor(mapel.warnaHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
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
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(hexColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mapel.kode,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mapel.namaMapel,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${mapel.kelompok} • ${mapel.jpPerMinggu} JP/Minggu",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$guruCount Guru pengampu terdaftar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
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
private fun MapelFormDialog(
    mapel: MapelEntity?,
    onDismiss: () -> Unit,
    onSave: (MapelEntity, Boolean) -> Unit
) {
    val isEdit = mapel != null
    var kode by remember { mutableStateOf(mapel?.kode ?: "") }
    var namaMapel by remember { mutableStateOf(mapel?.namaMapel ?: "") }
    var kelompok by remember { mutableStateOf(mapel?.kelompok ?: "Kelompok A") }
    var jpPerMinggu by remember { mutableStateOf(mapel?.jpPerMinggu?.toString() ?: "2") }
    var warnaHex by remember { mutableStateOf(mapel?.warnaHex ?: "#1E88E5") }

    val presetColors = listOf("#0D47A1", "#1B5E20", "#BF360C", "#4A148C", "#006064", "#B71C1C", "#1565C0", "#2E7D32")

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
                Text(
                    text = if (isEdit) "Edit Mata Pelajaran" else "Tambah Mata Pelajaran",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = kode,
                    onValueChange = { kode = it },
                    label = { Text("Kode Mapel (e.g. QH, AA, FK) *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isEdit,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = namaMapel,
                    onValueChange = { namaMapel = it },
                    label = { Text("Nama Mata Pelajaran *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = kelompok,
                    onValueChange = { kelompok = it },
                    label = { Text("Kelompok (Kelompok A / B / Muatan Lokal)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = jpPerMinggu,
                    onValueChange = { jpPerMinggu = it },
                    label = { Text("Beban JP / Minggu *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Pilih Warna Label", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presetColors.forEach { c ->
                        val col = try {
                            Color(android.graphics.Color.parseColor(c))
                        } catch (e: Exception) {
                            Color.Blue
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(col)
                                .clickable { warnaHex = c }
                        ) {
                            if (warnaHex.equals(c, ignoreCase = true)) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
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
                            if (kode.isNotBlank() && namaMapel.isNotBlank()) {
                                onSave(
                                    MapelEntity(
                                        kode = kode.uppercase().trim(),
                                        namaMapel = namaMapel,
                                        kelompok = kelompok,
                                        jpPerMinggu = jpPerMinggu.toIntOrNull() ?: 2,
                                        warnaHex = warnaHex
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
