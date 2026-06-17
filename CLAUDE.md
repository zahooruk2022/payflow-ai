# CLAUDE.md — PayFlow AI

Spring AI demo app: AI payment analyst with tool calling on `gpt-oss:20b`.
Conversational chat with per-session memory and six LLM-callable tools.
Deploys as a single jar to Tanzu Application Service via `cf push`.
GitHub: https://github.com/zahooruk2022/payflow-ai

Model used (Tanzu Platform AI):
- **gpt-oss:20b** — chat + tool calling (ChatController, AiConfig, PaymentDataTools)

---

## Commands

```bash
# Local dev — requires an OpenAI-compatible LLM (e.g. Ollama)
export AI_BASE_URL=http://localhost:11434/v1
export AI_CHAT_MODEL=llama3.2
cd backend && mvn spring-boot:run

cd frontend && npm install && npm run dev   # http://localhost:5174

# CF deploy (one-time service setup)
cf create-service ai-models chat-and-tools-model payflow-ai-chat-tools

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
| `backend/src/main/java/.../config/AiConfig.java` | Wires ChatClient (with memory + tools) |
| `backend/src/main/java/.../config/GenAiVcapPostProcessor.java` | Reads VCAP_SERVICES `ai-models` binding → sets Spring AI chat base-url + api-key |
| `backend/src/main/resources/META-INF/spring.factories` | Registers GenAiVcapPostProcessor as an EnvironmentPostProcessor |
| `backend/src/main/java/.../tools/PaymentDataTools.java` | @Tool methods available to gpt-oss:20b |
| `backend/src/main/java/.../controller/ChatController.java` | POST /api/chat — per-session conversation memory |
| `backend/src/main/resources/application.yml` | AI model name + local dev defaults (base-url/api-key overridden by VCAP on CF) |
| `frontend/src/App.jsx` | React chat UI — message history, suggested questions, session management |
| `manifest.yml` | CF manifest — binds payflow-ai-chat-tools service |

---

## Spring AI notes

- **ChatClient** is built in `AiConfig` with `defaultTools(paymentDataTools)` and `MessageChatMemoryAdvisor` for per-session history.
- **@Tool** methods in `PaymentDataTools` use `org.springframework.ai.tool.annotation.Tool`.
- **Conversation memory** key: `AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY` passed per request via `.advisors(spec -> spec.param(...))`.
- **GenAiVcapPostProcessor** reads `VCAP_SERVICES["ai-models"][0]` and maps `credentials.endpoint.openai_api_base` + `api_key` to `spring.ai.openai.chat.base-url` + `chat.api-key`. Registered in `META-INF/spring.factories` (same pattern as `RedisSslFixPostProcessor` in payflow-demo-cf).
