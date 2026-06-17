# CLAUDE.md — PayFlow AI

Spring AI demo app: payment chat analyst (tool calling) + semantic transaction search (embeddings).
Deploys as a single jar to Tanzu Application Service via `cf push`.
GitHub: https://github.com/zahooruk2022/payflow-ai

Models used (Tanzu Platform AI):
- **gpt-oss:20b** — chat + tool calling (ChatController, AiConfig)
- **nomic-embed-text-v2-moe** — embeddings + semantic search (SemanticSearchService)

---

## Commands

```bash
# Local dev — requires an OpenAI-compatible LLM (e.g. Ollama)
export AI_BASE_URL=http://localhost:11434/v1
export AI_CHAT_MODEL=llama3.2
export AI_EMBEDDING_MODEL=nomic-embed-text
cd backend && mvn spring-boot:run

cd frontend && npm install && npm run dev   # http://localhost:5174

# CF deploy (one-time service setup)
cf create-service ai-models chat-and-tools-model payflow-ai-chat-tools
cf create-service ai-models <embedding-plan>     payflow-ai-embeddings

# Then every deploy:
./build.sh && cf push
```

---

## Key files

| File | Purpose |
|---|---|
| `backend/src/main/java/.../config/AiConfig.java` | Wires ChatClient (with memory + tools) and SimpleVectorStore |
| `backend/src/main/java/.../tools/PaymentDataTools.java` | @Tool methods available to gpt-oss:20b |
| `backend/src/main/java/.../service/SemanticSearchService.java` | Seeds vector store, exposes similarity search |
| `backend/src/main/java/.../controller/ChatController.java` | POST /api/chat — per-session conversation memory |
| `backend/src/main/java/.../controller/SearchController.java` | GET /api/search?q=&topK= |
| `backend/src/main/java/.../config/GenAiVcapPostProcessor.java` | Reads VCAP_SERVICES `ai-models` bindings → sets per-type Spring AI base-url + api-key |
| `backend/src/main/resources/META-INF/spring.factories` | Registers GenAiVcapPostProcessor as an EnvironmentPostProcessor |
| `backend/src/main/resources/application.yml` | AI model names + local dev defaults (base-url/api-key overridden by VCAP on CF) |
| `frontend/src/App.jsx` | Full UI — Chat tab + Semantic Search tab |
| `manifest.yml` | CF manifest — binds payflow-ai-chat-tools + payflow-ai-embeddings services |

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

## Spring AI notes

- **ChatClient** is built in `AiConfig` with `defaultTools(paymentDataTools)` and `MessageChatMemoryAdvisor` for per-session history.
- **@Tool** methods in `PaymentDataTools` use `org.springframework.ai.tool.annotation.Tool`.
- **SimpleVectorStore** is in-memory — seeded at startup in `SemanticSearchService@PostConstruct`. No external vector DB needed.
- **Conversation memory** key: `AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY` passed per request via `.advisors(spec -> spec.param(...))`.
- Each AI service binding provides its own `openai_api_base` URL and JWT `api_key`. `GenAiVcapPostProcessor` reads VCAP_SERVICES and sets `spring.ai.openai.chat.base-url` / `spring.ai.openai.embedding.base-url` per-type. Registered in `META-INF/spring.factories` (same pattern as `RedisSslFixPostProcessor` in payflow-demo-cf).
- Binding whose CF instance name contains "embed" is mapped to embedding properties; all others to chat properties.
