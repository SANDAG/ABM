package org.sandag.abm.active;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class Path<N extends Node> implements Iterable<N> {
	private final Path<N> predecessorPath;
	private final N next;
	private final int length;
	
	public Path(Path<N> predecessorPath, N next) {
		this.predecessorPath = predecessorPath;
		this.next = next;
		this.length = predecessorPath == null ? 1 : predecessorPath.length + 1;
	}
	
	public Path(N first) {
		this(null,first);
	}
	
	public int getLength()
	{
	    return length;
	}
	
	public N getNode(int index) {
		if (index < 0 || index >= length)
			throw new IllegalArgumentException("Invalid index " + index + " for path of length " + length);
		return getNodeNoChecks(index+1);
	}
	
	private N getNodeNoChecks(int index) { //index here is 1-based, not zero based!
		return index == length ? next : predecessorPath.getNodeNoChecks(index);
	}
	
	public Path<N> extendPath(N next) {
		return new Path<N>(this,next);
	}
	
	public String getPathString() {
		StringBuilder sb = new StringBuilder();
		for (N n : this)
			sb.append(n.getId()).append(n == next ? "" : " ");
		return sb.toString();
	}
	
	public Iterator<N> iterator() {
		return new Iterator<N>() {
			private int point = 0;

			@Override
			public boolean hasNext() {
				return point < length; 
			}

			@Override
			public N next() {
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
