package com.example.engine

import com.example.model.BoardShape
import com.example.model.Cell
import com.example.model.Player
import com.example.model.PlayerType

object OAnQuanEngine {
    const val PAWN_COUNT_PER_SIDE = 5
    const val STARTING_STONES_PER_PAWN = 5
    const val MANDARIN_VALUE = 10

    /**
     * Creates and initializes a circular board based on the board shape.
     */
    fun createInitialBoard(shape: BoardShape): List<Cell> {
        val list = mutableListOf<Cell>()
        val sides = shape.sides
        val totalCells = sides * (PAWN_COUNT_PER_SIDE + 1)
        for (i in 0 until totalCells) {
            val section = i / (PAWN_COUNT_PER_SIDE + 1)
            val isM = (i % (PAWN_COUNT_PER_SIDE + 1)) == PAWN_COUNT_PER_SIDE
            list.add(
                Cell(
                    index = i,
                    section = section,
                    qtyDan = if (isM) 0 else STARTING_STONES_PER_PAWN,
                    qtyQuan = if (isM) 1 else 0,
                    isMandarin = isM
                )
            )
        }
        return list
    }

    /**
     * Checks if a cell index belongs to a specific player's pawn section.
     */
    fun isCellOwner(playerIndex: Int, cellIndex: Int, shape: BoardShape): Boolean {
        val sectionSize = PAWN_COUNT_PER_SIDE + 1
        val playerSection = playerIndex // Player s owns section s
        val cellSection = cellIndex / sectionSize
        val localIndex = cellIndex % sectionSize
        return cellSection == playerSection && localIndex < PAWN_COUNT_PER_SIDE
    }

    /**
     * Simulate a single turn fully and return the captured points and final board.
     * Useful for AI lookahead and instant-results logic.
     */
    fun simulateMoveFully(
        board: List<Cell>,
        activePlayer: Int,
        srcIndex: Int,
        direction: Int, // 1 (CW), -1 (CCW)
        allowYoungMandarinCapture: Boolean
    ): SimulationResult {
        val activeBoard = board.map { it.copy() }.toMutableList()
        val totalCells = activeBoard.size
        
        var stonesInHand = activeBoard[srcIndex].qtyDan
        if (stonesInHand <= 0) return SimulationResult(board, 0, "No stones to sow.")

        // Empty source
        activeBoard[srcIndex] = activeBoard[srcIndex].copy(qtyDan = 0)
        
        var currentIdx = srcIndex
        var capturedCount = 0
        var debugLog = "Player fully sowed from cell $srcIndex. "

        while (true) {
            // Drop phase
            while (stonesInHand > 0) {
                currentIdx = (currentIdx + direction).mod(totalCells)
                val cell = activeBoard[currentIdx]
                activeBoard[currentIdx] = cell.copy(qtyDan = cell.qtyDan + 1)
                stonesInHand--
            }

            // Decide phase
            val nextIdx = (currentIdx + direction).mod(totalCells)
            val nextCell = activeBoard[nextIdx]

            if (nextCell.isMandarin) {
                // Sowing onto Mandarin ends turn
                debugLog += "Stopped at Mandarin cell $nextIdx."
                break
            } else if (nextCell.qtyDan > 0) {
                // Continue sowing from next cell
                stonesInHand = nextCell.qtyDan
                activeBoard[nextIdx] = nextCell.copy(qtyDan = 0)
                currentIdx = nextIdx
                debugLog += "Picked up $stonesInHand stones from cell $nextIdx and continued sowing. "
            } else {
                // Next cell is empty! Check for capture
                var currentEmptyIdx = nextIdx
                var hasCaptured = false
                while (true) {
                    val targetIdx = (currentEmptyIdx + direction).mod(totalCells)
                    val targetCell = activeBoard[targetIdx]
                    val canCaptureMandarin = !targetCell.isMandarin || 
                            (!allowYoungMandarinCapture || targetCell.qtyDan > 0 || targetCell.qtyQuan > 0)

                    if (activeBoard[currentEmptyIdx].qtyDan == 0 && (targetCell.qtyDan > 0 || targetCell.qtyQuan > 0)) {
                        // Capture!
                        val danPoints = targetCell.qtyDan
                        val quanPoints = targetCell.qtyQuan * MANDARIN_VALUE
                        val totalPoints = danPoints + quanPoints
                        
                        // If it's pure "young mandarin" and forbidden, bypass
                        if (targetCell.isMandarin && !allowYoungMandarinCapture && targetCell.qtyDan == 0) {
                            // Can't capture young mandarin
                            debugLog += "Encountered young Mandarin cell $targetIdx (cannot capture yet)."
                            break
                        }

                        capturedCount += totalPoints
                        activeBoard[targetIdx] = targetCell.copy(qtyDan = 0, qtyQuan = 0)
                        hasCaptured = true
                        debugLog += "Captured cell $targetIdx ($totalPoints pts). "

                        // Move over to check for chain capture
                        currentEmptyIdx = (targetIdx + direction).mod(totalCells)
                    } else {
                        break
                    }
                }
                break //Sowing and actions end
            }
        }

        return SimulationResult(activeBoard, capturedCount, debugLog)
    }

    data class SimulationResult(
        val finalBoard: List<Cell>,
        val capturedPoints: Int,
        val logDescription: String
    )

    /**
     * Evaluates and picks the best move for AI.
     */
    fun chooseBestMove(
        board: List<Cell>,
        aiPlayerIndex: Int,
        shape: BoardShape,
        difficulty: PlayerType,
        allowYoungMandarinCapture: Boolean
    ): AIMoveChoice {
        val validMoves = mutableListOf<AIMoveChoice>()
        val totalCells = board.size
        
        // Find all player's pawn cells containing stones
        for (i in 0 until totalCells) {
            if (isCellOwner(aiPlayerIndex, i, shape) && board[i].qtyDan > 0) {
                // Clockwise simulation
                val cwSim = simulateMoveFully(board, aiPlayerIndex, i, 1, allowYoungMandarinCapture)
                validMoves.add(AIMoveChoice(i, 1, cwSim.capturedPoints, cwSim.finalBoard))

                // Counter-Clockwise simulation
                val ccwSim = simulateMoveFully(board, aiPlayerIndex, i, -1, allowYoungMandarinCapture)
                validMoves.add(AIMoveChoice(i, -1, ccwSim.capturedPoints, ccwSim.finalBoard))
            }
        }

        if (validMoves.isEmpty()) {
            return AIMoveChoice(-1, 0, 0, board) // No moves possible (must feed)
        }

        return when (difficulty) {
            PlayerType.AI_EASY -> {
                // Easy AI chooses a random move
                validMoves.random()
            }
            PlayerType.AI_HARD -> {
                // Hard AI evaluates the net score (My points - Opponent's next best move points)
                var bestMove = validMoves.first()
                var bestScore = -999999

                for (move in validMoves) {
                    // Simulate opponent's best response from this final state
                    val nextPlayerIndex = (aiPlayerIndex + 1) % shape.maxPlayers
                    var maxOpponentPoints = 0
                    
                    for (j in 0 until totalCells) {
                        if (isCellOwner(nextPlayerIndex, j, shape) && move.resultingBoard[j].qtyDan > 0) {
                            val opCw = simulateMoveFully(move.resultingBoard, nextPlayerIndex, j, 1, allowYoungMandarinCapture)
                            val opCcw = simulateMoveFully(move.resultingBoard, nextPlayerIndex, j, -1, allowYoungMandarinCapture)
                            val bestOp = maxOf(opCw.capturedPoints, opCcw.capturedPoints)
                            if (bestOp > maxOpponentPoints) {
                                maxOpponentPoints = bestOp
                            }
                        }
                    }

                    // Score is AI Points captured minus what they let the opponent capture
                    // Plus a premium for capturing Mandarins
                    val initialMandarinCapturedCount = board.size // calculate mandarin difference
                    val currentMandarins = move.resultingBoard.count { it.isMandarin && it.qtyQuan > 0 }
                    val startingMandarins = board.count { it.isMandarin && it.qtyQuan > 0 }
                    val mandarinsCapturedWeight = (startingMandarins - currentMandarins) * 15 // High priority to capture Mandarins safely
                    
                    val moveScore = move.capturedPoints * 3 - maxOpponentPoints + mandarinsCapturedWeight
                    if (moveScore > bestScore) {
                        bestScore = moveScore
                        bestMove = move
                    }
                }
                bestMove
            }
            else -> validMoves.first()
        }
    }

    data class AIMoveChoice(
        val srcIndex: Int,
        val direction: Int,
        val capturedPoints: Int,
        val resultingBoard: List<Cell>
    )

    /**
     * Checks if all Mandarin cells are empty.
     */
    fun isGameOver(board: List<Cell>): Boolean {
        return board.none { it.isMandarin && (it.qtyQuan > 0 || it.qtyDan > 0) }
    }

    /**
     * Distributes remaining stones from player sections to their captures.
     * Returns a map of playerId -> points added.
     */
    fun distributeRemainingStones(board: List<Cell>, shape: BoardShape): Map<Int, Int> {
        val distribution = mutableMapOf<Int, Int>()
        val totalCells = board.size
        for (i in 0 until totalCells) {
            val cell = board[i]
            if (!cell.isMandarin && cell.qtyDan > 0) {
                val owner = cell.section // Section matches owner
                // We should only give to active players
                if (owner < shape.maxPlayers) {
                    distribution[owner] = (distribution[owner] ?: 0) + cell.qtyDan
                } else {
                    // If it is neutral, nobody gets it, or we distribute evenly (let's discard for neutral)
                }
            }
        }
        return distribution
    }
}
