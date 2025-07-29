package com.example.snipreader.service;

import com.example.snipreader.model.Question;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for processing images with Google's Gemini 2.5 Flash API to extract MCQ questions and generate answers.
 */
@Service
public class GeminiService {
    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";
    
    @Value("${gemini.api.key}")
    private String apiKey;
    
    @Value("${gemini.model}")
    private String model;
    
    @Value("${gemini.max-tokens:8192}")
    private int maxTokens;
    
    @Value("${gemini.temperature:0.4}")
    private double temperature;
    
    private final RestTemplate restTemplate;
    private final Gson gson;
    
    public GeminiService() {
        this.restTemplate = new RestTemplate();
        this.gson = new Gson();
    }
    
    /**
     * Processes an image to extract MCQ questions and generate answers using Gemini 2.5 Flash.
     * This replaces both OCR and question parsing in a single step.
     *
     * @param imageFile the image file to process
     * @return a list of Question objects with answers
     * @throws IOException if there's an error handling the file
     */
    public List<Question> processImageForQuestions(MultipartFile imageFile) throws IOException {
        logger.info("Processing image with Gemini 2.5 Flash: {}", imageFile.getOriginalFilename());
        
        // Convert image to Base64
        byte[] imageBytes = imageFile.getBytes();
        String base64Image = Base64.encodeBase64String(imageBytes);
        
        return processBase64ImageForQuestions(base64Image);
    }
    
    /**
     * Processes a base64-encoded image to extract MCQ questions and generate answers using Gemini 2.5 Flash.
     * This method is used for processing pasted images.
     *
     * @param base64Image the base64-encoded image data (without the data URL prefix)
     * @return a list of Question objects with answers
     * @throws IOException if there's an error handling the data
     */
    public List<Question> processBase64ImageForQuestions(String base64Image) throws IOException {
        logger.info("Processing base64 image with Gemini 2.5 Flash");
        
        // Create request payload
        JsonObject requestBody = createRequestPayload(base64Image);
        
        // Set up headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Create HTTP entity
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);
        
        // Make API call
        String apiUrl = String.format(GEMINI_API_URL, model, apiKey);
        String response = restTemplate.postForObject(apiUrl, requestEntity, String.class);
        
        // Parse response and extract questions
        return parseGeminiResponse(response);
    }
    
    /**
     * Creates the request payload for the Gemini 2.5 Flash API.
     *
     * @param base64Image the Base64-encoded image
     * @return the request payload as a JsonObject
     */
    private JsonObject createRequestPayload(String base64Image) {
        JsonObject requestBody = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        
        // Add text part (prompt)
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", "Analyze this image containing questions. " +
                "The questions could be multiple-choice questions (MCQs) or general questions without options. " +
                "Extract each question and provide the correct answer with a brief explanation. " +
                "Format your response as a JSON array with objects for each question. " +
                
                "For MCQ questions: " +
                "Include 'questionText', 'options' (as an array), 'answer' (just the letter), 'explanation', and set 'type' to 'MCQ'. " +
                "Example: [{\"type\":\"MCQ\",\"questionText\":\"What is 2+2?\",\"options\":[\"3\",\"4\",\"5\",\"6\"],\"answer\":\"B\",\"explanation\":\"2+2=4\"}] " +
                
                "For general questions without options: " +
                "Include 'questionText', 'answer' (the full answer text), 'explanation', and set 'type' to 'GENERAL'. " +
                "Example: [{\"type\":\"GENERAL\",\"questionText\":\"What is the capital of France?\",\"answer\":\"Paris\",\"explanation\":\"Paris is the capital and most populous city of France.\"}] " +
                
                "If you're not sure if it's an MCQ, process it as a general question.");
        parts.add(textPart);
        
        // Add image part
        JsonObject imagePart = new JsonObject();
        JsonObject inlineData = new JsonObject();
        inlineData.addProperty("mimeType", "image/jpeg");
        inlineData.addProperty("data", base64Image);
        imagePart.add("inlineData", inlineData);
        parts.add(imagePart);
        
        content.add("parts", parts);
        contents.add(content);
        requestBody.add("contents", contents);
        
        // Add generation config
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", temperature);
        generationConfig.addProperty("maxOutputTokens", maxTokens);
        requestBody.add("generationConfig", generationConfig);
        
        return requestBody;
    }
    
    /**
     * Parses the Gemini 2.5 Flash API response to extract questions.
     *
     * @param response the API response as a JSON string
     * @return a list of Question objects
     */
    private List<Question> parseGeminiResponse(String response) {
        List<Question> questions = new ArrayList<>();
        
        try {
            JsonObject responseJson = gson.fromJson(response, JsonObject.class);
            
            // Extract the text from the response
            String text = responseJson
                    .getAsJsonArray("candidates")
                    .get(0)
                    .getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0)
                    .getAsJsonObject()
                    .get("text")
                    .getAsString();
            
            // Find the JSON array in the text
            int startIndex = text.indexOf('[');
            int endIndex = text.lastIndexOf(']') + 1;
            
            if (startIndex >= 0 && endIndex > startIndex) {
                String jsonArrayString = text.substring(startIndex, endIndex);
                
                // Parse the JSON array
                JsonArray questionsArray = gson.fromJson(jsonArrayString, JsonArray.class);
                
                // Convert each JSON object to a Question
                for (int i = 0; i < questionsArray.size(); i++) {
                    JsonObject questionJson = questionsArray.get(i).getAsJsonObject();
                    
                    Question question = new Question();
                    question.setQuestionText(questionJson.get("questionText").getAsString());
                    
                    // Determine question type
                    String type = questionJson.has("type") ? 
                            questionJson.get("type").getAsString() : "MCQ"; // Default to MCQ for backward compatibility
                    
                    if ("GENERAL".equals(type)) {
                        // Set as general question
                        question.setQuestionType(Question.QuestionType.GENERAL);
                        question.setOptions(new ArrayList<>()); // Empty options list
                    } else {
                        // Set as MCQ question
                        question.setQuestionType(Question.QuestionType.MCQ);
                        
                        // Parse options
                        List<String> options = new ArrayList<>();
                        if (questionJson.has("options")) {
                            JsonArray optionsArray = questionJson.getAsJsonArray("options");
                            for (int j = 0; j < optionsArray.size(); j++) {
                                options.add(optionsArray.get(j).getAsString());
                            }
                        }
                        question.setOptions(options);
                    }
                    
                    // Set answer and explanation
                    question.setAnswer(questionJson.get("answer").getAsString());
                    
                    if (questionJson.has("explanation")) {
                        question.setExplanation(questionJson.get("explanation").getAsString());
                    } else {
                        question.setExplanation(""); // Default empty explanation if not provided
                    }
                    
                    questions.add(question);
                }
            } else {
                logger.error("Could not find JSON array in Gemini 2.5 Flash response");
            }
        } catch (Exception e) {
            logger.error("Error parsing Gemini 2.5 Flash response", e);
        }
        
        return questions;
    }
}