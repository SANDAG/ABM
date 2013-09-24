package org.sandag.abm.active;
import java.util.*;
import java.io.*;
import com.pb.common.datafile.*;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.HouseholdChoiceModelRunner;

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
    private static final String PROPERTIES_EDGE_ATTRIBUTES_STORE = "active.edge.attributes.store";
    private static final String PROPERTIES_EDGE_ATTRIBUTES_DISK_AB = "active.edge.attributes.disk.ab";
    private static final String PROPERTIES_EDGE_ATTRIBUTES_DISK_BA = "active.edge.attributes.disk.ba";
    private static final String PROPERTIES_EDGE_CENTROID_ATTRIBUTE = "active.edge.centroid.attribute";
    private static final String PROPERTIES_EDGE_CENTROID_VALUES = "active.edge.centroid.value";
    private static final String PROPERTIES_EDGE_AUTOSPERMITTED_ATTRIBUTE = "active.edge.autospermitted.attribute";
    private static final String PROPERTIES_EDGE_AUTOSPERMITTED_VALUES = "active.edge.autospermitted.values";
    
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
        return network;
    }
    
    private void readNodes()
    {
        try{
            TableDataSet data = (new DBFFileReader()).readFile(new File(propertyMap.get(PROPERTIES_NODE_FILE)));
            HashMap<String,Number> attributes = new HashMap<String,Number>();
            for (int row = 1; row <= data.getRowCount(); row = row+1 ){
                for (String label : data.getColumnLabels()){
                    attributes.put(label, data.getValueAt(row,label));
                }
                network.addNode((int) data.getValueAt(row, propertyMap.get(PROPERTIES_NODE_ID)), attributes);   
            }
        } catch  (IOException e){
            logger.error( "Exception caught reading nodes from disk.", e);
        }    
    }
    
    private void readEdges()
    {
        try{
            TableDataSet data = (new DBFFileReader()).readFile(new File(propertyMap.get(PROPERTIES_EDGE_FILE)));
            HashMap<String,Number> attributes = new HashMap<String,Number>();
            List<String> storeLabels = Arrays.asList(propertyMap.get(PROPERTIES_EDGE_ATTRIBUTES_STORE.split("\\s*,\\s*")));
            List<String> abLabels = Arrays.asList(propertyMap.get(PROPERTIES_EDGE_ATTRIBUTES_DISK_AB.split("\\s*,\\s*")));
            List<String> baLabels = new ArrayList<String>();
            boolean directional = Boolean.parseBoolean(propertyMap.get(PROPERTIES_EDGE_DIRECTIONAL));
            if ( ! directional ) { baLabels = Arrays.asList(propertyMap.get(PROPERTIES_EDGE_ATTRIBUTES_DISK_BA.split("\\s*,\\s*"))); }
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
        } catch  (IOException e){
            logger.error( "Exception caught reading edges from disk.", e);
        }
    }

}
