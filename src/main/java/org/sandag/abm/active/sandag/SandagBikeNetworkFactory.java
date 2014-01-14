package org.sandag.abm.active.sandag;
import org.sandag.abm.active.*;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;

import org.apache.log4j.Logger;

import com.linuxense.javadbf.*;

public class SandagBikeNetworkFactory extends AbstractNetworkFactory<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal>
{
    protected Logger logger = Logger.getLogger(SandagBikeNetworkFactory.class);
    private Map<String,String> propertyMap;
    private PropertyParser propertyParser;
    private Collection<SandagBikeNode> nodes = null;
    private Collection<SandagBikeEdge> edges = null;
    private Collection<SandagBikeTraversal> traversals = null;
    
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
    private static final double DISTANCE_CONVERSION_FACTOR = 0.000189;
    private static final double INACCESSIBLE_COST_COEF = 999.0;
    
    private static final String PROPERTIES_COEF_DISTCLA0 = "active.coef.distcla0";
    private static final String PROPERTIES_COEF_DISTCLA1 = "active.coef.distcla1";
    private static final String PROPERTIES_COEF_DISTCLA2 = "active.coef.distcla2";
    private static final String PROPERTIES_COEF_DISTCLA3 = "active.coef.distcla3";
    private static final String PROPERTIES_COEF_DARTNE2  = "active.coef.dartne2";
    private static final String PROPERTIES_COEF_DWRONGWY = "active.coef.dwrongwy";
    private static final String PROPERTIES_COEF_GAIN = "active.coef.gain";
    private static final String PROPERTIES_COEF_TURN = "active.coef.turn";
    private static final String PROPERTIES_COEF_DISTANCE_WALK = "active.coef.distance.walk";
    private static final String PROPERTIES_COEF_GAIN_WALK = "active.coef.gain.walk";
    private static final String PROPERTIES_COEF_DCYCTRAC = "active.coef.dcyctrac";
    private static final String PROPERTIES_COEF_DBIKBLVD = "active.coef.dbikblvd";
    private static final String PROPERTIES_COEF_SIGNALS = "active.coef.signals";
    private static final String PROPERTIES_COEF_UNLFRMA = "active.coef.unlfrma";
    private static final String PROPERTIES_COEF_UNLFRMI = "active.coef.unlfrmi";
    private static final String PROPERTIES_COEF_UNTOMA = "active.coef.untoma";
    private static final String PROPERTIES_COEF_UNTOMI = "active.coef.untomi";
    
    public SandagBikeNetworkFactory(Map<String,String> propertyMap)
    {
        this.propertyMap = propertyMap;
        propertyParser = new PropertyParser(propertyMap);
    }
    
    protected Collection<SandagBikeNode> readNodes()
    {
    	Set<SandagBikeNode> nodes = new LinkedHashSet<>();
        try{
            InputStream stream = new FileInputStream(propertyMap.get(PROPERTIES_NODE_FILE));
            DBFReader reader = new DBFReader(stream);
            Map<String,String> fieldMap = propertyParser.mapStringPropertyListToStrings(PROPERTIES_NODE_FIELDNAMES, PROPERTIES_NODE_COLUMNS);
            Field f;
            int fieldCount = reader.getFieldCount();
            Map<String,Integer> labels = new HashMap<String,Integer>();
            for (int i=0; i<fieldCount; i++) {
                labels.put(reader.getField(i).getName(), i);
            }
            Object[] rowObjects;
            while ( ( rowObjects = reader.nextRecord() ) != null )  {             
                int id = ((Number) rowObjects[labels.get(propertyMap.get(PROPERTIES_NODE_ID))] ).intValue();
                SandagBikeNode node = new SandagBikeNode(id);
                for (String fieldName : fieldMap.keySet()) {
                    try {
                        f = node.getClass().getField(fieldName);
                        setNumericFieldWithCast(node, f, (Number) rowObjects[labels.get(fieldMap.get(fieldName))]);
                     } catch (NoSuchFieldException | SecurityException e) {
                        logger.error( "Exception caught getting class field " + fieldName + " for object of class " + node.getClass().getName(), e);
                        throw new RuntimeException();
                     }
                }
                nodes.add(node);   
            }
        } catch  (IOException e) {
            logger.error( "Exception caught reading nodes from disk.", e);
            throw new RuntimeException();
        }    
        return nodes;
    }
    
    protected Collection<SandagBikeEdge> readEdges(Collection<SandagBikeNode> nodes)
    {
    	Set<SandagBikeEdge> edges = new LinkedHashSet<>();
    	Map<Integer,SandagBikeNode> idNodeMap = new HashMap<>();
    	for (SandagBikeNode node : nodes)
    		idNodeMap.put(node.getId(),node);
    	
        try {
            InputStream stream = new FileInputStream(propertyMap.get(PROPERTIES_EDGE_FILE));
            DBFReader reader = new DBFReader(stream);
            Map<String,String> abFieldMap = propertyParser.mapStringPropertyListToStrings(PROPERTIES_EDGE_FIELDNAMES, PROPERTIES_EDGE_COLUMNS_AB);
            Map<String,String> baFieldMap = new HashMap<String,String>();
            boolean directional = Boolean.parseBoolean(propertyMap.get(PROPERTIES_EDGE_DIRECTIONAL));
            if ( ! directional ) { baFieldMap = propertyParser.mapStringPropertyListToStrings(PROPERTIES_EDGE_FIELDNAMES, PROPERTIES_EDGE_COLUMNS_BA); }
            int columnCount = reader.getFieldCount();
            Map<String,Integer> labels = new HashMap<String,Integer>();
            for (int i=0; i<columnCount; i++) {
                labels.put(reader.getField(i).getName(), i);
            }
            Object[] rowObjects;
            while ( ( rowObjects = reader.nextRecord() ) != null )  {             
            	SandagBikeNode a = idNodeMap.get(((Number) rowObjects[labels.get(propertyMap.get(PROPERTIES_EDGE_ANODE))] ).intValue());
                SandagBikeNode b =  idNodeMap.get(((Number) rowObjects[labels.get(propertyMap.get(PROPERTIES_EDGE_BNODE))] ).intValue());
                
                SandagBikeEdge  edge = new SandagBikeEdge(a,b);
                for (String fieldName : abFieldMap.keySet()) {
                    try {
                        Field f = edge.getClass().getField(fieldName);
                        setNumericFieldWithCast(edge, f, (Number) rowObjects[labels.get(abFieldMap.get(fieldName))]);
                    } catch (NoSuchFieldException | SecurityException e) {
                        logger.error( "Exception caught getting class field " + fieldName + " for object of class " + edge.getClass().getName(), e);
                        throw new RuntimeException();
                    }
                }
                edges.add(edge);
                
                if ( ! directional ){
                    edge = new SandagBikeEdge(b,a);
                    for (String fieldName : baFieldMap.keySet()) {
                        try {
                            Field f = edge.getClass().getField(fieldName);
                            setNumericFieldWithCast(edge, f, (Number) rowObjects[labels.get(baFieldMap.get(fieldName))]);
                        } catch (NoSuchFieldException | SecurityException e) {
                            logger.error( "Exception caught getting class field " + fieldName + " for object of class " + edge.getClass().getName(), e);
                            throw new RuntimeException();
                        }
                    }            
                    edges.add(edge);
                }                
            }
        } catch  (IOException e) {
            logger.error( "Exception caught reading edges from disk.", e);
            throw new RuntimeException();
        }
        return edges;
    }
    
    @Override
    protected void calculateDerivedNodeAttributes(Network<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal> network)
    {
        Iterator<SandagBikeNode> nodeIterator = network.nodeIterator();
        while ( nodeIterator.hasNext() ) {
        	SandagBikeNode n = nodeIterator.next();
            n.centroid = ( n.mgra > 0 ) || ( n.taz > 0 );
            if ( n.mgra > 0 ) { n.taz = 0; }
        }
    }

    @Override
    protected void calculateDerivedEdgeAttributes(Network<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal> network)
    {
        try {
            Iterator<SandagBikeEdge> edgeIterator = network.edgeIterator();
            Field apf = SandagBikeEdge.class.getField(propertyMap.get(PROPERTIES_EDGE_AUTOSPERMITTED_FIELD));
            Field cf = SandagBikeEdge.class.getField(propertyMap.get(PROPERTIES_EDGE_CENTROID_FIELD));
            while ( edgeIterator.hasNext() ) {
            	SandagBikeEdge edge = edgeIterator.next();
                edge.autosPermitted = propertyParser.isIntValueInPropertyList(apf.getInt(edge),PROPERTIES_EDGE_AUTOSPERMITTED_VALUES);
                edge.centroidConnector = propertyParser.isIntValueInPropertyList(cf.getInt(edge),PROPERTIES_EDGE_CENTROID_VALUE);
                edge.distance = edge.distance * (float) DISTANCE_CONVERSION_FACTOR;
                edge.bikeCost = (double) edge.distance * (
                                           Double.parseDouble(propertyMap.get(PROPERTIES_COEF_DISTCLA0)) * ( ( edge.bikeClass < 1 ? 1 : 0 ) + ( edge.bikeClass > 3 ? 1 : 0 ) )
                                         + Double.parseDouble(propertyMap.get(PROPERTIES_COEF_DISTCLA1)) * ( edge.bikeClass == 1 ? 1 : 0 )
                                         + Double.parseDouble(propertyMap.get(PROPERTIES_COEF_DISTCLA2)) * ( edge.bikeClass == 2 ? 1 : 0 ) * ( edge.cycleTrack ? 0 : 1 )
                                         + Double.parseDouble(propertyMap.get(PROPERTIES_COEF_DISTCLA3)) * ( edge.bikeClass == 3 ? 1 : 0 ) * ( edge.bikeBlvd ? 0 : 1 )
                                         + Double.parseDouble(propertyMap.get(PROPERTIES_COEF_DARTNE2))  * ( edge.bikeClass != 2 && edge.bikeClass != 1 ? 1 : 0 ) * ( ( edge.functionalClass < 5 && edge.functionalClass > 0 ) ? 1 : 0 )
                                         + Double.parseDouble(propertyMap.get(PROPERTIES_COEF_DWRONGWY)) * ( edge.bikeClass != 1 ? 1 : 0 ) * ( edge.lanes == 0 ? 1 : 0 )
                                         + Double.parseDouble(propertyMap.get(PROPERTIES_COEF_DCYCTRAC)) * ( edge.cycleTrack ? 1 : 0 )
                                         + Double.parseDouble(propertyMap.get(PROPERTIES_COEF_DBIKBLVD)) * ( edge.bikeBlvd ? 1 : 0 )
                                     )
                                     + Double.parseDouble(propertyMap.get(PROPERTIES_COEF_GAIN)) * edge.gain
                                     + INACCESSIBLE_COST_COEF * ( ( edge.functionalClass < 3 && edge.functionalClass > 0 ) ? 1 : 0 );
                edge.walkCost = (double) edge.distance * Double.parseDouble(propertyMap.get(PROPERTIES_COEF_DISTANCE_WALK)) + Double.parseDouble(propertyMap.get(PROPERTIES_COEF_GAIN_WALK)) * edge.gain; 
            }
            
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error( "Exception caught calculating derived edge attributes.", e);
            throw new RuntimeException();
        }
    }

    @Override
    protected void calculateDerivedTraversalAttributes(Network<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal> network)
    {     
        Iterator<SandagBikeTraversal> traversalIterator = network.traversalIterator();
        while ( traversalIterator.hasNext() ) {
        	SandagBikeTraversal t = traversalIterator.next();
            t.turnType = calculateTurnType(t,network);
            t.thruCentroid = t.getFromEdge().centroidConnector && t.getToEdge().centroidConnector;
            boolean signalized = t.getFromEdge().getToNode().signalized;
            boolean fromMajorArt = t.getFromEdge().functionalClass <= 3 && t.getFromEdge().functionalClass > 0 && t.getFromEdge().bikeClass != 1;
            boolean fromMinorArt = t.getFromEdge().functionalClass == 4 && t.getFromEdge().bikeClass != 1;
            t.signalExclRightAndThruJunction = signalized && t.turnType != TurnType.RIGHT && !isThruJunction(t, network);
            t.unsigLeftFromMajorArt = !signalized && fromMajorArt && t.turnType == TurnType.LEFT;
            t.unsigLeftFromMinorArt = !signalized && fromMinorArt && t.turnType == TurnType.LEFT;
            t.unsigCrossMajorArt = !signalized && isCrossingOfMajorArterial(t, network);
            t.unsigCrossMinorArt = !signalized && isCrossingOfMinorArterial(t, network);
            t.cost = Double.parseDouble(propertyMap.get(PROPERTIES_COEF_TURN)) * ( ( t.turnType != TurnType.NONE ) ? 1 : 0 )
                    + INACCESSIBLE_COST_COEF * ( t.thruCentroid ? 1 : 0 )
                    + Double.parseDouble(propertyMap.get(PROPERTIES_COEF_SIGNALS)) * ( t.signalExclRightAndThruJunction ? 1 : 0 )
                    + Double.parseDouble(propertyMap.get(PROPERTIES_COEF_UNLFRMA)) * ( t.unsigLeftFromMajorArt ? 1 : 0 )
                    + Double.parseDouble(propertyMap.get(PROPERTIES_COEF_UNLFRMI)) * ( t.unsigLeftFromMinorArt ? 1 : 0 )
                    + Double.parseDouble(propertyMap.get(PROPERTIES_COEF_UNTOMA)) * ( t.unsigCrossMajorArt ? 1 : 0 )
                    + Double.parseDouble(propertyMap.get(PROPERTIES_COEF_UNTOMI)) * ( t.unsigCrossMinorArt ? 1 : 0 );
        }
    }
    
    private boolean isCrossingOfMajorArterial(SandagBikeTraversal t, Network<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal> network) {
        SandagBikeNode startNode = t.getFromEdge().getFromNode();
        SandagBikeNode thruNode = t.getFromEdge().getToNode();
        SandagBikeNode endNode = t.getToEdge().getToNode();
        
        if (startNode.centroid || thruNode.centroid || endNode.centroid ) {
            return false;
        }
        
        SandagBikeEdge edge = t.getToEdge();
        if (t.turnType == TurnType.LEFT && edge.functionalClass <= 3 && edge.functionalClass > 0 && edge.bikeClass != 1 ) {
            return true;
        }
        
        if ( t.turnType != TurnType.NONE) {
            return false;
        }
        
        int majorArtCount = 0;
        for (SandagBikeNode successor : network.getSuccessors(thruNode) ) {
            edge = network.getEdge(thruNode,successor);
            boolean majorArt = edge.functionalClass <= 3 && edge.functionalClass > 0 && edge.bikeClass != 1;
            if (majorArt && (!(successor.equals(startNode))) && (!(successor.equals(endNode)))) {
                majorArtCount += 1;
            }
        }
        
        return majorArtCount >= 2;
    }
    
    private boolean isCrossingOfMinorArterial(SandagBikeTraversal t, Network<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal> network) {
        if ( isCrossingOfMajorArterial(t, network) ) {
            return false;
        }
        
        SandagBikeNode startNode = t.getFromEdge().getFromNode();
        SandagBikeNode thruNode = t.getFromEdge().getToNode();
        SandagBikeNode endNode = t.getToEdge().getToNode();
        
        if (startNode.centroid || thruNode.centroid || endNode.centroid ) {
            return false;
        }
        
        SandagBikeEdge edge = t.getToEdge();
        if (t.turnType == TurnType.LEFT && edge.functionalClass == 4 && edge.bikeClass != 1 ) {
            return true;
        }
        
        if ( t.turnType != TurnType.NONE) {
            return false;
        }
        
        int artCount = 0;
        for (SandagBikeNode successor : network.getSuccessors(thruNode) ) {
            edge = network.getEdge(thruNode,successor);
            boolean art = edge.functionalClass <= 4 && edge.functionalClass > 0 && edge.bikeClass != 1;
            if (art && (!(successor.equals(startNode))) && (!(successor.equals(endNode)))) {
                artCount += 1;
            }
        }
                  
        return artCount >= 2;
    }
    
    private boolean isThruJunction(SandagBikeTraversal t, Network<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal> network) {
        SandagBikeNode startNode = t.getFromEdge().getFromNode();
        SandagBikeNode thruNode = t.getFromEdge().getToNode();
        SandagBikeNode endNode = t.getToEdge().getToNode();
        
        if (startNode.centroid || thruNode.centroid || endNode.centroid ) {
            return false;
        }
        
        if ( t.turnType != TurnType.NONE) {
            return false;
        }
        
        boolean rightTurnExists = false;
        for (SandagBikeNode successor : network.getSuccessors(thruNode) ) {
            SandagBikeTraversal traversal = network.getTraversal(t.getFromEdge(),network.getEdge(thruNode,successor));
            if ((!(successor.equals(startNode))) && (!(successor.equals(endNode)))) {
                rightTurnExists = traversal.turnType == TurnType.NONE;
            }
        }
        
        return !rightTurnExists;
    }
    
    private double calculateTraversalAngle(SandagBikeTraversal t)
    {    
    	float xDiff1 = t.getFromEdge().getToNode().x - t.getFromEdge().getFromNode().x;
        float xDiff2 = t.getToEdge().getToNode().x - t.getToEdge().getFromNode().x;
    	float yDiff1 = t.getFromEdge().getToNode().y - t.getFromEdge().getFromNode().y;
        float yDiff2 = t.getToEdge().getToNode().y - t.getToEdge().getFromNode().y;
        
        double angle = Math.atan2(yDiff2, xDiff2) - Math.atan2(yDiff1, xDiff1);
        
        if ( angle > Math.PI ) {
            angle = angle - 2 * Math.PI;
        }
        if (angle < - Math.PI ) {
            angle = angle + 2 * Math.PI;
        }
    
        return angle;
    }
    
    private TurnType calculateTurnType(SandagBikeTraversal t, Network<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal> network)
    {        
    	SandagBikeNode startNode = t.getFromEdge().getFromNode();
    	SandagBikeNode thruNode = t.getFromEdge().getToNode();
    	SandagBikeNode endNode = t.getToEdge().getToNode();
        
        if (startNode.centroid || thruNode.centroid || endNode.centroid ) {
            return TurnType.NONE;
        }
        
        if ( startNode.equals(endNode)) {
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
        
        SandagBikeEdge startEdge = network.getEdge(startNode,thruNode);
        for (SandagBikeNode successor : network.getSuccessors(thruNode) ) {
        	SandagBikeEdge edge = network.getEdge(thruNode,successor);
            if (edge.autosPermitted && (!(successor.equals(startNode)))) {
                currentAngle = calculateTraversalAngle(network.getTraversal(startEdge,edge));
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
    
    private void loadNetworkData() {
    	if (nodes == null) {
    		nodes = readNodes();
    		edges = readEdges(nodes);
    	}
    }

	@Override
	protected Collection<SandagBikeNode> getNodes() {
		loadNetworkData();
		return nodes;
	}

	@Override
	protected Collection<SandagBikeEdge> getEdges() {
		loadNetworkData();
		return edges;
	}

	@Override
	protected SandagBikeTraversal getTraversal(SandagBikeEdge fromEdge, SandagBikeEdge toEdge) {
		return new SandagBikeTraversal(fromEdge,toEdge);
	}
}
