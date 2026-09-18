package com.navadhiti.resumeanalyzer.controller;

import com.navadhiti.resumeanalyzer.model.AnalysisResult;
import com.navadhiti.resumeanalyzer.service.PdfExtractionService;
import com.navadhiti.resumeanalyzer.service.ResumeAnalysisService;
import com.navadhiti.resumeanalyzer.service.ResumeProcessingException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class ResumeController {

    private final PdfExtractionService pdfExtractionService;
    private final ResumeAnalysisService resumeAnalysisService;

    public ResumeController(PdfExtractionService pdfExtractionService,
                             ResumeAnalysisService resumeAnalysisService) {
        this.pdfExtractionService = pdfExtractionService;
        this.resumeAnalysisService = resumeAnalysisService;
    }

    @GetMapping("/")
    public String uploadPage() {
        return "upload";
    }

    @PostMapping("/analyze")
    public String analyze(@RequestParam("resume") MultipartFile resume, Model model) {
        try {
            String text = pdfExtractionService.extractText(resume);
            AnalysisResult result = resumeAnalysisService.analyze(text);
            model.addAttribute("result", result);
            model.addAttribute("fileName", resume.getOriginalFilename());
            return "result";

        } catch (ResumeProcessingException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "upload";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Something unexpected went wrong: " + e.getMessage());
            return "upload";
        }
    }
}
