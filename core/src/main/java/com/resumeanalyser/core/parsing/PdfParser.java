package com.resumeanalyser.core.parsing;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class PdfParser implements FileParser {
    @Override
    public ParsingResult parse(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String rawText = stripper.getText(document);
            String cleaned = TextCleaner.clean(rawText);
            return new ParsingResult(cleaned, Map.of("pages", String.valueOf(document.getNumberOfPages())));
        }
    }
}
