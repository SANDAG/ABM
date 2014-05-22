package org.sandag.abm.active.sandag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import org.sandag.abm.active.EdgeEvaluator;
import org.sandag.abm.active.Network;
import org.sandag.abm.active.NodePair;
import org.sandag.abm.active.ParallelSingleSourceDijkstra;
import org.sandag.abm.active.PathAlternativeListGenerationConfiguration;
import org.sandag.abm.active.RepeatedSingleSourceDijkstra;
import org.sandag.abm.active.ShortestPathResultSet;
import org.sandag.abm.active.ShortestPathStrategy;
import org.sandag.abm.active.TraversalEvaluator;

public abstract class SandagBikePathAlternativeListGenerationConfiguration
        implements
        PathAlternativeListGenerationConfiguration<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal>
{

    public static final String                                             PROPERTIES_COEF_DISTCLA0          = "active.coef.distcla0";
    public static final String                                             PROPERTIES_COEF_DISTCLA1          = "active.coef.distcla1";
    public static final String                                             PROPERTIES_COEF_DISTCLA2          = "active.coef.distcla2";
    public static final String                                             PROPERTIES_COEF_DISTCLA3          = "active.coef.distcla3";
    public static final String                                             PROPERTIES_COEF_DARTNE2           = "active.coef.dartne2";
    public static final String                                             PROPERTIES_COEF_DWRONGWY          = "active.coef.dwrongwy";
    public static final String                                             PROPERTIES_COEF_GAIN              = "active.coef.gain";
    public static final String                                             PROPERTIES_COEF_TURN              = "active.coef.turn";
    public static final String                                             PROPERTIES_COEF_DISTANCE_WALK     = "active.coef.distance.walk";
    public static final String                                             PROPERTIES_COEF_GAIN_WALK         = "active.coef.gain.walk";
    public static final String                                             PROPERTIES_COEF_DCYCTRAC          = "active.coef.dcyctrac";
    public static final String                                             PROPERTIES_COEF_DBIKBLVD          = "active.coef.dbikblvd";
    public static final String                                             PROPERTIES_COEF_SIGNALS           = "active.coef.signals";
    public static final String                                             PROPERTIES_COEF_UNLFRMA           = "active.coef.unlfrma";
    public static final String                                             PROPERTIES_COEF_UNLFRMI           = "active.coef.unlfrmi";
    public static final String                                             PROPERTIES_COEF_UNTOMA            = "active.coef.untoma";
    public static final String                                             PROPERTIES_COEF_UNTOMI            = "active.coef.untomi";
    public static final String                                             PROPERTIES_BIKE_MINUTES_PER_MILE  = "active.bike.minutes.per.mile";
    public static final String                                             PROPERTIES_OUTPUT                 = "active.output.bike";
    private static final double                                            INACCESSIBLE_COST_COEF            = 999.0;

    protected Map<String, String>                                          propertyMap;
    protected PropertyParser                                               propertyParser;
    protected final String                                                 PROPERTIES_SAMPLE_MAXCOST         = "active.sample.maxcost";
    protected final String                                                 PROPERTIES_SAMPLE_RANDOM_SEEDED   = "active.sample.random.seeded";
    protected final String                                                 PROPERTIES_SAMPLE_DISTANCE_BREAKS = "active.sample.distance.breaks";
    protected final String                                                 PROPERTIES_SAMPLE_PATHSIZES       = "active.sample.pathsizes";
    protected final String                                                 PROPERTIES_SAMPLE_COUNT_MIN       = "active.sample.count.min";
    protected final String                                                 PROPERTIES_SAMPLE_COUNT_MAX       = "active.sample.count.max";
    protected final String                                                 PROPERTIES_TRACE_EXCLUSIVE        = "active.trace.exclusive";
    protected final String                                                 PROPERTIES_RANDOM_SCALE_COEF      = "active.sample.random.scale.coef";
    protected final String                                                 PROPERTIES_RANDOM_SCALE_LINK      = "active.sample.random.scale.link";

    protected String                                                       PROPERTIES_MAXDIST_ZONE;
    protected String                                                       PROPERTIES_TRACE_ORIGINS;

    protected Map<Integer, Map<Integer, Double>>                           nearbyZonalDistanceMap;
    protected Map<Integer, Integer>                                        zonalCentroidIdMap;
    protected Network<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> network;
    private final double                                                   bikeMinutesPerMile;

    public SandagBikePathAlternativeListGenerationConfiguration(Map<String, String> propertyMap,
            Network<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> network)
    {
        this.propertyMap = propertyMap;
        this.propertyParser = new PropertyParser(propertyMap);
        this.nearbyZonalDistanceMap = null;
        this.zonalCentroidIdMap = null;
        this.network = network;
        bikeMinutesPerMile = Double.parseDouble(propertyMap.get(PROPERTIES_BIKE_MINUTES_PER_MILE));
    }

    public Set<Integer> getTraceOrigins()
    {
        return propertyMap.containsKey(PROPERTIES_TRACE_ORIGINS) ? new HashSet<>(
                propertyParser.parseIntPropertyList(PROPERTIES_TRACE_ORIGINS))
                : new HashSet<Integer>();
    }

    @Override
    public Network<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> getNetwork()
    {
        return network;
    }

    public String getOutputDirectory()
    {
        return propertyMap.get(PROPERTIES_OUTPUT);
    }

    static class SandagBikeDistanceEvaluator
            implements EdgeEvaluator<SandagBikeEdge>
    {
        public double evaluate(SandagBikeEdge edge)
        {
            return edge.distance;
        }
    }

    static class SandagBikeAccessibleDistanceEvaluator
            implements EdgeEvaluator<SandagBikeEdge>
    {
        public double evaluate(SandagBikeEdge edge)
        {
            return edge.distance + (edge.bikeCost > 998 ? 999 : 0);
        }
    }

    static class ZeroTraversalEvaluator
            implements TraversalEvaluator<SandagBikeTraversal>
    {
        public double evaluate(SandagBikeTraversal traversal)
        {
            return 999 * (traversal.thruCentroid ? 1 : 0);
        }
    }

    @Override
    public EdgeEvaluator<SandagBikeEdge> getEdgeLengthEvaluator()
    {
        return new SandagBikeDistanceEvaluator();
    }

    @Override
    public EdgeEvaluator<SandagBikeEdge> getEdgeCostEvaluator()
    {
        final class SandagBikeEdgeCostEvaluator
                implements EdgeEvaluator<SandagBikeEdge>
        {
            public double evaluate(SandagBikeEdge edge)
            {
                return edge.bikeCost;
            }
        }

        return new SandagBikeEdgeCostEvaluator();
    }

    @Override
    public TraversalEvaluator<SandagBikeTraversal> getTraversalCostEvaluator()
    {
        final class SandagBikeTraversalCostEvaluator
                implements TraversalEvaluator<SandagBikeTraversal>
        {
            public double evaluate(SandagBikeTraversal traversal)
            {
                return traversal.cost;
            }
        }

        return new SandagBikeTraversalCostEvaluator();
    }

    @Override
    public double getMaxCost()
    {
        return Double.parseDouble(propertyMap.get(PROPERTIES_SAMPLE_MAXCOST));
    }

    @Override
    public double getDefaultMinutesPerMile()
    {
        return bikeMinutesPerMile;
    }

    @Override
    public double[] getSampleDistanceBreaks()
    {
        return propertyParser.parseDoublePropertyArray(PROPERTIES_SAMPLE_DISTANCE_BREAKS);
    }

    @Override
    public double[] getSamplePathSizes()
    {
        return propertyParser.parseDoublePropertyArray(PROPERTIES_SAMPLE_PATHSIZES);
    }

    @Override
    public double[] getSampleMinCounts()
    {
        return propertyParser.parseDoublePropertyArray(PROPERTIES_SAMPLE_COUNT_MIN);
    }

    @Override
    public double[] getSampleMaxCounts()
    {
        return propertyParser.parseDoublePropertyArray(PROPERTIES_SAMPLE_COUNT_MAX);
    }

    @Override
    public boolean isRandomCostSeeded()
    {
        return Boolean.parseBoolean(propertyMap.get(PROPERTIES_SAMPLE_RANDOM_SEEDED));
    }

    @Override
    public Map<Integer, Map<Integer, Double>> getNearbyZonalDistanceMap()
    {
        if (nearbyZonalDistanceMap == null)
        {
            nearbyZonalDistanceMap = new HashMap<>();
            ShortestPathStrategy<SandagBikeNode> sps = new ParallelSingleSourceDijkstra<SandagBikeNode>(
                    new RepeatedSingleSourceDijkstra<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal>(
                            network, new SandagBikeAccessibleDistanceEvaluator(),
                            new ZeroTraversalEvaluator()),
                    ParallelSingleSourceDijkstra.ParallelMethod.QUEUE);
            if (zonalCentroidIdMap == null)
            {
                createZonalCentroidIdMap();
            }
            Set<SandagBikeNode> originNodes = new HashSet<>();
            Set<SandagBikeNode> destinationNodes = new HashSet<>();
            Map<SandagBikeNode, Integer> inverseOriginZonalCentroidMap = new HashMap<>();
            Map<SandagBikeNode, Integer> inverseDestinationZonalCentroidMap = new HashMap<>();
            SandagBikeNode n;
            Map<Integer, Integer> relevantOriginZonalCentroidIdMap = getOriginZonalCentroidIdMap();
            Map<Integer, Integer> destinationZonalCentroidIdMap = getDestinationZonalCentroidIdMap();
            for (int zone : relevantOriginZonalCentroidIdMap.keySet())
            {
                n = network.getNode(zonalCentroidIdMap.get(zone));
                originNodes.add(n);
                inverseOriginZonalCentroidMap.put(n, zone);
            }
            for (int zone : destinationZonalCentroidIdMap.keySet())
            {
                n = network.getNode(zonalCentroidIdMap.get(zone));
                destinationNodes.add(n);
                inverseDestinationZonalCentroidMap.put(n, zone);
            }
            System.out.println("Calculating nearby Zonal Distance Map");
            ShortestPathResultSet<SandagBikeNode> resultSet = sps.getShortestPaths(originNodes,
                    destinationNodes, Double.parseDouble(propertyMap.get(PROPERTIES_MAXDIST_ZONE)));
            int originZone, destinationZone;
            for (NodePair<SandagBikeNode> odPair : resultSet)
            {
                originZone = inverseOriginZonalCentroidMap.get(odPair.getFromNode());
                destinationZone = inverseDestinationZonalCentroidMap.get(odPair.getToNode());
                if (!nearbyZonalDistanceMap.containsKey(originZone))
                {
                    nearbyZonalDistanceMap.put(originZone, new HashMap<Integer, Double>());
                }
                nearbyZonalDistanceMap.get(originZone).put(destinationZone,
                        resultSet.getShortestPathResult(odPair).getCost());
            }
        }
        return nearbyZonalDistanceMap;
    }

    @Override
    public Map<Integer, Integer> getOriginZonalCentroidIdMap()
    {
        if (zonalCentroidIdMap == null)
        {
            createZonalCentroidIdMap();
        }

        if (isTraceExclusive())
        {
            Map<Integer, Integer> m = new HashMap<>();
            for (int o : getTraceOrigins())
            {
                m.put(o, zonalCentroidIdMap.get(o));
            }
            return m;
        } else return zonalCentroidIdMap;
    }

    public Map<Integer, Integer> getOriginZonalCentroidIdMapNonExclusiveOfTrace()
    {
        if (zonalCentroidIdMap == null)
        {
            createZonalCentroidIdMap();
        }

        return zonalCentroidIdMap;
    }

    @Override
    public Map<Integer, Integer> getDestinationZonalCentroidIdMap()
    {
        return getOriginZonalCentroidIdMapNonExclusiveOfTrace();
    }

    @Override
    public Map<String, String> getPropertyMap()
    {
        return propertyMap;
    }

    protected abstract void createZonalCentroidIdMap();

    public Map<Integer, Integer> getInverseOriginZonalCentroidIdMap()
    {
        HashMap<Integer, Integer> newMap = new HashMap<>();
        Map<Integer, Integer> origMap = getOriginZonalCentroidIdMap();
        for (Integer o : origMap.keySet())
        {
            newMap.put(origMap.get(o), o);
        }
        return newMap;
    }

    public Map<Integer, Integer> getInverseDestinationZonalCentroidIdMap()
    {
        HashMap<Integer, Integer> newMap = new HashMap<>();
        Map<Integer, Integer> origMap = getDestinationZonalCentroidIdMap();
        for (Integer d : origMap.keySet())
        {
            newMap.put(origMap.get(d), d);
        }
        return newMap;
    }

    @Override
    public boolean isTraceExclusive()
    {
        return Boolean.parseBoolean(propertyMap.get(PROPERTIES_TRACE_EXCLUSIVE));
    }

    private class RandomizedEdgeCostEvaluator
            implements EdgeEvaluator<SandagBikeEdge>
    {
        long   seed;
        Random random;
        double cDistCla0, cDistCla1, cDistCla2, cDistCla3, cArtNe2, cWrongWay, cCycTrac, cBikeBlvd,
                cGain;

        public RandomizedEdgeCostEvaluator(long seed)
        {
            this.seed = seed;

            if (isRandomCostSeeded())
            {
                random = new Random(seed);
            } else
            {
                random = new Random();
            }

            cDistCla0 = Double.parseDouble(propertyMap.get(PROPERTIES_COEF_DISTCLA0))
                    * (1 + Double.parseDouble(propertyMap.get(PROPERTIES_RANDOM_SCALE_COEF))
                            * (2 * random.nextDouble() - 1));
            cDistCla1 = Double.parseDouble(propertyMap.get(PROPERTIES_COEF_DISTCLA1))
                    * (1 + Double.parseDouble(propertyMap.get(PROPERTIES_RANDOM_SCALE_COEF))
                            * (2 * random.nextDouble() - 1));
            cDistCla2 = Double.parseDouble(propertyMap.get(PROPERTIES_COEF_DISTCLA2))
                    * (1 + Double.parseDouble(propertyMap.get(PROPERTIES_RANDOM_SCALE_COEF))
                            * (2 * random.nextDouble() - 1));
            cDistCla3 = Double.parseDouble(propertyMap.get(PROPERTIES_COEF_DISTCLA3))
                    * (1 + Double.parseDouble(propertyMap.get(PROPERTIES_RANDOM_SCALE_COEF))
                            * (2 * random.nextDouble() - 1));
            cArtNe2 = Double.parseDouble(propertyMap.get(PROPERTIES_COEF_DARTNE2))
                    * (1 + Double.parseDouble(propertyMap.get(PROPERTIES_RANDOM_SCALE_COEF))
                            * (2 * random.nextDouble() - 1));
            cWrongWay = Double.parseDouble(propertyMap.get(PROPERTIES_COEF_DWRONGWY))
                    * (1 + Double.parseDouble(propertyMap.get(PROPERTIES_RANDOM_SCALE_COEF))
                            * (2 * random.nextDouble() - 1));
            cCycTrac = Double.parseDouble(propertyMap.get(PROPERTIES_COEF_DCYCTRAC))
                    * (1 + Double.parseDouble(propertyMap.get(PROPERTIES_RANDOM_SCALE_COEF))
                            * (2 * random.nextDouble() - 1));
            cBikeBlvd = Double.parseDouble(propertyMap.get(PROPERTIES_COEF_DBIKBLVD))
                    * (1 + Double.parseDouble(propertyMap.get(PROPERTIES_RANDOM_SCALE_COEF))
                            * (2 * random.nextDouble() - 1));
            cGain = Double.parseDouble(propertyMap.get(PROPERTIES_COEF_GAIN))
                    * (1 + Double.parseDouble(propertyMap.get(PROPERTIES_RANDOM_SCALE_COEF))
                            * (2 * random.nextDouble() - 1));

        }

        public double evaluate(SandagBikeEdge edge)
        {

            if (isRandomCostSeeded())
            {
                random = new Random(Objects.hash(seed, edge));
            } else
            {
                random = new Random();
            }

            return (edge.distance
                    * ((cDistCla0 * ((edge.bikeClass < 1 ? 1 : 0) + (edge.bikeClass > 3 ? 1 : 0))
                            + cDistCla1 * (edge.bikeClass == 1 ? 1 : 0) + cDistCla2
                            * (edge.bikeClass == 2 ? 1 : 0) * (edge.cycleTrack ? 0 : 1) + cDistCla3
                            * (edge.bikeClass == 3 ? 1 : 0) * (edge.bikeBlvd ? 0 : 1) + cArtNe2
                            * (edge.bikeClass != 2 && edge.bikeClass != 1 ? 1 : 0)
                            * ((edge.functionalClass < 5 && edge.functionalClass > 0) ? 1 : 0)
                            + cWrongWay * (edge.bikeClass != 1 ? 1 : 0) * (edge.lanes == 0 ? 1 : 0)
                            + cCycTrac * (edge.cycleTrack ? 1 : 0) + cBikeBlvd
                            * (edge.bikeBlvd ? 1 : 0)) + cGain * edge.gain)
                    * (1 + Double.parseDouble(propertyMap.get(PROPERTIES_RANDOM_SCALE_LINK))
                            * (random.nextBoolean() ? 1 : -1)) + INACCESSIBLE_COST_COEF
                    * ((edge.functionalClass < 3 && edge.functionalClass > 0) ? 1 : 0));
        }
    }

    public EdgeEvaluator<SandagBikeEdge> getRandomizedEdgeCostEvaluator(int iter, long seed)
    {

        if (iter == 1)
        {
            return getEdgeCostEvaluator();
        } else
        {
            return new RandomizedEdgeCostEvaluator(seed);
        }

    }

    public boolean isIntrazonalsNeeded()
    {
        return true;
    }
}
