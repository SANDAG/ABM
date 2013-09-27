package org.sandag.abm.active;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

public class Path implements Iterable<Node> {
	private final Path predecessorPath;
	private final Node next;
	private final int length;
	
	public Path(Path predecessorPath, Node next) {
		this.predecessorPath = predecessorPath;
		this.next = next;
		this.length = predecessorPath == null ? 1 : predecessorPath.length + 1;
	}
	
	public Path(Node first) {
		this(null,first);
	}
	
	public Node getNode(int index) {
		if (index < 0 || index >= length)
			throw new IllegalArgumentException("Invalid index for path of length " + length);
		return getNodeNoChecks(index+1);
	}
	
	private Node getNodeNoChecks(int index) { //index here is 1-based, not zero based!
		return index == length ? next : predecessorPath.getNodeNoChecks(index);
	}
	
	public Path extendPath(Node next) {
		return new Path(this,next);
	}
	
	public String getPathString() {
		StringBuilder sb = new StringBuilder();
		for (Node n : this)
			sb.append(n.getId()).append(n == next ? "" : " ");
		return sb.toString();
	}
	
	public Iterator<Node> iterator() {
		return new Iterator<Node>() {
			private int point = 0;

			@Override
			public boolean hasNext() {
				return point < length; 
			}

			@Override
			public Node next() {
				if (point < length)
					return getNodeNoChecks(++point);
				else
					throw new NoSuchElementException();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}
