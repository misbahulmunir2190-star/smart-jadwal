package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfScheduleExporter {

    // A4 Standard Dimensions at 72 dpi (points)
    private const val PAGE_WIDTH_PORTRAIT = 595
    private const val PAGE_HEIGHT_PORTRAIT = 842

    private const val PAGE_WIDTH_LANDSCAPE = 842
    private const val PAGE_HEIGHT_LANDSCAPE = 595

    private const val MARGIN_PORTRAIT = 36f
    private const val MARGIN_LANDSCAPE = 28f

    // Color Palette - Official Madrasah / Kemenag
    private val COLOR_PRIMARY = Color.rgb(6, 95, 70) // Emerald Green 800 (#065F46)
    private val COLOR_PRIMARY_DARK = Color.rgb(4, 120, 87) // Emerald Green 700 (#047857)
    private val COLOR_PRIMARY_LIGHT = Color.rgb(236, 253, 245) // Emerald 50 (#ECFDF5)
    private val COLOR_ACCENT_GOLD = Color.rgb(217, 119, 6) // Amber/Gold (#D97706)
    private val COLOR_ACCENT_BG = Color.rgb(254, 243, 199) // Amber 100 (#FEF3C7)
    private val COLOR_BORDER = Color.rgb(203, 213, 225) // Slate 300 (#CBD5E1)
    private val COLOR_BORDER_DARK = Color.rgb(148, 163, 184) // Slate 400
    private val COLOR_ROW_ALT = Color.rgb(248, 250, 252) // Slate 50 (#F8FAFC)
    private val COLOR_TEXT_MAIN = Color.rgb(15, 23, 42) // Slate 900
    private val COLOR_TEXT_MUTED = Color.rgb(100, 116, 139) // Slate 500
    private val COLOR_SPECIAL_ROW = Color.rgb(240, 253, 250) // Teal 50

    // =========================================================================
    // 1. DOKUMEN MASTER MATRIKS INDUK JADWAL (A4 LANDSCAPE)
    // =========================================================================

    /**
     * Ekspor Matriks Induk Jadwal / Master Schedule Seluruh Rombel Kelas (A4 Landscape)
     * Format komprehensif lanskap yang rapi, profesional, dan siap dipajang di Ruang Guru/Kurikulum.
     */
    fun exportMasterMatrixPdf(
        context: Context,
        kelasList: List<KelasEntity>,
        jadwalDetails: List<JadwalDetail>,
        mapelList: List<MapelEntity>,
        guruList: List<GuruEntity>,
        hariList: List<HariEntity>,
        jamList: List<JamEntity>,
        settingsMap: Map<String, String>
    ): File {
        val pdfDocument = PdfDocument()
        val activeHari = hariList.filter { it.isAktif }.sortedBy { it.urutan }
        val activeJam = jamList.sortedBy { it.urutan }

        // Chunk by 3 days per landscape sheet to keep high readability and comfortable cell heights
        val daysPerPage = 3
        val pageChunks = activeHari.chunked(daysPerPage)
        val totalPages = pageChunks.size

        pageChunks.forEachIndexed { pageIndex, hariChunk ->
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_LANDSCAPE, PAGE_HEIGHT_LANDSCAPE, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            drawHeaderKopLandscape(canvas, PAGE_WIDTH_LANDSCAPE, settingsMap)

            var currentY = 100f
            val hariNames = hariChunk.joinToString(", ") { it.namaHari }
            currentY = drawDocumentTitleLandscape(
                canvas = canvas,
                pageWidth = PAGE_WIDTH_LANDSCAPE,
                startY = currentY,
                title = "MATRIKS JADWAL PELAJARAN INDUK (MASTER SCHEDULE)",
                subtitle = "Tahun Pelajaran ${settingsMap["tahun_pelajaran"] ?: "2025/2026"} • Semester ${settingsMap["semester"] ?: "Ganjil"}",
                badgeText = "Bagian ${pageIndex + 1} ($hariNames)"
            )

            // Draw Master Landscape Matrix Table
            val endTableY = drawMasterMatrixTable(
                canvas = canvas,
                pageWidth = PAGE_WIDTH_LANDSCAPE,
                startY = currentY,
                hariChunk = hariChunk,
                activeJam = activeJam,
                kelasList = kelasList,
                jadwalDetails = jadwalDetails
            )

            // Draw bottom panel: Compact Legend and Signatures on the last page or bottom
            drawLandscapeFooterPanel(
                canvas = canvas,
                pageWidth = PAGE_WIDTH_LANDSCAPE,
                pageHeight = PAGE_HEIGHT_LANDSCAPE,
                startY = endTableY + 8f,
                guruList = guruList,
                mapelList = mapelList,
                settingsMap = settingsMap,
                isLastPage = (pageIndex == totalPages - 1)
            )

            drawFooterPageNumber(canvas, PAGE_WIDTH_LANDSCAPE, PAGE_HEIGHT_LANDSCAPE, pageIndex + 1, totalPages, MARGIN_LANDSCAPE)

            pdfDocument.finishPage(page)
        }

        val fileName = "Master_Jadwal_Induk_Landscape_${System.currentTimeMillis()}.pdf"
        val file = savePdfToFile(context, pdfDocument, fileName)
        pdfDocument.close()
        return file
    }

    // =========================================================================
    // 2. DOKUMEN JADWAL KELAS FORMAT LANDSCAPE (A4 LANDSCAPE)
    // =========================================================================

    /**
     * Ekspor Jadwal Pelajaran 1 Rombel Kelas dalam Format A4 Landscape Lebar
     * Format lebar dengan ruang sel luas, nama mapel jelas, guru pengampu, legenda dan pengesahan.
     */
    fun exportJadwalKelasLandscapePdf(
        context: Context,
        kelas: KelasEntity,
        jadwalDetails: List<JadwalDetail>,
        mapelList: List<MapelEntity>,
        guruList: List<GuruEntity>,
        hariList: List<HariEntity>,
        jamList: List<JamEntity>,
        settingsMap: Map<String, String>
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_LANDSCAPE, PAGE_HEIGHT_LANDSCAPE, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val activeHari = hariList.filter { it.isAktif }.sortedBy { it.urutan }
        val activeJam = jamList.sortedBy { it.urutan }
        val classJadwal = jadwalDetails.filter { it.kelasId == kelas.id }

        drawHeaderKopLandscape(canvas, PAGE_WIDTH_LANDSCAPE, settingsMap)

        var currentY = 100f
        val waliStr = "Wali Kelas: ${kelas.waliKelasNama.ifEmpty { guruList.find { it.id == kelas.waliKelasGuruId }?.nama ?: "-" }}"
        currentY = drawDocumentTitleLandscape(
            canvas = canvas,
            pageWidth = PAGE_WIDTH_LANDSCAPE,
            startY = currentY,
            title = "JADWAL PELAJARAN KELAS ${kelas.namaKelas.uppercase()}",
            subtitle = "Tahun Pelajaran ${settingsMap["tahun_pelajaran"] ?: "2025/2026"} • Semester ${settingsMap["semester"] ?: "Ganjil"}",
            badgeText = waliStr
        )

        // Draw Class Schedule Landscape Table
        val endTableY = drawClassScheduleTableLandscape(
            canvas = canvas,
            pageWidth = PAGE_WIDTH_LANDSCAPE,
            startY = currentY,
            activeHari = activeHari,
            activeJam = activeJam,
            classJadwal = classJadwal
        )

        // Draw side-by-side Legend & Signature Block below table
        drawClassLandscapeBottomBlock(
            canvas = canvas,
            pageWidth = PAGE_WIDTH_LANDSCAPE,
            pageHeight = PAGE_HEIGHT_LANDSCAPE,
            startY = endTableY + 8f,
            kelas = kelas,
            classJadwal = classJadwal,
            guruList = guruList,
            mapelList = mapelList,
            settingsMap = settingsMap
        )

        drawFooterPageNumber(canvas, PAGE_WIDTH_LANDSCAPE, PAGE_HEIGHT_LANDSCAPE, 1, 1, MARGIN_LANDSCAPE)

        pdfDocument.finishPage(page)

        val safeClassName = kelas.namaKelas.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val fileName = "Jadwal_Kelas_${safeClassName}_Landscape_${System.currentTimeMillis()}.pdf"
        val file = savePdfToFile(context, pdfDocument, fileName)
        pdfDocument.close()
        return file
    }

    /**
     * Ekspor Buku Jadwal Seluruh Kelas dalam Format A4 Landscape (Multi-Halaman)
     */
    fun exportJadwalSemuaKelasLandscapePdf(
        context: Context,
        kelasList: List<KelasEntity>,
        jadwalDetails: List<JadwalDetail>,
        mapelList: List<MapelEntity>,
        guruList: List<GuruEntity>,
        hariList: List<HariEntity>,
        jamList: List<JamEntity>,
        settingsMap: Map<String, String>
    ): File {
        val pdfDocument = PdfDocument()
        val totalPages = kelasList.size.coerceAtLeast(1)
        val activeHari = hariList.filter { it.isAktif }.sortedBy { it.urutan }
        val activeJam = jamList.sortedBy { it.urutan }

        kelasList.forEachIndexed { index, kelas ->
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_LANDSCAPE, PAGE_HEIGHT_LANDSCAPE, index + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val classJadwal = jadwalDetails.filter { it.kelasId == kelas.id }

            drawHeaderKopLandscape(canvas, PAGE_WIDTH_LANDSCAPE, settingsMap)

            var currentY = 100f
            val waliStr = "Wali Kelas: ${kelas.waliKelasNama.ifEmpty { guruList.find { it.id == kelas.waliKelasGuruId }?.nama ?: "-" }}"
            currentY = drawDocumentTitleLandscape(
                canvas = canvas,
                pageWidth = PAGE_WIDTH_LANDSCAPE,
                startY = currentY,
                title = "JADWAL PELAJARAN KELAS ${kelas.namaKelas.uppercase()}",
                subtitle = "Tahun Pelajaran ${settingsMap["tahun_pelajaran"] ?: "2025/2026"} • Semester ${settingsMap["semester"] ?: "Ganjil"}",
                badgeText = waliStr
            )

            val endTableY = drawClassScheduleTableLandscape(
                canvas = canvas,
                pageWidth = PAGE_WIDTH_LANDSCAPE,
                startY = currentY,
                activeHari = activeHari,
                activeJam = activeJam,
                classJadwal = classJadwal
            )

            drawClassLandscapeBottomBlock(
                canvas = canvas,
                pageWidth = PAGE_WIDTH_LANDSCAPE,
                pageHeight = PAGE_HEIGHT_LANDSCAPE,
                startY = endTableY + 8f,
                kelas = kelas,
                classJadwal = classJadwal,
                guruList = guruList,
                mapelList = mapelList,
                settingsMap = settingsMap
            )

            drawFooterPageNumber(canvas, PAGE_WIDTH_LANDSCAPE, PAGE_HEIGHT_LANDSCAPE, index + 1, totalPages, MARGIN_LANDSCAPE)

            pdfDocument.finishPage(page)
        }

        val fileName = "Buku_Jadwal_Semua_Kelas_Landscape_${System.currentTimeMillis()}.pdf"
        val file = savePdfToFile(context, pdfDocument, fileName)
        pdfDocument.close()
        return file
    }

    // =========================================================================
    // 3. DOKUMEN JADWAL MENGAJAR GURU (A4 LANDSCAPE)
    // =========================================================================

    /**
     * Ekspor Jadwal Mengajar Pribadi Guru Format A4 Landscape (Matriks Kalender Mingguan)
     */
    fun exportJadwalGuruLandscapePdf(
        context: Context,
        guru: GuruEntity,
        jadwalDetails: List<JadwalDetail>,
        mapelList: List<MapelEntity>,
        kelasList: List<KelasEntity>,
        hariList: List<HariEntity>,
        jamList: List<JamEntity>,
        settingsMap: Map<String, String>
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_LANDSCAPE, PAGE_HEIGHT_LANDSCAPE, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val activeHari = hariList.filter { it.isAktif }.sortedBy { it.urutan }
        val activeJam = jamList.sortedBy { it.urutan }
        val guruJadwal = jadwalDetails.filter { it.guruId == guru.id }

        drawHeaderKopLandscape(canvas, PAGE_WIDTH_LANDSCAPE, settingsMap)

        var currentY = 100f
        currentY = drawDocumentTitleLandscape(
            canvas = canvas,
            pageWidth = PAGE_WIDTH_LANDSCAPE,
            startY = currentY,
            title = "JADWAL MENGAJAR GURU (KARTU BEBAN KERJA)",
            subtitle = "${guru.nama} • NIP: ${guru.nip.ifBlank { "-" }} • Mapel Utama: ${guru.mapelUtama}",
            badgeText = "Total Beban: ${guruJadwal.size} JP / Minggu"
        )

        // Draw Teacher Weekly Landscape Matrix Table
        val endTableY = drawTeacherScheduleTableLandscape(
            canvas = canvas,
            pageWidth = PAGE_WIDTH_LANDSCAPE,
            startY = currentY,
            activeHari = activeHari,
            activeJam = activeJam,
            guruJadwal = guruJadwal
        )

        // Draw Teacher Landscape Bottom Summary & Signatures
        drawTeacherLandscapeBottomBlock(
            canvas = canvas,
            pageWidth = PAGE_WIDTH_LANDSCAPE,
            pageHeight = PAGE_HEIGHT_LANDSCAPE,
            startY = endTableY + 8f,
            guru = guru,
            guruJadwal = guruJadwal,
            activeHari = activeHari,
            settingsMap = settingsMap
        )

        drawFooterPageNumber(canvas, PAGE_WIDTH_LANDSCAPE, PAGE_HEIGHT_LANDSCAPE, 1, 1, MARGIN_LANDSCAPE)

        pdfDocument.finishPage(page)

        val safeGuruName = guru.nama.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val fileName = "Jadwal_Guru_${safeGuruName}_Landscape_${System.currentTimeMillis()}.pdf"
        val file = savePdfToFile(context, pdfDocument, fileName)
        pdfDocument.close()
        return file
    }

    // =========================================================================
    // 4. DOKUMEN REKAPITULASI BEBAN MENGAJAR GURU (A4 LANDSCAPE)
    // =========================================================================

    /**
     * Ekspor Rekapitulasi Matriks Beban Mengajar Seluruh Guru (A4 Landscape)
     */
    fun exportRekapBebanGuruLandscapePdf(
        context: Context,
        guruList: List<GuruEntity>,
        jadwalDetails: List<JadwalDetail>,
        mapelList: List<MapelEntity>,
        hariList: List<HariEntity>,
        settingsMap: Map<String, String>
    ): File {
        val pdfDocument = PdfDocument()
        val activeHari = hariList.filter { it.isAktif }.sortedBy { it.urutan }
        val itemsPerPage = 14
        val chunks = guruList.chunked(itemsPerPage)
        val totalPages = chunks.size.coerceAtLeast(1)

        chunks.forEachIndexed { pageIdx, chunk ->
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_LANDSCAPE, PAGE_HEIGHT_LANDSCAPE, pageIdx + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            drawHeaderKopLandscape(canvas, PAGE_WIDTH_LANDSCAPE, settingsMap)

            var currentY = 100f
            currentY = drawDocumentTitleLandscape(
                canvas = canvas,
                pageWidth = PAGE_WIDTH_LANDSCAPE,
                startY = currentY,
                title = "REKAPITULASI BEBAN KERJA & TATAP MUKA GURU",
                subtitle = "Tahun Pelajaran ${settingsMap["tahun_pelajaran"] ?: "2025/2026"} • Semester ${settingsMap["semester"] ?: "Ganjil"}",
                badgeText = "Standar Sertifikasi: Minimal 24 JP Tatap Muka / Minggu"
            )

            // Draw Rekap Table in Landscape
            val endTableY = drawRekapGuruTableLandscape(
                canvas = canvas,
                pageWidth = PAGE_WIDTH_LANDSCAPE,
                startY = currentY,
                guruChunk = chunk,
                startIndex = pageIdx * itemsPerPage,
                jadwalDetails = jadwalDetails,
                activeHari = activeHari
            )

            if (pageIdx == totalPages - 1) {
                drawLandscapeSignatures(
                    canvas = canvas,
                    pageWidth = PAGE_WIDTH_LANDSCAPE,
                    pageHeight = PAGE_HEIGHT_LANDSCAPE,
                    settingsMap = settingsMap
                )
            }

            drawFooterPageNumber(canvas, PAGE_WIDTH_LANDSCAPE, PAGE_HEIGHT_LANDSCAPE, pageIdx + 1, totalPages, MARGIN_LANDSCAPE)

            pdfDocument.finishPage(page)
        }

        val fileName = "Rekap_Beban_Guru_Landscape_${System.currentTimeMillis()}.pdf"
        val file = savePdfToFile(context, pdfDocument, fileName)
        pdfDocument.close()
        return file
    }

    // =========================================================================
    // DOKUMEN STANDAR PORTRAIT (Tetap Tersedia untuk Pilihan Fleksibel)
    // =========================================================================

    /**
     * Ekspor Jadwal 1 Rombel Kelas (A4 Portrait)
     */
    fun exportJadwalKelasPdf(
        context: Context,
        kelas: KelasEntity,
        jadwalDetails: List<JadwalDetail>,
        mapelList: List<MapelEntity>,
        guruList: List<GuruEntity>,
        hariList: List<HariEntity>,
        jamList: List<JamEntity>,
        settingsMap: Map<String, String>
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_PORTRAIT, PAGE_HEIGHT_PORTRAIT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val activeHari = hariList.filter { it.isAktif }.sortedBy { it.urutan }
        val activeJam = jamList.sortedBy { it.urutan }
        val classJadwal = jadwalDetails.filter { it.kelasId == kelas.id }

        drawHeaderKopPortrait(canvas, PAGE_WIDTH_PORTRAIT, settingsMap)

        var currentY = 118f
        currentY = drawDocumentTitlePortrait(
            canvas = canvas,
            pageWidth = PAGE_WIDTH_PORTRAIT,
            startY = currentY,
            title = "JADWAL PELAJARAN KELAS ${kelas.namaKelas.uppercase()}",
            subtitle = "Tahun Pelajaran ${settingsMap["tahun_pelajaran"] ?: "2025/2026"} • Semester ${settingsMap["semester"] ?: "Ganjil"}",
            waliKelas = "Wali Kelas: ${kelas.waliKelasNama.ifEmpty { guruList.find { it.id == kelas.waliKelasGuruId }?.nama ?: "-" }}"
        )

        currentY = drawClassScheduleTablePortrait(
            canvas = canvas,
            pageWidth = PAGE_WIDTH_PORTRAIT,
            startY = currentY,
            activeHari = activeHari,
            activeJam = activeJam,
            classJadwal = classJadwal
        )

        currentY = drawMapelLegendPortrait(
            canvas = canvas,
            pageWidth = PAGE_WIDTH_PORTRAIT,
            startY = currentY + 10f,
            classJadwal = classJadwal,
            guruList = guruList,
            mapelList = mapelList
        )

        drawSignatureBlockPortrait(
            canvas = canvas,
            pageWidth = PAGE_WIDTH_PORTRAIT,
            pageHeight = PAGE_HEIGHT_PORTRAIT,
            settingsMap = settingsMap
        )

        drawFooterPageNumber(canvas, PAGE_WIDTH_PORTRAIT, PAGE_HEIGHT_PORTRAIT, 1, 1, MARGIN_PORTRAIT)

        pdfDocument.finishPage(page)

        val safeClassName = kelas.namaKelas.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val fileName = "Jadwal_Kelas_${safeClassName}_Portrait_${System.currentTimeMillis()}.pdf"
        val file = savePdfToFile(context, pdfDocument, fileName)
        pdfDocument.close()
        return file
    }

    /**
     * Ekspor Jadwal Seluruh Kelas dalam Buku PDF Portrait
     */
    fun exportJadwalSemuaKelasPdf(
        context: Context,
        kelasList: List<KelasEntity>,
        jadwalDetails: List<JadwalDetail>,
        mapelList: List<MapelEntity>,
        guruList: List<GuruEntity>,
        hariList: List<HariEntity>,
        jamList: List<JamEntity>,
        settingsMap: Map<String, String>
    ): File {
        val pdfDocument = PdfDocument()
        val totalPages = kelasList.size.coerceAtLeast(1)

        kelasList.forEachIndexed { index, kelas ->
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_PORTRAIT, PAGE_HEIGHT_PORTRAIT, index + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val activeHari = hariList.filter { it.isAktif }.sortedBy { it.urutan }
            val activeJam = jamList.sortedBy { it.urutan }
            val classJadwal = jadwalDetails.filter { it.kelasId == kelas.id }

            drawHeaderKopPortrait(canvas, PAGE_WIDTH_PORTRAIT, settingsMap)

            var currentY = 118f
            currentY = drawDocumentTitlePortrait(
                canvas = canvas,
                pageWidth = PAGE_WIDTH_PORTRAIT,
                startY = currentY,
                title = "JADWAL PELAJARAN KELAS ${kelas.namaKelas.uppercase()}",
                subtitle = "Tahun Pelajaran ${settingsMap["tahun_pelajaran"] ?: "2025/2026"} • Semester ${settingsMap["semester"] ?: "Ganjil"}",
                waliKelas = "Wali Kelas: ${kelas.waliKelasNama.ifEmpty { guruList.find { it.id == kelas.waliKelasGuruId }?.nama ?: "-" }}"
            )

            currentY = drawClassScheduleTablePortrait(
                canvas = canvas,
                pageWidth = PAGE_WIDTH_PORTRAIT,
                startY = currentY,
                activeHari = activeHari,
                activeJam = activeJam,
                classJadwal = classJadwal
            )

            currentY = drawMapelLegendPortrait(
                canvas = canvas,
                pageWidth = PAGE_WIDTH_PORTRAIT,
                startY = currentY + 10f,
                classJadwal = classJadwal,
                guruList = guruList,
                mapelList = mapelList
            )

            drawSignatureBlockPortrait(
                canvas = canvas,
                pageWidth = PAGE_WIDTH_PORTRAIT,
                pageHeight = PAGE_HEIGHT_PORTRAIT,
                settingsMap = settingsMap
            )

            drawFooterPageNumber(canvas, PAGE_WIDTH_PORTRAIT, PAGE_HEIGHT_PORTRAIT, index + 1, totalPages, MARGIN_PORTRAIT)

            pdfDocument.finishPage(page)
        }

        val fileName = "Buku_Jadwal_Semua_Kelas_Portrait_${System.currentTimeMillis()}.pdf"
        val file = savePdfToFile(context, pdfDocument, fileName)
        pdfDocument.close()
        return file
    }

    /**
     * Ekspor Jadwal Mengajar Pribadi Guru (A4 Portrait)
     */
    fun exportJadwalGuruPdf(
        context: Context,
        guru: GuruEntity,
        jadwalDetails: List<JadwalDetail>,
        mapelList: List<MapelEntity>,
        kelasList: List<KelasEntity>,
        hariList: List<HariEntity>,
        jamList: List<JamEntity>,
        settingsMap: Map<String, String>
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_PORTRAIT, PAGE_HEIGHT_PORTRAIT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val guruJadwal = jadwalDetails.filter { it.guruId == guru.id }
            .sortedWith(compareBy({ it.hariId }, { it.jpKode }))

        drawHeaderKopPortrait(canvas, PAGE_WIDTH_PORTRAIT, settingsMap)

        var currentY = 118f
        currentY = drawDocumentTitlePortrait(
            canvas = canvas,
            pageWidth = PAGE_WIDTH_PORTRAIT,
            startY = currentY,
            title = "JADWAL MENGAJAR GURU",
            subtitle = "${guru.nama} • NIP: ${guru.nip.ifBlank { "-" }}",
            waliKelas = "Mata Pelajaran: ${guru.mapelUtama} • Beban Mengajar: ${guruJadwal.size} JP / Minggu"
        )

        currentY = drawGuruScheduleTablePortrait(
            canvas = canvas,
            pageWidth = PAGE_WIDTH_PORTRAIT,
            startY = currentY,
            guruJadwal = guruJadwal
        )

        currentY = drawGuruSummaryPortrait(
            canvas = canvas,
            pageWidth = PAGE_WIDTH_PORTRAIT,
            startY = currentY + 12f,
            guruJadwal = guruJadwal,
            hariList = hariList
        )

        drawSignatureBlockPortrait(
            canvas = canvas,
            pageWidth = PAGE_WIDTH_PORTRAIT,
            pageHeight = PAGE_HEIGHT_PORTRAIT,
            settingsMap = settingsMap
        )

        drawFooterPageNumber(canvas, PAGE_WIDTH_PORTRAIT, PAGE_HEIGHT_PORTRAIT, 1, 1, MARGIN_PORTRAIT)

        pdfDocument.finishPage(page)

        val safeGuruName = guru.nama.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val fileName = "Jadwal_Guru_${safeGuruName}_Portrait_${System.currentTimeMillis()}.pdf"
        val file = savePdfToFile(context, pdfDocument, fileName)
        pdfDocument.close()
        return file
    }

    /**
     * Ekspor Rekapitulasi Beban Mengajar Guru (A4 Portrait)
     */
    fun exportRekapBebanGuruPdf(
        context: Context,
        guruList: List<GuruEntity>,
        jadwalDetails: List<JadwalDetail>,
        mapelList: List<MapelEntity>,
        settingsMap: Map<String, String>
    ): File {
        val pdfDocument = PdfDocument()
        val itemsPerPage = 18
        val chunks = guruList.chunked(itemsPerPage)
        val totalPages = chunks.size.coerceAtLeast(1)

        chunks.forEachIndexed { pageIdx, chunk ->
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_PORTRAIT, PAGE_HEIGHT_PORTRAIT, pageIdx + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            drawHeaderKopPortrait(canvas, PAGE_WIDTH_PORTRAIT, settingsMap)

            var currentY = 118f
            currentY = drawDocumentTitlePortrait(
                canvas = canvas,
                pageWidth = PAGE_WIDTH_PORTRAIT,
                startY = currentY,
                title = "REKAPITULASI BEBAN MENGAJAR GURU",
                subtitle = "Tahun Pelajaran ${settingsMap["tahun_pelajaran"] ?: "2025/2026"} • Semester ${settingsMap["semester"] ?: "Ganjil"}",
                waliKelas = "Standar Minimal: 24 JP / Minggu"
            )

            val endTableY = drawRekapGuruTablePortrait(
                canvas = canvas,
                pageWidth = PAGE_WIDTH_PORTRAIT,
                startY = currentY,
                guruChunk = chunk,
                startIndex = pageIdx * itemsPerPage,
                jadwalDetails = jadwalDetails
            )

            if (pageIdx == totalPages - 1) {
                drawSignatureBlockPortrait(
                    canvas = canvas,
                    pageWidth = PAGE_WIDTH_PORTRAIT,
                    pageHeight = PAGE_HEIGHT_PORTRAIT,
                    settingsMap = settingsMap
                )
            }

            drawFooterPageNumber(canvas, PAGE_WIDTH_PORTRAIT, PAGE_HEIGHT_PORTRAIT, pageIdx + 1, totalPages, MARGIN_PORTRAIT)

            pdfDocument.finishPage(page)
        }

        val fileName = "Rekap_Beban_Guru_Portrait_${System.currentTimeMillis()}.pdf"
        val file = savePdfToFile(context, pdfDocument, fileName)
        pdfDocument.close()
        return file
    }

    // =========================================================================
    // DRAWING HELPERS: LANDSCAPE (A4 WIDE)
    // =========================================================================

    /**
     * Menggambar Kop Surat Resmi Standar Kementerian Agama & Madrasah Format Landscape
     */
    private fun drawHeaderKopLandscape(canvas: Canvas, pageWidth: Int, settingsMap: Map<String, String>) {
        val margin = MARGIN_LANDSCAPE
        val namaMadrasah = settingsMap["nama_madrasah"] ?: "MTs DARUSSALAM"
        val kota = settingsMap["kota"] ?: "MAGETAN"
        val npsn = settingsMap["npsn"] ?: "20582511"
        val nsm = settingsMap["nsm"] ?: "121235200016"
        val alamat = settingsMap["alamat"] ?: "Lembeyan Kulon, Kec. Lembeyan"

        // Draw Official Kemenag Vector Emblem on Left
        drawOfficialKemenagEmblem(canvas, margin + 8f, margin + 4f, 44f)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

        // Line 1: Kemenag
        paint.color = COLOR_TEXT_MAIN
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("KEMENTERIAN AGAMA REPUBLIK INDONESIA", pageWidth / 2f, margin + 11f, paint)

        // Line 2: Kantor Kemenag Kabupaten/Kota
        paint.textSize = 8.5f
        paint.typeface = Typeface.DEFAULT
        paint.color = COLOR_TEXT_MAIN
        canvas.drawText("KANTOR KEMENTERIAN AGAMA KABUPATEN / KOTA ${kota.uppercase()}", pageWidth / 2f, margin + 22f, paint)

        // Line 3: Nama Madrasah Besar
        paint.textSize = 13.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = COLOR_PRIMARY
        canvas.drawText(namaMadrasah.uppercase(), pageWidth / 2f, margin + 37f, paint)

        // Line 4: Alamat & Kontak (NSM, NPSN, Alamat)
        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 7.5f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("NSM: $nsm • NPSN: $npsn • Alamat: $alamat, Kab. $kota", pageWidth / 2f, margin + 48f, paint)

        // Double Horizontal Divider
        val linePaintThick = Paint().apply {
            color = COLOR_PRIMARY
            strokeWidth = 2.2f
        }
        val linePaintThin = Paint().apply {
            color = COLOR_PRIMARY
            strokeWidth = 0.8f
        }

        val startX = margin
        val endX = pageWidth - margin
        val yThick = margin + 55f
        val yThin = margin + 58.5f

        canvas.drawLine(startX, yThick, endX, yThick, linePaintThick)
        canvas.drawLine(startX, yThin, endX, yThin, linePaintThin)
    }

    private fun drawDocumentTitleLandscape(
        canvas: Canvas,
        pageWidth: Int,
        startY: Float,
        title: String,
        subtitle: String,
        badgeText: String
    ): Float {
        val margin = MARGIN_LANDSCAPE
        var y = startY

        val paintTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }

        val paintSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 8.5f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.LEFT
        }

        canvas.drawText(title, margin, y, paintTitle)
        canvas.drawText(subtitle, margin, y + 11f, paintSub)

        if (badgeText.isNotBlank()) {
            val badgePaintBg = Paint().apply {
                color = COLOR_PRIMARY_LIGHT
                style = Paint.Style.FILL
            }
            val badgePaintBorder = Paint().apply {
                color = COLOR_PRIMARY
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
            }
            val badgePaintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_PRIMARY
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }

            val badgeX = pageWidth - margin
            val badgeY = y + 5f

            val textWidth = badgePaintText.measureText(badgeText)
            val rectLeft = badgeX - textWidth - 16f
            val rectTop = y - 8f
            val rectRight = badgeX
            val rectBottom = y + 14f

            val r = RectF(rectLeft, rectTop, rectRight, rectBottom)
            canvas.drawRoundRect(r, 6f, 6f, badgePaintBg)
            canvas.drawRoundRect(r, 6f, 6f, badgePaintBorder)
            canvas.drawText(badgeText, badgeX - 8f, badgeY, badgePaintText)
        }

        return y + 20f
    }

    /**
     * Menggambar Master Matrix Landscape Table
     */
    private fun drawMasterMatrixTable(
        canvas: Canvas,
        pageWidth: Int,
        startY: Float,
        hariChunk: List<HariEntity>,
        activeJam: List<JamEntity>,
        kelasList: List<KelasEntity>,
        jadwalDetails: List<JadwalDetail>
    ): Float {
        val tableLeft = MARGIN_LANDSCAPE
        val tableRight = pageWidth - MARGIN_LANDSCAPE
        val tableWidth = tableRight - tableLeft

        val dayColWidth = 52f
        val jpColWidth = 34f
        val timeColWidth = 58f
        val fixedWidth = dayColWidth + jpColWidth + timeColWidth
        val classColWidth = (tableWidth - fixedWidth) / kelasList.size.coerceAtLeast(1)

        val headerHeight = 22f
        val rowHeight = 15.5f

        val paintBg = Paint().apply { style = Paint.Style.FILL }
        val paintBorder = Paint().apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.75f
        }
        val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
        }

        var currentY = startY

        // Table Header
        paintBg.color = COLOR_PRIMARY
        canvas.drawRect(tableLeft, currentY, tableRight, currentY + headerHeight, paintBg)
        canvas.drawRect(tableLeft, currentY, tableRight, currentY + headerHeight, paintBorder)

        paintText.color = Color.WHITE
        paintText.textSize = 8f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("HARI", tableLeft + dayColWidth / 2, currentY + 14f, paintText)
        canvas.drawLine(tableLeft + dayColWidth, currentY, tableLeft + dayColWidth, currentY + headerHeight, paintBorder)

        canvas.drawText("JP", tableLeft + dayColWidth + jpColWidth / 2, currentY + 14f, paintText)
        canvas.drawLine(tableLeft + dayColWidth + jpColWidth, currentY, tableLeft + dayColWidth + jpColWidth, currentY + headerHeight, paintBorder)

        canvas.drawText("WAKTU", tableLeft + fixedWidth - timeColWidth / 2, currentY + 14f, paintText)
        canvas.drawLine(tableLeft + fixedWidth, currentY, tableLeft + fixedWidth, currentY + headerHeight, paintBorder)

        var clsX = tableLeft + fixedWidth
        kelasList.forEach { k ->
            canvas.drawText("KL ${k.namaKelas}", clsX + classColWidth / 2, currentY + 14f, paintText)
            canvas.drawLine(clsX + classColWidth, currentY, clsX + classColWidth, currentY + headerHeight, paintBorder)
            clsX += classColWidth
        }

        currentY += headerHeight

        val slotMap = jadwalDetails.associateBy { "${it.hariId}_${it.jpKode}_${it.kelasId}" }

        hariChunk.forEach { hari ->
            val startHariY = currentY
            activeJam.forEachIndexed { jamIdx, jam ->
                if (jam.isIstirahat) {
                    paintBg.color = COLOR_ACCENT_BG
                    canvas.drawRect(tableLeft + dayColWidth, currentY, tableRight, currentY + 13.5f, paintBg)
                    canvas.drawRect(tableLeft + dayColWidth, currentY, tableRight, currentY + 13.5f, paintBorder)

                    paintText.color = COLOR_ACCENT_GOLD
                    paintText.textSize = 7f
                    paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("ISTIRAHAT & REHAT (${jam.jamMulai} - ${jam.jamSelesai})", (tableLeft + fixedWidth + tableRight) / 2f, currentY + 9.5f, paintText)
                    currentY += 13.5f
                } else {
                    if (jamIdx % 2 == 1) {
                        paintBg.color = COLOR_ROW_ALT
                        canvas.drawRect(tableLeft + dayColWidth, currentY, tableRight, currentY + rowHeight, paintBg)
                    }
                    canvas.drawRect(tableLeft + dayColWidth, currentY, tableRight, currentY + rowHeight, paintBorder)

                    // JP
                    paintText.color = COLOR_TEXT_MAIN
                    paintText.textSize = 7.5f
                    paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(jam.jpKode, tableLeft + dayColWidth + jpColWidth / 2, currentY + 11f, paintText)
                    canvas.drawLine(tableLeft + dayColWidth + jpColWidth, currentY, tableLeft + dayColWidth + jpColWidth, currentY + rowHeight, paintBorder)

                    // Waktu
                    paintText.typeface = Typeface.DEFAULT
                    paintText.textSize = 6.5f
                    paintText.color = COLOR_TEXT_MUTED
                    canvas.drawText("${jam.jamMulai}-${jam.jamSelesai}", tableLeft + fixedWidth - timeColWidth / 2, currentY + 11f, paintText)
                    canvas.drawLine(tableLeft + fixedWidth, currentY, tableLeft + fixedWidth, currentY + rowHeight, paintBorder)

                    // Classes Slots
                    var cX = tableLeft + fixedWidth
                    kelasList.forEach { k ->
                        val slot = slotMap["${hari.id}_${jam.jpKode}_${k.id}"]
                        if (slot != null) {
                            // Mapel Code
                            paintText.color = COLOR_PRIMARY
                            paintText.textSize = 7f
                            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            canvas.drawText(slot.mapelKode, cX + classColWidth / 2, currentY + 7.5f, paintText)

                            // Short Guru Initials
                            paintText.color = COLOR_TEXT_MAIN
                            paintText.textSize = 5.8f
                            paintText.typeface = Typeface.DEFAULT
                            val initialGuru = getShortTeacherName(slot.namaGuru)
                            canvas.drawText(initialGuru, cX + classColWidth / 2, currentY + 13.5f, paintText)
                        } else {
                            paintText.color = Color.rgb(203, 213, 225)
                            paintText.textSize = 7.5f
                            paintText.typeface = Typeface.DEFAULT
                            canvas.drawText("-", cX + classColWidth / 2, currentY + 10.5f, paintText)
                        }

                        canvas.drawLine(cX + classColWidth, currentY, cX + classColWidth, currentY + rowHeight, paintBorder)
                        cX += classColWidth
                    }

                    currentY += rowHeight
                }
            }

            // Draw Day cell on left with emerald background
            val endHariY = currentY
            paintBg.color = COLOR_PRIMARY_LIGHT
            canvas.drawRect(tableLeft, startHariY, tableLeft + dayColWidth, endHariY, paintBg)
            canvas.drawRect(tableLeft, startHariY, tableLeft + dayColWidth, endHariY, paintBorder)

            paintText.color = COLOR_PRIMARY
            paintText.textSize = 9f
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val dayCenterY = (startHariY + endHariY) / 2f
            canvas.drawText(hari.namaHari.uppercase(), tableLeft + dayColWidth / 2, dayCenterY + 3.5f, paintText)
        }

        return currentY
    }

    /**
     * Menggambar Jadwal 1 Kelas Format A4 Landscape (Lengkap & Elegan)
     */
    private fun drawClassScheduleTableLandscape(
        canvas: Canvas,
        pageWidth: Int,
        startY: Float,
        activeHari: List<HariEntity>,
        activeJam: List<JamEntity>,
        classJadwal: List<JadwalDetail>
    ): Float {
        val tableLeft = MARGIN_LANDSCAPE
        val tableRight = pageWidth - MARGIN_LANDSCAPE
        val tableWidth = tableRight - tableLeft

        val timeColWidth = 85f
        val dayColWidth = (tableWidth - timeColWidth) / activeHari.size.coerceAtLeast(1)

        val headerHeight = 22f
        val rowHeight = 25f

        val paintBg = Paint().apply { style = Paint.Style.FILL }
        val paintBorder = Paint().apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.75f
        }
        val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
        }

        var currentY = startY

        // Table Header
        paintBg.color = COLOR_PRIMARY
        canvas.drawRect(tableLeft, currentY, tableRight, currentY + headerHeight, paintBg)
        canvas.drawRect(tableLeft, currentY, tableRight, currentY + headerHeight, paintBorder)

        paintText.color = Color.WHITE
        paintText.textSize = 8.5f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("JAM / WAKTU", tableLeft + timeColWidth / 2, currentY + 14.5f, paintText)
        canvas.drawLine(tableLeft + timeColWidth, currentY, tableLeft + timeColWidth, currentY + headerHeight, paintBorder)

        var dayX = tableLeft + timeColWidth
        activeHari.forEach { h ->
            canvas.drawText(h.namaHari.uppercase(), dayX + dayColWidth / 2, currentY + 14.5f, paintText)
            canvas.drawLine(dayX + dayColWidth, currentY, dayX + dayColWidth, currentY + headerHeight, paintBorder)
            dayX += dayColWidth
        }

        currentY += headerHeight

        val slotMap = classJadwal.associateBy { "${it.hariId}_${it.jpKode}" }

        activeJam.forEachIndexed { rowIdx, jam ->
            if (jam.isIstirahat) {
                paintBg.color = COLOR_ACCENT_BG
                canvas.drawRect(tableLeft, currentY, tableRight, currentY + 16f, paintBg)
                canvas.drawRect(tableLeft, currentY, tableRight, currentY + 16f, paintBorder)

                paintText.color = COLOR_ACCENT_GOLD
                paintText.textSize = 8f
                paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("${jam.jpKode} (${jam.jamMulai} - ${jam.jamSelesai}) • ISTIRAHAT / SHOLAT", pageWidth / 2f, currentY + 11f, paintText)
                currentY += 16f
            } else {
                if (rowIdx % 2 == 1) {
                    paintBg.color = COLOR_ROW_ALT
                    canvas.drawRect(tableLeft, currentY, tableRight, currentY + rowHeight, paintBg)
                }
                canvas.drawRect(tableLeft, currentY, tableRight, currentY + rowHeight, paintBorder)

                // Time Col
                paintText.color = COLOR_TEXT_MAIN
                paintText.textSize = 8.5f
                paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(jam.jpKode, tableLeft + timeColWidth / 2, currentY + 11f, paintText)

                paintText.textSize = 7f
                paintText.typeface = Typeface.DEFAULT
                paintText.color = COLOR_TEXT_MUTED
                canvas.drawText("${jam.jamMulai} - ${jam.jamSelesai}", tableLeft + timeColWidth / 2, currentY + 20f, paintText)

                canvas.drawLine(tableLeft + timeColWidth, currentY, tableLeft + timeColWidth, currentY + rowHeight, paintBorder)

                // Day Columns (Spacious in Landscape)
                var cellX = tableLeft + timeColWidth
                activeHari.forEach { h ->
                    val slot = slotMap["${h.id}_${jam.jpKode}"]
                    if (slot != null) {
                        // Mapel Code & Name
                        paintText.color = COLOR_PRIMARY
                        paintText.textSize = 8.5f
                        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        canvas.drawText("${slot.mapelKode} - ${slot.namaMapel.take(16)}", cellX + dayColWidth / 2, currentY + 10f, paintText)

                        // Teacher Name
                        paintText.color = COLOR_TEXT_MAIN
                        paintText.textSize = 7f
                        paintText.typeface = Typeface.DEFAULT
                        val shortGuru = if (slot.namaGuru.length > 20) slot.namaGuru.take(18) + ".." else slot.namaGuru
                        canvas.drawText(shortGuru, cellX + dayColWidth / 2, currentY + 17.5f, paintText)

                        // Room
                        paintText.color = COLOR_TEXT_MUTED
                        paintText.textSize = 6f
                        canvas.drawText("Ruangan: ${slot.namaRuangan}", cellX + dayColWidth / 2, currentY + 23.5f, paintText)
                    } else {
                        paintText.color = Color.rgb(203, 213, 225)
                        paintText.textSize = 8.5f
                        paintText.typeface = Typeface.DEFAULT
                        canvas.drawText("-", cellX + dayColWidth / 2, currentY + 15f, paintText)
                    }

                    canvas.drawLine(cellX + dayColWidth, currentY, cellX + dayColWidth, currentY + rowHeight, paintBorder)
                    cellX += dayColWidth
                }

                currentY += rowHeight
            }
        }

        return currentY
    }

    /**
     * Menggambar Bagian Bawah Jadwal Kelas Landscape: Legenda & Tanda Tangan Bersebelahan
     */
    private fun drawClassLandscapeBottomBlock(
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        startY: Float,
        kelas: KelasEntity,
        classJadwal: List<JadwalDetail>,
        guruList: List<GuruEntity>,
        mapelList: List<MapelEntity>,
        settingsMap: Map<String, String>
    ) {
        val margin = MARGIN_LANDSCAPE
        val blockWidth = pageWidth - (2 * margin)

        val leftWidth = blockWidth * 0.55f // Legend on Left
        val rightWidth = blockWidth * 0.45f // Signatures on Right

        val leftX = margin
        val rightX = margin + leftWidth + 10f

        // 1. Legend on Left
        val paintTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 8f
            color = COLOR_PRIMARY
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 6.8f
            color = COLOR_TEXT_MAIN
        }

        canvas.drawText("Keterangan Guru & Mata Pelajaran:", leftX, startY + 8f, paintTitle)

        val usedMapelKodes = classJadwal.map { it.mapelKode }.distinct().sorted()
        val halfSize = (usedMapelKodes.size + 1) / 2
        val colW = leftWidth / 2f
        var legendY = startY + 18f

        usedMapelKodes.forEachIndexed { idx, kode ->
            val sampleSlot = classJadwal.find { it.mapelKode == kode }
            val namaMapel = sampleSlot?.namaMapel ?: kode
            val namaGuru = sampleSlot?.namaGuru ?: "-"

            val col = if (idx < halfSize) 0 else 1
            val row = if (idx < halfSize) idx else idx - halfSize

            val itemX = leftX + (col * colW)
            val itemY = legendY + (row * 9f)

            if (itemY < pageHeight - 30f) {
                paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paintText.color = COLOR_PRIMARY
                canvas.drawText("[$kode]", itemX, itemY, paintText)

                paintText.typeface = Typeface.DEFAULT
                paintText.color = COLOR_TEXT_MAIN
                val desc = ": ${namaMapel.take(13)} (${namaGuru.take(13)})"
                canvas.drawText(desc, itemX + 22f, itemY, paintText)
            }
        }

        // 2. Signatures on Right (Wali Kelas & Kepala Madrasah)
        val kota = settingsMap["kota"] ?: "Jakarta"
        val kepala = settingsMap["kepala_madrasah"] ?: "Drs. H. Mulyadi, M.Pd.I"
        val nipKepala = settingsMap["nip_kepala"] ?: "19750312 200003 1 002"
        val wali = kelas.waliKelasNama.ifEmpty { guruList.find { it.id == kelas.waliKelasGuruId }?.nama ?: "Wali Kelas ${kelas.namaKelas}" }
        val nipWali = guruList.find { it.nama.equals(wali, ignoreCase = true) || it.id == kelas.waliKelasGuruId }?.nip?.ifBlank { "-" } ?: "-"

        val sdf = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))
        val tanggalStr = sdf.format(Date())

        val sigStartY = startY + 8f
        val colWaliX = rightX + (rightWidth * 0.25f)
        val colKepalaX = rightX + (rightWidth * 0.75f)

        val paintSig = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 7.5f
            color = COLOR_TEXT_MAIN
            textAlign = Paint.Align.CENTER
        }

        // Date on top right
        paintSig.textAlign = Paint.Align.CENTER
        canvas.drawText("Ditetapkan di: $kota, $tanggalStr", colKepalaX, sigStartY, paintSig)

        // Role Titles
        canvas.drawText("Wali Kelas ${kelas.namaKelas},", colWaliX, sigStartY + 12f, paintSig)
        canvas.drawText("Mengetahui,", colKepalaX, sigStartY + 12f, paintSig)
        canvas.drawText("Kepala Madrasah,", colKepalaX, sigStartY + 21f, paintSig)

        // Names
        val nameY = sigStartY + 50f
        paintSig.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintSig.isUnderlineText = true
        canvas.drawText(wali, colWaliX, nameY, paintSig)
        canvas.drawText(kepala, colKepalaX, nameY, paintSig)

        // NIP
        paintSig.isUnderlineText = false
        paintSig.typeface = Typeface.DEFAULT
        paintSig.textSize = 7f
        canvas.drawText("NIP. $nipWali", colWaliX, nameY + 9f, paintSig)
        canvas.drawText("NIP. $nipKepala", colKepalaX, nameY + 9f, paintSig)
    }

    /**
     * Menggambar Jadwal Mengajar Guru Format Landscape
     */
    private fun drawTeacherScheduleTableLandscape(
        canvas: Canvas,
        pageWidth: Int,
        startY: Float,
        activeHari: List<HariEntity>,
        activeJam: List<JamEntity>,
        guruJadwal: List<JadwalDetail>
    ): Float {
        val tableLeft = MARGIN_LANDSCAPE
        val tableRight = pageWidth - MARGIN_LANDSCAPE
        val tableWidth = tableRight - tableLeft

        val timeColWidth = 85f
        val dayColWidth = (tableWidth - timeColWidth) / activeHari.size.coerceAtLeast(1)

        val headerHeight = 22f
        val rowHeight = 24f

        val paintBg = Paint().apply { style = Paint.Style.FILL }
        val paintBorder = Paint().apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.75f
        }
        val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
        }

        var currentY = startY

        // Header
        paintBg.color = COLOR_PRIMARY
        canvas.drawRect(tableLeft, currentY, tableRight, currentY + headerHeight, paintBg)
        canvas.drawRect(tableLeft, currentY, tableRight, currentY + headerHeight, paintBorder)

        paintText.color = Color.WHITE
        paintText.textSize = 8.5f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("JAM / WAKTU", tableLeft + timeColWidth / 2, currentY + 14.5f, paintText)
        canvas.drawLine(tableLeft + timeColWidth, currentY, tableLeft + timeColWidth, currentY + headerHeight, paintBorder)

        var dayX = tableLeft + timeColWidth
        activeHari.forEach { h ->
            canvas.drawText(h.namaHari.uppercase(), dayX + dayColWidth / 2, currentY + 14.5f, paintText)
            canvas.drawLine(dayX + dayColWidth, currentY, dayX + dayColWidth, currentY + headerHeight, paintBorder)
            dayX += dayColWidth
        }

        currentY += headerHeight

        val slotMap = guruJadwal.associateBy { "${it.hariId}_${it.jpKode}" }

        activeJam.forEachIndexed { rowIdx, jam ->
            if (jam.isIstirahat) {
                paintBg.color = COLOR_ACCENT_BG
                canvas.drawRect(tableLeft, currentY, tableRight, currentY + 16f, paintBg)
                canvas.drawRect(tableLeft, currentY, tableRight, currentY + 16f, paintBorder)

                paintText.color = COLOR_ACCENT_GOLD
                paintText.textSize = 8f
                paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("${jam.jpKode} (${jam.jamMulai} - ${jam.jamSelesai}) • ISTIRAHAT", pageWidth / 2f, currentY + 11f, paintText)
                currentY += 16f
            } else {
                if (rowIdx % 2 == 1) {
                    paintBg.color = COLOR_ROW_ALT
                    canvas.drawRect(tableLeft, currentY, tableRight, currentY + rowHeight, paintBg)
                }
                canvas.drawRect(tableLeft, currentY, tableRight, currentY + rowHeight, paintBorder)

                // Time Col
                paintText.color = COLOR_TEXT_MAIN
                paintText.textSize = 8.5f
                paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(jam.jpKode, tableLeft + timeColWidth / 2, currentY + 11f, paintText)

                paintText.textSize = 7f
                paintText.typeface = Typeface.DEFAULT
                paintText.color = COLOR_TEXT_MUTED
                canvas.drawText("${jam.jamMulai} - ${jam.jamSelesai}", tableLeft + timeColWidth / 2, currentY + 20f, paintText)

                canvas.drawLine(tableLeft + timeColWidth, currentY, tableLeft + timeColWidth, currentY + rowHeight, paintBorder)

                // Days
                var cellX = tableLeft + timeColWidth
                activeHari.forEach { h ->
                    val slot = slotMap["${h.id}_${jam.jpKode}"]
                    if (slot != null) {
                        paintText.color = COLOR_PRIMARY
                        paintText.textSize = 8.5f
                        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        canvas.drawText("KELAS ${slot.namaKelas}", cellX + dayColWidth / 2, currentY + 10f, paintText)

                        paintText.color = COLOR_TEXT_MAIN
                        paintText.textSize = 7f
                        paintText.typeface = Typeface.DEFAULT
                        canvas.drawText("${slot.mapelKode} - ${slot.namaMapel.take(14)}", cellX + dayColWidth / 2, currentY + 17.5f, paintText)

                        paintText.color = COLOR_TEXT_MUTED
                        paintText.textSize = 6f
                        canvas.drawText("Ruangan: ${slot.namaRuangan}", cellX + dayColWidth / 2, currentY + 23f, paintText)
                    } else {
                        paintText.color = Color.rgb(203, 213, 225)
                        paintText.textSize = 8.5f
                        paintText.typeface = Typeface.DEFAULT
                        canvas.drawText("-", cellX + dayColWidth / 2, currentY + 15f, paintText)
                    }

                    canvas.drawLine(cellX + dayColWidth, currentY, cellX + dayColWidth, currentY + rowHeight, paintBorder)
                    cellX += dayColWidth
                }

                currentY += rowHeight
            }
        }

        return currentY
    }

    /**
     * Ringkasan & Pengesahan Jadwal Guru Landscape
     */
    private fun drawTeacherLandscapeBottomBlock(
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        startY: Float,
        guru: GuruEntity,
        guruJadwal: List<JadwalDetail>,
        activeHari: List<HariEntity>,
        settingsMap: Map<String, String>
    ) {
        val margin = MARGIN_LANDSCAPE
        val blockWidth = pageWidth - (2 * margin)
        val leftWidth = blockWidth * 0.52f
        val rightWidth = blockWidth * 0.48f

        val leftX = margin
        val rightX = margin + leftWidth + 10f

        // Daily breakdown summary
        val paintTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 8f
            color = COLOR_PRIMARY
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 7.5f
            color = COLOR_TEXT_MAIN
        }

        canvas.drawText("Rekapitulasi Mengajar Mingguan:", leftX, startY + 8f, paintTitle)

        var sumY = startY + 20f
        val hariCounts = activeHari.map { h ->
            val count = guruJadwal.count { it.hariId == h.id }
            "${h.namaHari}: $count JP"
        }.joinToString("  •  ")

        canvas.drawText(hariCounts, leftX, sumY, paintText)
        sumY += 12f

        val statusText = if (guruJadwal.size >= 24) "Status: Memenuhi Standar Minimal Beban Mengajar (>= 24 JP)" else "Status: Beban Mengajar di bawah 24 JP (${guruJadwal.size} JP)"
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.color = if (guruJadwal.size >= 24) COLOR_PRIMARY else Color.rgb(220, 38, 38)
        canvas.drawText(statusText, leftX, sumY, paintText)

        // Right Signatures: Guru Ybs & Kepala Madrasah
        val kota = settingsMap["kota"] ?: "Jakarta"
        val kepala = settingsMap["kepala_madrasah"] ?: "Drs. H. Mulyadi, M.Pd.I"
        val nipKepala = settingsMap["nip_kepala"] ?: "19750312 200003 1 002"

        val sdf = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))
        val tanggalStr = sdf.format(Date())

        val sigStartY = startY + 8f
        val colGuruX = rightX + (rightWidth * 0.25f)
        val colKepalaX = rightX + (rightWidth * 0.75f)

        val paintSig = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 7.5f
            color = COLOR_TEXT_MAIN
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("Ditetapkan di: $kota, $tanggalStr", colKepalaX, sigStartY, paintSig)
        canvas.drawText("Guru Yang Bersangkutan,", colGuruX, sigStartY + 12f, paintSig)
        canvas.drawText("Mengetahui,", colKepalaX, sigStartY + 12f, paintSig)
        canvas.drawText("Kepala Madrasah,", colKepalaX, sigStartY + 21f, paintSig)

        val nameY = sigStartY + 50f
        paintSig.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintSig.isUnderlineText = true
        canvas.drawText(guru.nama, colGuruX, nameY, paintSig)
        canvas.drawText(kepala, colKepalaX, nameY, paintSig)

        paintSig.isUnderlineText = false
        paintSig.typeface = Typeface.DEFAULT
        paintSig.textSize = 7f
        canvas.drawText("NIP. ${guru.nip.ifBlank { "-" }}", colGuruX, nameY + 9f, paintSig)
        canvas.drawText("NIP. $nipKepala", colKepalaX, nameY + 9f, paintSig)
    }

    /**
     * Menggambar Rekapitulasi Guru Table Landscape
     */
    private fun drawRekapGuruTableLandscape(
        canvas: Canvas,
        pageWidth: Int,
        startY: Float,
        guruChunk: List<GuruEntity>,
        startIndex: Int,
        jadwalDetails: List<JadwalDetail>,
        activeHari: List<HariEntity>
    ): Float {
        val tableLeft = MARGIN_LANDSCAPE
        val tableRight = pageWidth - MARGIN_LANDSCAPE
        val tableWidth = tableRight - tableLeft

        val noWidth = 26f
        val nipWidth = 100f
        val nameWidth = 145f
        val mapelWidth = 105f
        val totalJpWidth = 45f
        val statusWidth = 110f

        val fixedWidth = noWidth + nipWidth + nameWidth + mapelWidth + totalJpWidth + statusWidth
        val dayColWidth = (tableWidth - fixedWidth) / activeHari.size.coerceAtLeast(1)

        val headerHeight = 22f
        val rowHeight = 16.5f

        val paintBg = Paint().apply { style = Paint.Style.FILL }
        val paintBorder = Paint().apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.75f
        }
        val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
        }

        var currentY = startY

        // Table Header
        paintBg.color = COLOR_PRIMARY
        canvas.drawRect(tableLeft, currentY, tableRight, currentY + headerHeight, paintBg)
        canvas.drawRect(tableLeft, currentY, tableRight, currentY + headerHeight, paintBorder)

        paintText.color = Color.WHITE
        paintText.textSize = 7.5f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        var curX = tableLeft
        canvas.drawText("NO", curX + noWidth / 2, currentY + 14f, paintText)
        curX += noWidth
        canvas.drawLine(curX, currentY, curX, currentY + headerHeight, paintBorder)

        canvas.drawText("NIP", curX + nipWidth / 2, currentY + 14f, paintText)
        curX += nipWidth
        canvas.drawLine(curX, currentY, curX, currentY + headerHeight, paintBorder)

        canvas.drawText("NAMA GURU", curX + nameWidth / 2, currentY + 14f, paintText)
        curX += nameWidth
        canvas.drawLine(curX, currentY, curX, currentY + headerHeight, paintBorder)

        canvas.drawText("MAPEL UTAMA", curX + mapelWidth / 2, currentY + 14f, paintText)
        curX += mapelWidth
        canvas.drawLine(curX, currentY, curX, currentY + headerHeight, paintBorder)

        // Day Columns (Senin..Sabtu)
        activeHari.forEach { h ->
            canvas.drawText(h.namaHari.take(3).uppercase(), curX + dayColWidth / 2, currentY + 14f, paintText)
            curX += dayColWidth
            canvas.drawLine(curX, currentY, curX, currentY + headerHeight, paintBorder)
        }

        canvas.drawText("TOTAL", curX + totalJpWidth / 2, currentY + 14f, paintText)
        curX += totalJpWidth
        canvas.drawLine(curX, currentY, curX, currentY + headerHeight, paintBorder)

        canvas.drawText("STATUS BEBAN", curX + statusWidth / 2, currentY + 14f, paintText)

        currentY += headerHeight

        guruChunk.forEachIndexed { idx, guru ->
            val guruJadwal = jadwalDetails.filter { it.guruId == guru.id }
            val totalJp = guruJadwal.size

            if (idx % 2 == 1) {
                paintBg.color = COLOR_ROW_ALT
                canvas.drawRect(tableLeft, currentY, tableRight, currentY + rowHeight, paintBg)
            }
            canvas.drawRect(tableLeft, currentY, tableRight, currentY + rowHeight, paintBorder)

            var rX = tableLeft

            // NO
            paintText.color = COLOR_TEXT_MAIN
            paintText.textSize = 7.5f
            paintText.typeface = Typeface.DEFAULT
            canvas.drawText("${startIndex + idx + 1}", rX + noWidth / 2, currentY + 11.5f, paintText)
            rX += noWidth
            canvas.drawLine(rX, currentY, rX, currentY + rowHeight, paintBorder)

            // NIP
            paintText.textSize = 6.8f
            paintText.color = COLOR_TEXT_MUTED
            canvas.drawText(guru.nip.ifBlank { "-" }, rX + nipWidth / 2, currentY + 11.5f, paintText)
            rX += nipWidth
            canvas.drawLine(rX, currentY, rX, currentY + rowHeight, paintBorder)

            // NAMA
            paintText.color = COLOR_TEXT_MAIN
            paintText.textSize = 7.5f
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paintText.textAlign = Paint.Align.LEFT
            canvas.drawText(guru.nama.take(24), rX + 4f, currentY + 11.5f, paintText)
            paintText.textAlign = Paint.Align.CENTER
            rX += nameWidth
            canvas.drawLine(rX, currentY, rX, currentY + rowHeight, paintBorder)

            // MAPEL
            paintText.color = COLOR_TEXT_MUTED
            paintText.textSize = 7f
            paintText.typeface = Typeface.DEFAULT
            canvas.drawText(guru.mapelUtama.take(18), rX + mapelWidth / 2, currentY + 11.5f, paintText)
            rX += mapelWidth
            canvas.drawLine(rX, currentY, rX, currentY + rowHeight, paintBorder)

            // Daily JP counts
            activeHari.forEach { h ->
                val dayJp = guruJadwal.count { it.hariId == h.id }
                paintText.color = if (dayJp > 0) COLOR_TEXT_MAIN else Color.rgb(203, 213, 225)
                paintText.textSize = 7.5f
                canvas.drawText(if (dayJp > 0) "$dayJp" else "-", rX + dayColWidth / 2, currentY + 11.5f, paintText)
                rX += dayColWidth
                canvas.drawLine(rX, currentY, rX, currentY + rowHeight, paintBorder)
            }

            // TOTAL JP
            val totalBebanJp = totalJp + guru.ekuivalensiJp
            paintText.color = COLOR_PRIMARY
            paintText.textSize = 7.5f
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val jpDisplay = if (guru.ekuivalensiJp > 0) "$totalJp+${guru.ekuivalensiJp}=$totalBebanJp" else "$totalJp JP"
            canvas.drawText(jpDisplay, rX + totalJpWidth / 2, currentY + 11.5f, paintText)
            rX += totalJpWidth
            canvas.drawLine(rX, currentY, rX, currentY + rowHeight, paintBorder)

            // STATUS
            val isEligible = totalBebanJp >= 24
            paintText.color = if (isEligible) COLOR_PRIMARY else Color.rgb(220, 38, 38)
            paintText.textSize = 6.8f
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val statusText = when {
                !guru.isSertifikasi -> "Non-TPG ($totalBebanJp JP)"
                isEligible -> "Layak TPG (>=24)"
                else -> "Kurang ${24 - totalBebanJp} JP"
            }
            canvas.drawText(statusText, rX + statusWidth / 2, currentY + 11.5f, paintText)

            currentY += rowHeight
        }

        return currentY
    }

    /**
     * Menggambar Panel Bawah Master Matriks: Legenda Guru & Pengesahan
     */
    private fun drawLandscapeFooterPanel(
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        startY: Float,
        guruList: List<GuruEntity>,
        mapelList: List<MapelEntity>,
        settingsMap: Map<String, String>,
        isLastPage: Boolean
    ) {
        val margin = MARGIN_LANDSCAPE
        val blockWidth = pageWidth - (2 * margin)

        if (!isLastPage) {
            val paintNote = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 7f
                color = COLOR_TEXT_MUTED
            }
            canvas.drawText("Catatan: Matriks master berlanjut pada halaman berikutnya. Lembar pengesahan tertera pada halaman akhir.", margin, pageHeight - 24f, paintNote)
            return
        }

        val leftWidth = blockWidth * 0.55f
        val rightWidth = blockWidth * 0.45f

        val leftX = margin
        val rightX = margin + leftWidth + 12f

        // Legend of Teachers & Subjects
        val paintTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 7.5f
            color = COLOR_PRIMARY
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 6.5f
            color = COLOR_TEXT_MAIN
        }

        canvas.drawText("Kode Guru & Mata Pelajaran Utama:", leftX, startY + 6f, paintTitle)

        val colW = leftWidth / 3f
        var curY = startY + 16f

        val sampleGurus = guruList.take(12)
        sampleGurus.forEachIndexed { idx, g ->
            val col = idx % 3
            val row = idx / 3
            val itemX = leftX + (col * colW)
            val itemY = curY + (row * 9f)

            if (itemY < pageHeight - 26f) {
                paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paintText.color = COLOR_PRIMARY
                val initial = getShortTeacherName(g.nama)
                canvas.drawText("[$initial]", itemX, itemY, paintText)

                paintText.typeface = Typeface.DEFAULT
                paintText.color = COLOR_TEXT_MAIN
                val desc = ": ${g.nama.take(12)}"
                canvas.drawText(desc, itemX + 22f, itemY, paintText)
            }
        }

        // Signatures on Right
        val kota = settingsMap["kota"] ?: "Jakarta"
        val kepala = settingsMap["kepala_madrasah"] ?: "Drs. H. Mulyadi, M.Pd.I"
        val nipKepala = settingsMap["nip_kepala"] ?: "19750312 200003 1 002"
        val waka = settingsMap["waka_kurikulum"] ?: "Ahmad Syafii, S.Pd., M.Si"
        val nipWaka = settingsMap["nip_waka"] ?: "19820514 200801 1 015"

        val sdf = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))
        val tanggalStr = sdf.format(Date())

        val sigStartY = startY + 6f
        val colLeftSigX = rightX + (rightWidth * 0.25f)
        val colRightSigX = rightX + (rightWidth * 0.75f)

        val paintSig = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 7.5f
            color = COLOR_TEXT_MAIN
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("Ditetapkan di: $kota, $tanggalStr", colRightSigX, sigStartY, paintSig)
        canvas.drawText("Waka Kurikulum / Pengembang,", colLeftSigX, sigStartY + 11f, paintSig)
        canvas.drawText("Mengetahui,", colRightSigX, sigStartY + 11f, paintSig)
        canvas.drawText("Kepala Madrasah,", colRightSigX, sigStartY + 20f, paintSig)

        val nameY = sigStartY + 48f
        paintSig.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintSig.isUnderlineText = true
        canvas.drawText(waka, colLeftSigX, nameY, paintSig)
        canvas.drawText(kepala, colRightSigX, nameY, paintSig)

        paintSig.isUnderlineText = false
        paintSig.typeface = Typeface.DEFAULT
        paintSig.textSize = 6.8f
        canvas.drawText("NIP. $nipWaka", colLeftSigX, nameY + 9f, paintSig)
        canvas.drawText("NIP. $nipKepala", colRightSigX, nameY + 9f, paintSig)
    }

    private fun drawLandscapeSignatures(
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        settingsMap: Map<String, String>
    ) {
        val margin = MARGIN_LANDSCAPE
        val kota = settingsMap["kota"] ?: "Jakarta"
        val kepala = settingsMap["kepala_madrasah"] ?: "Drs. H. Mulyadi, M.Pd.I"
        val nipKepala = settingsMap["nip_kepala"] ?: "19750312 200003 1 002"
        val waka = settingsMap["waka_kurikulum"] ?: "Ahmad Syafii, S.Pd., M.Si"
        val nipWaka = settingsMap["nip_waka"] ?: "19820514 200801 1 015"

        val sdf = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))
        val tanggalStr = sdf.format(Date())

        val sigStartY = pageHeight - 88f
        val colLeftSigX = margin + 140f
        val colRightSigX = pageWidth - margin - 140f

        val paintSig = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 8f
            color = COLOR_TEXT_MAIN
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("Ditetapkan di: $kota, $tanggalStr", colRightSigX, sigStartY, paintSig)
        canvas.drawText("Waka Kurikulum / Pengembang,", colLeftSigX, sigStartY + 12f, paintSig)
        canvas.drawText("Mengetahui,", colRightSigX, sigStartY + 12f, paintSig)
        canvas.drawText("Kepala Madrasah,", colRightSigX, sigStartY + 22f, paintSig)

        val nameY = sigStartY + 52f
        paintSig.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintSig.isUnderlineText = true
        canvas.drawText(waka, colLeftSigX, nameY, paintSig)
        canvas.drawText(kepala, colRightSigX, nameY, paintSig)

        paintSig.isUnderlineText = false
        paintSig.typeface = Typeface.DEFAULT
        paintSig.textSize = 7f
        canvas.drawText("NIP. $nipWaka", colLeftSigX, nameY + 10f, paintSig)
        canvas.drawText("NIP. $nipKepala", colRightSigX, nameY + 10f, paintSig)
    }

    // =========================================================================
    // DRAWING HELPERS: PORTRAIT (A4 VERTICAL)
    // =========================================================================

    private fun drawHeaderKopPortrait(canvas: Canvas, pageWidth: Int, settingsMap: Map<String, String>) {
        val namaMadrasah = settingsMap["nama_madrasah"] ?: "MTs DARUSSALAM"
        val kota = settingsMap["kota"] ?: "MAGETAN"
        val npsn = settingsMap["npsn"] ?: "20582511"
        val nsm = settingsMap["nsm"] ?: "121235200016"
        val alamat = settingsMap["alamat"] ?: "Lembeyan Kulon, Kec. Lembeyan"

        drawOfficialKemenagEmblem(canvas, MARGIN_PORTRAIT + 4f, MARGIN_PORTRAIT + 2f, 40f)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

        paint.color = COLOR_TEXT_MAIN
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("KEMENTERIAN AGAMA REPUBLIK INDONESIA", pageWidth / 2f, MARGIN_PORTRAIT + 12f, paint)

        paint.textSize = 8.5f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("KANTOR KEMENTERIAN AGAMA KABUPATEN / KOTA ${kota.uppercase()}", pageWidth / 2f, MARGIN_PORTRAIT + 24f, paint)

        paint.textSize = 12.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = COLOR_PRIMARY
        canvas.drawText(namaMadrasah.uppercase(), pageWidth / 2f, MARGIN_PORTRAIT + 39f, paint)

        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 7.5f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("NSM: $nsm • NPSN: $npsn • Alamat: $alamat, Kab. $kota", pageWidth / 2f, MARGIN_PORTRAIT + 51f, paint)

        val linePaintThick = Paint().apply {
            color = COLOR_PRIMARY
            strokeWidth = 2.0f
        }
        val linePaintThin = Paint().apply {
            color = COLOR_PRIMARY
            strokeWidth = 0.8f
        }

        val startX = MARGIN_PORTRAIT
        val endX = pageWidth - MARGIN_PORTRAIT
        val yThick = MARGIN_PORTRAIT + 58f
        val yThin = MARGIN_PORTRAIT + 61.5f

        canvas.drawLine(startX, yThick, endX, yThick, linePaintThick)
        canvas.drawLine(startX, yThin, endX, yThin, linePaintThin)
    }

    private fun drawDocumentTitlePortrait(
        canvas: Canvas,
        pageWidth: Int,
        startY: Float,
        title: String,
        subtitle: String,
        waliKelas: String
    ): Float {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var y = startY

        paint.color = COLOR_PRIMARY
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(title, pageWidth / 2f, y, paint)
        y += 13f

        paint.color = COLOR_TEXT_MAIN
        paint.textSize = 8.5f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(subtitle, pageWidth / 2f, y, paint)
        y += 12f

        if (waliKelas.isNotBlank()) {
            paint.color = COLOR_TEXT_MUTED
            paint.textSize = 8f
            canvas.drawText(waliKelas, pageWidth / 2f, y, paint)
            y += 12f
        }

        return y + 4f
    }

    private fun drawClassScheduleTablePortrait(
        canvas: Canvas,
        pageWidth: Int,
        startY: Float,
        activeHari: List<HariEntity>,
        activeJam: List<JamEntity>,
        classJadwal: List<JadwalDetail>
    ): Float {
        val tableLeft = MARGIN_PORTRAIT
        val tableRight = pageWidth - MARGIN_PORTRAIT
        val tableWidth = tableRight - tableLeft

        val timeColWidth = 65f
        val dayColWidth = (tableWidth - timeColWidth) / activeHari.size.coerceAtLeast(1)

        val headerHeight = 20f
        val rowHeight = 24f

        val paintBg = Paint().apply { style = Paint.Style.FILL }
        val paintBorder = Paint().apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.75f
        }
        val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
        }

        var currentY = startY

        paintBg.color = COLOR_PRIMARY
        canvas.drawRect(tableLeft, currentY, tableRight, currentY + headerHeight, paintBg)
        canvas.drawRect(tableLeft, currentY, tableRight, currentY + headerHeight, paintBorder)

        paintText.color = Color.WHITE
        paintText.textSize = 8f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("JAM / WAKTU", tableLeft + timeColWidth / 2, currentY + 13f, paintText)
        canvas.drawLine(tableLeft + timeColWidth, currentY, tableLeft + timeColWidth, currentY + headerHeight, paintBorder)

        var dayX = tableLeft + timeColWidth
        activeHari.forEach { h ->
            canvas.drawText(h.namaHari.uppercase(), dayX + dayColWidth / 2, currentY + 13f, paintText)
            canvas.drawLine(dayX + dayColWidth, currentY, dayX + dayColWidth, currentY + headerHeight, paintBorder)
            dayX += dayColWidth
        }

        currentY += headerHeight

        val slotMap = classJadwal.associateBy { "${it.hariId}_${it.jpKode}" }

        activeJam.forEachIndexed { rowIdx, jam ->
            if (jam.isIstirahat) {
                paintBg.color = COLOR_ACCENT_BG
                canvas.drawRect(tableLeft, currentY, tableRight, currentY + 16f, paintBg)
                canvas.drawRect(tableLeft, currentY, tableRight, currentY + 16f, paintBorder)

                paintText.color = COLOR_ACCENT_GOLD
                paintText.textSize = 7.5f
                paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

                val timeStr = "${jam.jamMulai}-${jam.jamSelesai}"
                canvas.drawText("${jam.jpKode} ($timeStr) - ISTIRAHAT", pageWidth / 2f, currentY + 11f, paintText)
                currentY += 16f
            } else {
                if (rowIdx % 2 == 1) {
                    paintBg.color = COLOR_ROW_ALT
                    canvas.drawRect(tableLeft, currentY, tableRight, currentY + rowHeight, paintBg)
                }
                canvas.drawRect(tableLeft, currentY, tableRight, currentY + rowHeight, paintBorder)

                paintText.color = COLOR_TEXT_MAIN
                paintText.textSize = 8f
                paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(jam.jpKode, tableLeft + timeColWidth / 2, currentY + 10f, paintText)

                paintText.textSize = 6.5f
                paintText.typeface = Typeface.DEFAULT
                paintText.color = COLOR_TEXT_MUTED
                canvas.drawText("${jam.jamMulai} - ${jam.jamSelesai}", tableLeft + timeColWidth / 2, currentY + 19f, paintText)

                canvas.drawLine(tableLeft + timeColWidth, currentY, tableLeft + timeColWidth, currentY + rowHeight, paintBorder)

                var cellX = tableLeft + timeColWidth
                activeHari.forEach { h ->
                    val slot = slotMap["${h.id}_${jam.jpKode}"]
                    if (slot != null) {
                        paintText.color = COLOR_PRIMARY
                        paintText.textSize = 8f
                        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        canvas.drawText(slot.mapelKode, cellX + dayColWidth / 2, currentY + 9.5f, paintText)

                        paintText.color = COLOR_TEXT_MAIN
                        paintText.textSize = 6.5f
                        paintText.typeface = Typeface.DEFAULT
                        val shortGuru = if (slot.namaGuru.length > 13) slot.namaGuru.take(11) + ".." else slot.namaGuru
                        canvas.drawText(shortGuru, cellX + dayColWidth / 2, currentY + 16.5f, paintText)

                        paintText.color = COLOR_TEXT_MUTED
                        paintText.textSize = 5.5f
                        canvas.drawText(slot.namaRuangan, cellX + dayColWidth / 2, currentY + 22.5f, paintText)
                    } else {
                        paintText.color = Color.rgb(203, 213, 225)
                        paintText.textSize = 8f
                        canvas.drawText("-", cellX + dayColWidth / 2, currentY + 14f, paintText)
                    }

                    canvas.drawLine(cellX + dayColWidth, currentY, cellX + dayColWidth, currentY + rowHeight, paintBorder)
                    cellX += dayColWidth
                }

                currentY += rowHeight
            }
        }

        return currentY
    }

    private fun drawMapelLegendPortrait(
        canvas: Canvas,
        pageWidth: Int,
        startY: Float,
        classJadwal: List<JadwalDetail>,
        guruList: List<GuruEntity>,
        mapelList: List<MapelEntity>
    ): Float {
        val usedMapelKodes = classJadwal.map { it.mapelKode }.distinct().sorted()
        if (usedMapelKodes.isEmpty()) return startY

        val tableLeft = MARGIN_PORTRAIT
        val tableRight = pageWidth - MARGIN_PORTRAIT
        val tableWidth = tableRight - tableLeft

        val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 7f
            color = COLOR_TEXT_MAIN
        }
        val paintTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 7.5f
            color = COLOR_PRIMARY
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("Keterangan Mata Pelajaran & Guru Pengampu:", tableLeft, startY, paintTitle)
        var y = startY + 10f

        val colWidth = tableWidth / 2f
        val halfSize = (usedMapelKodes.size + 1) / 2

        usedMapelKodes.forEachIndexed { idx, kode ->
            val mapel = mapelList.find { it.kode == kode }
            val sampleSlot = classJadwal.find { it.mapelKode == kode }
            val namaMapel = mapel?.namaMapel ?: sampleSlot?.namaMapel ?: kode
            val namaGuru = sampleSlot?.namaGuru ?: "-"

            val col = if (idx < halfSize) 0 else 1
            val row = if (idx < halfSize) idx else idx - halfSize

            val x = tableLeft + (col * colWidth)
            val itemY = y + (row * 10f)

            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("[$kode]", x, itemY, paintText)

            paintText.typeface = Typeface.DEFAULT
            val desc = " : $namaMapel ($namaGuru)"
            val truncated = if (desc.length > 38) desc.take(36) + ".." else desc
            canvas.drawText(truncated, x + 24f, itemY, paintText)
        }

        return y + (halfSize * 10f)
    }

    private fun drawGuruScheduleTablePortrait(
        canvas: Canvas,
        pageWidth: Int,
        startY: Float,
        guruJadwal: List<JadwalDetail>
    ): Float {
        val tableLeft = MARGIN_PORTRAIT
        val tableRight = pageWidth - MARGIN_PORTRAIT

        val noWidth = 30f
        val hariWidth = 70f
        val jpWidth = 70f
        val kelasWidth = 80f
        val mapelWidth = 140f
        val ruangWidth = (tableRight - tableLeft) - (noWidth + hariWidth + jpWidth + kelasWidth + mapelWidth)

        val headerHeight = 20f
        val rowHeight = 18f

        val paintBg = Paint().apply { style = Paint.Style.FILL }
        val paintBorder = Paint().apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.75f
        }
        val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
        }

        var currentY = startY

        paintBg.color = COLOR_PRIMARY
        canvas.drawRect(tableLeft, currentY, tableRight, currentY + headerHeight, paintBg)
        canvas.drawRect(tableLeft, currentY, tableRight, currentY + headerHeight, paintBorder)

        paintText.color = Color.WHITE
        paintText.textSize = 8f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        var x = tableLeft
        canvas.drawText("NO", x + noWidth / 2, currentY + 13f, paintText)
        x += noWidth
        canvas.drawLine(x, currentY, x, currentY + headerHeight, paintBorder)

        canvas.drawText("HARI", x + hariWidth / 2, currentY + 13f, paintText)
        x += hariWidth
        canvas.drawLine(x, currentY, x, currentY + headerHeight, paintBorder)

        canvas.drawText("JP / WAKTU", x + jpWidth / 2, currentY + 13f, paintText)
        x += jpWidth
        canvas.drawLine(x, currentY, x, currentY + headerHeight, paintBorder)

        canvas.drawText("KELAS", x + kelasWidth / 2, currentY + 13f, paintText)
        x += kelasWidth
        canvas.drawLine(x, currentY, x, currentY + headerHeight, paintBorder)

        canvas.drawText("MATA PELAJARAN", x + mapelWidth / 2, currentY + 13f, paintText)
        x += mapelWidth
        canvas.drawLine(x, currentY, x, currentY + headerHeight, paintBorder)

        canvas.drawText("RUANGAN", x + ruangWidth / 2, currentY + 13f, paintText)

        currentY += headerHeight

        guruJadwal.forEachIndexed { idx, slot ->
            if (idx % 2 == 1) {
                paintBg.color = COLOR_ROW_ALT
                canvas.drawRect(tableLeft, currentY, tableRight, currentY + rowHeight, paintBg)
            }
            canvas.drawRect(tableLeft, currentY, tableRight, currentY + rowHeight, paintBorder)

            var rowX = tableLeft
            paintText.color = COLOR_TEXT_MAIN
            paintText.textSize = 7.5f
            paintText.typeface = Typeface.DEFAULT

            // No
            canvas.drawText("${idx + 1}", rowX + noWidth / 2, currentY + 12f, paintText)
            rowX += noWidth
            canvas.drawLine(rowX, currentY, rowX, currentY + rowHeight, paintBorder)

            // Hari
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(slot.namaHari, rowX + hariWidth / 2, currentY + 12f, paintText)
            rowX += hariWidth
            canvas.drawLine(rowX, currentY, rowX, currentY + rowHeight, paintBorder)

            // JP
            paintText.typeface = Typeface.DEFAULT
            canvas.drawText("${slot.jpKode} (${slot.jamMulai})", rowX + jpWidth / 2, currentY + 12f, paintText)
            rowX += jpWidth
            canvas.drawLine(rowX, currentY, rowX, currentY + rowHeight, paintBorder)

            // Kelas
            paintText.color = COLOR_PRIMARY
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(slot.namaKelas, rowX + kelasWidth / 2, currentY + 12f, paintText)
            rowX += kelasWidth
            canvas.drawLine(rowX, currentY, rowX, currentY + rowHeight, paintBorder)

            // Mapel
            paintText.color = COLOR_TEXT_MAIN
            paintText.typeface = Typeface.DEFAULT
            canvas.drawText(slot.namaMapel, rowX + mapelWidth / 2, currentY + 12f, paintText)
            rowX += mapelWidth
            canvas.drawLine(rowX, currentY, rowX, currentY + rowHeight, paintBorder)

            // Ruang
            paintText.color = COLOR_TEXT_MUTED
            canvas.drawText(slot.namaRuangan, rowX + ruangWidth / 2, currentY + 12f, paintText)

            currentY += rowHeight
        }

        return currentY
    }

    private fun drawGuruSummaryPortrait(
        canvas: Canvas,
        pageWidth: Int,
        startY: Float,
        guruJadwal: List<JadwalDetail>,
        hariList: List<HariEntity>
    ): Float {
        val tableLeft = MARGIN_PORTRAIT
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 7.5f
            color = COLOR_TEXT_MAIN
        }

        val totalJp = guruJadwal.size
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Rekapitulasi Mengajar: Total $totalJp JP Tatap Muka per Minggu", tableLeft, startY, paint)

        var y = startY + 12f
        paint.typeface = Typeface.DEFAULT
        val breakdown = hariList.filter { it.isAktif }.joinToString(" | ") { h ->
            val count = guruJadwal.count { it.hariId == h.id }
            "${h.namaHari}: $count JP"
        }
        canvas.drawText(breakdown, tableLeft, y, paint)

        return y + 14f
    }

    private fun drawRekapGuruTablePortrait(
        canvas: Canvas,
        pageWidth: Int,
        startY: Float,
        guruChunk: List<GuruEntity>,
        startIndex: Int,
        jadwalDetails: List<JadwalDetail>
    ): Float {
        val tableLeft = MARGIN_PORTRAIT
        val tableRight = pageWidth - MARGIN_PORTRAIT
        val tableWidth = tableRight - tableLeft

        val noWidth = 30f
        val nipWidth = 110f
        val mapelWidth = 130f
        val jpWidth = 55f
        val statusWidth = 75f
        val nameWidth = tableWidth - (noWidth + nipWidth + mapelWidth + jpWidth + statusWidth)

        val headerHeight = 20f
        val rowHeight = 17f

        val paintBg = Paint().apply { style = Paint.Style.FILL }
        val paintBorder = Paint().apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.75f
        }
        val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
        }

        var currentY = startY

        paintBg.color = COLOR_PRIMARY
        canvas.drawRect(tableLeft, currentY, tableRight, currentY + headerHeight, paintBg)
        canvas.drawRect(tableLeft, currentY, tableRight, currentY + headerHeight, paintBorder)

        paintText.color = Color.WHITE
        paintText.textSize = 7.5f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        var rX = tableLeft
        canvas.drawText("NO", rX + noWidth / 2, currentY + 13f, paintText)
        rX += noWidth
        canvas.drawLine(rX, currentY, rX, currentY + headerHeight, paintBorder)

        canvas.drawText("NIP", rX + nipWidth / 2, currentY + 13f, paintText)
        rX += nipWidth
        canvas.drawLine(rX, currentY, rX, currentY + headerHeight, paintBorder)

        canvas.drawText("NAMA GURU", rX + nameWidth / 2, currentY + 13f, paintText)
        rX += nameWidth
        canvas.drawLine(rX, currentY, rX, currentY + headerHeight, paintBorder)

        canvas.drawText("MAPEL UTAMA", rX + mapelWidth / 2, currentY + 13f, paintText)
        rX += mapelWidth
        canvas.drawLine(rX, currentY, rX, currentY + headerHeight, paintBorder)

        canvas.drawText("TOTAL JP", rX + jpWidth / 2, currentY + 13f, paintText)
        rX += jpWidth
        canvas.drawLine(rX, currentY, rX, currentY + headerHeight, paintBorder)

        canvas.drawText("STATUS", rX + statusWidth / 2, currentY + 13f, paintText)

        currentY += headerHeight

        guruChunk.forEachIndexed { idx, guru ->
            val totalJp = jadwalDetails.count { it.guruId == guru.id }

            if (idx % 2 == 1) {
                paintBg.color = COLOR_ROW_ALT
                canvas.drawRect(tableLeft, currentY, tableRight, currentY + rowHeight, paintBg)
            }
            canvas.drawRect(tableLeft, currentY, tableRight, currentY + rowHeight, paintBorder)

            var rowX = tableLeft
            paintText.color = COLOR_TEXT_MAIN
            paintText.textSize = 7.5f
            paintText.typeface = Typeface.DEFAULT

            // No
            canvas.drawText("${startIndex + idx + 1}", rowX + noWidth / 2, currentY + 12f, paintText)
            rowX += noWidth
            canvas.drawLine(rowX, currentY, rowX, currentY + rowHeight, paintBorder)

            // NIP
            paintText.textSize = 6.8f
            paintText.color = COLOR_TEXT_MUTED
            canvas.drawText(guru.nip.ifBlank { "-" }, rowX + nipWidth / 2, currentY + 12f, paintText)
            rowX += nipWidth
            canvas.drawLine(rowX, currentY, rowX, currentY + rowHeight, paintBorder)

            // Nama
            paintText.color = COLOR_TEXT_MAIN
            paintText.textSize = 7.5f
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paintText.textAlign = Paint.Align.LEFT
            canvas.drawText(guru.nama.take(22), rowX + 4f, currentY + 12f, paintText)
            paintText.textAlign = Paint.Align.CENTER
            rowX += nameWidth
            canvas.drawLine(rowX, currentY, rowX, currentY + rowHeight, paintBorder)

            // Mapel
            paintText.color = COLOR_TEXT_MUTED
            paintText.textSize = 7f
            paintText.typeface = Typeface.DEFAULT
            canvas.drawText(guru.mapelUtama.take(18), rowX + mapelWidth / 2, currentY + 12f, paintText)
            rowX += mapelWidth
            canvas.drawLine(rowX, currentY, rowX, currentY + rowHeight, paintBorder)

            // Total JP
            val totalBebanJp = totalJp + guru.ekuivalensiJp
            paintText.color = COLOR_PRIMARY
            paintText.textSize = 7.5f
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val jpDisplay = if (guru.ekuivalensiJp > 0) "$totalJp+${guru.ekuivalensiJp}=$totalBebanJp" else "$totalJp JP"
            canvas.drawText(jpDisplay, rowX + jpWidth / 2, currentY + 12f, paintText)
            rowX += jpWidth
            canvas.drawLine(rowX, currentY, rowX, currentY + rowHeight, paintBorder)

            // Status
            val isEligible = totalBebanJp >= 24
            paintText.color = if (isEligible) COLOR_PRIMARY else Color.rgb(220, 38, 38)
            paintText.textSize = 6.8f
            val statusText = when {
                !guru.isSertifikasi -> "Non-TPG"
                isEligible -> "Memenuhi"
                else -> "Kurang (${24 - totalBebanJp})"
            }
            canvas.drawText(statusText, rowX + statusWidth / 2, currentY + 12f, paintText)

            currentY += rowHeight
        }

        return currentY
    }

    private fun drawSignatureBlockPortrait(
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        settingsMap: Map<String, String>
    ) {
        val kota = settingsMap["kota"] ?: "Jakarta"
        val kepala = settingsMap["kepala_madrasah"] ?: "Drs. H. Mulyadi, M.Pd.I"
        val nipKepala = settingsMap["nip_kepala"] ?: "19750312 200003 1 002"
        val waka = settingsMap["waka_kurikulum"] ?: "Ahmad Syafii, S.Pd., M.Si"
        val nipWaka = settingsMap["nip_waka"] ?: "19820514 200801 1 015"

        val sdf = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))
        val tanggalStr = sdf.format(Date())

        val bottomMargin = 40f
        val sigStartY = pageHeight - bottomMargin - 75f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 8f
            color = COLOR_TEXT_MAIN
            textAlign = Paint.Align.CENTER
        }

        val colLeftX = MARGIN_PORTRAIT + 100f
        val colRightX = pageWidth - MARGIN_PORTRAIT - 100f

        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Ditetapkan di: $kota", colRightX, sigStartY, paint)
        canvas.drawText("Pada tanggal: $tanggalStr", colRightX, sigStartY + 11f, paint)

        canvas.drawText("Waka Kurikulum / Tim Pengembang,", colLeftX, sigStartY + 24f, paint)
        canvas.drawText("Mengetahui,", colRightX, sigStartY + 24f, paint)
        canvas.drawText("Kepala Madrasah,", colRightX, sigStartY + 35f, paint)

        val nameY = sigStartY + 70f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.isUnderlineText = true
        canvas.drawText(waka, colLeftX, nameY, paint)
        canvas.drawText(kepala, colRightX, nameY, paint)

        paint.isUnderlineText = false
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("NIP. $nipWaka", colLeftX, nameY + 11f, paint)
        canvas.drawText("NIP. $nipKepala", colRightX, nameY + 11f, paint)
    }

    // =========================================================================
    // EMBLEM & FOOTER VECTOR HELPERS
    // =========================================================================

    /**
     * Menggambar Lambang / Emblem Resmi Kementerian Agama & Madrasah secara Vektor
     */
    private fun drawOfficialKemenagEmblem(canvas: Canvas, centerX: Float, centerY: Float, size: Float) {
        val half = size / 2f
        val cx = centerX + half
        val cy = centerY + half

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Golden Outer Glow Ring
        paint.color = COLOR_ACCENT_GOLD
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, half, paint)

        // 2. Deep Emerald Green Shield / Inner Circle
        paint.color = COLOR_PRIMARY
        canvas.drawCircle(cx, cy, half - 2.5f, paint)

        // 3. 5-pointed Golden Star at Top
        paint.color = COLOR_ACCENT_GOLD
        val starPath = Path().apply {
            val starSize = 5.5f
            val starY = cy - half * 0.45f
            moveTo(cx, starY - starSize)
            lineTo(cx + starSize * 0.35f, starY - starSize * 0.2f)
            lineTo(cx + starSize, starY - starSize * 0.2f)
            lineTo(cx + starSize * 0.45f, starY + starSize * 0.25f)
            lineTo(cx + starSize * 0.65f, starY + starSize * 0.85f)
            lineTo(cx, starY + starSize * 0.4f)
            lineTo(cx - starSize * 0.65f, starY + starSize * 0.85f)
            lineTo(cx - starSize * 0.45f, starY + starSize * 0.25f)
            lineTo(cx - starSize, starY - starSize * 0.2f)
            lineTo(cx - starSize * 0.35f, starY - starSize * 0.2f)
            close()
        }
        canvas.drawPath(starPath, paint)

        // 4. Open Book / Quran Symbol in Center
        paint.color = Color.WHITE
        val bookPath = Path().apply {
            val bookY = cy + 2f
            val bW = half * 0.48f
            val bH = half * 0.32f

            // Left page
            moveTo(cx, bookY + bH * 0.3f)
            quadTo(cx - bW * 0.5f, bookY - bH * 0.6f, cx - bW, bookY - bH * 0.3f)
            lineTo(cx - bW, bookY + bH * 0.6f)
            quadTo(cx - bW * 0.5f, bookY + bH * 0.2f, cx, bookY + bH)
            close()

            // Right page
            moveTo(cx, bookY + bH * 0.3f)
            quadTo(cx + bW * 0.5f, bookY - bH * 0.6f, cx + bW, bookY - bH * 0.3f)
            lineTo(cx + bW, bookY + bH * 0.6f)
            quadTo(cx + bW * 0.5f, bookY + bH * 0.2f, cx, bookY + bH)
            close()
        }
        canvas.drawPath(bookPath, paint)

        // Center spine / divider
        paint.color = COLOR_ACCENT_GOLD
        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(cx, cy - 2f, cx, cy + half * 0.35f, paint)
    }

    private fun drawFooterPageNumber(
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        currentPage: Int,
        totalPages: Int,
        margin: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 7f
            color = COLOR_TEXT_MUTED
        }

        val y = pageHeight - 14f

        // Left note
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Dokumen Resmi • Smart Scheduler Madrasah Cerdas Kemenag", margin, y, paint)

        // Right page number
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Halaman $currentPage dari $totalPages", pageWidth - margin, y, paint)
    }

    private fun getShortTeacherName(fullName: String): String {
        val parts = fullName.split(" ").filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "-"
            parts.size == 1 -> parts[0].take(6)
            parts.size == 2 -> "${parts[0].take(1)}. ${parts[1].take(6)}"
            else -> "${parts[0].take(1)}.${parts[1].take(1)}. ${parts[2].take(5)}"
        }
    }

    private fun savePdfToFile(context: Context, pdfDocument: PdfDocument, fileName: String): File {
        val pdfDir = File(context.cacheDir, "pdf_schedules").apply { if (!exists()) mkdirs() }
        val file = File(pdfDir, fileName)
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        return file
    }

    /**
     * Buka PDF di Aplikasi PDF Viewer atau Share Langsung
     */
    fun openOrSharePdf(context: Context, file: File, title: String = "Cetak Jadwal Pelajaran") {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "Berikut adalah file PDF $title (Format Resmi Siap Cetak).")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Buka atau Cetak Dokumen PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka file PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
