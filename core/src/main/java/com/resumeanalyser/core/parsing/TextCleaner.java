package com.resumeanalyser.core.parsing;

import org.apache.commons.text.StringEscapeUtils;

public class TextCleaner {
    private TextCleaner() {
    }

    public static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        String text = StringEscapeUtils.unescapeHtml4(raw);
        text = text.replaceAll("\\r", "\n");
        text = text.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s]", " ");
        text = text.replaceAll("\\s+", " ").trim();
        return text.toLowerCase();
    }
}
