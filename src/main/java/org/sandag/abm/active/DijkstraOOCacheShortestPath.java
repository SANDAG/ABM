package org.sandag.abm.active;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DijkstraOOCacheShortestPath<N extends Node,E extends Edge<N>,T extends Traversal<E>> extends DijkstraOOShortestPath<N,E,T> {
	private final ThreadLocal<Map<T,Double>> cachedCosts;

	public DijkstraOOCacheShortestPath(Network<N,E,T> network,PathElementEvaluator<E,T> traversalEvaluator) {
		super(network,traversalEvaluator);
		cachedCosts = new ThreadLocal<Map<T,Double>>() {
			protected Map<T,Double> initialValue() {
				return new HashMap<>();
			}
		};
	}
	
	protected double evaluateTraversalCost(T traversal) {
		Double value = cachedCosts.get().get(traversal);
		if (value == null) {
			value = super.evaluateTraversalCost(traversal);
			cachedCosts.get().put(traversal,value);
		}
		return value; 
	}
	
	protected ShortestPathResults<N> getShortestPaths(N originNode, Set<N> destinationNodes, double maxCost) {
		cachedCosts.get().clear();
		return super.getShortestPaths(originNode,destinationNodes,maxCost);
	}

}
