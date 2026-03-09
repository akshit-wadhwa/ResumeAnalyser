package com.resumeanalyser.core.parsing;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class PdfParser implements FileParser {

    @Override
    public ParsingResult parse(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String rawText = stripper.getText(document);
            String cleaned = TextCleaner.clean(rawText);

            Map<String, String> meta = new HashMap<>();
            meta.put("pages", String.valueOf(document.getNumberOfPages()));
            return new ParsingResult(cleaned, meta);
        }
    }
}
