package org.sandag.abm.active.sandag;
import java.util.*;
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
        network = factory.create();
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
            edge2 = network.getEdge(edge1.getFromId(), edge1.getToId());
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
            traversal2 = network.getTraversal(traversal1.getStartId(), traversal1.getThruId(), traversal1.getEndId());
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
            for (Node successor : network.getSuccessors(node.getId()) ) {
                assertTrue(network.containsEdgeIds(new int[] {node.getId(), successor.getId()}));
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
            for (Node predecessor : network.getPredecessors(node.getId()) ) {
                assertTrue(network.containsEdgeIds(new int[] {predecessor.getId(), node.getId()}));
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
            for (SandagBikeNode node : network.getSuccessors(edge.getFromId()) ) {
                count = count + ( node.getId() == edge.getToId() ? 1 : 0);
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
            for (SandagBikeNode node : network.getPredecessors(edge.getToId()) ) {
                count = count + ( node.getId() == edge.getFromId() ? 1 : 0);
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
            for (int s : network.getSuccessorIds(edge.getToId()) ) {
                assertTrue(network.containsTraversalIds(new int[] {edge.getFromId(), edge.getToId(), s}));
            }
        }
    }
    
    @Test
    public void testEachEdgeAndPredecessorFormsTraversal() {
        Iterator <SandagBikeEdge> edgeIterator = network.edgeIterator();
        SandagBikeEdge edge;
        while (edgeIterator.hasNext()) {
            edge = edgeIterator.next();
            for (int p : network.getPredecessorIds(edge.getFromId()) ) {
                assertTrue(network.containsTraversalIds(new int[] {p, edge.getFromId(), edge.getToId()}));
            }
        }
    }
    
    @Test
    public void testEachTraversalIsEdgeSequence() {
        Iterator <SandagBikeTraversal> traversalIterator = network.traversalIterator();
        SandagBikeTraversal traversal;
        while (traversalIterator.hasNext()) {
            traversal = traversalIterator.next();
            assertTrue(network.containsEdgeIds(new int[] {traversal.getStartId(), traversal.getThruId()}));
            assertTrue(network.containsEdgeIds(new int[] {traversal.getThruId(), traversal.getEndId()}));
        }
    }
    
    @Test
    public void testFieldValuesMatchInput() {
        SandagBikeNode node = network.getNode(100003629);
        assertEquals(3629, node.mgra);
        assertEquals(6264311, node.x, 5);
        
        SandagBikeEdge edge = network.getEdge(755011, 753841);
        assertEquals(580.8311, edge.distance, 0.001);
        edge = network.getEdge(746401, 749381);
        assertEquals(5, edge.gain);
        edge = network.getEdge(749381, 746401);
        assertEquals(0, edge.gain);
    }
    
    @Test
    public void testTurnTypesMatchInput() {
        SandagBikeTraversal traversal = network.getTraversal(728811, 727491, 728811);
        assertEquals(TurnType.REVERSAL, traversal.turnType);
        traversal = network.getTraversal(728811,727491,723691);
        assertEquals(TurnType.NONE, traversal.turnType);
        traversal = network.getTraversal(728811,727491,727871);
        assertEquals(TurnType.LEFT,traversal.turnType);
        traversal = network.getTraversal(728811,727491,726911);
        assertEquals(TurnType.RIGHT, traversal.turnType);
        traversal = network.getTraversal(723511, 725251, 723691);
        assertEquals(TurnType.RIGHT, traversal.turnType);
        traversal = network.getTraversal(723511, 725251, 726911);
        assertEquals(TurnType.NONE, traversal.turnType);
        traversal = network.getTraversal(726911, 723691, 725251);
        assertEquals(TurnType.RIGHT, traversal.turnType);
        traversal = network.getTraversal(739131, 739421, 736701);
        assertEquals(TurnType.NONE, traversal.turnType);
        traversal = network.getTraversal(762181, 760261, 759961);
        assertEquals(TurnType.RIGHT, traversal.turnType);
        traversal = network.getTraversal(743031, 741901, 100003897);
        assertEquals(TurnType.NONE, traversal.turnType);
        traversal = network.getTraversal(743031, 741901, 740811);
        assertEquals(TurnType.NONE, traversal.turnType);
        traversal = network.getTraversal(743031, 741901, 742821);
        assertEquals(TurnType.LEFT, traversal.turnType);
    }
}
