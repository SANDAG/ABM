package org.sandag.abm.active;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.sandag.abm.active.sandag.SandagBikePathChoiceModel;

public abstract class AbstractPathChoiceLogsumMatrixApplication<N extends Node, E extends Edge<N>, T extends Traversal<E>>
{
	private static final Logger logger = Logger.getLogger(AbstractPathChoiceLogsumMatrixApplication.class);
	
    PathAlternativeListGenerationConfiguration<N,E,T> configuration;
    Network<N,E,T> network;
    Map<Integer,Map<Integer,Double>> nearbyZonalDistanceMap;
    Map<Integer,Integer> zonalCentroidIdMap;
    double[] sampleDistanceBreaks;
    double[] samplePathSizes;
    double[] sampleMinCounts;
    double[] sampleMaxCounts;
    double[] randomizationSpreads;
    boolean randomCostSeeded;
    EdgeEvaluator<E> edgeLengthEvaluator;
    EdgeEvaluator<E> edgeCostEvaluator;
    TraversalEvaluator<T> traversalCostEvaluator;
    double maxCost;
    long startTime;
    String outputDir;
    Set<Integer> traceOrigins;
    Map<String,String> propertyMap;
    
    private static final int ORIGIN_PROGRESS_REPORT_COUNT = 50;
    private static final double PATHSIZE_PRECISION_TOLERANCE = 0.001;
    
    protected abstract double[] calculateMarketSegmentLogsums(PathAlternativeList<N,E> alternativeList);
    
    public AbstractPathChoiceLogsumMatrixApplication(PathAlternativeListGenerationConfiguration<N,E,T> configuration) {
        this.configuration = configuration;
        this.network = configuration.getNetwork();
        this.nearbyZonalDistanceMap = Collections.unmodifiableMap(configuration.getNearbyZonalDistanceMap());
        this.zonalCentroidIdMap = Collections.unmodifiableMap(configuration.getZonalCentroidIdMap());
        this.sampleDistanceBreaks = configuration.getSampleDistanceBreaks();
        this.samplePathSizes = configuration.getSamplePathSizes();
        this.sampleMinCounts = configuration.getSampleMinCounts();
        this.sampleMaxCounts = configuration.getSampleMaxCounts();
        this.randomizationSpreads = configuration.getRandomizationScales();
        this.randomCostSeeded = configuration.isRandomCostSeeded();
        this.edgeLengthEvaluator = configuration.getEdgeLengthEvaluator();
        this.edgeCostEvaluator = configuration.getEdgeCostEvaluator();
        this.traversalCostEvaluator = configuration.getTraversalCostEvaluator();
        this.maxCost = configuration.getMaxCost();
        this.outputDir = configuration.getOutputDirectory();
        this.traceOrigins = configuration.getTraceOrigins();
        this.propertyMap = configuration.getPropertyMap();
    }

    public Map<NodePair<N>,double[]> calculateMarketSegmentLogsums()
    {      
        System.out.println("Generating path alternative lists...");
        System.out.println("Writing to " + outputDir);
        Map<NodePair<N>,double[]> logsums = new ConcurrentHashMap<>();
        startTime = System.currentTimeMillis();
        int threadCount = 1;//Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount); 
        final Queue<Integer> originQueue = new ConcurrentLinkedQueue<>(zonalCentroidIdMap.keySet());
        final ConcurrentHashMap<Integer,List<Integer>> insufficientSamplePairs = new ConcurrentHashMap<>();
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger counter = new AtomicInteger();
        for (int i = 0; i < threadCount; i++)
            executor.execute(new CalculationTask(originQueue,counter,latch,logsums,insufficientSamplePairs));
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        executor.shutdown();
        
        /*
        for (int origin : insufficientSamplePairs.keySet() ) {          
            String message = "Sample insufficient for origin zone " + origin + " and destination zones ";
            for (int destination : insufficientSamplePairs.get(origin) ) {
                message = message + destination + " ";
            }
            System.out.println(message);
        }
        */
        
        int totalPairs = 0;
        for (int o : nearbyZonalDistanceMap.keySet() ) {
            totalPairs += nearbyZonalDistanceMap.get(o).size();
        }
        System.out.println("Total OD pairs: " + totalPairs);
        System.out.println("Total insufficient sample pairs: " + insufficientSamplePairs.size());
        
        return logsums;
    }
    
    private int findFirstIndexGreaterThan(double value, double[] array)
    {
        for (int i=0; i < array.length; i++) {
            if ( array[i] >= value ) { return i; }
        }
        return array.length;
    }
    
    private class CalculationTask implements Runnable
    {
        private final Queue<Integer> originQueue;
        private final AtomicInteger counter;
        private final CountDownLatch latch;

        private final ConcurrentHashMap<Integer,List<Integer>> insufficientSamplePairs;
        private final Map<NodePair<N>,double[]> logsums;
        
        private CalculationTask(Queue<Integer> originQueue, AtomicInteger counter, CountDownLatch latch, Map<NodePair<N>,double[]> logsums, ConcurrentHashMap<Integer,List<Integer>> insufficientSamplePairs)
        {
            this.originQueue = originQueue;
            this.counter = counter;
            this.latch = latch;
            this.insufficientSamplePairs = insufficientSamplePairs;
            this.logsums = logsums;
        }
        
        private Map<NodePair<N>, PathAlternativeList<N,E>> generateAlternatives(int origin)
        {
            Set<N> singleOriginNode = new HashSet<>();
            Set<N> destinationNodes = new HashSet<>();
            Map<N,Integer> destinationZoneMap = new HashMap<>();
            Map<N,Double> destinationDistanceMap = new HashMap<>();
            Map<N,Double> destinationPathSizeMap = new HashMap<>();
            Map<N,Double> destinationMinCountMap = new HashMap<>();
            Map<N,Double> destinationMaxCountMap = new HashMap<>();
            HashMap<NodePair<N>, PathAlternativeList<N,E>> alternativeLists = new HashMap<>();
            EdgeEvaluator<E> randomizedEdgeCost;
            ShortestPathStrategy<N> shortestPathStrategy;
            ShortestPathResultSet<N> result;
            int distanceIndex;
            
            singleOriginNode.add(network.getNode(zonalCentroidIdMap.get(origin)));
            N destinationNode;
            PathAlternativeList<N,E> alternativeList;
            
            if ( nearbyZonalDistanceMap.containsKey(origin) ) {
                for (int destination : nearbyZonalDistanceMap.get(origin).keySet()) {
                    destinationNode = network.getNode(zonalCentroidIdMap.get(destination));
                    destinationNodes.add(destinationNode);
                    destinationDistanceMap.put(destinationNode, nearbyZonalDistanceMap.get(origin).get(destination));
                    destinationZoneMap.put(destinationNode, destination);
                    distanceIndex = findFirstIndexGreaterThan(destinationDistanceMap.get(destinationNode), sampleDistanceBreaks);
                    destinationPathSizeMap.put(destinationNode, samplePathSizes[distanceIndex]);
                    destinationMinCountMap.put(destinationNode, sampleMinCounts[distanceIndex]);
                    destinationMaxCountMap.put(destinationNode, sampleMaxCounts[distanceIndex]);
                }
            }
            
            int iterCount = 1;
            while( destinationNodes.size() > 0 ) {
                
                double spread = randomizationSpreads[Math.min(iterCount,randomizationSpreads.length-1)];

                if ( randomCostSeeded ) { 
                    randomizedEdgeCost = new RandomizedEdgeCost(edgeCostEvaluator, edgeLengthEvaluator, spread, Objects.hash(origin,iterCount));
                } else {
                    randomizedEdgeCost = new RandomizedEdgeCost(edgeCostEvaluator, edgeLengthEvaluator, spread);
                }
                    
                shortestPathStrategy = new RepeatedSingleSourceDijkstra<N,E,T>(network, randomizedEdgeCost, traversalCostEvaluator);                    
                result = shortestPathStrategy.getShortestPaths(singleOriginNode,destinationNodes,maxCost);
                
                for (NodePair<N> odPair : result) {
                    if ( ! alternativeLists.containsKey(odPair) ) { alternativeLists.put(odPair,new PathAlternativeList<N,E>(odPair, network, edgeLengthEvaluator)); }
                    alternativeList = alternativeLists.get(odPair);
                    alternativeList.add(result.getShortestPathResult(odPair).getPath());
                    destinationNode = odPair.getToNode();
                    
                    if ( alternativeList.getSizeMeasureTotal() >= destinationPathSizeMap.get(destinationNode) - PATHSIZE_PRECISION_TOLERANCE && iterCount >= destinationMinCountMap.get(destinationNode) ) {
                        destinationNodes.remove(odPair.getToNode());
                        alternativeList.clearPathSizeCalculator();
                    } else if ( iterCount >= destinationMaxCountMap.get(destinationNode) ) {
                        destinationNodes.remove(odPair.getToNode());
                        alternativeList.clearPathSizeCalculator();
                        if ( ! insufficientSamplePairs.containsKey(origin) ) 
                        	insufficientSamplePairs.put( origin, new ArrayList<Integer>() );
                        insufficientSamplePairs.get(origin).add(destinationZoneMap.get(destinationNode));       
                    }
                }
                
                iterCount++;
            }
            
            return alternativeLists;
        }
        
//        private PathAlternativeList<N,E> resampleAlternatives(PathAlternativeList<N,E> alts)
//        {
//            return alts.getNewPathSample();
//        }
        
        @Override
        public void run()
        {                      
            while ( originQueue.size() > 0 ) {
                int origin = originQueue.poll();
                
                Map<NodePair<N>, PathAlternativeList<N,E>> alternativeLists =  generateAlternatives(origin);
                
                if ( traceOrigins.contains(origin) ) {
                    try {
                        PathAlternativeListWriter<N,E> writer = new PathAlternativeListWriter<N,E>(outputDir + "paths_" + origin + ".csv", outputDir + "links_" + origin + ".csv");
                        writer.writeHeaders();
                        for (PathAlternativeList<N,E> list : alternativeLists.values()) {
                            writer.write(list);
                        }
                        writer.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e.getMessage());
                    }
                }
                
                //note: either of these ways will work
                //      this first one gets a new sample from the old one, while the second mutates (via a resample) the alternatives themselves
//                for( NodePair<N> odPair : alternativeLists.keySet() )
//                    alternativeLists.put(odPair, resampleAlternatives(alternativeLists.get(odPair)));
                for (PathAlternativeList<N,E> pal : alternativeLists.values())
                	pal.resampleAlternatives();
                
                double[] logsumValues;
                for(NodePair<N> odPair : alternativeLists.keySet()) {

                    String pathAltFile = SandagBikePathChoiceModel.getPathAltsFile(propertyMap);
                    String pathAltLinksFile = SandagBikePathChoiceModel.getPathAltsLinkFile(propertyMap);
                    try (PathAlternativeListWriter<N,E> writer = new PathAlternativeListWriter<>(pathAltFile,pathAltLinksFile)) {
                        writer.writeHeaders();
                        writer.write(alternativeLists.get(odPair));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    
                    logsumValues = calculateMarketSegmentLogsums(alternativeLists.get(odPair));
                    logsums.put(odPair,logsumValues);
                    

                    try {
                    	Files.delete(Paths.get(pathAltFile));
                    } catch (IOException e) {
                    	logger.warn("problem deleting " + pathAltFile,e);
                    }
                    try {
                    	Files.delete(Paths.get(pathAltLinksFile));
                    } catch (IOException e) {
                    	logger.warn("problem deleting " + pathAltLinksFile,e);
                    }
                }
                
                int c = counter.addAndGet(1); 
                if ( ( c % ORIGIN_PROGRESS_REPORT_COUNT ) == 0) {
                    System.out.println("   done with " + c + " origins, run time: " + ( System.currentTimeMillis() - startTime) / 1000 + " sec.");
                }
            }

            latch.countDown();
        }  
    } 

    private class RandomizedEdgeCost implements EdgeEvaluator<E>
    {
        private EdgeEvaluator<E> edgeCostEvaluator;
        private EdgeEvaluator<E> edgeLengthEvaluator;
        private Random random;
        private double spread;
        
        public RandomizedEdgeCost(EdgeEvaluator<E> edgeCostEvaluator, EdgeEvaluator<E> edgeLengthEvaluator, double spread)
        {
            this.edgeCostEvaluator = edgeCostEvaluator;
            this.edgeLengthEvaluator = edgeLengthEvaluator;
            this.spread = spread;
            random = new Random();
        }
        
        public RandomizedEdgeCost(EdgeEvaluator<E> edgeCostEvaluator, EdgeEvaluator<E> edgeLengthEvaluator, double spread, long seed)
        {
            this.edgeCostEvaluator = edgeCostEvaluator;
            this.edgeLengthEvaluator = edgeLengthEvaluator;
            this.spread = spread;
            random = new Random(seed);
        }

        @Override
        public double evaluate(E edge)
        {
            return edgeCostEvaluator.evaluate(edge) + edgeLengthEvaluator.evaluate(edge) * random.nextDouble() * spread;
        }
        
    }
    
}


