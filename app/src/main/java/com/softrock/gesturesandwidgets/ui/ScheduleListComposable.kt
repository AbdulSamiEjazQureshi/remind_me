package com.softrock.gesturesandwidgets.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListComposable() {

    var context = LocalContext.current
    var scrollState = rememberLazyListState(
//        initialFirstVisibleItemIndex = 1,
        initialFirstVisibleItemScrollOffset = 5
    )


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Remind me")
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {

                Button(
                    onClick = {

                    }
                ) {
                    Text("Jump To Index 7")
                }

                Text("12-Apr-2025, Wednesday", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            items(10) {
                ScheduleListItemComposable()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListItemComposable() {

    var sliderState by remember { mutableStateOf(SliderState()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = "Build Icon"
            )
            VerticalDivider(
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text("On Wednesday at 12:07 PM")
                Slider(
                    state = sliderState,
                    thumb = {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Slider Thumb"
                        )
                    }
                )
                Text("View Notes", textDecoration = TextDecoration.Underline)
            }
        }
    }
}
