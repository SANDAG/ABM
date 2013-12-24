package org.sandag.abm.active.sandag;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.apache.log4j.Logger;
import org.sandag.abm.active.AbstractPathChoiceLogsumMatrixApplication;
import org.sandag.abm.active.Network;
import org.sandag.abm.active.NodePair;
import org.sandag.abm.active.PathAlternativeList;
import org.sandag.abm.active.PathAlternativeListGenerationConfiguration;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.BikeLogsum;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.Person;
import org.sandag.abm.ctramp.Tour;
import java.text.DecimalFormat;

import com.pb.common.util.ResourceUtil;

public class SandagBikePathChoiceLogsumMatrixApplication extends AbstractPathChoiceLogsumMatrixApplication<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal>
{
	private static final Logger logger = Logger.getLogger(SandagBikePathChoiceLogsumMatrixApplication.class);

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
    	if (args.length == 0) {
            logger.error( String.format("no properties file base name (without .properties extension) was specified as an argument.") );
            return;
        }
    	
        //String RESOURCE_BUNDLE_NAME = "sandag_abm_active_test";
    	@SuppressWarnings("unchecked") //this is ok - the map will be String->String
        Map<String,String> propertyMap = (Map<String,String>) ResourceUtil.getResourceBundleAsHashMap (args[0]);
        
        SandagBikeNetworkFactory factory = new SandagBikeNetworkFactory(propertyMap);
        Network<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> network = factory.createNetwork();
        
        //order matters, taz first, then mgra, so use linked hash map
        Map<PathAlternativeListGenerationConfiguration<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal>,String> configurationOutputMap = new LinkedHashMap<>();
        configurationOutputMap.put(new SandagBikeTazPathAlternativeListGenerationConfiguration(propertyMap,network),
        		                   propertyMap.get(BikeLogsum.BIKE_LOGSUM_TAZ_FILE_PROPERTY));
        configurationOutputMap.put(new SandagBikeMgraPathAlternativeListGenerationConfiguration(propertyMap,network),
                                   propertyMap.get(BikeLogsum.BIKE_LOGSUM_MGRA_FILE_PROPERTY));

        for (PathAlternativeListGenerationConfiguration<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal> configuration : configurationOutputMap.keySet()) {
        	Path outputDirectory = Paths.get(configuration.getOutputDirectory()); 
            Path outputFile = outputDirectory.resolve(configurationOutputMap.get(configuration));
            SandagBikePathChoiceLogsumMatrixApplication application = new SandagBikePathChoiceLogsumMatrixApplication(configuration,propertyMap);

            Map<Integer,Integer> origins = configuration.getInverseOriginZonalCentroidIdMap(); 
            Map<Integer,Integer> dests = configuration.getInverseDestinationZonalCentroidIdMap();
            
            try {
            	Files.createDirectories(outputDirectory);
            } catch (IOException e) {
            	throw new RuntimeException(e);
            }
            
            Map<NodePair<SandagBikeNode>,double[]> logsums = application.calculateMarketSegmentLogsums();
            Map<Integer,Integer> centroids = configuration.getInverseOriginZonalCentroidIdMap();
            
            DecimalFormat formatter = new DecimalFormat("#.###");

            try (PrintWriter writer = new PrintWriter(outputFile.toFile())) {
            	StringBuilder sb = new StringBuilder("i,j");
            	for (String segment : MARKET_SEGMENT_NAMES)
            		sb.append(",").append(segment);
            	writer.println(sb.toString());
            	for (NodePair<SandagBikeNode> od : logsums.keySet()) {
            		sb = new StringBuilder();
            		sb.append(origins.get(od.getFromNode().getId())).append(",").append(dests.get(od.getToNode().getId()));
            		for (double logsum : logsums.get(od))
            			sb.append(",").append(formatter.format(logsum));
            		writer.println(sb.toString());
            	}
            } catch (IOException e) {
            	throw new RuntimeException(e);
			}
        }
    }
}
