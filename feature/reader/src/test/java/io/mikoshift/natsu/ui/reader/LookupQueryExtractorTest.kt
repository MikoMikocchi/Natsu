package io.mikoshift.natsu.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LookupQueryExtractorTest {
    @Test
    fun extractLookupQuery_prefersCjkSequence() {
        assertEquals("本", extractLookupQuery("See 本 here"))
    }

    @Test
    fun extractLookupQuery_fallsBackToFirstWord() {
        assertEquals("hello", extractLookupQuery("hello world"))
    }

    @Test
    fun extractLookupQuery_returnsNullForBlank() {
        assertNull(extractLookupQuery("   "))
    }
}
