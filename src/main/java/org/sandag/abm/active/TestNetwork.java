package org.sandag.abm.active;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mahout.math.Arrays;
import org.sandag.abm.active.ParallelShortestPath.ParallelMethod;

import com.pb.sawdust.util.concurrent.DnCRecursiveAction;

public class TestNetwork implements NetworkInterface {
	private final Set<Node> nodes;
	private final Set<Node> centroids;
	private final Map<Node,List<Node>> successors;
	private final Map<Edge,Double> edgeCosts;
	
	public static enum TestNetworkType {
		ADJACENCY_COST,
		AB_COST
	}
	
	public TestNetwork(java.nio.file.Path file, TestNetworkType networkType, double sampleFraction) {
		nodes = new TreeSet<>();
		successors = new HashMap<>();
		edgeCosts = new HashMap<>();
		centroids = new TreeSet<>();
		Map<Integer,Node> nodeSet = new HashMap<>();
		switch (networkType) {
			case ADJACENCY_COST : {
				int counter = 0;
				try (Scanner reader = new Scanner(file.toFile())) {
					while (reader.hasNext()) {
						String line = reader.nextLine();
						String[] splitLine = line.split("[\\t,]");
						int nodeId = Integer.parseInt(splitLine[0]);
						if (!nodeSet.containsKey(nodeId)) {
							Node n = new Node(nodeId); 
							nodeSet.put(nodeId,n);
							successors.put(n,new LinkedList<Node>());
							nodes.add(n);
							centroids.add(n);
						}
						Node f = nodeSet.get(nodeId);
						for (int i = 1; i < splitLine.length; i += 2) {
							nodeId = Integer.parseInt(splitLine[i]);
							double cost = Double.parseDouble(splitLine[i+1]);
							if (!nodeSet.containsKey(nodeId)) {
								Node n = new Node(nodeId); 
								nodeSet.put(nodeId,n);
								successors.put(n,new LinkedList<Node>());
								nodes.add(n);
								centroids.add(n);
							}
							Node t = nodeSet.get(nodeId);
							edgeCosts.put(new Edge(f,t),cost);
							successors.get(f).add(t);
						}
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			} break;
			case AB_COST : {
				Random random = new Random(20); //fixed seed so we can compare results
				boolean first = true;
				try (Scanner reader = new Scanner(file.toFile())) {
					while (reader.hasNext()) {
						String line = reader.nextLine();
						if (first) {
							first = false;
							continue;
						}
						String[] splitLine = line.split(",");
						int fromNode = Integer.parseInt(splitLine[0]);
						int toNode = Integer.parseInt(splitLine[1]);
						double cost = Double.parseDouble(splitLine[2]);
						boolean fmaz = Integer.parseInt(splitLine[3]) == 1;
						boolean ftaz = Integer.parseInt(splitLine[4]) == 1;
						boolean tmaz = Integer.parseInt(splitLine[5]) == 1;
						boolean ttaz = Integer.parseInt(splitLine[6]) == 1;
						
						if (!nodeSet.containsKey(fromNode)) {
							Node n = new Node(fromNode); 
							nodeSet.put(fromNode,n);
							successors.put(n,new LinkedList<Node>());
							nodes.add(n);
							if (ftaz && (random.nextDouble() < sampleFraction)) 
								centroids.add(n);
						}
						Node f = nodeSet.get(fromNode);
						if (!nodeSet.containsKey(toNode)) {
							Node n = new Node(toNode); 
							nodeSet.put(toNode,n);
							successors.put(n,new LinkedList<Node>());
							nodes.add(n);
							if (ttaz && (random.nextDouble() < sampleFraction))
								centroids.add(n);
						}
						Node t = nodeSet.get(toNode);
						edgeCosts.put(new Edge(f,t),cost);
						successors.get(f).add(t);
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Override
	public Iterator<Node> nodeIterator() {
        return nodes.iterator();
	}

	@Override
	public Iterator<Node> centroidIterator() {
		return centroids.iterator();
	}

	@Override
	public Iterator<Edge> edgeIterator() {
		return edgeCosts.keySet().iterator();
	}

	@Override
	public List<Node> getSuccessors(Node node) {
		return successors.get(node);
	}
	
	public double getEdgeCost(Edge edge) {
		return edgeCosts.get(edge);
	}
	
	private static void printSubarray(int[] array, int count) {
		StringBuilder sb = new StringBuilder();
		sb.append("[").append(array[0]);
		for (int i = 1; i < count; i++)
			sb.append(",").append(array[i]);
		sb.append(",...]");
		System.out.println(sb);
	}
	
	private static void printSubarray(double[] array, int count) {
		StringBuilder sb = new StringBuilder();
		sb.append("[").append(array[0]);
		for (int i = 1; i < count; i++)
			sb.append(",").append(array[i]);
		sb.append(",...]");
		System.out.println(sb);
	}
	
	public static void main(String ... args) {
		double sampleFraction = 0.01;
		double maxCost = 30*5280;
		System.out.print("reading network...");
//		final NetworkInterface network = new TestNetwork(Paths.get("D:/projects/sandag/sp/dijkstraData.txt"),TestNetworkType.ADJACENCY_COST,sampleFraction);
		final NetworkInterface network = new TestNetwork(Paths.get("D:/projects/sandag/sp/mtc_final_network.csv"),TestNetworkType.AB_COST,sampleFraction);
		System.out.println("done");
		
		TraversalEvaluator traversalEvaluator = new TraversalEvaluator() {
			private final TestNetwork n = (TestNetwork) network;
			private final Set<Node> centroids = new HashSet<>();
			{
				Iterator<Node> centroidIterator = network.centroidIterator();
				while (centroidIterator.hasNext())
					centroids.add(centroidIterator.next());
			}
			
			@Override
			public double evaluate(Traversal traversal) {
				Edge fromEdge = traversal.getFromEdge();
				if (fromEdge != null && centroids.contains(fromEdge.getToNode()))
					return Double.POSITIVE_INFINITY;
				return n.getEdgeCost(traversal.getToEdge());
			}
		};
		//for (ShortestPath sp : new ShortestPath[] {sp1,sp2,sp3}) {
		//for (int sptype : new int[] {1,2,3}) {
		for (int sptype : new int[] {1}) {
			long time = System.currentTimeMillis();
			ShortestPath sp = null;
			if (sptype == 1)
				sp = new DijkstraArrayShortestPath(network,traversalEvaluator);
			else if (sptype == 2)
				sp = new DijkstraOOShortestPath(network,traversalEvaluator);
			else if (sptype == 3)
				sp = new DijkstraOOCacheShortestPath(network,traversalEvaluator);

			Set<Node> originNodes = new LinkedHashSet<>();
			Iterator<Node> centroidIterator = network.centroidIterator();
			while (centroidIterator.hasNext())
				originNodes.add(centroidIterator.next());
			System.out.println("origins to process: " + originNodes.size());
//			ShortestPath psp = new ParallelShortestPath(sp,ParallelMethod.FORK_JOIN);
			ShortestPath psp = new ParallelShortestPath(sp,ParallelMethod.QUEUE);
			ShortestPathResults spResults = psp.getShortestPaths(originNodes,originNodes,maxCost);
			
			System.out.println("Time to run: " + (((double) (System.currentTimeMillis() - time)) / 1000));
//			for (int i : new int[] {7,37,59,82,99,115,133,165,188,197}) 
//				System.out.println(i + " : " + spResults.getShortestPathResult(new NodePair(new Node(1),new Node(i))).getCost());
//			System.out.println(spResults.getShortestPathResult(new NodePair(new Node(1),new Node(7))).getPath().getPathString());
		}
	}

}
