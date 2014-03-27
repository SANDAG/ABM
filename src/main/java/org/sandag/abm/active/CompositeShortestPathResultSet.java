package org.sandag.abm.active;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CompositeShortestPathResultSet<N extends Node> implements ShortestPathResultSet<N> {
	private final Map<NodePair<N>,ShortestPathResultSet<N>> spResultsLookup;
	
	public CompositeShortestPathResultSet() {
		spResultsLookup = new HashMap<>();
	}
	
	public void addShortestPathResults(ShortestPathResultSet<N> spResults) {
		for (NodePair<N> nodePair : spResults)
			if (spResultsLookup.put(nodePair,spResults) != null)
				throw new IllegalArgumentException("Repeated shortest path results for node pair: (" + 
			                                       nodePair.getFromNode().getId() + "," + nodePair.getToNode().getId() + ")");
	}

	@Override
	public Iterator<NodePair<N>> iterator() {
		return spResultsLookup.keySet().iterator();
	}

	@Override
	public ShortestPathResult<N> getShortestPathResult(NodePair<N> od) {
		return spResultsLookup.get(od).getShortestPathResult(od);
	}
	
	@Override
	public int size() {
		return spResultsLookup.size();
	}

	@Override
	public Collection<ShortestPathResult<N>> getResults() {
		List<ShortestPathResult<N>> results = new LinkedList<>();
		for (ShortestPathResultSet<N> spr : spResultsLookup.values())
			results.addAll(spr.getResults());
		return results;
	}

}
