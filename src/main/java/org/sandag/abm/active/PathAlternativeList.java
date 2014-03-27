package org.sandag.abm.active;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PathAlternativeList<N extends Node, E extends Edge<N>>
{
    private List<Path<N>> paths;
    private NodePair<N> odPair;
    private List<Double> sizeMeasures;
    private PathSizeCalculator sizeCalculator;
    private double sizeMeasureTotal;
    private boolean sizeMeasuresUpdated;
    Network<N,E,?> network;
    EdgeEvaluator<E> lengthEvaluator;
    
    public PathAlternativeList(NodePair<N> odPair, Network<N,E,?> network, EdgeEvaluator<E> lengthEvaluator)
    {
        paths = new ArrayList<Path<N>>();
        sizeMeasures = new ArrayList<Double>();
        this.odPair = odPair;
        sizeMeasuresUpdated = true;
        this.network = network;
        this.lengthEvaluator = lengthEvaluator;
        this.sizeCalculator = new PathSizeCalculator(this);
        this.sizeMeasureTotal = 0.0;
    }
    
    public Network<N,E,?> getNetwork() {
        return network;
    }
    
    public void add(Path<N> path)
    {
        if ( ! path.getNode(0).equals(odPair.getFromNode()) || ! path.getNode(path.getLength() - 1).equals(odPair.getToNode()) ) {
            throw new IllegalStateException("OD pair of path does not match that of path alternative list");
        }
        for (Path<N> otherPath : paths) {
            if ( path.equals(otherPath) ) { return; }
        }
        paths.add(path);
        sizeMeasures.add(0.0);
        if ( sizeCalculator == null ) {
            sizeMeasuresUpdated = false;
        } else {
            sizeCalculator.update();
        }
    }
    
    public List<Double> getSizeMeasures()
    {
        return sizeMeasures;
    }
    
    private void setSizeMeasure(int index, double value)
    {
        sizeMeasureTotal += value - sizeMeasures.get(index);
        sizeMeasures.set(index, value);
    }
    
    public double getSizeMeasureTotal()
    {
        return sizeMeasureTotal;
    }

    public int getCount()
    {
        return paths.size();
    }

    public Path<N> get(int index)
    {
        return paths.get(index);
    }
    
    public boolean areSizeMeasuresUpdated()
    {
        return sizeMeasuresUpdated;
    }
    
    public void clearPathSizeCalculator()
    {
        sizeCalculator = null;
    }
    
    public void restartPathSizeCalculator()
    {
        if ( sizeCalculator == null ) {
            sizeCalculator = new PathSizeCalculator(this);
            sizeMeasuresUpdated = true;
        }
    }
    
    private class PathSizeCalculator
    {
            Map<E,List<Integer>> incidenceMap;
            List<Double> lengths;
            PathAlternativeList<N,E> alternatives;
            int nUsingEdge;
            double edgeLength;
            
            private PathSizeCalculator(PathAlternativeList<N,E> alternatives)
            {
                incidenceMap = new HashMap<E,List<Integer>>();
                lengths = new ArrayList<Double>();
                this.alternatives = alternatives;
                if ( alternatives.getCount() > 0 ) {
                    for (int i=0; i < alternatives.getCount(); i++) {
                        alternatives.setSizeMeasure(i, 0.0);
                        update();
                    }
                }
            }
            
            private void update()
            {
                lengths.add(0.0);
                N previous = null;
                E edge;
                int index = lengths.size() - 1;
                double decrement;
                for (N node : alternatives.get(index) ) {
                    if ( previous != null ) {
                        edge = network.getEdge(previous,node);
                        if ( ! incidenceMap.containsKey(edge) ) {
                            incidenceMap.put(edge, new ArrayList<Integer>());
                        }
                        incidenceMap.get(edge).add(index);
                        edgeLength = lengthEvaluator.evaluate(edge);
                        lengths.set(index, lengths.get(index) + edgeLength);
                        nUsingEdge = incidenceMap.get(edge).size();
                        alternatives.setSizeMeasure(index, alternatives.getSizeMeasures().get(index) + edgeLength / nUsingEdge );
                        for (Integer i : incidenceMap.get(edge).subList(0,nUsingEdge-1) ) {
                            decrement = edgeLength / lengths.get(i) / ( nUsingEdge ) / ( nUsingEdge - 1 );
                            alternatives.setSizeMeasure(i, alternatives.getSizeMeasures().get(i) - decrement);
                        }
                    }
                    previous = node;
                }
                alternatives.setSizeMeasure(index, alternatives.getSizeMeasures().get(index) / lengths.get(index));
            }
                
    }
    
    public NodePair<N> getODPair() {
        return odPair;
    }

    public EdgeEvaluator<E> getLengthEvaluator() {
        return lengthEvaluator;
    }
}
