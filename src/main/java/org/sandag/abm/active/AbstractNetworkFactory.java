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
	
	@Override
    public Network<N,E,T> createNetwork()
    {
        Network<N,E,T> network = new SimpleNetwork<>(getNodes(),getEdges(),getTraversals());
        calculateDerivedNodeAttributes(network);
        calculateDerivedEdgeAttributes(network);
        calculateDerivedTraversalAttributes(network);
        return network;
    }


	@Override
    protected Collection<T> getTraversals() 
    {
    	Collection<E> edges = getEdges();
    	Map<N,List<E>> predecessors = new HashMap<>();
    	for (N node : getNodes())
			predecessors.put(node,new LinkedList<E>());
    	for (E edge : edges) 
    		predecessors.get(edge.getToNode()).add(edge);
    	Set<T> traversals = new LinkedSetList<>();
		for (E toEdge : getEdges()) {
			for (E fromEdge : predecessors.get(toEdge.getFromNode()))
				if (!isReversal(fromEdge,toEdge))
					traversals.add(getTraversal(fromEdge,toEdge));
		}
		return Collections.unmodifiableCollection(traversals);
    }
	
	private boolean isReversal(E edge1, E edge2) {
		return (edge1.getToNode().equals(edge2.getFromNode())) &&
			   (edge1.getFromNode().equals(edge2.getToNode()));
	}
    
    abstract protected T getTraversal(E fromEdge, E toEdge);
}
