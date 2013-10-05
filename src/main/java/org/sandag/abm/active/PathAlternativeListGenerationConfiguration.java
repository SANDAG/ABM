package org.sandag.abm.active;

import java.util.*;

public interface PathAlternativeListGenerationConfiguration<N extends Node, E extends Edge<N>, T extends Traversal<E>>
{
    public Network<N,E,T> getNetwork();
    public EdgeEvaluator<E> getPathSizeOverlapEvaluator();
    public EdgeEvaluator<E> getEdgeCostEvaluator();
    public TraversalEvaluator<T> getTraversalCostEvaluator();
    public double getMaxCost();
    public int getMaxRandomizationIters();
    public double[] getRandomizationLowerBounds();
    public double[] getRandomizationUpperBounds();
    public double[] getPathSizeDistanceBreaks();
    public double[] getPathSizeOversampleTargets(); 
    public boolean isRandomCostSeeded();
    public Map<Integer,Map<Integer,Double>> getNearbyZonalDistanceMap();
    public Map<Integer,Integer> getZonalCentroidIdMap(); 
}
