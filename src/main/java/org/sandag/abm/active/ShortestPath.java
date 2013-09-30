package org.sandag.abm.active;

import java.util.List;
import java.util.Set;


public interface ShortestPath {
	ShortestPathResults getShortestPaths(Set<Node> originNodes, Set<Node> destinationNodes, double maxCost);
	ShortestPathResults getShortestPaths(Set<Node> originNodes, Set<Node> destinationNodes);
}
