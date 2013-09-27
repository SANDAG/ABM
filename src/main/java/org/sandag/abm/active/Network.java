package org.sandag.abm.active;
import com.pb.common.datafile.*;
import java.util.*;

public class Network
{
    private TableDataSet nodeAttributeTable;
    private TableDataSet edgeAttributeTable;
    private TableDataSet traversalAttributeTable;
    
    private Map<Node,Integer> nodeIndex;
    private Map<Edge,Integer> edgeIndex;
    private Map<Traversal,Integer> traversalIndex;
    
    private Map<Node,ArrayList<Node>> successorIndex;
    private Map<Node,ArrayList<Node>> predecessorIndex;
    
    public Network()
    {
        nodeAttributeTable = new TableDataSet();
        edgeAttributeTable = new TableDataSet();
        traversalAttributeTable = new TableDataSet();
        nodeIndex = new HashMap<Node,Integer>();
        edgeIndex = new HashMap<Edge,Integer>();
        traversalIndex = new HashMap<Traversal,Integer>();
        successorIndex =  new HashMap<Node,ArrayList<Node>>();
        predecessorIndex =  new HashMap<Node,ArrayList<Node>>();
    }
    
    public void addNode(Node node, HashMap attributes)
    {
        nodeAttributeTable.appendRow(attributes);
        nodeIndex.put(node, nodeAttributeTable.getRowCount());
        successorIndex.put(node, new ArrayList<Node>());
        predecessorIndex.put(node, new ArrayList<Node>());
    }
    
    public void addNode(int id, HashMap attributes)
    {
        addNode(new Node(id), attributes);
    }
    
    public void addNode(Node node)
    {
        addNode(node, null);
    }
    
    public void addNode(int id)
    {
        addNode(new Node(id));
    }
    
    public void addEdge(Edge edge, HashMap attributes)
    {
        Node fromNode = edge.getFromNode();
        Node toNode = edge.getToNode();
        
        if ( ! nodeIndex.containsKey(fromNode) ){
            addNode(fromNode);
            successorIndex.put(fromNode, new ArrayList<Node>());
            predecessorIndex.put(fromNode, new ArrayList<Node>());
        }
        if ( ! nodeIndex.containsKey(toNode) ){
            addNode(toNode);
            successorIndex.put(toNode, new ArrayList<Node>());
            predecessorIndex.put(toNode, new ArrayList<Node>());
        }
        
        edgeAttributeTable.appendRow(attributes);
        edgeIndex.put(edge, edgeAttributeTable.getRowCount());
        
        if ( ! successorIndex.get(fromNode).contains(toNode) ) { successorIndex.get(fromNode).add(toNode); }
        if ( ! predecessorIndex.get(toNode).contains(fromNode) ) { successorIndex.get(toNode).add(fromNode); }
    
        for (Node n : getSuccessors(fromNode)){
            Traversal t = new Traversal(edge, new Edge(toNode, n));
            if ( ! traversalIndex.containsKey(t) ){
                traversalAttributeTable.appendRow(null);
                traversalIndex.put(t, traversalAttributeTable.getRowCount());
            }
        }
        
        for (Node n : getPredecessors(toNode)){
            Traversal t = new Traversal(new Edge(n, fromNode), edge);
            if ( ! traversalIndex.containsKey(t) ){
                traversalAttributeTable.appendRow(null);
                traversalIndex.put(t, traversalAttributeTable.getRowCount());
            }
        }
    }
    
    public void addEdge(Edge edge)
    {
        addEdge(edge, null);
    }
    
    public void addEdge(int fromId, int toId, HashMap attributes)
    {
        addEdge(new Edge(fromId, toId), attributes);
    }
    
    public void addEdge(int fromId, int toId)
    {
        addEdge(new Edge(fromId, toId));
    }
    
    public List<Node> getSuccessors(Node node)
    {
        return successorIndex.get(node);
    }
    
    public List<Node> getPredecessors(Node node)
    {
        return predecessorIndex.get(node);
    }
    
    public void addNodeAttribute(String attribute)
    {
        nodeAttributeTable.appendColumn(null, attribute);
    }
    
    public void addEdgeAttribute(String attribute)
    {
        edgeAttributeTable.appendColumn(null, attribute);
    }
    
    public void addTraversalAttribute(String attribute)
    {
        traversalAttributeTable.appendColumn(null, attribute);
    }
    
    public float getNodeAttributeValue(Node node, String attribute)
    {
        return nodeAttributeTable.getValueAt(nodeIndex.get(node), attribute);
    }
    
    public float getNodeAttributeValue(int id, String attribute)
    {
        return nodeAttributeTable.getValueAt(nodeIndex.get(new Node(id)), attribute);
    }
    
    public float getEdgeAttributeValue(Edge edge, String attribute)
    {
        return edgeAttributeTable.getValueAt(edgeIndex.get(edge), attribute);
    }
    
    public float getEdgeAttributeValue(int id1, int id2, String attribute)
    {
        return edgeAttributeTable.getValueAt(edgeIndex.get(new Edge(id1, id2)), attribute);
    }
    
    public float getTraversalAttributeValue(Traversal traversal, String attribute)
    {
        return traversalAttributeTable.getValueAt(traversalIndex.get(traversal), attribute);
    }
    
    public float getTraversalAttributeValue(int id1, int id2, int id3, String attribute)
    {
        return traversalAttributeTable.getValueAt(traversalIndex.get(new Traversal(id1, id2, id3)), attribute);
    }
    
    public void setNodeAttributeValue(Node node, String attribute, Number value)
    {
        nodeAttributeTable.setValueAt(nodeIndex.get(node), attribute, (Float) value);
    }
    
    public void setNodeAttributeValue(int id, String attribute, Number value)
    {
        nodeAttributeTable.setValueAt(nodeIndex.get(new Node(id)), attribute, (Float) value);
    }
    
    public void setEdgeAttributeValue(Edge edge, String attribute, Number value)
    {
        edgeAttributeTable.setValueAt(edgeIndex.get(edge), attribute, (Float) value);
    }
    
    public void setEdgeAttributeValue(int id1, int id2, String attribute, Number value)
    {
        edgeAttributeTable.setValueAt(edgeIndex.get(new Edge(id1, id2)), attribute, (Float) value);
    }
    
    public void setTraversalAttributeValue(Traversal traversal, String attribute, Number value)
    {
        traversalAttributeTable.setValueAt(traversalIndex.get(traversal), attribute, (Float) value);
    }
    
    public void setTraversalAttributeValue(int id1, int id2, int id3, String attribute, Number value)
    {
        traversalAttributeTable.setValueAt(traversalIndex.get(new Traversal(id1, id2, id3)), attribute, (Float) value);
    }
    
    public Iterator<Node> nodeIterator()
    {
        return nodeIndex.keySet().iterator();
    }
    
    public Iterator<Edge> edgeIterator()
    {
        return edgeIndex.keySet().iterator();
    }
    
    public Iterator<Traversal> traversalIterator()
    {
        return traversalIndex.keySet().iterator();
    }
    
}
