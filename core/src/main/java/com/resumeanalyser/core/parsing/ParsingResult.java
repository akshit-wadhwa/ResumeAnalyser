package com.resumeanalyser.core.parsing;

import java.util.Map;

public class ParsingResult {
    private final String text;
    private final Map<String, String> metadata;

    public ParsingResult(String text, Map<String, String> metadata) {
        this.text = text;
        this.metadata = metadata;
    }

    public String getText() {
        return text;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }
}
