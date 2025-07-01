# spring-ai-chat-rag
A Spring Boot application leveraging OpenAI/Ollama embeddings and PostgreSQL vector store for efficient document retrieval and question answering through RAG (Retrieval Augmented Generation). Enables document uploads, semantic search, and context-aware responses.

# install

podman pull pgvector/pgvector:pg17
podman run -d \
  --name postgres-pgvector \
  -p 5432:5432 \
  -e POSTGRES_USER=pguser \
  -e POSTGRES_PASSWORD=secret \
  -e POSTGRES_DB=vectordb \
  pgvector/pgvector:pg17

## German Optimizations

This project is preconfigured to work well with German documents. It uses a German embedding model (`jina-embeddings-v2-base-de`) and the prompts are written in German. Page numbers like `Seite 1` and common footnote patterns are removed during preprocessing. Hyphenation across line breaks is also fixed to improve chunk quality.
