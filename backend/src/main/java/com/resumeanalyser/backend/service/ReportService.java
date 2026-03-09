package com.resumeanalyser.backend.service;

import java.io.ByteArrayOutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import com.resumeanalyser.backend.dto.AnalysisResultDto;

@Service
public class ReportService {

    public byte[] buildReport(AnalysisResultDto result) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                float y = 730;
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA_BOLD, 20);
                stream.newLineAtOffset(50, y);
                stream.showText("Resume Analysis Report");
                stream.endText();

                y -= 40;
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                stream.newLineAtOffset(50, y);
                stream.showText("Overall Score");
                stream.endText();

                y -= 20;
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(50, y);
                stream.showText(String.format("Match Score: %.1f%%", result.getMatchScore()));
                stream.endText();

                y -= 18;
                stream.beginText();
                stream.newLineAtOffset(50, y);
                stream.showText(String.format("Confidence: %.1f%%", result.getConfidenceScore()));
                stream.endText();

                y -= 18;
                stream.beginText();
                stream.newLineAtOffset(50, y);
                stream.showText("Seniority: " + (result.getSeniority() != null ? result.getSeniority() : "N/A"));
                stream.endText();

                y -= 30;
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                stream.newLineAtOffset(50, y);
                stream.showText("Matched Skills");
                stream.endText();

                stream.setFont(PDType1Font.HELVETICA, 11);
                for (String skill : result.getMatchedSkills()) {
                    y -= 16;
                    if (y < 50) {
                        break;
                    }
                    stream.beginText();
                    stream.newLineAtOffset(60, y);
                    stream.showText("\u2022 " + skill);
                    stream.endText();
                }

                y -= 30;
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                stream.newLineAtOffset(50, y);
                stream.showText("Missing Skills");
                stream.endText();

                stream.setFont(PDType1Font.HELVETICA, 11);
                for (String skill : result.getMissingSkills()) {
                    y -= 16;
                    if (y < 50) {
                        break;
                    }
                    stream.beginText();
                    stream.newLineAtOffset(60, y);
                    stream.showText("\u2022 " + skill);
                    stream.endText();
                }

                y -= 30;
                if (y > 100) {
                    stream.beginText();
                    stream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                    stream.newLineAtOffset(50, y);
                    stream.showText("Recommendations");
                    stream.endText();

                    stream.setFont(PDType1Font.HELVETICA, 11);
                    for (String rec : result.getRecommendations()) {
                        y -= 16;
                        if (y < 50) {
                            break;
                        }
                        stream.beginText();
                        stream.newLineAtOffset(60, y);
                        stream.showText("\u2022 " + rec);
                        stream.endText();
                    }
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}
