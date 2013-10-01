package org.sandag.abm.active;

import java.util.Collection;
import java.util.Iterator;

public interface Network<N extends Node, E extends Edge<N>, T extends Traversal<E>> 
{
	N getNode(int nodeId);
    
    E getEdge(N fromNode, N toNode);
    
    E getEdge(NodePair<N> nodes);
    
    T getTraversal(E fromEdge, E toEdge);
    
    T getNullTraversal(E edge);
    
    Collection<N> getSuccessors(N node);
    
    Collection<N> getPredecessors(N node);
    
    Iterator<N> nodeIterator();
    
    Iterator<E> edgeIterator();
    
    Iterator<T> traversalIterator();

    boolean containsNodeId(int id) ;
    
    boolean containsNode(N node) ;

    boolean containsEdge(N fromNode, N toNode);

    boolean containsTraversal(E fromEdge, E toEdge);

//    public void addNode(T node)
//    {
//        if ( nodeIndex.containsKey(node.getId()) ) {
//            throw new RuntimeException("Network already contains Node with id " + node.getId());
//        }
//        nodeIndex.put(node.getId(), nodes.size());
//        nodes.add(node);
//        if (! successorIndex.containsKey(node.getId()) ) { successorIndex.put(node.getId(), new ArrayList<Integer>()); }
//        if (! predecessorIndex.containsKey(node.getId()) ) { predecessorIndex.put(node.getId(), new ArrayList<Integer>()); }
//    }
//    
//    public void addEdge(U edge)
//    {
//        int fromId = edge.getFromNode();
//        int toId = edge.getToNode();
//        EdgeKey edgeIndexKey = new EdgeKey(fromId, toId);
//        
//        if ( edgeIndex.containsKey(edgeIndexKey) ) {
//            throw new RuntimeException("Network already contains Edge with fromId " + edge.getFromNode() + " and toId " + edge.getToNode());
//        }
//        
//        edgeIndex.put(edgeIndexKey, edges.size());
//        edges.add(edge);
//        
//        if ( ! successorIndex.containsKey(fromId) ) { successorIndex.put(fromId, new ArrayList<Integer>()); }
//        if ( ! predecessorIndex.containsKey(toId) ) { predecessorIndex.put(toId, new ArrayList<Integer>()); }
//        
//        if ( ! successorIndex.get(fromId).contains(toId) ) { successorIndex.get(fromId).add(toId); }
//        if ( ! predecessorIndex.get(toId).contains(fromId) ) { predecessorIndex.get(toId).add(fromId); }
//    }
//    
//    public void addTraversal(V traversal)
//    {
//        int startId = traversal.getStartId();
//        int thruId = traversal.getThruId();
//        int endId = traversal.getEndId();
//        TraversalKey traversalIndexKey = new TraversalKey(startId, thruId, endId);
//        
//        traversalIndex.put(traversalIndexKey, traversals.size());
//        traversals.add(traversal);
//    }
//    
//    public boolean containsNodeId(int id) {
//        return nodeIndex.containsKey(id);
//    }
//    
//    public boolean containsEdgeIds(int[] ids) {
//        return edgeIndex.containsKey(new EdgeKey(ids[0],ids[1]));
//    }
//    
//    public boolean containsTraversalIds(int[] ids) {
//        return traversalIndex.containsKey(new TraversalKey(ids[0],ids[1],ids[2]));
//    }
//    
//    private class EdgeKey {
//        private int fromId, toId;
//        
//        EdgeKey(int fromId, int toId) {
//            this.fromId = fromId;
//            this.toId = toId;
//        }
//
//        @Override
//        public int hashCode()
//        {
//            final int prime = 31;
//            int result = 1;
//            result = prime * result + fromId;
//            result = prime * result + toId;
//            return result;
//        }
//
//        @Override
//        public boolean equals(Object obj)
//        {
//            if (this == obj) return true;
//            if (obj == null) return false;
//            if (getClass() != obj.getClass()) return false;
//            EdgeKey other = (EdgeKey) obj;
//            if (fromId != other.fromId) return false;
//            if (toId != other.toId) return false;
//            return true;
//        }        
//    }
//    
//    private class TraversalKey {
//        private int startId, thruId, endId;
//
//        public TraversalKey(int startId, int thruId, int endId)
//        {
//            this.startId = startId;
//            this.thruId = thruId;
//            this.endId = endId;
//        }
//
//        @Override
//        public int hashCode()
//        {
//            final int prime = 31;
//            int result = 1;
//            result = prime * result + endId;
//            result = prime * result + startId;
//            result = prime * result + thruId;
//            return result;
//        }
//
//        @Override
//        public boolean equals(Object obj)
//        {
//            if (this == obj) return true;
//            if (obj == null) return false;
//            if (getClass() != obj.getClass()) return false;
//            TraversalKey other = (TraversalKey) obj;
//            if (endId != other.endId) return false;
//            if (startId != other.startId) return false;
//            if (thruId != other.thruId) return false;
//            return true;
//        }
//    }
}
