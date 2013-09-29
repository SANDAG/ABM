package org.sandag.abm.active;

public interface Edge
{
    int getFromId();
    int getToId();
    void setFromId(int id);
    void setToId(int id);
}
