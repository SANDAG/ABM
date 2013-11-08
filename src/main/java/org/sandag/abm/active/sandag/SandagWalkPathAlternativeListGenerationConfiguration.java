package org.sandag.abm.active.sandag;

import java.util.*;
import org.sandag.abm.active.*;

public class SandagWalkPathAlternativeListGenerationConfiguration implements PathAlternativeListGenerationConfiguration<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal>
{  
    private SandagBikePathAlternativeListGenerationConfiguration bikeConfiguration;
    private static final String PROPERTIES_OUTPUT = "active.output.walk";
    
    public SandagWalkPathAlternativeListGenerationConfiguration(SandagBikePathAlternativeListGenerationConfiguration bikeConfiguration)
    {
        this.bikeConfiguration = bikeConfiguration;
    }

    public Set<Integer> getTraceOrigins()
    {
        return bikeConfiguration.getTraceOrigins();
    }

    public Network<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> getNetwork()
    {
        return bikeConfiguration.getNetwork();
    }

    public double getMaxCost()
    {
        return bikeConfiguration.getMaxCost();
    }

    public boolean isRandomCostSeeded()
    {
        return bikeConfiguration.isRandomCostSeeded();
    }

    public Map<Integer, Map<Integer, Double>> getNearbyZonalDistanceMap()
    {
        return bikeConfiguration.getNearbyZonalDistanceMap();
    }

    public Map<Integer, Integer> getZonalCentroidIdMap()
    {
        return bikeConfiguration.getZonalCentroidIdMap();
    }

    public Map<String, String> getPropertyMap()
    {
        return bikeConfiguration.getPropertyMap();
    }

    public Map<Integer, Integer> getInverseZonalCentroidIdMap()
    {
        return bikeConfiguration.getInverseZonalCentroidIdMap();
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
        return new SandagBikePathAlternativeListGenerationConfiguration.ZeroTraversalEvaluator();
    }

    @Override
    public double[] getRandomizationScales()
    {
        return new double[] {0.0};
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
    public String getOutputDirectory()
    {
        return bikeConfiguration.propertyMap.get(PROPERTIES_OUTPUT);
    }

    @Override
    public EdgeEvaluator<SandagBikeEdge> getEdgeLengthEvaluator()
    {
        return bikeConfiguration.getEdgeLengthEvaluator();
    }

    
    
}
