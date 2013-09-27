package org.sandag.abm.active;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractShortestPath implements ShortestPath {
	private final NetworkInterface network;
	
	public AbstractShortestPath(NetworkInterface network) {
		this.network = network;
	}
	
	protected NetworkInterface getNetwork() {
		return network;
	}

	@Override
	public List<Node> getOrderedNodeList() {
		List<Node> nodes = new LinkedList<>();
		Iterator<Node> centroidIterator = network.centroidIterator();
		while (centroidIterator.hasNext()) 
			nodes.add(centroidIterator.next());
		return nodes;
	}

	@Override
	public OriginShortestPathResult getShortestPaths(Node originNode) {
		return getShortestPaths(originNode,Double.POSITIVE_INFINITY);
	}

}
