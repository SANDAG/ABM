package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.Tour;
import org.sandag.abm.ctramp.Person;
import org.sandag.abm.ctramp.Household;
import org.sandag.abm.ctramp.Stop;


public class TourDriverDMU
    implements Serializable, VariableTable
{
    protected transient Logger logger = Logger.getLogger(TourDriverDMU.class);

    protected HashMap<String, Integer> methodIndexMap;
    
    protected Tour tour;
    protected Person person;
    protected Household hh;
    
    protected IndexValues dmuIndex;
    private ModelStructure modelStructure;	
    
    public TourDriverDMU(ModelStructure modelStructure ){
        this.modelStructure = modelStructure;
        setupMethodIndexMap();
    	dmuIndex = new IndexValues();
    }
 
    private void setupMethodIndexMap() {
        methodIndexMap = new HashMap<String, Integer>();
        
        methodIndexMap.put("getSize", 0);
        methodIndexMap.put("getNumAdults", 1);
        methodIndexMap.put("getAutos", 2);
        methodIndexMap.put("getAge", 3);
        methodIndexMap.put("getPersonIsWorker", 4);
        methodIndexMap.put("getPersonIsMale", 5);
        methodIndexMap.put("getPersonIsStudentDriving", 6);
        methodIndexMap.put("getTourMode", 7);
        methodIndexMap.put("getTourPurposeIsSchool", 8);
        methodIndexMap.put("getTourPurposeIsMaintenance", 9);
        methodIndexMap.put("getTourPurposeIsDiscretionary", 10);
        methodIndexMap.put("getTourHasEscortStops", 11);
        methodIndexMap.put("getPersonIsUniversityStudent", 12);
        methodIndexMap.put("getTourPurposeIsEscorting", 13);

    }
    
    public double getValueForIndex(int variableIndex, int arrayIndex) {

        switch ( variableIndex ){
            case 0: return getSize();
            case 1: return getNumAdults();
            case 2: return getAutos();
            case 3: return getAge();
            case 4: return getPersonIsWorker();
            case 5: return getPersonIsMale();
            case 6: return getPersonIsStudentDriving();
            case 7: return getTourMode();
            case 8: return getTourPurposeIsSchool();
            case 9: return getTourPurposeIsMaintenance();
            case 10: return getTourPurposeIsDiscretionary();
            case 11: return getTourHasEscortStops();
            case 12: return getPersonIsUniversityStudent();
            case 13: return getTourPurposeIsEscorting();

            default:
                logger.error("method number = "+variableIndex+" not found");
                throw new RuntimeException("method number = "+variableIndex+" not found");        
        }
    }
    
    public Household getHouseholdObject() {
        return hh;
    }
    
    public Tour getTourObject() {
        return tour;
    }
    
    public void setHouseholdObject ( Household hhObject ) {
        hh = hhObject;
    }
    
    public void setPersonObject ( Person personObject ) {
        person = personObject;
    }
    
    public void setTourObject ( Tour tourObject ) {
        tour = tourObject;
    }
    
    public int getSize() {
        return hh.getSize();
    }
    
    public int getNumAdults() {
        int num = 0;
        Person[] persons = hh.getPersons();
        for ( int i=1; i < persons.length; i++ )
            num += persons[i].getPersonIsAdult();
        return num;
    }
    
    public int getAutos() {
        return hh.getAutoOwnershipModelResult();
    }

    public int getAge() {
        return person.getAge();
    }
    
    public int getPersonIsWorker() {
        return person.getPersonIsWorker();
    }
    
    public int getPersonIsMale() {
        if ( person.getPersonIsMale() == 1 )
            return 1;
        else
            return 0;
    }
    
    public int getPersonIsStudentDriving() {
        return tour.getPersonObject().getPersonIsStudentDriving();
    }
    
    public int getPersonIsUniversityStudent() {
    	return tour.getPersonObject().getPersonIsUniversityStudent();
    }
    
    public int getTourMode() {
        return tour.getTourModeChoice();
    }
    
    public int getTourPurposeIsSchool() {
        return tour.getTourPrimaryPurpose().equalsIgnoreCase( modelStructure.SCHOOL_PRIMARY_PURPOSE_NAME ) ? 1 : 0;
    }
    
    public int getTourPurposeIsMaintenance() {
    	int maintTour = tour.getTourPrimaryPurpose().equalsIgnoreCase( modelStructure.OTH_MAINT_PRIMARY_PURPOSE_NAME ) ? 1 : 0;
    	int shopTour = tour.getTourPrimaryPurpose().equalsIgnoreCase( modelStructure.SHOPPING_PURPOSE_NAME ) ? 1 : 0;
    	maintTour = maintTour + shopTour;
        return maintTour;
    }
    
    public int getTourPurposeIsDiscretionary() {
        int discTour = tour.getTourPrimaryPurpose().equalsIgnoreCase( modelStructure.OTH_DISCR_PURPOSE_NAME ) ? 1 : 0;
        int visitTour = tour.getTourPrimaryPurpose().equalsIgnoreCase( modelStructure.SOCIAL_PURPOSE_NAME ) ? 1 : 0;
        int eatTour = tour.getTourPrimaryPurpose().equalsIgnoreCase( modelStructure.EAT_OUT_PURPOSE_NAME ) ? 1 : 0;
        discTour = discTour + visitTour + eatTour;
        return discTour;
    }
    
    public int getTourPurposeIsEscorting() {
    	return tour.getTourPrimaryPurpose().equalsIgnoreCase(modelStructure.ESCORT_PURPOSE_NAME ) ? 1 : 0;
    }
    
    public int getTourHasEscortStops() {
    	int hasEscortStops = 0;
    	
        Stop[] stops = tour.getInboundStops();
        if ( stops != null) {
        	for ( int i=0; i < stops.length; i++ ) {
            	if (stops[i].getDestPurpose().equalsIgnoreCase(modelStructure.ESCORT_PURPOSE_NAME)) {
            		hasEscortStops = 1;
            	}
            }
        }
        
        if ( stops != null) {
        	stops = tour.getOutboundStops();
            for ( int i=0; i < stops.length; i++ ) {
            	if (stops[i].getDestPurpose().equalsIgnoreCase(modelStructure.ESCORT_PURPOSE_NAME)) {
            		hasEscortStops = 1;
            	}
            }	
        }
        
        return hasEscortStops;
    }
       
    public IndexValues getDmuIndexValues() {
        return dmuIndex; 
    }

    public int getIndexValue(String variableName) {
        return methodIndexMap.get(variableName);
    }

    public int getAssignmentIndexValue(String variableName) {
        throw new UnsupportedOperationException();
    }

    public double getValueForIndex(int variableIndex) {
        throw new UnsupportedOperationException();
    }

    public void setValue(String variableName, double variableValue) {
        throw new UnsupportedOperationException();
    }

    public void setValue(int variableIndex, double variableValue) {
        throw new UnsupportedOperationException();
    }
    
}
