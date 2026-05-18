package com.sibam.graph.spatial;

import com.sibam.graph.model.Node;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class RTreeIndex {

    private static final int MAX_CHILDREN = 16;

    private TreeNode root;

    public void build(List<Node> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            root = null;
            return;
        }

        List<TreeNode> level = nodes.stream()
                .map(TreeNode::leaf)
                .sorted(Comparator.comparingDouble(TreeNode::centerLon)
                        .thenComparingDouble(TreeNode::centerLat))
                .toList();

        boolean sortByLon = false;
        while (level.size() > 1) {
            List<TreeNode> sorted = new ArrayList<>(level);
            Comparator<TreeNode> comparator = sortByLon
                    ? Comparator.comparingDouble(TreeNode::centerLon).thenComparingDouble(TreeNode::centerLat)
                    : Comparator.comparingDouble(TreeNode::centerLat).thenComparingDouble(TreeNode::centerLon);
            sorted.sort(comparator);

            List<TreeNode> parents = new ArrayList<>();
            for (int i = 0; i < sorted.size(); i += MAX_CHILDREN) {
                parents.add(TreeNode.parent(sorted.subList(i, Math.min(i + MAX_CHILDREN, sorted.size()))));
            }

            level = parents;
            sortByLon = !sortByLon;
        }

        root = level.getFirst();
    }

    public List<Node> nearest(double lat, double lon, int limit) {
        if (root == null || limit <= 0) {
            return List.of();
        }

        PriorityQueue<SearchEntry> queue = new PriorityQueue<>(Comparator.comparingDouble(SearchEntry::distanceMeters));
        PriorityQueue<NodeDistance> best = new PriorityQueue<>(
                Comparator.comparingDouble(NodeDistance::distanceMeters).reversed()
        );

        queue.add(new SearchEntry(root, minDistanceToBoundsMeters(root, lat, lon)));

        while (!queue.isEmpty()) {
            SearchEntry entry = queue.poll();
            if (best.size() == limit && entry.distanceMeters() >= best.peek().distanceMeters()) {
                break;
            }

            TreeNode treeNode = entry.treeNode();
            if (treeNode.node != null) {
                addCandidate(best, new NodeDistance(treeNode.node, haversineMeters(
                        lat,
                        lon,
                        treeNode.node.getLat(),
                        treeNode.node.getLon()
                )), limit);
                continue;
            }

            for (TreeNode child : treeNode.children) {
                double minDistance = minDistanceToBoundsMeters(child, lat, lon);
                if (best.size() < limit || minDistance < best.peek().distanceMeters()) {
                    queue.add(new SearchEntry(child, minDistance));
                }
            }
        }

        List<NodeDistance> nearest = new ArrayList<>(best);
        nearest.sort(Comparator.comparingDouble(NodeDistance::distanceMeters));
        return nearest.stream().map(NodeDistance::node).toList();
    }

    private void addCandidate(PriorityQueue<NodeDistance> best, NodeDistance candidate, int limit) {
        if (best.size() < limit) {
            best.add(candidate);
            return;
        }

        if (candidate.distanceMeters() < best.peek().distanceMeters()) {
            best.poll();
            best.add(candidate);
        }
    }

    private double minDistanceToBoundsMeters(TreeNode node, double lat, double lon) {
        double nearestLat = clamp(lat, node.minLat, node.maxLat);
        double nearestLon = clamp(lon, node.minLon, node.maxLon);
        return haversineMeters(lat, lon, nearestLat, nearestLon);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusM = 6_371_000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(rLat1) * Math.cos(rLat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusM * c;
    }

    private static class TreeNode {
        private final Node node;
        private final List<TreeNode> children;
        private final double minLat;
        private final double maxLat;
        private final double minLon;
        private final double maxLon;

        private TreeNode(Node node, List<TreeNode> children, double minLat, double maxLat, double minLon, double maxLon) {
            this.node = node;
            this.children = children;
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLon = minLon;
            this.maxLon = maxLon;
        }

        private static TreeNode leaf(Node node) {
            return new TreeNode(node, List.of(), node.getLat(), node.getLat(), node.getLon(), node.getLon());
        }

        private static TreeNode parent(List<TreeNode> children) {
            double minLat = Double.POSITIVE_INFINITY;
            double maxLat = Double.NEGATIVE_INFINITY;
            double minLon = Double.POSITIVE_INFINITY;
            double maxLon = Double.NEGATIVE_INFINITY;

            for (TreeNode child : children) {
                minLat = Math.min(minLat, child.minLat);
                maxLat = Math.max(maxLat, child.maxLat);
                minLon = Math.min(minLon, child.minLon);
                maxLon = Math.max(maxLon, child.maxLon);
            }

            return new TreeNode(null, List.copyOf(children), minLat, maxLat, minLon, maxLon);
        }

        private double centerLat() {
            return (minLat + maxLat) / 2.0;
        }

        private double centerLon() {
            return (minLon + maxLon) / 2.0;
        }
    }

    private record SearchEntry(TreeNode treeNode, double distanceMeters) {
    }

    private record NodeDistance(Node node, double distanceMeters) {
    }
}
