package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BoardShape
import com.example.model.PlayerType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    viewModel: OAnQuanViewModel,
    onStartGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedShape by viewModel.selectedBoardShape.collectAsState()
    val allowYoungCapture by viewModel.allowYoungMandarinCapture.collectAsState()
    val playerSettings by viewModel.playerSettingsList.collectAsState()
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
                    Text(
                        text = "Ô",
                        color = colors.onPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                }
                Column {
                    Text(
                        text = "Ô Ăn Quan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = colors.onBackground,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Traditional Logic Game",
                        fontSize = 11.sp,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 1. Board Geometry Choice (Bento Card Style)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = colors.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(28.dp)
                ),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Select Board Geometry",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = colors.onSurface,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BoardShape.entries.forEachIndexed { index, shape ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = BoardShape.entries.size),
                            onClick = { viewModel.updateBoardShape(shape) },
                            selected = selectedShape == shape,
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = colors.secondaryContainer,
                                activeContentColor = colors.onSecondary,
                                inactiveContainerColor = Color.Transparent,
                                inactiveContentColor = colors.onSurfaceVariant
                            )
                        ) {
                            Text(shape.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Player Configurations list (Bento Card Style)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(
                    width = 1.dp,
                    color = colors.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(28.dp)
                ),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Configure Player Participants",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = colors.primary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    playerSettings.forEachIndexed { index, type ->
                        val isSectionUnassigned = selectedShape == BoardShape.HEXAGON && index == 5
                        if (isSectionUnassigned) return@forEachIndexed

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.background.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                                .border(1.dp, colors.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            when (index) {
                                                0 -> Color(0xFFE57373)
                                                1 -> Color(0xFF4FC3F7)
                                                2 -> Color(0xFF81C784)
                                                3 -> Color(0xFFD4E157)
                                                else -> Color(0xFFBA68C8)
                                            }
                                        )
                                )
                                Text(
                                    text = "  Player ${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = colors.onSurface
                                )
                            }

                            // Controller Type Selection Select Menu
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                Text(
                                    text = type.displayName,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = colors.primary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.secondaryContainer)
                                        .clickable { expanded = true }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                                ExposedDropdownMenuBox(
                                    expanded = expanded,
                                    onExpandedChange = { expanded = it }
                                ) {
                                    ExposedDropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false },
                                        modifier = Modifier.background(colors.surface)
                                    ) {
                                        PlayerType.entries.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option.displayName, fontWeight = FontWeight.Bold) },
                                                onClick = {
                                                    viewModel.updatePlayerSetting(index, option)
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (selectedShape == BoardShape.HEXAGON) {
                        Text(
                            text = "Note: Hexagon has a 6-sided board. 5 active player spots, side 6 remains neutral empty wilderness.",
                            style = MaterialTheme.typography.bodySmall.copy(color = colors.onSurfaceVariant.copy(alpha = 0.7f)),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // 3. Game Regional Rule Switch (Bento Card Style)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = colors.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Allow Capturing Young Mandarins",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = colors.onSurface
                    )
                    Text(
                        text = "Enable to capture Ô Quan even if empty of Dan",
                        fontSize = 11.sp,
                        color = colors.onSurfaceVariant
                    )
                }
                Switch(
                    checked = allowYoungCapture,
                    onCheckedChange = { viewModel.toggleAllowYoungMandarinCapture(it) }
                )
            }
        }

        // Action Button
        Button(
            onClick = {
                viewModel.startNewGame()
                onStartGame()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "START MATCH",
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                fontSize = 15.sp
            )
        }
    }
}
