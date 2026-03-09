package com.resumeanalyser.backend.service;

import java.io.ByteArrayInputStream;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.resumeanalyser.core.parsing.DocumentType;
import com.resumeanalyser.core.parsing.ParsingResult;
import com.resumeanalyser.core.parsing.ParsingUtils;

@Service
public class ParsingService {

    public ParsingResult parseDocument(MultipartFile file) throws Exception {
        DocumentType type = detectType(file.getOriginalFilename());
        return ParsingUtils.parse(type, file.getInputStream());
    }

    public ParsingResult parseDocument(String filename, byte[] fileBytes) throws Exception {
        DocumentType type = detectType(filename);
        return ParsingUtils.parse(type, new ByteArrayInputStream(fileBytes));
    }

    private DocumentType detectType(String filename) {
        if (filename == null) {
            return DocumentType.PDF;
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".docx")) {
            return DocumentType.DOCX;
        }
        return DocumentType.PDF;
    }
}
