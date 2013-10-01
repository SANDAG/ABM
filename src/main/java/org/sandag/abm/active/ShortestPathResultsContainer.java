package org.sandag.abm.active;

public interface ShortestPathResultsContainer<N extends Node> extends ShortestPathResults<N> {
	void addResult(ShortestPathResult<N> spResult);
	void addResult(NodePair<N> od, Path<N> path, double cost);
	void addAll(ShortestPathResults<N> results);
}
