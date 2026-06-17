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

# CF deploy
./build.sh
cf set-env payflow-ai AI_BASE_URL  https://<tanzu-ai-endpoint>/v1
cf set-env payflow-ai AI_API_KEY   <api-key>
cf push
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
| `backend/src/main/resources/application.yml` | AI model config via AI_BASE_URL / AI_CHAT_MODEL / AI_EMBEDDING_MODEL |
| `frontend/src/App.jsx` | Full UI — Chat tab + Semantic Search tab |
| `manifest.yml` | CF manifest — set AI_API_KEY via cf set-env before pushing |

---

## Spring AI notes

- **ChatClient** is built in `AiConfig` with `defaultTools(paymentDataTools)` and `MessageChatMemoryAdvisor` for per-session history.
- **@Tool** methods in `PaymentDataTools` use `org.springframework.ai.tool.annotation.Tool`.
- **SimpleVectorStore** is in-memory — seeded at startup in `SemanticSearchService@PostConstruct`. No external vector DB needed.
- **Conversation memory** key: `AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY` passed per request via `.advisors(spec -> spec.param(...))`.
- Both chat and embedding models share the same `spring.ai.openai.base-url` — Tanzu Platform AI exposes both on one OpenAI-compatible endpoint.
