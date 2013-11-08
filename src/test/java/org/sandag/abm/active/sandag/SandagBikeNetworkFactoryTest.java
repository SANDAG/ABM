package org.sandag.abm.active.sandag;
import java.util.*;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import org.sandag.abm.active.*;
import static org.junit.Assert.*;
import org.junit.*;

public class SandagBikeNetworkFactoryTest
{
    final static String RESOURCE_BUNDLE_NAME = "sandag_abm_active_test";
    Map<String,String> propertyMap = new HashMap<String,String>();
    SandagBikeNetworkFactory factory;
    Network<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> network;
    
    @Before
    public void setUp() {
        ResourceBundle rb = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME);
        propertyMap = new HashMap<String,String>();
        Enumeration<String> keys = rb.getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            propertyMap.put(key, rb.getString(key));
        }
        factory = new SandagBikeNetworkFactory(propertyMap);
        network = factory.createNetwork();
        try {
            FileWriter writer =  new FileWriter(new File(propertyMap.get("active.sample.output") + "edges.csv"));
            Iterator<SandagBikeEdge> it = network.edgeIterator();
            while ( it.hasNext() ) {
                SandagBikeEdge e = it.next();
                writer.write(e.getFromNode().getId() + "," + e.getToNode().getId() + "," + e.bikeCost + "\n");
            }
            writer.flush();
            writer.close();   
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    
    @Test
    public void testIteratorAndGetReturnSameNode()
    {
        Iterator<SandagBikeNode> nodeIterator = network.nodeIterator();
        SandagBikeNode node1, node2;
        while (nodeIterator.hasNext()) {
            node1 = nodeIterator.next();
            node2 = network.getNode(node1.getId());
            assertEquals(node1, node2);
        }
    }
    
    @Test
    public void testIteratorAndGetReturnSameEdge()
    {
        Iterator<SandagBikeEdge> edgeIterator = network.edgeIterator();
        SandagBikeEdge edge1, edge2;
        while (edgeIterator.hasNext()) {
            edge1 = edgeIterator.next();
            edge2 = network.getEdge(edge1.getFromNode(), edge1.getToNode());
            assertEquals(edge1, edge2);
        }
    }
    
    @Test
    public void testIteratorAndGetReturnSameTraversal()
    {
        Iterator<SandagBikeTraversal> traversalIterator = network.traversalIterator();
        SandagBikeTraversal traversal1, traversal2;
        while (traversalIterator.hasNext()) {
            traversal1 = traversalIterator.next();
            traversal2 = network.getTraversal(traversal1.getFromEdge(), traversal1.getToEdge());
            assertEquals(traversal1, traversal2);
        }
    }
    
    @Test
    public void testEachSuccessorFormsEdge()
    {
        Iterator<SandagBikeNode> nodeIterator = network.nodeIterator();
        SandagBikeNode node;
        while (nodeIterator.hasNext()) {
            node = nodeIterator.next();
            for (SandagBikeNode successor : network.getSuccessors(node) ) {
                assertTrue(network.containsEdge(node, successor));
            }
        }
    }
    
    @Test
    public void testEachPredecessorFormsEdge()
    {
        Iterator<SandagBikeNode> nodeIterator = network.nodeIterator();
        SandagBikeNode node;
        while (nodeIterator.hasNext()) {
            node = nodeIterator.next();
            for (SandagBikeNode predecessor : network.getPredecessors(node) ) {
                assertTrue(network.containsEdge(predecessor, node));
            }
        }
    }
    
    @Test
    public void testEachEdgeInSuccessorsOnce() {
        Iterator <SandagBikeEdge> edgeIterator = network.edgeIterator();
        SandagBikeEdge edge;
        while (edgeIterator.hasNext()) {
            edge = edgeIterator.next();
            int count = 0;
            for (SandagBikeNode node : network.getSuccessors(edge.getFromNode()) ) {
                count = count + ( node.equals(edge.getToNode()) ? 1 : 0);
            }
            assertEquals(1,count);
        }
    }
    
    @Test
    public void testEachEdgeInPredecessorsOnce() {
        Iterator <SandagBikeEdge> edgeIterator = network.edgeIterator();
        SandagBikeEdge edge;
        while (edgeIterator.hasNext()) {
            edge = edgeIterator.next();
            int count = 0;
            for (SandagBikeNode node : network.getPredecessors(edge.getToNode()) ) {
                count = count + ( node.equals( edge.getFromNode()) ? 1 : 0);
            }
            assertEquals(1,count);
        }
    }
    
    @Test
    public void testEachEdgeAndSuccessorFormsTraversal() {
        Iterator <SandagBikeEdge> edgeIterator = network.edgeIterator();
        SandagBikeEdge edge;
        
        while (edgeIterator.hasNext()) {
            edge = edgeIterator.next();
            for (SandagBikeNode s : network.getSuccessors(edge.getToNode()) ) {
                assertTrue(s.equals(edge.getFromNode()) || network.containsTraversal(edge, network.getEdge(edge.getToNode(),s)));
            }
        }
    }
    
    @Test
    public void testEachEdgeAndPredecessorFormsTraversal() {
        Iterator <SandagBikeEdge> edgeIterator = network.edgeIterator();
        SandagBikeEdge edge;
        while (edgeIterator.hasNext()) {
            edge = edgeIterator.next();
            for (SandagBikeNode p : network.getPredecessors(edge.getFromNode()) ) {
                assertTrue(p.equals(edge.getToNode()) || network.containsTraversal(network.getEdge(p, edge.getFromNode()), edge));
            }
        }
    }
    
    @Test
    public void testEachTraversalIsEdgeSequence() {
        Iterator <SandagBikeTraversal> traversalIterator = network.traversalIterator();
        SandagBikeTraversal traversal;
        while (traversalIterator.hasNext()) {
            traversal = traversalIterator.next();
            assertTrue(network.containsEdge(traversal.getFromEdge().getFromNode(), traversal.getFromEdge().getToNode()));
            assertTrue(network.containsEdge(traversal.getToEdge().getFromNode(), traversal.getToEdge().getToNode()));
        }
    }
    
    @Test
    public void testFieldValuesMatchInput() {
        SandagBikeNode node = network.getNode(100003629);
        assertEquals(3629, node.mgra);
        assertEquals(6264311, node.x, 5);
        
        SandagBikeEdge edge = network.getEdge(network.getNode(755011), network.getNode(753841));
        assertEquals(0.11, edge.distance, 0.01);
        edge = network.getEdge(network.getNode(746401), network.getNode(749381));
        assertEquals(5, edge.gain);
        edge = network.getEdge(network.getNode(749381), network.getNode(746401));
        assertEquals(0, edge.gain);
    }
    
    @Test
    public void testTurnTypesMatchInput() {
        int start, thru, end;
        SandagBikeTraversal traversal;

        start = 728811; thru = 727491; end =723691;
        traversal = network.getTraversal( network.getEdge(network.getNode(start), network.getNode(thru)), network.getEdge(network.getNode(thru), network.getNode(end)));
        assertEquals(TurnType.NONE, traversal.turnType);
        
        start = 728811; thru = 727491; end =727871;
        traversal = network.getTraversal( network.getEdge(network.getNode(start), network.getNode(thru)), network.getEdge(network.getNode(thru), network.getNode(end)));
        assertEquals(TurnType.LEFT,traversal.turnType);
        
        start = 728811; thru = 727491; end =726911;
        traversal = network.getTraversal( network.getEdge(network.getNode(start), network.getNode(thru)), network.getEdge(network.getNode(thru), network.getNode(end)));
        assertEquals(TurnType.RIGHT, traversal.turnType);
        
        start = 723511; thru = 725251; end =723691;
        traversal = network.getTraversal( network.getEdge(network.getNode(start), network.getNode(thru)), network.getEdge(network.getNode(thru), network.getNode(end)));
        assertEquals(TurnType.RIGHT, traversal.turnType);
        
        start = 723511; thru = 725251; end =726911;
        traversal = network.getTraversal( network.getEdge(network.getNode(start), network.getNode(thru)), network.getEdge(network.getNode(thru), network.getNode(end)));        
        assertEquals(TurnType.NONE, traversal.turnType);
        
        start = 726911; thru = 723691; end =725251;
        traversal = network.getTraversal( network.getEdge(network.getNode(start), network.getNode(thru)), network.getEdge(network.getNode(thru), network.getNode(end)));
        assertEquals(TurnType.RIGHT, traversal.turnType);
        
        start = 739131; thru = 739421; end =736701;
        traversal = network.getTraversal( network.getEdge(network.getNode(start), network.getNode(thru)), network.getEdge(network.getNode(thru), network.getNode(end)));
        assertEquals(TurnType.NONE, traversal.turnType);
        
        start = 762181; thru = 760261; end =759961;
        traversal = network.getTraversal( network.getEdge(network.getNode(start), network.getNode(thru)), network.getEdge(network.getNode(thru), network.getNode(end)));
        assertEquals(TurnType.RIGHT, traversal.turnType);
        
        start = 743031; thru = 741901; end =100003897;
        traversal = network.getTraversal( network.getEdge(network.getNode(start), network.getNode(thru)), network.getEdge(network.getNode(thru), network.getNode(end)));
        assertEquals(TurnType.NONE, traversal.turnType);
        
        start = 743031; thru = 741901; end =740811;
        traversal = network.getTraversal( network.getEdge(network.getNode(start), network.getNode(thru)), network.getEdge(network.getNode(thru), network.getNode(end)));
        assertEquals(TurnType.NONE, traversal.turnType);
        
        start = 743031; thru = 741901; end =742821;
        traversal = network.getTraversal( network.getEdge(network.getNode(start), network.getNode(thru)), network.getEdge(network.getNode(thru), network.getNode(end)));
        assertEquals(TurnType.LEFT, traversal.turnType);
        
        start = 752521; thru = 750321; end =747871;
        traversal = network.getTraversal( network.getEdge(network.getNode(start), network.getNode(thru)), network.getEdge(network.getNode(thru), network.getNode(end)));
        assertEquals(TurnType.NONE, traversal.turnType);
    }
}
