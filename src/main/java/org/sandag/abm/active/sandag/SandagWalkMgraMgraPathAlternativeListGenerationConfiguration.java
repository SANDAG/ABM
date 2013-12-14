package org.sandag.abm.active.sandag;

import java.util.*;
import org.sandag.abm.active.*;

public class SandagWalkMgraMgraPathAlternativeListGenerationConfiguration extends SandagWalkPathAlternativeListGenerationConfiguration
{
    
    public SandagWalkMgraMgraPathAlternativeListGenerationConfiguration(Map<String,String> propertyMap, Network<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal> network)
    {
        super(propertyMap, network);
        this.PROPERTIES_MAXDIST_ZONE = "active.maxdist.walk.mgra";
    }
    
    protected void createOriginZonalCentroidIdMap()
    {
        System.out.println("Creating MGRA Origin Zonal Centroid Id Map...");
        originZonalCentroidIdMap = new HashMap<Integer,Integer>();
        Iterator<SandagBikeNode> nodeIterator = network.nodeIterator();
        SandagBikeNode n;
        while ( nodeIterator.hasNext() ) {
            n = nodeIterator.next();
            if ( n.mgra > 0 ) { originZonalCentroidIdMap.put((int) n.mgra, n.getId()); }
        }
    }
    
    protected void createDestinationZonalCentroidIdMap()
    {
        System.out.println("Creating MGRA Destination Zonal Centroid Id Map...");
        destinationZonalCentroidIdMap = new HashMap<Integer,Integer>();
        Iterator<SandagBikeNode> nodeIterator = network.nodeIterator();
        SandagBikeNode n;
        while ( nodeIterator.hasNext() ) {
            n = nodeIterator.next();
            if ( n.mgra > 0 ) { destinationZonalCentroidIdMap.put((int) n.mgra, n.getId()); }
        }
    }

}
