package org.sandag.abm.active;

public class ShortestPathResult {
	private final NodePair od;
	private final Path path;
	private final double cost;
	
	public ShortestPathResult(NodePair od, Path path, double cost) {
		this.od = od;
		this.path = path;
		this.cost = cost;
	}
	
	public NodePair getOriginDestination() {
		return od;
	}
	
	public Path getPath() {
		return path;
	}
	
	public double getCost() {
		return cost;
	}

}
