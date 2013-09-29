package org.sandag.abm.active;

import java.util.*;
import org.apache.log4j.Logger;

public abstract class NetworkFactory<T extends Node, U extends Edge, V extends Traversal>
{
    protected Logger logger = Logger.getLogger(NetworkFactory.class);
    protected Network<T,U,V> network;
    protected Class<T> nodeClass;
    protected Class<U> edgeClass;
    protected Class<V> traversalClass;
    
    protected Set<Integer> defaultNodeIds;
    protected Set<int[]> defaultTraversalIds;
    
    protected NetworkFactory(Class<T> nodeClass, Class<U> edgeClass, Class<V> traversalClass)
    {
        network = new Network<T,U,V>();
        this.nodeClass = nodeClass;
        this.edgeClass = edgeClass;
        this.traversalClass = traversalClass;
        defaultNodeIds = new HashSet<Integer>();
        defaultTraversalIds = new HashSet<int[]>();
    }

    public Network<T,U,V> create()
    {
        readNodes();
        readEdges();
        handleDefaultNodes();
        calculateDerivedNodeAttributes();
        calculateDerivedEdgeAttributes();
        calculateDerivedTraversalAttributes();
        return network;
    }
    
    public T createDefaultNode(int id)
    {
        T newNode = null;
        
        try {
            newNode = nodeClass.newInstance();
            newNode.setId(id);
        } catch (InstantiationException e) {
            logger.error( "Exception caught instantiating default Node.", e);
            throw new RuntimeException();
        } catch (IllegalAccessException e) {
            logger.error( "Exception caught instantiating default Node.", e);
            throw new RuntimeException();
        }
        
        return newNode;
    }
    
    public U createDefaultEdge(int fromId, int toId)
    {
        U newEdge = null;
        
        try {
            newEdge = edgeClass.newInstance();
            newEdge.setFromId(fromId);
            newEdge.setToId(toId);
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error( "Exception caught instantiating default Edge.", e);
            throw new RuntimeException();
        }
        
        return newEdge;
    }
    
    public V createDefaultTraversal(int startId, int thruId, int endId)
    {
        V newTraversal = null;
           
        try {
            newTraversal = traversalClass.newInstance();
            newTraversal.setStartId(startId);
            newTraversal.setThruId(thruId);
            newTraversal.setEndId(endId);
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error( "Exception caught instantiating default Traversal.", e);
            throw new RuntimeException();
        }
           
        return newTraversal;
    }
    
    
    public void addEdgeWithDefaultNodesAndTraversals(U edge)
    {
        network.addEdge(edge);
        
        int fromId = edge.getFromId();
        int toId = edge.getToId();
        
        if ( ! network.containsNodeId(fromId) ) {
            defaultNodeIds.add(fromId);
            network.addNode(createDefaultNode(fromId));
        }
        if ( ! network.containsNodeId(toId) ) {
            defaultNodeIds.add(toId);
            network.addNode(createDefaultNode(toId));
        }
    
        for (T successor : network.getSuccessors(toId)) {
            int[] traversalIndexKey = new int[] {fromId, toId, successor.getId()};
            if ( ! network.containsTraversalIds(traversalIndexKey) ) {
                network.addTraversal(createDefaultTraversal(fromId, toId, successor.getId()));
                defaultTraversalIds.add(traversalIndexKey);
            }
        }
        
        for (T predecessor : network.getPredecessors(fromId)) {
            int[] traversalIndexKey = new int[] {predecessor.getId(), fromId, toId};
            if ( ! network.containsTraversalIds(traversalIndexKey) ) {
                network.addTraversal(createDefaultTraversal(predecessor.getId(), fromId, toId));
                defaultTraversalIds.add(traversalIndexKey);
            }
        }
    }
    
    protected Iterator<Integer> defaultNodeIndexIterator()
    {
        return defaultNodeIds.iterator();
    }
    
    protected abstract void readNodes();
    protected abstract void readEdges();
    protected abstract void handleDefaultNodes();
    protected abstract void calculateDerivedNodeAttributes();
    protected abstract void calculateDerivedEdgeAttributes();
    protected abstract void calculateDerivedTraversalAttributes();
}
