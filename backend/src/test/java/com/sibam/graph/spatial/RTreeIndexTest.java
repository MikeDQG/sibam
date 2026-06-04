package com.sibam.graph.spatial;

import com.sibam.graph.model.BusNode;
import com.sibam.graph.model.Node;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class RTreeIndexTest {

    private final RTreeIndex index = new RTreeIndex();

    @Test
    void nearestOnEmptyIndexReturnsEmptyList() {
        assertThat(index.nearest(46.55, 15.64, 1)).isEmpty();
    }

    @Test
    void buildWithNullNodesLeavesIndexEmpty() {
        index.build(null);
        assertThat(index.nearest(46.55, 15.64, 1)).isEmpty();
    }

    @Test
    void buildWithEmptyListLeavesIndexEmpty() {
        index.build(List.of());
        assertThat(index.nearest(46.55, 15.64, 1)).isEmpty();
    }

    @Test
    void singleNodeIsReturnedAsNearest() {
        Node node = new BusNode(1, 46.55, 15.64, "Only");
        index.build(List.of(node));

        List<Node> result = index.nearest(46.55, 15.64, 1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1);
    }

    @Test
    void returnsClosestOfTwoNodes() {
        Node close = new BusNode(1, 46.550, 15.640, "Close");
        Node far   = new BusNode(2, 46.560, 15.650, "Far");
        index.build(List.of(close, far));

        List<Node> result = index.nearest(46.551, 15.641, 1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1);
    }

    @Test
    void limitIsRespected() {
        index.build(List.of(
                new BusNode(1, 46.550, 15.640, "A"),
                new BusNode(2, 46.551, 15.641, "B"),
                new BusNode(3, 46.560, 15.650, "C")
        ));

        assertThat(index.nearest(46.550, 15.640, 2)).hasSize(2);
    }

    @Test
    void zeroLimitReturnsEmptyList() {
        index.build(List.of(new BusNode(1, 46.55, 15.64, "A")));
        assertThat(index.nearest(46.55, 15.64, 0)).isEmpty();
    }

    @Test
    void resultsAreOrderedNearestFirst() {
        Node n1 = new BusNode(1, 46.550, 15.640, "Nearest");
        Node n2 = new BusNode(2, 46.553, 15.643, "Middle");
        Node n3 = new BusNode(3, 46.560, 15.650, "Furthest");
        index.build(List.of(n1, n2, n3));

        List<Node> result = index.nearest(46.550, 15.640, 3);

        assertThat(result.get(0).getId()).isEqualTo(1);
        assertThat(result.get(2).getId()).isEqualTo(3);
    }

    @Test
    void worksCorrectlyWithMoreThanMaxChildrenNodes() {
        // 20 nodes forces multi-level tree (MAX_CHILDREN = 16)
        List<Node> nodes = IntStream.rangeClosed(1, 20)
                .mapToObj(i -> (Node) new BusNode(i, 46.0 + i * 0.001, 15.0 + i * 0.001, "Stop " + i))
                .toList();
        index.build(nodes);

        List<Node> result = index.nearest(46.001, 15.001, 1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1);
    }

    @Test
    void rebuildReplacesOldIndex() {
        index.build(List.of(new BusNode(1, 46.550, 15.640, "Old")));
        index.build(List.of(new BusNode(2, 46.560, 15.650, "New")));

        List<Node> result = index.nearest(46.560, 15.650, 1);

        assertThat(result.get(0).getId()).isEqualTo(2);
    }
}
