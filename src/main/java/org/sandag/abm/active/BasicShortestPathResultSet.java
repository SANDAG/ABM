package org.sandag.abm.active;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class BasicShortestPathResultSet<N extends Node> extends AbstractShortestPathResultSet<N> {
	private final Map<NodePair<N>,ShortestPathResult<N>> results;
	
	public BasicShortestPathResultSet() {
		results = new LinkedHashMap<>(); //iteration order may not matter, but just in case, this is cheap
	}

	@Override
	public void addResult(ShortestPathResult<N> spResult) {
		ShortestPathResult<N> spr = results.put(spResult.getOriginDestination(),spResult); 
		if (spr != null)
			throw new IllegalArgumentException("Repeated shortest path results for node pair: (" + 
					spResult.getOriginDestination().getFromNode().getId() + "," + spResult.getOriginDestination().getToNode().getId() + ")");
	}

	@Override
	public void addResult(NodePair<N> od, Path<N> path, double cost) {
		addResult(new ShortestPathResult<N>(od,path,cost));
	}

	@Override
	public Iterator<NodePair<N>> iterator() {
		return results.keySet().iterator();
	}

	@Override
	public ShortestPathResult<N> getShortestPathResult(NodePair<N> od) {
		return results.get(od);
	}
	
	@Override
	public int size() {
		return results.size();
	}

	@Override
	public Collection<ShortestPathResult<N>> getResults() {
		return results.values();
	}

}
