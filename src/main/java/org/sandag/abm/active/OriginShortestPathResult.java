package org.sandag.abm.active;

class OriginShortestPathResult {
	private final Path[] paths;
	private final double[] costs;
	
	public OriginShortestPathResult(Path[] paths, double[] costs) {
		this.paths = paths;
		this.costs = costs;
	}
	
	public Path[] getPaths() {
		return paths;
	}
	
	public double[] getCosts() {
		return costs;
	}
}
