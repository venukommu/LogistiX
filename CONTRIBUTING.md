# Contributing to LogistiX

Thank you for your interest in contributing to **LogistiX**!

LogistiX is an open-source Decision Intelligence Platform designed to be extensible, reliable, and explainable. We welcome contributions from developers, architects, and researchers.

---

## 📜 Code of Conduct & Framework Constitution

Before contributing, please read our foundational guidelines:
1. [Framework Constitution](docs/CONSTITUTION.md): Enshrines our 10 engineering principles (Explainability before Intelligence, Rules before AI, Execution Strategy Agnostic).
2. [API Stability Matrix](docs/API_STABILITY.md): Details our public API stability tiers and deprecation policies.

---

## 🛠️ Development Setup

### Prerequisites
- **JDK 21** or later (OpenJDK, Temurin, or GraalVM).
- **Apache Maven 3.9+**.
- **Docker** and Docker Compose (for integration testing with PostgreSQL 17 + pgvector).

### Building Locally
```bash
git clone https://github.com/venukommu/LogistiX.git
cd LogistiX/backend
mvn clean test-compile
```

---

## 📐 Contribution Guidelines

### 1. Hexagonal Purity & Domain Isolation
- `logistix-domain` and `logistix-common` must **never** import Spring, Spring Boot, or AI SDKs.
- Keep domain models immutable (`record`) with explicit validation using `DomainAssertions`.

### 2. Decision Intelligence Model Principles
- When adding new node types or strategies, implement the contracts in `logistix-model` without embedding domain-specific logic.

### 3. Public API Changes
- Do not modify stable interfaces in `logistix-dsl` without opening an Architectural RFC issue first.

---

## 🔀 Pull Request Process

1. Fork the repository and create your branch from `master`:
   ```bash
   git checkout -b feat/your-feature-name
   ```
2. Ensure code compiles cleanly with `mvn clean test-compile`.
3. Adhere to Google Java Code Style conventions.
4. Submit a Pull Request targeting the `master` branch using the provided PR template.

---

## 📄 License
By contributing to LogistiX, you agree that your contributions will be licensed under the [Apache License, Version 2.0](LICENSE).
