package com.trade.service.kg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trade.common.BusinessException;
import com.trade.common.ErrorCode;
import com.trade.entity.NewsData;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class KgAiExtractService {

    private static final int MAX_TRIPLES = 120;

    @Autowired(required = false)
    private ChatLanguageModel chatLanguageModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Triple> extractTriples(List<NewsData> newsList) {
        if (chatLanguageModel == null) {
            throw new BusinessException(ErrorCode.KG_AI_DISABLED);
        }

        String prompt = buildPrompt(newsList);

        String text;
        try {
            text = chatLanguageModel.generate(prompt);
        } catch (Exception e) {
            log.error("KG AI generate failed", e);
            throw new BusinessException(ErrorCode.KG_AI_PARSE_FAILED, "AI 抽取失败：" + e.getMessage());
        }

        if (text == null) {
            throw new BusinessException(ErrorCode.KG_AI_PARSE_FAILED, "AI 输出为空");
        }

        return parseTriples(text);
    }

    private String buildPrompt(List<NewsData> newsList) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是信息抽取系统。请从给定新闻中抽取实体与关系三元组。\n");
        sb.append("严格只输出 JSON，不要输出任何额外文字、代码块标记或解释。\n");
        sb.append("输出 schema：{\"triples\":[{\"head\":string,\"headType\":string,\"relation\":string,\"tail\":string,\"tailType\":string,\"confidence\":number(0-1),\"source\":string}]}\n");
        sb.append("约束：\n");
        sb.append("1) triples 数组长度不超过 ").append(MAX_TRIPLES).append("。\n");
        sb.append("2) head/tail 必须是具体实体名称；relation 为简短关系词（中文）。\n");
        sb.append("3) headType/tailType 只能取：人物、组织、地点、事件、产品、国家、时间、其他。\n");
        sb.append("4) confidence 为 0 到 1 的小数。\n");
        sb.append("5) source 填写 newsId（如 \"news:123\"）或标题片段。\n\n");

        sb.append("新闻列表：\n");
        for (NewsData n : newsList) {
            sb.append("- newsId=").append(n.getId()).append("\n");
            if (n.getTitle() != null) {
                sb.append("  title=").append(trim(n.getTitle(), 200)).append("\n");
            }
            if (n.getPubDate() != null) {
                sb.append("  pubDate=").append(n.getPubDate()).append("\n");
            }
            if (n.getContentText() != null) {
                sb.append("  content=").append(trim(n.getContentText(), 1200)).append("\n");
            } else if (n.getDescText() != null) {
                sb.append("  content=").append(trim(n.getDescText(), 800)).append("\n");
            }
        }

        return sb.toString();
    }

    private List<Triple> parseTriples(String text) {
        JsonNode root = tryParseJson(text);
        if (root == null) {
            // 容错：截取第一个 { 到最后一个 }
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start >= 0 && end > start) {
                root = tryParseJson(text.substring(start, end + 1));
            }
        }

        if (root == null) {
            throw new BusinessException(ErrorCode.KG_AI_PARSE_FAILED, "AI 输出无法解析为 JSON");
        }

        JsonNode triplesNode = root.get("triples");
        if (triplesNode == null || !triplesNode.isArray()) {
            throw new BusinessException(ErrorCode.KG_AI_PARSE_FAILED, "AI 输出缺少 triples 数组");
        }

        List<Triple> out = new ArrayList<>();
        for (JsonNode t : triplesNode) {
            String head = asText(t.get("head"));
            String tail = asText(t.get("tail"));
            String relation = asText(t.get("relation"));
            if (isBlank(head) || isBlank(tail) || isBlank(relation)) {
                continue;
            }

            Triple triple = new Triple();
            triple.setHead(head.trim());
            triple.setHeadType(defaultType(asText(t.get("headType"))));
            triple.setRelation(relation.trim());
            triple.setTail(tail.trim());
            triple.setTailType(defaultType(asText(t.get("tailType"))));
            triple.setConfidence(asDouble(t.get("confidence")));
            triple.setSource(asText(t.get("source")));
            out.add(triple);

            if (out.size() >= MAX_TRIPLES) {
                break;
            }
        }

        return out;
    }

    private JsonNode tryParseJson(String s) {
        if (s == null) {
            return null;
        }
        try {
            return objectMapper.readTree(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String asText(JsonNode node) {
        return node == null ? null : node.asText(null);
    }

    private Double asDouble(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asDouble();
        }
        String s = node.asText(null);
        if (s == null) {
            return null;
        }
        try {
            return Double.parseDouble(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String defaultType(String t) {
        if (t == null || t.isBlank()) {
            return "其他";
        }
        return t.trim();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String trim(String s, int maxLen) {
        if (s == null) {
            return null;
        }
        String x = s.replace('\u0000', ' ').trim();
        if (x.length() <= maxLen) {
            return x;
        }
        return x.substring(0, maxLen);
    }
}
