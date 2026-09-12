package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.api.StandingDto
import com.example.ui.viewmodels.StandingsUiState
import com.example.ui.viewmodels.StandingsViewModel

@Composable
fun StandingsScreen(
    modifier: Modifier = Modifier,
    viewModel: StandingsViewModel = viewModel(),
    onTeamSelected: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Classificação",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        when (val state = uiState) {
            is StandingsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is StandingsUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Erro ao carregar classificação: ${state.message}",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
            is StandingsUiState.Success -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#", fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Equipe", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("P", fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                            Text("J", fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                            Text("V", fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                            Text("E", fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                            Text("D", fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                            Text("SG", fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                        
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.standings) { standing ->
                                StandingRow(
                                    standing = standing,
                                    onClick = { onTeamSelected(standing.team.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StandingRow(standing: StandingDto, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = standing.position.toString(),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = standing.team.logoUrl,
                contentDescription = standing.team.name,
                modifier = Modifier.size(24.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = standing.team.shortName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
        }
        
        Text(standing.points.toString(), fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
        Text(standing.played.toString(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
        Text(standing.won.toString(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
        Text(standing.drawn.toString(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
        Text(standing.lost.toString(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
        Text(standing.goalDifference.toString(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
    }
}
