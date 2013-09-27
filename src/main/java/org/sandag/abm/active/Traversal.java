package org.sandag.abm.active;

import java.util.Objects;

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
    
    public Traversal(Node node1, Node node2)
    {
        this.fromEdge = null;
        this.toEdge = new Edge(node1,node2);
    }
    
    public Traversal(int id1, int id2)
    {
        this.fromEdge = null;
        this.toEdge = new Edge(id1, id2);
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
    
    public boolean equals(Object o) {
    	if ((o == null) || !(o instanceof Traversal))
    		return false;
    	Traversal t = (Traversal) o;
    	if ((fromEdge != null) && (!fromEdge.equals(t.fromEdge)))
    		return false;
    	return toEdge.equals(t.toEdge);
    }
    
    public int hashCode() {
    	return Objects.hash(fromEdge,toEdge);
    }
}
