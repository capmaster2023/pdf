package com.pdfpocket.lite.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FilenameValidatorTest {
    @Test fun removesInvalidCharacters() {
        assertThat(FilenameValidator.sanitize(" dossier:secret?.pdf ")).isEqualTo("dossier_secret_.pdf")
    }
    @Test fun appendsPdfExtension() {
        assertThat(FilenameValidator.ensurePdf("rapport")).isEqualTo("rapport.pdf")
    }
    @Test fun keepsExistingPdfExtension() {
        assertThat(FilenameValidator.ensurePdf("rapport.PDF")).isEqualTo("rapport.PDF")
    }
}
