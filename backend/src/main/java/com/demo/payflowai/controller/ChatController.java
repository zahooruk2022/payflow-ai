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

    /**
     * Streaming endpoint — tokens arrive as SSE events.
     * A keepalive comment is sent every 15 s so CF GoRouter's 180-s idle timer never fires
     * while gpt-oss:20b is thinking (tool-call + response can take 60-120 s).
     */
    @PostMapping("/stream")
    public SseEmitter streamChat(@RequestBody ChatRequest request) {
        String sessionId = request.sessionId() != null ? request.sessionId() : UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(0L); // 0 = no server-side timeout; keepalive keeps CF connection alive

        executor.submit(() -> {
            // Commit the HTTP 200 response immediately so any AI failure below becomes an SSE error
            // event rather than a Spring Boot HTTP 503 (which happens when completeWithError fires
            // before the response headers are written).
            try { emitter.send(SseEmitter.event().comment("connected")); }
            catch (Exception ex) { emitter.completeWithError(ex); return; }

            // SSE comment every 15 s resets CF GoRouter's 180-s idle timer while the model thinks
            ScheduledExecutorService keepalive = Executors.newSingleThreadScheduledExecutor();
            keepalive.scheduleAtFixedRate(() -> {
                try { emitter.send(SseEmitter.event().comment("keepalive")); }
                catch (Exception ignored) { keepalive.shutdown(); }
            }, 15, 15, TimeUnit.SECONDS);

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
            } finally {
                keepalive.shutdownNow();
            }
        });

        return emitter;
    }

    /** Non-streaming fallback (for curl/testing). No future.cancel() — avoids InterruptedException. */
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
            // 170 s — just under CF's 180-s GoRouter limit; no future.cancel() to avoid interrupting Spring AI
            return ResponseEntity.ok(new ChatResponse(future.get(170, TimeUnit.SECONDS), sessionId));
        } catch (TimeoutException e) {
            return ResponseEntity.status(503).body(Map.of("error", "AI timed out — use streaming endpoint"));
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
