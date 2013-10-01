package org.sandag.abm.active;

public interface PathElementEvaluator<E extends Edge<?>,T extends Traversal<E>> {
	double evaluate(T traversal);
	double evaluate(E edge);
}
