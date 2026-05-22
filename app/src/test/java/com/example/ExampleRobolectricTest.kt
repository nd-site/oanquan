package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.OAnQuanEngine
import com.example.model.BoardShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("O An Quan", appName)
  }

  @Test
  fun `verify board creation for rectangle and hexagon`() {
    // 2 players: rectangle
    val rectangleBoard = OAnQuanEngine.createInitialBoard(BoardShape.RECTANGLE)
    assertNotNull(rectangleBoard)
    // 2 sections * (5 pawns + 1 mandarin) = 12 cells
    assertEquals(12, rectangleBoard.size)

    // Verify first pawn contains 5 stones
    assertEquals(5, rectangleBoard[0].qtyDan)
    assertEquals(0, rectangleBoard[0].qtyQuan)

    // Verify cell 5 is Mandarin
    assertEquals(true, rectangleBoard[5].isMandarin)
    assertEquals(1, rectangleBoard[5].qtyQuan)
    assertEquals(0, rectangleBoard[5].qtyDan)

    // 5 players: hexagon (6 sides)
    val hexagonBoard = OAnQuanEngine.createInitialBoard(BoardShape.HEXAGON)
    // 6 sections * (5 pawns + 1 mandarin) = 36 cells
    assertEquals(36, hexagonBoard.size)
  }
}
