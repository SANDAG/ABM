package org.sandag.abm.active.sandag;

import org.sandag.abm.active.SimpleEdge;

public class SandagBikeEdge
        extends SimpleEdge<SandagBikeNode>
{
    public volatile byte bikeClass, lanes, functionalClass;
    public volatile boolean centroidConnector, autosPermitted, cycleTrack, bikeBlvd;
    public volatile float   distance;
    public volatile short   gain;
    public volatile double  bikeCost, walkCost;

    public SandagBikeEdge(SandagBikeNode fromNode, SandagBikeNode toNode)
    {
        super(fromNode, toNode);
    }
}
