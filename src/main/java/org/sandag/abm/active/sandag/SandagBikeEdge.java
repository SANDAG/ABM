package org.sandag.abm.active.sandag;
import org.sandag.abm.active.*;

public class SandagBikeEdge extends SimpleEdge
{
    public byte bikeClass, lanes, functionalClass;
    public boolean centroidConnector, autosPermitted;
    public float distance;
    public short gain;
}
