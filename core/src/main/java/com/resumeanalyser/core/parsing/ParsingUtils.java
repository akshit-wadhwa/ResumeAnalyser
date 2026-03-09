package com.resumeanalyser.core.parsing;

import java.io.InputStream;

public class ParsingUtils {
    private ParsingUtils() {
    }

    public static FileParser parserFor(DocumentType type) {
        return switch (type) {
            case PDF -> new PdfParser();
            case DOCX -> new DocxParser();
        };
    }

    public static ParsingResult parse(DocumentType type, InputStream inputStream) throws Exception {
        return parserFor(type).parse(inputStream);
    }
}
