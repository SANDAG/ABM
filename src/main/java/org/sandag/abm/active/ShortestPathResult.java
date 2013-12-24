package org.sandag.abm.active;

public class ShortestPathResult<N extends Node> {
	private final NodePair<N> od;
	private final Path<N> path;
	private final double cost;
	
	public ShortestPathResult(NodePair<N> od, Path<N> path, double cost) {
		this.od = od;
		this.path = path;
		this.cost = cost;
	}
	
	public NodePair<N> getOriginDestination() {
		return od;
	}
	
	public Path<N> getPath() {
		return path;
	}
	
	public double getCost() {
		return cost;
	}

}
