package com.simha.dailyvitamins

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.ZoneId
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val vm: MainViewModel = viewModel(factory = MainViewModel.factory(applicationContext))
                MainScreen(vm = vm)
            }
        }
    }
}

@Composable
fun MainScreen(vm: MainViewModel) {
    val ui by vm.ui.collectAsState()
    val tz = ZoneId.of("Asia/Jerusalem")
    val today = LocalDate.now(tz).toString()

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "היום: $today")
            if (ui.isMWF) {
                Row {
                    Text("יום אימון")
                    Switch(checked = ui.workoutDay, onCheckedChange = { vm.setWorkoutDay(it) })
                }
            }
        }

        val tabs = listOf("השכמה","בוקר","ערב","לילה")
        var selTab by remember { mutableStateOf(0) }
        TabRow(selectedTabIndex = selTab) {
            tabs.forEachIndexed { i, label ->
                Tab(selected = selTab == i, onClick = { selTab = i }, text = { Text(label) })
            }
        }

        val parts = listOf(DayPart.WAKE, DayPart.MORNING, DayPart.EVENING, DayPart.NIGHT)
        val part = parts[selTab]
        val list = ui.entries[part] ?: emptyList()
        val checks = ui.checks[part] ?: emptySet()

        LazyColumn(Modifier.fillMaxSize()) {
            items(list, key = { it.item.id }) { e ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(e.item.name, textAlign = TextAlign.Start)
                    Checkbox(
                        checked = checks.contains(e.item.id),
                        onCheckedChange = { vm.toggle(part, e.item.id, it) }
                    )
                }
                Divider()
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
