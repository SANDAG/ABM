package org.sandag.abm.active;

import java.util.Iterator;
import java.util.List;

public interface NetworkInterface {

	Iterator<Node> nodeIterator();

	Iterator<Edge> edgeIterator();
	
	List<Node> getSuccessors(Node node);
	
	Iterator<Node> centroidIterator();

}