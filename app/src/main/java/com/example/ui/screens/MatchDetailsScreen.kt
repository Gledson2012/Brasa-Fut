package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.api.MatchDto
import com.example.ui.viewmodels.MatchDetailsUiState
import com.example.ui.viewmodels.MatchDetailsViewModel
import com.example.ui.viewmodels.MatchStatistic
import com.example.ui.viewmodels.PlayerMock
import com.example.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailsScreen(
    matchId: Int,
    onNavigateBack: () -> Unit,
    viewModel: MatchDetailsViewModel = viewModel(factory = MatchDetailsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(matchId) {
        viewModel.loadMatchDetails(matchId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes da Partida") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    val context = LocalContext.current

                    if (uiState is MatchDetailsUiState.Success) {
                        val match = (uiState as MatchDetailsUiState.Success).match
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val scoreText = if (match.homeScore != null && match.awayScore != null) "${match.homeScore} - ${match.awayScore}" else "vs"
                            val statusText = when (match.status) {
                                "FINISHED" -> "Finalizado"
                                "IN_PROGRESS", "LIVE" -> "Ao vivo"
                                "SCHEDULED" -> "Agendado"
                                else -> match.status ?: "Desconhecido"
                            }
                            val shareText = "Confira a partida no BrasaFut:\n${match.homeTeam.name} $scoreText ${match.awayTeam.name}\nStatus: $statusText"
                            
                            val sendIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            val shareIntent = android.content.Intent.createChooser(sendIntent, "Compartilhar partida")
                            context.startActivity(shareIntent)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartilhar"
                            )
                        }
                        
                        if (match.status == "NOT_STARTED" || match.status == "SCHEDULED" || match.status == "TIMED") {
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.scheduleReminder(context.applicationContext)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Me avise"
                                )
                            }
                        }
                    }

                    IconButton(onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleFavorite(context.applicationContext) 
                    }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                },
                label = "MatchDetailsStateAnimation"
            ) { state ->
                when (state) {
                    is MatchDetailsUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is MatchDetailsUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    is MatchDetailsUiState.Success -> {
                        MatchDetailsContent(
                            match = state.match,
                            statistics = state.statistics,
                            homeLineup = state.homeLineup,
                            awayLineup = state.awayLineup,
                            headToHead = state.headToHead,
                            events = state.events
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MatchDetailsContent(
    match: MatchDto,
    statistics: List<MatchStatistic>,
    homeLineup: List<PlayerMock>,
    awayLineup: List<PlayerMock>,
    headToHead: List<MatchDto>,
    events: List<com.example.data.api.MatchEventDto>
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Estatísticas", "Melhores Momentos", "Escalações", "Histórico")
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Scoreboard Header
        MatchScoreboardHeader(match)

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
            0 -> StatisticsTab(statistics, match.homeTeam.name, match.awayTeam.name)
            1 -> LineupsTab(homeLineup, awayLineup, match.homeTeam.name, match.awayTeam.name)
            2 -> HeadToHeadTab(headToHead)
        }
    }
}

@Composable
fun MatchScoreboardHeader(match: MatchDto) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = DateUtils.formatDate(match.kickoffTime),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = match.status ?: "Desconhecido",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Team
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    AsyncImage(
                        model = match.homeTeam.logoUrl,
                        contentDescription = match.homeTeam.name,
                        modifier = Modifier.size(64.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = match.homeTeam.shortName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                // Score
                Text(
                    text = "${match.homeScore ?: 0} - ${match.awayScore ?: 0}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Away Team
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    AsyncImage(
                        model = match.awayTeam.logoUrl,
                        contentDescription = match.awayTeam.name,
                        modifier = Modifier.size(64.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = match.awayTeam.shortName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun StatisticsTab(statistics: List<MatchStatistic>, homeName: String, awayName: String) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(homeName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                Text(awayName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(statistics) { stat ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stat.label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val homeText = if (stat.isPercentage) "${stat.homeValue}%" else stat.homeValue.toString()
                    val awayText = if (stat.isPercentage) "${stat.awayValue}%" else stat.awayValue.toString()

                    Text(homeText, style = MaterialTheme.typography.bodyMedium)

                    // Progress bar representation
                    val total = (stat.homeValue + stat.awayValue).coerceAtLeast(1)
                    val homeWeight = stat.homeValue.toFloat() / total
                    val awayWeight = stat.awayValue.toFloat() / total

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                            .height(8.dp)
                            .clip(MaterialTheme.shapes.small)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(homeWeight.coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Box(
                            modifier = Modifier
                                .weight(awayWeight.coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                        )
                    }

                    Text(awayText, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        
        item {
             Text(
                text = "Os dados estatísticos e de escalações não estão disponíveis na API e foram preenchidos com valores simulados apenas para exibição.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp).fillMaxWidth(),
                textAlign = TextAlign.Center
             )
        }
    }
}

@Composable
fun LineupsTab(homeLineup: List<PlayerMock>, awayLineup: List<PlayerMock>, homeName: String, awayName: String) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Home Lineup
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = homeName,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LazyColumn {
                items(homeLineup) { player ->
                    PlayerRow(player)
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Away Lineup
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = awayName,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp),
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.secondary
            )
            LazyColumn {
                items(awayLineup) { player ->
                    PlayerRow(player, isAway = true)
                }
            }
        }
    }
}

@Composable
fun PlayerRow(player: PlayerMock, isAway: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = if (isAway) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isAway) {
            Text(
                text = "${player.number}",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(24.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Column(horizontalAlignment = if (isAway) Alignment.End else Alignment.Start) {
            Text(text = player.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = player.position,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isAway) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${player.number}",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(24.dp),
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun HeadToHeadTab(matches: List<MatchDto>) {
    if (matches.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Nenhum histórico de confrontos encontrado ou erro ao carregar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(matches) { match ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = com.example.utils.DateUtils.formatDate(match.kickoffTime),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = match.homeTeam.shortName,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                            val scoreText = if (match.homeScore != null && match.awayScore != null) {
                                "${match.homeScore} - ${match.awayScore}"
                            } else "VS"
                            Text(
                                text = scoreText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Text(
                                text = match.awayTeam.shortName,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HighlightsTab(events: List<com.example.data.api.MatchEventDto>, match: MatchDto) {
    if (events.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhum evento registrado nesta partida.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(events) { event ->
            HighlightEventCard(event, match)
        }
    }
}

@Composable
fun HighlightEventCard(event: com.example.data.api.MatchEventDto, match: MatchDto) {
    val isHomeEvent = event.teamId == match.homeTeam.id
    val icon = when (event.type) {
        "GOAL", "PENALTY_SCORED" -> androidx.compose.material.icons.Icons.Default.SportsSoccer
        "YELLOW_CARD", "RED_CARD" -> androidx.compose.material.icons.Icons.Default.Stop
        "SUBSTITUTION" -> androidx.compose.material.icons.Icons.Default.SwapHoriz
        else -> androidx.compose.material.icons.Icons.Default.Info
    }
    val iconColor = when (event.type) {
        "GOAL", "PENALTY_SCORED" -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
        "YELLOW_CARD" -> androidx.compose.ui.graphics.Color(0xFFFFC107)
        "RED_CARD" -> androidx.compose.ui.graphics.Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isHomeEvent) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isHomeEvent) {
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
                Text(text = "${event.minute}'", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(text = event.player?.name ?: "Desconhecido", fontWeight = FontWeight.SemiBold)
                if (event.description != null) {
                    Text(text = event.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(imageVector = icon, contentDescription = event.type, tint = iconColor, modifier = Modifier.size(24.dp))
        } else {
            Icon(imageVector = icon, contentDescription = event.type, tint = iconColor, modifier = Modifier.size(24.dp))
            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.padding(start = 8.dp)) {
                Text(text = "${event.minute}'", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(text = event.player?.name ?: "Desconhecido", fontWeight = FontWeight.SemiBold)
                if (event.description != null) {
                    Text(text = event.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
