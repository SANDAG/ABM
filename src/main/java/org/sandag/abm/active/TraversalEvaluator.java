package org.sandag.abm.active;

public interface TraversalEvaluator<T extends Traversal<?>>
{
    double evaluate(T traversal);
}
