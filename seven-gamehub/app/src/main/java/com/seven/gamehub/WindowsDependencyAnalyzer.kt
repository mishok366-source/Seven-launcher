package com.seven.gamehub

import java.io.File
import android.content.Context
import android.net.Uri
import java.io.RandomAccessFile
import java.util.Locale

/**
 * Best-effort PE import-table inspection. This never executes the file and
 * never downloads or installs a dependency. It produces evidence-based hints
 * only; delay-loaded and dynamically loaded DLLs cannot be discovered here.
 */
internal object WindowsDependencyAnalyzer {
    fun analyze(context: Context, uri: Uri): String = runCatching {
        val temporary = File.createTempFile("sevenx-pe-", ".bin", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                temporary.outputStream().use { output -> input.copyTo(output) }
            } ?: return "Dependency scan unavailable"
            analyze(temporary)
        } finally {
            temporary.delete()
        }
    }.getOrElse { "Dependency scan unavailable" }

    fun analyze(file: File): String = runCatching {
        RandomAccessFile(file, "r").use { pe ->
            if (pe.length() < 256L || pe.readUnsignedShortLE(0) != 0x5A4D) return "PE analysis unavailable"
            val peOffset = pe.readUnsignedIntLE(0x3C)
            if (peOffset < 64L || peOffset > pe.length() - 24L) return "PE analysis unavailable"
            if (pe.readUnsignedIntLE(peOffset) != 0x00004550L) return "PE analysis unavailable"

            val machine = pe.readUnsignedShortLE(peOffset + 4)
            val sectionCount = pe.readUnsignedShortLE(peOffset + 6)
            val optionalSize = pe.readUnsignedShortLE(peOffset + 20)
            val optional = peOffset + 24
            val magic = pe.readUnsignedShortLE(optional)
            val directoryBase = when (magic) {
                0x10B -> optional + 96
                0x20B -> optional + 112
                else -> return "PE format not recognized"
            }
            val importRva = pe.readUnsignedIntLE(directoryBase + 8)
            val sectionTable = optional + optionalSize
            val sections = ArrayList<Section>()
            for (i in 0 until minOf(sectionCount, 96)) {
                val at = sectionTable + i * 40L
                if (at + 40 > pe.length()) break
                sections += Section(
                    pe.readUnsignedIntLE(at + 12),
                    maxOf(pe.readUnsignedIntLE(at + 8), pe.readUnsignedIntLE(at + 16)),
                    pe.readUnsignedIntLE(at + 20)
                )
            }
            fun offsetFor(rva: Long): Long? {
                val section = sections.firstOrNull { rva >= it.virtualAddress && rva < it.virtualAddress + it.span } ?: return null
                return section.rawOffset + (rva - section.virtualAddress)
            }
            val imports = linkedSetOf<String>()
            val importOffset = offsetFor(importRva)
            if (importOffset != null && importRva != 0L) {
                for (i in 0 until 512) {
                    val descriptor = importOffset + i * 20L
                    if (descriptor + 20 > pe.length()) break
                    val nameRva = pe.readUnsignedIntLE(descriptor + 12)
                    if (nameRva == 0L) break
                    val nameOffset = offsetFor(nameRva) ?: continue
                    val dll = pe.readAsciiZ(nameOffset, 260).lowercase(Locale.ROOT)
                    if (dll.isNotBlank()) imports += dll
                }
            }

            val hints = linkedSetOf<String>()
            if (imports.any { it.matches(Regex("msvcr[0-9]+\\.dll")) || it.matches(Regex("msvcp[0-9]+\\.dll")) || it.matches(Regex("vcruntime[0-9]+\\.dll")) }) {
                val legacy = imports.filter { it.matches(Regex("msvcr[0-9]+\\.dll")) || it.matches(Regex("msvcp[0-9]+\\.dll")) }
                if (legacy.any { it.contains("80") }) hints += "VC++ 2005"
                if (legacy.any { it.contains("90") }) hints += "VC++ 2008"
                if (legacy.any { it.contains("100") }) hints += "VC++ 2010"
                if (legacy.any { it.contains("110") }) hints += "VC++ 2012"
                if (legacy.any { it.contains("120") }) hints += "VC++ 2013"
                if (imports.any { it.contains("140") || it.contains("141") || it.contains("142") || it.contains("143") || it.contains("144") }) hints += "VC++ v14 (2015+)"
            }
            if ("mscoree.dll" in imports) hints += ".NET / CLR (version needs identification)"
            if (imports.any { it == "d3d9.dll" || it == "d3d10.dll" || it == "d3d11.dll" || it == "d3d12.dll" || it == "dxgi.dll" }) hints += "DirectX (DXVK/WineD3D/VKD3D profile)"
            if (imports.any { it.startsWith("xinput") || it.startsWith("dinput") }) hints += "DirectInput/XInput"
            if (imports.any { it == "webview2loader.dll" }) hints += "WebView2 loader detected (runtime support not guaranteed)"

            // Only request optional native components when static imports prove they are used.
            val autoComponents = linkedSetOf<String>()
            if (imports.any { it in setOf("quartz.dll", "strmiids.dll", "devenum.dll", "qedit.dll") }) {
                hints += "DirectShow"
                autoComponents += "directshow"
            }
            if (imports.any { it in setOf("dplay.dll", "dplayx.dll", "dpnet.dll") }) {
                hints += "DirectPlay"
                autoComponents += "directplay"
            }
            if (hints.isEmpty()) hints += "No known runtime dependency found in static imports"

            val arch = when (machine) {
                0x014c -> "x86"
                0x8664 -> "x64"
                0xAA64 -> "ARM64"
                0x01c4 -> "ARM"
                else -> "unknown architecture"
            }
            val componentMarker = if (autoComponents.isEmpty()) "" else
                " [SEVENX_COMPONENTS:" + autoComponents.joinToString(",") + "]"
            "$arch • " + hints.joinToString(", ") + componentMarker
        }
    }.getOrElse { "Dependency scan unavailable" }

    private data class Section(val virtualAddress: Long, val span: Long, val rawOffset: Long)

    private fun RandomAccessFile.readUnsignedShortLE(offset: Long): Int {
        seek(offset)
        val a = read()
        val b = read()
        if (a < 0 || b < 0) throw IllegalArgumentException("Truncated PE")
        return a or (b shl 8)
    }

    private fun RandomAccessFile.readUnsignedIntLE(offset: Long): Long {
        seek(offset)
        val a = read()
        val b = read()
        val c = read()
        val d = read()
        if (a < 0 || b < 0 || c < 0 || d < 0) throw IllegalArgumentException("Truncated PE")
        return (a.toLong() and 255) or ((b.toLong() and 255) shl 8) or
            ((c.toLong() and 255) shl 16) or ((d.toLong() and 255) shl 24)
    }

    private fun RandomAccessFile.readAsciiZ(offset: Long, limit: Int): String {
        seek(offset)
        val bytes = ByteArray(limit)
        var count = 0
        while (count < limit) {
            val value = read()
            if (value <= 0) break
            bytes[count++] = value.toByte()
        }
        return String(bytes, 0, count, Charsets.US_ASCII)
    }
}
