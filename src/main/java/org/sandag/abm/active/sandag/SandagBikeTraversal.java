package org.sandag.abm.active.sandag;

import org.sandag.abm.active.SimpleTraversal;

public class SandagBikeTraversal extends SimpleTraversal<SandagBikeEdge>
{
	public volatile TurnType turnType;
	public volatile double cost;
	public volatile boolean thruCentroid, signalExclRightAndThruJunction, unsigLeftFromMajorArt, unsigLeftFromMinorArt, unsigCrossMajorArt, unsigCrossMinorArt;
	
    public SandagBikeTraversal(SandagBikeEdge fromEdge, SandagBikeEdge toEdge) {
		super(fromEdge, toEdge);
	}
	
    public SandagBikeTraversal(SandagBikeEdge edge) {
		super(null,edge);
	}
}
