package org.sandag.abm.active;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.pbworld.sawdust.util.collections.LinkedSetList;

public abstract class AbstractNetworkFactory<N extends Node,E extends Edge<N>,T extends Traversal<E>> extends NetworkFactory<N,E,T> 
{


    protected Collection<T> getTraversals() {
    	Collection<E> edges = getEdges();
    	Map<N,List<E>> predecessors = new HashMap<>();
    	for (E edge : edges) {
    		N toNode = edge.getToNode();
    		if (!predecessors.containsKey(toNode))
    			predecessors.put(toNode,new LinkedList<E>());
    		predecessors.get(toNode).add(edge);
    	}
    	Set<T> traversals = new LinkedSetList<>();
		for (E toEdge : getEdges()) {
			traversals.add(getTraversal(toEdge));
			for (E fromEdge : predecessors.get(toEdge.getFromNode()))
				traversals.add(getTraversal(fromEdge,toEdge));
		}
		return Collections.unmodifiableCollection(traversals);
    }
    
    abstract protected T getTraversal(E edge);
    abstract protected T getTraversal(E fromEdge, E toEdge);
}
