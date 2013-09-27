package org.sandag.abm.active;

import java.util.Objects;

public class NodePair implements Comparable<NodePair>{
	private final Node originNode;
	private final Node destinationNode;
	
	public NodePair(Node originNode, Node destinationNode) {
		this.originNode = originNode;
		this.destinationNode = destinationNode;
	}
	
	public int compareTo(NodePair other) {
		int c = originNode.compareTo(other.originNode);
		if (c == 0)
			c = destinationNode.compareTo(other.destinationNode);
		return c;
	}
	
	public Node getOriginNode() {
		return originNode;
	}
	
	public Node getDestinationNode() {
		return destinationNode;
	}
	
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (!(other instanceof NodePair))
			return false;
		NodePair np = (NodePair) other;
		return (originNode == np.originNode) && (destinationNode == np.destinationNode);
	}
	
	public int hashCode() {
		return Objects.hash(originNode,destinationNode);
	}
}
