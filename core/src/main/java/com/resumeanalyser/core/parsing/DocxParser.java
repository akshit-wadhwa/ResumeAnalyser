package com.resumeanalyser.core.parsing;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

public class DocxParser implements FileParser {

    @Override
    public ParsingResult parse(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder textBuilder = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                if (textBuilder.length() > 0) {
                    textBuilder.append("\n");
                }
                textBuilder.append(paragraph.getText());
            }
            String text = textBuilder.toString();
            String cleaned = TextCleaner.clean(text);

            Map<String, String> meta = new HashMap<>();
            meta.put("paragraphs", String.valueOf(document.getParagraphs().size()));
            return new ParsingResult(cleaned, meta);
        }
    }
}
