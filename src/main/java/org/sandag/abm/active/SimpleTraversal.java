package org.sandag.abm.active;

import java.util.Objects;

public class SimpleTraversal<E extends Edge<?>>
        implements Traversal<E>
{
    private final E fromEdge;
    private final E toEdge;

    public SimpleTraversal(E fromEdge, E toEdge)
    {
        this.fromEdge = fromEdge;
        this.toEdge = toEdge;
    }

    @Override
    public E getFromEdge()
    {
        return fromEdge;
    }

    @Override
    public E getToEdge()
    {
        return toEdge;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(fromEdge, toEdge);
    }

    @Override
    public boolean equals(Object obj)
    {
        if ((obj == null) || (!(obj instanceof Traversal))) return false;
        Traversal<?> traversal = (Traversal<?>) obj;
        if (fromEdge == null) return (fromEdge == traversal.getFromEdge())
                && (toEdge.equals(traversal.getToEdge()));
        else return (fromEdge.equals(traversal.getFromEdge()))
                && (toEdge.equals(traversal.getToEdge()));
    }

}
