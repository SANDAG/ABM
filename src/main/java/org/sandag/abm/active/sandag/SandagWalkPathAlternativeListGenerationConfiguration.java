package org.sandag.abm.active.sandag;

import java.util.*;
import org.sandag.abm.active.*;

public abstract class SandagWalkPathAlternativeListGenerationConfiguration implements PathAlternativeListGenerationConfiguration<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal>
{
    protected Map<String,String> propertyMap;
    protected PropertyParser propertyParser;
    protected final String PROPERTIES_SAMPLE_MAXCOST = "active.sample.maxcost";
    protected final String PROPERTIES_OUTPUT = "active.output.walk";
    protected final String PROPERTIES_TRACE_EXCLUSIVE = "active.trace.exclusive";
    
    protected String PROPERTIES_MAXDIST_ZONE;
    protected String PROPERTIES_TRACE_ORIGINS;
    
    protected Map<Integer,Map<Integer,Double>> nearbyZonalDistanceMap;
    protected Map<Integer,Integer> originZonalCentroidIdMap;
    protected Map<Integer,Integer> destinationZonalCentroidIdMap;
    protected Network<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal> network;
    
    public SandagWalkPathAlternativeListGenerationConfiguration(Map<String,String> propertyMap, Network<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal> network)
    {
        this.propertyMap = propertyMap;
        this.propertyParser = new PropertyParser(propertyMap);
        this.nearbyZonalDistanceMap = null;
        this.originZonalCentroidIdMap = null;
        this.destinationZonalCentroidIdMap = null;
        this.network = network;
    }
    
    public Set<Integer> getTraceOrigins()
    {
        return propertyMap.containsKey(PROPERTIES_TRACE_ORIGINS) ? 
        		new HashSet<>(propertyParser.parseIntPropertyList(PROPERTIES_TRACE_ORIGINS)) :
        		new HashSet<Integer>();
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
    
    static class SandagBikeDistanceEvaluator implements EdgeEvaluator<SandagBikeEdge>
    {            
        public double evaluate(SandagBikeEdge edge) { return edge.distance; }
    }
    
    static class SandagWalkAccessibleDistanceEvaluator implements EdgeEvaluator<SandagBikeEdge>
    {            
        public double evaluate(SandagBikeEdge edge) { return edge.distance + (edge.walkCost > 998 ? 999 : 0); }
    }
    
    static class ZeroTraversalEvaluator implements TraversalEvaluator<SandagBikeTraversal>
    {
        public double evaluate(SandagBikeTraversal traversal) { return 999 * ( traversal.thruCentroid ? 1 : 0 ); }
    }
    
    @Override
    public EdgeEvaluator<SandagBikeEdge> getEdgeLengthEvaluator()
    {
        return new SandagBikeDistanceEvaluator();
    }

    @Override
    public EdgeEvaluator<SandagBikeEdge> getEdgeCostEvaluator()
    {
        final class SandagWalkEdgeCostEvaluator implements EdgeEvaluator<SandagBikeEdge>
        {
            public double evaluate(SandagBikeEdge edge) { return edge.walkCost; }
        }
        
        return new SandagWalkEdgeCostEvaluator();
    }

    @Override
    public TraversalEvaluator<SandagBikeTraversal> getTraversalCostEvaluator()
    {
        return new ZeroTraversalEvaluator();
    }

    @Override
    public double getMaxCost()
    {
        return Double.parseDouble(propertyMap.get(PROPERTIES_SAMPLE_MAXCOST));
    }

    @Override
    public double[] getSampleDistanceBreaks()
    {
        return new double[] {99.0};
    }

    @Override
    public double[] getSamplePathSizes()
    {
        return new double[] {1.0};
    }

    @Override
    public double[] getSampleMinCounts()
    {
        return new double[] {1.0};
    }

    @Override
    public double[] getSampleMaxCounts()
    {
        return new double[] {1.0};
    }
    
    @Override
    public boolean isRandomCostSeeded()
    {
        return false;
    }

    @Override
    public Map<Integer, Map<Integer, Double>> getNearbyZonalDistanceMap()
    {
        if ( nearbyZonalDistanceMap == null ) {
            nearbyZonalDistanceMap = new HashMap<>();
            ShortestPathStrategy<SandagBikeNode> sps = new ParallelSingleSourceDijkstra<SandagBikeNode>(new RepeatedSingleSourceDijkstra<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal>(network, new SandagWalkAccessibleDistanceEvaluator(), new ZeroTraversalEvaluator()), ParallelSingleSourceDijkstra.ParallelMethod.QUEUE);
            if ( originZonalCentroidIdMap == null ) {
                createOriginZonalCentroidIdMap();
            }
            if ( destinationZonalCentroidIdMap == null ) {
                createDestinationZonalCentroidIdMap();
            }
            Set<SandagBikeNode> originNodes = new HashSet<>();
            Set<SandagBikeNode> destinationNodes = new HashSet<>();
            Map<SandagBikeNode,Integer> inverseOriginZonalCentroidIdMap = new HashMap<>();
            Map<SandagBikeNode,Integer> inverseDestinationZonalCentroidIdMap = new HashMap<>();
            SandagBikeNode n;
            Map<Integer,Integer> relevantOriginZonalCentroidIdMap = getOriginZonalCentroidIdMap();
            for ( int zone : relevantOriginZonalCentroidIdMap.keySet() ) {
                n = network.getNode(originZonalCentroidIdMap.get(zone));
                originNodes.add(n);
                inverseOriginZonalCentroidIdMap.put(n, zone);
            }
            for ( int zone : destinationZonalCentroidIdMap.keySet() ) {
                n = network.getNode(destinationZonalCentroidIdMap.get(zone));
                destinationNodes.add(n);
                inverseDestinationZonalCentroidIdMap.put(n, zone);
            }
            System.out.println("Calculating nearby Zonal Distance Map");
            ShortestPathResultSet<SandagBikeNode> resultSet = sps.getShortestPaths(originNodes, destinationNodes, Double.parseDouble(propertyMap.get(PROPERTIES_MAXDIST_ZONE)));
            int originZone, destinationZone;
            for (NodePair<SandagBikeNode> odPair : resultSet) {
                    originZone = inverseOriginZonalCentroidIdMap.get(odPair.getFromNode());
                    destinationZone = inverseDestinationZonalCentroidIdMap.get(odPair.getToNode());
                    if ( ! nearbyZonalDistanceMap.containsKey(originZone) ) {
                        nearbyZonalDistanceMap.put(originZone, new HashMap<Integer,Double>() );
                    }
                    nearbyZonalDistanceMap.get(originZone).put(destinationZone,resultSet.getShortestPathResult(odPair).getCost());
            }
        }
        return nearbyZonalDistanceMap;
    }
    
    @Override
    public Map<Integer, Integer> getOriginZonalCentroidIdMap()
    {
        if (originZonalCentroidIdMap == null) {
            createOriginZonalCentroidIdMap();
        }
        
        if (isTraceExclusive()) {
            Map<Integer, Integer> m =  new HashMap<>();
            for (int o : getTraceOrigins()) {
                m.put(o, originZonalCentroidIdMap.get(o));
            }
            return m;
        }
        else return originZonalCentroidIdMap;
    }
    
    public Map<Integer, Integer> getOriginZonalCentroidIdMapNonExclusiveOfTrace()
    {
        if (originZonalCentroidIdMap == null) {
            createOriginZonalCentroidIdMap();
        }
        
        return originZonalCentroidIdMap;
    }
    
    @Override
    public Map<Integer, Integer> getDestinationZonalCentroidIdMap()
    {
        if (destinationZonalCentroidIdMap == null) {
            createDestinationZonalCentroidIdMap();
        }
        
        return destinationZonalCentroidIdMap;
    }
    
    @Override
    public Map<String,String> getPropertyMap() 
    {
    	return propertyMap;
    }
    
    protected abstract void createOriginZonalCentroidIdMap();
    protected abstract void createDestinationZonalCentroidIdMap();
    
    public Map<Integer,Integer> getInverseOriginZonalCentroidIdMap()
    {
        HashMap<Integer,Integer> newMap = new HashMap<>();
        Map<Integer,Integer> origMap = getOriginZonalCentroidIdMap();
        for (Integer o : origMap.keySet()) {
            newMap.put(origMap.get(o), o);
        }
        return newMap;
    }
    
    public Map<Integer,Integer> getInverseDestinationZonalCentroidIdMap()
    {
        HashMap<Integer,Integer> newMap = new HashMap<>();
        Map<Integer,Integer> origMap = getDestinationZonalCentroidIdMap();
        for (Integer o : origMap.keySet()) {
            newMap.put(origMap.get(o), o);
        }
        return newMap;
    }
    
    @Override
    public boolean isTraceExclusive() {
        return Boolean.parseBoolean(propertyMap.get(PROPERTIES_TRACE_EXCLUSIVE));
    }
    
    public EdgeEvaluator<SandagBikeEdge> getRandomizedEdgeCostEvaluator(int iter, long seed) {
        return getEdgeCostEvaluator();
    }
    
    @Override
    public abstract boolean isIntrazonalsNeeded();
}

