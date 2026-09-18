package com.navadhiti.resumeanalyzer.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class PdfExtractionService {

    /**
     * Pulls readable text out of an uploaded PDF resume.
     *
     * @throws ResumeProcessingException if the file isn't a real/valid PDF,
     *                                    or contains no extractable text
     *                                    (e.g. a scanned image with no OCR layer).
     */
    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResumeProcessingException("No file was uploaded. Please choose a resume file.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new ResumeProcessingException("Only PDF files are supported right now. Please upload a .pdf resume.");
        }

        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            if (document.isEncrypted()) {
                throw new ResumeProcessingException("This PDF is password-protected. Please upload an unprotected file.");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            if (text == null || text.trim().length() < 30) {
                throw new ResumeProcessingException(
                        "Couldn't find readable text in this PDF. It may be a scanned image without OCR - " +
                        "please upload a text-based PDF resume."
                );
            }

            return text.trim();

        } catch (IOException e) {
            throw new ResumeProcessingException("This file couldn't be read as a valid PDF: " + e.getMessage());
        }
    }
}
