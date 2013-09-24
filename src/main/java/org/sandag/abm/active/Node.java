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

}
