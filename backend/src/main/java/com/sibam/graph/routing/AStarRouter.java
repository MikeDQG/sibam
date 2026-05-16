package com.sibam.graph.routing;

import com.sibam.graph.model.Edge;
import com.sibam.graph.model.Graph;
import com.sibam.graph.storage.GraphStore;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AStarRouter {

    private final GraphStore graphStore;

    public AStarRouter(GraphStore graphStore) {
        this.graphStore = graphStore;
    }

    public PathResult findPath(int startNodeId, int goalNodeId) {

        Graph graph = graphStore.getGraph();

        PriorityQueue<NodeRecord> openSet =
                new PriorityQueue<>(Comparator.comparingInt(NodeRecord::fScore));

        Map<Integer, Integer> gScore = new HashMap<>();
        Map<Integer, Integer> cameFrom = new HashMap<>();

        gScore.put(startNodeId, 0);

        openSet.add(new NodeRecord(startNodeId, 0));

        while (!openSet.isEmpty()) {

            NodeRecord current = openSet.poll();

            if (current.nodeId() == goalNodeId) {
                return reconstruct(cameFrom, current.nodeId(), gScore.get(goalNodeId));
            }

            for (Edge edge : graph.getNeighbors(current.nodeId())) {

                int tentative =
                        gScore.get(current.nodeId()) + edge.getCostSeconds();

                if (tentative < gScore.getOrDefault(edge.getToNodeId(), Integer.MAX_VALUE)) {

                    cameFrom.put(edge.getToNodeId(), current.nodeId());

                    gScore.put(edge.getToNodeId(), tentative);

                    openSet.add(
                            new NodeRecord(edge.getToNodeId(), tentative)
                    );
                }
            }
        }

        return null;
    }

    private PathResult reconstruct(
            Map<Integer, Integer> cameFrom,
            int current,
            int totalCost
    ) {

        List<Integer> path = new ArrayList<>();

        path.add(current);

        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.add(current);
        }

        Collections.reverse(path);

        return new PathResult(path, totalCost);
    }

    private record NodeRecord(int nodeId, int fScore) {
    }
}
