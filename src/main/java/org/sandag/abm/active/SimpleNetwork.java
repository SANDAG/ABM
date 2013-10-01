package org.sandag.abm.active;
import java.util.*;

public class SimpleNetwork<N extends Node,E extends Edge<N>,T extends Traversal<E>> implements Network<N,E,T>
{
    private final Map<Integer,N> nodes;
    private final Map<NodePair<N>,E> edges;
    private final Map<EdgePair,T> traversals;
    
    private final Map<N,Collection<N>> successors;
    private final Map<N,Collection<N>> predecessors;
    
    public SimpleNetwork(Collection<N> nodes, Collection<E> edges, Collection<T> traversals)
    {
    	this.nodes = new LinkedHashMap<>();
    	this.edges = new LinkedHashMap<>();
    	this.traversals = new LinkedHashMap<>();
    	
    	for (N node : nodes)
    		this.nodes.put(node.getId(),node);
    	for (E edge : edges)
    		this.edges.put(new NodePair<N>(edge.getFromNode(),edge.getToNode()),edge);
    	for (T traversal : traversals)
    		this.traversals.put(new EdgePair(traversal.getFromEdge(),traversal.getToEdge()),traversal);
    	
    	successors = new LinkedHashMap<>();
    	predecessors = new LinkedHashMap<>();

    	for (N node : this.nodes.values()) {
    		successors.put(node,new LinkedList<N>());
    		predecessors.put(node, new LinkedList<N>());
    	}
    	for (NodePair<N> nodePair : this.edges.keySet()) {
    		N from = nodePair.getFromNode();
    		N to = nodePair.getToNode();
    		successors.get(from).add(to);
    		predecessors.get(to).add(from);
    	} 
    }
    
    @Override
    public N getNode(int nodeId)
    {
        return nodes.get(nodeId);
    }

    @Override
    public E getEdge(N fromNode, N toNode)
    {
        return getEdge(new NodePair<N>(fromNode,toNode));
    }

    @Override
    public E getEdge(NodePair<N> nodes)
    {
    	return edges.get(nodes);
    }

    @Override
    public T getTraversal(E fromEdge, E toEdge)
    {
        return traversals.get(new EdgePair(fromEdge,toEdge));
    }

    @Override
    public Collection<N> getSuccessors(Node node)
    {
        return Collections.unmodifiableCollection(successors.get(node));
    }

    @Override
    public Collection<N> getPredecessors(Node node)
    {
        return Collections.unmodifiableCollection(predecessors.get(node));
    }

    @Override
    public Iterator<N> nodeIterator()
    {
        return nodes.values().iterator();
    }

    @Override
    public Iterator<E> edgeIterator()
    {
        return edges.values().iterator();
    }

    @Override
    public Iterator<T> traversalIterator()
    {
        return traversals.values().iterator();
    }

    @Override
    public boolean containsNodeId(int id) 
    {
        return nodes.containsKey(id);
    }

    @Override
    public boolean containsNode(Node node) 
    {
        return nodes.containsValue(node);
    }

    @Override
    public boolean containsEdge(N fromNode, N toNode) 
    {
        return edges.containsKey(new NodePair<N>(fromNode,toNode));
    }

    @Override
    public boolean containsTraversal(E fromEdge, E toEdge) 
    {
        return traversals.containsKey(new EdgePair(fromEdge,toEdge));
    }

    private class EdgePair extends SimpleTraversal<E>
    {
		public EdgePair(E fromEdge, E toEdge) {
			super(fromEdge, toEdge);
		}
    }
        
}
