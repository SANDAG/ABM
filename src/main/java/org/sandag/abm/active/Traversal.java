package org.sandag.abm.active;

public interface Traversal
{
    int getStartId();
    int getThruId();
    int getEndId();
    void setStartId(int id);
    void setThruId(int id);
    void setEndId(int id);
}
