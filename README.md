# PayFlow AI

AI-powered payment analyst built on **Spring AI 1.0** and **Tanzu Platform GenAI**.

Two capabilities in one app:

| Feature | Model | Description |
|---|---|---|
| **Chat Analyst** | `gpt-oss:20b` | Conversational AI with tool calling — queries transaction data, explains fraud rules, generates investigation narratives |
| **Semantic Search** | `nomic-embed-text-v2-moe` | Finds transactions by meaning using vector embeddings — not keyword matching |

Deploys as a single jar to Tanzu Application Service (TAS). React frontend embedded in the Spring Boot jar.

---

## Features

### Chat Analyst
- Per-session conversation memory (asks follow-up questions, remembers context)
- Six LLM-callable tools:
  - `getRecentTransactions` — live transaction feed with risk scores
  - `getPaymentStatistics` — 24h volume, fraud rate, flagged count
  - `getFraudAlerts` — current high-risk alerts with rule/amount/risk
  - `explainFraudDetectionRules` — documents the four detection algorithms
  - `getAccountBalances` — current balances for all six demo banks
  - `generateFraudNarrative` — detailed investigation report for a transaction
- Suggested questions and one-click topic starters

### Semantic Search
- 20 synthetic transaction documents seeded at startup
- Natural language queries — "suspicious late night large payment", "split payment structuring"
- Results show similarity score, risk badge, and status badge
- Powered by in-memory `SimpleVectorStore` — no external vector DB required

---

## Demo banks (fictional)

Albion Bank PLC · Meridian Bank PLC · Crestfield Group PLC · Harrington PLC · Caledonian Bank · Vantage Bank PLC

---

## Running locally

Requires Java 21, Node 18+, and a local OpenAI-compatible LLM (e.g. [Ollama](https://ollama.ai/)).

```bash
# Terminal 1 — backend
export AI_BASE_URL=http://localhost:11434/v1
export AI_CHAT_MODEL=llama3.2
export AI_EMBEDDING_MODEL=nomic-embed-text
cd backend
mvn spring-boot:run

# Terminal 2 — frontend (dev mode with hot reload)
cd frontend
npm install
npm run dev
# Open http://localhost:5174
```

---

## Deploy to Cloud Foundry / Tanzu Application Service

```bash
# 1. Build (bundles React into the jar)
./build.sh

# 2. Set credentials (do NOT put the API key in manifest.yml)
cf set-env payflow-ai AI_BASE_URL  https://<your-genai-endpoint>/v1
cf set-env payflow-ai AI_API_KEY   <your-api-key>

# 3. Push
cf push

# 4. Open
cf app payflow-ai   # shows the route
```

The manifest uses `gpt-oss:20b` and `nomic-embed-text-v2-moe` by default. Override any model via env vars.

---

## Architecture

```
Browser
  └── React SPA (served by Spring Boot from /static/)
        ├── POST /api/chat      → ChatController
        │     └── ChatClient (Spring AI)
        │           ├── gpt-oss:20b  (OpenAI-compatible endpoint)
        │           ├── InMemoryChatMemory  (per sessionId)
        │           └── PaymentDataTools  (6 @Tool methods)
        └── GET  /api/search?q= → SearchController
              └── SemanticSearchService
                    ├── SimpleVectorStore  (in-memory)
                    └── nomic-embed-text-v2-moe  (embeddings)
```

No external databases or message queues — the only runtime dependency is the Tanzu Platform AI endpoint.

---

## Configuration

All via environment variables:

| Variable | Default | Description |
|---|---|---|
| `AI_BASE_URL` | `http://localhost:11434/v1` | OpenAI-compatible LLM base URL |
| `AI_API_KEY` | `local` | API key (set via `cf set-env`, never in manifest) |
| `AI_CHAT_MODEL` | `gpt-oss:20b` | Chat + tool-calling model name |
| `AI_EMBEDDING_MODEL` | `nomic-embed-text-v2-moe` | Embedding model name |
| `PORT` | `8080` | Server port (set automatically by CF) |

---

## Project structure

```
payflow-ai/
├── backend/
│   ├── src/main/java/com/demo/payflowai/
│   │   ├── config/AiConfig.java          ChatClient + VectorStore beans
│   │   ├── tools/PaymentDataTools.java   @Tool methods
│   │   ├── service/SemanticSearchService.java
│   │   └── controller/
│   │       ├── ChatController.java       POST /api/chat
│   │       └── SearchController.java    GET  /api/search
│   └── src/main/resources/application.yml
├── frontend/
│   └── src/App.jsx                       Full React UI
├── build.sh                              npm ci + mvn package
└── manifest.yml                          CF deployment manifest
```

---

## Tech stack

- **Spring Boot 3.5** / Java 21
- **Spring AI 1.0.0** — OpenAI client, ChatClient, EmbeddingModel, SimpleVectorStore
- **React 18** + **Vite** + **Tailwind CSS 3**
- **Tanzu Platform GenAI tile** (OpenAI-compatible endpoint)
