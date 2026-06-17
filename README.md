# PayFlow AI

AI-powered payment analyst built on **Spring AI 1.0** and **Tanzu Platform GenAI**.

Two capabilities in one app:

| Feature | Model | Description |
|---|---|---|
| **Chat Analyst** | `gpt-oss:20b` | Conversational AI with tool calling — queries transaction data, explains fraud rules, generates investigation narratives |
| **Semantic Search** | `nomic-embed-text-v2-moe` | Finds transactions by meaning using vector embeddings — not keyword matching |

Deploys as a single jar to Tanzu Application Service (TAS). React frontend embedded in the Spring Boot jar.

---

## PayFlow demo suite

| Repo | Stack | Purpose |
|---|---|---|
| [payflow-demo](https://github.com/zahooruk2022/payflow-demo) | Spring Boot · Docker Compose | Local dev — PostgreSQL, RabbitMQ, Redis, Prometheus, Grafana |
| [payflow-demo-cf](https://github.com/zahooruk2022/payflow-demo-cf) | Spring Boot · CF managed services | Tanzu/TAS — single `cf push`, VCAP_SERVICES auto-wiring |
| **payflow-ai** ← you are here | Spring AI · Tanzu GenAI | AI payment analyst — tool-calling chat + semantic transaction search |

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
# 1. Create AI service instances (one-time)
cf create-service ai-models chat-and-tools-model payflow-ai-chat-tools
cf create-service ai-models <embedding-plan>     payflow-ai-embeddings

# 2. Build (bundles React into the jar)
./build.sh

# 3. Push — services are bound automatically via manifest.yml
cf push

# 4. Open
cf app payflow-ai   # shows the route
```

Credentials (base URL + API key) are injected from VCAP_SERVICES at startup by `GenAiVcapPostProcessor` — no `cf set-env` needed. Model names (`gpt-oss:20b`, `nomic-embed-text-v2-moe`) are set in `manifest.yml` since the service binding does not expose them.

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

**On CF** — AI credentials (URL + API key) come from VCAP_SERVICES via service binding. No `cf set-env` needed for credentials:

| CF Service | Provides |
|---|---|
| `payflow-ai-chat-tools` | `spring.ai.openai.chat.base-url` + `chat.api-key` |
| `payflow-ai-embeddings` | `spring.ai.openai.embedding.base-url` + `embedding.api-key` |

**Env vars** (manifest.yml + local dev fallback):

| Variable | Default | Description |
|---|---|---|
| `AI_CHAT_MODEL` | `gpt-oss:20b` | Chat model name (not in binding) |
| `AI_EMBEDDING_MODEL` | `nomic-embed-text-v2-moe` | Embedding model name (not in binding) |
| `AI_BASE_URL` | `http://localhost:11434/v1` | Local dev only — overridden by VCAP on CF |
| `AI_API_KEY` | `local` | Local dev only — overridden by VCAP on CF |
| `PORT` | `8080` | Set automatically by CF |

> To inspect binding credentials for debugging, use `cf create-service-key <instance> temp-key` then `cf service-key <instance> temp-key`. Delete the key afterwards with `cf delete-service-key`. Never store or commit the output.

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
