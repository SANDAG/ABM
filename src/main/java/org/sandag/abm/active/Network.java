package org.sandag.abm.active;
import java.util.*;

public class Network <T extends Node, U extends Edge, V extends Traversal>
{
  
    private List<T> nodes;
    private List<U> edges;
    private List<V> traversals;
    
    private Map<Integer,Integer> nodeIndex;
    private Map<EdgeKey,Integer> edgeIndex;
    private Map<TraversalKey,Integer> traversalIndex;
    
    private Map<Integer,ArrayList<Integer>> successorIndex;
    private Map<Integer,ArrayList<Integer>> predecessorIndex;
    
    public Network()
    {
        nodes = new ArrayList<T>();
        edges = new ArrayList<U>();
        traversals = new ArrayList<V>();
        nodeIndex = new HashMap<Integer,Integer>();
        edgeIndex = new HashMap<EdgeKey,Integer>();
        traversalIndex = new HashMap<TraversalKey,Integer>();
        successorIndex =  new HashMap<Integer,ArrayList<Integer>>();
        predecessorIndex =  new HashMap<Integer,ArrayList<Integer>>();
    }
    
    public T getNode(int nodeId)
    {
        return nodes.get(nodeIndex.get(nodeId));
    }
    
    public U getEdge(int fromId, int toId)
    {
        return edges.get(edgeIndex.get(new EdgeKey(fromId,toId)));
    }
    
    public V getTraversal(int startId, int thruId, int endId)
    {
        return traversals.get(traversalIndex.get(new TraversalKey(startId, thruId, endId)));
    }
    
    public List<Integer> getSuccessorIds(int nodeId) {
        return successorIndex.get(nodeId);
    }
    
    public List<Integer> getPredecessorIds(int nodeId) {
        return predecessorIndex.get(nodeId);
    }
    
    public List<T> getSuccessors(int nodeId)
    {
        List<T> successors = new ArrayList<T>();
        for ( int s : successorIndex.get(nodeId) ) {
            successors.add(nodes.get(nodeIndex.get(s)));
        }
        return successors;
    }
    
    public List<T> getPredecessors(int nodeId)
    {
        List<T> predecessors = new ArrayList<T>();
        for ( int p : predecessorIndex.get(nodeId) ) {
            predecessors.add(nodes.get(nodeIndex.get(p)));
        }
        return predecessors;
    }
    
    public Iterator<T> nodeIterator()
    {
        return nodes.iterator();
    }
    
    public Iterator<U> edgeIterator()
    {
        return edges.iterator();
    }
    
    public Iterator<V> traversalIterator()
    {
        return traversals.iterator();
    }

    public void addNode(T node)
    {
        if ( nodeIndex.containsKey(node.getId()) ) {
            throw new RuntimeException("Network already contains Node with id " + node.getId());
        }
        nodeIndex.put(node.getId(), nodes.size());
        nodes.add(node);
        if (! successorIndex.containsKey(node.getId()) ) { successorIndex.put(node.getId(), new ArrayList<Integer>()); }
        if (! predecessorIndex.containsKey(node.getId()) ) { predecessorIndex.put(node.getId(), new ArrayList<Integer>()); }
    }
    
    public void addEdge(U edge)
    {
        int fromId = edge.getFromId();
        int toId = edge.getToId();
        EdgeKey edgeIndexKey = new EdgeKey(fromId, toId);
        
        if ( edgeIndex.containsKey(edgeIndexKey) ) {
            throw new RuntimeException("Network already contains Edge with fromId " + edge.getFromId() + " and toId " + edge.getToId());
        }
        
        edgeIndex.put(edgeIndexKey, edges.size());
        edges.add(edge);
        
        if ( ! successorIndex.containsKey(fromId) ) { successorIndex.put(fromId, new ArrayList<Integer>()); }
        if ( ! predecessorIndex.containsKey(toId) ) { predecessorIndex.put(toId, new ArrayList<Integer>()); }
        
        if ( ! successorIndex.get(fromId).contains(toId) ) { successorIndex.get(fromId).add(toId); }
        if ( ! predecessorIndex.get(toId).contains(fromId) ) { predecessorIndex.get(toId).add(fromId); }
    }
    
    public void addTraversal(V traversal)
    {
        int startId = traversal.getStartId();
        int thruId = traversal.getThruId();
        int endId = traversal.getEndId();
        TraversalKey traversalIndexKey = new TraversalKey(startId, thruId, endId);
        
        traversalIndex.put(traversalIndexKey, traversals.size());
        traversals.add(traversal);
    }
    
    public boolean containsNodeId(int id) {
        return nodeIndex.containsKey(id);
    }
    
    public boolean containsEdgeIds(int[] ids) {
        return edgeIndex.containsKey(new EdgeKey(ids[0],ids[1]));
    }
    
    public boolean containsTraversalIds(int[] ids) {
        return traversalIndex.containsKey(new TraversalKey(ids[0],ids[1],ids[2]));
    }
    
    private class EdgeKey {
        private int fromId, toId;
        
        EdgeKey(int fromId, int toId) {
            this.fromId = fromId;
            this.toId = toId;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + fromId;
            result = prime * result + toId;
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            EdgeKey other = (EdgeKey) obj;
            if (fromId != other.fromId) return false;
            if (toId != other.toId) return false;
            return true;
        }        
    }
    
    private class TraversalKey {
        private int startId, thruId, endId;

        public TraversalKey(int startId, int thruId, int endId)
        {
            this.startId = startId;
            this.thruId = thruId;
            this.endId = endId;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + endId;
            result = prime * result + startId;
            result = prime * result + thruId;
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            TraversalKey other = (TraversalKey) obj;
            if (endId != other.endId) return false;
            if (startId != other.startId) return false;
            if (thruId != other.thruId) return false;
            return true;
        }
    }
        
}
