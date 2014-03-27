package org.sandag.abm.active;

public interface EdgeEvaluator<E extends Edge<?>>
{
    double evaluate(E edge);
}
