package com.emat.reapi.ai.port

import spock.lang.Specification

/**
 * Pure-logic unit tests for {@code TextChunksSplitter} (converted from JUnit).
 */
class TextChunksSplitterSpec extends Specification {

    def splitter = new TextChunksSplitter()

    def "should split text by fixed length"() {
        when:
        def chunks = splitter.splitByLength("ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890", 10)

        then:
        chunks == ["ABCDEFGHIJ", "KLMNOPQRST", "UVWXYZ1234", "567890"]
    }

    def "should split text by sentences"() {
        when:
        def chunks = splitter.splitBySentences("Hello. This is a test. It should split at sentence boundaries! Right?", 30)

        then:
        chunks.size() == 2
        chunks[0].endsWith(".")
        chunks[1].endsWith("?")
    }

    def "should handle text shorter than chunk size"() {
        when:
        def chunks = splitter.splitByLength("Short text.", 100)

        then:
        chunks == ["Short text."]
    }

    def "should split a long sentence when no punctuation is found"() {
        when:
        def chunks = splitter.splitBySentences("This text has no sentence-ending punctuation and should be split by chunk size only", 30)

        then:
        !chunks.isEmpty()
        chunks*.length().max() <= 30
    }

    def "should not return empty chunks"() {
        when:
        def chunks = splitter.splitBySentences("....", 2)

        then:
        chunks.every { !it.isBlank() }
    }

    def "should respect the max tolerance extension"() {
        when:
        def chunks = splitter.splitBySentences("A chunk. Another sentence right beyond limit!", 20)

        then:
        chunks.size() == 3
    }

    def "should keep a sentence whole when it ends exactly at the tolerance edge"() {
        when:
        def chunks = splitter.splitBySentences("Aa sentence ends right.", 21)

        then:
        chunks.size() == 1
        chunks[0].endsWith(".")
    }

    def "should split multiple sentences with no gaps"() {
        given:
        def text = "One. Two! Three? Four. Five."

        when:
        def chunks = splitter.splitBySentences(text, 15)

        then:
        chunks.size() == 2
        chunks.join("").replaceAll(" ", "") == text.replaceAll(" ", "")
    }
}
