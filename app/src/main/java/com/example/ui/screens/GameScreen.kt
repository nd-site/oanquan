package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BoardShape
import com.example.model.GamePlayState
import com.example.model.Player
import com.example.model.PlayerType
import com.example.ui.components.BoardPainter

@Composable
fun GameScreen(
    viewModel: OAnQuanViewModel,
    onBackToSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val board by viewModel.boardState.collectAsState()
    val shape by viewModel.selectedBoardShape.collectAsState()
    val players by viewModel.players.collectAsState()
    val activePlayerIdx by viewModel.activePlayerIndex.collectAsState()
    val gameplayState by viewModel.gameState.collectAsState()
    val sowProgress by viewModel.sowProgress.collectAsState()
    val selectedCellIndex by viewModel.selectedCellIndex.collectAsState()
    val movesLog by viewModel.movesLog.collectAsState()
    
    val activePlayer = players.getOrNull(activePlayerIdx)
    val colors = MaterialTheme.colorScheme

    var showRulesDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // TOP Header Card: Active Turn & Navigation
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = colors.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (gameplayState == GamePlayState.GAME_OVER) 
                    colors.secondaryContainer else colors.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    if (gameplayState == GamePlayState.GAME_OVER) {
                        val winner = players.firstOrNull { it.isWinner }
                        Text(
                            text = "GAME COMPLETED!",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.primary
                            )
                        )
                        Text(
                            text = if (winner != null) "${winner.name} won!" else "Draw Game!",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    } else {
                        Text(
                            text = "ACTIVE TURN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.onSurfaceVariant.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = activePlayer?.name ?: "Game Setup",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = activePlayer?.colorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: colors.onSurface
                            )
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { showRulesDialog = true }) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Rules", tint = colors.primary)
                    }
                    IconButton(onClick = onBackToSetup) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", tint = colors.primary)
                    }
                }
            }
        }

        // 1. Interactive Custom drawn Canvas Board (Bento Board Frame)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.3f)
                .border(
                    width = 3.dp,
                    color = colors.primary,
                    shape = RoundedCornerShape(28.dp)
                )
                .clip(RoundedCornerShape(28.dp))
        ) {
            BoardPainter(
                board = board,
                shape = shape,
                activePlayerIndex = activePlayerIdx,
                sowProgress = sowProgress,
                selectedCellIndex = selectedCellIndex,
                onCellClicked = { index ->
                    viewModel.selectCell(index)
                }
            )
        }

        // 2. DIRECTION CHOOSER (Tactile Bento Button Layout)
        AnimatedVisibility(
            visible = selectedCellIndex != null && gameplayState == GamePlayState.PLAYING,
            modifier = Modifier.fillMaxWidth()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = colors.primary.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Choose Sowing Direction for Pit #${selectedCellIndex}:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.playSelectedMove(1) }, // CW
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Clockwise")
                            Spacer(Modifier.width(6.dp))
                            Text("CW (Clockwise)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.playSelectedMove(-1) }, // CCW
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryContainer, contentColor = colors.onSurface)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Counter-Clockwise")
                            Spacer(Modifier.width(6.dp))
                            Text("CCW (Counter-CW)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. SEED ANIMATION STEPS BARS
        AnimatedVisibility(
            visible = gameplayState == GamePlayState.SOWING_ANIMATION && sowProgress != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = colors.primary.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = colors.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = sowProgress?.stateText ?: "Sowing pebbles...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                    )
                }
            }
        }

        // 4. SCOREBOARD CARD (Perfect Rounded Grid Bento Boxes)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = colors.outline.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                players.forEachIndexed { index, player ->
                    val isSectionUnassigned = shape == BoardShape.HEXAGON && index == 5
                    if (isSectionUnassigned) return@forEachIndexed

                    val isActive = index == activePlayerIdx && gameplayState == GamePlayState.PLAYING
                    val pColor = Color(android.graphics.Color.parseColor(player.colorHex))

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (isActive) 2.dp else 1.dp,
                                color = if (isActive) pColor else colors.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) pColor.copy(alpha = 0.12f) else colors.background.copy(alpha = 0.8f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = player.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = pColor,
                                maxLines = 1
                            )
                            Text(
                                text = "${player.score}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 5. MATCH CONSOLE LOG PANEL
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.7f)
                .border(
                    width = 1.dp,
                    color = colors.outline.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Match Analytics Log",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.primary,
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                if (movesLog.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No movements recorded in this match yet.",
                            style = MaterialTheme.typography.bodySmall.copy(color = colors.onSurfaceVariant.copy(alpha = 0.6f))
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(movesLog.asReversed()) { move ->
                            val player = players.getOrNull(move.playerIndex)
                            val pColor = player?.colorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: colors.onSurface
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.background.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                    .border(1.dp, colors.outline.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = player?.name ?: "Player",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pColor,
                                    modifier = Modifier.weight(0.3f)
                                )
                                Text(
                                    text = move.actionText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.onSurfaceVariant,
                                    modifier = Modifier.weight(0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Rules Instruction Dialog Modal
    if (showRulesDialog) {
        AlertDialog(
            onDismissRequest = { showRulesDialog = false },
            title = { Text("How to Play Ô Ăn Quan") },
            text = {
                Column(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "1. SOWING",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = colors.primary
                    )
                    Text(
                        text = "On your turn, click any pawn cell of your matching color containing stones. Choose direction (Clockwise or Counter-CW). Stones are sown 1-by-1 circular around the board.",
                        fontSize = 12.sp
                    )

                    Text(
                        text = "2. CONTINUING & STOPPING",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = colors.primary
                    )
                    Text(
                        text = "If sowing ends before an occupied cell, pick up all its stones and continue. Sowing finishes immediately if it lands adjacent to a Mandarin cell, or before an empty cell.",
                        fontSize = 12.sp
                    )

                    Text(
                        text = "3. CAPTURING",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = colors.primary
                    )
                    Text(
                        text = "If sowing ends before an empty cell, you capture all stones inside the cell AFTER that empty cell. If the cell following the capture is also empty, another capture takes place (Chain Capture!).",
                        fontSize = 12.sp
                    )

                    Text(
                        text = "4. FEEDING THE ROW",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = colors.primary
                    )
                    Text(
                        text = "If your turn starts but all your pawn cells are empty, you must 'feed' 5 stones (1 per cell) from your captured stash (creating a temporary point debt if required).",
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showRulesDialog = false }) {
                    Text("Got It!")
                }
            }
        )
    }
}
