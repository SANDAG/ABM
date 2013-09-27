package org.sandag.abm.active;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class ArrayShortestPathResults implements ShortestPathResults {
	private final Path[][] paths;
	private final double[][] costs;
	protected final List<Node> orderedOrigins;
	protected final List<Node> orderedDestinations;

	public ArrayShortestPathResults(Path[][] paths, double[][] costs, List<Node> orderedOrigins, List<Node> orderedDestinations) {
		this.paths = paths;
		this.costs = costs;
		this.orderedOrigins = new LinkedList<>(orderedOrigins);
		this.orderedDestinations = new LinkedList<>(orderedDestinations);
	}
	
	public ArrayShortestPathResults(Path[][] paths, double[][] costs, List<Node> orderedNodes) {
		this(paths,costs,orderedNodes,orderedNodes);
	}
	
	public ShortestPathResult getShortestPathResult(NodePair od) {
		int originIndex = getOriginIndex(od.getOriginNode());
		int destinationIndex = getDestinationIndex(od.getDestinationNode());
		return new ShortestPathResult(od,paths[originIndex][destinationIndex], costs[originIndex][destinationIndex]);
	}
	
	protected int getOriginIndex(Node origin) {
		return origin.getId();
	}
	
	protected int getDestinationIndex(Node destination) {
		return destination.getId();
	}
	
	public Iterator<NodePair> iterator() {
		return new Iterator<NodePair>() {
			Iterator<Node> originIterator = orderedOrigins.iterator();
			Node origin = originIterator.hasNext() ? originIterator.next() : null;
			Iterator<Node> destinationIterator = orderedDestinations.iterator();

			public boolean hasNext() {
				return originIterator.hasNext() || destinationIterator.hasNext();
			}

			public NodePair next() {
				if (!destinationIterator.hasNext()) {
					if (!originIterator.hasNext())
						throw new NoSuchElementException();
					origin = originIterator.next();
					destinationIterator = orderedDestinations.iterator();
				}
				if ((origin == null) || (!destinationIterator.hasNext()))
					throw new NoSuchElementException();
				
				return new NodePair(origin,destinationIterator.next());
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

}
