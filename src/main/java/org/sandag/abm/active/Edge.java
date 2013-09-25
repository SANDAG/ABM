package org.sandag.abm.active;

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
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fromNode == null) ? 0 : fromNode.hashCode());
        result = prime * result + ((toNode == null) ? 0 : toNode.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Edge other = (Edge) obj;
        if (fromNode == null)
        {
            if (other.fromNode != null) return false;
        } else if (!fromNode.equals(other.fromNode)) return false;
        if (toNode == null)
        {
            if (other.toNode != null) return false;
        } else if (!toNode.equals(other.toNode)) return false;
        return true;
    }
    
    
}
