# PayFlow AI

AI-powered payment analyst built on **Spring AI 1.0** and **Tanzu Platform GenAI**.

Conversational chat analyst powered by `gpt-oss:20b` with tool calling — queries live transaction data, explains fraud patterns, analyses risk, and generates investigation narratives. Per-session conversation memory. Six LLM-callable tools.

Deploys as a single jar to Tanzu Application Service (TAS). React frontend embedded in the Spring Boot jar.

---

## PayFlow demo suite

| Repo | Stack | Purpose |
|---|---|---|
| [payflow-demo](https://github.com/zahooruk2022/payflow-demo) | Spring Boot · Docker Compose | Local dev — PostgreSQL, RabbitMQ, Redis, Prometheus, Grafana |
| [payflow-demo-cf](https://github.com/zahooruk2022/payflow-demo-cf) | Spring Boot · CF managed services | Tanzu/TAS — single `cf push`, VCAP_SERVICES auto-wiring |
| **payflow-ai** ← you are here | Spring AI · Tanzu GenAI | AI payment analyst — tool-calling chat on `gpt-oss:20b` |

---

## Features

- Per-session conversation memory — ask follow-up questions, the model remembers context
- Six LLM-callable tools the model invokes autonomously:
  - `getRecentTransactions` — live transaction feed with risk scores
  - `getPaymentStatistics` — 24h volume, fraud rate, flagged count
  - `getFraudAlerts` — current high-risk alerts with rule/amount/risk
  - `explainFraudDetectionRules` — documents the four detection algorithms
  - `getAccountBalances` — current balances for all six demo banks
  - `generateFraudNarrative` — detailed investigation report for a transaction
- Suggested questions and one-click topic starters
- "New session" button resets conversation memory

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
# 1. Create AI service instance (one-time)
cf create-service ai-models chat-and-tools-model payflow-ai-chat-tools

# 2. Build (bundles React into the jar)
./build.sh

# 3. Push — service is bound automatically via manifest.yml
cf push

# 4. Open
cf app payflow-ai   # shows the route
```

Credentials (base URL + API key) are injected from `VCAP_SERVICES` at startup by `GenAiVcapPostProcessor` — no `cf set-env` needed. The model name (`gpt-oss:20b`) is set in `manifest.yml` since the service binding does not expose it.

---

## Architecture

```
Browser
  └── React SPA (served by Spring Boot from /static/)
        └── POST /api/chat      → ChatController
              └── ChatClient (Spring AI)
                    ├── gpt-oss:20b  (Tanzu Platform GenAI — OpenAI-compatible)
                    ├── InMemoryChatMemory  (per sessionId)
                    └── PaymentDataTools  (6 @Tool methods)
```

No external databases or message queues. The only runtime dependency is the Tanzu Platform AI endpoint.

---

## Configuration

**On CF** — credentials come from VCAP_SERVICES via service binding:

| CF Service | Provides |
|---|---|
| `payflow-ai-chat-tools` | `spring.ai.openai.chat.base-url` + `chat.api-key` |

**Env vars** (manifest.yml + local dev fallback):

| Variable | Default | Description |
|---|---|---|
| `AI_CHAT_MODEL` | `gpt-oss:20b` | Chat model name (not in binding) |
| `AI_BASE_URL` | `http://localhost:11434/v1` | Local dev only — overridden by VCAP on CF |
| `AI_API_KEY` | `local` | Local dev only — overridden by VCAP on CF |
| `PORT` | `8080` | Set automatically by CF |

> To inspect binding credentials for debugging: `cf create-service-key payflow-ai-chat-tools temp-key` then `cf service-key payflow-ai-chat-tools temp-key`. Delete the key afterwards. Never store or commit the output.

---

## Project structure

```
payflow-ai/
├── backend/
│   ├── src/main/java/com/demo/payflowai/
│   │   ├── config/AiConfig.java                  ChatClient bean
│   │   ├── config/GenAiVcapPostProcessor.java     VCAP_SERVICES → Spring AI properties
│   │   ├── tools/PaymentDataTools.java            @Tool methods
│   │   └── controller/ChatController.java         POST /api/chat
│   └── src/main/resources/
│       ├── application.yml
│       └── META-INF/spring.factories
├── frontend/
│   └── src/App.jsx                               React chat UI
├── build.sh                                      npm ci + mvn package
└── manifest.yml                                  CF deployment manifest
```

---

## Tech stack

- **Spring Boot 3.5** / Java 21
- **Spring AI 1.0.0** — OpenAI client, ChatClient, tool calling, InMemoryChatMemory
- **React 18** + **Vite** + **Tailwind CSS 3**
- **Tanzu Platform GenAI tile** — `gpt-oss:20b` via OpenAI-compatible endpoint
