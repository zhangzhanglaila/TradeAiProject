package com.trade.service.kg;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.ArrayList;

@Repository
public class Neo4jGraphRepository {

    private final Neo4jClient neo4jClient;

    public Neo4jGraphRepository(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    public void upsertGraph(String graphKey, Long userId, List<Map<String, Object>> newsList, List<Triple> triples) {
        if (graphKey == null || graphKey.isBlank()) {
            return;
        }

        // 1) 根节点
        neo4jClient.query(
                        "MERGE (g:Graph {graphKey:$graphKey}) " +
                                "SET g.userId=$userId, g.createdAt=$createdAt")
                .bindAll(Map.of(
                        "graphKey", graphKey,
                        "userId", userId,
                        "createdAt", LocalDateTime.now().toString()
                ))
                .run();

        // 2) News 节点 + HAS_NEWS
        for (Map<String, Object> n : newsList) {
            Map<String, Object> params = new HashMap<>();
            params.put("graphKey", graphKey);
            params.put("newsId", n.get("newsId"));
            params.put("title", n.get("title"));
            params.put("pubDate", n.get("pubDate"));
            params.put("contentSnippet", n.get("contentSnippet"));

            neo4jClient.query(
                            "MATCH (g:Graph {graphKey:$graphKey}) " +
                                    "MERGE (news:News {graphKey:$graphKey, newsId:$newsId}) " +
                                    "SET news.title=$title, news.pubDate=$pubDate, news.contentSnippet=$contentSnippet " +
                                    "MERGE (g)-[:HAS_NEWS {graphKey:$graphKey}]->(news)")
                    .bindAll(params)
                    .run();
        }

        // 3) Entity + RELATES_TO
        for (Triple t : triples) {
            if (t == null) {
                continue;
            }
            if (isBlank(t.getHead()) || isBlank(t.getTail()) || isBlank(t.getRelation())) {
                continue;
            }

            neo4jClient.query(
                            "MATCH (g:Graph {graphKey:$graphKey}) " +
                                    "MERGE (h:Entity {graphKey:$graphKey, name:$head}) " +
                                    "SET h.type=$headType " +
                                    "MERGE (ta:Entity {graphKey:$graphKey, name:$tail}) " +
                                    "SET ta.type=$tailType " +
                                    "MERGE (g)-[:HAS_ENTITY {graphKey:$graphKey}]->(h) " +
                                    "MERGE (g)-[:HAS_ENTITY {graphKey:$graphKey}]->(ta) " +
                                    "MERGE (h)-[r:RELATES_TO {graphKey:$graphKey, relationType:$relationType}]->(ta) " +
                                    "SET r.confidence=$confidence, r.source=$source")
                    .bindAll(Map.of(
                            "graphKey", graphKey,
                            "head", t.getHead(),
                            "headType", nullToEmpty(t.getHeadType()),
                            "tail", t.getTail(),
                            "tailType", nullToEmpty(t.getTailType()),
                            "relationType", t.getRelation(),
                            "confidence", t.getConfidence(),
                            "source", t.getSource()
                    ))
                    .run();
        }
    }

    public Map<String, Object> loadGraph(String graphKey) {
        // nodes
        List<Map<String, Object>> news = new ArrayList<>(neo4jClient.query(
                        "MATCH (g:Graph {graphKey:$graphKey})-[:HAS_NEWS {graphKey:$graphKey}]->(n:News {graphKey:$graphKey}) " +
                                "RETURN n.newsId AS newsId, n.title AS title, n.pubDate AS pubDate, n.contentSnippet AS contentSnippet")
                .bind(graphKey).to("graphKey")
                .fetch()
                .all());

        List<Map<String, Object>> entities = new ArrayList<>(neo4jClient.query(
                        "MATCH (g:Graph {graphKey:$graphKey})-[:HAS_ENTITY {graphKey:$graphKey}]->(e:Entity {graphKey:$graphKey}) " +
                                "RETURN e.name AS name, e.type AS type")
                .bind(graphKey).to("graphKey")
                .fetch()
                .all());

        // edges
        List<Map<String, Object>> edges = new ArrayList<>(neo4jClient.query(
                        "MATCH (a:Entity {graphKey:$graphKey})-[r:RELATES_TO {graphKey:$graphKey}]->(b:Entity {graphKey:$graphKey}) " +
                                "RETURN a.name AS source, b.name AS target, r.relationType AS relationType, r.confidence AS confidence, r.source AS sourceInfo")
                .bind(graphKey).to("graphKey")
                .fetch()
                .all());

        Map<String, Object> out = new HashMap<>();
        out.put("news", news);
        out.put("entities", entities);
        out.put("edges", edges);
        return out;
    }

    public void deleteGraph(String graphKey) {
        neo4jClient.query(
                        "MATCH (n {graphKey:$graphKey}) DETACH DELETE n")
                .bind(graphKey).to("graphKey")
                .run();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
