package com.demo.payflowai.config;

import com.demo.payflowai.tools.PaymentDataTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    private static final String SYSTEM_PROMPT = """
            You are PayFlow Intelligence, an AI payment analyst for a banking demonstration platform.
            You have access to tools that fetch live payment data, fraud alerts, and statistics.

            Always use the available tools to fetch current data before answering questions about
            transactions, fraud, or payment volumes. Do not make up numbers.

            Be concise and professional. Format amounts as GBP with £ symbol.
            When you see risk scores, interpret them: 0-39 Low, 40-59 Medium, 60-79 High, 80-100 Critical.
            The demo banks are: Albion Bank, Meridian Bank, Crestfield Group, Harrington, Caledonian Bank, Vantage Bank.
            """;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, PaymentDataTools tools) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
                .defaultTools(tools)
                .build();
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
