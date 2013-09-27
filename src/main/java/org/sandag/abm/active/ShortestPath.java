package org.sandag.abm.active;

import java.util.List;


public interface ShortestPath {
	List<Node> getOrderedNodeList();
	OriginShortestPathResult getShortestPaths(Node originNode, double maxCost);
	OriginShortestPathResult getShortestPaths(Node originNode);
}
