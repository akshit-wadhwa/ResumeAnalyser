package com.resumeanalyser.core.parsing;

import java.io.InputStream;

public class ParsingUtils {

    private ParsingUtils() {
    }

    public static FileParser parserFor(DocumentType type) {
        if (type == DocumentType.DOCX) {
            return new DocxParser();
        }
        return new PdfParser();
    }

    public static ParsingResult parse(DocumentType type, InputStream inputStream) throws Exception {
        return parserFor(type).parse(inputStream);
    }
}
