package org.sandag.abm.active;

import java.util.Arrays;
import java.util.HashMap;

import org.sandag.abm.ctramp.Person;
import org.sandag.abm.ctramp.Tour;

import com.pb.common.newmodel.ChoiceModelApplication;

//note this class is not thread safe! - use a ThreadLocal or something to isolate it amongst threads
public class PathChoiceModel {
	public static final String PATH_CHOICE_MODEL_UEC_SPREADSHEET_PROPERTY = "path.choice.uec.spreadsheet";
	public static final String PATH_CHOICE_MODEL_UEC_MODEL_SHEET_PROPERTY = "path.choice.uec.model.sheet";
	public static final String PATH_CHOICE_MODEL_UEC_DATA_SHEET_PROPERTY = "path.choice.uec.data.sheet";
	public static final String PATH_CHOICE_MODEL_MAX_PATH_COUNT_PROPERTY = "path.choice.max.path.count";
	
	private final ChoiceModelApplication model;
	private final PathChoiceDmu dmu;
	private final int maxPathCount;
	private final boolean[] pathAltsAvailable;
	private final int[] pathAltsSample;
	
	public PathChoiceModel(HashMap<String,String> propertyMap) {
		String uecSpreadsheet = propertyMap.get(PATH_CHOICE_MODEL_UEC_SPREADSHEET_PROPERTY);
		int modelSheet = Integer.parseInt(propertyMap.get(PATH_CHOICE_MODEL_UEC_MODEL_SHEET_PROPERTY));
		int dataSheet = Integer.parseInt(propertyMap.get(PATH_CHOICE_MODEL_UEC_DATA_SHEET_PROPERTY));
		dmu = new PathChoiceDmu();
		model = new ChoiceModelApplication(uecSpreadsheet,modelSheet,dataSheet, propertyMap,dmu);
		
		maxPathCount = Integer.parseInt(propertyMap.get(PATH_CHOICE_MODEL_MAX_PATH_COUNT_PROPERTY));
		pathAltsAvailable = new boolean[maxPathCount+1];
		pathAltsSample = new int[maxPathCount+1];
	}
	

	public double[] getPathProbabilities(Person person, PathAlternatives paths, boolean inboundTrip, Tour tour) {
		applyPathChoiceModel(person,paths,inboundTrip,tour);
        double[] probabilities = model.getProbabilities();
        
        double[] pathProbabilities = new double[paths.getPathCount()];
        System.arraycopy(probabilities,0,pathProbabilities,0,pathProbabilities.length);
        return pathProbabilities;
		
	}
	
	public double getPathLogsums(Person person, PathAlternatives paths, boolean inboundTrip, Tour tour) {
		applyPathChoiceModel(person,paths,inboundTrip,tour);
		return model.getLogsum();
	}

    private void applyPathChoiceModel(Person person, PathAlternatives paths, boolean inboundTrip, Tour tour) {
    	dmu.setPersonIsFemale(person.getPersonIsFemale() == 1);
    	dmu.setTourPurpose(tour.getTourPrimaryPurposeIndex());
    	dmu.setIsInboundTrip(inboundTrip);
    	dmu.setPathAlternatives(paths);
    	
    	Arrays.fill(pathAltsAvailable,false);
        Arrays.fill(pathAltsSample,0);
        
        for (int i = 1; i <= paths.getPathCount(); i++) {
        	pathAltsAvailable[i] = true;
        	pathAltsSample[i] = 1;
        }
        
        model.computeUtilities(dmu,dmu.getDmuIndexValues(),pathAltsAvailable,pathAltsSample);
    }

}
