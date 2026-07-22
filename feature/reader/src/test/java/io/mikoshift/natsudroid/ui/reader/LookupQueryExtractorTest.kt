package io.mikoshift.natsudroid.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LookupQueryExtractorTest {
    @Test
    fun extractWordAtOffset_selectsWordAtTapPosition() {
        val text = "Typical Japanese textbooks have many shortcomings."
        val range = extractWordAtOffset(text, text.indexOf("textbooks"))
        assertEquals(text.indexOf("textbooks")..text.indexOf("textbooks") + "textbooks".length - 1, range)
    }

    @Test
    fun extractWordAtOffset_selectsCjkRun() {
        val text = "See 日本語 here"
        val range = extractWordAtOffset(text, text.indexOf('本'))
        assertEquals("日本語", text.substring(range!!))
    }

    @Test
    fun extractWordAtOffset_resolvesNearestWordFromWhitespace() {
        val text = "hello world"
        val range = extractWordAtOffset(text, text.indexOf(' '))
        assertEquals("hello", text.substring(range!!))
    }

    @Test
    fun extractWordAtOffset_returnsNullForPunctuationOnly() {
        assertNull(extractWordAtOffset("...", 1))
    }

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
