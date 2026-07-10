package com.pdfpocket.lite.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PageRangeParserTest {
    @Test fun parsesSingleRangesAndLists() {
        assertThat(PageRangeParser.parse("1-3,5,8-9", 10).getOrThrow()).containsExactly(0, 1, 2, 4, 7, 8).inOrder()
    }
    @Test fun removesDuplicatesWithoutChangingOrder() {
        assertThat(PageRangeParser.parse("1-3,2,3", 4).getOrThrow()).containsExactly(0, 1, 2).inOrder()
    }
    @Test fun rejectsOutOfBoundsRange() {
        assertThat(PageRangeParser.parse("1-12", 5).isFailure).isTrue()
    }
    @Test fun rejectsReversedRange() {
        assertThat(PageRangeParser.parse("5-2", 8).isFailure).isTrue()
    }
}
