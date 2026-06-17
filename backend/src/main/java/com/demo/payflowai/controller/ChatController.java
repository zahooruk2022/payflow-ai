package com.demo.payflowai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final int TIMEOUT_SECONDS = 90;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ChatClient chatClient;

    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping
    public ResponseEntity<?> chat(@RequestBody ChatRequest request) {
        String sessionId = request.sessionId() != null ? request.sessionId() : UUID.randomUUID().toString();
        Future<String> future = executor.submit(() ->
            chatClient.prompt()
                    .user(request.message())
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, sessionId))
                    .call()
                    .content()
        );
        try {
            String response = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return ResponseEntity.ok(new ChatResponse(response, sessionId));
        } catch (TimeoutException e) {
            future.cancel(true);
            return ResponseEntity.status(503).body(Map.of(
                "error", "AI response timed out after " + TIMEOUT_SECONDS + "s. The model may be under load — try again."
            ));
        } catch (ExecutionException e) {
            return ResponseEntity.status(503).body(Map.of(
                "error", "AI service error: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage())
            ));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of(
                "error", "AI service unavailable: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{sessionId}")
    public void clearSession(@PathVariable String sessionId) {
        // MessageWindowChatMemory evicts automatically; explicit clear is a no-op here
    }

    public record ChatRequest(String message, String sessionId) {}
    public record ChatResponse(String response, String sessionId) {}
}
