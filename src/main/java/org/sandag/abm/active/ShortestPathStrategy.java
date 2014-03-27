package org.sandag.abm.active;

import java.util.Set;

public interface ShortestPathStrategy<N extends Node> {
	ShortestPathResultSet<N> getShortestPaths(Set<N> originNodes, Set<N> destinationNodes, double maxCost);
	ShortestPathResultSet<N> getShortestPaths(Set<N> originNodes, Set<N> destinationNodes);
}
