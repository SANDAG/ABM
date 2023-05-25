package org.sandag.abm.active;

import java.util.Map;

/**
 * The {@code IntrazonalCalculation} class provides a framework for calculation
 * intrazonals.
 * 
 * @param <N>
 *            The type of the zone nodes.
 */
public interface IntrazonalCalculation<N extends Node>
{
    /**
     * Get the intrazonal value given the origin node and the logsum values with
     * that origin node. The logsum values may be stratified across markets, so
     * the index of the market of interest is also provided.
     * 
     * @param originNode
     *            The origin node.
     * 
     * @param logsums
     *            The logsums with {@code originNode} as their origin. The
     *            logsums are stored as a map with the destination node as the
     *            key and an array of logsums as the value. The logsum array has
     *            a different logsum for each market.
     * 
     * @param logsumIndex
     *            The index for the logsum of interest in the logsum arrays
     *            provided in {@code logsums}.
     * 
     * @return the intrazonal logsum for {@code originNode}.
     */
    double getIntrazonalValue(N originNode, Map<N, double[]> logsums, int logsumIndex);
}
