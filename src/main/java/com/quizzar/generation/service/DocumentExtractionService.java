package com.quizzar.generation.service;

import com.quizzar.common.exception.DocumentExtractionException;
import com.quizzar.common.exception.InvalidFileTypeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class DocumentExtractionService {

    public String extractText(byte[] fileBytes, String filename, String contentType) {
        String originalFilename = StringUtils.hasText(filename) ? filename.toLowerCase() : "";
        
        try {
            if (contentType != null && contentType.equals("application/pdf") || originalFilename.endsWith(".pdf")) {
                return extractFromPdf(fileBytes);
            } else if (contentType != null && contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") 
                       || originalFilename.endsWith(".docx")) {
                return extractFromDocx(fileBytes);
            } else if (contentType != null && contentType.equals("application/msword") 
                       || originalFilename.endsWith(".doc")) {
                return extractFromDoc(fileBytes);
            } else if (contentType != null && contentType.startsWith("text/") 
                       || originalFilename.endsWith(".txt")) {
                return new String(fileBytes, StandardCharsets.UTF_8);
            } else {
                throw new InvalidFileTypeException("Unsupported file type: " + contentType);
            }
        } catch (InvalidFileTypeException e) {
            throw e;
        } catch (Exception e) {
            throw new DocumentExtractionException("Failed to extract text from document: " + e.getMessage());
        }
    }

    private String extractFromPdf(byte[] fileBytes) throws IOException {
        try (PDDocument doc = Loader.loadPDF(fileBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private String extractFromDocx(byte[] fileBytes) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(fileBytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }

    private String extractFromDoc(byte[] fileBytes) throws IOException {
        try (HWPFDocument doc = new HWPFDocument(new ByteArrayInputStream(fileBytes));
             WordExtractor extractor = new WordExtractor(doc)) {
            return extractor.getText();
        }
    }
}
