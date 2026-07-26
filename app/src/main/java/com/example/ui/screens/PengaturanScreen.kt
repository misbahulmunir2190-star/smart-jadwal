package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.PengaturanEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PengaturanScreen(
    pengaturanList: List<PengaturanEntity>,
    auditLogs: List<AuditLogEntity>,
    activeRole: String,
    onRoleChange: (String) -> Unit,
    onSavePengaturan: (key: String, value: String) -> Unit,
    onResetSampleData: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Profil & Tahun Pelajaran, 1: Hak Akses (Role), 2: Audit Log, 3: Backup & Restore

    val settingsMap = remember(pengaturanList) {
        pengaturanList.associate { it.key to it.value }
    }

    var namaMadrasah by remember(settingsMap) { mutableStateOf(settingsMap["nama_madrasah"] ?: "MTs Negeri 1 Model") }
    var semester by remember(settingsMap) { mutableStateOf(settingsMap["semester"] ?: "Ganjil") }
    var tahunPelajaran by remember(settingsMap) { mutableStateOf(settingsMap["tahun_pelajaran"] ?: "2025/2026") }
    var durasiJp by remember(settingsMap) { mutableStateOf(settingsMap["durasi_jp"] ?: "40 Menit") }
    var jamMasuk by remember(settingsMap) { mutableStateOf(settingsMap["jam_masuk"] ?: "07.00") }
    var jamPulang by remember(settingsMap) { mutableStateOf(settingsMap["jam_pulang"] ?: "13.20") }

    val roles = listOf("Administrator", "Operator Madrasah", "Kepala Madrasah")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Profil") },
                icon = { Icon(Icons.Default.School, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Hak Akses") },
                icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Audit Log") },
                icon = { Icon(Icons.Default.History, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Backup") },
                icon = { Icon(Icons.Default.Backup, contentDescription = null) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "PENGATURAN MADRASAH & TAHUN PELAJARAN",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = namaMadrasah,
                            onValueChange = {
                                namaMadrasah = it
                                onSavePengaturan("nama_madrasah", it)
                            },
                            label = { Text("Nama Madrasah") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = semester,
                            onValueChange = {
                                semester = it
                                onSavePengaturan("semester", it)
                            },
                            label = { Text("Semester (Ganjil / Genap)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = tahunPelajaran,
                            onValueChange = {
                                tahunPelajaran = it
                                onSavePengaturan("tahun_pelajaran", it)
                            },
                            label = { Text("Tahun Pelajaran") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = durasiJp,
                            onValueChange = {
                                durasiJp = it
                                onSavePengaturan("durasi_jp", it)
                            },
                            label = { Text("Durasi 1 JP") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = jamMasuk,
                                onValueChange = {
                                    jamMasuk = it
                                    onSavePengaturan("jam_masuk", it)
                                },
                                label = { Text("Jam Masuk") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = jamPulang,
                                onValueChange = {
                                    jamPulang = it
                                    onSavePengaturan("jam_pulang", it)
                                },
                                label = { Text("Jam Pulang") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        } else if (selectedTab == 1) {
            Column(modifier = Modifier.fillMaxSize()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ROLE MANAGEMENT / HAK AKSES",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Pilih peran pengguna saat ini. 'Kepala Madrasah' adalah mode baca-saja (Read-Only) untuk pengawasan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        roles.forEach { role ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (activeRole == role) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = role,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = when (role) {
                                                "Administrator" -> "Akses Penuh: Master data, pengaturan, AI Generator"
                                                "Operator Madrasah" -> "Akses Operasional: CRUD master & susun jadwal"
                                                else -> "Akses Pengawasan: Hanya melihat & mencetak jadwal (Read-Only)"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    RadioButton(
                                        selected = activeRole == role,
                                        onClick = { onRoleChange(role) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedTab == 2) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "AUDIT LOGS (${auditLogs.size} Aktivitas Terakhir)",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(auditLogs) { log ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = log.aksi,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = log.userRole,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = log.keterangan,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Text(
                                    text = dateFormat.format(Date(log.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Backup & Restore
            Column(modifier = Modifier.fillMaxSize()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "BACKUP, RESTORE & PEMULIHAN DATA",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Gunakan tombol di bawah ini jika terjadi ketidaksesuaian data atau ingin mengembalikan contoh data madrasah awal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onResetSampleData,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset & Pulihkan Data Contoh MTs Standard")
                        }
                    }
                }
            }
        }
    }
}
