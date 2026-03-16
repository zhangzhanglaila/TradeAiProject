package com.trade.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private boolean enabled = false;

    /** DashScope API Key */
    private String apiKey;

    /** e.g. qwen3-max */
    private String modelName = "qwen3.5-plus";

    /** DashScope embedding 模型名（用于新闻语义检索） */
    private String embeddingModelName = "text-embedding-v3";

    /** 新闻切块大小（字符数），默认 600 */
    private int newsChunkSize = 600;

    /** 切块重叠大小（字符数），默认 80 */
    private int newsChunkOverlap = 80;

    /** 语义检索粗筛候选条数（从 MySQL 拉取最近 N 个 chunk），默认 2000 */
    private int newsRagCandidateLimit = 2000;
}
