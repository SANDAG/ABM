package org.sandag.abm.active;

public abstract class AbstractShortestPathResultsContainer<N extends Node> implements ShortestPathResultsContainer<N> {

	@Override
	public void addResult(NodePair<N> od, Path<N> path, double cost) {
		addResult(new ShortestPathResult<N>(od,path,cost));
	}

	@Override
	public void addAll(ShortestPathResults<N> results) {
		for (ShortestPathResult<N> result : results.getResults())
			addResult(result);
	}

}
