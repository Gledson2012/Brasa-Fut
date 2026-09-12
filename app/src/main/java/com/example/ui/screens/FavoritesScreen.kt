package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.local.FavoriteMatch
import com.example.data.local.FavoriteTeam
import com.example.ui.viewmodels.FavoritesViewModel
import com.example.utils.DateUtils

@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = viewModel(factory = FavoritesViewModel.Factory),
    onMatchSelected: (Int) -> Unit = {}
) {
    val favoriteTeams by viewModel.favoriteTeams.collectAsState()
    val favoriteMatches by viewModel.favoriteMatches.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Times Salvos", "Partidas Salvas")

    val haptic = LocalHapticFeedback.current
    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTabIndex = index 
                    },
                    text = { Text(title, fontWeight = FontWeight.Bold) }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> {
                if (favoriteTeams.isEmpty()) {
                    EmptyStateMessage("Você ainda não salvou nenhum time.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(favoriteTeams, key = { it.id }) { team ->
                            Box(modifier = Modifier.animateItem()) {
                                FavoriteTeamCard(
                                    team = team,
                                    onRemove = { viewModel.removeTeam(team.id) }
                                )
                            }
                        }
                    }
                }
            }
            1 -> {
                if (favoriteMatches.isEmpty()) {
                    EmptyStateMessage("Você ainda não salvou nenhuma partida.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(favoriteMatches, key = { it.id }) { match ->
                            Box(modifier = Modifier.animateItem()) {
                                FavoriteMatchCard(
                                    match = match,
                                    onClick = { onMatchSelected(match.id) },
                                    onRemove = { viewModel.removeMatch(match.id) }
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
fun EmptyStateMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FavoriteTeamCard(team: FavoriteTeam, onRemove: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = team.logoUrl,
                contentDescription = team.name,
                modifier = Modifier.size(48.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = team.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = team.shortName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onRemove() }) {
                Icon(Icons.Default.Delete, contentDescription = "Remover", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun FavoriteMatchCard(match: FavoriteMatch, onClick: () -> Unit, onRemove: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = DateUtils.formatDate(match.kickoffTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = match.homeLogoUrl,
                        contentDescription = match.homeTeamName,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(match.homeTeamName, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = match.awayLogoUrl,
                        contentDescription = match.awayTeamName,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(match.awayTeamName, style = MaterialTheme.typography.bodyMedium)
                }
            }
            IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onRemove() }) {
                Icon(Icons.Default.Delete, contentDescription = "Remover", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
