package org.sandag.abm.active;

import java.util.Objects;

public class NodePair<N extends Node> implements Comparable<NodePair<N>>{
	private final N fromNode;
	private final N toNode;
	
	public NodePair(N fromNode, N toNode) {
		this.fromNode = fromNode;
		this.toNode = toNode;
	}
	
	public int compareTo(NodePair<N> other) {
		int c = fromNode.compareTo(other.fromNode);
		if (c == 0)
			c = toNode.compareTo(other.toNode);
		return c;
	}
	
	public N getFromNode() {
		return fromNode;
	}
	
	public N getToNode() {
		return toNode;
	}
	
	public boolean equals(Object other) {
		if ((other == null) || (!(other instanceof NodePair)))
			return false;
		NodePair<?> np = (NodePair<?>) other;
		return (fromNode.equals(np.fromNode)) && (toNode.equals(np.toNode));
	}
	
	public int hashCode() {
		return Objects.hash(fromNode,toNode);
	}
	
	public String toString() {
		return "<od pair: " + fromNode + "," + toNode + ">";
	}
}
