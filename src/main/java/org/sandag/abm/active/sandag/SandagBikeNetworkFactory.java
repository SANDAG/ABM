package org.sandag.abm.active.sandag;
import org.sandag.abm.active.*;
import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import org.apache.log4j.Logger;
import com.linuxense.javadbf.*;

public class SandagBikeNetworkFactory extends NetworkFactory<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal>
{
    private Map<String,String> propertyMap;
    private PropertyParser propertyParser;
    
    private static final String PROPERTIES_NODE_FILE = "active.node.file";
    private static final String PROPERTIES_NODE_ID = "active.node.id";
    private static final String PROPERTIES_NODE_FIELDNAMES = "active.node.fieldnames";
    private static final String PROPERTIES_NODE_COLUMNS = "active.node.columns";
    private static final String PROPERTIES_EDGE_FILE = "active.edge.file";
    private static final String PROPERTIES_EDGE_ANODE = "active.edge.anode";
    private static final String PROPERTIES_EDGE_BNODE = "active.edge.bnode";
    private static final String PROPERTIES_EDGE_DIRECTIONAL = "active.edge.directional";
    private static final String PROPERTIES_EDGE_FIELDNAMES = "active.edge.fieldnames";
    private static final String PROPERTIES_EDGE_COLUMNS_AB = "active.edge.columns.ab";
    private static final String PROPERTIES_EDGE_COLUMNS_BA = "active.edge.columns.ba";
    private static final String PROPERTIES_EDGE_CENTROID_FIELD = "active.edge.centroid.field";
    private static final String PROPERTIES_EDGE_CENTROID_VALUE = "active.edge.centroid.value";
    private static final String PROPERTIES_EDGE_AUTOSPERMITTED_FIELD = "active.edge.autospermitted.field";
    private static final String PROPERTIES_EDGE_AUTOSPERMITTED_VALUES = "active.edge.autospermitted.values";
    
    private static final double TURN_ANGLE_TOLERANCE = Math.PI / 6;
    
    public SandagBikeNetworkFactory(Map<String, String> propertyMap)
    {
        super(SandagBikeNode.class, SandagBikeEdge.class, SandagBikeTraversal.class);
        this.propertyMap = propertyMap;
        propertyParser = new PropertyParser(propertyMap);
        logger = Logger.getLogger(SandagBikeNetworkFactory.class);
    }
    
    protected void readNodes()
    {
        try{
            InputStream stream = new FileInputStream(propertyMap.get(PROPERTIES_NODE_FILE));
            DBFReader reader = new DBFReader(stream);
            Map<String,String> fieldMap = propertyParser.mapStringPropertyListToStrings(PROPERTIES_NODE_FIELDNAMES, PROPERTIES_NODE_COLUMNS);
            Field f;
            SandagBikeNode node;
            int fieldCount = reader.getFieldCount();
            Map<String,Integer> labels = new HashMap<String,Integer>();
            for (int i=0; i<fieldCount; i++) {
                labels.put(reader.getField(i).getName(), i);
            }
            Object[] rowObjects;
            while ( ( rowObjects = reader.nextRecord() ) != null )  {             
                int id = ((Number) rowObjects[labels.get(propertyMap.get(PROPERTIES_NODE_ID))] ).intValue();
                node = createDefaultNode(id);
                for (String fieldName : fieldMap.keySet()) {
                    try {
                        f = nodeClass.getField(fieldName);
                        setNumericFieldWithCast(node, f, (Number) rowObjects[labels.get(fieldMap.get(fieldName))]);
                     } catch (NoSuchFieldException | SecurityException e) {
                        logger.error( "Exception caught getting class field " + fieldName + " for object of class " + node.getClass().getName(), e);
                        throw new RuntimeException();
                     }
                }
                network.addNode(node);   
            }
        } catch  (IOException e) {
            logger.error( "Exception caught reading nodes from disk.", e);
            throw new RuntimeException();
        }    
    }
    
    protected void readEdges()
    {
        try {
            InputStream stream = new FileInputStream(propertyMap.get(PROPERTIES_EDGE_FILE));
            DBFReader reader = new DBFReader(stream);
            Map<String,String> abFieldMap = propertyParser.mapStringPropertyListToStrings(PROPERTIES_EDGE_FIELDNAMES, PROPERTIES_EDGE_COLUMNS_AB);
            Map<String,String> baFieldMap = new HashMap<String,String>();
            boolean directional = Boolean.parseBoolean(propertyMap.get(PROPERTIES_EDGE_DIRECTIONAL));
            if ( ! directional ) { baFieldMap = propertyParser.mapStringPropertyListToStrings(PROPERTIES_EDGE_FIELDNAMES, PROPERTIES_EDGE_COLUMNS_BA); }
            Field f;
            SandagBikeEdge edge;
            int columnCount = reader.getFieldCount();
            Map<String,Integer> labels = new HashMap<String,Integer>();
            for (int i=0; i<columnCount; i++) {
                labels.put(reader.getField(i).getName(), i);
            }
            Object[] rowObjects;
            while ( ( rowObjects = reader.nextRecord() ) != null )  {             
                int a = ((Number) rowObjects[labels.get(propertyMap.get(PROPERTIES_EDGE_ANODE))] ).intValue();
                int b = ((Number) rowObjects[labels.get(propertyMap.get(PROPERTIES_EDGE_BNODE))] ).intValue();
                
                edge = createDefaultEdge(a, b);
                for (String fieldName : abFieldMap.keySet()) {
                    try {
                        f = edgeClass.getField(fieldName);
                        setNumericFieldWithCast(edge, f, (Number) rowObjects[labels.get(abFieldMap.get(fieldName))]);
                    } catch (NoSuchFieldException | SecurityException e) {
                        logger.error( "Exception caught getting class field " + fieldName + " for object of class " + edge.getClass().getName(), e);
                        throw new RuntimeException();
                    }
                }
                addEdgeWithDefaultNodesAndTraversals(edge);
                
                if ( ! directional ){
                    edge = createDefaultEdge(b, a);
                    for (String fieldName : baFieldMap.keySet()) {
                        try {
                            f = edgeClass.getField(fieldName);
                            setNumericFieldWithCast(edge, f, (Number) rowObjects[labels.get(baFieldMap.get(fieldName))]);
                        } catch (NoSuchFieldException | SecurityException e) {
                            logger.error( "Exception caught getting class field " + fieldName + " for object of class " + edge.getClass().getName(), e);
                            throw new RuntimeException();
                        }
                    }            
                    addEdgeWithDefaultNodesAndTraversals(edge);
                }                
            }
        } catch  (IOException e) {
            logger.error( "Exception caught reading edges from disk.", e);
            throw new RuntimeException();
        }
    }
    
    protected void handleDefaultNodes()
    {
        if ( defaultNodeIds.size() > 0 ) { throw new RuntimeException("Edge data contain nodes missing from node file"); }
    }
    
    protected void calculateDerivedNodeAttributes()
    {
        Iterator<SandagBikeNode> nodeIterator = network.nodeIterator();
        SandagBikeNode n;
        while ( nodeIterator.hasNext() ) {
            n = nodeIterator.next();
            n.centroid = ( n.mgra > 0 ) || ( n.taz > 0 );
        }
    }
    
    protected void calculateDerivedEdgeAttributes()
    {
        try {
            Iterator<SandagBikeEdge> edgeIterator;
            Field f;
            
            edgeIterator = network.edgeIterator();
            f = edgeClass.getField(propertyMap.get(PROPERTIES_EDGE_AUTOSPERMITTED_FIELD));
            SandagBikeEdge edge;
            while ( edgeIterator.hasNext() ) {
                edge = edgeIterator.next();
                edge.autosPermitted = propertyParser.isIntValueInPropertyList(f.getInt(edge),PROPERTIES_EDGE_AUTOSPERMITTED_VALUES);
            }
            
            edgeIterator = network.edgeIterator();
            f = edgeClass.getField(propertyMap.get(PROPERTIES_EDGE_CENTROID_FIELD));
            while ( edgeIterator.hasNext() ) {
                edge = edgeIterator.next();
                edge.centroidConnector = propertyParser.isIntValueInPropertyList(f.getInt(edge),PROPERTIES_EDGE_CENTROID_VALUE);
            }
            
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error( "Exception caught calculating derived edge attributes.", e);
            throw new RuntimeException();
        }
    }
    
    protected void calculateDerivedTraversalAttributes()
    {     
        Iterator<SandagBikeTraversal> traversalIterator = network.traversalIterator();
        SandagBikeTraversal t;
        while ( traversalIterator.hasNext() ) {
            t = traversalIterator.next();
            t.turnType = calculateTurnType(t);
        }
    }
    
    private double calculateTraversalAngle(SandagBikeTraversal t)
    {    
        float xDiff1 = network.getNode(t.getThruId()).x - network.getNode(t.getStartId()).x;
        float xDiff2 = network.getNode(t.getEndId()).x - network.getNode(t.getThruId()).x;
        float yDiff1 = network.getNode(t.getThruId()).y - network.getNode(t.getStartId()).y;
        float yDiff2 = network.getNode(t.getEndId()).y - network.getNode(t.getThruId()).y;
        
        double angle = Math.atan2(yDiff2, xDiff2) - Math.atan2(yDiff1, xDiff1);
        
        if ( angle > Math.PI ) {
            angle = angle - 2 * Math.PI;
        }
        if (angle < - Math.PI ) {
            angle = angle + 2 * Math.PI;
        }
    
        return angle;
    }
    
    private TurnType calculateTurnType(SandagBikeTraversal t)
    {        
        int startId = t.getStartId();
        int thruId = t.getThruId();
        int endId = t.getEndId();
        
        if ( network.getNode(startId).centroid || network.getNode(thruId).centroid || network.getNode(endId).centroid ) {
            return TurnType.NONE;
        }
        
        if ( startId == endId ) {
            return TurnType.REVERSAL;
        }
        
        double thisAngle = calculateTraversalAngle(t);
    
        if ( thisAngle < - Math.PI + TURN_ANGLE_TOLERANCE || thisAngle > Math.PI - TURN_ANGLE_TOLERANCE ) {
            return TurnType.REVERSAL;
        }
        
        double minAngle = Math.PI;
        double maxAngle = -Math.PI;
        double minAbsAngle = Math.PI;
        double currentAngle;
        int legCount = 1;
        
        for (SandagBikeNode successor : network.getSuccessors(thruId) ) {
            if ( network.getEdge(thruId, successor.getId()).autosPermitted && successor.getId() != startId ){
                currentAngle = calculateTraversalAngle(network.getTraversal(startId, thruId, successor.getId()));
                minAngle = Math.min(minAngle, currentAngle);
                maxAngle = Math.max(maxAngle, currentAngle);
                minAbsAngle = Math.min(minAbsAngle, Math.abs(currentAngle));
                legCount += 1;
            }
        }
        
        if ( legCount <= 2 ) {
            return TurnType.NONE;
        } else if ( legCount == 3) {
            if ( thisAngle <= minAngle && Math.abs(thisAngle) > TURN_ANGLE_TOLERANCE ) {
                return TurnType.RIGHT;
            } else if ( thisAngle >= maxAngle && Math.abs(thisAngle) > TURN_ANGLE_TOLERANCE ) {
                return TurnType.LEFT;
            } else {
                return TurnType.NONE;
            }
        } else {
            if ( Math.abs(thisAngle) <= minAbsAngle || ( Math.abs(thisAngle) < TURN_ANGLE_TOLERANCE && thisAngle > minAngle && thisAngle < maxAngle ) ) {
                return TurnType.NONE;
            } else if ( thisAngle < 0 ) {
                return TurnType.RIGHT;
            } else {
                return TurnType.LEFT;            
            }
        }
    }
        
    private void setNumericFieldWithCast(Object o, Field f, Number n) {
        Class<?> c = f.getType();
        try {
            if ( c.equals(Integer.class) || c.equals(Integer.TYPE) ) {
                f.set(o,n.intValue());      
            } else if ( c.equals(Float.class) || c.equals(Float.TYPE) ) {
                f.set(o,n.floatValue());
            } else if ( c.equals(Double.class) || c.equals(Double.TYPE) ) {
                f.set(o,n.doubleValue());
            } else if ( c.equals(Boolean.class) || c.equals(Boolean.TYPE) ) {
                f.set(o, n.intValue() == 1);
            } else if ( c.equals(Byte.class) || c.equals(Byte.TYPE)) {
                f.set(o, n.byteValue());
            } else if ( c.equals(Short.class) || c.equals(Short.TYPE)) {
                f.set(o, n.shortValue());
            } else if ( c.equals(Long.class) || c.equals(Long.TYPE)) {
                f.set(o, n.longValue());
            } else {
                throw new RuntimeException("Field " + f.getName() + " in class " + o.getClass().getName() + " is not numeric");
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            logger.error( "Exception caught setting class field " + f.getName() + " for object of class " + o.getClass().getName(), e);
            throw new RuntimeException();
        }
    }
}
