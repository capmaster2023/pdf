package com.pdfpocket.lite.core

object PageRangeParser {
    fun parse(input: String, pageCount: Int): Result<List<Int>> = runCatching {
        require(pageCount > 0 && input.isNotBlank())
        val result = linkedSetOf<Int>()
        input.split(',').map(String::trim).filter(String::isNotBlank).forEach { token ->
            if ('-' in token) {
                val parts = token.split('-', limit = 2)
                val start = parts[0].trim().toInt()
                val end = parts[1].trim().toInt()
                require(start in 1..pageCount && end in 1..pageCount && start <= end)
                for (page in start..end) result += page - 1
            } else {
                val page = token.toInt()
                require(page in 1..pageCount)
                result += page - 1
            }
        }
        require(result.isNotEmpty())
        result.toList()
    }
}
