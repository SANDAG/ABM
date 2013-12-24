package org.sandag.abm.active;

public interface Edge<N extends Node> extends Comparable<Edge<N>>
{
    N getFromNode();
    N getToNode();
}
