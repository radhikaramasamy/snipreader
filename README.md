# MCQ Snip Reader

A Spring Boot application that extracts multiple-choice questions from screenshots and provides AI-generated answers using Google's Gemini AI.

## Features

- Paste screenshots directly from clipboard or upload image files
- Process images directly with Google's Gemini 2.5 Flash AI in "flashmode"
- Extract questions, options, and generate answers in a single step
- Display results with explanations
- Clean and responsive user interface with Thymeleaf

## Prerequisites

- Java 17 or higher
- Gradle
- Google Gemini API key (for image processing and answer generation)

## Setup

### 1. Clone the repository

```bash
git clone https://github.com/yourusername/snipreader.git
cd snipreader
```

### 2. Configure API Keys

Edit `src/main/resources/application.properties` and replace the API key placeholder with your actual key:

```properties
# Google Gemini API key
gemini.api.key=your-gemini-api-key-here
```

You can get a Gemini API key from the [Google AI Studio](https://makersuite.google.com/app/apikey).

### 3. Build the application

```bash
./gradlew build
```

### 5. Run the application

```bash
./gradlew bootRun
```

The application will be available at `http://localhost:8080`

## Usage

1. Open your browser and navigate to `http://localhost:8080`
2. Take a screenshot of MCQ questions
3. Choose one of the following methods:
   - **Paste directly**: Copy the screenshot to your clipboard (Ctrl+C or Print Screen), then paste it (Ctrl+V) into the paste area
   - **Upload file**: Click the "Upload File" tab and select the screenshot file using the file picker
4. Wait for the processing to complete
5. View the extracted questions and AI-generated answers

## Technical Details

### Architecture

- **Frontend**: Thymeleaf templates with Bootstrap for styling
- **Backend**: Spring Boot application
- **AI**: Google Gemini 2.5 Flash API for image processing and answer generation

### Key Components

- **GeminiService**: Processes images directly with Google's Gemini 2.5 Flash AI to extract questions and generate answers in one step
- **MCQController**: Handles HTTP requests and coordinates the services
- **Question Model**: Represents MCQ questions with their options, answers, and explanations

## Limitations

- AI-generated answers may not always be correct
- Performance depends on the quality of the screenshot
- Requires an active internet connection to access the Gemini API
- The Gemini API has rate limits and usage quotas

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Google Gemini AI](https://ai.google.dev/gemini-api)
- [Bootstrap](https://getbootstrap.com/)