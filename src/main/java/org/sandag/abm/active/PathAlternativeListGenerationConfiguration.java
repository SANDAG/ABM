package org.sandag.abm.active;

import java.util.*;

public interface PathAlternativeListGenerationConfiguration<N extends Node, E extends Edge<N>, T extends Traversal<E>>
{
    public Network<N,E,T> getNetwork();
    public EdgeEvaluator<E> getEdgeLengthEvaluator();
    public EdgeEvaluator<E> getEdgeCostEvaluator();
    public TraversalEvaluator<T> getTraversalCostEvaluator();
    public double getMaxCost();
    public double[] getRandomizationScales();
    public double[] getSampleDistanceBreaks();
    public double[] getSamplePathSizes();
    public double[] getSampleMinCounts();
    public double[] getSampleMaxCounts();
    public boolean isRandomCostSeeded();
    public Map<Integer,Map<Integer,Double>> getNearbyZonalDistanceMap();
    public Map<Integer,Integer> getOriginZonalCentroidIdMap();
    public Map<Integer,Integer> getDestinationZonalCentroidIdMap();
    public String getOutputDirectory();
    public Set<Integer> getTraceOrigins();
    public Map<String,String> getPropertyMap();
    public Map<Integer,Integer> getInverseOriginZonalCentroidIdMap(); 
    public Map<Integer,Integer> getInverseDestinationZonalCentroidIdMap();
}
