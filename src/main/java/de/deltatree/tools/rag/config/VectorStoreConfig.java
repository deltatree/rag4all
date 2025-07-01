package de.deltatree.tools.rag.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import de.deltatree.tools.rag.repository.DocumentEmbeddingRepository;
import de.deltatree.tools.rag.vectorstore.PostgresVectorStore;

@Configuration
public class VectorStoreConfig {

    @Bean
    PostgresVectorStore vectorStore(
            @Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel,
            DocumentEmbeddingRepository repository) {
        System.out.println("Creating Ollama VectorStore");
        return new PostgresVectorStore(repository, embeddingModel);
    }
}
