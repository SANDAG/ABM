package org.sandag.abm.active;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;


@SuppressWarnings("rawtypes") //ignore all of the Path[] arrays because we can't instantiate generic arrays
public class DijkstraArrayShortestPath<N extends Node> implements ShortestPath<N> {
	private final AdjacencyNetwork network;
	
	public <E extends Edge<N>,T extends Traversal<E>> DijkstraArrayShortestPath(Network<N,E,T> network, EdgeEvaluator<E> edgeEvaluator, TraversalEvaluator<T> traversalEvaluator) {
		this.network = new AdjacencyNetwork(network,edgeEvaluator,traversalEvaluator);
	}

	@Override
	public ShortestPathResults<N> getShortestPaths(Set<N> originNodes, Set<N> destinationNodes) {
		return getShortestPaths(originNodes,destinationNodes,Double.POSITIVE_INFINITY);
	}

	@Override
	public ShortestPathResults<N> getShortestPaths(Set<N> originNodes, Set<N> destinationNodes, double maxCost) {
		Set<N> unfinishedOrigins = new HashSet<N>(originNodes);
		ShortestPathResultsContainer<N> spResults = new BasicShortestPathResults<>();
		for (int i = 0; i < network.nodeIndices.length; i++) {
			if (originNodes.contains(network.nodeIndices[i])) {
				spResults.addAll(getShortestPaths(i,destinationNodes,maxCost));
				unfinishedOrigins.remove(network.nodeIndices[i]);
			}
		}
		if (!unfinishedOrigins.isEmpty())
			throw new IllegalStateException("Not all origins processed: " + unfinishedOrigins);
		return spResults;
	}
	
	private final ThreadLocal<double[]> finalCostsContainer = new ThreadLocal<double[]>() {
		@Override
		protected double[] initialValue() {
			return new double[network.edgeList.length];
		}
	};
	
	private final ThreadLocal<double[]> tempCostsContainer = new ThreadLocal<double[]>() {
		@Override
		protected double[] initialValue() {
			return new double[network.traversalFromList.length];
		}
	};
	
	private final ThreadLocal<Path[]> tempPathsContainer = new ThreadLocal<Path[]>() {
		@Override
		protected Path[] initialValue() {
			return new Path[network.traversalFromList.length];
		}
	};
	
	private ShortestPathResults<N> getShortestPaths(int nodeIndex, Set<N> destinationNodes, double maxCost) {
		Set<N> targetNodes = new TreeSet<>(destinationNodes);
		Map<N,Integer> targetIndices = new HashMap<>();
		int counter = 0;
		for (N targetNode : targetNodes)
			targetIndices.put(targetNode,counter++);
		Map<N,Integer> resultsIndices = new HashMap<>(targetIndices); //just a copy, for later
		
		Path[] paths = new Path[targetNodes.size()];
		double[] costs = new double[targetNodes.size()];
		double[] finalCosts = finalCostsContainer.get();
		Arrays.fill(finalCosts,Double.POSITIVE_INFINITY);
		Path[] tempPaths = tempPathsContainer.get();
		final double[] tempCosts = tempCostsContainer.get();
		
		PriorityQueue<Integer> traversalQueue = new PriorityQueue<>(network.traversalFromList.length,new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				//return (int) Math.signum(tempCosts[o1] - tempCosts[o2]);
				//return ((Double) tempCosts[o1]).compareTo(tempCosts[o2]);
				return Double.compare(tempCosts[o1],tempCosts[o2]);
			}
		});

		@SuppressWarnings("unchecked") //node indices only holds N
		Path<N> startPath = new Path<N>((N) network.nodeIndices[nodeIndex]);
		Map<Integer,Integer> targets = new HashMap<>();
		for (int i = 0; i < network.nodeIndices.length; i++) 
			if (targetNodes.contains(network.nodeIndices[i]))
				targets.put(i,targetIndices.get(network.nodeIndices[i]));
		if (targets.containsKey(nodeIndex)) {
			int centroidIndex = targets.remove(nodeIndex);
			costs[centroidIndex] = 0;
			paths[centroidIndex] = startPath;
		}
		
		//initialize traversalQueue and costs
		int endEdgePoint = network.fromNodeList[nodeIndex+1];
		for (int i = network.fromNodeList[nodeIndex]; i < endEdgePoint; i++) {
			int traversal = network.edgeList[i];
			double c = network.traversalCosts[traversal];
			if (c < maxCost) {
				tempCosts[traversal] = c;
				tempPaths[traversal] = startPath.extendPath((N) network.nodeIndices[network.toNodeList[network.traversalToList[traversal]]]);
				traversalQueue.add(traversal);
			}
		}
		//djikstra
		while (!traversalQueue.isEmpty() && !targets.isEmpty()) {
			int traversal = traversalQueue.poll();
			int edge = network.traversalToList[traversal];
			
			if (finalCosts[edge] < Double.POSITIVE_INFINITY) //already considered
				continue;
			
			double cost = tempCosts[traversal];
			if (cost == Double.POSITIVE_INFINITY)
				continue;
			finalCosts[edge] = cost;
			int toNode = network.toNodeList[edge];
			if (targets.containsKey(toNode)) {
			    int centroidNode = targets.remove(toNode); 
				paths[centroidNode] = tempPaths[traversal];
				costs[centroidNode] = cost;
			}
			
			int startEdgePoint = network.edgeList[edge] + 1; //skip the untraversed link (where origin is starting point)
			endEdgePoint = network.edgeList[edge+1];
			for (int i = startEdgePoint; i < endEdgePoint; i++) {
				double c = finalCosts[edge] + network.traversalCosts[i];
				if (c < maxCost) {
					tempCosts[i] = c;
					tempPaths[i] = new Path<N>((Path<N>) tempPaths[traversal],(N) network.nodeIndices[network.toNodeList[network.traversalToList[i]]]);
					traversalQueue.add(i);
				}
			}
		}
		
		BasicShortestPathResults<N> spResults = new BasicShortestPathResults<N>();
		@SuppressWarnings("unchecked") //node indices only holds N
		N originNode = (N) network.nodeIndices[nodeIndex];
		for (N destinationNode : destinationNodes) {
			int index = resultsIndices.get(destinationNode);
			spResults.addResult(new NodePair<N>(originNode,destinationNode),(Path<N>) paths[index],costs[index]);
		}
		
		return spResults;
	}
	
	private class AdjacencyNetwork {
		final Node[] nodeIndices;
		final int[] fromNodeList;      //# of nodes -> points to position in edgeList/toNodeList it is from node for
		final int[] edgeList;          //# of edges -> points to position in toEdgeList it is the start edge for
		final int[] toNodeList;        //# of edges -> the to node for edge in edgeList
		final int[] traversalFromList; //# of edges + # of edge pairs -> points to the from node (fromNodeList) in the traversal
		final int[] traversalToList;   //# of edges + # of edge pairs -> points to the to node (toNodeList point) in the traversal
		final double[] traversalCosts; //# of edge pairs -> the traversal cost (with edge cost added)
		
		<E extends Edge<N>,T extends Traversal<E>> 
		AdjacencyNetwork(Network<N,E,T> network, EdgeEvaluator<E> edgeEvaluator, TraversalEvaluator<T> traversalEvaluator) {
			Iterator<N> nodeIterator = network.nodeIterator();
			Map<N,Integer> nodes = new LinkedHashMap<>();
			int counter = 0;
			while (nodeIterator.hasNext())
				nodes.put(nodeIterator.next(),counter++);
			int nodeCount = nodes.size();
			nodeIndices = new Node[nodeCount];
			for (Node n : nodes.keySet())
				nodeIndices[nodes.get(n)] = n;

			Iterator<E> edgeIterator = network.edgeIterator();
			int edgeCount = 0;
			while (edgeIterator.hasNext()) {
				edgeIterator.next();
				edgeCount++;
			}

			fromNodeList = new int[nodeCount+1];
			fromNodeList[nodeCount] = edgeCount;
			edgeList = new int[edgeCount+1];
			toNodeList = new int[edgeCount];
			Map<NodePair<N>,Integer> edgePositions = new HashMap<>();
			
			int edgeCounter = 0;
			for (int f = 0; f < nodeIndices.length; f++) {
				@SuppressWarnings("unchecked") //this is a correct cast
				N fromNode = (N) nodeIndices[f];
				fromNodeList[f] = edgeCounter;
				for (N toNode : network.getSuccessors(fromNode)) {
					int edgeIndex = edgeCounter++;
					NodePair<N> nodePair = new NodePair<>(fromNode,toNode);
					toNodeList[edgeIndex] = nodes.get(toNode);
					edgePositions.put(nodePair,edgeIndex);
				}
			}
			//build traversals
			List<int[]> traversals = new LinkedList<>();
			List<Double> costs = new LinkedList<>();
			edgeCounter = 0;
			for (int f = 0; f < nodeIndices.length; f++) {
				for (int toIndex = fromNodeList[f]; toIndex < fromNodeList[f+1]; toIndex++) {
					int t = toNodeList[toIndex];
					edgeList[toIndex] = edgeCounter++;
					@SuppressWarnings("unchecked") //these casts are correct
					NodePair<N> nodePair = new NodePair<>((N) nodeIndices[f],(N) nodeIndices[t]);
					traversals.add(new int[] {f,edgePositions.get(nodePair)});
					E fromEdge = network.getEdge(nodePair);
					costs.add(edgeEvaluator.evaluate(fromEdge));
					for (int endIndex = fromNodeList[t]; endIndex < fromNodeList[t+1]; endIndex++) {
						int e = toNodeList[endIndex];
						nodePair = new NodePair<>((N) nodeIndices[t],(N) nodeIndices[e]);
						E toEdge = network.getEdge(nodePair);
						traversals.add(new int[] {t,edgePositions.get(nodePair)});
						costs.add(edgeEvaluator.evaluate(toEdge) + traversalEvaluator.evaluate(network.getTraversal(fromEdge,toEdge)));
						edgeCounter++;
					}
				}
			}
			traversalFromList = new int[traversals.size()];
			traversalToList = new int[traversals.size()];
			counter = 0;
			for (int[] traversal : traversals) {
				traversalFromList[counter] = traversal[0];
				traversalToList[counter] = traversal[1];
				counter++;
			}
			traversalCosts = new double[costs.size()];
			counter = 0;
			for (double traversalCost : costs)
				traversalCosts[counter++] = traversalCost;
			edgeList[edgeCount] = traversalCosts.length;
		}

	}
	
}
