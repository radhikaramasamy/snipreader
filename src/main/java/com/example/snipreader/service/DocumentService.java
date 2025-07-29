package com.example.snipreader.service;

import com.example.snipreader.model.Question;
import com.example.snipreader.model.QuestionSet;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

/**
 * Service for generating document exports (PDF and Word) of question sets.
 */
@Service
public class DocumentService {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
    private static final Font HEADING_FONT = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
    private static final Font SUBHEADING_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
    
    /**
     * Generate a PDF document from a question set.
     *
     * @param questionSet the question set to generate a PDF from
     * @return a byte array containing the PDF document
     * @throws DocumentException if there is an error generating the PDF
     */
    public byte[] generatePdf(QuestionSet questionSet) throws DocumentException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        
        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();
            
            // Add title
            Paragraph title = new Paragraph(questionSet.getTitle(), TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            // Add date
            Paragraph date = new Paragraph("Created: " + 
                    questionSet.getCreatedAt().format(DATE_FORMATTER), NORMAL_FONT);
            date.setAlignment(Element.ALIGN_CENTER);
            document.add(date);
            
            document.add(Chunk.NEWLINE);
            
            // Add questions
            List<Question> questions = questionSet.getQuestions();
            for (int i = 0; i < questions.size(); i++) {
                Question question = questions.get(i);
                
                // Question number and text
                Paragraph questionPara = new Paragraph("Question " + (i + 1) + ": " + 
                        question.getQuestionText(), HEADING_FONT);
                document.add(questionPara);
                document.add(Chunk.NEWLINE);
                
                // Options - only for MCQ questions with options
                if (question.getQuestionType() == Question.QuestionType.MCQ && 
                        question.getOptions() != null && !question.getOptions().isEmpty()) {
                    document.add(new Paragraph("Options:", SUBHEADING_FONT));
                    List<String> options = question.getOptions();
                    for (int j = 0; j < options.size(); j++) {
                        char optionLetter = (char) ('A' + j);
                        String optionText = optionLetter + ") " + options.get(j);
                        
                        // Highlight correct answer
                        Font optionFont = NORMAL_FONT;
                        if (question.getAnswer() != null && 
                                question.getAnswer().contains(String.valueOf(optionLetter))) {
                            optionFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
                        }
                        
                        document.add(new Paragraph(optionText, optionFont));
                    }
                    
                    document.add(Chunk.NEWLINE);
                }
                
                // Answer and explanation
                document.add(new Paragraph("Answer:", SUBHEADING_FONT));
                document.add(new Paragraph(question.getAnswer(), NORMAL_FONT));
                document.add(Chunk.NEWLINE);
                
                document.add(new Paragraph("Explanation:", SUBHEADING_FONT));
                document.add(new Paragraph(question.getExplanation(), NORMAL_FONT));
                
                // Add space between questions
                if (i < questions.size() - 1) {
                    document.add(Chunk.NEWLINE);
                    document.add(new Paragraph("---------------------------------------------------"));
                    document.add(Chunk.NEWLINE);
                }
            }
            
        } finally {
            document.close();
        }
        
        return outputStream.toByteArray();
    }
    
    /**
     * Generate a Word document from a question set.
     *
     * @param questionSet the question set to generate a Word document from
     * @return a byte array containing the Word document
     * @throws IOException if there is an error generating the Word document
     */
    public byte[] generateWord(QuestionSet questionSet) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XWPFDocument document = new XWPFDocument();
        
        try {
            // Add title
            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setText(questionSet.getTitle());
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            
            // Add date
            XWPFParagraph dateParagraph = document.createParagraph();
            dateParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun dateRun = dateParagraph.createRun();
            dateRun.setText("Created: " + questionSet.getCreatedAt().format(DATE_FORMATTER));
            dateRun.setFontSize(10);
            
            // Add questions
            List<Question> questions = questionSet.getQuestions();
            for (int i = 0; i < questions.size(); i++) {
                Question question = questions.get(i);
                
                // Question number and text
                XWPFParagraph questionParagraph = document.createParagraph();
                XWPFRun questionRun = questionParagraph.createRun();
                questionRun.setText("Question " + (i + 1) + ": " + question.getQuestionText());
                questionRun.setBold(true);
                questionRun.setFontSize(14);
                questionRun.addBreak();
                
                // Options - only for MCQ questions with options
                if (question.getQuestionType() == Question.QuestionType.MCQ && 
                        question.getOptions() != null && !question.getOptions().isEmpty()) {
                    XWPFParagraph optionsHeadingParagraph = document.createParagraph();
                    XWPFRun optionsHeadingRun = optionsHeadingParagraph.createRun();
                    optionsHeadingRun.setText("Options:");
                    optionsHeadingRun.setBold(true);
                    optionsHeadingRun.setFontSize(12);
                    
                    List<String> options = question.getOptions();
                    for (int j = 0; j < options.size(); j++) {
                        char optionLetter = (char) ('A' + j);
                        String optionText = optionLetter + ") " + options.get(j);
                        
                        XWPFParagraph optionParagraph = document.createParagraph();
                        XWPFRun optionRun = optionParagraph.createRun();
                        optionRun.setText(optionText);
                        
                        // Highlight correct answer
                        if (question.getAnswer() != null && 
                                question.getAnswer().contains(String.valueOf(optionLetter))) {
                            optionRun.setBold(true);
                        }
                    }
                }
                
                // Answer
                XWPFParagraph answerHeadingParagraph = document.createParagraph();
                XWPFRun answerHeadingRun = answerHeadingParagraph.createRun();
                answerHeadingRun.setText("Answer:");
                answerHeadingRun.setBold(true);
                answerHeadingRun.setFontSize(12);
                
                XWPFParagraph answerParagraph = document.createParagraph();
                XWPFRun answerRun = answerParagraph.createRun();
                answerRun.setText(question.getAnswer());
                
                // Explanation
                XWPFParagraph explanationHeadingParagraph = document.createParagraph();
                XWPFRun explanationHeadingRun = explanationHeadingParagraph.createRun();
                explanationHeadingRun.setText("Explanation:");
                explanationHeadingRun.setBold(true);
                explanationHeadingRun.setFontSize(12);
                
                XWPFParagraph explanationParagraph = document.createParagraph();
                XWPFRun explanationRun = explanationParagraph.createRun();
                explanationRun.setText(question.getExplanation());
                
                // Add separator between questions
                if (i < questions.size() - 1) {
                    XWPFParagraph separatorParagraph = document.createParagraph();
                    XWPFRun separatorRun = separatorParagraph.createRun();
                    separatorRun.setText("---------------------------------------------------");
                    separatorRun.addBreak();
                }
            }
            
            document.write(outputStream);
        } finally {
            document.close();
        }
        
        return outputStream.toByteArray();
    }
    
    /**
     * Generate a PDF document from multiple question sets.
     *
     * @param questionSets the collection of question sets to generate a PDF from
     * @return a byte array containing the PDF document
     * @throws DocumentException if there is an error generating the PDF
     */
    public byte[] generatePdfFromMultiple(Collection<QuestionSet> questionSets) throws DocumentException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        
        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();
            
            // Add title
            Paragraph title = new Paragraph("Selected Question Sets", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            document.add(Chunk.NEWLINE);
            
            int setCounter = 1;
            // Process each question set
            for (QuestionSet questionSet : questionSets) {
                // Add question set title and date
                Paragraph setTitle = new Paragraph("Set " + setCounter + ": " + questionSet.getTitle(), HEADING_FONT);
                setTitle.setAlignment(Element.ALIGN_LEFT);
                document.add(setTitle);
                
                Paragraph date = new Paragraph("Created: " + 
                        questionSet.getCreatedAt().format(DATE_FORMATTER), NORMAL_FONT);
                document.add(date);
                
                document.add(Chunk.NEWLINE);
                
                // Add questions
                List<Question> questions = questionSet.getQuestions();
                for (int i = 0; i < questions.size(); i++) {
                    Question question = questions.get(i);
                    
                    // Question number and text
                    Paragraph questionPara = new Paragraph("Question " + (i + 1) + ": " + 
                            question.getQuestionText(), SUBHEADING_FONT);
                    document.add(questionPara);
                    document.add(Chunk.NEWLINE);
                    
                    // Options - only for MCQ questions with options
                    if (question.getQuestionType() == Question.QuestionType.MCQ && 
                            question.getOptions() != null && !question.getOptions().isEmpty()) {
                        document.add(new Paragraph("Options:", SUBHEADING_FONT));
                        List<String> options = question.getOptions();
                        for (int j = 0; j < options.size(); j++) {
                            char optionLetter = (char) ('A' + j);
                            String optionText = optionLetter + ") " + options.get(j);
                            
                            // Highlight correct answer
                            Font optionFont = NORMAL_FONT;
                            if (question.getAnswer() != null && 
                                    question.getAnswer().contains(String.valueOf(optionLetter))) {
                                optionFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
                            }
                            
                            document.add(new Paragraph(optionText, optionFont));
                        }
                        
                        document.add(Chunk.NEWLINE);
                    }
                    
                    // Answer and explanation
                    document.add(new Paragraph("Answer:", SUBHEADING_FONT));
                    document.add(new Paragraph(question.getAnswer(), NORMAL_FONT));
                    document.add(Chunk.NEWLINE);
                    
                    document.add(new Paragraph("Explanation:", SUBHEADING_FONT));
                    document.add(new Paragraph(question.getExplanation(), NORMAL_FONT));
                    
                    // Add space between questions
                    if (i < questions.size() - 1) {
                        document.add(Chunk.NEWLINE);
                        document.add(new Paragraph("---------------------------------------------------"));
                        document.add(Chunk.NEWLINE);
                    }
                }
                
                // Add separator between question sets
                if (setCounter < questionSets.size()) {
                    document.add(Chunk.NEWLINE);
                    document.add(new Paragraph("=================================================="));
                    document.add(Chunk.NEWLINE);
                }
                
                setCounter++;
            }
            
        } finally {
            document.close();
        }
        
        return outputStream.toByteArray();
    }
    
    /**
     * Generate a Word document from multiple question sets.
     *
     * @param questionSets the collection of question sets to generate a Word document from
     * @return a byte array containing the Word document
     * @throws IOException if there is an error generating the Word document
     */
    public byte[] generateWordFromMultiple(Collection<QuestionSet> questionSets) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XWPFDocument document = new XWPFDocument();
        
        try {
            // Add title
            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setText("Selected Question Sets");
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.addBreak();
            
            int setCounter = 1;
            // Process each question set
            for (QuestionSet questionSet : questionSets) {
                // Add question set title and date
                XWPFParagraph setTitleParagraph = document.createParagraph();
                XWPFRun setTitleRun = setTitleParagraph.createRun();
                setTitleRun.setText("Set " + setCounter + ": " + questionSet.getTitle());
                setTitleRun.setBold(true);
                setTitleRun.setFontSize(16);
                
                XWPFParagraph dateParagraph = document.createParagraph();
                XWPFRun dateRun = dateParagraph.createRun();
                dateRun.setText("Created: " + questionSet.getCreatedAt().format(DATE_FORMATTER));
                dateRun.setFontSize(10);
                dateRun.addBreak();
                
                // Add questions
                List<Question> questions = questionSet.getQuestions();
                for (int i = 0; i < questions.size(); i++) {
                    Question question = questions.get(i);
                    
                    // Question number and text
                    XWPFParagraph questionParagraph = document.createParagraph();
                    XWPFRun questionRun = questionParagraph.createRun();
                    questionRun.setText("Question " + (i + 1) + ": " + question.getQuestionText());
                    questionRun.setBold(true);
                    questionRun.setFontSize(14);
                    questionRun.addBreak();
                    
                    // Options - only for MCQ questions with options
                    if (question.getQuestionType() == Question.QuestionType.MCQ && 
                            question.getOptions() != null && !question.getOptions().isEmpty()) {
                        XWPFParagraph optionsHeadingParagraph = document.createParagraph();
                        XWPFRun optionsHeadingRun = optionsHeadingParagraph.createRun();
                        optionsHeadingRun.setText("Options:");
                        optionsHeadingRun.setBold(true);
                        optionsHeadingRun.setFontSize(12);
                        
                        List<String> options = question.getOptions();
                        for (int j = 0; j < options.size(); j++) {
                            char optionLetter = (char) ('A' + j);
                            String optionText = optionLetter + ") " + options.get(j);
                            
                            XWPFParagraph optionParagraph = document.createParagraph();
                            XWPFRun optionRun = optionParagraph.createRun();
                            optionRun.setText(optionText);
                            optionRun.setFontSize(10);
                            
                            // Highlight correct answer
                            if (question.getAnswer() != null && 
                                    question.getAnswer().contains(String.valueOf(optionLetter))) {
                                optionRun.setBold(true);
                            }
                        }
                    }
                    
                    // Answer
                    XWPFParagraph answerHeadingParagraph = document.createParagraph();
                    XWPFRun answerHeadingRun = answerHeadingParagraph.createRun();
                    answerHeadingRun.setText("Answer:");
                    answerHeadingRun.setBold(true);
                    answerHeadingRun.setFontSize(12);
                    
                    XWPFParagraph answerParagraph = document.createParagraph();
                    XWPFRun answerRun = answerParagraph.createRun();
                    answerRun.setText(question.getAnswer());
                    answerRun.setFontSize(10);
                    
                    // Explanation
                    XWPFParagraph explanationHeadingParagraph = document.createParagraph();
                    XWPFRun explanationHeadingRun = explanationHeadingParagraph.createRun();
                    explanationHeadingRun.setText("Explanation:");
                    explanationHeadingRun.setBold(true);
                    explanationHeadingRun.setFontSize(12);
                    
                    XWPFParagraph explanationParagraph = document.createParagraph();
                    XWPFRun explanationRun = explanationParagraph.createRun();
                    explanationRun.setText(question.getExplanation());
                    explanationRun.setFontSize(10);
                    
                    // Add separator between questions
                    if (i < questions.size() - 1) {
                        XWPFParagraph separatorParagraph = document.createParagraph();
                        XWPFRun separatorRun = separatorParagraph.createRun();
                        separatorRun.setText("---------------------------------------------------");
                        separatorRun.addBreak();
                    }
                }
                
                // Add separator between question sets
                if (setCounter < questionSets.size()) {
                    XWPFParagraph setSeparatorParagraph = document.createParagraph();
                    XWPFRun setSeparatorRun = setSeparatorParagraph.createRun();
                    setSeparatorRun.setText("==================================================");
                    setSeparatorRun.addBreak();
                    setSeparatorRun.addBreak();
                }
                
                setCounter++;
            }
            
            document.write(outputStream);
        } finally {
            document.close();
        }
        
        return outputStream.toByteArray();
    }
}