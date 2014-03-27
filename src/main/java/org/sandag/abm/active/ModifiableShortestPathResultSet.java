package org.sandag.abm.active;

public interface ModifiableShortestPathResultSet<N extends Node> extends ShortestPathResultSet<N> {
	void addResult(ShortestPathResult<N> spResult);
	void addResult(NodePair<N> od, Path<N> path, double cost);
	void addAll(ShortestPathResultSet<N> results);
}
