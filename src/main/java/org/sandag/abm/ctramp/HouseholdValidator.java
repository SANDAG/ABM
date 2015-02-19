package org.sandag.abm.ctramp;

import org.apache.log4j.Logger;
import org.sandag.abm.application.SandagTourBasedModel;

public class HouseholdValidator {
    private static Logger      logger                          = Logger.getLogger(SandagTourBasedModel.class);
	
	public static boolean vailidateHousehold(Household hh){
	    boolean result=true;
    	Person [] persons=hh.getPersons();
    	for(int j=0; j<persons.length; j++){
    		Person person=persons[j];
    		if(person.getPersonIsWorker()==1){
    			int index=person.getWorkLocationSegmentIndex();
    			int mgra=person.getPersonWorkLocationZone();
    			if(index!=-1 && mgra==0){
    				result=false;
    				logger.fatal("Invalid work location choice for "+person.getPersonId()+" a "+person.getAge()+" years old with work segment index "+person.getWorkLocationSegmentIndex());
    				break;
    			}
    		}else if(person.getPersonIsStudent()==1){
    			int index=person.getSchoolLocationSegmentIndex();
    			int mgra=person.getPersonSchoolLocationZone();   
    			if(index!=-1 && mgra==0){
    				result=false;
    				logger.fatal("Invalid school location choice for "+person.getPersonId()+" a "+person.getAge()+" years old with school segment index "+person.getSchoolLocationSegmentIndex());
    				break;
    			}
    		}
    	}	
    	return result;
	}
	
    public static boolean validateHouseholds(Household[] householdArray)
    {
    	boolean result=true;
        for(int i=0; i<householdArray.length; i++){
        	if(!vailidateHousehold(householdArray[i])){
        		result=false;
        		break;
        	}
        }
        return result;
    }
}
