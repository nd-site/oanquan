package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.OAnQuanEngine
import com.example.model.BoardShape
import com.example.model.Cell
import com.example.model.SowProgress
import kotlin.math.cos
import kotlin.math.sin

data class CellGeometry(
    val index: Int,
    val center: Offset,
    val radius: Float,
    val isMandarin: Boolean,
    val ownerSection: Int
)

@Composable
fun BoardPainter(
    board: List<Cell>,
    shape: BoardShape,
    activePlayerIndex: Int,
    sowProgress: SowProgress?,
    selectedCellIndex: Int?,
    onCellClicked: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Compute the geometrical layouts of each cell
    val colors = MaterialTheme.colorScheme

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(board, shape) {
                detectTapGestures { offset ->
                    val geometries = computeGeometries(shape, size.width.toFloat(), size.height.toFloat())
                    // Find closest cell within acceptable threshold (target touch area)
                    val closest = geometries.minByOrNull { geom ->
                        val dist = (geom.center - offset).getDistance()
                        dist
                    }
                    if (closest != null) {
                        val distance = (closest.center - offset).getDistance()
                        if (distance <= closest.radius * 1.5f) {
                            onCellClicked(closest.index)
                        }
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val geometries = computeGeometries(shape, width, height)

        // Draw elegant Bento board background
        drawBentoBackground(colors)

        // Draw cell/pit backgrounds (beautiful white circular pockets)
        geometries.forEach { geom ->
            if (!geom.isMandarin) {
                // Standard pit backing
                drawCircle(
                    color = Color.White,
                    radius = geom.radius * 0.95f,
                    center = geom.center
                )
                drawCircle(
                    color = colors.outline.copy(alpha = 0.5f),
                    radius = geom.radius * 0.95f,
                    center = geom.center,
                    style = Stroke(width = 1.dp.toPx())
                )
            } else {
                // Mandarin semi-circles/pits filled with soft clean background for bento aesthetic
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = geom.radius * 0.95f,
                    center = geom.center
                )
                drawCircle(
                    color = colors.primary.copy(alpha = 0.3f),
                    radius = geom.radius * 0.95f,
                    center = geom.center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        // Draw grid lines and borders
        drawBoardBorders(geometries, shape, colors.primary)

        // Paint each Cell
        geometries.forEach { geom ->
            val cellData = board.getOrNull(geom.index) ?: return@forEach
            val isSelected = selectedCellIndex == geom.index
            val isActiveSowing = sowProgress?.currentCellIndex == geom.index
            val isOwnedByActivePlayer = OAnQuanEngine.isCellOwner(activePlayerIndex, geom.index, shape)

            // 1. Draw Cell Background Highlight
            if (isSelected) {
                drawCircle(
                    color = colors.primary.copy(alpha = 0.35f),
                    radius = geom.radius,
                    center = geom.center
                )
                drawCircle(
                    color = colors.primary,
                    radius = geom.radius,
                    center = geom.center,
                    style = Stroke(width = 4.dp.toPx())
                )
            } else if (isActiveSowing) {
                drawCircle(
                    color = colors.tertiary.copy(alpha = 0.45f),
                    radius = geom.radius,
                    center = geom.center
                )
                drawCircle(
                    color = colors.tertiary,
                    radius = geom.radius,
                    center = geom.center,
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )
            } else if (isOwnedByActivePlayer && !geom.isMandarin && cellData.qtyDan > 0) {
                // Highlight valid cells to guide players
                drawCircle(
                    color = colors.secondary.copy(alpha = 0.15f * pulseAlpha),
                    radius = geom.radius,
                    center = geom.center
                )
                drawCircle(
                    color = colors.secondary.copy(alpha = 0.4f * pulseAlpha),
                    radius = geom.radius,
                    center = geom.center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // 2. Draw Stones/Pebbles inside the cell
            drawCellStones(geom, cellData)

            // 3. Draw Stones Count Badge
            val countStr = if (geom.isMandarin) {
                if (cellData.qtyQuan > 0) "${cellData.qtyDan} + Q" else "${cellData.qtyDan}"
            } else {
                "${cellData.qtyDan}"
            }

            val countStyle = TextStyle(
                color = if (geom.isMandarin) Color(0xFFFFD54F) else colors.onSurface.copy(alpha = 0.9f),
                fontSize = if (geom.isMandarin) 13.sp else 12.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            val textLayout = textMeasurer.measure(countStr, countStyle)
            val textPos = Offset(
                geom.center.x - textLayout.size.width / 2f,
                geom.center.y - textLayout.size.height / 2f + (geom.radius * 0.6f) // draw slightly lower
            )
            
            // Background box for numbers so they remain highly legible over pebble piles
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.45f),
                topLeft = Offset(textPos.x - 4.dp.toPx(), textPos.y - 1.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(textLayout.size.width + 8.dp.toPx(), textLayout.size.height + 2.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )

            drawText(
                textMeasurer = textMeasurer,
                text = countStr,
                style = countStyle,
                topLeft = textPos
            )

            // Small owner label for guidance
            val labelStr = if (geom.isMandarin) "M${geom.ownerSection + 1}" else "P${geom.ownerSection + 1}"
            val labelStyle = TextStyle(
                color = colors.onSurface.copy(alpha = 0.4f),
                fontSize = 8.sp
            )
            val labelLayout = textMeasurer.measure(labelStr, labelStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = labelStr,
                style = labelStyle,
                topLeft = Offset(
                    geom.center.x - labelLayout.size.width / 2f,
                    geom.center.y - geom.radius * 0.95f
                )
            )
        }
    }
}

private fun DrawScope.drawBentoBackground(colors: androidx.compose.material3.ColorScheme) {
    // Elegant soft solid light cream or rich dark green bento surface matching theme
    drawRect(color = colors.surface, size = size)
    
    // Smooth outer green boundary
    drawRect(
        color = colors.primary.copy(alpha = 0.12f),
        style = Stroke(width = 4.dp.toPx())
    )
}

private fun DrawScope.drawBoardBorders(
    geometries: List<CellGeometry>,
    shape: BoardShape,
    borderCol: Color
) {
    val lineCol = borderCol.copy(alpha = 0.25f)
    
    if (shape == BoardShape.RECTANGLE) {
        // Draw the iconic long rectangle box with 2 semi-circles
        // Find outer boundary from coordinates
        val cells = geometries.filter { !it.isMandarin }
        val leftM = geometries.first { it.index == 11 }
        val rightM = geometries.first { it.index == 5 }

        val xs = cells.map { it.center.x }.sorted()
        val ys = cells.map { it.center.y }.sorted()

        val minX = xs.first() - cells.first().radius
        val maxX = xs.last() + cells.first().radius
        val minY = ys.first() - cells.first().radius
        val maxY = ys.last() + cells.first().radius

        // Draw Main Outer Rectangle Frame
        drawRect(
            color = borderCol.copy(alpha = 0.6f),
            topLeft = Offset(minX, minY),
            size = androidx.compose.ui.geometry.Size(maxX - minX, maxY - minY),
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw Central horizontal split divider
        val midY = (minY + maxY) / 2
        drawLine(
            color = borderCol.copy(alpha = 0.6f),
            start = Offset(minX, midY),
            end = Offset(maxX, midY),
            strokeWidth = 2.dp.toPx()
        )

        // Draw Left Semi-circle Mandarin square
        val leftPath = Path().apply {
            val radius = (maxY - minY) / 2
            val center = LeftArcCenter(leftM.center, radius)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    center.x - radius, center.y - radius,
                    center.x + radius, center.y + radius
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = true
            )
        }
        drawPath(
            path = leftPath,
            color = borderCol.copy(alpha = 0.6f),
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw Right Semi-circle Mandarin square
        val rightPath = Path().apply {
            val radius = (maxY - minY) / 2
            val center = RightArcCenter(rightM.center, radius)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    center.x - radius, center.y - radius,
                    center.x + radius, center.y + radius
                ),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 180f,
                forceMoveTo = true
            )
        }
        drawPath(
            path = rightPath,
            color = borderCol.copy(alpha = 0.6f),
            style = Stroke(width = 3.dp.toPx())
        )
    } else {
        // Draw polygons (Triangle, Square, Hexagon) cyclic rings
        // Connect the vertices using a beautiful geometric polygon ring
        val sides = shape.sides
        val mandList = geometries.filter { it.isMandarin }.sortedBy { it.index }

        for (i in mandList.indices) {
            val v1 = mandList[i]
            val v2 = mandList[(i + 1) % mandList.size]

            // Draw large vertex hubs (Mandarin pits)
            drawCircle(
                color = borderCol.copy(alpha = 0.15f),
                radius = v1.radius,
                center = v1.center
            )
            drawCircle(
                color = borderCol.copy(alpha = 0.5f),
                radius = v1.radius,
                center = v1.center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw corridors/bridges connecting Mandarins
            drawLine(
                color = lineCol,
                start = v1.center,
                end = v2.center,
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

// Helpers for arc drawing coordinates
private fun LeftArcCenter(cellCenter: Offset, radius: Float): Offset {
    return Offset(cellCenter.x + radius * 0.4f, cellCenter.y)
}
private fun RightArcCenter(cellCenter: Offset, radius: Float): Offset {
    return Offset(cellCenter.x - radius * 0.4f, cellCenter.y)
}

private fun DrawScope.drawCellStones(geom: CellGeometry, cell: Cell) {
    // Deterministic placement generator
    val geomSeed = geom.index.toLong()

    // 1. Draw large Mandarin stone if present
    if (geom.isMandarin && cell.qtyQuan > 0) {
        // Shimmering golden mandarin coin/orb
        val grad = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF176), Color(0xFFF57F17), Color(0xFF3E2723)),
            center = geom.center,
            radius = geom.radius * 0.45f
        )
        drawCircle(
            brush = grad,
            radius = geom.radius * 0.42f,
            center = geom.center
        )
        // Add a nice premium inner emboss ring
        drawCircle(
            color = Color.White.copy(alpha = 0.4f),
            radius = geom.radius * 0.35f,
            center = geom.center,
            style = Stroke(width = 2.dp.toPx())
        )
    }

    // 2. Arrange ordinary Dan stones around center
    val danCount = cell.qtyDan
    if (danCount > 0) {
        val maxSpreadRadius = geom.radius * 0.45f
        // Beautiful stones colors: Ivory, Granite, Sandstone, River green
        val colorPalettes = listOf(
            Color(0xFFE0E0E0), // Ivory
            Color(0xFF90A4AE), // Slate Granite
            Color(0xFFFFCC80), // Peach sandstone
            Color(0xFFB0BEC5), // Light blue pebble
            Color(0xFFA5D6A7)  // River Jade
        )

        for (k in 0 until danCount) {
            val random = java.util.Random(geomSeed * 179 + k * 233)
            val angle = random.nextFloat() * 2 * Math.PI
            // Heap clusters: more stones cluster closer to the center, imitating gravity
            val distanceSpread = random.nextFloat() * maxSpreadRadius
            val x = geom.center.x + cos(angle).toFloat() * distanceSpread
            val y = geom.center.y + sin(angle).toFloat() * distanceSpread

            val stoneColor = colorPalettes[random.nextInt(colorPalettes.size)]
            val stoneGrad = Brush.radialGradient(
                colors = listOf(Color.White, stoneColor, Color(0xFF263238)),
                center = Offset(x - 1.5.dp.toPx(), y - 1.5.dp.toPx()),
                radius = 5.dp.toPx()
            )

            drawCircle(
                brush = stoneGrad,
                radius = 5.5.dp.toPx(),
                center = Offset(x, y)
            )
            // Tiny drop shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.25f),
                radius = 6.dp.toPx(),
                center = Offset(x + 1.dp.toPx(), y + 1.2.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

/**
 * Solid mathematical function mapping cell indices to circular or grid coordinates.
 */
fun computeGeometries(shape: BoardShape, canvasWidth: Float, canvasHeight: Float): List<CellGeometry> {
    val list = mutableListOf<CellGeometry>()
    val sizeRef = minOf(canvasWidth, canvasHeight)

    if (shape == BoardShape.RECTANGLE) {
        // Traditional 2x5 rectangle mapping
        val cx = canvasWidth / 2f
        val cy = canvasHeight / 2f

        // Let grid take up comfortable space
        val gridW = canvasWidth * 0.65f
        val gridH = canvasHeight * 0.28f
        val colWidth = gridW / 5f
        val rowHeight = gridH / 2f
        val radius = minOf(colWidth, rowHeight) * 0.43f

        // Top Row (Player 1) - index 0, 1, 2, 3, 4 (ordered Left-to-Right)
        val yTop = cy - rowHeight / 2f
        for (col in 0 until 5) {
            val x = cx + (col - 2) * colWidth
            list.add(
                CellGeometry(
                    index = col,
                    center = Offset(x, yTop),
                    radius = radius,
                    isMandarin = false,
                    ownerSection = 0
                )
            )
        }

        // Right Mandarin - index 5
        val xRight = cx + 3 * colWidth
        list.add(
            CellGeometry(
                index = 5,
                center = Offset(xRight, cy),
                radius = radius * 1.35f,
                isMandarin = true,
                ownerSection = 0
            )
        )

        // Bottom Row (Player 2) - index 6, 7, 8, 9, 10 (ordered Right-to-Left, i.e., column 4,3,2,1,0)
        val yBottom = cy + rowHeight / 2f
        for (col in 4 downTo 0) {
            val index = 6 + (4 - col)
            val x = cx + (col - 2) * colWidth
            list.add(
                CellGeometry(
                    index = index,
                    center = Offset(x, yBottom),
                    radius = radius,
                    isMandarin = false,
                    ownerSection = 1
                )
            )
        }

        // Left Mandarin - index 11
        val xLeft = cx - 3 * colWidth
        list.add(
            CellGeometry(
                index = 11,
                center = Offset(xLeft, cy),
                radius = radius * 1.35f,
                isMandarin = true,
                ownerSection = 1
            )
        )

    } else {
        // Polygons (Triangle, Square, Hexagon) mapping
        val cx = canvasWidth / 2f
        val cy = canvasHeight / 2f

        // Radius of the circle containing the polygon vertices
        val R = sizeRef * 0.35f
        val sides = shape.sides
        val C = OAnQuanEngine.PAWN_COUNT_PER_SIDE

        // First, compute coordinates of the S vertices (these host Mandarins)
        val vertices = Array(sides) { i ->
            // Shift angle by -90 degrees so index 0 points up nicely
            val angle = i * (2 * Math.PI / sides) - Math.PI / 2
            Offset(
                (cx + R * cos(angle)).toFloat(),
                (cy + R * sin(angle)).toFloat()
            )
        }

        // Cell boundary size
        val pawnRadius = (sizeRef * 0.045f)

        // Construct index map side-by-side
        for (k in 0 until sides) {
            val vStart = vertices[k]
            val vEnd = vertices[(k + 1) % sides]

            // Pawn cells of this section lie along the line connecting vertices k and k+1
            for (i in 0 until C) {
                val index = k * (C + 1) + i
                // Interpolate spacing. Use values between 0.15 and 0.85 to avoid overlapping Mandarin hubs
                val t = 0.16f + (i.toFloat() / (C - 1)) * 0.68f
                val x = vStart.x * (1f - t) + vEnd.x * t
                val y = vStart.y * (1f - t) + vEnd.y * t

                // Avoid players overreaching - Player owners maps to Section index
                list.add(
                    CellGeometry(
                        index = index,
                        center = Offset(x, y),
                        radius = pawnRadius,
                        isMandarin = false,
                        ownerSection = k
                    )
                )
            }

            // Mandarin cell is exactly at the end vertex (Vertex k + 1)
            val mandIndex = k * (C + 1) + C
            list.add(
                CellGeometry(
                    index = mandIndex,
                    center = vEnd,
                    radius = pawnRadius * 1.45f,
                    isMandarin = true,
                    ownerSection = k
                )
            )
        }
    }

    return list
}
