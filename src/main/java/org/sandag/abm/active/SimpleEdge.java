package org.sandag.abm.active;

public abstract class SimpleEdge implements Edge
{
    private int fromId, toId;

    public int getFromId()
    {
        return fromId;
    }

    public void setFromId(int fromId)
    {
        this.fromId = fromId;
    }

    public int getToId()
    {
        return toId;
    }

    public void setToId(int toId)
    {
        this.toId = toId;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + fromId;
        result = prime * result + toId;
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        SimpleEdge other = (SimpleEdge) obj;
        if (fromId != other.fromId) return false;
        if (toId != other.toId) return false;
        return true;
    }
}
