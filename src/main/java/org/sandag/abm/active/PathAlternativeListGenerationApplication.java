package org.sandag.abm.active;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class PathAlternativeListGenerationApplication<N extends Node, E extends Edge<N>, T extends Traversal<E>>
{
    PathAlternativeListGenerationConfiguration<N,E,T> configuration;
    Network<N,E,T> network;
    Map<Integer,Map<Integer,Double>> nearbyZonalDistanceMap;
    Map<Integer,Integer> zonalCentroidIdMap;
    double[] pathSizeDistanceBreaks;
    double[] pathSizeOversampleTargets;
    double[] randomizationLowerBounds;
    double[] randomizationUpperBounds;
    int maxRandomizationIters;
    boolean randomCostSeeded;
    EdgeEvaluator<E> pathSizeOverlapEvaluator;
    EdgeEvaluator<E> edgeCostEvaluator;
    TraversalEvaluator<T> traversalCostEvaluator;
    double maxCost;
    
    private static final int ORIGIN_PROGRESS_REPORT_COUNT = 100;
    
    public PathAlternativeListGenerationApplication(PathAlternativeListGenerationConfiguration<N,E,T> configuration) {
        this.configuration = configuration;
        this.network = configuration.getNetwork();
        this.nearbyZonalDistanceMap = configuration.getNearbyZonalDistanceMap();
        this.zonalCentroidIdMap = configuration.getZonalCentroidIdMap();
        this.pathSizeDistanceBreaks = configuration.getPathSizeDistanceBreaks();
        this.pathSizeOversampleTargets = configuration.getPathSizeOversampleTargets();
        this.randomizationLowerBounds = configuration.getRandomizationLowerBounds();
        this.randomizationUpperBounds = configuration.getRandomizationUpperBounds();
        this.maxRandomizationIters = configuration.getMaxRandomizationIters();
        this.randomCostSeeded = configuration.isRandomCostSeeded();
        this.pathSizeOverlapEvaluator = configuration.getPathSizeOverlapEvaluator();
        this.edgeCostEvaluator = configuration.getEdgeCostEvaluator();
        this.traversalCostEvaluator = configuration.getTraversalCostEvaluator();
        this.maxCost = configuration.getMaxCost();
    }

    public Map<NodePair<N>,PathAlternativeList<N,E>> generateAlternativeLists(int zone) throws DestinationNotFoundException
    {      
        int threadCount = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final ConcurrentHashMap<NodePair<N>,PathAlternativeList<N,E>> alternativeLists = new ConcurrentHashMap<NodePair<N>,PathAlternativeList<N,E>>();
        final Queue<Integer> originQueue = new ConcurrentLinkedQueue<>(zonalCentroidIdMap.keySet());
        final Queue<DestinationNotFoundException> exceptionQueue = new ConcurrentLinkedQueue<>();
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger counter = new AtomicInteger();
        for (int i = 0; i < threadCount; i++)
            executor.execute(new GenerationTask(originQueue,alternativeLists,counter,latch,exceptionQueue));
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        executor.shutdown();
        
        while ( exceptionQueue.size() > 0 )
        {
            throw exceptionQueue.poll();
        }
        
        return alternativeLists;
    }
    
    private int findFirstIndexGreaterThan(double value, double[] array)
    {
        int index = 0;
        double current = 0.0;
        while ( current <= value && index < (array.length - 1) ) {
            index += 1;
            current = array[index];
        }
        return index;
    }
    
    private class GenerationTask implements Runnable
    {
        private final Queue<Integer> originQueue;
        private final AtomicInteger counter;
        private final CountDownLatch latch;
        private final ConcurrentHashMap<NodePair<N>, PathAlternativeList<N,E>> alternativeLists;
        private final Queue<DestinationNotFoundException> exceptionQueue;
        
        private GenerationTask(Queue<Integer> originQueue, ConcurrentHashMap<NodePair<N>, PathAlternativeList<N,E>> alternativeLists, AtomicInteger counter, CountDownLatch latch, Queue<DestinationNotFoundException> exceptionQueue)
        {
            this.originQueue = originQueue;
            this.counter = counter;
            this.latch = latch;
            this.alternativeLists = alternativeLists;
            this.exceptionQueue = exceptionQueue;
        }
        
        @Override
        public void run()
        {
            Set<N> singleOriginNode;
            Set<N> destinationNodes;
            Map<N,Integer> destinationZoneMap;
            Map<N,Double> destinationDistanceMap;
            Map<N,Double> destinationPathSizeMap;
            
            while ( originQueue.size() > 0 ) {
                int origin = originQueue.poll();
                
                singleOriginNode = new HashSet<N>();
                singleOriginNode.add(network.getNode(zonalCentroidIdMap.get(origin)));
                destinationNodes = new HashSet<N>();
                destinationZoneMap = new HashMap<N,Integer>();
                destinationDistanceMap = new HashMap<N,Double>();
                destinationPathSizeMap = new HashMap<N,Double>();
                N destinationNode;
                for (int destination : nearbyZonalDistanceMap.get(origin).keySet()) {
                    destinationNode = network.getNode(zonalCentroidIdMap.get(destination));
                    destinationNodes.add(destinationNode);
                    destinationDistanceMap.put(destinationNode, nearbyZonalDistanceMap.get(origin).get(destination));
                    destinationZoneMap.put(destinationNode, destination);
                    destinationPathSizeMap.put(destinationNode, pathSizeOversampleTargets[findFirstIndexGreaterThan(destinationDistanceMap.get(destinationNode), configuration.getPathSizeDistanceBreaks())]);
                }
                
                int iterCount = 0;
                EdgeEvaluator<E> randomizedEdgeCost;
                TraversalEvaluator<T> randomizedTraversalCost;
                ShortestPathStrategy<N> shortestPathStrategy;
                ShortestPathResultSet<N> result;
                while( iterCount < maxRandomizationIters && destinationNodes.size() > 0 ) {
                    
                    double lower = randomizationLowerBounds[Math.min(iterCount,randomizationLowerBounds.length-1)];
                    double upper = randomizationUpperBounds[Math.min(iterCount,randomizationUpperBounds.length-1)];
   
                    if ( randomCostSeeded ) { 
                        randomizedEdgeCost = new RandomizedEdgeCost(edgeCostEvaluator,lower, upper, Objects.hash(origin,iterCount));
                        randomizedTraversalCost = new RandomizedTraversalCost(traversalCostEvaluator, lower, upper, Objects.hash(origin,iterCount) + 1);
                    } else {
                        randomizedEdgeCost = new RandomizedEdgeCost(edgeCostEvaluator,lower, upper);
                        randomizedTraversalCost = new RandomizedTraversalCost(traversalCostEvaluator, lower, upper);
                    }
                        
                    shortestPathStrategy = new RepeatedSingleSourceDijkstra<N,E,T>(network, randomizedEdgeCost, randomizedTraversalCost);                    
                    result = shortestPathStrategy.getShortestPaths(singleOriginNode,destinationNodes,maxCost);
                    
                    for (NodePair<N> odPair : result) {
                        if ( ! alternativeLists.containsKey(odPair) ) { alternativeLists.put(odPair,new PathAlternativeList<N,E>(odPair, network, pathSizeOverlapEvaluator)); }
                        alternativeLists.get(odPair).add(result.getShortestPathResult(odPair).getPath());
                        if ( alternativeLists.get(odPair).getSizeMeasureTotal() > destinationPathSizeMap.get(odPair.getToNode()) ) {
                            destinationNodes.remove(odPair.getToNode());
                            alternativeLists.get(odPair).clearPathSizeCalculator();
                        }
                    }
                    
                    iterCount++;
                }
                
                if ( destinationNodes.size() > 0 ) {
                    String message = "Total path sizes of sample insufficient for origin zone" + origin + "and destination zones ";
                    for (N node : destinationNodes) {
                        message = message + node.getId() + " ";
                    }
                    exceptionQueue.add(new DestinationNotFoundException(message));
                }
                
                int c = counter.addAndGet(1); 
                if (c % ORIGIN_PROGRESS_REPORT_COUNT == 0) { System.out.println("   done with " + c + " origins"); }
            }
            latch.countDown();
        }  
    }
 

    private class RandomizedEdgeCost implements EdgeEvaluator<E>
    {
        private EdgeEvaluator<E> edgeCostEvaluator;
        private Random random;
        private double lower, upper;
        
        public RandomizedEdgeCost(EdgeEvaluator<E> edgeCostEvaluator, double lower, double upper)
        {
            this.edgeCostEvaluator = edgeCostEvaluator;
            this.lower = lower;
            this.upper = upper;
            random = new Random();
        }
        
        public RandomizedEdgeCost(EdgeEvaluator<E> edgeCostEvaluator, double lower, double upper, long seed)
        {
            this.edgeCostEvaluator = edgeCostEvaluator;
            this.lower = lower;
            this.upper = upper;
            random = new Random(seed);
        }

        @Override
        public double evaluate(E edge)
        {
            return edgeCostEvaluator.evaluate(edge) * ( lower + random.nextDouble() * (upper - lower) );
        }
        
    }
    
    private class RandomizedTraversalCost implements TraversalEvaluator<T>
    {
        private TraversalEvaluator<T> traversalCostEvaluator;
        private Random random;
        private double lower, upper;
        
        public RandomizedTraversalCost(TraversalEvaluator<T> traversalCostEvaluator, double lower, double upper)
        {
            this.traversalCostEvaluator = traversalCostEvaluator;
            this.lower = lower;
            this.upper = upper;
            random = new Random();
        }
        
        public RandomizedTraversalCost(TraversalEvaluator<T> traversalCostEvaluator, double lower, double upper, long seed)
        {
            this.traversalCostEvaluator = traversalCostEvaluator;
            this.lower = lower;
            this.upper = upper;
            random = new Random(seed);
        }

        @Override
        public double evaluate(T traversal)
        {
            return traversalCostEvaluator.evaluate(traversal) * ( lower + random.nextDouble() * (upper - lower) );
        }
        
    }
}

