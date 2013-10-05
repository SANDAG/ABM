package org.sandag.abm.active.sandag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.sandag.abm.active.Network;
import org.sandag.abm.active.NodePair;
import org.sandag.abm.active.ParallelSingleSourceDijkstra;
import org.sandag.abm.active.RepeatedSingleSourceDijkstra;
import org.sandag.abm.active.ShortestPathResultSet;
import org.sandag.abm.active.ShortestPathStrategy;
import org.sandag.abm.active.sandag.SandagBikePathAlternativeListGenerationConfiguration.SandagBikeDistanceEvaluator;
import org.sandag.abm.active.sandag.SandagBikePathAlternativeListGenerationConfiguration.ZeroTraversalEvaluator;

public abstract class SandagBikeTazPathAlternativeListGenerationConfiguration extends SandagBikePathAlternativeListGenerationConfiguration
{
    
    private SandagBikeTazPathAlternativeListGenerationConfiguration(Map<String,String> propertyMap, Network<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal> network)
    {
        super(propertyMap, network);
        this.PROPERTIES_MAXDIST_ZONE = "active.maxdist.taz";
    }
    
    protected void createZonalCentroidIdMap()
    {
        zonalCentroidIdMap = new HashMap<Integer,Integer>();
        Iterator<SandagBikeNode> nodeIterator = network.nodeIterator();
        SandagBikeNode n;
        while ( nodeIterator.hasNext() ) {
            n = nodeIterator.next();
            if ( n.taz > 0 ) { zonalCentroidIdMap.put((int) n.taz, n.getId()); }
        }
    }

}
