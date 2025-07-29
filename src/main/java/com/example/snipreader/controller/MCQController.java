package com.example.snipreader.controller;

import com.example.snipreader.model.ImageUploadForm;
import com.example.snipreader.model.Question;
import com.example.snipreader.model.QuestionSet;
import com.example.snipreader.repository.QuestionSetRepository;
import com.example.snipreader.service.DocumentService;
import com.example.snipreader.service.GeminiService;
import com.itextpdf.text.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controller for handling MCQ screenshot uploads and processing.
 */
@Controller
public class MCQController {
    private static final Logger logger = LoggerFactory.getLogger(MCQController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    private final GeminiService geminiService;
    private final QuestionSetRepository questionSetRepository;
    private final DocumentService documentService;
    
    @Autowired
    public MCQController(GeminiService geminiService, QuestionSetRepository questionSetRepository, 
                         DocumentService documentService) {
        this.geminiService = geminiService;
        this.questionSetRepository = questionSetRepository;
        this.documentService = documentService;
    }
    
    /**
     * Download a question set as a PDF document.
     *
     * @param id the ID of the question set to download
     * @return the PDF document as a response entity
     */
    @GetMapping("/results/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        Optional<QuestionSet> questionSetOpt = questionSetRepository.findById(id);
        
        if (questionSetOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            QuestionSet questionSet = questionSetOpt.get();
            byte[] pdfBytes = documentService.generatePdf(questionSet);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "mcq-results-" + id + ".pdf");
            headers.setContentLength(pdfBytes.length);
            
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (DocumentException e) {
            logger.error("Error generating PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Download a question set as a Word document.
     *
     * @param id the ID of the question set to download
     * @return the Word document as a response entity
     */
    @GetMapping("/results/{id}/word")
    public ResponseEntity<byte[]> downloadWord(@PathVariable Long id) {
        Optional<QuestionSet> questionSetOpt = questionSetRepository.findById(id);
        
        if (questionSetOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            QuestionSet questionSet = questionSetOpt.get();
            byte[] wordBytes = documentService.generateWord(questionSet);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
            headers.setContentDispositionFormData("attachment", "mcq-results-" + id + ".docx");
            headers.setContentLength(wordBytes.length);
            
            return new ResponseEntity<>(wordBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            logger.error("Error generating Word document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Download multiple selected question sets as a single document.
     *
     * @param selectedSets the IDs of the question sets to download
     * @param format the format to download (pdf or word)
     * @return the document as a response entity
     */
    @PostMapping("/download-selected")
    public ResponseEntity<byte[]> downloadSelected(@RequestParam("selectedSets") List<Long> selectedSets, 
                                                 @RequestParam("format") String format) {
        if (selectedSets == null || selectedSets.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        List<QuestionSet> questionSets = new ArrayList<>();
        for (Long id : selectedSets) {
            Optional<QuestionSet> questionSetOpt = questionSetRepository.findById(id);
            questionSetOpt.ifPresent(questionSets::add);
        }
        
        if (questionSets.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            byte[] documentBytes;
            HttpHeaders headers = new HttpHeaders();
            
            if ("pdf".equalsIgnoreCase(format)) {
                documentBytes = documentService.generatePdfFromMultiple(questionSets);
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDispositionFormData("attachment", "selected-mcq-results.pdf");
            } else {
                documentBytes = documentService.generateWordFromMultiple(questionSets);
                headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
                headers.setContentDispositionFormData("attachment", "selected-mcq-results.docx");
            }
            
            headers.setContentLength(documentBytes.length);
            return new ResponseEntity<>(documentBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            logger.error("Error generating document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Displays the home page with the upload form.
     *
     * @param model the model to add attributes to
     * @return the view name
     */
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("imageUploadForm", new ImageUploadForm());
        // Add recent question sets to the model
        List<QuestionSet> recentSets = questionSetRepository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("recentSets", recentSets);
        return "index";
    }
    
    /**
     * Displays the history of all question sets.
     *
     * @param model the model to add attributes to
     * @return the view name
     */
    @GetMapping("/history")
    public String viewHistory(Model model) {
        List<QuestionSet> questionSets = questionSetRepository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("questionSets", questionSets);
        model.addAttribute("dateFormatter", DATE_FORMATTER);
        return "history";
    }
    
    /**
     * Displays a specific question set by ID.
     *
     * @param id the ID of the question set to display
     * @param model the model to add attributes to
     * @param redirectAttributes attributes for redirect scenarios
     * @return the view name
     */
    @GetMapping("/results/{id}")
    public String viewQuestionSet(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<QuestionSet> questionSetOpt = questionSetRepository.findById(id);
        
        if (questionSetOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Question set not found");
            return "redirect:/history";
        }
        
        QuestionSet questionSet = questionSetOpt.get();
        model.addAttribute("questions", questionSet.getQuestions());
        model.addAttribute("questionSet", questionSet);
        model.addAttribute("dateFormatter", DATE_FORMATTER);
        
        // Find previous and next question sets for navigation
        List<QuestionSet> allSets = questionSetRepository.findAllByOrderByCreatedAtDesc();
        int currentIndex = -1;
        
        for (int i = 0; i < allSets.size(); i++) {
            if (allSets.get(i).getId().equals(id)) {
                currentIndex = i;
                break;
            }
        }
        
        if (currentIndex > 0) {
            model.addAttribute("previousSet", allSets.get(currentIndex - 1));
        }
        
        if (currentIndex < allSets.size() - 1 && currentIndex >= 0) {
            model.addAttribute("nextSet", allSets.get(currentIndex + 1));
        }
        
        return "results";
    }
    
    /**
     * Processes the uploaded screenshot and displays the results.
     *
     * @param imageUploadForm the form with the uploaded image
     * @param model the model to add attributes to
     * @param redirectAttributes attributes for redirect scenarios
     * @return the view name
     */
    @PostMapping("/upload")
    public String processImage(@ModelAttribute ImageUploadForm imageUploadForm, 
                              Model model, 
                              RedirectAttributes redirectAttributes) {
        MultipartFile imageFile = imageUploadForm.getImage();
        
        // Validate the uploaded file
        if (imageFile == null || imageFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select an image to upload");
            return "redirect:/";
        }
        
        try {
            // Process the image with Gemini 2.5 Flash to extract questions and generate answers in one step
            logger.info("Processing uploaded image with Gemini 2.5 Flash: {}", imageFile.getOriginalFilename());
            List<Question> questions = geminiService.processImageForQuestions(imageFile);
            
            if (questions.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No questions could be extracted from the image");
                return "redirect:/";
            }
            
            // Create and save a new QuestionSet
            String title = "Uploaded Image: " + (imageFile.getOriginalFilename() != null ? 
                    imageFile.getOriginalFilename() : "Unnamed");
            QuestionSet questionSet = new QuestionSet(title);
            
            // Add all questions to the question set
            for (Question question : questions) {
                questionSet.addQuestion(question);
            }
            
            // Save to database
            questionSet = questionSetRepository.save(questionSet);
            
            // Redirect to the saved question set
            return "redirect:/results/" + questionSet.getId();
            
        } catch (IOException e) {
            logger.error("Error processing the uploaded file", e);
            redirectAttributes.addFlashAttribute("error", "Error processing the uploaded file: " + e.getMessage());
            return "redirect:/";
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred: " + e.getMessage());
            return "redirect:/";
        }
    }
    
    /**
     * Processes a pasted screenshot and displays the results.
     *
     * @param imageUploadForm the form with the base64 image data
     * @param model the model to add attributes to
     * @param redirectAttributes attributes for redirect scenarios
     * @return the view name
     */
    @PostMapping("/paste-image")
    public String processPastedImage(@ModelAttribute ImageUploadForm imageUploadForm, 
                                    Model model, 
                                    RedirectAttributes redirectAttributes) {
        String base64Image = imageUploadForm.getBase64Image();
        
        // Validate the pasted image data
        if (base64Image == null || base64Image.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No image data was provided. Please paste an image.");
            return "redirect:/";
        }
        
        try {
            // Process the base64 image with Gemini 2.5 Flash
            logger.info("Processing pasted image with Gemini 2.5 Flash");
            List<Question> questions = geminiService.processBase64ImageForQuestions(base64Image);
            
            if (questions.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No questions could be extracted from the image");
                return "redirect:/";
            }
            
            // Create and save a new QuestionSet
            QuestionSet questionSet = new QuestionSet("Pasted Image");
            
            // Add all questions to the question set
            for (Question question : questions) {
                questionSet.addQuestion(question);
            }
            
            // Save to database
            questionSet = questionSetRepository.save(questionSet);
            
            // Redirect to the saved question set
            return "redirect:/results/" + questionSet.getId();
            
        } catch (IOException e) {
            logger.error("Error processing the pasted image", e);
            redirectAttributes.addFlashAttribute("error", "Error processing the pasted image: " + e.getMessage());
            return "redirect:/";
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred: " + e.getMessage());
            return "redirect:/";
        }
    }
}