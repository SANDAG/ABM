package org.sandag.abm.active;
import java.util.*;
import java.io.*;
import com.pb.common.datafile.*;
import org.apache.log4j.Logger;

public class NetworkFactory
{
    private Logger logger = Logger.getLogger(NetworkFactory.class);
    private Network network;
    private HashMap<String,String> propertyMap;
    private static final String PROPERTIES_NODE_FILE = "active.node.file";
    private static final String PROPERTIES_NODE_ID = "active.node.id";
    private static final String PROPERTIES_NODE_XCOORD = "active.node.xcoord";
    private static final String PROPERTIES_NODE_YCOORD = "active.node.ycoord";
    private static final String PROPERTIES_EDGE_FILE = "active.edge.file";
    private static final String PROPERTIES_EDGE_ANODE = "active.edge.anode";
    private static final String PROPERTIES_EDGE_BNODE = "active.edge.bnode";
    private static final String PROPERTIES_EDGE_DIRECTIONAL = "active.edge.directional";
    private static final String PROPERTIES_EDGE_ATTRIBUTES_TARGET = "active.edge.attributes.store";
    private static final String PROPERTIES_EDGE_ATTRIBUTES_SOURCE_AB = "active.edge.attributes.disk.ab";
    private static final String PROPERTIES_EDGE_ATTRIBUTES_SOURCE_BA = "active.edge.attributes.disk.ba";
    private static final String PROPERTIES_EDGE_CENTROID_ATTRIBUTE = "active.edge.centroid.attribute";
    private static final String PROPERTIES_EDGE_CENTROID_VALUE = "active.edge.centroid.value";
    private static final String PROPERTIES_EDGE_AUTOSPERMITTED_ATTRIBUTE_SOURCE = "active.edge.autospermitted.attribute.disk";
    private static final String PROPERTIES_EDGE_AUTOSPERMITTED_ATTRIBUTE_TARGET = "active.edge.autospermitted.attribute.store";
    private static final String PROPERTIES_EDGE_AUTOSPERMITTED_VALUES = "active.edge.autospermitted.values";
    private static final String TRAVERSAL_TURN_TYPE_ATTRIBUTE_NAME = "TraversalType";
    
    public NetworkFactory(HashMap<String, String> propertyMap)
    {
        this.propertyMap = propertyMap;
        network = new Network();
    }
    
    public Network createNetwork()
    {
        network = new Network();
        readNodes();
        readEdges();
        calculateDerivedNodeAttributes();
        calculateDerivedEdgeAttributes();
        calculateDerivedTraversalAttributes();
        return network;
    }
    
    private void readNodes()
    {
        try{
            TableDataSet data = (new DBFFileReader()).readFile(new File(propertyMap.get(PROPERTIES_NODE_FILE)));
            HashMap<String,Number> attributes = new HashMap<String,Number>();
            for (int row = 1; row <= data.getRowCount(); row = row+1 ) {
                for (String label : data.getColumnLabels()) {
                    attributes.put(label, data.getValueAt(row,label));
                }
                network.addNode((int) data.getValueAt(row, propertyMap.get(PROPERTIES_NODE_ID)), attributes);   
            }
        } catch  (IOException e) {
            logger.error( "Exception caught reading nodes from disk.", e);
        }    
    }
    
    private void readEdges()
    {
        try {
            TableDataSet data = (new DBFFileReader()).readFile(new File(propertyMap.get(PROPERTIES_EDGE_FILE)));
            HashMap<String,Number> attributes = new HashMap<String,Number>();
            List<String> storeLabels = parseStringPropertyList(PROPERTIES_EDGE_ATTRIBUTES_TARGET);
            List<String> abLabels = parseStringPropertyList(PROPERTIES_EDGE_ATTRIBUTES_SOURCE_AB);
            List<String> baLabels = new ArrayList<String>();
            boolean directional = Boolean.parseBoolean(propertyMap.get(PROPERTIES_EDGE_DIRECTIONAL));
            if ( ! directional ) { baLabels = parseStringPropertyList(PROPERTIES_EDGE_ATTRIBUTES_SOURCE_BA); }
            for (int row = 1; row <= data.getRowCount(); row = row+1 ){
                for (int i = 0; i < storeLabels.size(); i = i+1 ){
                    attributes.put(storeLabels.get(i), data.getValueAt(row,abLabels.get(i)));
                }            
                network.addEdge((int) data.getValueAt(row, propertyMap.get(PROPERTIES_EDGE_ANODE)), (int) data.getValueAt(row, propertyMap.get(PROPERTIES_EDGE_BNODE)), attributes);
                if ( ! directional ){
                    for (int i = 0; i < storeLabels.size(); i = i+1 ){
                        attributes.put(storeLabels.get(i), data.getValueAt(row,baLabels.get(i)));
                    }            
                    network.addEdge((int) data.getValueAt(row, propertyMap.get(PROPERTIES_EDGE_BNODE)), (int) data.getValueAt(row, propertyMap.get(PROPERTIES_EDGE_ANODE)), attributes); 
                }                
            }
        } catch  (IOException e) {
            logger.error( "Exception caught reading edges from disk.", e);
        }
    }
    
    private void calculateDerivedNodeAttributes() {}
    
    private void calculateDerivedEdgeAttributes()
    {
        Iterator<Edge> edgeIterator = network.edgeIterator();
        String autosPermittedTarget = propertyMap.get(PROPERTIES_EDGE_AUTOSPERMITTED_ATTRIBUTE_TARGET);
        String autosPermittedSource = propertyMap.get(PROPERTIES_EDGE_AUTOSPERMITTED_ATTRIBUTE_SOURCE);
        network.addEdgeAttribute(autosPermittedTarget);
        List<Float> autosPermittedValues = parseFloatPropertyList(PROPERTIES_EDGE_AUTOSPERMITTED_VALUES);
        while ( edgeIterator.hasNext() ) {
            Edge e = edgeIterator.next();
            network.setEdgeAttributeValue(e, autosPermittedTarget, isValueIn(network.getEdgeAttributeValue(e, autosPermittedSource),autosPermittedValues) ? 1 : 0);
        }
    }
    
    private void calculateDerivedTraversalAttributes()
    {
        Iterator<Traversal> traversalIterator = network.traversalIterator();
        network.addTraversalAttribute(TRAVERSAL_TURN_TYPE_ATTRIBUTE_NAME);
        while ( traversalIterator.hasNext() ) {
            Traversal t = traversalIterator.next();
            network.setTraversalAttributeValue(t, TRAVERSAL_TURN_TYPE_ATTRIBUTE_NAME, calculateTurnType(t).getKey());
        }
    }
    
    private float calculateTraversalAngle(Traversal t)
    {
        String xLabel = propertyMap.get(PROPERTIES_NODE_XCOORD);
        String yLabel = propertyMap.get(PROPERTIES_NODE_YCOORD);
        
        float xDiff1 = network.getNodeAttributeValue(t.getFromEdge().getToNode(), xLabel) - network.getNodeAttributeValue(t.getFromEdge().getFromNode(), xLabel);
        float xDiff2 = network.getNodeAttributeValue(t.getToEdge().getToNode(), xLabel) - network.getNodeAttributeValue(t.getToEdge().getFromNode(), xLabel);
        float yDiff1 = network.getNodeAttributeValue(t.getFromEdge().getToNode(), yLabel) - network.getNodeAttributeValue(t.getFromEdge().getFromNode(), yLabel);
        float yDiff2 = network.getNodeAttributeValue(t.getToEdge().getToNode(), yLabel) - network.getNodeAttributeValue(t.getToEdge().getFromNode(), yLabel);
        
        double angle = Math.atan2(yDiff2, xDiff2) - Math.atan2(yDiff1, xDiff1);
        
        if ( angle > Math.PI ) {
            angle = angle - 2 * Math.PI;
        }
        if (angle < - Math.PI ) {
            angle = angle + 2 * Math.PI;
        }
    
        return (float) angle;
    }
    
    private TurnType calculateTurnType(Traversal t)
    {
        Edge fromEdge = t.getFromEdge();
        Edge toEdge = t.getToEdge();
        
        if ( isCentroidConnector(fromEdge) || isCentroidConnector(toEdge) ) {
            return TurnType.NONE;
        }
        
        if ( fromEdge.getFromNode().equals(fromEdge.getToNode()) ) {
            return TurnType.REVERSAL;
        }
        
        Float thisAngle = calculateTraversalAngle(t);
    
        if ( thisAngle < - 5.0 * Math.PI / 6 || thisAngle > 5.0 * Math.PI / 6 ) {
            return TurnType.REVERSAL;
        }
        
        Float minAngle = (float) Math.PI;
        Float maxAngle = (float) -Math.PI;
        Float minAbsAngle = (float) Math.PI;
        int legCount = 0;
        String autosPermittedAttribute = propertyMap.get(PROPERTIES_EDGE_AUTOSPERMITTED_ATTRIBUTE_TARGET);
        
        for (Node successor : network.getSuccessors(fromEdge.getToNode()) ) {
            Edge currentEdge = new Edge(toEdge.getToNode(),successor);
            if ( Float.compare(network.getEdgeAttributeValue(currentEdge, autosPermittedAttribute), 0) == 0 && successor != fromEdge.getFromNode()){
                Float currentAngle = calculateTraversalAngle(new Traversal(fromEdge,currentEdge));
                minAngle = Math.min(minAngle, currentAngle);
                maxAngle = Math.max(maxAngle, currentAngle);
                minAbsAngle = Math.min(Math.abs(minAngle), Math.abs(currentAngle));
                legCount += 1;
            }
        }
        
        if ( legCount <= 2 ) {
            return TurnType.NONE;
        } else if ( legCount == 3) {
            if ( thisAngle <= minAngle && Math.abs(thisAngle) > Math.PI/6 ) {
                return TurnType.RIGHT;
            } else if ( thisAngle >= maxAngle && Math.abs(thisAngle) > Math.PI/6 ) {
                return TurnType.LEFT;
            }
            else {
                return TurnType.NONE;
            }
        } else {
            if ( Math.abs(thisAngle) <= minAbsAngle || ( Math.abs(thisAngle) < Math.PI/6 && thisAngle < minAngle && thisAngle > maxAngle ) ) {
                return TurnType.NONE;
            } else if ( thisAngle < 0 ) {
                return TurnType.RIGHT;
            } else {
            return TurnType.LEFT;            
            }
        }
    }
    
    private List<String> parseStringPropertyList(String property)
    {
        return Arrays.asList(propertyMap.get(property.split("\\s*,\\s*")));
    }
    
    private List<Float> parseFloatPropertyList(String property)
    {
        List<String> stringList = Arrays.asList(propertyMap.get(property.split("\\s*,\\s*")));
        List<Float> floatList = new ArrayList<Float>();
        for (String str: stringList) {
            floatList.add(Float.parseFloat(str));
        }
        return floatList;
    }
    
    private boolean isCentroidConnector(Edge e)
    {
        String centroidAttribute = propertyMap.get(PROPERTIES_EDGE_CENTROID_ATTRIBUTE);
        Float centroidValue = Float.parseFloat(propertyMap.get(PROPERTIES_EDGE_CENTROID_VALUE));
        return centroidValue.equals(network.getEdgeAttributeValue(e,centroidAttribute));
    }
    
    private boolean isValueIn(float value,List<Float> referenceValues)
    {  
        for (Float v : referenceValues) {
            if ( v.equals(value) ) {
                return true;
            }
        }  
        return false;
    }

}
