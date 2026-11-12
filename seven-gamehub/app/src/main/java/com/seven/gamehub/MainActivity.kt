package com.seven.gamehub

import android.net.Uri
import android.content.Context
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.Build
import java.io.File
import java.util.UUID
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.seven.gamehub.runtime.RuntimeLaunchResult
import com.seven.gamehub.runtime.WindowsRuntime
import com.seven.gamehub.runtime.RuntimeDiagnostics

private val Background = Color(0xFF090B12)
private val Panel = Color(0xFF141925)
private val Muted = Color(0xFF8D98AC)
private val Accent = Color(0xFF9B7BFF)
private val AccentBlue = Color(0xFF54C7F5)

private data class GameEntry(val name: String, val uri: Uri, val isInstaller: Boolean = false)

private fun pinGameShortcut(context: Context, game: GameEntry): Boolean {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return false
    val manager = context.getSystemService(ShortcutManager::class.java) ?: return false
    if (!manager.isRequestPinShortcutSupported) return false
    val launchIntent = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        putExtra("sevenx_game_uri", game.uri.toString())
        putExtra("sevenx_game_name", game.name)
        putExtra("sevenx_game_is_installer", game.isInstaller)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    val shortcut = ShortcutInfo.Builder(context, "sevenx_" + game.uri.toString().hashCode())
        .setShortLabel(game.name.take(10))
        .setLongLabel("Запустить " + game.name)
        .setIcon(Icon.createWithResource(context, android.R.mipmap.sym_def_app_icon))
        .setIntent(launchIntent)
        .build()
    return manager.requestPinShortcut(shortcut, null)
}

private const val PREFS_NAME = "seven_gamehub_library"

private fun isExeName(name: String): Boolean = name.trim().lowercase().endsWith(".exe")

// Installer entries are removed from the SevenX library only after the runtime
// confirms that Windows successfully started the installer. The imported file
// itself is never deleted by this policy.
private fun isInstallerName(name: String): Boolean {
    val baseName = name.substringBeforeLast('.', name).lowercase()
    return baseName == "setup" || baseName.contains("setup") ||
        baseName == "install" || baseName.contains("installer") || baseName.contains("install")
}

private fun hasPortableExecutableHeader(context: Context, uri: Uri): Boolean {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.buffered()?.use { input ->
            val dosHeader = ByteArray(64)
            var headerRead = 0
            while (headerRead < dosHeader.size) {
                val count = input.read(dosHeader, headerRead, dosHeader.size - headerRead)
                if (count < 0) return@use false
                headerRead += count
            }

            // IMAGE_DOS_HEADER.e_lfanew is a little-endian file offset at 0x3C.
            if (dosHeader[0] != 0x4D.toByte() || dosHeader[1] != 0x5A.toByte()) {
                return@use false
            }
            val peOffset = (dosHeader[0x3C].toLong() and 0xFF) or
                ((dosHeader[0x3D].toLong() and 0xFF) shl 8) or
                ((dosHeader[0x3E].toLong() and 0xFF) shl 16) or
                ((dosHeader[0x3F].toLong() and 0xFF) shl 24)

            // Reject unreasonable offsets and malformed/truncated headers.
            if (peOffset < 64L || peOffset > 1024L * 1024L) return@use false
            var remaining = peOffset - dosHeader.size
            while (remaining > 0L) {
                val skipped = input.skip(remaining)
                if (skipped <= 0L) {
                    if (input.read() < 0) return@use false
                    remaining--
                } else {
                    remaining -= skipped
                }
            }

            input.read() == 0x50 && input.read() == 0x45 &&
                input.read() == 0x00 && input.read() == 0x00
        } ?: false
    }.getOrDefault(false)
}

private suspend fun importExternalExe(context: Context, uri: Uri): GameEntry? = withContext(Dispatchers.IO) {
    runCatching {
        val displayName = context.contentResolver.query(
            uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: uri.lastPathSegment ?: "Windows-app.exe"

        if (!isExeName(displayName)) return@withContext null
        if (!hasPortableExecutableHeader(context, uri)) return@withContext null

        val importDir = File(context.filesDir, "exe-imports").apply { mkdirs() }
        val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(100)
        val destination = File(importDir, "${UUID.randomUUID()}_$safeName")
        context.contentResolver.openInputStream(uri)?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        } ?: return@withContext null
        GameEntry(displayName, FileProvider.getUriForFile(context, "${context.packageName}.files", destination), isInstallerName(displayName))
    }.getOrNull()
}

private const val PREFS_GAMES = "games"

private fun loadSavedGames(context: android.content.Context): List<GameEntry> {
    return runCatching {
        val saved = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .getString(PREFS_GAMES, "[]") ?: "[]"
        val array = org.json.JSONArray(saved)
        val permissions = context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission }.map { it.uri.toString() }.toSet()
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val uriString = item.getString("uri")
                val savedUri = Uri.parse(uriString)
                val privateFileExists = savedUri.scheme == "file" && savedUri.path?.let { File(it).isFile } == true
                val providerFileExists = savedUri.authority == "${context.packageName}.files" &&
                    runCatching { context.contentResolver.openFileDescriptor(savedUri, "r")?.use { true } ?: false }.getOrDefault(false)
                if (privateFileExists || providerFileExists || uriString in permissions) {
                    add(GameEntry(item.getString("name"), savedUri, item.optBoolean("isInstaller", false)))
                }
            }
        }
    }.getOrDefault(emptyList())
}

private fun saveGames(context: android.content.Context, games: List<GameEntry>) {
    val array = org.json.JSONArray()
    games.forEach { game ->
        array.put(org.json.JSONObject().put("name", game.name).put("uri", game.uri.toString()).put("isInstaller", game.isInstaller))
    }
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .edit().putString(PREFS_GAMES, array.toString()).apply()
}

class MainActivity : ComponentActivity() {
    private val libraryRefreshGeneration = mutableIntStateOf(0)
    private val shortcutGameUri = mutableStateOf<String?>(null)
    private val incomingExeUri = mutableStateOf<String?>(null)

    private fun readLaunchIntent(launchIntent: Intent?) {
        shortcutGameUri.value = launchIntent?.getStringExtra("sevenx_game_uri")
        incomingExeUri.value = if (shortcutGameUri.value == null &&
            launchIntent?.action == Intent.ACTION_VIEW) launchIntent.data?.toString() else null
    }

    override fun onResume() {
        super.onResume()
        libraryRefreshGeneration.intValue++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readLaunchIntent(intent)
        setContent {
            SevenGameHubApp(
                shortcutGameUri = shortcutGameUri.value,
                onShortcutHandled = { shortcutGameUri.value = null },
                incomingExeUri = incomingExeUri.value,
                onIncomingExeHandled = { incomingExeUri.value = null },
                libraryRefreshGeneration = libraryRefreshGeneration.intValue
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readLaunchIntent(intent)
    }
}

@Composable
private fun SevenGameHubApp(
    shortcutGameUri: String?,
    onShortcutHandled: () -> Unit,
    incomingExeUri: String?,
    onIncomingExeHandled: () -> Unit,
    libraryRefreshGeneration: Int
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var games by remember(context) { mutableStateOf(loadSavedGames(context)) }
    var pickerIsInstaller by remember { mutableStateOf(false) }

    LaunchedEffect(games) {
        saveGames(context, games)
    }

    LaunchedEffect(libraryRefreshGeneration) {
        if (libraryRefreshGeneration > 0) {
            val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val resultUri = preferences.getString("runtime_last_result_uri", null)
            if (resultUri != null) {
                val previous = games.firstOrNull { it.uri.toString() == resultUri }
                val started = preferences.getBoolean("runtime_last_result_started", false)
                games = loadSavedGames(context)
                snackbar.showSnackbar(
                    if (started) {
                        if (previous?.isInstaller == true) "Установщик запущен и убран из библиотеки. Файл остался на устройстве."
                        else "${previous?.name ?: "Приложение"} запущено в Winlator."
                    } else {
                        "Winlator не подтвердил запуск. Запись и файл сохранены."
                    }
                )
                preferences.edit().remove("runtime_last_result_uri").remove("runtime_last_result_started").apply()
            } else {
                games = loadSavedGames(context)
            }
        }
    }

    LaunchedEffect(shortcutGameUri, games) {
        val targetUri = shortcutGameUri
        if (targetUri != null) {
            val targetGame = games.firstOrNull { it.uri.toString() == targetUri }
            if (targetGame != null) {
                selectedTab = 0
                snackbar.showSnackbar("Открыт ярлык: ${targetGame.name}. Для запуска нужен Windows runtime.")
            } else {
                snackbar.showSnackbar("Файл из ярлыка не найден в библиотеке.")
            }
            onShortcutHandled()
        }
    }

    LaunchedEffect(incomingExeUri) {
        val uriString = incomingExeUri ?: return@LaunchedEffect
        selectedTab = 0
        val imported = importExternalExe(context, Uri.parse(uriString))
        if (imported != null) {
            val existing = games.firstOrNull { it.name == imported.name }
            if (existing == null) games = games + imported
            snackbar.showSnackbar(
                if (existing == null) "${imported.name} импортирован в SevenX" else "${imported.name} уже есть в библиотеке"
            )
        } else {
            snackbar.showSnackbar("Не удалось открыть файл. Нужен корректный EXE с расширением .exe.")
        }
        onIncomingExeHandled()
    }

    val exePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val name = context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: uri.lastPathSegment ?: "Windows game.exe"

            if (!isExeName(name) || !hasPortableExecutableHeader(context, uri)) {
                scope.launch { snackbar.showSnackbar("Это не распознанный Windows EXE-файл") }
            } else {
                if (games.none { it.uri == uri }) games = games + GameEntry(name, uri, pickerIsInstaller || isInstallerName(name))
                scope.launch { snackbar.showSnackbar("$name добавлен в библиотеку") }
            }
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent, secondary = AccentBlue, background = Background,
            surface = Panel, onBackground = Color(0xFFF4F5FA), onSurface = Color(0xFFF4F5FA)
        )
    ) {
        Scaffold(
            containerColor = Background,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF101521), contentColor = Color.White) {
                    NavigationBarItem(
                        selected = selectedTab == 0, onClick = { selectedTab = 0 },
                        icon = { androidx.compose.material3.Icon(Icons.Rounded.Gamepad, null) },
                        label = { Text("Игры") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1, onClick = { selectedTab = 1 },
                        icon = { androidx.compose.material3.Icon(Icons.Rounded.Tune, null) },
                        label = { Text("Настройки") }
                    )
                }
            }
        ) { padding ->
            if (selectedTab == 0) {
                LibraryScreen(
                    games = games,
                    onAdd = { pickerIsInstaller = false; exePicker.launch(arrayOf("*/*")) },
                    onAddSetup = { pickerIsInstaller = true; exePicker.launch(arrayOf("*/*")) },
                    onPinShortcut = { game ->
                        val pinned = pinGameShortcut(context, game)
                        scope.launch { snackbar.showSnackbar(if (pinned) "Запрос на создание ярлыка отправлен" else "Не удалось закрепить ярлык") }
                    },
                    onRemove = { game ->
                        games = games.filterNot { it.uri == game.uri }
                    },
                    onRun = { game ->
                        scope.launch {
                            when (WindowsRuntime.launch(context, game.uri)) {
                                RuntimeLaunchResult.STARTED -> {
                                    if (game.isInstaller) {
                                        // Remove only the library entry. Do not delete game.uri or its file.
                                        games = games.filterNot { it.uri == game.uri }
                                        snackbar.showSnackbar("${game.name} запущен и убран из библиотеки. Файл остался на устройстве.")
                                    } else {
                                        snackbar.showSnackbar("${game.name} запущен")
                                    }
                                }
                                RuntimeLaunchResult.LAUNCH_REQUESTED -> snackbar.showSnackbar(
                                    "${game.name} передан в Winlator. Ждём подтверждения запуска."
                                )
                                RuntimeLaunchResult.NOT_INSTALLED -> snackbar.showSnackbar(
                                    "Winlator с поддержкой SevenX не установлен. Установщик остался в библиотеке."
                                )
                                RuntimeLaunchResult.FAILED -> snackbar.showSnackbar(
                                    "Не удалось запустить ${game.name}. Запись и файл сохранены."
                                )
                            }
                        }
                    },
                    modifier = Modifier.padding(padding)
                )
            } else {
                SettingsScreen(modifier = Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    games: List<GameEntry>,
    onAdd: () -> Unit,
    onAddSetup: () -> Unit,
    onPinShortcut: (GameEntry) -> Unit,
    onRemove: (GameEntry) -> Unit,
    onRun: (GameEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp, top = 22.dp, end = 20.dp, bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(15.dp))
                        .background(Brush.linearGradient(listOf(Accent, Color(0xFF5742B8)))),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(Icons.Rounded.Gamepad, null, tint = Color.White, modifier = Modifier.size(27.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("SEVENX", fontSize = 12.sp, color = Muted, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("SEVENX", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
                Surface(color = Color(0xFF242039), shape = CircleShape) {
                    Text("PREVIEW", color = Color(0xFFC5B7FF), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp))
                }
            }
        }
        item {
            Column {
                Text("Твоя игровая библиотека", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(5.dp))
                Text("Все Windows-игры в одном месте", color = Muted, fontSize = 14.sp)
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(25.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF29234B), Color(0xFF172A40), Color(0xFF111B2A))))
                    .border(1.dp, Color(0xFF51477D), RoundedCornerShape(25.dp)).padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color(0x33B5A1FF), shape = CircleShape) {
                        androidx.compose.material3.Icon(Icons.Rounded.Gamepad, null, tint = Color(0xFFD5CBFF),
                            modifier = Modifier.padding(10.dp).size(25.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("WINDOWS GAMING", color = Color(0xFFC9C0F7), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                }
                Spacer(Modifier.height(17.dp))
                Text("Твой ПК.\nТеперь в кармане.", color = Color.White, fontSize = 25.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(8.dp))
                Text("Библиотека EXE, ярлыки и запуск через Winlator. Совместимость зависит от игры и устройства.",
                    color = Color(0xFFB5BED0), fontSize = 13.sp, lineHeight = 19.sp)
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onAdd,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    androidx.compose.material3.Icon(Icons.Rounded.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить EXE", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(9.dp))
                OutlinedButton(
                    onClick = onAddSetup,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    androidx.compose.material3.Icon(Icons.Rounded.FolderOpen, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить SETUP / установщик")
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("МОЯ БИБЛИОТЕКА", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                Spacer(Modifier.weight(1f))
                Text("${games.size} ИГР", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (games.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Panel)
                        .border(1.dp, Color(0xFF252D3C), RoundedCornerShape(22.dp))
                        .padding(vertical = 30.dp, horizontal = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(Color(0xFF22233A)), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Icon(Icons.Rounded.FolderOpen, null, tint = Accent, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Здесь пока пусто", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Выбери EXE-файл, чтобы добавить\nпервую игру в библиотеку", color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
                    Spacer(Modifier.height(17.dp))
                    OutlinedButton(onClick = onAdd, shape = RoundedCornerShape(13.dp)) {
                        androidx.compose.material3.Icon(Icons.Rounded.FolderOpen, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Выбрать файл")
                    }
                }
            }
        } else {
            items(games, key = { it.uri.toString() }) { game ->
                GameCard(
                    game = game,
                    onRun = { onRun(game) },
                    onPinShortcut = { onPinShortcut(game) },
                    onRemove = { onRemove(game) }
                )
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(Icons.Rounded.Info, null, tint = Muted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(7.dp))
                Text("Версия 0.7.0 • Winlator Bridge", color = Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun GameCard(game: GameEntry, onRun: () -> Unit, onPinShortcut: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel)
            .border(1.dp, Color(0xFF283142), RoundedCornerShape(18.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF292440)), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(Icons.Rounded.Gamepad, null, tint = Accent, modifier = Modifier.size(25.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(game.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(if (game.isInstaller) "Windows • SETUP" else "Windows • EXE", color = Muted, fontSize = 12.sp)
        }
        OutlinedButton(onClick = onRun, shape = RoundedCornerShape(12.dp)) {
            androidx.compose.material3.Icon(Icons.Rounded.PlayArrow, "Запустить", modifier = Modifier.size(19.dp))
        }
        IconButton(onClick = onPinShortcut) {
            androidx.compose.material3.Icon(Icons.Rounded.Add, "Ярлык на рабочий стол", tint = AccentBlue)
        }
        IconButton(onClick = onRemove) {
            androidx.compose.material3.Icon(Icons.Rounded.Delete, "Удалить из библиотеки", tint = Muted)
        }
    }
}

@Composable
private fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val runtimeStatus = WindowsRuntime.status(context)
    val diagnostics = RuntimeDiagnostics.inspect(context)
    val runtimeLabel = when (runtimeStatus.availability) {
        com.seven.gamehub.runtime.RuntimeAvailability.READY -> "ГОТОВО"
        com.seven.gamehub.runtime.RuntimeAvailability.NOT_INSTALLED -> "НЕ ПОДКЛЮЧЕНО"
        com.seven.gamehub.runtime.RuntimeAvailability.UNSUPPORTED_DEVICE -> "НЕ ПОДДЕРЖИВАЕТСЯ"
        com.seven.gamehub.runtime.RuntimeAvailability.INITIALIZING -> "ЗАПУСК"
        com.seven.gamehub.runtime.RuntimeAvailability.ERROR -> "ОШИБКА"
    }
    Column(modifier = modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Настройки", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Среда запуска и параметры игр", color = Muted, fontSize = 14.sp)
        SettingsCard(runtimeStatus.engineName, runtimeStatus.message, Icons.Rounded.Gamepad, runtimeLabel)
        SettingsCard("Графика", "Vulkan, DXVK, Turnip и OpenGL", Icons.Rounded.Tune, "СКОРО")
        SettingsCard("Контейнеры", "Отдельные префиксы и настройки для игр", Icons.Rounded.Settings, "СКОРО")
        Spacer(Modifier.height(4.dp))
        SettingsCard("Архитектура устройства", diagnostics.primaryAbi, Icons.Rounded.Info, if (diagnostics.supportsArm64) "ARM64" else "ABI")
        SettingsCard("Совместимость", diagnostics.summary, Icons.Rounded.Tune, if (diagnostics.supportsArm64) "ПРОВЕРЕНО" else "ОГРАНИЧЕНО")
        Spacer(Modifier.height(4.dp))
        Text("EXE-файлы принимаются через файловый менеджер и проверяются перед импортом. Для реального запуска ещё требуется встроенная среда Wine и соответствующий транслятор x86/x64. Совместимость зависит от процессора и графического драйвера.",
            color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun SettingsCard(title: String, subtitle: String, icon: ImageVector, status: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFF292440)), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(icon, null, tint = Accent)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Text(status, color = Color(0xFFB7A9FF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
