package org.sandag.abm.active;

import java.util.Map;

public interface IntrazonalCalculation<N extends Node> {
	double getIntrazonalValue(N originaNode, Map<N, double[]> logsums, int logsumIndex);
}
