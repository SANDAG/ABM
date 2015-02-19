package org.sandag.abm.ctramp;

import org.apache.log4j.Logger;
import org.sandag.abm.application.SandagTourBasedModel;

public class HouseholdValidator {
    private static Logger      logger                          = Logger.getLogger(SandagTourBasedModel.class);
	
<<<<<<< HEAD
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
=======
	public static boolean vailidateWorkLocChoices(Household hh){
	    boolean result=true;
    	Person [] persons=hh.getPersons();
    	for(int j=0; j<persons.length; j++){
			int windex=persons[j].getWorkLocationSegmentIndex();
			int wmgra=persons[j].getPersonWorkLocationZone(); 
    		if(windex!=-1 && wmgra==0){
    			result=false;
    			logger.info("Invalid work location choice for "+persons[j].getPersonId()+" a "+persons[j].getAge()+" years old with work segment index "+persons[j].getWorkLocationSegmentIndex()+" resubmitting job......");
    			break;
    		}  
    	}
    	return result;
	}
	
	public static boolean vailidateSchoolLocChoices(Household hh){
	    boolean result=true;
    	Person [] persons=hh.getPersons();
    	for(int j=0; j<persons.length; j++){
			int sindex=persons[j].getSchoolLocationSegmentIndex();
			int smgra=persons[j].getPersonSchoolLocationZone();    
    		if(sindex!=-1 && smgra==0){
    			result=false;
    			logger.fatal("Invalid school location choice for "+persons[j].getPersonId()+" a "+persons[j].getAge()+" years old with school segment index "+persons[j].getSchoolLocationSegmentIndex()+" resubmitting job.....");
    			break;
    		}
    	}
    	return result;
	}
	
    public static boolean validateMandatoryDestinationChoices(Household[] householdArray, String type )
    {
    	boolean result=true;
    	if(type.equalsIgnoreCase("work")){
	        for(int i=0; i<householdArray.length; i++){
		        if(!vailidateWorkLocChoices(householdArray[i])){
		        	result=false;
		        	break;
		        }
	        }
    	}else if(type.equalsIgnoreCase("school")){
	        for(int i=0; i<householdArray.length; i++){
	        	if(!vailidateSchoolLocChoices(householdArray[i])){
	        		result=false;
	        		break;
	        	}
	        }
        }else{
        	logger.fatal("invalide mandatory destination choice type:"+type);
>>>>>>> (ABM-592) Add household validator to check work and school loction
        }
        return result;
    }
}
