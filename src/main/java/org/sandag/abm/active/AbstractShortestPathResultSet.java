package org.sandag.abm.active;

public abstract class AbstractShortestPathResultSet<N extends Node> implements ModifiableShortestPathResultSet<N> {

	@Override
	public void addResult(NodePair<N> od, Path<N> path, double cost) {
		addResult(new ShortestPathResult<N>(od,path,cost));
	}

	@Override
	public void addAll(ShortestPathResultSet<N> results) {
		for (ShortestPathResult<N> result : results.getResults())
			addResult(result);
	}

}
