package de.deltatree.tools.rag.controller;

import de.deltatree.tools.rag.model.Answer;
import de.deltatree.tools.rag.model.Question;
import de.deltatree.tools.rag.service.OllamaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import de.deltatree.tools.rag.vectorstore.PostgresVectorStore;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/chat")
public class ChatController {
    private static final Logger LOG = LoggerFactory.getLogger(ChatController.class);
    private final PostgresVectorStore vectorStore;
    private final OllamaService ollamaService;

    public ChatController(PostgresVectorStore vectorStore, OllamaService ollamaService) {
        this.vectorStore = vectorStore;
        this.ollamaService = ollamaService;
        LOG.info("ChatController initialized successfully");
    }

    @PostMapping
    public Answer chat(@RequestBody Question question, Authentication user) {
        LOG.info("Received question: {}", question.getQuestion());

        // Handle simple greetings without context
        if (isGreeting(question.getQuestion())) {
            return new Answer("Hello! I'm here to help you with questions about the documents in my knowledge base. What would you like to know?");
        }

        // 1. Retrieve relevant documents
        List<Document> retrievedDocs = vectorStore.similaritySearch(
                SearchRequest.query(question.getQuestion())
                        .withTopK(20) // fetch more in case of duplicates
                        .withSimilarityThreshold(0.38)

        );

        LOG.info("Retrieved {} documents from vector store", retrievedDocs.size());

        // Remove duplicate chunks and limit to top 8
        List<Document> documents = deduplicateDocuments(retrievedDocs, 8);

        // 2. Check if we have relevant context AND if it's actually related to the question
        if (documents.isEmpty()) {
            long count = vectorStore.getDocumentCount();
            if (count == 0) {
                return new Answer("My knowledge base is empty. Please upload documents before asking questions.");
            }
            return new Answer("I don't have information about that topic in my knowledge base. Please try rephrasing your question or check if relevant documents have been uploaded.");
        }

        // 3. Quick relevance check - if the question seems completely unrelated to document content
        if (isGeneralKnowledgeQuestion(question.getQuestion(), documents)) {
            return new Answer("I don't have information about that in my knowledge base. Please ask questions related to the uploaded documents.");
        }

        // 3. Format the context with source information
        StringBuilder contextBuilder = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            String filename = doc.getMetadata().getOrDefault("filename", "unknown").toString();
//            contextBuilder.append(String.format("Source %d (from %s):\n%s\n\n",
//                    i + 1, filename, doc.getContent()));
            contextBuilder.append(String.format("From %s:\n%s\n\n",
                    filename, doc.getContent()));
//            String cleanFilename = filename.replace(".pdf", "").replace(".md", "").replace("-", " ");
//            contextBuilder.append(String.format("From %s:\n%s\n\n", cleanFilename, doc.getContent()));
        }

        String context = contextBuilder.toString();
        LOG.info("Context length: {} characters from {} sources", context.length(), documents.size());

        // 4. Create an improved prompt
        String prompt = createImprovedPrompt(context, question.getQuestion());

        // 5. Get response from Ollama
        String response = ollamaService.generateResponse(prompt);

        // 6. Post-process the response
        String finalResponse = postProcessResponse(response, documents);

        return new Answer(finalResponse);
    }

    private boolean isGreeting(String text) {
        String lowerText = text.toLowerCase().trim();
        return lowerText.matches("^(hi|hello|hey|good morning|good afternoon|good evening|how are you|what's up|greetings|hallo|guten morgen|guten tag|guten abend|servus|moin|gr\u00fc\u00df gott).*");
    }

    private boolean isGeneralKnowledgeQuestion(String question, List<Document> documents) {
        String lowerQuestion = question.toLowerCase();

        // Common patterns for general knowledge questions
        String[] generalPatterns = {
                "how tall is", "how high is", "what is the height of",
                "when was.*born", "when did.*die", "who invented",
                "what is the capital of", "what is the population of",
                "how far is", "what time is it", "what's the weather",
                "who is the president", "who is the ceo of.*(?!.*" + getDocumentTopics(documents) + ")",
                "what year did", "how old is", "what color is",
                "recipe for", "how to cook", "lyrics to",
                // German equivalents
                "wie hoch ist", "wann wurde.*geboren", "wann ist.*gestorben",
                "wer hat.*erfunden", "was ist die hauptstadt von",
                "wie weit ist", "wie ist das wetter", "wer ist der pr.sident",
                "wer ist der ceo von.*(?!.*" + getDocumentTopics(documents) + ")",
                "in welchem jahr", "wie alt ist"
        };

        for (String pattern : generalPatterns) {
            if (lowerQuestion.matches(".*" + pattern + ".*")) {
                // Double-check: if the documents actually contain related content, allow it
                if (documentsContainRelevantTerms(question, documents)) {
                    return false; // Not a general knowledge question if our docs have related content
                }
                LOG.info("Detected general knowledge question: {}", question);
                return true;
            }
        }

        return false;
    }

    private String getDocumentTopics(List<Document> documents) {
        // Extract key terms from documents to help identify if question might be relevant
        return documents.stream()
                .map(doc -> doc.getMetadata().getOrDefault("filename", "").toString())
                .collect(Collectors.joining("|"));
    }

    private boolean documentsContainRelevantTerms(String question, List<Document> documents) {
        String[] questionWords = question.toLowerCase().split("\\s+");

        // Check if any significant words from the question appear in the documents
        for (Document doc : documents) {
            String content = doc.getContent().toLowerCase();
            long matchingWords = 0;

            for (String word : questionWords) {
                if (word.length() > 3 && content.contains(word)) { // Only check significant words
                    matchingWords++;
                }
            }

            // If more than 20% of significant words match, consider it potentially relevant
            if (matchingWords > questionWords.length * 0.2) {
                return true;
            }
        }

        return false;
    }

    private String createImprovedPrompt(String context, String question) {
        return String.format("""
            Du bist ein hilfsbereiter KI-Assistent und beantwortest Fragen ausschließlich auf Basis des folgenden Dokumentenkontexts. Externes Wissen darfst du nicht verwenden.

            **WICHTIGE REGELN:**
            1. **Nur der bereitgestellte Kontext**: Antworte nur mit Informationen, die im Kontext unten vorkommen.
            2. **Kein externes Wissen**: Beantworte keine allgemeinen Themen oder Aktuelles außerhalb des Kontexts.
            3. **Sei strikt**: Falls die Antwort nicht im Kontext steht, sage "Dazu habe ich keine Informationen in meiner Wissensbasis."
            4. **Quellen nennen**: Wenn du passende Informationen findest, gib an, aus welchem Dokument sie stammen.
            5. **Hilfreich bleiben**: Enthält der Kontext relevante Informationen, liefere eine ausführliche Antwort.

            **ANTWORTREGELN:**
            - Enthält der Kontext relevante Informationen, gib eine umfassende Antwort mit Quellenangaben.
            - Deckt der Kontext die Frage nur teilweise ab, beantworte nur, was enthalten ist, und verweise auf fehlende Informationen.
            - Ist kein passender Kontext vorhanden, antworte: "Dazu habe ich keine Informationen in meiner Wissensbasis. Bitte stelle Fragen zu den hochgeladenen Dokumenten."

            **BEISPIELE, WAS NICHT BEANTWORTET WIRD:**
            - Allgemeinwissen (z.B. Höhe von Bergen, historische Daten, sofern nicht im Dokument enthalten)
            - Aktuelle Ereignisse, die nicht im Dokument stehen
            - Mathematische Berechnungen ohne Bezug zum Dokument
            - Persönliche Ratschläge oder Meinungen

            **KONTEXT AUS DEN DOKUMENTEN:**
            %s

            **NUTZERFRAGE:** %s

            **DEINE ANTWORT:**
            """, context, question);
    }

    private String postProcessResponse(String response, List<Document> sources) {
        // If response seems incomplete, add helpful information about sources
        if (response.toLowerCase().contains("don't contain") ||
                response.toLowerCase().contains("not available") ||
                response.toLowerCase().contains("don't have")) {

            StringBuilder sourceInfo = new StringBuilder("\n\nAvailable documents in knowledge base:\n");
            sources.stream()
                    .map(doc -> doc.getMetadata().getOrDefault("filename", "unknown").toString())
                    .distinct()
                    .forEach(filename -> sourceInfo.append("• ").append(filename).append("\n"));

            return response + sourceInfo.toString();
        }

        return response;
    }

    private List<Document> deduplicateDocuments(List<Document> docs, int max) {
        Set<String> seen = new HashSet<>();
        List<Document> unique = new ArrayList<>();
        for (Document doc : docs) {
            if (seen.add(doc.getContent())) {
                unique.add(doc);
                if (unique.size() >= max) {
                    break;
                }
            }
        }
        LOG.info("After deduplication {} documents remain", unique.size());
        return unique;
    }
}

//old code
////package de.deltatree.tools.rag.controller;
////
////import de.deltatree.tools.rag.model.Answer;
////import de.deltatree.tools.rag.model.Question;
////import org.slf4j.Logger;
////import org.slf4j.LoggerFactory;
////import org.springframework.ai.chat.client.ChatClient;
////import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
////import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
////import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
////import org.springframework.ai.chat.memory.InMemoryChatMemory;
////import org.springframework.ai.vectorstore.SearchRequest;
////import org.springframework.ai.vectorstore.VectorStore;
////import org.springframework.security.core.Authentication;
////import org.springframework.web.bind.annotation.*;
////@RestController
////@RequestMapping("/chat")
////public class ChatController {
////    private static final Logger LOG = LoggerFactory.getLogger(ChatController.class);
////    private final ChatClient chatClient;
////    private static final String CHAT_MEMORY_CONVERSATION_ID_KEY = "conversationId";
////
////    public ChatController(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
////        LOG.info("Initializing ChatController");
////        this.chatClient = chatClientBuilder
////                .defaultAdvisors(
////                        new SimpleLoggerAdvisor(),
////                        new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults().withTopK(5)
////                                .withSimilarityThreshold(0.5)), // Lower threshold),  // Get top 5 matches
////                        new PromptChatMemoryAdvisor(new InMemoryChatMemory())
////                )
////                .build();
////        LOG.info("ChatClient initialized successfully");
////    }
////
////    @PostMapping
////    public Answer chat(@RequestBody Question question, Authentication user) {
////        LOG.info("Received question: {}", question.getQuestion());
////        Answer answer = chatClient.prompt()
////                .user(question.getQuestion())
////                .advisors(
////                        advisorSpec -> advisorSpec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, user.getPrincipal())
////                )
////                .call()
////                .entity(Answer.class);
////        LOG.info("Generated answer: {}", answer.getAnswer());
////        return answer;
////    }
////}
//package de.deltatree.tools.rag.controller;
//
//import de.deltatree.tools.rag.model.Answer;
//import de.deltatree.tools.rag.model.Question;
//import de.deltatree.tools.rag.service.OllamaService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.vectorstore.SearchRequest;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.security.core.Authentication;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@RestController
//@RequestMapping("/chat")
//public class ChatController {
//    private static final Logger LOG = LoggerFactory.getLogger(ChatController.class);
//    private final VectorStore vectorStore;
//    private final OllamaService ollamaService;
//
//    public ChatController(VectorStore vectorStore, OllamaService ollamaService) {
//        this.vectorStore = vectorStore;
//        this.ollamaService = ollamaService;
//        LOG.info("ChatController initialized successfully");
//    }
//
//    @PostMapping
//    public Answer chat(@RequestBody Question question, Authentication user) {
//        LOG.info("Received question: {}", question.getQuestion());
//
//        // 1. Retrieve relevant documents
//        List<Document> documents = vectorStore.similaritySearch(
//                SearchRequest.query(question.getQuestion()).withTopK(10)
//        );
//
//        // 2. Format the context
//        String context = documents.stream()
//                .map(Document::getContent)
//                .collect(Collectors.joining("\n\n"));
//
//        LOG.info("Retrieved context: {} characters", context.length());
//
//        // 3. Create prompt with context
//
//        //using this prompt
//        String prompt=String.format("""
//      You are an AI assistant that ONLY answers questions based on the provided context information.
//
//    IMPORTANT INSTRUCTIONS:
//    - If the answer is not contained in the context except for greeting messages such as hi, how are you doing etc, respond with ONLY: "I don't have information about that in my knowledge base."
//    - If the user says "hi" or greets you, respond EXACTLY: "Hello! I can help you find information from the uploaded documents. What would you like to know?"
//    - Do not use any knowledge outside of the provided context
//    - Do not make up or infer information not explicitly stated in the context
//
//    Context:
//    %s
//
//    Question: %s
//
//    Answer (based ONLY on the context above):
//    """, context, question.getQuestion());
//        String enhancedPrompt = String.format("""
//SYSTEM: You must follow these rules exactly. No exceptions.
//
//RULE 1: If user says "hi", "hello", or any greeting, respond ONLY with: "Hello! I can help you find information from the uploaded documents. What would you like to know?"
//
//RULE 2: If the question cannot be answered from the context below, respond ONLY with: "I don't have information about that in my knowledge base."
//
//RULE 3: Never explain what the context contains. Never mention document topics unless directly asked.
//
//RULE 4: Only answer if the exact information is in the context below.
//
//Context: %s
//
//User Question: %s
//
//Response:""", context, question.getQuestion());
//
//        // 4. Get response from Ollama
//        String response = ollamaService.generateResponse(enhancedPrompt);
//        System.out.println("Context length: {} characters"+context.length());
//        return new Answer(response);
//    }
//}