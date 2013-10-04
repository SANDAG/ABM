package org.sandag.abm.active;

public interface PathElementEvaluator<E extends Edge<?>,T extends Traversal<E>> extends EdgeEvaluator<E>, TraversalEvaluator<T> {}
