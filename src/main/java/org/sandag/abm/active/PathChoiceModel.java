package org.sandag.abm.active;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.sandag.abm.active.sandag.SandagBikeEdge;
import org.sandag.abm.active.sandag.SandagBikeNetworkFactory;
import org.sandag.abm.active.sandag.SandagBikeNode;
import org.sandag.abm.active.sandag.SandagBikeTazPathAlternativeListGenerationConfiguration;
import org.sandag.abm.active.sandag.SandagBikeTraversal;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.Person;
import org.sandag.abm.ctramp.Tour;

import com.pb.common.newmodel.ChoiceModelApplication;

//note this class is not thread safe! - use a ThreadLocal or something to isolate it amongst threads
public class PathChoiceModel {
	public static final String PATH_CHOICE_MODEL_UEC_SPREADSHEET_PROPERTY = "path.choice.uec.spreadsheet";
	public static final String PATH_CHOICE_MODEL_UEC_MODEL_SHEET_PROPERTY = "path.choice.uec.model.sheet";
	public static final String PATH_CHOICE_MODEL_UEC_DATA_SHEET_PROPERTY = "path.choice.uec.data.sheet";
	public static final String PATH_CHOICE_MODEL_MAX_PATH_COUNT_PROPERTY = "path.choice.max.path.count";
	
	public static final String PATH_CHOICE_ALTS_ID_TOKEN_PROPERTY = "path.alts.id.token";
	public static final String PATH_CHOICE_ALTS_FILE_PROPERTY = "path.alts.file";
	public static final String PATH_CHOICE_ALTS_LINK_FILE_PROPERTY = "path.alts.link.file";
	
	private static final Logger logger = Logger.getLogger(PathChoiceModel.class);
	
	private final ThreadLocal<ChoiceModelApplication> model;
	private final ThreadLocal<PathChoiceDmu> dmu;
	private final int maxPathCount;
	private final ThreadLocal<boolean[]> pathAltsAvailable;
	private final ThreadLocal<int[]> pathAltsSample;
	
	public PathChoiceModel(final Map<String,String> propertyMap) {
		final String uecSpreadsheet = propertyMap.get(PATH_CHOICE_MODEL_UEC_SPREADSHEET_PROPERTY);
		final int modelSheet = Integer.parseInt(propertyMap.get(PATH_CHOICE_MODEL_UEC_MODEL_SHEET_PROPERTY));
		final int dataSheet = Integer.parseInt(propertyMap.get(PATH_CHOICE_MODEL_UEC_DATA_SHEET_PROPERTY));
		final ThreadLocal<HashMap<String,String>> threadPropertyMap =  new ThreadLocal<HashMap<String,String>>() {
			@Override
			protected HashMap<String,String> initialValue() {
				HashMap<String,String> map = new HashMap<>(propertyMap);
				map.put(PATH_CHOICE_ALTS_FILE_PROPERTY,getPathAltsFile(propertyMap));
				map.put(PATH_CHOICE_ALTS_LINK_FILE_PROPERTY,getPathAltsLinkFile(propertyMap));
				return map;
			}
		};
		dmu = new ThreadLocal<PathChoiceDmu>() {
			@Override
			protected PathChoiceDmu initialValue() {
				return new PathChoiceDmu();
			}
		};
		model = new ThreadLocal<ChoiceModelApplication>() {
			@Override
			protected ChoiceModelApplication initialValue() {
				return new ChoiceModelApplication(uecSpreadsheet,modelSheet,dataSheet,threadPropertyMap.get(),dmu.get());
			}
		};
		
		maxPathCount = Integer.parseInt(propertyMap.get(PATH_CHOICE_MODEL_MAX_PATH_COUNT_PROPERTY));
		pathAltsAvailable = new ThreadLocal<boolean[]>() {
			@Override
			protected boolean[] initialValue() {
				return new boolean[maxPathCount+1];
			}
		};
		pathAltsSample = new ThreadLocal<int[]>() {
			@Override
			protected int[] initialValue() {
				return new int[maxPathCount+1];
			}
		};			
	}
	

	private static String getThreadSpecificPathFile(Map<String,String> propertyMap, String property) {
		return propertyMap.get(property).replace(propertyMap.get(PATH_CHOICE_ALTS_ID_TOKEN_PROPERTY),"" + Thread.currentThread().getId());
	}
	
	public static String getPathAltsFile(Map<String,String> propertyMap) {
		//note this is thread-specific!
		return getThreadSpecificPathFile(propertyMap,PATH_CHOICE_ALTS_FILE_PROPERTY);
	}
	
	public static String getPathAltsLinkFile(Map<String,String> propertyMap) {
		//note this is thread-specific!
		return getThreadSpecificPathFile(propertyMap,PATH_CHOICE_ALTS_LINK_FILE_PROPERTY);
	}
	

	public double[] getPathProbabilities(Person person, PathAlternatives paths, boolean inboundTrip, Tour tour, boolean debug) {
		applyPathChoiceModel(person,paths,inboundTrip,tour);
        double[] probabilities = Arrays.copyOf(model.get().getProbabilities(),paths.getPathCount());
        if (debug)
        	debugPathChoiceModel(person,paths,inboundTrip,tour,probabilities);
        return probabilities;
		
	}
	
	public double getPathLogsums(Person person, PathAlternatives paths, boolean inboundTrip, Tour tour) {
		applyPathChoiceModel(person,paths,inboundTrip,tour);
		return model.get().getLogsum();
	}

    private void applyPathChoiceModel(Person person, PathAlternatives paths, boolean inboundTrip, Tour tour) {
    	PathChoiceDmu dmu = this.dmu.get();
    	dmu.setPersonIsFemale(person.getPersonIsFemale() == 1);
    	dmu.setTourPurpose(tour.getTourPrimaryPurposeIndex());
    	dmu.setIsInboundTrip(inboundTrip);
    	dmu.setPathAlternatives(paths);
    	
    	boolean[] pathAltsAvailable = this.pathAltsAvailable.get();
    	int[] pathAltsSample = this.pathAltsSample.get();
    	Arrays.fill(pathAltsAvailable,false);
        Arrays.fill(pathAltsSample,0);
        
        for (int i = 1; i <= paths.getPathCount(); i++) {
        	pathAltsAvailable[i] = true;
        	pathAltsSample[i] = 1;
        }
        
        model.get().computeUtilities(dmu,dmu.getDmuIndexValues(),pathAltsAvailable,pathAltsSample);
    }
    
    private void debugPathChoiceModel(Person person, PathAlternatives paths, boolean inboundTrip, Tour tour, double[] probabilities) {
    	//want: person id, tour id, inbound trip, path count, path probabilities 
    	logger.info(String.format("debug of path choice model for person id %s, tour id %s, inbound trip: %s, path alternative count: %s",
    			                  person.getPersonId(),tour.getTourId(),inboundTrip,paths.getPathCount()));
    	logger.info("    path probabilities: " + Arrays.toString(probabilities));
    }
    
    public static void main(String ... args) {
    	//PathAlternativeList pal = new PathAlternativeList<>(odPair, network, lengthEvaluator)
    			
        String RESOURCE_BUNDLE_NAME = "sandag_abm_active_test";
        Map<String,String> propertyMap = new HashMap<String,String>();
        SandagBikeNetworkFactory factory;
        Network<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> network;
        PathAlternativeListGenerationConfiguration<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> configuration;
        PathAlternativeListGenerationApplication<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> application;
        ResourceBundle rb = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME);
        propertyMap = new HashMap<>();
        Enumeration<String> keys = rb.getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            propertyMap.put(key, rb.getString(key));
        }
        factory = new SandagBikeNetworkFactory(propertyMap);
        network = factory.createNetwork();
        configuration = new SandagBikeTazPathAlternativeListGenerationConfiguration(propertyMap, network);
        application =  new PathAlternativeListGenerationApplication<>(configuration);
        Map<NodePair<SandagBikeNode>,PathAlternativeList<SandagBikeNode,SandagBikeEdge>> pathAlternatives = application.generateAlternativeLists();
        System.out.println("done, got " + pathAlternatives.size() + " node pairs");
        
        NodePair<SandagBikeNode> np = pathAlternatives.keySet().iterator().next(); //just pick "first" od pair
        
        String pathAltFile = PathChoiceModel.getPathAltsFile(propertyMap);
        String pathAltLinksFile = PathChoiceModel.getPathAltsLinkFile(propertyMap);
        try (PathAlternativeListWriter<SandagBikeNode,SandagBikeEdge> writer = new PathAlternativeListWriter<>(pathAltFile,pathAltLinksFile)) {
        	writer.writeHeaders();
        	writer.write(pathAlternatives.get(np));
        } catch (IOException e) {
			throw new RuntimeException(e);
		}
        
        EvaluationPathAlternatives<SandagBikeNode,SandagBikeEdge> epa = new EvaluationPathAlternatives<>(pathAlternatives.get(np));
        Person person = new Person(null,1,new SandagModelStructure());
        person.setPersGender(2);
        Tour tour = new Tour(person,1,1);
        
        PathChoiceModel pcModel = new PathChoiceModel((HashMap<String,String>) propertyMap);
        System.out.println(pcModel.getPathLogsums(person,epa,true,tour));
        System.out.println(Arrays.toString(pcModel.getPathProbabilities(person,epa,true,tour,true)));
        
        try {
        	Files.delete(Paths.get(pathAltFile));
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }
        try {
        	Files.delete(Paths.get(pathAltLinksFile));
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }
    }

}
