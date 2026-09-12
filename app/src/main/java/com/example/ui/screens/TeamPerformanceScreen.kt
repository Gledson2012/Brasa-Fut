package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodels.TeamPerformanceUiState
import com.example.ui.viewmodels.TeamPerformanceViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamPerformanceScreen(
    teamId: Int,
    onNavigateBack: () -> Unit,
    viewModel: TeamPerformanceViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(teamId) {
        viewModel.loadPerformance(teamId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Desempenho") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is TeamPerformanceUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is TeamPerformanceUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is TeamPerformanceUiState.Success -> {
                    if (state.performance.isEmpty()) {
                        Text(
                            text = "Nenhum dado de desempenho encontrado.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Progressão de Gols - ${state.team.name}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            // Vico Chart
                            val scoredEntries = state.performance.mapIndexed { i, data -> 
                                FloatEntry(x = i.toFloat(), y = data.goalsScoredCumulative.toFloat()) 
                            }
                            val concededEntries = state.performance.mapIndexed { i, data -> 
                                FloatEntry(x = i.toFloat(), y = data.goalsConcededCumulative.toFloat()) 
                            }

                            val chartEntryModel = entryModelOf(scoredEntries, concededEntries)

                            Chart(
                                chart = lineChart(),
                                model = chartEntryModel,
                                startAxis = rememberStartAxis(title = "Gols"),
                                bottomAxis = rememberBottomAxis(title = "Partidas (Rodadas)"),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Legenda
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Scored (Line 1 is typically the first color, let's just make generic legend)
                                Box(modifier = Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Marcados", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(modifier = Modifier.width(24.dp))
                                
                                // Conceded
                                Box(modifier = Modifier.size(12.dp).background(MaterialTheme.colorScheme.secondary))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sofridos", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
