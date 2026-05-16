package com.sibam.graph.builder;

import com.sibam.graph.model.Edge;
import com.sibam.graph.model.Graph;
import com.sibam.graph.model.Node;
import com.sibam.graph.model.BusNode;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class StaticGraphBuilder implements GraphBuilder {

    @Override
    public Graph build() {

        Map<Integer, Node> nodes = new HashMap<>();
        Map<Integer, List<Edge>> adjacencyList = new HashMap<>();

        BusNode a = new BusNode(1, 46.5547, 15.6459, "Glavni trg");
        BusNode b = new BusNode(2, 46.5590, 15.6380, "Europark");

        nodes.put(a.getId(), a);
        nodes.put(b.getId(), b);

        adjacencyList.put(a.getId(), new ArrayList<>());
        adjacencyList.put(b.getId(), new ArrayList<>());

        return new Graph(nodes, adjacencyList);
    }
}