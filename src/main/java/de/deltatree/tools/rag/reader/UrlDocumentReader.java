package de.deltatree.tools.rag.reader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.document.DocumentReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.*;


public class UrlDocumentReader implements DocumentReader {
    private static final Logger LOG = LoggerFactory.getLogger(UrlDocumentReader.class);
    private final String url;
    private final int timeout;

    public UrlDocumentReader(String url) {
        this(url, 30000);
    }

    public UrlDocumentReader(String url, int timeout) {
        this.url = url;
        this.timeout = timeout;
    }

    @Override
    public List<org.springframework.ai.document.Document> get() {
        try {
            LOG.info("Fetching content from URL: {}", url);
            Document jsoupDoc = Jsoup.connect(url)
                    .timeout(timeout)
                    .get();

            jsoupDoc.select("script, style, iframe, noscript").remove();
            String content = jsoupDoc.text();

            String title = jsoupDoc.title();

            Map<String, Object> metadata = Map.of(
                    "source", url,
                    "title", title,
                    "type", "url"
            );

            LOG.info("Successfully fetched content from URL: {}, title: {}, content length: {} chars",
                    url, title, content.length());

            return List.of(new org.springframework.ai.document.Document(content, metadata));
        }
        catch (IOException e) {
            LOG.error("Failed to fetch URL content: {}", url, e);
            return Collections.emptyList();
        }
    }
}