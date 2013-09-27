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

public class DijkstraOOShortestPath extends AbstractShortestPath {
	private final NetworkInterface network;
	private final TraversalEvaluator traversalEvaluator;
	
	public DijkstraOOShortestPath(NetworkInterface network, TraversalEvaluator traversalEvaluator) {
		super(network);
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
	
	public OriginShortestPathResult getShortestPaths(Node originNode, double maxCost) {
		List<Node> orderedNodes = getOrderedNodeList();
		Map<Node,Path> paths = new HashMap<>();         //zone node paths
		Map<Node,Double> costs = new HashMap<>();       //zone node costs
		Map<Edge,Double> finalCosts = new HashMap<>();  //cost to (and including) edge
		Map<Edge,Path> tempPaths = new HashMap<>();     //shortest path to edge
		Map<Edge,Double> tempCosts = new HashMap<>();   //cost to (and including) edge
		
		PriorityQueue<TraversedEdge> traversalQueue = new PriorityQueue<>();
		
		Set<Node> targets = new HashSet<>(orderedNodes);
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
		Path[] pathsArray = new Path[orderedNodes.size()];
		double[] costsArray = new double[orderedNodes.size()];
		int counter = 0;
		for (Node node : orderedNodes) {
			if (paths.containsKey(node)) {
				pathsArray[counter] = paths.get(node);
				costsArray[counter] = costs.get(node);
			} else {
				costsArray[counter] = Double.POSITIVE_INFINITY;
			}
			counter++;
		}
		return new OriginShortestPathResult(pathsArray,costsArray);
	}
	
	protected double evaluateTraversal(Traversal traversal) {
		return traversalEvaluator.evaluate(traversal);
	}

}
