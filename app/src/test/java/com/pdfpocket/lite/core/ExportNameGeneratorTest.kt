package com.pdfpocket.lite.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExportNameGeneratorTest {
    @Test fun createsSafeExportName() {
        assertThat(ExportNameGenerator.generate("contrat.pdf", "signé")).isEqualTo("contrat_signé.pdf")
    }
}
