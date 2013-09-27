package org.sandag.abm.active;

import java.util.HashMap;
import java.util.Map;

public class DijkstraOOCacheShortestPath extends DijkstraOOShortestPath {
	private final Map<Traversal,Double> cachedCosts;

	public DijkstraOOCacheShortestPath(NetworkInterface network,TraversalEvaluator traversalEvaluator) {
		super(network, traversalEvaluator);
		cachedCosts = new HashMap<>();
	}
	
	protected double evaluateTraversal(Traversal traversal) {
		if (!cachedCosts.containsKey(traversal))
			cachedCosts.put(traversal,super.evaluateTraversal(traversal));
		return cachedCosts.get(traversal);
	}

}
