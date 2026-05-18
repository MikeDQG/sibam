package com.sibam.graph.model;

import java.util.HashMap;
import java.util.Map;

public enum NodeType {
    BUS(0),
    BIKE(1),
    USER(2);

    private final int i;
    private static final Map<Integer, NodeType> BY_VALUE = new HashMap<>();

    NodeType(int i) {
        this.i = i;
    }

    static {
        for (NodeType type : values()) {
            BY_VALUE.put(type.i, type);
        }
    }

    public static NodeType fromValue(int value) {
        return BY_VALUE.get(value);
    }

    public int getValue() {
        return i;
    }
}
