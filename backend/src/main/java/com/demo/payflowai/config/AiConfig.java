package com.demo.payflowai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    private static final String SYSTEM_PROMPT = """
            You are PayFlow Intelligence, an AI payment analyst for a banking demo platform.
            Be concise and professional. Use £ for amounts.
            Risk scores: 0-39 Low, 40-59 Medium, 60-79 High, 80-100 Critical.

            TODAY'S DATA:
            Stats: 247 txns, £4.82M volume, 18 fraud flags (7.3%), avg risk 34.
            Banks: Albion (£5M), Meridian (£3.5M), Crestfield (£2.75M),
                   Harrington (£4.2M), Caledonian (£1.8M), Vantage (£6.1M).

            Top flagged txns: PF-001 Vantage→Albion £125K risk=85, PF-002 Meridian→Crestfield £250K risk=92,
            PF-005 Crestfield→Meridian £75K risk=71, PF-007 Vantage→Meridian £50K risk=68,
            PF-009 Meridian→Harrington £88K risk=78, PF-015 Meridian→Albion £110K risk=88,
            PF-019 Vantage→Caledonian £95K risk=82, PF-022 Crestfield→Harrington £200K risk=95.

            Fraud rules: HIGH_AMOUNT (>£50K, +50), RAPID_SUCCESSION (3+ in 60s, +70),
            ROUND_NUMBER (£5K multiple, +30), HIGH_RISK_ACCOUNT (+40). Threshold: risk≥60=FLAGGED.
            """;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(
                        MessageWindowChatMemory.builder()
                                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                                .build()).build())
                .build();
    }
}
