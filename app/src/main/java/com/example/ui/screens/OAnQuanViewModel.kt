package com.example.ui.screens

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MatchHistory
import com.example.data.MatchHistoryRepository
import com.example.engine.OAnQuanEngine
import com.example.model.BoardShape
import com.example.model.Cell
import com.example.model.GameMove
import com.example.model.GamePlayState
import com.example.model.Player
import com.example.model.PlayerType
import com.example.model.SowProgress
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OAnQuanViewModel(private val repository: MatchHistoryRepository) : ViewModel() {

    // Board Configuration States
    private val _selectedBoardShape = MutableStateFlow(BoardShape.RECTANGLE)
    val selectedBoardShape: StateFlow<BoardShape> = _selectedBoardShape.asStateFlow()

    private val _allowYoungMandarinCapture = MutableStateFlow(false)
    val allowYoungMandarinCapture: StateFlow<Boolean> = _allowYoungMandarinCapture.asStateFlow()

    private val _aiDifficulty = MutableStateFlow(PlayerType.AI_HARD)
    val aiDifficulty: StateFlow<PlayerType> = _aiDifficulty.asStateFlow()

    // Core Active Game States
    private val _gameState = MutableStateFlow(GamePlayState.CONFIG)
    val gameState: StateFlow<GamePlayState> = _gameState.asStateFlow()

    private val _boardState = MutableStateFlow<List<Cell>>(emptyList())
    val boardState: StateFlow<List<Cell>> = _boardState.asStateFlow()

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players.asStateFlow()

    private val _activePlayerIndex = MutableStateFlow(0)
    val activePlayerIndex: StateFlow<Int> = _activePlayerIndex.asStateFlow()

    // Animation & Realtime states
    private val _sowProgress = MutableStateFlow<SowProgress?>(null)
    val sowProgress: StateFlow<SowProgress?> = _sowProgress.asStateFlow()

    private val _movesLog = MutableStateFlow<List<GameMove>>(emptyList())
    val movesLog: StateFlow<List<GameMove>> = _movesLog.asStateFlow()

    // Selection indicators for Human turns
    private val _selectedCellIndex = MutableStateFlow<Int?>(null)
    val selectedCellIndex: StateFlow<Int?> = _selectedCellIndex.asStateFlow()

    private val _selectedDirection = MutableStateFlow<Int?>(null) // 1 (CW), -1 (CCW)
    val selectedDirection: StateFlow<Int?> = _selectedDirection.asStateFlow()

    // Events flow (sound trigger, custom visual vibration hints)
    private val _gameEvents = MutableSharedFlow<String>()
    val gameEvents: SharedFlow<String> = _gameEvents.asSharedFlow()

    // DB Stream for historical statistics and scoreboard
    val matchHistoryList: StateFlow<List<MatchHistory>> = repository.allMatches
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _playerSettingsList = MutableStateFlow<List<PlayerType>>(
        listOf(PlayerType.HUMAN, PlayerType.AI_HARD)
    )
    val playerSettingsList: StateFlow<List<PlayerType>> = _playerSettingsList.asStateFlow()

    fun updateBoardShape(shape: BoardShape) {
        _selectedBoardShape.value = shape
        // Adjust the settings list size to fit the board shape configuration
        val currentSettings = _playerSettingsList.value.toMutableList()
        val requiredPlayers = shape.maxPlayers
        if (currentSettings.size < requiredPlayers) {
            for (i in currentSettings.size until requiredPlayers) {
                // Alternately set as CPU or HUMAN
                currentSettings.add(if (i == 0) PlayerType.HUMAN else PlayerType.AI_HARD)
            }
        } else if (currentSettings.size > requiredPlayers) {
            _playerSettingsList.value = currentSettings.subList(0, requiredPlayers)
            return
        }
        _playerSettingsList.value = currentSettings
    }

    fun updatePlayerSetting(index: Int, type: PlayerType) {
        val current = _playerSettingsList.value.toMutableList()
        if (index in current.indices) {
            current[index] = type
            _playerSettingsList.value = current
        }
    }

    fun toggleAllowYoungMandarinCapture(allow: Boolean) {
        _allowYoungMandarinCapture.value = allow
    }

    fun updateAIDifficulty(difficulty: PlayerType) {
        _aiDifficulty.value = difficulty
    }

    fun resetToConfig() {
        _gameState.value = GamePlayState.CONFIG
    }

    /**
     * Set up and start a new game matching the active configuration.
     */
    fun startNewGame() {
        val shape = _selectedBoardShape.value
        val initialBoard = OAnQuanEngine.createInitialBoard(shape)
        _boardState.value = initialBoard

        val configTypes = _playerSettingsList.value
        val list = mutableListOf<Player>()
        
        val colors = listOf(
            "#E57373", // Light Red
            "#4FC3F7", // Light Blue
            "#81C784", // Light Green
            "#D4E157", // Yellow / Lime
            "#BA68C8"  // Purple
        )

        for (i in 0 until shape.maxPlayers) {
            val type = configTypes.getOrElse(i) { if (i == 0) PlayerType.HUMAN else PlayerType.AI_HARD }
            val name = if (type == PlayerType.HUMAN) "Player ${i + 1}" else "CPU ${i + 1} (${if (type == PlayerType.AI_EASY) "Easy" else "Hard"})"
            list.add(
                Player(
                    id = i,
                    name = name,
                    type = type,
                    colorHex = colors[i % colors.size],
                    score = 0
                )
            )
        }
        _players.value = list
        _activePlayerIndex.value = 0
        _movesLog.value = emptyList()
        _sowProgress.value = null
        _selectedCellIndex.value = null
        _selectedDirection.value = null
        _gameState.value = GamePlayState.PLAYING

        triggerEngineCheckAndAI()
    }

    /**
     * Human/UI triggers cell selection.
     */
    fun selectCell(cellIndex: Int) {
        if (_gameState.value != GamePlayState.PLAYING) return
        val activePlayer = _activePlayerIndex.value
        val activePlayerInfo = _players.value.getOrNull(activePlayer) ?: return
        
        if (activePlayerInfo.type != PlayerType.HUMAN) return // Ignore if CPU's turn

        val shape = _selectedBoardShape.value
        if (OAnQuanEngine.isCellOwner(activePlayer, cellIndex, shape) && _boardState.value[cellIndex].qtyDan > 0) {
            _selectedCellIndex.value = cellIndex
            _selectedDirection.value = null // Reset direction picker
        }
    }

    /**
     * Executes the move with step-by-stone animation.
     */
    fun playSelectedMove(direction: Int) {
        val srcCell = _selectedCellIndex.value ?: return
        _selectedCellIndex.value = null
        _selectedDirection.value = null
        
        performSowMove(srcCell, direction)
    }

    private fun performSowMove(srcIndex: Int, direction: Int) {
        val activePlayer = _activePlayerIndex.value
        val playerInfo = _players.value.getOrNull(activePlayer) ?: return
        val board = _boardState.value
        val totalCells = board.size

        if (board[srcIndex].qtyDan <= 0 || board[srcIndex].isMandarin) return

        _gameState.value = GamePlayState.SOWING_ANIMATION

        viewModelScope.launch {
            var localBoard = board.map { it.copy() }.toMutableList()
            var stonesInHand = localBoard[srcIndex].qtyDan
            
            // Pick up stones
            localBoard[srcIndex] = localBoard[srcIndex].copy(qtyDan = 0)
            _boardState.value = localBoard.toList()
            
            var currentCellIndex = srcIndex
            _sowProgress.value = SowProgress(
                stonesInHand = stonesInHand,
                currentCellIndex = currentCellIndex,
                direction = direction,
                startCellIndex = srcIndex,
                stateText = "${playerInfo.name} picked up $stonesInHand stones from cell $srcIndex."
            )
            
            _gameEvents.emit("pickup")
            delay(450)

            while (true) {
                // Drop phase
                while (stonesInHand > 0) {
                    currentCellIndex = (currentCellIndex + direction).mod(totalCells)
                    val cell = localBoard[currentCellIndex]
                    localBoard[currentCellIndex] = cell.copy(qtyDan = cell.qtyDan + 1)
                    stonesInHand--

                    _boardState.value = localBoard.toList()
                    _sowProgress.value = SowProgress(
                        stonesInHand = stonesInHand,
                        currentCellIndex = currentCellIndex,
                        direction = direction,
                        startCellIndex = srcIndex,
                        stateText = "Sowing stones... (${stonesInHand} remaining)"
                    )
                    _gameEvents.emit("sow")
                    delay(200) // Smooth animation timing
                }

                // Decide phase
                val nextIdx = (currentCellIndex + direction).mod(totalCells)
                val nextCell = localBoard[nextIdx]

                if (nextCell.isMandarin) {
                    // Turn ends because we landed adjacent to a Mandarin
                    _sowProgress.value = SowProgress(0, currentCellIndex, direction, srcIndex, "Adjoined to Mandarin cell $nextIdx. Sowing complete!")
                    _gameEvents.emit("stops")
                    delay(800)
                    break
                } else if (nextCell.qtyDan > 0) {
                    // Landed before an occupied pawn cell, pick up all and continue
                    _sowProgress.value = SowProgress(
                        stonesInHand = 0,
                        currentCellIndex = currentCellIndex,
                        direction = direction,
                        startCellIndex = srcIndex,
                        stateText = "Landed before cell $nextIdx. Picking up ${nextCell.qtyDan} stones!"
                    )
                    _gameEvents.emit("pickup")
                    delay(600)

                    stonesInHand = nextCell.qtyDan
                    localBoard[nextIdx] = nextCell.copy(qtyDan = 0)
                    currentCellIndex = nextIdx
                    _boardState.value = localBoard.toList()

                    _sowProgress.value = SowProgress(
                        stonesInHand = stonesInHand,
                        currentCellIndex = currentCellIndex,
                        direction = direction,
                        startCellIndex = srcIndex,
                        stateText = "Continuing sowing with $stonesInHand stones!"
                    )
                    delay(400)
                } else {
                    // Landed before an EMPTY cell. Check for Capture!
                    var currentEmptyIdx = nextIdx
                    var capturedPointsThisMove = 0
                    
                    while (true) {
                        val targetIdx = (currentEmptyIdx + direction).mod(totalCells)
                        val targetCell = localBoard[targetIdx]

                        // If the cell after empty is NOT empty
                        if (localBoard[currentEmptyIdx].qtyDan == 0 && (targetCell.qtyDan > 0 || targetCell.qtyQuan > 0)) {
                            // Check young mandarin rule
                            if (targetCell.isMandarin && !_allowYoungMandarinCapture.value && targetCell.qtyDan == 0 && targetCell.qtyQuan > 0) {
                                _sowProgress.value = SowProgress(0, currentEmptyIdx, direction, srcIndex, "Opposite Mandarin cell $targetIdx is young. Cannot capture!")
                                _gameEvents.emit("rule_bypassed")
                                delay(1200)
                                break
                            }

                            val danPts = targetCell.qtyDan
                            val quanPts = targetCell.qtyQuan * OAnQuanEngine.MANDARIN_VALUE
                            val totalPts = danPts + quanPts

                            capturedPointsThisMove += totalPts

                            _sowProgress.value = SowProgress(
                                stonesInHand = 0,
                                currentCellIndex = currentEmptyIdx,
                                direction = direction,
                                startCellIndex = srcIndex,
                                stateText = "Captured cell $targetIdx! Got $totalPts points!"
                            )
                            _gameEvents.emit("capture")

                            // Empty captured cell
                            localBoard[targetIdx] = targetCell.copy(qtyDan = 0, qtyQuan = 0)
                            _boardState.value = localBoard.toList()

                            // Update active player's score
                            _players.value = _players.value.mapIndexed { pIdx, player ->
                                if (pIdx == activePlayer) {
                                    player.copy(score = player.score + totalPts)
                                } else {
                                    player
                                }
                            }
                            delay(1000) // Settle visual capturing feedback

                            // Look ahead for chain captures: must have empty cell then full cell
                            val checkEmptyIdx = (targetIdx + direction).mod(totalCells)
                            currentEmptyIdx = checkEmptyIdx
                        } else {
                            _sowProgress.value = SowProgress(
                                stonesInHand = 0,
                                currentCellIndex = currentEmptyIdx,
                                direction = direction,
                                startCellIndex = srcIndex,
                                stateText = "Move completed: Captured total $capturedPointsThisMove points."
                            )
                            delay(600)
                            break
                        }
                    }
                    break
                }
            }

            // Move completed. Append to log
            val fullMoveSim = OAnQuanEngine.simulateMoveFully(board, activePlayer, srcIndex, direction, _allowYoungMandarinCapture.value)
            val logItem = GameMove(
                playerIndex = activePlayer,
                srcCellIndex = srcIndex,
                direction = direction,
                actionText = "Moved from $srcIndex in direction $direction. Captured ${fullMoveSim.capturedPoints} points.",
                capturedPoints = fullMoveSim.capturedPoints
            )
            _movesLog.value = _movesLog.value + logItem
            
            finalizeTurnAndAdvance()
        }
    }

    private suspend fun finalizeTurnAndAdvance() {
        val board = _boardState.value
        val shape = _selectedBoardShape.value

        // Check central game termination state
        if (OAnQuanEngine.isGameOver(board)) {
            // Game Over! Distribute residual stones
            val distributions = OAnQuanEngine.distributeRemainingStones(board, shape)
            _players.value = _players.value.mapIndexed { idx, player ->
                val addition = distributions[idx] ?: 0
                player.copy(score = player.score + addition)
            }

            // Clear board
            _boardState.value = _boardState.value.map { it.copy(qtyDan = 0, qtyQuan = 0) }

            // Find overall winner
            val highestScore = _players.value.maxOfOrNull { it.score } ?: 0
            _players.value = _players.value.map { player ->
                player.copy(isWinner = player.score == highestScore)
            }

            val winner = _players.value.firstOrNull { it.isWinner }
            
            // Save results to Room persistence
            val historyItem = MatchHistory(
                numPlayers = shape.maxPlayers,
                boardShape = shape.displayName,
                playerNames = _players.value.joinToString(",") { it.name },
                playerScores = _players.value.joinToString(",") { it.score.toString() },
                winnerName = winner?.name ?: "Draw / None",
                winnerScore = winner?.score ?: 0
            )
            
            repository.insertMatch(historyItem)
            _gameState.value = GamePlayState.GAME_OVER
            _sowProgress.value = null
            _gameEvents.emit("game_over")
        } else {
            // Check next player's status
            val nextPlayerIdx = (_activePlayerIndex.value + 1) % shape.maxPlayers
            _activePlayerIndex.value = nextPlayerIdx
            _sowProgress.value = null
            _gameState.value = GamePlayState.PLAYING

            // Empty side check
            feedPlayerIfRowEmpty(nextPlayerIdx)

            // Trigger engine triggers for prospective CPU turns
            triggerEngineCheckAndAI()
        }
    }

    private suspend fun feedPlayerIfRowEmpty(playerIndex: Int) {
        val board = _boardState.value
        val shape = _selectedBoardShape.value
        
        // Find if they have any stones in their own lawn/pawns
        var totalStonesInRow = 0
        for (i in board.indices) {
            if (OAnQuanEngine.isCellOwner(playerIndex, i, shape)) {
                totalStonesInRow += board[i].qtyDan
            }
        }

        if (totalStonesInRow == 0) {
            // Must feed 5 stones! (1 per pawns)
            val costToFeed = OAnQuanEngine.PAWN_COUNT_PER_SIDE
            _players.value = _players.value.mapIndexed { idx, player ->
                if (idx == playerIndex) {
                    player.copy(score = player.score - costToFeed) // Goes into score debt
                } else {
                    player
                }
            }

            val fedBoard = board.map { cell ->
                if (OAnQuanEngine.isCellOwner(playerIndex, cell.index, shape)) {
                    cell.copy(qtyDan = 1)
                } else {
                    cell
                }
            }
            _boardState.value = fedBoard
            
            val activeP = _players.value.getOrNull(playerIndex)
            val logItem = GameMove(
                playerIndex = playerIndex,
                srcCellIndex = -1,
                direction = 0,
                actionText = "${activeP?.name ?: "Player"} was empty! Fed 5 stones (1 per cell) from captured stash.",
                capturedPoints = -costToFeed
            )
            _movesLog.value = _movesLog.value + logItem
            _gameEvents.emit("feed")
            delay(1000)
        }
    }

    private fun triggerEngineCheckAndAI() {
        val activePlayer = _activePlayerIndex.value
        val player = _players.value.getOrNull(activePlayer) ?: return
        val shape = _selectedBoardShape.value
        val board = _boardState.value

        if (player.type != PlayerType.HUMAN && _gameState.value == GamePlayState.PLAYING) {
            viewModelScope.launch {
                delay(1200) // Smooth artificial lookahead latency before playing
                val decision = OAnQuanEngine.chooseBestMove(
                    board = board,
                    aiPlayerIndex = activePlayer,
                    shape = shape,
                    difficulty = player.type,
                    allowYoungMandarinCapture = _allowYoungMandarinCapture.value
                )

                if (decision.srcIndex != -1) {
                    performSowMove(decision.srcIndex, decision.direction)
                } else {
                    // No valid moves, must skip or end turn
                    finalizeTurnAndAdvance()
                }
            }
        }
    }

    fun clearStatsDb() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
