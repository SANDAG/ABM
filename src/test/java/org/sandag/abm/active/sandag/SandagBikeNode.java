package org.sandag.abm.active.sandag;

import org.sandag.abm.active.SimpleNode;

public class SandagBikeNode
        extends SimpleNode
{
    public volatile float x, y;
    public volatile short mgra, taz, tap;
    public volatile boolean signalized, centroid;

    public SandagBikeNode(int id)
    {
        super(id);
    }
}
