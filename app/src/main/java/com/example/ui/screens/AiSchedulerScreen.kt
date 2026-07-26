package com.example.ui.screens

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.*
import com.example.data.remote.AiSchedulerResult
import com.example.domain.validator.ScheduleConflict

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSchedulerScreen(
    guruList: List<GuruEntity>,
    mapelList: List<MapelEntity>,
    kelasList: List<KelasEntity>,
    hariList: List<HariEntity>,
    jamList: List<JamEntity>,
    ruanganList: List<RuanganEntity>,
    conflicts: List<ScheduleConflict>,
    aiStage: Int,
    isGenerating: Boolean,
    aiResult: AiSchedulerResult?,
    onRunAiScheduler: (stageMode: String) -> Unit,
    onApplyAiResult: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Generator 4 Tahap, 1: Analisis Beban & Saran

    val activeHariCount = hariList.count { it.isAktif }
    val activeJamCount = jamList.count { !it.isIstirahat && !it.isSholatJumat }

    val isDataReady = guruList.isNotEmpty() && mapelList.isNotEmpty() && kelasList.isNotEmpty() && activeHariCount > 0 && activeJamCount > 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Header Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Engine",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GEMINI AI SCHEDULER",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Penyusun Jadwal Otomatis Berbasis Kecerdasan Buatan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Tabs: Generator vs Analysis
        item {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("4-Tahap AI Generator") },
                    icon = { Icon(Icons.Default.PrecisionManufacturing, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Analisis Beban & Explanation") },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = null) }
                )
            }
        }

        if (selectedTab == 0) {
            // Stage 1: Data Validation & Readiness
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "Tahap 1",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Validasi Kesiapan Data Master",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        ReadinessItem("Master Guru", "${guruList.size} Orang Terdaftar", guruList.isNotEmpty())
                        ReadinessItem("Master Mapel", "${mapelList.size} Mata Pelajaran", mapelList.isNotEmpty())
                        ReadinessItem("Master Kelas", "${kelasList.size} Rombel Kelas", kelasList.isNotEmpty())
                        ReadinessItem("Hari Aktif", "$activeHariCount Hari Mengajar", activeHariCount > 0)
                        ReadinessItem("Jam Pelajaran", "$activeJamCount Slot Jam Aktif", activeJamCount > 0)
                        ReadinessItem("Ruangan", "${ruanganList.size} Ruangan", ruanganList.isNotEmpty())
                    }
                }
            }

            // Stage 2 & 3: Run AI Draft & Conflict Repair
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondary
                            ) {
                                Text(
                                    text = "Tahap 2 & 3",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Penyusunan Draf AI & Perbaikan Konflik",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Gemini AI akan membaca seluruh konstrain Guru, Mapel, Kelas, Hari, Jam, dan Ruangan untuk menghasilkan jadwal yang optimal dan bebas bentrok.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isGenerating) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Gemini AI sedang menghitung kombinasi jadwal tanpa bentrok...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onRunAiScheduler("generate") },
                                    modifier = Modifier.weight(1f),
                                    enabled = isDataReady,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Susun Jadwal AI")
                                }

                                OutlinedButton(
                                    onClick = { onRunAiScheduler("fix_conflicts") },
                                    modifier = Modifier.weight(1f),
                                    enabled = isDataReady,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Build, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Perbaiki Bentrok")
                                }
                            }
                        }
                    }
                }
            }

            // Stage 4: Results Review & Apply
            if (aiResult != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (aiResult.success) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.tertiary
                                ) {
                                    Text(
                                        text = "Tahap 4",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Hasil Penyusunan & Review AI",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = aiResult.message,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Tergenerasi ${aiResult.jadwalList.size} slot jadwal.",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = onApplyAiResult,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Terapkan ke Jadwal Utama Madrasah")
                            }
                        }
                    }
                }
            }
        } else {
            // Analysis & Explanation
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "1. ANALISIS BEBAN MENGAJAR GURU",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = aiResult?.workloadAnalysis?.ifEmpty { "Jalankan AI Scheduler untuk melihat analisis beban mengajar guru." }
                                ?: "Jalankan AI Scheduler untuk melihat analisis beban mengajar guru.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "2. SARAN DISTRIBUSI JAM PELAJARAN (JP)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = aiResult?.jpAdvice?.ifEmpty { "Mapel keagamaan (Al-Qur'an Hadits, Aqidah Akhlak, Fikih, SKI, B. Arab) disarankan ditempatkan pada jam-jam pagi hari." }
                                ?: "Mapel keagamaan (Al-Qur'an Hadits, Aqidah Akhlak, Fikih, SKI, B. Arab) disarankan ditempatkan pada jam-jam pagi hari.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "3. PENJELASAN LOGIKA PENYUSUNAN AI",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = aiResult?.aiExplanation?.ifEmpty { "AI menggunakan Smart Constraint Satisfaction Algorithm untuk memastikan tidak ada guru, kelas, atau ruangan yang bentrok di jam dan hari yang sama." }
                                ?: "AI menggunakan Smart Constraint Satisfaction Algorithm untuk memastikan tidak ada guru, kelas, atau ruangan yang bentrok di jam dan hari yang sama.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadinessItem(label: String, detail: String, isReady: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (isReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
        }
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
