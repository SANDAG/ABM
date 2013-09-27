package org.sandag.abm.active;

import java.util.Objects;

public class Edge implements Comparable<Edge>
{
    private Node fromNode, toNode;
    
    public Edge(Node fromNode, Node toNode)
    {
        this.fromNode = fromNode;
        this.toNode = toNode;
    }
    
    public Edge(int fromId, int toId)
    {
        this(new Node(fromId),new Node(toId));
    }
    
    public Node getFromNode()
    {
        return fromNode;
    }
    
    public Node getToNode()
    {
        return toNode;
    }

    @Override
    public int compareTo(Edge o)
    {
        int fromResult = this.fromNode.compareTo(o.fromNode);
        int toResult = this.toNode.compareTo(o.toNode);
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
    	Edge other = (Edge) o;
    	return fromNode.equals(other.fromNode) && toNode.equals(other.toNode);
    }
}
