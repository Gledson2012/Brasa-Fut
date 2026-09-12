package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.viewmodels.GlobalSearchUiState
import com.example.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    uiState: GlobalSearchUiState,
    onMatchSelected: (Int) -> Unit,
    onTeamSelected: (Int) -> Unit,
    onSettingsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (active) 0.dp else 16.dp, vertical = if (active) 0.dp else 8.dp)
            .wrapContentHeight()
    ) {
        SearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = { onActiveChange(false) },
            active = active,
            onActiveChange = onActiveChange,
            placeholder = { Text("Buscar partidas, times, jogadores...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            trailingIcon = {
                if (active) {
                    IconButton(onClick = { 
                        if (query.isNotEmpty()) onQueryChange("") 
                        else onActiveChange(false) 
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                } else {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Configurações")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            when (uiState) {
                is GlobalSearchUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is GlobalSearchUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = uiState.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is GlobalSearchUiState.Success -> {
                    if (uiState.teams.isEmpty() && uiState.matches.isEmpty() && uiState.players.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Nenhum resultado encontrado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            if (uiState.teams.isNotEmpty()) {
                                item {
                                    Text("Times", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)
                                }
                                items(uiState.teams) { team ->
                                    ListItem(
                                        headlineContent = { Text(team.name) },
                                        supportingContent = { Text("Time") },
                                        leadingContent = {
                                            AsyncImage(
                                                model = team.logoUrl,
                                                contentDescription = null,
                                                modifier = Modifier.size(40.dp)
                                            )
                                        },
                                        modifier = Modifier.clickable { 
                                            onActiveChange(false)
                                            onTeamSelected(team.id) 
                                        }
                                    )
                                }
                            }

                            if (uiState.matches.isNotEmpty()) {
                                item {
                                    Text("Partidas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)
                                }
                                items(uiState.matches) { match ->
                                    ListItem(
                                        headlineContent = { Text("${match.homeTeam.shortName} x ${match.awayTeam.shortName}") },
                                        supportingContent = { Text(DateUtils.formatDate(match.kickoffTime)) },
                                        leadingContent = { Icon(Icons.Default.SportsSoccer, contentDescription = null) },
                                        modifier = Modifier.clickable { 
                                            onActiveChange(false)
                                            onMatchSelected(match.id) 
                                        }
                                    )
                                }
                            }

                            if (uiState.players.isNotEmpty()) {
                                item {
                                    Text("Jogadores", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)
                                }
                                items(uiState.players) { player ->
                                    ListItem(
                                        headlineContent = { Text(player.name) },
                                        supportingContent = { Text("${player.position} - ${player.teamName}") },
                                        leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
                                        modifier = Modifier.clickable {
                                            onActiveChange(false)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                is GlobalSearchUiState.Idle -> {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopCenter) {
                        Text("Digite ao menos 2 caracteres para buscar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
