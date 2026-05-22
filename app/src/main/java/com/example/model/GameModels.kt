package com.example.model

enum class BoardShape(val displayName: String, val maxPlayers: Int, val sides: Int) {
    RECTANGLE("Rectangle (2 Players)", 2, 2),
    TRIANGLE("Triangle (3 Players)", 3, 3),
    SQUARE("Square (4 Players)", 4, 4),
    HEXAGON("Hexagon (5 Players)", 5, 6)
}

enum class PlayerType(val displayName: String) {
    HUMAN("Human"),
    AI_EASY("AI (Easy)"),
    AI_HARD("AI (Hard)")
}

data class Player(
    val id: Int,
    val name: String,
    val type: PlayerType,
    val colorHex: String,
    val score: Int = 0,
    val borrowedPoints: Int = 0,
    val isWinner: Boolean = false
)

data class Cell(
    val index: Int,
    val section: Int,
    val qtyDan: Int,
    val qtyQuan: Int,
    val isMandarin: Boolean
)

enum class GamePlayState {
    CONFIG,
    PLAYING,
    SOWING_ANIMATION,
    GAME_OVER
}

data class SowProgress(
    val stonesInHand: Int,
    val currentCellIndex: Int,
    val direction: Int, // 1 for Clockwise (increasing index), -1 for Counter-Clockwise (decreasing index)
    val startCellIndex: Int,
    val stateText: String = "",
    val multipleCaptureChainCount: Int = 0
)

data class GameMove(
    val playerIndex: Int,
    val srcCellIndex: Int,
    val direction: Int, // 1 or -1
    val actionText: String,
    val capturedPoints: Int,
    val timestamp: Long = System.currentTimeMillis()
)
