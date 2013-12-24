package org.sandag.abm.active;

import java.util.Objects;

public class SimpleEdge<N extends Node> implements Edge<N>
{
    private final N fromNode;
    private final N toNode;
    
    public SimpleEdge(N fromNode, N toNode) {
    	this.fromNode = fromNode;
    	this.toNode = toNode;
    }

    @Override
    public N getFromNode()
    {
        return fromNode;
    }

    @Override
    public N getToNode()
    {
        return toNode;
    }
    
    @Override
    public int compareTo(Edge<N> o)
    {
        int fromResult = this.fromNode.compareTo(o.getFromNode());
        int toResult = this.toNode.compareTo(o.getToNode());
        return fromResult + ( (fromResult == 0) ? 1 : 0 ) * toResult;
    }
    
    @Override
    public int hashCode() 
    {
    	return Objects.hash(fromNode,toNode);
    }
    
    @Override
    public boolean equals(Object o) 
    {
    	if ((o == null) || !(o instanceof Edge))
    		return false;
    	Edge<?> other = (Edge<?>) o;
    	return fromNode.equals(other.getFromNode()) && toNode.equals(other.getToNode());
    }
}
