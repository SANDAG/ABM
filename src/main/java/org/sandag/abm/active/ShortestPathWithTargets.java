package org.sandag.abm.active;

import java.util.*;

public interface ShortestPathWithTargets<N extends Node> {
	ShortestPathResults<N> getShortestPaths(Map<N,HashSet<N>> originDestinationMap, double maxCost);
	ShortestPathResults<N> getShortestPaths(Map<N,HashSet<N>> originDestinationMap);
}
