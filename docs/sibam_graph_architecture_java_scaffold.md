# ŠibaM Graph Architecture – Java Scaffold

## Package Structure

```text
com.sibam.graph
├── model
├── builder
├── storage
├── spatial
└── routing
```

---

# MODEL

## EdgeType.java

```java
package com.sibam.graph.model;

public enum EdgeType {
    WALK,
    BUS,
    BIKE,
    TRANSFER
}
```

---

## Node.java

```java
package com.sibam.graph.model;

public abstract class Node {

    protected final int id;
    protected final double lat;
    protected final double lon;

    protected Node(int id, double lat, double lon) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
    }

    public int getId() {
        return id;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }
}
```

---

## StopNode.java

```java
package com.sibam.graph.model;

public class StopNode extends Node {

    private final String stopName;

    public StopNode(int id, double lat, double lon, String stopName) {
        super(id, lat, lon);
        this.stopName = stopName;
    }

    public String getStopName() {
        return stopName;
    }
}
```

---

## BikeStationNode.java

```java
package com.sibam.graph.model;

public class BikeStationNode extends Node {

    private final String stationName;

    public BikeStationNode(int id, double lat, double lon, String stationName) {
        super(id, lat, lon);
        this.stationName = stationName;
    }

    public String getStationName() {
        return stationName;
    }
}
```

---

## Edge.java

```java
package com.sibam.graph.model;

public class Edge {

    private final int fromNodeId;
    private final int toNodeId;
    private final EdgeType edgeType;

    private final int distanceMeters;
    private final int costSeconds;

    public Edge(
            int fromNodeId,
            int toNodeId,
            EdgeType edgeType,
            int distanceMeters,
            int costSeconds
    ) {
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.edgeType = edgeType;
        this.distanceMeters = distanceMeters;
        this.costSeconds = costSeconds;
    }

    public int getFromNodeId() {
        return fromNodeId;
    }

    public int getToNodeId() {
        return toNodeId;
    }

    public EdgeType getEdgeType() {
        return edgeType;
    }

    public int getDistanceMeters() {
        return distanceMeters;
    }

    public int getCostSeconds() {
        return costSeconds;
    }
}
```

---

## Graph.java

```java
package com.sibam.graph.model;

import java.util.List;
import java.util.Map;

public class Graph {

    private final Map<Integer, Node> nodes;
    private final Map<Integer, List<Edge>> adjacencyList;

    public Graph(
            Map<Integer, Node> nodes,
            Map<Integer, List<Edge>> adjacencyList
    ) {
        this.nodes = nodes;
        this.adjacencyList = adjacencyList;
    }

    public Map<Integer, Node> getNodes() {
        return nodes;
    }

    public Map<Integer, List<Edge>> getAdjacencyList() {
        return adjacencyList;
    }

    public List<Edge> getNeighbors(int nodeId) {
        return adjacencyList.getOrDefault(nodeId, List.of());
    }
}
```

---

# BUILDER

## GraphBuilder.java

```java
package com.sibam.graph.builder;

import com.sibam.graph.model.Graph;

public interface GraphBuilder {

    Graph build();
}
```

---

## WalkingEdgeBuilder.java

```java
package com.sibam.graph.builder;

import com.sibam.graph.model.Edge;
import com.sibam.graph.model.EdgeType;
import com.sibam.graph.model.Node;

public class WalkingEdgeBuilder {

    public Edge build(Node from, Node to) {

        int distance = calculateDistance(from, to);

        int walkingSeconds = distance / 1;

        return new Edge(
                from.getId(),
                to.getId(),
                EdgeType.WALK,
                distance,
                walkingSeconds
        );
    }

    private int calculateDistance(Node from, Node to) {
        return 250;
    }
}
```

---

## BusEdgeBuilder.java

```java
package com.sibam.graph.builder;

import com.sibam.graph.model.Edge;
import com.sibam.graph.model.EdgeType;

public class BusEdgeBuilder {

    public Edge build(
            int fromNodeId,
            int toNodeId,
            int distanceMeters,
            int travelTimeSeconds
    ) {
        return new Edge(
                fromNodeId,
                toNodeId,
                EdgeType.BUS,
                distanceMeters,
                travelTimeSeconds
        );
    }
}
```

---

## BikeEdgeBuilder.java

```java
package com.sibam.graph.builder;

import com.sibam.graph.model.Edge;
import com.sibam.graph.model.EdgeType;

public class BikeEdgeBuilder {

    public Edge build(
            int fromNodeId,
            int toNodeId,
            int distanceMeters,
            int travelTimeSeconds
    ) {
        return new Edge(
                fromNodeId,
                toNodeId,
                EdgeType.BIKE,
                distanceMeters,
                travelTimeSeconds
        );
    }
}
```

---

# STORAGE

## GraphStore.java

```java
package com.sibam.graph.storage;

import com.sibam.graph.model.Graph;

public interface GraphStore {

    Graph getGraph();

    void replaceGraph(Graph graph);
}
```

---

## InMemoryGraphStore.java

```java
package com.sibam.graph.storage;

import com.sibam.graph.model.Graph;
import org.springframework.stereotype.Service;

@Service
public class InMemoryGraphStore implements GraphStore {

    private volatile Graph graph;

    @Override
    public Graph getGraph() {
        return graph;
    }

    @Override
    public void replaceGraph(Graph graph) {
        this.graph = graph;
    }
}
```

---

## GraphSerializer.java

```java
package com.sibam.graph.storage;

import com.sibam.graph.model.Graph;

public interface GraphSerializer {

    void save(Graph graph);

    Graph load();

    boolean exists();
}
```

---

## KryoGraphSerializer.java

```java
package com.sibam.graph.storage;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.sibam.graph.model.Graph;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class KryoGraphSerializer implements GraphSerializer {

    private static final String FILE_NAME = "graph.bin";

    private final Kryo kryo;

    public KryoGraphSerializer() {
        this.kryo = new Kryo();
        kryo.register(Graph.class);
    }

    @Override
    public void save(Graph graph) {

        try (
                Output output = new Output(new FileOutputStream(FILE_NAME))
        ) {
            kryo.writeObject(output, graph);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Graph load() {

        try (
                Input input = new Input(new FileInputStream(FILE_NAME))
        ) {
            return kryo.readObject(input, Graph.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean exists() {
        return Files.exists(Path.of(FILE_NAME));
    }
}
```

---

# SPATIAL

## RTreeIndex.java

```java
package com.sibam.graph.spatial;

import com.sibam.graph.model.Node;

import java.util.List;

public class RTreeIndex {

    public void build(List<Node> nodes) {
        // TODO implement R-Tree
    }

    public List<Node> nearest(double lat, double lon, int limit) {
        return List.of();
    }
}
```

---

## SpatialSearchService.java

```java
package com.sibam.graph.spatial;

import com.sibam.graph.model.Node;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpatialSearchService {

    private final RTreeIndex rTreeIndex = new RTreeIndex();

    public List<Node> findNearest(
            double lat,
            double lon,
            int limit
    ) {
        return rTreeIndex.nearest(lat, lon, limit);
    }
}
```

---

# ROUTING

## CostFunction.java

```java
package com.sibam.graph.routing;

import com.sibam.graph.model.Edge;

public interface CostFunction {

    int calculateCost(Edge edge);
}
```

---

## HeuristicService.java

```java
package com.sibam.graph.routing;

import com.sibam.graph.model.Node;
import org.springframework.stereotype.Service;

@Service
public class HeuristicService {

    public double estimate(Node current, Node goal) {

        double dx = current.getLat() - goal.getLat();
        double dy = current.getLon() - goal.getLon();

        return Math.sqrt(dx * dx + dy * dy);
    }
}
```

---

## PathResult.java

```java
package com.sibam.graph.routing;

import java.util.List;

public class PathResult {

    private final List<Integer> nodeIds;
    private final int totalCostSeconds;

    public PathResult(
            List<Integer> nodeIds,
            int totalCostSeconds
    ) {
        this.nodeIds = nodeIds;
        this.totalCostSeconds = totalCostSeconds;
    }

    public List<Integer> getNodeIds() {
        return nodeIds;
    }

    public int getTotalCostSeconds() {
        return totalCostSeconds;
    }
}
```

---

## AStarRouter.java

```java
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
```

---

# BOOTSTRAP

## GraphBootstrap.java

```java
package com.sibam.graph;

import com.sibam.graph.builder.GraphBuilder;
import com.sibam.graph.model.Graph;
import com.sibam.graph.storage.GraphSerializer;
import com.sibam.graph.storage.GraphStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class GraphBootstrap {

    private final GraphStore graphStore;
    private final GraphBuilder graphBuilder;
    private final GraphSerializer graphSerializer;

    public GraphBootstrap(
            GraphStore graphStore,
            GraphBuilder graphBuilder,
            GraphSerializer graphSerializer
    ) {
        this.graphStore = graphStore;
        this.graphBuilder = graphBuilder;
        this.graphSerializer = graphSerializer;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {

        Graph graph;

        if (graphSerializer.exists()) {
            graph = graphSerializer.load();
        } else {
            graph = graphBuilder.build();
            graphSerializer.save(graph);
        }

        graphStore.replaceGraph(graph);

        System.out.println("Graph initialized successfully.");
    }
}
```

---

# DODATNA PRIPOROČILA

## Maven dependency za Kryo

```xml
<dependency>
    <groupId>com.esotericsoftware</groupId>
    <artifactId>kryo</artifactId>
    <version>5.6.2</version>
</dependency>
```

---

# NASLEDNJI KORAKI

1. Implementacija pravega GraphBuilder-ja
2. Integracija GTFS stop_times
3. Dodajanje time-dependent edge-ov
4. RTree implementacija
5. Real-time MBajk state manager
6. Path reconstruction z edge segmenti
7. Multimodal routing heuristika
8. Cache invalidation strategija

