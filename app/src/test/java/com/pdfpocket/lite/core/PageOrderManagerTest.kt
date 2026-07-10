package com.pdfpocket.lite.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PageOrderManagerTest {
    @Test fun movesPage() {
        assertThat(PageOrderManager.move(listOf(0, 1, 2, 3), 0, 2)).containsExactly(1, 2, 0, 3).inOrder()
    }
    @Test fun rotatesSelectedPages() {
        assertThat(PageOrderManager.rotateSelection(mapOf(0 to 90), setOf(0, 2), 270)).containsExactly(0, 0, 2, 270)
    }
}
