package org.sandag.abm.active;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;

public abstract class AbstractPathChoiceLogsumMatrixApplication<N extends Node, E extends Edge<N>, T extends Traversal<E>>
{
    private static final Logger                                   logger                       = Logger.getLogger(AbstractPathChoiceLogsumMatrixApplication.class);

    protected PathAlternativeListGenerationConfiguration<N, E, T> configuration;
    Network<N, E, T>                                              network;
    Map<Integer, Map<Integer, Double>>                            nearbyZonalDistanceMap;
    Map<Integer, Integer>                                         originZonalCentroidIdMap;
    Map<Integer, Integer>                                         destinationZonalCentroidIdMap;
    double[]                                                      sampleDistanceBreaks;
    double[]                                                      samplePathSizes;
    double[]                                                      sampleMinCounts;
    double[]                                                      sampleMaxCounts;
    EdgeEvaluator<E>                                              edgeLengthEvaluator;
    EdgeEvaluator<E>                                              edgeCostEvaluator;
    TraversalEvaluator<T>                                         traversalCostEvaluator;
    double                                                        maxCost;
    long                                                          startTime;
    String                                                        outputDir;
    Set<Integer>                                                  traceOrigins;
    protected Map<String, String>                                 propertyMap;
    boolean                                                       randomCostSeeded;
    boolean                                                       intrazonalsNeeded;

    private static final int                                      ORIGIN_PROGRESS_REPORT_COUNT = 50;
    private static final double                                   DOUBLE_PRECISION_TOLERANCE   = 0.001;

    protected abstract double[] calculateMarketSegmentLogsums(
            PathAlternativeList<N, E> alternativeList);

    protected abstract List<IntrazonalCalculation<N>> getMarketSegmentIntrazonalCalculations();

    public AbstractPathChoiceLogsumMatrixApplication(
            PathAlternativeListGenerationConfiguration<N, E, T> configuration)
    {
        this.configuration = configuration;
        this.network = configuration.getNetwork();
        this.nearbyZonalDistanceMap = Collections.unmodifiableMap(configuration
                .getNearbyZonalDistanceMap());
        this.originZonalCentroidIdMap = Collections.unmodifiableMap(configuration
                .getOriginZonalCentroidIdMap());
        this.destinationZonalCentroidIdMap = Collections.unmodifiableMap(configuration
                .getDestinationZonalCentroidIdMap());
        this.sampleDistanceBreaks = configuration.getSampleDistanceBreaks();
        this.samplePathSizes = configuration.getSamplePathSizes();
        this.sampleMinCounts = configuration.getSampleMinCounts();
        this.sampleMaxCounts = configuration.getSampleMaxCounts();
        this.edgeLengthEvaluator = configuration.getEdgeLengthEvaluator();
        this.edgeCostEvaluator = configuration.getEdgeCostEvaluator();
        this.traversalCostEvaluator = configuration.getTraversalCostEvaluator();
        this.maxCost = configuration.getMaxCost();
        this.outputDir = configuration.getOutputDirectory();
        this.traceOrigins = configuration.getTraceOrigins();
        this.propertyMap = configuration.getPropertyMap();
        this.randomCostSeeded = configuration.isRandomCostSeeded();
        this.intrazonalsNeeded = configuration.isIntrazonalsNeeded();
    }

    public Map<NodePair<N>, double[]> calculateMarketSegmentLogsums()
    {
        logger.info("Generating path alternative lists...");
        logger.info("Writing to " + outputDir);
        Map<N, ConcurrentHashMap<N, double[]>> logsums = new ConcurrentHashMap<>();
        startTime = System.currentTimeMillis();
        int threadCount = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final Queue<Integer> originQueue = new ConcurrentLinkedQueue<>(
                originZonalCentroidIdMap.keySet());

        final ConcurrentHashMap<Integer, List<Integer>> insufficientSamplePairs = new ConcurrentHashMap<>();
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger counter = new AtomicInteger();
        for (int i = 0; i < threadCount; i++)
            executor.execute(new CalculationTask(originQueue, counter, latch, logsums,
                    insufficientSamplePairs));
        try
        {
            latch.await();
        } catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        executor.shutdown();

        /*
         * for (int origin : insufficientSamplePairs.keySet() ) { String message
         * = "Sample insufficient for origin zone " + origin +
         * " and destination zones "; for (int destination :
         * insufficientSamplePairs.get(origin) ) { message = message +
         * destination + " "; } System.out.println(message); }
         */

        int totalPairs = 0;
        for (int o : nearbyZonalDistanceMap.keySet())
        {
            totalPairs += nearbyZonalDistanceMap.get(o).size();
        }
        logger.info("Total OD pairs: " + totalPairs);

        int totalInsuffPairs = 0;
        for (int o : insufficientSamplePairs.keySet())
        {
            totalInsuffPairs += insufficientSamplePairs.get(o).size();
        }

        logger.info("Total insufficient sample pairs: " + totalInsuffPairs);

        if (intrazonalsNeeded)
        {
            logger.info("Calculating intrazonals");
            List<IntrazonalCalculation<N>> intrazonalCalculations = getMarketSegmentIntrazonalCalculations();
            int segments = intrazonalCalculations.size();
            for (int segment = 0; segment < segments; segment++)
            {
                for (N origin : logsums.keySet())
                {
                    Map<N, double[]> originLogsums = logsums.get(origin);
                    if (segment == 0) originLogsums.put(origin, new double[segments]);
                    originLogsums.get(origin)[segment] = intrazonalCalculations.get(segment)
                            .getIntrazonalValue(origin, originLogsums, segment);
                }
            }
        }

        Map<NodePair<N>, double[]> pairLogsums = new HashMap<>();
        for (N oNode : logsums.keySet())
        {
            for (N dNode : logsums.get(oNode).keySet())
            {
                pairLogsums.put(new NodePair<N>(oNode, dNode), logsums.get(oNode).get(dNode));
            }
        }

        return pairLogsums;
    }

    private int findFirstIndexGreaterThan(double value, double[] array)
    {
        for (int i = 0; i < array.length; i++)
        {
            if (array[i] >= value)
            {
                return i;
            }
        }
        return array.length;
    }

    private class CalculationTask
            implements Runnable
    {
        private final Queue<Integer>                            originQueue;
        private final AtomicInteger                             counter;
        private final CountDownLatch                            latch;

        private final ConcurrentHashMap<Integer, List<Integer>> insufficientSamplePairs;
        private final Map<N, ConcurrentHashMap<N, double[]>>    logsums;

        private CalculationTask(Queue<Integer> originQueue, AtomicInteger counter,
                CountDownLatch latch, Map<N, ConcurrentHashMap<N, double[]>> logsums,
                ConcurrentHashMap<Integer, List<Integer>> insufficientSamplePairs)
        {
            this.originQueue = originQueue;
            this.counter = counter;
            this.latch = latch;
            this.insufficientSamplePairs = insufficientSamplePairs;
            this.logsums = logsums;
        }

        private Map<NodePair<N>, PathAlternativeList<N, E>> generateAlternatives(int origin)
        {
            Set<N> singleOriginNode = new HashSet<>();
            Set<N> destinationNodes = new HashSet<>();
            Map<N, Integer> destinationZoneMap = new HashMap<>();
            Map<N, Double> destinationDistanceMap = new HashMap<>();
            Map<N, Double> destinationPathSizeMap = new HashMap<>();
            Map<N, Double> destinationMinCountMap = new HashMap<>();
            Map<N, Double> destinationMaxCountMap = new HashMap<>();
            HashMap<NodePair<N>, PathAlternativeList<N, E>> alternativeLists = new HashMap<>();
            EdgeEvaluator<E> randomizedEdgeCost;
            ShortestPathStrategy<N> shortestPathStrategy;
            ShortestPathResultSet<N> result;
            int distanceIndex;

            singleOriginNode.add(network.getNode(originZonalCentroidIdMap.get(origin)));
            N destinationNode = null;
            PathAlternativeList<N, E> alternativeList;

            if (nearbyZonalDistanceMap.containsKey(origin))
            {
                for (int destination : nearbyZonalDistanceMap.get(origin).keySet())
                {
                    try
                    {
                        destinationNode = network.getNode(destinationZonalCentroidIdMap
                                .get(destination));
                    } catch (NullPointerException e)
                    {
                        logger.warn(destinationZonalCentroidIdMap.get(destination));
                    }
                    destinationNodes.add(destinationNode);
                    destinationDistanceMap.put(destinationNode, nearbyZonalDistanceMap.get(origin)
                            .get(destination));
                    destinationZoneMap.put(destinationNode, destination);
                    distanceIndex = findFirstIndexGreaterThan(
                            destinationDistanceMap.get(destinationNode), sampleDistanceBreaks);
                    destinationPathSizeMap.put(destinationNode, samplePathSizes[distanceIndex]);
                    destinationMinCountMap.put(destinationNode, sampleMinCounts[distanceIndex]);
                    destinationMaxCountMap.put(destinationNode, sampleMaxCounts[distanceIndex]);
                }
            }

            int iterCount = 1;
            while (destinationNodes.size() > 0)
            {

                if (randomCostSeeded)
                {
                    randomizedEdgeCost = configuration.getRandomizedEdgeCostEvaluator(iterCount,
                            Objects.hash(origin, iterCount));
                } else
                {
                    randomizedEdgeCost = configuration.getRandomizedEdgeCostEvaluator(iterCount, 0);
                }

                shortestPathStrategy = new RepeatedSingleSourceDijkstra<N, E, T>(network,
                        randomizedEdgeCost, traversalCostEvaluator);
                result = shortestPathStrategy.getShortestPaths(singleOriginNode, destinationNodes,
                        maxCost);

                for (NodePair<N> odPair : result)
                {
                    if (!alternativeLists.containsKey(odPair))
                    {
                        alternativeLists.put(odPair, new PathAlternativeList<N, E>(odPair, network,
                                edgeLengthEvaluator));
                    }
                    alternativeList = alternativeLists.get(odPair);
                    alternativeList.add(result.getShortestPathResult(odPair).getPath());
                    destinationNode = odPair.getToNode();

                    if (alternativeList.getSizeMeasureTotal() >= destinationPathSizeMap
                            .get(destinationNode) - DOUBLE_PRECISION_TOLERANCE
                            && iterCount >= destinationMinCountMap.get(destinationNode))
                    {
                        destinationNodes.remove(odPair.getToNode());
                        alternativeList.clearPathSizeCalculator();
                    } else if (iterCount >= destinationMaxCountMap.get(destinationNode))
                    {
                        destinationNodes.remove(odPair.getToNode());
                        alternativeList.clearPathSizeCalculator();
                        if (!insufficientSamplePairs.containsKey(origin))
                            insufficientSamplePairs.put(origin, new ArrayList<Integer>());
                        insufficientSamplePairs.get(origin).add(
                                destinationZoneMap.get(destinationNode));
                    }
                }

                iterCount++;
            }

            if (traceOrigins.contains(origin))
            {
                try
                {
                    PathAlternativeListWriter<N, E> writer = new PathAlternativeListWriter<N, E>(
                            outputDir + "origpaths_" + origin + ".csv", outputDir + "origlinks_"
                                    + origin + ".csv");
                    writer.writeHeaders();
                    for (PathAlternativeList<N, E> list : alternativeLists.values())
                    {
                        writer.write(list);
                    }
                    writer.close();
                } catch (IOException e)
                {
                    throw new RuntimeException(e.getMessage());
                }

            }

            for (NodePair<N> odPair : alternativeLists.keySet())
                alternativeLists.put(
                        odPair,
                        resampleAlternatives(alternativeLists.get(odPair),
                                destinationPathSizeMap.get(odPair.getToNode())));

            return alternativeLists;
        }

        private PathAlternativeList<N, E> resampleAlternatives(PathAlternativeList<N, E> alts,
                double targetSize)
        {
            if (targetSize >= alts.getSizeMeasureTotal())
            {
                return alts;
            }
            Random r;
            if (randomCostSeeded)
            {
                r = new Random(alts.getODPair().hashCode());
            } else
            {
                r = new Random();
            }
            PathAlternativeList<N, E> newAlts = new PathAlternativeList<>(alts.getODPair(),
                    network, alts.getLengthEvaluator());
            double[] prob = new double[alts.getCount()];
            double[] cum = new double[alts.getCount()];
            double tot = 0.0;
            for (int i = 0; i < prob.length; i++)
            {
                prob[i] = alts.getSizeMeasures().get(i) / alts.getSizeMeasureTotal();
                tot = tot + prob[i];
                cum[i] = tot;
            }
            cum[alts.getCount() - 1] = 1.0;

            while (newAlts.getSizeMeasureTotal() < targetSize
                    && newAlts.getCount() < alts.getCount())
            {
                double p = r.nextDouble();
                int idx = BinarySearch.binarySearch(cum, p);
                newAlts.add(alts.get(idx));
                double curProb = cum[idx];
                if (idx > 0)
                {
                    curProb = curProb - cum[idx - 1];
                }
                for (int i = 0; i < cum.length; i++)
                {
                    if (i < idx)
                    {
                        cum[i] = cum[i] / (1 - curProb);
                    } else
                    {
                        cum[i] = (cum[i] - curProb) / (1 - curProb);
                    }
                }
            }
            return newAlts;
        }

        @Override
        public void run()
        {
            while (originQueue.size() > 0)
            {
                int origin = originQueue.poll();

                Map<NodePair<N>, PathAlternativeList<N, E>> alternativeLists = generateAlternatives(origin);

                if (traceOrigins.contains(origin))
                {
                    try
                    {
                        PathAlternativeListWriter<N, E> writer = new PathAlternativeListWriter<N, E>(
                                outputDir + "resamplepaths_" + origin + ".csv", outputDir
                                        + "resamplelinks_" + origin + ".csv");
                        writer.writeHeaders();
                        for (PathAlternativeList<N, E> list : alternativeLists.values())
                        {
                            writer.write(list);
                        }
                        writer.close();
                    } catch (IOException e)
                    {
                        throw new RuntimeException(e.getMessage());
                    }
                }

                double[] logsumValues;
                for (NodePair<N> odPair : alternativeLists.keySet())
                {

                    if (!odPair.getFromNode().equals(odPair.getToNode()))
                    {

                        logsumValues = calculateMarketSegmentLogsums(alternativeLists.get(odPair));
                        if (!logsums.containsKey(odPair.getFromNode()))
                        {
                            logsums.put(odPair.getFromNode(), new ConcurrentHashMap<N, double[]>());
                        }
                        logsums.get(odPair.getFromNode()).put(odPair.getToNode(), logsumValues);
                    }

                }

                int c = counter.addAndGet(1);
                if ((c % ORIGIN_PROGRESS_REPORT_COUNT) == 0)
                {
                    logger.info("   done with " + c + " origins, run time: "
                            + (System.currentTimeMillis() - startTime) / 1000 + " sec.");
                }
            }

            latch.countDown();
        }
    }

}
