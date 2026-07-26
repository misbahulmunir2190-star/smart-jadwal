package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.*
import com.example.data.remote.AiSchedulerResult
import com.example.data.remote.GeminiSchedulerService
import com.example.data.repository.SchedulerRepository
import com.example.domain.validator.ScheduleConflict
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppScreen(val title: String, val iconName: String) {
    DASHBOARD("Dashboard", "Dashboard"),
    GURU("Guru", "People"),
    MAPEL("Mata Pelajaran", "MenuBook"),
    KELAS("Kelas", "Class"),
    HARI("Hari", "Today"),
    JAM("Jam Pelajaran", "AccessTime"),
    RUANGAN("Ruangan", "MeetingRoom"),
    JADWAL("Jadwal", "CalendarMonth"),
    AI_SCHEDULER("AI Scheduler", "AutoAwesome"),
    LAPORAN("Laporan", "Assessment"),
    PENGATURAN("Pengaturan", "Settings")
}

data class MainUiState(
    val currentScreen: AppScreen = AppScreen.DASHBOARD,
    val activeRole: String = "Operator Madrasah", // "Administrator", "Operator Madrasah", "Kepala Madrasah"
    val isLoading: Boolean = false,
    val snackbarMessage: String? = null,

    // Search & Filter
    val guruSearchQuery: String = "",
    val guruFilterStatus: String = "Semua",
    val mapelSearchQuery: String = "",
    val mapelFilterKelompok: String = "Semua",
    val jadwalFilterKelasId: Long? = null,
    val jadwalFilterGuruId: Long? = null,
    val jadwalFilterHariId: Int? = null,

    // AI Generation Stage State
    val aiStage: Int = 1, // 1: Validasi Data, 2: Draf AI, 3: Perbaikan Bentrok, 4: Selesai & Review
    val isAiGenerating: Boolean = false,
    val aiResult: AiSchedulerResult? = null
)

class MainViewModel(private val repository: SchedulerRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.prepopulateDefaultDataIfEmpty()
        }
    }

    val guruList: StateFlow<List<GuruEntity>> = repository.allGuru
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mapelList: StateFlow<List<MapelEntity>> = repository.allMapel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val kelasList: StateFlow<List<KelasEntity>> = repository.allKelas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hariList: StateFlow<List<HariEntity>> = repository.allHari
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val jamList: StateFlow<List<JamEntity>> = repository.allJam
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ruanganList: StateFlow<List<RuanganEntity>> = repository.allRuangan
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val enrichedJadwalPair: StateFlow<Pair<List<JadwalDetail>, List<ScheduleConflict>>> =
        repository.enrichedJadwalDetails
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(emptyList(), emptyList()))

    val pengaturanList: StateFlow<List<PengaturanEntity>> = repository.allPengaturan
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.allAuditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun setRole(role: String) {
        _uiState.update { it.copy(activeRole = role) }
        showSnackbar("Role diubah menjadi $role")
        viewModelScope.launch {
            repository.savePengaturan("role_aktif", role)
        }
    }

    fun showSnackbar(msg: String) {
        _uiState.update { it.copy(snackbarMessage = msg) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    // --- Search & Filter Setters ---
    fun setGuruSearchQuery(query: String) {
        _uiState.update { it.copy(guruSearchQuery = query) }
    }

    fun setGuruFilterStatus(status: String) {
        _uiState.update { it.copy(guruFilterStatus = status) }
    }

    fun setMapelSearchQuery(query: String) {
        _uiState.update { it.copy(mapelSearchQuery = query) }
    }

    fun setMapelFilterKelompok(kelompok: String) {
        _uiState.update { it.copy(mapelFilterKelompok = kelompok) }
    }

    fun setJadwalFilterKelas(kelasId: Long?) {
        _uiState.update { it.copy(jadwalFilterKelasId = kelasId) }
    }

    fun setJadwalFilterGuru(guruId: Long?) {
        _uiState.update { it.copy(jadwalFilterGuruId = guruId) }
    }

    fun setJadwalFilterHari(hariId: Int?) {
        _uiState.update { it.copy(jadwalFilterHariId = hariId) }
    }

    // --- CRUD ACTIONS ---
    fun saveGuru(guru: GuruEntity) {
        viewModelScope.launch {
            if (guru.id == 0L) {
                repository.insertGuru(guru)
                showSnackbar("Berhasil menambahkan Guru: ${guru.nama}")
            } else {
                repository.updateGuru(guru)
                showSnackbar("Berhasil memperbarui data Guru: ${guru.nama}")
            }
        }
    }

    fun deleteGuru(guru: GuruEntity) {
        viewModelScope.launch {
            repository.deleteGuru(guru)
            showSnackbar("Guru ${guru.nama} berhasil dihapus")
        }
    }

    fun saveMapel(mapel: MapelEntity, isEdit: Boolean) {
        viewModelScope.launch {
            if (!isEdit) {
                repository.insertMapel(mapel)
                showSnackbar("Berhasil menambahkan Mapel: ${mapel.namaMapel}")
            } else {
                repository.updateMapel(mapel)
                showSnackbar("Berhasil memperbarui Mapel: ${mapel.namaMapel}")
            }
        }
    }

    fun deleteMapel(mapel: MapelEntity) {
        viewModelScope.launch {
            repository.deleteMapel(mapel)
            showSnackbar("Mapel ${mapel.namaMapel} berhasil dihapus")
        }
    }

    fun saveKelas(kelas: KelasEntity) {
        viewModelScope.launch {
            if (kelas.id == 0L) {
                repository.insertKelas(kelas)
                showSnackbar("Berhasil menambahkan Kelas: ${kelas.namaKelas}")
            } else {
                repository.updateKelas(kelas)
                showSnackbar("Berhasil memperbarui Kelas: ${kelas.namaKelas}")
            }
        }
    }

    fun deleteKelas(kelas: KelasEntity) {
        viewModelScope.launch {
            repository.deleteKelas(kelas)
            showSnackbar("Kelas ${kelas.namaKelas} berhasil dihapus")
        }
    }

    fun toggleHari(hari: HariEntity) {
        viewModelScope.launch {
            val updated = hari.copy(isAktif = !hari.isAktif)
            repository.updateHari(updated)
            showSnackbar("Hari ${hari.namaHari} ${if (updated.isAktif) "diaktifkan" else "dinonaktifkan"}")
        }
    }

    fun saveJam(jam: JamEntity, isEdit: Boolean) {
        viewModelScope.launch {
            if (!isEdit) {
                repository.insertJam(jam)
                showSnackbar("Berhasil menambahkan slot jam: ${jam.jpKode}")
            } else {
                repository.updateJam(jam)
                showSnackbar("Berhasil memperbarui slot jam: ${jam.jpKode}")
            }
        }
    }

    fun deleteJam(jam: JamEntity) {
        viewModelScope.launch {
            repository.deleteJam(jam)
            showSnackbar("Slot jam ${jam.jpKode} dihapus")
        }
    }

    fun saveRuangan(ruangan: RuanganEntity) {
        viewModelScope.launch {
            if (ruangan.id == 0L) {
                repository.insertRuangan(ruangan)
                showSnackbar("Berhasil menambahkan Ruangan: ${ruangan.namaRuangan}")
            } else {
                repository.updateRuangan(ruangan)
                showSnackbar("Berhasil memperbarui Ruangan: ${ruangan.namaRuangan}")
            }
        }
    }

    fun deleteRuangan(ruangan: RuanganEntity) {
        viewModelScope.launch {
            repository.deleteRuangan(ruangan)
            showSnackbar("Ruangan ${ruangan.namaRuangan} dihapus")
        }
    }

    // --- MANUAL JADWAL EDIT ---
    fun saveJadwalSlot(
        hariId: Int,
        jpKode: String,
        kelasId: Long,
        guruId: Long,
        mapelKode: String,
        ruanganId: Long
    ) {
        viewModelScope.launch {
            repository.saveJadwalSlot(hariId, jpKode, kelasId, guruId, mapelKode, ruanganId)
            showSnackbar("Jadwal slot $jpKode berhasil disimpan")
        }
    }

    fun clearJadwalSlot(hariId: Int, jpKode: String, kelasId: Long) {
        viewModelScope.launch {
            repository.clearJadwalSlot(hariId, jpKode, kelasId)
            showSnackbar("Slot $jpKode dikosongkan")
        }
    }

    fun clearAllJadwal() {
        viewModelScope.launch {
            repository.clearAllJadwal()
            showSnackbar("Seluruh jadwal berhasil dikosongkan")
        }
    }

    fun savePengaturan(key: String, value: String) {
        viewModelScope.launch {
            repository.savePengaturan(key, value)
            showSnackbar("Pengaturan $key disimpan")
        }
    }

    // --- AI GENERATOR 4 STAGES ---
    fun runAiScheduleGenerator(stageMode: String = "generate") {
        viewModelScope.launch {
            _uiState.update { it.copy(isAiGenerating = true, aiStage = 2) }

            val currentJadwal = repository.allJadwal.first()
            val result = GeminiSchedulerService.generateScheduleWithAi(
                guruList = guruList.value,
                mapelList = mapelList.value,
                kelasList = kelasList.value,
                hariList = hariList.value,
                jamList = jamList.value,
                ruanganList = ruanganList.value,
                existingJadwal = currentJadwal,
                mode = stageMode
            )

            _uiState.update {
                it.copy(
                    isAiGenerating = false,
                    aiStage = 4,
                    aiResult = result
                )
            }

            if (result.success) {
                showSnackbar("Proses AI Scheduler selesai: ${result.message}")
            } else {
                showSnackbar("Perhatian: ${result.message}")
            }
        }
    }

    fun applyAiResultToDatabase() {
        val result = _uiState.value.aiResult ?: return
        if (result.jadwalList.isEmpty()) return

        viewModelScope.launch {
            val entities = result.jadwalList.mapIndexed { idx, item ->
                JadwalEntity(
                    id = idx.toLong() + 1,
                    hariId = item.hariId,
                    jpKode = item.jpKode,
                    kelasId = item.kelasId,
                    guruId = item.guruId,
                    mapelKode = item.mapelKode,
                    ruanganId = item.ruanganId
                )
            }
            repository.saveGeneratedJadwalList(entities)
            showSnackbar("${entities.size} slot jadwal AI berhasil diterapkan ke sistem!")
            _uiState.update { it.copy(currentScreen = AppScreen.JADWAL) }
        }
    }

    fun resetSampleData() {
        viewModelScope.launch {
            repository.clearAllJadwal()
            repository.prepopulateDefaultDataIfEmpty()
            showSnackbar("Data contoh Madrasah berhasil dipulihkan!")
        }
    }
}

class MainViewModelFactory(private val repository: SchedulerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
