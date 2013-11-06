package org.sandag.abm.active.sandag;

import org.sandag.abm.active.*;
import java.util.*;

public class SandagBikePathAlternatives {
	private final PathAlternativeList<SandagBikeNode,SandagBikeEdge> pathAlternativeList;
	private List<Double> distance, distClass1, distClass2, distClass3, distArtNoLane, distWrongWay, gain, turns;
	private Network<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal> network;
	
	public SandagBikePathAlternatives(PathAlternativeList<SandagBikeNode,SandagBikeEdge> pathAlternativeList) {
		this.pathAlternativeList = pathAlternativeList;
		this.network = (Network<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal>) pathAlternativeList.getNetwork();
		calcAndStoreAttributes();
	}
	
	private void calcAndStoreAttributes() {
	    distance = new ArrayList<>();
	    distClass1 = new ArrayList<>();
	    distClass2 = new ArrayList<>();
	    distClass3 = new ArrayList<>();
	    distArtNoLane = new ArrayList<>();
	    distWrongWay = new ArrayList<>();
	    gain = new ArrayList<>();
	    turns = new ArrayList<>();
	    
	    for (int i=0; i<getPathCount(); i++) {
	        distance.add(0.0);
	        distClass1.add(0.0);
	        distClass2.add(0.0);
	        distClass3.add(0.0);
	        distArtNoLane.add(0.0);
	        distWrongWay.add(0.0);
	        gain.add(0.0);
	        turns.add(0.0);
	        SandagBikeNode parent = null, grandparent = null; 
	        for(SandagBikeNode current : pathAlternativeList.get(i)) {
	            if ( parent != null ) {
	                SandagBikeEdge edge = network.getEdge(parent,current);
	                distance.set(i, distance.get(i) + edge.distance);
	                distClass1.set(i, distClass1.get(i) + edge.distance * ( edge.bikeClass == 1 ? 1 : 0 ) );
	                distClass2.set(i, distClass2.get(i) + edge.distance * ( edge.bikeClass == 2 ? 1 : 0 ) );
	                distClass3.set(i, distClass3.get(i) + edge.distance * ( edge.bikeClass == 3 ? 1 : 0 ) );
	                distArtNoLane.set(i, distArtNoLane.get(i) + edge.distance * ( edge.bikeClass != 2 && edge.bikeClass != 1 ? 1 : 0 ) * ( ( edge.functionalClass < 5 && edge.functionalClass > 0 ) ? 1 : 0 ) );
	                distWrongWay.set(i, distWrongWay.get(i) + edge.distance * ( edge.bikeClass != 1 ? 1 : 0 ) * ( edge.lanes == 0 ? 1 : 0 ) );
	                gain.set(i, gain.get(i) + edge.gain);
	                if ( grandparent != null ) {
	                    SandagBikeEdge fromEdge = network.getEdge(grandparent,parent);
	                    SandagBikeTraversal traversal = network.getTraversal(fromEdge, edge);
	                    turns.set(i, turns.get(i) + ( traversal.turnType != TurnType.NONE  ? 1 : 0 ) );
	                }
	            }
	            grandparent = parent;
	            parent = current;
	        }
	    }
	}

	public double getSizeAlt(int path) {
		return pathAlternativeList.getSizeMeasures().get(path) / pathAlternativeList.getSizeMeasureTotal();
	}

	public double getDistanceAlt(int path) {
		return distance.get(path);
	}

	public double getDistanceClass1Alt(int path) {
	    return distClass1.get(path);
	}

	public double getDistanceClass2Alt(int path) {
	    return distClass2.get(path);
	}

	public double getDistanceClass3Alt(int path) {
	    return distClass3.get(path);
	}

	public double getDistanceArtNoLaneAlt(int path) {
	    return distArtNoLane.get(path);
	}

	public double getDistanceCycleTrackAlt(int path) {
	    // TODO implement when network data contains Cycle Track field
	    return 0.0;
	}

	public double getDistanceBikeBlvdAlt(int path) {
	    // TODO implement when network data contains Bike Blvd field
		return 0.0;
	}

	public double getDistanceWrongWayAlt(int path) {
	    return distWrongWay.get(path);
	}

	public double getGainAlt(int path) {
	    return gain.get(path);
	}

	public double getTurnsAlt(int path) {
	    return turns.get(path);
	}

	public int getPathCount() {
		return pathAlternativeList.getCount();
	}

}
