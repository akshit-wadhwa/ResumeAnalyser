package com.resumeanalyser.core.parsing;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;

public class DocxParser implements FileParser {
    @Override
    public ParsingResult parse(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            String text = document.getParagraphs().stream()
                .map(XWPFParagraph::getText)
                .collect(Collectors.joining("\n"));
            String cleaned = TextCleaner.clean(text);
            return new ParsingResult(cleaned, Map.of("paragraphs", String.valueOf(document.getParagraphs().size())));
        }
    }
}
