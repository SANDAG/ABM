package org.sandag.abm.active;

import java.util.Objects;

public class SimpleNode implements Node
{
	private final int id;
	
	public SimpleNode(int id)
	{
		this.id = id;
	}
	
	@Override
	public int getId()
	{
	    return id;
	}
    
    @Override
    public int compareTo(Node node) {
    	return Integer.compare(id,node.getId());
    }
    
    @Override
    public int hashCode() 
    {
    	return Objects.hash(id);
    }
    
    @Override
    public boolean equals(Object o) 
    {
    	if ((o == null) || !(o instanceof Node))
    		return false;
    	return id == ((Node) o).getId();
    }
    
    public String toString()
    {
    	return "<node: " + id + ">";
    }
   
}
