package org.sandag.abm.active;

public interface Traversal<E extends Edge<?>>
{
    E getFromEdge();
    E getToEdge();
}
