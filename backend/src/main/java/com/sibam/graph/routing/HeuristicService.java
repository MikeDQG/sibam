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
