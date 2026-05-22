package com.example.ui.screens

import android.text.format.DateFormat
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Date

@Composable
fun StatsScreen(
    viewModel: OAnQuanViewModel,
    modifier: Modifier = Modifier
) {
    val history by viewModel.matchHistoryList.collectAsState()
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Bento Style Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(colors.primary, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Analytics",
                        tint = colors.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "Match Analytics",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = colors.onBackground,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Historical Game Performance",
                        fontSize = 11.sp,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .background(colors.surface, RoundedCornerShape(28.dp))
                        .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Empty",
                        tint = colors.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "No matches played yet.\nYour historical scoreboards will appear here!",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurfaceVariant
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Summary Dashboard Bento Blocks (Beautiful Side-by-Side Cards)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Matches Block (Left Card)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = 1.dp,
                            color = colors.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Matches",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${history.size}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = colors.primary
                        )
                        Text(
                            text = "completed on device",
                            fontSize = 10.sp,
                            color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                val winStats = history.groupingBy { it.winnerName }.eachCount()
                val podiumPlayer = winStats.maxByOrNull { it.value }

                // Top Achiever Block (Right Card)
                Card(
                    modifier = Modifier
                        .weight(1.2f)
                        .border(
                            width = 1.dp,
                            color = colors.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Top Achiever",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = podiumPlayer?.key ?: "N/A",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.primary,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (podiumPlayer != null) "${podiumPlayer.value} wins" else "No matches won yet",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Scrolling Match logs
            Text(
                text = "Match History Log",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = colors.onSurface,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                textAlign = TextAlign.Start
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history) { record ->
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
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "BOARD: ${record.boardShape}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.primary,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = DateFormat.format("yyyy-MM-dd HH:mm", Date(record.timestamp)).toString(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Scores line-ups inside the individual history bento
                            val names = record.playerNames.split(",")
                            val scores = record.playerScores.split(",")

                            names.zip(scores).forEach { (player, score) ->
                                val isWinner = player == record.winnerName
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isWinner) colors.primary.copy(alpha = 0.08f) else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = player,
                                        fontSize = 12.sp,
                                        fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isWinner) colors.onSurface else colors.onSurfaceVariant
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = score,
                                            fontSize = 13.sp,
                                            fontWeight = if (isWinner) FontWeight.Black else FontWeight.Bold,
                                            color = if (isWinner) colors.primary else colors.onSurfaceVariant
                                        )
                                        if (isWinner) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Victor",
                                                tint = Color(0xFFFFD54F),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Wipe history utility command button
            OutlinedButton(
                onClick = { viewModel.clearStatsDb() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                shape = RoundedCornerShape(16.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy()
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Wipe", tint = colors.primary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("CLEAR STATISTICS HISTORY", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 0.5.sp)
            }
        }
    }
}
