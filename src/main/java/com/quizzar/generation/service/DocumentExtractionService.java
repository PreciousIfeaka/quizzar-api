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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class DocumentExtractionService {

    public String extractText(MultipartFile file) {
        String contentType = file.getContentType();
        String originalFilename = StringUtils.hasText(file.getOriginalFilename()) 
            ? file.getOriginalFilename().toLowerCase() : "";
        
        try {
            if (contentType != null && contentType.equals("application/pdf") || originalFilename.endsWith(".pdf")) {
                return extractFromPdf(file);
            } else if (contentType != null && contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") 
                       || originalFilename.endsWith(".docx")) {
                return extractFromDocx(file);
            } else if (contentType != null && contentType.equals("application/msword") 
                       || originalFilename.endsWith(".doc")) {
                return extractFromDoc(file);
            } else if (contentType != null && contentType.startsWith("text/") 
                       || originalFilename.endsWith(".txt")) {
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            } else {
                throw new InvalidFileTypeException("Unsupported file type: " + contentType);
            }
        } catch (InvalidFileTypeException e) {
            throw e;
        } catch (Exception e) {
            throw new DocumentExtractionException("Failed to extract text from document: " + e.getMessage());
        }
    }

    private String extractFromPdf(MultipartFile file) throws IOException {
        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private String extractFromDocx(MultipartFile file) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
            XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
            return extractor.getText();
        }
    }

    private String extractFromDoc(MultipartFile file) throws IOException {
        try (HWPFDocument doc = new HWPFDocument(file.getInputStream())) {
            WordExtractor extractor = new WordExtractor(doc);
            return extractor.getText();
        }
    }
}
