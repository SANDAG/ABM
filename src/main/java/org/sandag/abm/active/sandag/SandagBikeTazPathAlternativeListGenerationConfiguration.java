package org.sandag.abm.active.sandag;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.sandag.abm.active.Network;

public class SandagBikeTazPathAlternativeListGenerationConfiguration
        extends SandagBikePathAlternativeListGenerationConfiguration
{

    public SandagBikeTazPathAlternativeListGenerationConfiguration(Map<String, String> propertyMap,
            Network<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> network)
    {
        super(propertyMap, network);
        this.PROPERTIES_MAXDIST_ZONE = "active.maxdist.bike.taz";
        this.PROPERTIES_TRACE_ORIGINS = "active.trace.origins.taz";
    }

    protected void createZonalCentroidIdMap()
    {
        zonalCentroidIdMap = new HashMap<Integer, Integer>();
        Iterator<SandagBikeNode> nodeIterator = network.nodeIterator();
        SandagBikeNode n;
        while (nodeIterator.hasNext())
        {
            n = nodeIterator.next();
            if (n.taz > 0)
            {
                zonalCentroidIdMap.put((int) n.taz, n.getId());
            }
        }
    }

}
