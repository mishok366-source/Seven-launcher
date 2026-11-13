package com.seven.gamehub

import android.content.Context
import androidx.core.content.FileProvider
import com.seven.gamehub.runtime.RuntimeAvailability
import com.seven.gamehub.runtime.WindowsRuntime
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.withContext

/**
 * Downloads the official compatibility installers and runs them one at a time
 * in the active SevenX Wine prefix. Completion is based on the guest process
 * exit callback; unsupported packages are reported and do not block later ones.
 */
internal object DependencyInstaller {
    private data class Package(
        val title: String,
        val url: String,
        val fileName: String,
        val arguments: String
    )

    private val packages = listOf(
        Package("Visual C++ v14 x86", "https://aka.ms/vc14/vc_redist.x86.exe", "vc14-x86.exe", "/install /quiet /norestart"),
        Package("Visual C++ v14 x64", "https://aka.ms/vc14/vc_redist.x64.exe", "vc14-x64.exe", "/install /quiet /norestart"),
        Package("Visual C++ 2013 x86", "https://aka.ms/highdpimfc2013x86enu", "vc2013-x86.exe", "/install /quiet /norestart"),
        Package("Visual C++ 2013 x64", "https://aka.ms/highdpimfc2013x64enu", "vc2013-x64.exe", "/install /quiet /norestart"),
        Package("Wine Mono 11.3 x86", "https://github.com/wine-mono/wine-mono/releases/download/wine-mono-11.3.0/wine-mono-11.3.0-x86.msi", "wine-mono-x86.msi", "/qn /norestart"),
        Package("Wine Mono 11.3 ARM64", "https://github.com/wine-mono/wine-mono/releases/download/wine-mono-11.3.0/wine-mono-11.3.0-arm64.msi", "wine-mono-arm64.msi", "/qn /norestart"),
        Package(".NET Desktop Runtime 10 x86", "https://builds.dotnet.microsoft.com/dotnet/WindowsDesktop/10.0.15/windowsdesktop-runtime-10.0.15-win-x86.exe", "dotnet10-x86.exe", "/install /quiet /norestart"),
        Package(".NET Desktop Runtime 10 x64", "https://builds.dotnet.microsoft.com/dotnet/WindowsDesktop/10.0.15/windowsdesktop-runtime-10.0.15-win-x64.exe", "dotnet10-x64.exe", "/install /quiet /norestart"),
        Package(".NET Desktop Runtime 10 ARM64", "https://builds.dotnet.microsoft.com/dotnet/WindowsDesktop/10.0.15/windowsdesktop-runtime-10.0.15-win-arm64.exe", "dotnet10-arm64.exe", "/install /quiet /norestart"),
        Package(".NET Framework 4.8", "https://download.microsoft.com/download/f/3/a/f3a6af84-da23-40a5-8d1c-49cc10c8e76f/NDP48-x86-x64-AllOS-ENU.exe", "dotnet-framework-48.exe", "/q /norestart"),
        Package("WebView2 Evergreen", "https://aka.ms/MicrosoftEdgeWebview2Setup.exe", "webview2-setup.exe", "/silent /install")
    )

    private val isRunning = AtomicBoolean(false)

    private val allowedHosts = setOf(
        "aka.ms",
        "github.com",
        "release-assets.githubusercontent.com",
        "builds.dotnet.microsoft.com",
        "download.microsoft.com",
        "download.visualstudio.microsoft.com",
        "go.microsoft.com",
        "msedge.sf.dl.delivery.mp.microsoft.com",
        "dotnetcli.azureedge.net"
    )

    suspend fun downloadAndInstall(
        context: Context,
        onProgress: suspend (String) -> Unit
    ): String {
        check(isRunning.compareAndSet(false, true)) { "Установка совместимости уже выполняется" }
        try {
        val status = WindowsRuntime.status(context)
        check(status.availability == RuntimeAvailability.READY) {
            "SevenX Desktop не найден. Сначала установи runtime SevenX Desktop."
        }

        val cache = File(context.filesDir, "compatibility-installers").apply { mkdirs() }
        val succeeded = mutableListOf<String>()
        val failed = mutableListOf<String>()
        val downloaded = mutableListOf<Pair<Package, File>>()

        // Phase 1: finish every download before opening any Windows installer.
        // This prevents a guest installer window from competing with the next
        // Android-side download and ensures the complete set is cached first.
        packages.forEachIndexed { index, item ->
            onProgress("Скачивание ${index + 1}/${packages.size}: ${item.title}")
            try {
                val installer = withContext(Dispatchers.IO) {
                    val destination = File(cache, item.fileName)
                    if (!destination.isFile || destination.length() < 1024L || !looksLikeInstaller(destination, item.fileName)) {
                        destination.delete()
                        val temporary = File(cache, item.fileName + ".part")
                        temporary.delete()
                        try {
                            downloadVerifiedHttps(item.url, temporary)
                            require(looksLikeInstaller(temporary, item.fileName)) {
                                "Скачанный файл не прошёл проверку формата EXE/MSI"
                            }
                            check(temporary.renameTo(destination)) { "Не удалось сохранить загруженный пакет" }
                        } catch (error: Exception) {
                            temporary.delete()
                            throw error
                        }
                    }
                    destination
                }
                downloaded += item to installer
                onProgress("Скачано ${downloaded.size}/${packages.size}: ${item.title}")
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                failed += "${item.title}: загрузка — ${error.message ?: "ошибка"}"
                onProgress("Не удалось скачать ${item.title}; перехожу к следующему пакету")
            }
        }

        // Phase 2: only after the download queue is finished, install the
        // cached packages sequentially in the active Wine prefix.
        onProgress("Загрузка завершена: ${downloaded.size}/${packages.size}. Начинаю установку.")
        downloaded.forEachIndexed { index, entry ->
            val (item, installer) = entry
            onProgress("Установка ${index + 1}/${downloaded.size}: ${item.title}")
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.files",
                    installer
                )
                val exitCode = WindowsRuntime.launchAndWait(
                    context = context,
                    executable = uri,
                    installArgs = item.arguments
                )
                // Give Winlator time to finish its display activity and return
                // SevenX to the foreground before the next installer is opened.
                delay(1_800)
                if (exitCode == 0) {
                    succeeded += item.title
                    onProgress("Готово: ${item.title}")
                } else {
                    failed += "${item.title}: Wine завершился с кодом $exitCode"
                    onProgress("Установщик ${item.title} завершился с кодом $exitCode; продолжаю")
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                failed += "${item.title}: ${error.message ?: "ошибка установки"}"
                onProgress("Не удалось завершить ${item.title}; продолжаю")
            }
        }

        return buildString {
            append("Установка набора завершена. Успешно: ${succeeded.size}/${packages.size}.")
            if (failed.isNotEmpty()) {
                append("\nОшибки/несовместимость:")
                failed.forEach { append("\n• ").append(it) }
            }
            append("\nУспешный код выхода не гарантирует, что компонент полностью поддерживается Wine.")
            append("\nARM64-пакеты могут быть несовместимы с x86_64 Wine-префиксом.")
        }
        } finally {
            isRunning.set(false)
        }
    }

    private fun looksLikeInstaller(file: File, fileName: String): Boolean {
        return runCatching {
            file.inputStream().buffered().use { input ->
                val header = ByteArray(64)
                var offset = 0
                while (offset < header.size) {
                    val count = input.read(header, offset, header.size - offset)
                    if (count < 0) return@use false
                    offset += count
                }
                if (fileName.endsWith(".msi", ignoreCase = true)) {
                    header.copyOfRange(0, 8).contentEquals(
                        byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11.toByte(), 0xE0.toByte(),
                            0xA1.toByte(), 0xB1.toByte(), 0x1A.toByte(), 0xE1.toByte())
                    )
                } else {
                    if (header[0] != 0x4D.toByte() || header[1] != 0x5A.toByte()) {
                        false
                    } else {
                        val peOffset = (header[0x3C].toLong() and 0xFF) or
                            ((header[0x3D].toLong() and 0xFF) shl 8) or
                            ((header[0x3E].toLong() and 0xFF) shl 16) or
                            ((header[0x3F].toLong() and 0xFF) shl 24)
                        if (peOffset < 64L || peOffset > file.length() - 4L || peOffset > 1024L * 1024L) {
                            false
                        } else {
                            RandomAccessFile(file, "r").use { random ->
                                random.seek(peOffset)
                                val signature = ByteArray(4)
                                random.readFully(signature)
                                signature.contentEquals(byteArrayOf(0x50.toByte(), 0x45.toByte(), 0x00.toByte(), 0x00.toByte()))
                            }
                        }
                    }
                }
            }
        }.getOrDefault(false)
    }

    private fun downloadVerifiedHttps(initialUrl: String, destination: File) {
        var current = URL(initialUrl)
        repeat(6) { redirectCount ->
            require(current.protocol.equals("https", ignoreCase = true)) {
                "Запрещён небезопасный URL"
            }
            require(current.host.lowercase(Locale.ROOT) in allowedHosts) {
                "Источник загрузки не входит в список доверенных"
            }

            val connection = (current.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 30_000
                readTimeout = 120_000
                setRequestProperty("User-Agent", "SevenX-Compatibility-Installer/0.13")
            }

            try {
                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location")
                        ?: error("Сервер вернул перенаправление без адреса")
                    require(redirectCount < 5) { "Слишком много перенаправлений" }
                    current = URL(current, location)
                    return@repeat
                }
                require(code == HttpURLConnection.HTTP_OK) {
                    "HTTP $code при загрузке"
                }
                val length = connection.contentLengthLong
                require(length != 0L && length >= -1L) { "Сервер вернул пустой файл" }

                connection.inputStream.use { input ->
                    FileOutputStream(destination).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var total = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            total += count
                            require(total <= 2L * 1024L * 1024L * 1024L) {
                                "Размер пакета превышает лимит 2 ГБ"
                            }
                        }
                        output.fd.sync()
                        require(total >= 1024L) { "Загруженный файл слишком мал" }
                        if (length > 0L) require(total == length) {
                            "Загрузка оборвалась: получено $total из $length байт"
                        }
                    }
                }
                return
            } finally {
                connection.disconnect()
            }
        }
        error("Не удалось завершить загрузку после перенаправлений")
    }
}
