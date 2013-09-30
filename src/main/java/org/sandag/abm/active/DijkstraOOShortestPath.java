package org.sandag.abm.active;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class DijkstraOOShortestPath implements ShortestPath {
	private final NetworkInterface network;
	private final TraversalEvaluator traversalEvaluator;
	
	public DijkstraOOShortestPath(NetworkInterface network, TraversalEvaluator traversalEvaluator) {
		this.network = network;
		this.traversalEvaluator = traversalEvaluator;
	}
	
	private class TraversedEdge implements Comparable<TraversedEdge> {
		private final Edge edge;
		private final double cost;
		private final Path path;
		
		private TraversedEdge(Edge edge, double cost, Path path) {
			this.edge = edge;
			this.cost = cost;
			this.path = path;
		}
		
		public int compareTo(TraversedEdge other) {
			return (int) Math.signum(cost - other.cost);
		}
	}

	@Override
	public ShortestPathResults getShortestPaths(Set<Node> originNodes, Set<Node> destinationNodes) {
		return getShortestPaths(originNodes,destinationNodes,Double.POSITIVE_INFINITY);
	}

	@Override
	public ShortestPathResults getShortestPaths(Set<Node> originNodes, Set<Node> destinationNodes, double maxCost) {
		ShortestPathResultsContainer spResults = new BasicShortestPathResults();
		for (Node originNode : originNodes)
			spResults.addAll(getShortestPaths(originNode,destinationNodes,maxCost));
		return spResults;
	}
	
	private ShortestPathResults getShortestPaths(Node originNode, Set<Node> destinationNodes, double maxCost) {
		//List<Node> orderedNodes = getOrderedNodeList();
		Map<Node,Path> paths = new HashMap<>();         //zone node paths
		Map<Node,Double> costs = new HashMap<>();       //zone node costs
		Map<Edge,Double> finalCosts = new HashMap<>();  //cost to (and including) edge
		Map<Edge,Path> tempPaths = new HashMap<>();     //shortest path to edge
		Map<Edge,Double> tempCosts = new HashMap<>();   //cost to (and including) edge
		
		PriorityQueue<TraversedEdge> traversalQueue = new PriorityQueue<>();
		
		Set<Node> targets = new HashSet<>(destinationNodes);
		Path basePath = new Path(originNode);
		if (targets.contains(originNode)) {
			targets.remove(originNode);
			costs.put(originNode,0.0);
			paths.put(originNode,basePath);
		}
		
		//initialize traversalQueue and costs
		for (Node successor : network.getSuccessors(originNode)) {
			Traversal traversal = new Traversal(originNode,successor);
			double traversalCost = evaluateTraversal(traversal);
			if (traversalCost <= maxCost) {
				TraversedEdge traversedEdge = new TraversedEdge(traversal.getToEdge(),traversalCost,basePath.extendPath(successor));
				traversalQueue.add(traversedEdge);
			}
		}
		
		//djikstra
		while (!traversalQueue.isEmpty() && !targets.isEmpty()) {
			TraversedEdge traversedEdge = traversalQueue.poll();
			Edge edge = traversedEdge.edge;
			
			if (finalCosts.containsKey(edge)) //already considered
				continue;
			Path path = traversedEdge.path;
			double cost = traversedEdge.cost;
			
			finalCosts.put(edge,cost);
			Node toNode = edge.getToNode();
			if (targets.remove(toNode)) {
				paths.put(toNode,path);
				costs.put(toNode,cost);
			}
			
			for (Node successor : network.getSuccessors(toNode)) {
				Traversal traversal = new Traversal(toNode,successor);
				double traversalCost = cost + evaluateTraversal(traversal);
				if (traversalCost <= maxCost) {
					traversedEdge = new TraversedEdge(traversal.getToEdge(),traversalCost,path.extendPath(successor));
					traversalQueue.add(traversedEdge);
				}
			}
		}
		
		BasicShortestPathResults spResults = new BasicShortestPathResults();
		for (Node destinationNode : destinationNodes) {
			boolean pathDefined = paths.containsKey(destinationNode);
			Path path = pathDefined ? paths.get(destinationNode) : null;
			double cost = pathDefined ? costs.get(destinationNode) : Double.POSITIVE_INFINITY;
			spResults.addResult(new NodePair(originNode,destinationNode),path,cost);
		}
		
		return spResults;
	}
	
	protected double evaluateTraversal(Traversal traversal) {
		return traversalEvaluator.evaluate(traversal);
	}

}
