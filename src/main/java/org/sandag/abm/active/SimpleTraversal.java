package org.sandag.abm.active;

public abstract class SimpleTraversal implements Traversal
{
    private int startId,thruId,endId;

    public int getStartId()
    {
        return startId;
    }

    public void setStartId(int startId)
    {
        this.startId = startId;
    }

    public int getThruId()
    {
        return thruId;
    }

    public void setThruId(int thruId)
    {
        this.thruId = thruId;
    }

    public int getEndId()
    {
        return endId;
    }

    public void setEndId(int endId)
    {
        this.endId = endId;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + endId;
        result = prime * result + startId;
        result = prime * result + thruId;
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        SimpleTraversal other = (SimpleTraversal) obj;
        if (endId != other.endId) return false;
        if (startId != other.startId) return false;
        if (thruId != other.thruId) return false;
        return true;
    }
    
}
