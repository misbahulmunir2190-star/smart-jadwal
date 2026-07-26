package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanScreen(
    jadwalDetails: List<JadwalDetail>,
    guruList: List<GuruEntity>,
    kelasList: List<KelasEntity>,
    mapelList: List<MapelEntity>,
    hariList: List<HariEntity>,
    ruanganList: List<RuanganEntity>,
    namaMadrasah: String,
    tahunPelajaran: String,
    semester: String
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableIntStateOf(0) } // 0: Per Guru, 1: Per Kelas, 2: Per Hari, 3: Per Mapel, 4: Per Ruangan

    var filterGuruId by remember { mutableLongStateOf(guruList.firstOrNull()?.id ?: 1L) }
    var filterKelasId by remember { mutableLongStateOf(kelasList.firstOrNull()?.id ?: 1L) }
    var filterHariId by remember { mutableIntStateOf(hariList.firstOrNull()?.id ?: 1) }
    var filterMapelKode by remember { mutableStateOf(mapelList.firstOrNull()?.kode ?: "QH") }
    var filterRuanganId by remember { mutableLongStateOf(ruanganList.firstOrNull()?.id ?: 1L) }

    val categories = listOf("Per Guru", "Per Kelas", "Per Hari", "Per Mapel", "Per Ruangan")

    val filteredJadwal = remember(selectedCategory, filterGuruId, filterKelasId, filterHariId, filterMapelKode, filterRuanganId, jadwalDetails) {
        when (selectedCategory) {
            0 -> jadwalDetails.filter { it.guruId == filterGuruId }
            1 -> jadwalDetails.filter { it.kelasId == filterKelasId }
            2 -> jadwalDetails.filter { it.hariId == filterHariId }
            3 -> jadwalDetails.filter { it.mapelKode == filterMapelKode }
            4 -> jadwalDetails.filter { it.ruanganId == filterRuanganId }
            else -> jadwalDetails
        }.sortedWith(compareBy({ it.hariId }, { it.jpKode }))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LAPORAN & CETAK JADWAL",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = "$namaMadrasah • $tahunPelajaran ($semester)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    val reportText = buildString {
                        append("--- LAPORAN JADWAL PELAJARAN ---\n")
                        append("Madrasah: $namaMadrasah\n")
                        append("Tahun Pelajaran: $tahunPelajaran ($semester)\n")
                        append("Kategori Laporan: ${categories[selectedCategory]}\n\n")

                        filteredJadwal.forEach { j ->
                            append("[Hari ${j.namaHari} ${j.jpKode} (${j.jamMulai}-${j.jamSelesai})]\n")
                            append("Kelas: ${j.namaKelas} | Mapel: ${j.namaMapel}\n")
                            append("Guru: ${j.namaGuru} | Ruangan: ${j.namaRuangan}\n\n")
                        }
                    }

                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, reportText)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Ekspor / Cetak Laporan Jadwal")
                    context.startActivity(shareIntent)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Bagikan/Cetak")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ekspor / Cetak")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Category Tabs
        SecondaryScrollableTabRow(selectedTabIndex = selectedCategory) {
            categories.forEachIndexed { idx, cat ->
                Tab(
                    selected = selectedCategory == idx,
                    onClick = { selectedCategory = idx },
                    text = { Text(cat) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sub-filter dropdown/chips depending on category
        when (selectedCategory) {
            0 -> {
                ExposedDropdownFilter(
                    label = "Pilih Guru",
                    selectedValue = guruList.find { it.id == filterGuruId }?.nama ?: "",
                    options = guruList.map { Pair(it.id, it.nama) },
                    onSelect = { filterGuruId = it }
                )
            }
            1 -> {
                ExposedDropdownFilter(
                    label = "Pilih Kelas",
                    selectedValue = kelasList.find { it.id == filterKelasId }?.let { "Kelas ${it.namaKelas}" } ?: "",
                    options = kelasList.map { Pair(it.id, "Kelas ${it.namaKelas}") },
                    onSelect = { filterKelasId = it }
                )
            }
            2 -> {
                ExposedDropdownFilter(
                    label = "Pilih Hari",
                    selectedValue = hariList.find { it.id == filterHariId }?.namaHari ?: "",
                    options = hariList.map { Pair(it.id.toLong(), it.namaHari) },
                    onSelect = { filterHariId = it.toInt() }
                )
            }
            3 -> {
                ExposedDropdownFilter(
                    label = "Pilih Mata Pelajaran",
                    selectedValue = mapelList.find { it.kode == filterMapelKode }?.namaMapel ?: "",
                    options = mapelList.map { Pair(0L, "${it.kode} - ${it.namaMapel}") },
                    onSelect = { idx ->
                        mapelList.getOrNull(idx.toInt())?.let { filterMapelKode = it.kode }
                    }
                )
            }
            4 -> {
                ExposedDropdownFilter(
                    label = "Pilih Ruangan",
                    selectedValue = ruanganList.find { it.id == filterRuanganId }?.namaRuangan ?: "",
                    options = ruanganList.map { Pair(it.id, it.namaRuangan) },
                    onSelect = { filterRuanganId = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Total ${filteredJadwal.size} Slot Terjadwal dalam Kategori Ini",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredJadwal) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Hari ${item.namaHari} • ${item.jpKode} (${item.jamMulai} - ${item.jamSelesai})",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Text(
                                text = "Ruangan: ${item.namaRuangan}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${item.mapelKode} - ${item.namaMapel}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Text(
                            text = "Guru: ${item.namaGuru} | Kelas: ${item.namaKelas}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdownFilter(
    label: String,
    selectedValue: String,
    options: List<Pair<Long, String>>,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEachIndexed { idx, opt ->
                DropdownMenuItem(
                    text = { Text(opt.second) },
                    onClick = {
                        onSelect(if (opt.first != 0L) opt.first else idx.toLong())
                        expanded = false
                    }
                )
            }
        }
    }
}
