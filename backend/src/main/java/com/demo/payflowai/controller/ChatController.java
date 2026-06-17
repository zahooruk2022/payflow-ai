package com.demo.payflowai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping
    public ResponseEntity<?> chat(@RequestBody ChatRequest request) {
        try {
            String sessionId = request.sessionId() != null ? request.sessionId() : UUID.randomUUID().toString();
            String response = chatClient.prompt()
                    .user(request.message())
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, sessionId))
                    .call()
                    .content();
            return ResponseEntity.ok(new ChatResponse(response, sessionId));
        } catch (Exception e) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "AI service unavailable: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{sessionId}")
    public void clearSession(@PathVariable String sessionId) {
        // InMemoryChatMemory evicts automatically; explicit clear is a no-op here
    }

    public record ChatRequest(String message, String sessionId) {}
    public record ChatResponse(String response, String sessionId) {}
}
