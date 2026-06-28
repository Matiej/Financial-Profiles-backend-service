package com.emat.reapi.ai.port;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
class TextChunksSplitter {
    private static final int DEFAULT_TOLERANCE = 50; // Allow sentences to go slightly beyond limit


    public List<String> splitByLength(String text, int maxLength) {
        List<String> chunks = new ArrayList<>();
        int length = text.length();
        for (int i = 0; i < length; i += maxLength) {
            int end = Math.min(i + maxLength, length);
            chunks.add(text.substring(i, end).trim());
        }
        return chunks;
    }

    public List<String> splitBySentences(String text, int maxChunkLength) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        int length = text.length();

        while (start < length) {
            int end = Math.min(start + maxChunkLength, length);
            int breakPoint = findSentenceEnd(text, start, end, DEFAULT_TOLERANCE);

            if (breakPoint <= start) {
                breakPoint = Math.min(start + maxChunkLength, length);
            }

            String chunk = text.substring(start, breakPoint).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            start = breakPoint;
        }

        return chunks;
    }

    /**
     * Finds the last sentence-ending punctuation within the given range.
     * Looks for '.', '!' or '?' between start and limit.
     */
    private int findSentenceEnd(String text, int start, int limit, int maxTolerance) {
        int lastDot = -1;
        int tolerance = Math.min(limit / 10, maxTolerance);
        int softLimit = Math.min(start + limit + tolerance, text.length());

        for (int i = softLimit - 1; i > start; i--) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?') {
                lastDot = i;
                break;
            }
        }

        return (lastDot != -1) ? lastDot + 1 : limit;
    }
}
