package org.sandag.abm.active;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;

public abstract class AbstractPathChoiceEdgeAssignmentApplication<N extends Node, E extends Edge<N>, T extends Traversal<E>>
{
    private static final Logger logger = Logger.getLogger(AbstractPathChoiceEdgeAssignmentApplication.class);
    
    protected PathAlternativeListGenerationConfiguration<N,E,T> configuration;
    private boolean randomCostSeeded;
    private double maxCost;
    private TraversalEvaluator<T> traversalCostEvaluator;
    private EdgeEvaluator<E> edgeLengthEvaluator;
    long startTime;
    protected Network<N,E,T> network;
    String outputDir;
    
    double[] sampleDistanceBreaks;
    double[] samplePathSizes;
    double[] sampleMinCounts;
    double[] sampleMaxCounts;
    
    private static final int TRIP_PROGRESS_REPORT_COUNT = 1000;
    
    public AbstractPathChoiceEdgeAssignmentApplication(PathAlternativeListGenerationConfiguration<N,E,T> configuration) {
        this.configuration = configuration;
        this.randomCostSeeded = configuration.isRandomCostSeeded();
        this.maxCost = configuration.getMaxCost();
        this.traversalCostEvaluator = configuration.getTraversalCostEvaluator();
        this.edgeLengthEvaluator = configuration.getEdgeLengthEvaluator();
        this.sampleDistanceBreaks = configuration.getSampleDistanceBreaks();
        this.samplePathSizes = configuration.getSamplePathSizes();
        this.sampleMinCounts = configuration.getSampleMinCounts();
        this.sampleMaxCounts = configuration.getSampleMaxCounts();
        this.network = configuration.getNetwork();
        this.outputDir = configuration.getOutputDirectory();
    }
    
    protected abstract Map<E,double[]> assignTrip(int tripNum, PathAlternativeList<N,E> alternativeList);
    
    public Map<E,double[]> assignTrips(List<Integer> tripNums)
    {        
        logger.info("Assigning trips...");
        logger.info("Writing to " + outputDir);
        ConcurrentHashMap<E,double[]> volumes =  new ConcurrentHashMap<>();
        int threadCount = Runtime.getRuntime().availableProcessors();
        ExecutorService executor =  Executors.newFixedThreadPool(threadCount);
        final Queue<Integer> tripQueue = new ConcurrentLinkedQueue<>(tripNums);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger counter = new AtomicInteger();
        startTime = System.currentTimeMillis();
        for (int i=0; i<threadCount; i++)
            executor.execute(new CalculationTask(tripQueue,counter,latch,volumes));
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        executor.shutdown();
        
        return volumes;
    }
    
    private class CalculationTask implements Runnable
    {
        private final Queue<Integer> tripQueue;
        private final AtomicInteger counter;
        private final CountDownLatch latch;
        private final ConcurrentHashMap<E,double[]> volumes;
        
        private Network<N,E,T> network;

        private CalculationTask(Queue<Integer> tripQueue, AtomicInteger counter, CountDownLatch latch, ConcurrentHashMap<E,double[]> volumes) {
            this.tripQueue = tripQueue;
            this.counter = counter;
            this.latch = latch;
            this.volumes = volumes;
        }
        
        private PathAlternativeList<N,E> generateAlternatives(int tripId)
        {
            Set<N> singleOriginNode = new HashSet<>();
            Set<N> singleDestinationNode = new HashSet<>();
            
            EdgeEvaluator<E> randomizedEdgeCost;
            ShortestPathStrategy<N> shortestPathStrategy;
            ShortestPathResultSet<N> result;
            
            singleOriginNode.add(getOriginNode(tripId));
            singleDestinationNode.add(getDestinationNode(tripId));
            
            NodePair<N> odPair = new NodePair<>(getOriginNode(tripId),getDestinationNode(tripId));
            PathAlternativeList<N,E> alternativeList = new PathAlternativeList<>(odPair, network, edgeLengthEvaluator);
          
            TraversalEvaluator<T> zeroTraversalEvaluator = new ZeroTraversalEvaluator();
            
            shortestPathStrategy = new RepeatedSingleSourceDijkstra<N,E,T>(network, edgeLengthEvaluator, zeroTraversalEvaluator );
            result = shortestPathStrategy.getShortestPaths(singleOriginNode,singleDestinationNode,Double.MAX_VALUE);
            double distance = result.getShortestPathResult(odPair).getCost();
            int distanceIndex = findFirstIndexGreaterThan(distance, sampleDistanceBreaks);
            
            for (int iterCount = 1; iterCount<= sampleMinCounts[distanceIndex]; iterCount++)
            {
                if (randomCostSeeded) {
                    randomizedEdgeCost = configuration.getRandomizedEdgeCostEvaluator(iterCount, Objects.hash(tripId,iterCount) );
                } else {
                    randomizedEdgeCost = configuration.getRandomizedEdgeCostEvaluator(iterCount, 0);
                }
                
                shortestPathStrategy = new RepeatedSingleSourceDijkstra<N,E,T>(network, randomizedEdgeCost, traversalCostEvaluator);                    
                result = shortestPathStrategy.getShortestPaths(singleOriginNode,singleDestinationNode,maxCost);
                
                alternativeList.add(result.getShortestPathResult(odPair).getPath());                
            }
        return alternativeList;
        }    
        
        public void run()
        {
            while (tripQueue.size() > 0 ) {
                int tripId = tripQueue.poll();
                PathAlternativeList<N,E> alternativeList =  generateAlternatives(tripId);
                
                Map<E,double[]> tripVolumes = assignTrip(tripId, alternativeList);
                
                for (E edge : tripVolumes.keySet()) {
                    if ( volumes.containsKey(edge) ) {
                        double[] values = volumes.get(edge);
                        for (int i=0; i<values.length; i++)
                            values[i] += tripVolumes.get(edge)[i];
                        volumes.put(edge, values);
                    } else {
                        volumes.put(edge, tripVolumes.get(edge));
                    }
                }
            
                int c = counter.addAndGet(1); 
                if ( ( c % TRIP_PROGRESS_REPORT_COUNT ) == 0) {
                    System.out.println("   done with " + c + " trips, run time: " + ( System.currentTimeMillis() - startTime) / 1000 + " sec.");
                }
            }
        }
    }
    
    protected abstract N getOriginNode(int tripId);
    protected abstract N getDestinationNode(int tripId);
    
    private class ZeroTraversalEvaluator implements TraversalEvaluator<T>
    {
        private ZeroTraversalEvaluator() {}
        
        public double evaluate(T traversal)
        {
            return 0.0;
        }
    }
    
    protected int findFirstIndexGreaterThan(double value, double[] array)
    {
        for (int i=0; i < array.length; i++) {
            if ( array[i] >= value ) { return i; }
        }
        return array.length;
    }
}
