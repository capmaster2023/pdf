package com.pdfpocket.lite.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SizeFormatterTest {
    @Test fun formatsBytesAndMegabytes() {
        assertThat(SizeFormatter.format(512)).isEqualTo("512 B")
        assertThat(SizeFormatter.format(1024 * 1024)).contains("MB")
    }
}
