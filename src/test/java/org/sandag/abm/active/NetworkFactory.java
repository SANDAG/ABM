package org.sandag.abm.active;

import java.util.Collection;

public abstract class NetworkFactory<N extends Node, E extends Edge<N>, T extends Traversal<E>>
{

    public Network<N, E, T> createNetwork()
    {
        Network<N, E, T> network = new SimpleNetwork<>(getNodes(), getEdges(), getTraversals());
        calculateDerivedNodeAttributes(network);
        calculateDerivedEdgeAttributes(network);
        calculateDerivedTraversalAttributes(network);
        return network;
    }

    protected abstract Collection<N> getNodes();

    protected abstract Collection<E> getEdges();

    protected abstract Collection<T> getTraversals();

    protected void calculateDerivedNodeAttributes(Network<N, E, T> network)
    {
    }

    protected void calculateDerivedEdgeAttributes(Network<N, E, T> network)
    {
    }

    protected void calculateDerivedTraversalAttributes(Network<N, E, T> network)
    {
    }

}
