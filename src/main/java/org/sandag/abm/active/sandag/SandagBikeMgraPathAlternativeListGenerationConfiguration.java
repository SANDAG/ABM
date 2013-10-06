package org.sandag.abm.active.sandag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.sandag.abm.active.Network;

public class SandagBikeMgraPathAlternativeListGenerationConfiguration extends SandagBikePathAlternativeListGenerationConfiguration
{
    
    public SandagBikeMgraPathAlternativeListGenerationConfiguration(Map<String,String> propertyMap, Network<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal> network)
    {
        super(propertyMap, network);
        this.PROPERTIES_MAXDIST_ZONE = "active.maxdist.mgra";
    }
    
    protected void createZonalCentroidIdMap()
    {
        zonalCentroidIdMap = new HashMap<Integer,Integer>();
        Iterator<SandagBikeNode> nodeIterator = network.nodeIterator();
        SandagBikeNode n;
        while ( nodeIterator.hasNext() ) {
            n = nodeIterator.next();
            if ( n.taz > 0 ) { zonalCentroidIdMap.put((int) n.mgra, n.getId()); }
        }
    }

}
