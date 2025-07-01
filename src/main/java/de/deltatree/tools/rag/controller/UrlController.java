package de.deltatree.tools.rag.controller;

import de.deltatree.tools.rag.model.UrlRequest;
import de.deltatree.tools.rag.reader.UrlDocumentReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class UrlController {
    private static final Logger LOG = LoggerFactory.getLogger(UrlController.class);
    private final VectorStore vectorStore;

    public UrlController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostMapping("/process-url")
    public ResponseEntity<?> processUrl(@RequestBody UrlRequest urlRequest) {
        String urlString = urlRequest.getUrl();
        LOG.info("Processing URL: {}", urlString);

        // Validate URL
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            LOG.error("Invalid URL: {}", urlString, e);
            Map<String, String> response = new HashMap<>();
            response.put("error", "Invalid URL format");
            return ResponseEntity.badRequest().body(response);
        }

        // Fetch content from URL
        UrlDocumentReader documentReader = new UrlDocumentReader(urlString);
        List<Document> documents = documentReader.get();

        if (documents.isEmpty()) {
            LOG.error("Failed to fetch content from URL: {}", urlString);
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to fetch content from the provided URL");
            return ResponseEntity.badRequest().body(response);
        }

        // Split documents
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> splitDocuments = splitter.apply(documents);
        LOG.info("URL content split into {} chunks", splitDocuments.size());

        // Add to vector store
        vectorStore.add(splitDocuments);
        LOG.info("All URL chunks added to vector store successfully");

        Map<String, Object> response = new HashMap<>();
        response.put("url", urlString);
        response.put("title", documents.get(0).getMetadata().getOrDefault("title", "Unknown"));
        response.put("chunkCount", splitDocuments.size());

        return ResponseEntity.ok(response);
    }

}