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
