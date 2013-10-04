package org.sandag.abm.active;


import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.sandag.abm.active.PathAlternativeListGenerationConfiguration.ZoneDistancePair;

public class PathAlternativeListGenerationApplication<N extends Node, E extends Edge<N>, T extends Traversal<E>>
{
    PathAlternativeListGenerationConfiguration<N,E,T> configuration;
    HashMap<NodePair<N>,PathAlternativeList<N,E>> alternativeLists;
    
    public PathAlternativeListGenerationApplication(PathAlternativeListGenerationConfiguration<N,E,T> configuration) {
        this.configuration = configuration;
        alternativeLists = new HashMap<NodePair<N>,PathAlternativeList<N,E>>();
    }

    public HashMap<NodePair<N>,PathAlternativeList<N,E>> generateAlternativeLists(int zone)
    {
        int threadCount = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final Queue<HashMap<Integer,PathAlternativeList<N,E>>> alternativeListQueue = new ConcurrentLinkedQueue<>();
        final Queue<Integer> originQueue = new ConcurrentLinkedQueue<>(configuration.getZonalCentroidIdMap().keySet());
    }
    
    private class GenerationTask implements Runnable
    {
        private final Queue<Integer> origins;
        private final AtomicInteger counter;
        private final CountDownLatch latch;
        private final ThreadLocal<HashMap<NodePair<N>,PathAlternativeList<N,E>>> alternativeLists;
        
        private GenerationTask(Queue<Integer> origins, AtomicInteger counter, ThreadLocal<HashMap<NodePair<N>,PathAlternativeList<N,E>>> alternativeLists, CountDownLatch latch)
        {
            this.origins = origins;
            this.counter = counter;
            this.alternativeLists = alternativeLists;
            this.latch = latch;
        }
        
        @Override
        public void run()
        {
            int origin = origins.poll();
            Set<N> singleOriginNode = new HashSet<N>();
            singleOriginNode.add(configuration.getNetwork().getNode(configuration.getZonalCentroidIdMap().get(origin)));
            Set<N> destinationNodes = new HashSet<N>();
            Map<N,Integer> destinationZoneMap = new HashMap<N,Integer>();
            Map<N,Double> destinationDistanceMap = new HashMap<N,Double>();
            N destinationNode;
            for (int destination : configuration.getNearbyZonalDistanceMap().get(origin).keySet()) {
                destinationNode = configuration.getNetwork().getNode(configuration.getZonalCentroidIdMap().get(destination));
                destinationNodes.add(destinationNode);
                destinationDistanceMap.put(destinationNode, configuration.getNearbyZonalDistanceMap().get(origin).get(destination));
                destinationZoneMap.put(destinationNode, destination);
            }
            
            int iterCount = 0;
            while( iterCount < configuration.getMaxNRandomizations() && destinationNodes.size() > 0 )
            {
                ShortestPathResults<N> result = configuration.getShortestPath().getShortestPaths(singleOriginNode,destinationNodes,configuration.getMaxCost());
                for (NodePair<N> odPair : result) 
                ShortestPathResultsContainer<N> sprc = spr.get();
                for (ShortestPathResult<N> spResult : result.getResults()) 
                    sprc.addResult(spResult);
                int c = counter.addAndGet(origins.size()); 
                if (c % segmentSize < origins.size())
                    System.out.println("   done with " + ((c / segmentSize)*segmentSize) + " origins");
                origins.clear();
            }
            latch.countDown();
        }  
    }

    
    @Override
    public ShortestPathResults<N> getShortestPaths(Set<N> originNodes, Set<N> destinationNodes, double maxCost) {
        switch (method) {
            case FORK_JOIN : {
                ShortestPathRecursiveTask task = new ShortestPathRecursiveTask(sp,originNodes,destinationNodes,maxCost);
                new ForkJoinPool().execute(task);
                ShortestPathResultsContainer<N> sprc = task.getResult();
                return sprc;
            }
            case QUEUE : {
                int threadCount = Runtime.getRuntime().availableProcessors();
                ExecutorService executor = Executors.newFixedThreadPool(threadCount);
                final Queue<ShortestPathResultsContainer<N>> sprcQueue = new ConcurrentLinkedQueue<>();
                final Queue<N> originNodeQueue = new ConcurrentLinkedQueue<>(originNodes);
                ThreadLocal<ShortestPathResultsContainer<N>> sprcThreadLocal = new ThreadLocal<ShortestPathResultsContainer<N>>() {
                    @Override
                    public ShortestPathResultsContainer<N> initialValue() {
                        ShortestPathResultsContainer<N> sprc = new BasicShortestPathResults<>();
                        sprcQueue.add(sprc);
                        return sprc;
                    }
                };
                final CountDownLatch latch = new CountDownLatch(threadCount);
                final AtomicInteger counter = new AtomicInteger();
                for (int i = 0; i < threadCount; i++)
                    executor.execute(new QueueMethodTask(sp,originNodeQueue,destinationNodes,maxCost,counter,sprcThreadLocal,latch));
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                executor.shutdown();
                
                ShortestPathResultsContainer<N> finalContainer = null;
                for (ShortestPathResultsContainer<N> sprc : sprcQueue)
                    if (finalContainer == null)
                        finalContainer = sprc;
                    else
                        finalContainer.addAll(sprc);
                
                return finalContainer;
            }
            default : throw new IllegalStateException("Should not be here.");
        }
    }

    @Override
    public ShortestPathResults<N> getShortestPaths(Set<N> originNodes, Set<N> destinationNodes) {
        return getShortestPaths(originNodes,destinationNodes,Double.POSITIVE_INFINITY);
    }
    
    private class QueueMethodTask implements Runnable {
        private final ShortestPath<N> sp;
        private final Queue<N> originNodes;
        private final Set<N> destinationNodes;
        private final double maxCost;
        private final AtomicInteger counter;
        private final ThreadLocal<ShortestPathResultsContainer<N>> spr;
        private final CountDownLatch latch;
        
        private QueueMethodTask(ShortestPath<N> sp, Queue<N> originNodes, Set<N> destinationNodes, double maxCost, AtomicInteger counter, ThreadLocal<ShortestPathResultsContainer<N>> spr, CountDownLatch latch) {
            this.sp = sp;
            this.destinationNodes = destinationNodes;
            this.originNodes = originNodes;
            this.maxCost = maxCost;
            this.counter = counter;
            this.spr = spr;
            this.latch = latch;
        }

        @Override
        public void run() {
            int segmentSize = 5;
            final Set<N> origins = new HashSet<>();
            while (originNodes.size() > 0) {
                while ((originNodes.size() > 0) && (origins.size() < segmentSize)) {
                    N origin = originNodes.poll();
                    if (origin != null)
                        origins.add(origin);
                }
                if (origins.size() == 0)
                    break;
                ShortestPathResults<N> result = sp.getShortestPaths(origins,destinationNodes,maxCost);
                ShortestPathResultsContainer<N> sprc = spr.get();
                for (ShortestPathResult<N> spResult : result.getResults()) 
                    sprc.addResult(spResult);
                int c = counter.addAndGet(origins.size()); 
                if (c % segmentSize < origins.size())
                    System.out.println("   done with " + ((c / segmentSize)*segmentSize) + " origins");
                origins.clear();
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
            // TODO Auto-generated method stub
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
            // TODO Auto-generated method stub
            return traversalCostEvaluator.evaluate(traversal) * ( lower + random.nextDouble() * (upper - lower) );
        }
        
    }
}
