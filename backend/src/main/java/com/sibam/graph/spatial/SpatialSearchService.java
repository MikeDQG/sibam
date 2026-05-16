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
