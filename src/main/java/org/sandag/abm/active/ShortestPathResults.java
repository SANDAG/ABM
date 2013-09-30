package org.sandag.abm.active;

import java.util.Collection;
import java.util.Map;

public interface ShortestPathResults extends Iterable<NodePair> {
	int size();
	ShortestPathResult getShortestPathResult(NodePair od);
	Collection<ShortestPathResult> getResults();
}
