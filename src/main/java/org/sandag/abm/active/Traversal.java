package org.sandag.abm.active;

public class Traversal implements Comparable<Traversal>
{
private Edge fromEdge, toEdge;
    
    public Traversal(Edge fromEdge, Edge toEdge)
    {
        this.fromEdge = fromEdge;
        this.toEdge = toEdge;
    }
    
    public Traversal(Node node1, Node node2, Node node3)
    {
        this.fromEdge = new Edge(node1, node2);
        this.toEdge = new Edge(node2, node3);
    }
    
    public Traversal(int id1, int id2, int id3)
    {
        this.fromEdge = new Edge(id1, id2);
        this.toEdge = new Edge(id2, id3);
    }
    
    public Edge getFromEdge()
    {
        return fromEdge;
    }
    
    public Edge getToEdge()
    {
        return toEdge;
    }

    @Override
    public int compareTo(Traversal o)
    {
        int fromResult = this.fromEdge.compareTo(o.fromEdge);
        int toResult = this.toEdge.compareTo(o.toEdge);
        return fromResult + ( (fromResult == 0) ? 1 : 0 ) * toResult;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fromEdge == null) ? 0 : fromEdge.hashCode());
        result = prime * result + ((toEdge == null) ? 0 : toEdge.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Traversal other = (Traversal) obj;
        if (fromEdge == null)
        {
            if (other.fromEdge != null) return false;
        } else if (!fromEdge.equals(other.fromEdge)) return false;
        if (toEdge == null)
        {
            if (other.toEdge != null) return false;
        } else if (!toEdge.equals(other.toEdge)) return false;
        return true;
    }
    
}
