package org.sandag.abm.active.sandag;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import org.sandag.abm.active.AbstractPathChoiceLogsumMatrixApplication;
import org.sandag.abm.active.Network;
import org.sandag.abm.active.NodePair;
import org.sandag.abm.active.PathAlternativeList;
import org.sandag.abm.active.PathAlternativeListGenerationConfiguration;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.Person;
import org.sandag.abm.ctramp.Tour;

public class SandagBikePathChoiceLogsumMatrixApplication extends AbstractPathChoiceLogsumMatrixApplication<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal>
{

    private static final String[] MARKET_SEGMENT_NAMES = {"MaleMandatoryOutbound", "MaleMandatoryInbound", "MaleOther", "FemaleMandatoryOutbound", "FemaleMandatoryInbound", "FemaleOther"};
    private static final int[] MARKET_SEGMENT_GENDER_VALUES = {1,1,1,2,2,2};
    private static final int[] MARKET_SEGMENT_TOUR_PURPOSE_INDICES = {1,1,4,1,1,4};
    private static final boolean[] MARKET_SEGMENT_INBOUND_TRIP_VALUES = {false,true,false,false,true,false};
    
    private ThreadLocal<SandagBikePathChoiceModel> model;
    private Person[] persons;
    private Tour[] tours;
    
    public SandagBikePathChoiceLogsumMatrixApplication(PathAlternativeListGenerationConfiguration<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal> configuration, 
    		                                           final Map<String,String> propertyMap)
    {
        super(configuration);
        model = new ThreadLocal<SandagBikePathChoiceModel>() {
        	@Override
        	protected SandagBikePathChoiceModel initialValue() {
        		return new SandagBikePathChoiceModel((HashMap<String,String>) propertyMap);
        	}
        };
        persons = new Person[MARKET_SEGMENT_NAMES.length];
        tours = new Tour[MARKET_SEGMENT_NAMES.length];
        
        //for dummy person
        SandagModelStructure modelStructure = new SandagModelStructure();
        for (int i=0; i<MARKET_SEGMENT_NAMES.length; i++) {
            persons[i] = new Person(null,1,modelStructure);
            persons[i].setPersGender(MARKET_SEGMENT_GENDER_VALUES[i]);
            tours[i] = new Tour(persons[i],1,MARKET_SEGMENT_TOUR_PURPOSE_INDICES[i]);
        }
    }

    @Override
    protected double[] calculateMarketSegmentLogsums(PathAlternativeList<SandagBikeNode, SandagBikeEdge> alternativeList)
    {
        SandagBikePathAlternatives alts = new SandagBikePathAlternatives(alternativeList);
        double[] logsums = new double[MARKET_SEGMENT_NAMES.length]; 
        for (int i=0; i<MARKET_SEGMENT_NAMES.length; i++) {
            logsums[i] = model.get().getPathLogsums(persons[i], alts, MARKET_SEGMENT_INBOUND_TRIP_VALUES[i], tours[i]);
        }
        return logsums;    
    }
    
    public static void main(String ... args) {
        String RESOURCE_BUNDLE_NAME = "sandag_abm_active_test";
        Map<String,String> propertyMap = new HashMap<String,String>();
        SandagBikeNetworkFactory factory;
        Network<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> network;
        List<PathAlternativeListGenerationConfiguration<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal>> configurations = new ArrayList<>();
        SandagBikePathChoiceLogsumMatrixApplication application;
        
        ResourceBundle rb = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME);
        propertyMap = new HashMap<>();
        Enumeration<String> keys = rb.getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            propertyMap.put(key, rb.getString(key));
        }
        factory = new SandagBikeNetworkFactory(propertyMap);
        network = factory.createNetwork();

        configurations.add(new SandagBikeTazPathAlternativeListGenerationConfiguration(propertyMap, network));
        configurations.add(new SandagBikeMgraPathAlternativeListGenerationConfiguration(propertyMap, network));
        String[] fileProperties = new String[] {"active.logsum.matrix.file.bike.taz", "active.logsum.matrix.file.bike.mgra"};

        for(int i=0; i<configurations.size(); i++)  {
            PathAlternativeListGenerationConfiguration<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> configuration  = configurations.get(i);
            String filename = propertyMap.get(configuration.getOutputDirectory() + fileProperties[i]);
            application = new SandagBikePathChoiceLogsumMatrixApplication(configuration,propertyMap);
            
            new File(configuration.getOutputDirectory()).mkdirs();
            
            Map<NodePair<SandagBikeNode>,double[]> logsums = application.calculateMarketSegmentLogsums();
            Map<Integer,Integer> centroids = configuration.getInverseZonalCentroidIdMap();
            
            try
            {
                FileWriter writer = new FileWriter(new File(filename));
                writer.write("i, j, " + Arrays.toString(MARKET_SEGMENT_NAMES).substring(1).replaceFirst("]", "") + "\n");
                for (NodePair<SandagBikeNode> od : logsums.keySet()) {
                    writer.write(centroids.get(od.getFromNode().getId()) + ", " + centroids.get(od.getToNode().getId()) + ", " + Arrays.toString(logsums.get(od)).substring(1).replaceFirst("]", "") + "\n" );
                }
                writer.flush();
                writer.close();  
            } catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
        }

    }
    
}
