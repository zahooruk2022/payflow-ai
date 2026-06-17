# CLAUDE.md — PayFlow AI

Spring AI demo app: AI payment analyst on `mistralai/Devstral-Small-2507` (GTO-models plan, Tanzu Platform dhaka).
Data embedded in system prompt. SSE streaming chat with per-session memory.
Deploys as a single jar to Tanzu Application Service via `cf push`.
GitHub: https://github.com/zahooruk2022/payflow-ai

Model (Tanzu Platform GenAI — dhaka foundation):
- **mistralai/Devstral-Small-2507** — streaming chat via OpenAI-compatible SSE

> **Tool calling is disabled.** The GTO-models proxy on dhaka returns 400 when the `tools` field is present in OpenAI API requests (affects all three available models: Devstral, gpt-oss-120b, gemma-4-31B). Payment data is embedded directly in the system prompt instead. `PaymentDataTools.java` remains in the codebase for the `payflow-ai-epc` variant where native tool calling may work.

---

## Commands

```bash
# Local dev — requires an OpenAI-compatible LLM (e.g. Ollama)
export AI_BASE_URL=http://localhost:11434/v1
export AI_CHAT_MODEL=llama3.2
cd backend && mvn spring-boot:run

cd frontend && npm install && npm run dev   # http://localhost:5174

# CF deploy (one-time service setup — dhaka foundation)
cf target -a https://api.sys.dhaka.cf-app.com
cf create-service ai-models GTO-models payflow-ai-chat-tools
cf create-service p.rabbitmq rmq-single-node payflow-rabbitmq
cf create-service p.redis vk-ha-plan payflow-redis

# Then every deploy:
./build.sh && cf push
```

---

## Inspecting AI service credentials (debugging only)

Never store the output of these commands — they contain live API keys.

```bash
# See what VCAP_SERVICES the app received (after cf push):
cf env payflow-ai

# Inspect a binding before deploying (temporary key — delete after):
cf create-service-key payflow-ai-chat-tools temp-key
cf service-key payflow-ai-chat-tools temp-key
cf delete-service-key payflow-ai-chat-tools temp-key -f
```

---

## Key files

| File | Purpose |
|---|---|
| `backend/src/main/java/.../config/AiConfig.java` | Wires ChatClient: system prompt with embedded payment data, MessageChatMemoryAdvisor |
| `backend/src/main/java/.../config/GenAiVcapPostProcessor.java` | Reads VCAP_SERVICES `ai-models` binding → sets Spring AI base-url + api-key |
| `backend/src/main/java/.../config/RedisSslFixPostProcessor.java` | Fixes java-cfenv-boot 3.1.x setting `ssl=true` (String) which breaks Spring Boot 3.3+ `RedisProperties.Ssl` binding |
| `backend/src/main/resources/META-INF/spring.factories` | Registers GenAiVcapPostProcessor + RedisSslFixPostProcessor |
| `backend/src/main/java/.../tools/PaymentDataTools.java` | Mock @Tool methods — present but NOT registered with ChatClient (see note above) |
| `backend/src/main/java/.../controller/ChatController.java` | `POST /api/chat/stream` (SSE streaming) + `POST /api/chat` (sync fallback) |
| `backend/src/main/resources/application.yml` | Model name, H2 DB, RabbitMQ/Redis fallbacks, `spring.ai.retry.max-attempts: 1` |
| `manifest.yml` | CF manifest — binds payflow-ai-chat-tools, payflow-rabbitmq, payflow-redis |

---

## Spring AI / streaming notes

- **ChatClient** is built in `AiConfig` with a compact system prompt (payment data embedded, ~150 tokens) and `MessageChatMemoryAdvisor` for per-session history.
- **SSE streaming**: `ChatController.streamChat()` uses `SseEmitter` + `Flux<String>.toIterable()`.
  - SSE comment sent before AI call to commit HTTP 200 and prevent 503 on fast failure.
  - Keepalive every 15 s resets CF GoRouter's 180 s idle timeout.
- **Retry disabled**: `spring.ai.retry.max-attempts: 1` — retry doubles latency and masks errors.
- **GenAiVcapPostProcessor** reads `VCAP_SERVICES["ai-models"][0]`, maps `credentials.endpoint.openai_api_base` + `api_key` → `spring.ai.openai.chat.base-url` + `chat.api-key`. Registered in `META-INF/spring.factories`.
- **RedisSslFixPostProcessor** — java-cfenv-boot 3.1.x writes `spring.data.redis.ssl=true` (String) into the environment; Spring Boot 3.3+ expects `spring.data.redis.ssl.enabled` (Boolean inside a record). The processor adds `.enabled` at highest priority. Same pattern as `payflow-demo-cf`.
