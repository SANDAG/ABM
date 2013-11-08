package org.sandag.abm.active.sandag;

import java.util.Arrays;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.Person;
import org.sandag.abm.ctramp.Tour;
import com.pb.common.newmodel.ChoiceModelApplication;

//note this class is not thread safe! - use a ThreadLocal or something to isolate it amongst threads
public class SandagBikePathChoiceModel {
	public static final String PATH_CHOICE_MODEL_UEC_SPREADSHEET_PROPERTY = "path.choice.uec.spreadsheet";
	public static final String PATH_CHOICE_MODEL_UEC_MODEL_SHEET_PROPERTY = "path.choice.uec.model.sheet";
	public static final String PATH_CHOICE_MODEL_UEC_DATA_SHEET_PROPERTY = "path.choice.uec.data.sheet";
	public static final String PATH_CHOICE_MODEL_MAX_PATH_COUNT_PROPERTY = "path.choice.max.path.count";
	
	public static final String PATH_CHOICE_ALTS_ID_TOKEN_PROPERTY = "path.alts.id.token";
	public static final String PATH_CHOICE_ALTS_FILE_PROPERTY = "path.alts.file";
	public static final String PATH_CHOICE_ALTS_LINK_FILE_PROPERTY = "path.alts.link.file";
	
	private static final Logger logger = Logger.getLogger(SandagBikePathChoiceModel.class);
	
	private final ThreadLocal<ChoiceModelApplication> model;
	private final ThreadLocal<SandagBikePathChoiceDmu> dmu;
	private final int maxPathCount;
	private final ThreadLocal<boolean[]> pathAltsAvailable;
	private final ThreadLocal<int[]> pathAltsSample;
	
	public SandagBikePathChoiceModel(final HashMap<String,String> propertyMap) {
		final String uecSpreadsheet = propertyMap.get(PATH_CHOICE_MODEL_UEC_SPREADSHEET_PROPERTY);
		final int modelSheet = Integer.parseInt(propertyMap.get(PATH_CHOICE_MODEL_UEC_MODEL_SHEET_PROPERTY));
		final int dataSheet = Integer.parseInt(propertyMap.get(PATH_CHOICE_MODEL_UEC_DATA_SHEET_PROPERTY));

		dmu = new ThreadLocal<SandagBikePathChoiceDmu>() {
			@Override
			protected SandagBikePathChoiceDmu initialValue() {
				return new SandagBikePathChoiceDmu();
			}
		};
		model = new ThreadLocal<ChoiceModelApplication>() {
			@Override
			protected ChoiceModelApplication initialValue() {
				return new ChoiceModelApplication(uecSpreadsheet,modelSheet,dataSheet,propertyMap,dmu.get());
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
	

	public double[] getPathProbabilities(Person person, SandagBikePathAlternatives paths, boolean inboundTrip, Tour tour, boolean debug) {
		applyPathChoiceModel(person,paths,inboundTrip,tour);
        double[] probabilities = Arrays.copyOf(model.get().getProbabilities(),paths.getPathCount());
        if (debug)
        	debugPathChoiceModel(person,paths,inboundTrip,tour,probabilities);
        return probabilities;
		
	}
	
	public double getPathLogsums(Person person, SandagBikePathAlternatives paths, boolean inboundTrip, Tour tour) {
		applyPathChoiceModel(person,paths,inboundTrip,tour);
		return model.get().getLogsum();
	}

    private void applyPathChoiceModel(Person person, SandagBikePathAlternatives paths, boolean inboundTrip, Tour tour) {
    	SandagBikePathChoiceDmu dmu = this.dmu.get();
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
    
    private void debugPathChoiceModel(Person person, SandagBikePathAlternatives paths, boolean inboundTrip, Tour tour, double[] probabilities) {
    	//want: person id, tour id, inbound trip, path count, path probabilities 
    	logger.info(String.format("debug of path choice model for person id %s, tour id %s, inbound trip: %s, path alternative count: %s",
    			                  person.getPersonId(),tour.getTourId(),inboundTrip,paths.getPathCount()));
    	logger.info("    path probabilities: " + Arrays.toString(probabilities));
    }

}
