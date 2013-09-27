package org.sandag.abm.active;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NonContiguousArrayShortestPathResults extends ArrayShortestPathResults {
	private final Map<Node,Integer> nodeToArrayOriginIndex;
	private final Map<Node,Integer> nodeToArrayDestinationIndex;
	
	public NonContiguousArrayShortestPathResults(Path[][] paths, double[][] costs, 
			                                     List<Node> orderedOrigins, List<Node> orderedDestinations) {
		super(paths,costs,orderedOrigins,orderedDestinations);
		nodeToArrayOriginIndex = new HashMap<>();
		nodeToArrayDestinationIndex = new HashMap<>();
		int counter = 0;
		for (Node origin : orderedOrigins)
			nodeToArrayOriginIndex.put(origin,counter++);
		counter = 0;
		for (Node destination : orderedDestinations)
			nodeToArrayDestinationIndex.put(destination,counter++);
	}
	
	protected int getOriginIndex(Node origin) {
		return nodeToArrayOriginIndex.get(origin);
	}
	
	protected int getDestinationIndex(Node destination) {
		return nodeToArrayDestinationIndex.get(destination);
	}
}
