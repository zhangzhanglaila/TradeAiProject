package com.trade.vo.kg;

import lombok.Data;

import java.util.List;

@Data
public class KgGraphVO {
    private List<Node> nodes;
    private List<Edge> edges;

    @Data
    public static class Node {
        private String id;
        private String label;
        private String type;
        private Object properties;
    }

    @Data
    public static class Edge {
        private String id;
        private String source;
        private String target;
        private String label;
        private Object properties;
    }
}
