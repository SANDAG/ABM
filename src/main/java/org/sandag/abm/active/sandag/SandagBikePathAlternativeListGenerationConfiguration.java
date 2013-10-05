package org.sandag.abm.active.sandag;

import java.util.*;
import org.sandag.abm.active.*;

public abstract class SandagBikePathAlternativeListGenerationConfiguration implements PathAlternativeListGenerationConfiguration<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal>
{
    protected Map<String,String> propertyMap;
    protected PropertyParser propertyParser;
    protected final String PROPERTIES_MAXCOST = "active.generation.maxcost";
    protected final String PROPERTIES_RANDOM_MAXITERS = "active.generation.random.maxiters";
    protected final String PROPERTIES_RANDOM_LOWER = "active.generation.random.lower";
    protected final String PROPERTIES_RANDOM_UPPER = "active.generation.random.upper";
    protected final String PROPERTIES_RANDOM_SEEDED = "active.pathsize.random.seeded";
    protected final String PROPERTIES_PATHSIZE_DISTANCE_BREAKS = "active.pathsize.distance.breaks";
    protected final String PROPERTIES_PATHSIZE_DISTANCE_SIZES = "active.pathsize.distance.sizes";
    protected final String PROPERTIES_PATHSIZE_SAMPLE_MULTIPLE = "active.pathsize.sample.multiple";
    
    protected String PROPERTIES_MAXDIST_ZONE;
    
    protected Map<Integer,Map<Integer,Double>> nearbyZonalDistanceMap;
    protected Map<Integer,Integer> zonalCentroidIdMap;
    protected Network<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal> network;
    
    public SandagBikePathAlternativeListGenerationConfiguration(Map<String,String> propertyMap, Network<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal> network)
    {
        this.propertyMap = propertyMap;
        this.propertyParser = new PropertyParser(propertyMap);
        this.nearbyZonalDistanceMap = null;
        this.zonalCentroidIdMap = null;
        this.network = network;
    }
    
    @Override
    public Network<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> getNetwork()
    {
        return network;
    }
    
    static class SandagBikeDistanceEvaluator implements EdgeEvaluator<SandagBikeEdge>
    {            
        public double evaluate(SandagBikeEdge edge) { return edge.distance; }
    }
    
    static class ZeroTraversalEvaluator implements TraversalEvaluator<SandagBikeTraversal>
    {
        public double evaluate(SandagBikeTraversal traversal) { return 0.0; }
    }
    
    @Override
    public EdgeEvaluator<SandagBikeEdge> getPathSizeOverlapEvaluator()
    {
        return new SandagBikeDistanceEvaluator();
    }

    @Override
    public EdgeEvaluator<SandagBikeEdge> getEdgeCostEvaluator()
    {
        final class SandagBikeEdgeCostEvaluator implements EdgeEvaluator<SandagBikeEdge>
        {
            public double evaluate(SandagBikeEdge edge) { return edge.cost; }
        }
        
        return new SandagBikeEdgeCostEvaluator();
    }

    @Override
    public TraversalEvaluator<SandagBikeTraversal> getTraversalCostEvaluator()
    {
        final class SandagBikeTraversalCostEvaluator implements TraversalEvaluator<SandagBikeTraversal>
        {
            public double evaluate(SandagBikeTraversal traversal) { return traversal.cost; }
        }
        
        return new SandagBikeTraversalCostEvaluator();
    }

    @Override
    public double getMaxCost()
    {
        return Double.parseDouble(propertyMap.get(PROPERTIES_MAXCOST));
    }

    @Override
    public int getMaxRandomizationIters()
    {
        return Integer.parseInt(propertyMap.get(PROPERTIES_RANDOM_MAXITERS));
    }

    @Override
    public double[] getRandomizationLowerBounds()
    {
        return propertyParser.parseDoublePropertyArray(PROPERTIES_RANDOM_LOWER);
    }

    @Override
    public double[] getRandomizationUpperBounds()
    {
        return propertyParser.parseDoublePropertyArray(PROPERTIES_RANDOM_UPPER);
    }

    @Override
    public double[] getPathSizeDistanceBreaks()
    {
        return propertyParser.parseDoublePropertyArray(PROPERTIES_PATHSIZE_DISTANCE_BREAKS);
    }

    @Override
    public double[] getPathSizeOversampleTargets()
    {
        double[] array = propertyParser.parseDoublePropertyArray(PROPERTIES_PATHSIZE_DISTANCE_SIZES);
        for (int i=0; i<array.length; i++)
            array[i] = array[i] * Double.parseDouble(propertyMap.get(PROPERTIES_PATHSIZE_SAMPLE_MULTIPLE));
        return array;
    }

    @Override
    public boolean isRandomCostSeeded()
    {
        return Boolean.parseBoolean(propertyMap.get(PROPERTIES_RANDOM_SEEDED));
    }

    @Override
    public Map<Integer, Map<Integer, Double>> getNearbyZonalDistanceMap()
    {
        if ( nearbyZonalDistanceMap == null ) {
            nearbyZonalDistanceMap = new HashMap<>();
            ShortestPathStrategy<SandagBikeNode> sps = new ParallelSingleSourceDijkstra<SandagBikeNode>(new RepeatedSingleSourceDijkstra<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal>(network, new SandagBikeDistanceEvaluator(), new ZeroTraversalEvaluator()), ParallelSingleSourceDijkstra.ParallelMethod.FORK_JOIN);
            if ( zonalCentroidIdMap == null ) {
                createZonalCentroidIdMap();
            }
            Set<SandagBikeNode> nodes = new HashSet<>();
            Map<SandagBikeNode,Integer> inverseZonalCentroidMap = new HashMap<>();
            SandagBikeNode n;
            for ( int zone : zonalCentroidIdMap.keySet() ) {
                n = network.getNode(zonalCentroidIdMap.get(zone));
                nodes.add(n);
                inverseZonalCentroidMap.put(n, zone);
            }
            ShortestPathResultSet<SandagBikeNode> resultSet = sps.getShortestPaths(nodes, nodes, Double.parseDouble(propertyMap.get(PROPERTIES_MAXDIST_ZONE)));
            int originZone, destinationZone;
            for (NodePair<SandagBikeNode> odPair : resultSet) {
                originZone = inverseZonalCentroidMap.get(odPair.getFromNode());
                destinationZone = inverseZonalCentroidMap.get(odPair.getToNode());
                if ( nearbyZonalDistanceMap.containsKey(originZone) ) {
                    nearbyZonalDistanceMap.put(originZone, new HashMap<Integer,Double>() );
                }
                nearbyZonalDistanceMap.get(originZone).put(destinationZone,resultSet.getShortestPathResult(odPair).getCost());
            }
        }
        return nearbyZonalDistanceMap;
    }
    
    @Override
    public Map<Integer, Integer> getZonalCentroidIdMap()
    {
        if (zonalCentroidIdMap == null) {
            createZonalCentroidIdMap();
        }
        
        return zonalCentroidIdMap;
    }
    
    protected abstract void createZonalCentroidIdMap();
    
}
