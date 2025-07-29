package com.example.snipreader.service;

import com.example.snipreader.model.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for parsing OCR text into structured Question objects.
 */
@Service
public class QuestionParserService {
    private static final Logger logger = LoggerFactory.getLogger(QuestionParserService.class);
    
    // Pattern to match question numbers (e.g., "1.", "Q1.", "Question 1:")
    private static final Pattern QUESTION_NUMBER_PATTERN = 
            Pattern.compile("(?:^|\\n)\\s*(?:Q(?:uestion)?\\s*)?\\d+[.:]\\s*", Pattern.CASE_INSENSITIVE);
    
    // Pattern to match option labels (e.g., "A)", "a.", "(B)", "C.")
    private static final Pattern OPTION_PATTERN = 
            Pattern.compile("(?:^|\\n)\\s*(?:\\()?([A-D])(?:\\)|\\.)\\s*", Pattern.CASE_INSENSITIVE);

    /**
     * Parses OCR text into a list of structured Question objects.
     *
     * @param ocrText the text extracted from the image
     * @return a list of Question objects
     */
    public List<Question> parseQuestions(String ocrText) {
        List<Question> questions = new ArrayList<>();
        
        try {
            // Split the text into question blocks
            List<String> questionBlocks = splitIntoQuestionBlocks(ocrText);
            
            for (String block : questionBlocks) {
                Question question = parseQuestionBlock(block);
                if (question != null) {
                    questions.add(question);
                    logger.info("Parsed question: {}", question.getQuestionText());
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing questions from OCR text", e);
        }
        
        return questions;
    }
    
    /**
     * Splits the OCR text into individual question blocks.
     *
     * @param ocrText the text extracted from the image
     * @return a list of question blocks
     */
    private List<String> splitIntoQuestionBlocks(String ocrText) {
        List<String> blocks = new ArrayList<>();
        Matcher matcher = QUESTION_NUMBER_PATTERN.matcher(ocrText);
        
        int startIndex = 0;
        int endIndex;
        
        while (matcher.find()) {
            if (startIndex > 0) {
                endIndex = matcher.start();
                blocks.add(ocrText.substring(startIndex, endIndex).trim());
            }
            startIndex = matcher.start();
        }
        
        // Add the last question block
        if (startIndex < ocrText.length()) {
            blocks.add(ocrText.substring(startIndex).trim());
        }
        
        // If no questions were found, treat the entire text as one question
        if (blocks.isEmpty() && !ocrText.trim().isEmpty()) {
            blocks.add(ocrText.trim());
        }
        
        return blocks;
    }
    
    /**
     * Parses a question block into a Question object.
     *
     * @param block the question block text
     * @return a Question object
     */
    private Question parseQuestionBlock(String block) {
        // Skip empty blocks
        if (block == null || block.trim().isEmpty()) {
            return null;
        }
        
        // Extract question text (everything before the first option)
        String questionText = extractQuestionText(block);
        if (questionText == null || questionText.trim().isEmpty()) {
            return null;
        }
        
        // Extract options
        List<String> options = extractOptions(block);
        if (options.isEmpty()) {
            return null;
        }
        
        return new Question(questionText.trim(), options);
    }
    
    /**
     * Extracts the question text from a question block.
     *
     * @param block the question block text
     * @return the question text
     */
    private String extractQuestionText(String block) {
        // Remove question number if present
        Matcher numberMatcher = QUESTION_NUMBER_PATTERN.matcher(block);
        if (numberMatcher.find()) {
            block = block.substring(numberMatcher.end());
        }
        
        // Find the first option
        Matcher optionMatcher = OPTION_PATTERN.matcher(block);
        if (optionMatcher.find()) {
            return block.substring(0, optionMatcher.start()).trim();
        }
        
        // If no options are found, return the entire block
        return block.trim();
    }
    
    /**
     * Extracts the options from a question block.
     *
     * @param block the question block text
     * @return a list of options
     */
    private List<String> extractOptions(String block) {
        List<String> options = new ArrayList<>();
        Matcher matcher = OPTION_PATTERN.matcher(block);
        
        int startIndex = -1;
        String currentOption = null;
        
        while (matcher.find()) {
            if (startIndex != -1 && currentOption != null) {
                String optionText = block.substring(startIndex, matcher.start()).trim();
                options.add(optionText);
            }
            
            currentOption = matcher.group(1).toUpperCase();
            startIndex = matcher.end();
        }
        
        // Add the last option
        if (startIndex != -1 && currentOption != null && startIndex < block.length()) {
            String optionText = block.substring(startIndex).trim();
            options.add(optionText);
        }
        
        return options;
    }
}