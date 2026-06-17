package com.demo.payflowai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ChatClient chatClient;

    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /** Streaming endpoint — tokens arrive as SSE events, no CF router timeout hit */
    @PostMapping("/stream")
    public SseEmitter streamChat(@RequestBody ChatRequest request) {
        String sessionId = request.sessionId() != null ? request.sessionId() : UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(110_000L); // 110s — under CF's 180s router timeout

        executor.submit(() -> {
            try {
                Flux<String> tokens = chatClient.prompt()
                        .user(request.message())
                        .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, sessionId))
                        .stream()
                        .content();

                for (String token : tokens.toIterable()) {
                    emitter.send(SseEmitter.event().data(token));
                }
                emitter.send(SseEmitter.event().name("done").data(sessionId));
                emitter.complete();
            } catch (Exception e) {
                try {
                    String msg = e.getMessage() != null ? e.getMessage() : "Unknown AI error";
                    emitter.send(SseEmitter.event().name("error").data(msg));
                } catch (IOException ignored) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /** Non-streaming fallback (kept for compatibility) */
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
            return ResponseEntity.ok(new ChatResponse(future.get(90, TimeUnit.SECONDS), sessionId));
        } catch (TimeoutException e) {
            future.cancel(true);
            return ResponseEntity.status(503).body(Map.of("error", "AI timed out — try the streaming endpoint or retry"));
        } catch (ExecutionException e) {
            return ResponseEntity.status(503).body(Map.of(
                "error", "AI error: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage())));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of("error", "AI unavailable: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{sessionId}")
    public void clearSession(@PathVariable String sessionId) {}

    public record ChatRequest(String message, String sessionId) {}
    public record ChatResponse(String response, String sessionId) {}
}
