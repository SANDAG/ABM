package org.sandag.abm.active;

import java.util.Map;

public interface ShortestPathResults extends Iterable<NodePair> {
	ShortestPathResult getShortestPathResult(NodePair od);
}
