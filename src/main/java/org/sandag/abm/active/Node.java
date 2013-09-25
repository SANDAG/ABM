package org.sandag.abm.active;

public class Node implements Comparable<Node>
{

	private Integer id;
	
	public Node(int id)
	{
	    this.id = id;
	}
	
	public int getId()
	{
	    return id;
	}

    @Override
    public int compareTo(Node o)
    {
        return this.id.compareTo(o.id);
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Node other = (Node) obj;
        if (id == null)
        {
            if (other.id != null) return false;
        } else if (!id.equals(other.id)) return false;
        return true;
    }
    
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

}
