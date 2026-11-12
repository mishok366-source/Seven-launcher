package com.seven.gamehub

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

private val Background = Color(0xFF090B12)
private val Panel = Color(0xFF141925)
private val Muted = Color(0xFF8D98AC)
private val Accent = Color(0xFF9B7BFF)
private val AccentBlue = Color(0xFF54C7F5)

private data class GameEntry(val name: String, val uri: Uri)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SevenGameHubApp() }
    }
}

@Composable
private fun SevenGameHubApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var games by remember { mutableStateOf(emptyList<GameEntry>()) }

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

            if (games.none { it.uri == uri }) games = games + GameEntry(name, uri)
            scope.launch { snackbar.showSnackbar("$name добавлена в библиотеку") }
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
                    onAdd = { exePicker.launch(arrayOf("*/*")) },
                    onRun = {
                        scope.launch { snackbar.showSnackbar("Wine/Proton ещё не подключён — это следующий этап") }
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
                        .background(Brush.linearGradient(listOf(Accent, Color(0xFF5742B8))),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(Icons.Rounded.Gamepad, null, tint = Color.White, modifier = Modifier.size(27.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("SEVEN", fontSize = 12.sp, color = Muted, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("GAMEHUB", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
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
                Text("Добавляй EXE-файлы. Поддержку Wine и Proton подключим следующим этапом.",
                    color = Color(0xFFB5BED0), fontSize = 13.sp, lineHeight = 19.sp)
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onAdd,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    androidx.compose.material3.Icon(Icons.Rounded.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить EXE-файл", fontWeight = FontWeight.Bold)
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
                GameCard(game = game, onRun = { onRun(game) })
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(Icons.Rounded.Info, null, tint = Muted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(7.dp))
                Text("Версия 0.1.0 • Основа интерфейса", color = Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun GameCard(game: GameEntry, onRun: () -> Unit) {
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
            Text("Windows • EXE", color = Muted, fontSize = 12.sp)
        }
        OutlinedButton(onClick = onRun, shape = RoundedCornerShape(12.dp)) {
            androidx.compose.material3.Icon(Icons.Rounded.PlayArrow, "Запустить", modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun SettingsScreen(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Настройки", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Среда запуска и параметры игр", color = Muted, fontSize = 14.sp)
        SettingsCard("Среда Windows", "Wine / Proton — будет подключено следующим этапом", Icons.Rounded.Gamepad, "НЕ ПОДКЛЮЧЕНО")
        SettingsCard("Графика", "Vulkan, DXVK, Turnip и OpenGL", Icons.Rounded.Tune, "СКОРО")
        SettingsCard("Контейнеры", "Отдельные префиксы и настройки для игр", Icons.Rounded.Settings, "СКОРО")
        Spacer(Modifier.height(4.dp))
        Text("Сначала создаём стабильную оболочку. Затем подключим совместимую Android-среду Wine и проверим реальный запуск EXE.",
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
