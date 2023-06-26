package org.sandag.abm.active.sandag;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.sandag.abm.active.Network;

public class SandagWalkMgraMgraPathAlternativeListGenerationConfiguration
        extends SandagWalkPathAlternativeListGenerationConfiguration
{

    public SandagWalkMgraMgraPathAlternativeListGenerationConfiguration(
            Map<String, String> propertyMap,
            Network<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> network)
    {
        super(propertyMap, network);
        this.PROPERTIES_MAXDIST_ZONE = Math.max(
            Math.max(
                Double.parseDouble(propertyMap.get("active.maxdist.walk.mgra")),
                Double.parseDouble(propertyMap.get("active.maxdist.micromobility.mgra"))),
            Double.parseDouble(propertyMap.get("active.maxdist.microtransit.mgra")));
        this.PROPERTIES_TRACE_ORIGINS = "active.trace.origins.mgra";
    }

    protected void createOriginZonalCentroidIdMap()
    {
        System.out.println("Creating MGRA Origin Zonal Centroid Id Map...");
        originZonalCentroidIdMap = new HashMap<Integer, Integer>();
        Iterator<SandagBikeNode> nodeIterator = network.nodeIterator();
        SandagBikeNode n;
        while (nodeIterator.hasNext())
        {
            n = nodeIterator.next();
            if (n.mgra > 0)
            {
                originZonalCentroidIdMap.put((int) n.mgra, n.getId());
            }
        }
    }

    protected void createDestinationZonalCentroidIdMap()
    {
        System.out.println("Creating MGRA Destination Zonal Centroid Id Map...");
        destinationZonalCentroidIdMap = new HashMap<Integer, Integer>();
        Iterator<SandagBikeNode> nodeIterator = network.nodeIterator();
        SandagBikeNode n;
        while (nodeIterator.hasNext())
        {
            n = nodeIterator.next();
            if (n.mgra > 0)
            {
                destinationZonalCentroidIdMap.put((int) n.mgra, n.getId());
            }
        }
    }

    public boolean isIntrazonalsNeeded()
    {
        return true;
    }

}
