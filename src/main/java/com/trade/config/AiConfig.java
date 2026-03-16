package com.trade.config;

import com.trade.ai.AiAssistant;
import com.trade.ai.AiProperties;
import com.trade.ai.TradeAiTools;
import com.trade.ai.memory.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

@Configuration
public class AiConfig {

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private TradeAiTools tradeAiTools;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Bean
    @ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "true")
    public ChatLanguageModel chatLanguageModel() {
        return QwenChatModel.builder()
                .apiKey(aiProperties.getApiKey())
                .modelName(aiProperties.getModelName())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "true")
    public ChatMemoryStore chatMemoryStore() {
        // 默认 7 天过期；需要更长可改成配置项
        return new RedisChatMemoryStore(stringRedisTemplate, "ai:chat:memory:", Duration.ofDays(7));
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "true")
    public AiAssistant aiAssistant(ChatLanguageModel chatLanguageModel, ChatMemoryStore chatMemoryStore) {
        return AiServices.builder(AiAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(20)
                        .chatMemoryStore(chatMemoryStore)
                        .build())
                .tools(tradeAiTools)
                .build();
    }
}
