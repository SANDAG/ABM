package org.sandag.abm.active;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class RepeatedSingleSourceDijkstra<N extends Node,E extends Edge<N>,T extends Traversal<E>> implements ShortestPathStrategy<N> {
	private final Network<N,E,T> network;
	private final EdgeEvaluator<E> edgeEvaluator;
	private final TraversalEvaluator<T> traversalEvaluator;
	
	public RepeatedSingleSourceDijkstra(Network<N,E,T> network, EdgeEvaluator<E> edgeEvaluator, TraversalEvaluator<T> traversalEvaluator) {
		this.network = network;
		this.edgeEvaluator = edgeEvaluator;
		this.traversalEvaluator = traversalEvaluator;
	}
	
	private class TraversedEdge implements Comparable<TraversedEdge> {
		private final E edge;
		private final double cost;
		private final Path<N> path;
		
		private TraversedEdge(E edge, double cost, Path<N> path) {
			this.edge = edge;
			this.cost = cost;
			this.path = path;
		}
		
		public int compareTo(TraversedEdge other) {
			return Double.compare(cost,other.cost);
		}
	}

	@Override
	public ShortestPathResultSet<N> getShortestPaths(Set<N> originNodes, Set<N> destinationNodes) {
		return getShortestPaths(originNodes,destinationNodes,Double.POSITIVE_INFINITY);
	}

	@Override
	public ShortestPathResultSet<N> getShortestPaths(Set<N> originNodes, Set<N> destinationNodes, double maxCost) {
		ModifiableShortestPathResultSet<N> spResults = new BasicShortestPathResultSet<>();
		for (N originNode : originNodes)
			spResults.addAll(getShortestPaths(originNode,destinationNodes,maxCost));
		return spResults;
	}
	
	protected ShortestPathResultSet<N> getShortestPaths(N originNode, Set<N> destinationNodes, double maxCost) {
	    
	    BasicShortestPathResultSet<N> spResults = new BasicShortestPathResultSet<>();
		Map<E,Double> finalCosts = new HashMap<>();     //cost to (and including) edge
		
		PriorityQueue<TraversedEdge> traversalQueue = new PriorityQueue<>();
		
		Set<N> targets = new HashSet<>(destinationNodes);
		Path<N> basePath = new Path<>(originNode);
		
		// Don't remove origin node, and then we can force a circle for intrazonal trips
		//if (targets.contains(originNode)) {
		//	targets.remove(originNode);
		//	costs.put(originNode,0.0);
		//	paths.put(originNode,basePath);
		//}
		
		//initialize traversalQueue and costs
		for (N successor : network.getSuccessors(originNode)) {
			E edge = network.getEdge(originNode,successor);
			double edgeCost = edgeEvaluator.evaluate(edge);
			if (edgeCost < 0) {
			    throw new RuntimeException("Negative weight found for edge with fromNode " + edge.getFromNode().getId() + " and toNode " + edge.getToNode().getId() ); 
			}
			
			if (edgeCost < maxCost) {
				TraversedEdge traversedEdge = new TraversedEdge(edge,edgeCost,basePath.extendPath(successor));
				traversalQueue.add(traversedEdge);
			}
		}
		
		double traversalCost;
		
		//dijkstra
		while (!traversalQueue.isEmpty() && !targets.isEmpty()) {
			TraversedEdge traversedEdge = traversalQueue.poll();
			E edge = traversedEdge.edge;
			
			if (finalCosts.containsKey(edge)) //already considered
				continue;
			Path<N> path = traversedEdge.path;
			double cost = traversedEdge.cost;
			
			finalCosts.put(edge,cost);
			N fromNode = edge.getFromNode();
			N toNode = edge.getToNode();
			if (targets.remove(toNode)) {
			    spResults.addResult(new NodePair<N>(originNode,toNode),path,cost);
			}

			for (N successor : network.getSuccessors(toNode)) {
				if (successor.equals(fromNode))
					continue; //no u-turns will be allowed, so don't pollute heap
				T traversal = network.getTraversal(traversedEdge.edge,network.getEdge(toNode,successor));
				traversalCost = evaluateTraversalCost(traversal);
				if (traversalCost < 0) {
	                throw new RuntimeException("Negative weight found for traversal with start node " + traversal.getFromEdge().getFromNode().getId() + ", thru node " + traversal.getFromEdge().getToNode().getId() + ", and end node " + traversal.getToEdge().getToNode().getId() ); 
	            }
				traversalCost += cost;
				if (traversalCost < maxCost) 
					traversalQueue.add(new TraversedEdge(traversal.getToEdge(),traversalCost,path.extendPath(successor)));
			}
		}
		
		// Not returning null path references and infinite costs for nodes not found for possibility of insufficient memory
		
		return spResults;
	}
	
	protected double evaluateTraversalCost(T traversal) {
		return edgeEvaluator.evaluate(traversal.getToEdge()) + traversalEvaluator.evaluate(traversal);
	}

}
