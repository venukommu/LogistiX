# ADR 002: Using pgvector for RAG and Vector Storage

## Status
Accepted

## Context
LogistiX requires semantic search over carrier compliance manuals, freight contracts, routing regulations, and dispatcher notes. Operating a standalone specialized vector database (e.g., Pinecone, Milvus, Qdrant) introduces additional operational overhead for open-source self-hosted deployments.

## Decision
We adopt `pgvector` as the primary vector storage engine within PostgreSQL:
- Co-locates relational transactional tables (shipments, drivers, routes) with high-dimensional vector embeddings in the same database engine.
- Leverages ACID transactions and PostgreSQL indexing (HNSW, IVFFlat).
- Supported out-of-the-box via Docker Compose and Spring AI pgvector driver.

## Consequences
- **Positive**: Single database to manage, back up, and deploy for self-hosted instances.
- **Positive**: Enables joint relational + vector queries (e.g., filter drivers within a geo-radius and rank by semantic matching score).
- **Negative**: Dedicated vector engines might offer higher scale for billions of vectors; however, for typical logistics platform requirements, pgvector provides performance with minimal operational complexity.
