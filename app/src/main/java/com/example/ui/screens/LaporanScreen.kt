package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.*
import com.example.util.PdfScheduleExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanScreen(
    jadwalDetails: List<JadwalDetail>,
    guruList: List<GuruEntity>,
    kelasList: List<KelasEntity>,
    mapelList: List<MapelEntity>,
    hariList: List<HariEntity>,
    jamList: List<JamEntity>,
    ruanganList: List<RuanganEntity>,
    pengaturanList: List<PengaturanEntity>,
    namaMadrasah: String,
    tahunPelajaran: String,
    semester: String
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableIntStateOf(0) } // 0: Per Guru, 1: Per Kelas, 2: Per Hari, 3: Per Mapel, 4: Per Ruangan
    var showPdfMenuDialog by remember { mutableStateOf(false) }

    var filterGuruId by remember { mutableLongStateOf(guruList.firstOrNull()?.id ?: 1L) }
    var filterKelasId by remember { mutableLongStateOf(kelasList.firstOrNull()?.id ?: 1L) }
    var filterHariId by remember { mutableIntStateOf(hariList.firstOrNull()?.id ?: 1) }
    var filterMapelKode by remember { mutableStateOf(mapelList.firstOrNull()?.kode ?: "QH") }
    var filterRuanganId by remember { mutableLongStateOf(ruanganList.firstOrNull()?.id ?: 1L) }

    val settingsMap = remember(pengaturanList, namaMadrasah, tahunPelajaran, semester) {
        val map = pengaturanList.associate { it.key to it.value }.toMutableMap()
        map["nama_madrasah"] = namaMadrasah
        map["tahun_pelajaran"] = tahunPelajaran
        map["semester"] = semester
        map
    }

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
        // Top Header and PDF Export Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "LAPORAN & OUTPUT CETAK JADWAL",
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // PDF Action Button
                Button(
                    onClick = { showPdfMenuDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Cetak PDF")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cetak PDF", fontWeight = FontWeight.Bold)
                }

                // Text Share Action Button
                IconButton(
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
                        val shareIntent = Intent.createChooser(sendIntent, "Ekspor Teks Laporan")
                        context.startActivity(shareIntent)
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Bagikan Teks")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PDF Print Quick Banner (Landscape Highlight)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Print,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Format Cetak Landscape & Resmi",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Matriks Induk, Jadwal Kelas Lebar, & Kartu Beban Guru",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalButton(
                    onClick = {
                        // Quick print Master Landscape
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
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ViewModule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Master Induk")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filter Category Chips
        ScrollableTabRow(
            selectedTabIndex = selectedCategory,
            edgePadding = 0.dp,
            divider = {}
        ) {
            categories.forEachIndexed { index, category ->
                Tab(
                    selected = selectedCategory == index,
                    onClick = { selectedCategory = index },
                    text = {
                        Text(
                            text = category,
                            fontWeight = if (selectedCategory == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sub-filter dropdown/chips depending on category
        when (selectedCategory) {
            0 -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ExposedDropdownFilter(
                            label = "Pilih Guru",
                            selectedValue = guruList.find { it.id == filterGuruId }?.nama ?: "",
                            options = guruList.map { Pair(it.id, it.nama) },
                            onSelect = { filterGuruId = it }
                        )
                    }
                    Button(
                        onClick = {
                            val selectedGuru = guruList.find { it.id == filterGuruId }
                            if (selectedGuru != null) {
                                val file = PdfScheduleExporter.exportJadwalGuruLandscapePdf(
                                    context = context,
                                    guru = selectedGuru,
                                    jadwalDetails = jadwalDetails,
                                    mapelList = mapelList,
                                    kelasList = kelasList,
                                    hariList = hariList,
                                    jamList = jamList,
                                    settingsMap = settingsMap
                                )
                                PdfScheduleExporter.openOrSharePdf(context, file, "Jadwal Mengajar (Landscape) - ${selectedGuru.nama}")
                            } else {
                                Toast.makeText(context, "Pilih guru terlebih dahulu", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cetak Guru")
                    }
                }
            }
            1 -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ExposedDropdownFilter(
                            label = "Pilih Kelas",
                            selectedValue = kelasList.find { it.id == filterKelasId }?.let { "Kelas ${it.namaKelas}" } ?: "",
                            options = kelasList.map { Pair(it.id, "Kelas ${it.namaKelas}") },
                            onSelect = { filterKelasId = it }
                        )
                    }
                    Button(
                        onClick = {
                            val selectedKelas = kelasList.find { it.id == filterKelasId }
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
                                PdfScheduleExporter.openOrSharePdf(context, file, "Jadwal Pelajaran (Landscape) - Kelas ${selectedKelas.namaKelas}")
                            } else {
                                Toast.makeText(context, "Pilih kelas terlebih dahulu", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cetak Kelas")
                    }
                }
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

    // Modal Dialog for PDF Output Options
    if (showPdfMenuDialog) {
        Dialog(onDismissRequest = { showPdfMenuDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(20.dp),
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
                                text = "FORMAT CETAK JADWAL (PDF)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = "Dokumen Resmi Kop Kemenag & Pengesahan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showPdfMenuDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Option 1: Matriks Master Jadwal Induk (Landscape A4)
                    PdfOptionItem(
                        icon = Icons.Default.ViewModule,
                        title = "Matriks Master Jadwal Induk (Landscape)",
                        subtitle = "Format A4 Landscape lebar mencakup seluruh rombel kelas & JP",
                        isHighlight = true,
                        onClick = {
                            showPdfMenuDialog = false
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
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 2: Jadwal Kelas Landscape (Cocok untuk Mading Kelas)
                    val currKelas = kelasList.find { it.id == filterKelasId } ?: kelasList.firstOrNull()
                    PdfOptionItem(
                        icon = Icons.Default.Class,
                        title = "Jadwal Kelas ${currKelas?.namaKelas ?: ""} (Landscape)",
                        subtitle = "Format A4 Landscape lebar dengan ruang nama mapel & guru lega",
                        isHighlight = true,
                        onClick = {
                            showPdfMenuDialog = false
                            if (currKelas != null) {
                                val file = PdfScheduleExporter.exportJadwalKelasLandscapePdf(
                                    context = context,
                                    kelas = currKelas,
                                    jadwalDetails = jadwalDetails,
                                    mapelList = mapelList,
                                    guruList = guruList,
                                    hariList = hariList,
                                    jamList = jamList,
                                    settingsMap = settingsMap
                                )
                                PdfScheduleExporter.openOrSharePdf(context, file, "Jadwal Pelajaran Kelas ${currKelas.namaKelas} (Landscape)")
                            } else {
                                Toast.makeText(context, "Data kelas tidak tersedia", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 3: Buku Jadwal Seluruh Kelas (Landscape Multi-Halaman)
                    PdfOptionItem(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        title = "Buku Jadwal Semua Kelas (Landscape)",
                        subtitle = "Semua rombel kelas dalam format landscape rapi untuk arsip",
                        onClick = {
                            showPdfMenuDialog = false
                            if (kelasList.isNotEmpty()) {
                                Toast.makeText(context, "Menyiapkan PDF buku seluruh kelas...", Toast.LENGTH_SHORT).show()
                                val file = PdfScheduleExporter.exportJadwalSemuaKelasLandscapePdf(
                                    context = context,
                                    kelasList = kelasList,
                                    jadwalDetails = jadwalDetails,
                                    mapelList = mapelList,
                                    guruList = guruList,
                                    hariList = hariList,
                                    jamList = jamList,
                                    settingsMap = settingsMap
                                )
                                PdfScheduleExporter.openOrSharePdf(context, file, "Buku Jadwal Semua Kelas (Landscape)")
                            } else {
                                Toast.makeText(context, "Data kelas kosong", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 4: Jadwal Mengajar Guru Landscape
                    val currGuru = guruList.find { it.id == filterGuruId } ?: guruList.firstOrNull()
                    PdfOptionItem(
                        icon = Icons.Default.Person,
                        title = "Jadwal Mengajar Guru (${currGuru?.nama?.take(16) ?: "Pribadi"})",
                        subtitle = "Format A4 Landscape kalender mingguan + rekap JP & status",
                        onClick = {
                            showPdfMenuDialog = false
                            if (currGuru != null) {
                                val file = PdfScheduleExporter.exportJadwalGuruLandscapePdf(
                                    context = context,
                                    guru = currGuru,
                                    jadwalDetails = jadwalDetails,
                                    mapelList = mapelList,
                                    kelasList = kelasList,
                                    hariList = hariList,
                                    jamList = jamList,
                                    settingsMap = settingsMap
                                )
                                PdfScheduleExporter.openOrSharePdf(context, file, "Jadwal Mengajar (Landscape) - ${currGuru.nama}")
                            } else {
                                Toast.makeText(context, "Data guru kosong", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 5: Rekapitulasi Beban Kerja Guru Landscape
                    PdfOptionItem(
                        icon = Icons.Default.Assessment,
                        title = "Rekap Beban Mengajar Guru (Landscape)",
                        subtitle = "Matriks pembagian jam Senin–Sabtu seluruh tenaga pendidik",
                        onClick = {
                            showPdfMenuDialog = false
                            val file = PdfScheduleExporter.exportRekapBebanGuruLandscapePdf(
                                context = context,
                                guruList = guruList,
                                jadwalDetails = jadwalDetails,
                                mapelList = mapelList,
                                hariList = hariList,
                                settingsMap = settingsMap
                            )
                            PdfScheduleExporter.openOrSharePdf(context, file, "Rekap Beban Mengajar Guru (Landscape)")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isHighlight: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isHighlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
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
                color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isHighlight) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isHighlight) {
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
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
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
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
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
