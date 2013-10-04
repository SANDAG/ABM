package org.sandag.abm.active;

import java.util.*;

public interface PathAlternativeListGenerationConfiguration<N extends Node, E extends Edge<N>, T extends Traversal<E>>
{
    public Network<N,E,T> getNetwork(); 
    public ShortestPath<N> getShortestPath();
    public EdgeEvaluator<E> getPathSizeOverlapEvaluator();
    public EdgeEvaluator<E> getEdgeCostEvaluator();
    public TraversalEvaluator<T> geTraversalCostEvaluator();
    public double getMaxCost();
    public int getMaxNRandomizations();
    public double[] getInitialRandomizationLowerBounds();
    public double[] getInitialRandomizationUpperBounds();
    public double getSubsequentRandomizationLowerBound();
    public double getSubsequentRandomizationUpperBound();
    public double[] getPathSizeDistanceBreaks();
    public double[] getPathSizeOversampleTargets(); 
    public boolean shouldSeedRandomCosts();
    public Map<Integer,Map<Integer,Double>> getNearbyZonalDistanceMap();
    public Map<Integer,Integer> getZonalCentroidIdMap(); 
}
