# PayFlow AI

AI-powered payment analyst built on **Spring AI 1.0** and **Tanzu Platform GenAI**.

Conversational chat analyst streaming from `mistralai/Devstral-Small-2507` — analyses mock payment data, explains fraud patterns, and generates investigation narratives. Per-session conversation memory. Payment data embedded in the system prompt. Deploys as a single jar to Tanzu Application Service (TAS).

---

## PayFlow demo suite

| Repo | Stack | Purpose |
|---|---|---|
| [payflow-demo](https://github.com/zahooruk2022/payflow-demo) | Spring Boot · Docker Compose | Local dev — PostgreSQL, RabbitMQ, Redis, Prometheus, Grafana |
| [payflow-demo-cf](https://github.com/zahooruk2022/payflow-demo-cf) | Spring Boot · CF managed services | Tanzu/TAS — single `cf push`, VCAP_SERVICES auto-wiring |
| **payflow-ai** ← you are here | Spring AI · Tanzu GenAI | AI payment analyst — streaming chat on Devstral-Small |
| [payflow-ai-epc](https://github.com/zahooruk2022/payflow-ai-epc) | Spring AI · Tanzu GenAI | EPC foundation variant — tool calling investigation |

---

## Features

- SSE streaming — tokens appear in the browser as the model generates them
- Per-session conversation memory — ask follow-up questions, the model remembers context
- Payment data embedded in system prompt — fraud stats, transaction feed, bank balances, fraud rules
- Suggested questions and one-click topic starters
- System Health dashboard — live status for API, DB, RabbitMQ, Redis, AI Model
- "New session" resets conversation memory

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

Tested on Tanzu Platform GenAI. Check `cf marketplace` for available plans on your foundation.

```bash
# 1. Create service instances (one-time)
cf create-service ai-models <your-ai-plan> payflow-ai-chat-tools
cf create-service p.rabbitmq rmq-single-node payflow-rabbitmq
cf create-service p.redis vk-ha-plan payflow-redis

# 2. Build (bundles React into the jar)
./build.sh

# 3. Push — services bound automatically via manifest.yml
cf push

# 4. Open
cf app payflow-ai   # shows the route
```

AI credentials (base URL + API key) are injected from `VCAP_SERVICES` at startup by `GenAiVcapPostProcessor` — no `cf set-env` needed. The model name is set in `manifest.yml`.

> **Service plans vary by foundation** — check `cf marketplace -e ai-models` on your target foundation to see available plans.

---

## Architecture

```
Browser
  └── React SPA (served by Spring Boot from /static/)
        └── POST /api/chat/stream   → ChatController (SSE)
              └── ChatClient (Spring AI)
                    ├── Devstral-Small-2507  (Tanzu GenAI — OpenAI-compatible)
                    ├── InMemoryChatMemory   (per sessionId)
                    └── System prompt with embedded payment data

CF managed services:
  payflow-ai-chat-tools  → spring.ai.openai.chat.base-url + api-key  (GenAiVcapPostProcessor)
  payflow-rabbitmq       → spring.rabbitmq.*                          (java-cfenv-boot)
  payflow-redis          → spring.data.redis.*                        (java-cfenv-boot + RedisSslFixPostProcessor)
  H2 in-memory           → spring.datasource.*  (local fallback, gives DB health indicator)
```

> **Why no tool calling?** The AI proxy returns 400 Bad Request when the OpenAI `tools` field is present in the request — all three available models are affected. Payment data is embedded directly in the system prompt as a workaround. `PaymentDataTools.java` remains in the codebase for the `payflow-ai-epc` variant targeting a foundation with native tool calling support.

---

## Configuration

**On CF** — AI credentials come from VCAP_SERVICES via service binding:

| CF Service | Plan | Provides |
|---|---|---|
| `payflow-ai-chat-tools` | `<your-ai-plan>` | `spring.ai.openai.chat.base-url` + `api-key` |
| `payflow-rabbitmq` | `rmq-single-node` | `spring.rabbitmq.*` |
| `payflow-redis` | `vk-ha-plan` | `spring.data.redis.*` |

**Env vars** (set in manifest.yml):

| Variable | Value | Description |
|---|---|---|
| `AI_CHAT_MODEL` | `mistralai/Devstral-Small-2507` | Chat model name (not in VCAP binding) |
| `AI_BASE_URL` | `http://localhost:11434/v1` | Local dev only — overridden by VCAP on CF |
| `AI_API_KEY` | `local` | Local dev only — overridden by VCAP on CF |
| `PORT` | _(CF-assigned)_ | Set automatically by CF |

> To inspect binding credentials for debugging: `cf create-service-key payflow-ai-chat-tools temp-key` then `cf service-key payflow-ai-chat-tools temp-key`. Delete the key afterwards. Never store or commit the output.

---

## Project structure

```
payflow-ai/
├── backend/
│   ├── src/main/java/com/demo/payflowai/
│   │   ├── config/AiConfig.java                    ChatClient bean (system prompt, memory)
│   │   ├── config/GenAiVcapPostProcessor.java       VCAP_SERVICES → Spring AI properties
│   │   ├── config/RedisSslFixPostProcessor.java     java-cfenv-boot SSL compat fix
│   │   ├── tools/PaymentDataTools.java              @Tool methods (inactive — proxy limitation)
│   │   └── controller/ChatController.java           POST /api/chat/stream (SSE)
│   └── src/main/resources/
│       ├── application.yml
│       └── META-INF/spring.factories
├── frontend/
│   └── src/
│       ├── App.jsx                                  Root layout — tabs, dark mode
│       └── components/
│           ├── ChatPanel.jsx                        SSE streaming chat UI
│           └── SystemStatus.jsx                     Live health dashboard
├── build.sh                                         npm ci + mvn package
└── manifest.yml                                     CF deployment manifest
```

---

## Tech stack

- **Spring Boot 3.5** / Java 21
- **Spring AI 1.0.0** — OpenAI client, ChatClient, SSE streaming, InMemoryChatMemory
- **java-cfenv-boot 3.1.5** — VCAP_SERVICES → Spring properties auto-wiring
- **React 18** + **Vite** + **Tailwind CSS 3**
- **Tanzu Platform GenAI tile** — `mistralai/Devstral-Small-2507` via OpenAI-compatible endpoint
