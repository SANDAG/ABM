package org.sandag.abm.active.sandag;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.sandag.abm.active.Network;

public class SandagWalkTapMgraPathAlternativeListGenerationConfiguration
        extends SandagWalkPathAlternativeListGenerationConfiguration
{

    public SandagWalkTapMgraPathAlternativeListGenerationConfiguration(
            Map<String, String> propertyMap,
            Network<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> network)
    {
        super(propertyMap, network);
        this.PROPERTIES_MAXDIST_ZONE = Double.max(
            Double.max(
                Double.parseDouble(propertyMap.get("active.maxdist.walk.tap")),
                Double.parseDouble(propertyMap.get("active.maxdist.micromobility.tap"))),
            Double.parseDouble(propertyMap.get("active.maxdist.microtransit.tap")));
        this.PROPERTIES_TRACE_ORIGINS = "active.trace.origins.tap";
    }

    protected void createOriginZonalCentroidIdMap()
    {
        System.out.println("Creating TAP Zonal Centroid Id Map...");
        originZonalCentroidIdMap = new HashMap<Integer, Integer>();
        Iterator<SandagBikeNode> nodeIterator = network.nodeIterator();
        SandagBikeNode n;
        while (nodeIterator.hasNext())
        {
            n = nodeIterator.next();
            if (n.tap > 0)
            {
                originZonalCentroidIdMap.put((int) n.tap, n.getId());
            }
        }
    }

    protected void createDestinationZonalCentroidIdMap()
    {
        System.out.println("Creating MGRA Zonal Centroid Id Map...");
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
        return false;
    }

}
