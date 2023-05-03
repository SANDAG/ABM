package org.sandag.abm.utilities;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.Household;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.Person;
import org.sandag.abm.ctramp.Tour;

import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;

public class ModelOutputReader {

    private transient Logger    logger                          = Logger.getLogger(ModelOutputReader.class);

    private static final String PROPERTIES_HOUSEHOLD_DATA_FILE  = "Accessibilities.HouseholdDataFile";
    private static final String PROPERTIES_PERSON_DATA_FILE     = "Accessibilities.PersonDataFile";
    private static final String PROPERTIES_INDIV_TOUR_DATA_FILE = "Accessibilities.IndivTourDataFile";
    private static final String PROPERTIES_JOINT_TOUR_DATA_FILE = "Accessibilities.JointTourDataFile";
    private static final String PROPERTIES_INDIV_TRIP_DATA_FILE = "Accessibilities.IndivTripDataFile";
    private static final String PROPERTIES_JOINT_TRIP_DATA_FILE = "Accessibilities.JointTripDataFile";
    private ModelStructure      modelStructure;
    private int                 iteration;
    private HashMap<String,String> rbMap;
    private HashMap<Long, HouseholdFileAttributes> householdFileAttributesMap;
    private HashMap<Long, PersonFileAttributes> personFileAttributesMap;
    private HashMap<Long, ArrayList<TourFileAttributes>> individualTourAttributesMap; //by person_id
    private HashMap<Long, ArrayList<TourFileAttributes>> jointTourAttributesMap; //by hh_id
   
    private boolean readIndividualTourFile = false;
    private boolean readJointTourFile = false;

    /**
     * Default constructor.
     * @param rbMap          Hashmap of properties
     * @param modelStructure Model structure object
     * @param iteration      Iteration number used for file names
     */
	public ModelOutputReader(HashMap<String,String> rbMap, ModelStructure modelStructure,
        int iteration)
	{
		logger.info("Writing data structures to files.");
		this.modelStructure = modelStructure;
		this.iteration = iteration;
		this.rbMap = rbMap;
	}
   
	
	/**
	 * Read household data and store records in householdFileAttributesMap
	 */
    public void readHouseholdDataOutput(){
		
		String baseDir = rbMap.get(CtrampApplication.PROPERTIES_PROJECT_DIRECTORY);
		String hhFile = rbMap.get(PROPERTIES_HOUSEHOLD_DATA_FILE);
		
	    TableDataSet householdData = readTableData(baseDir+hhFile);

	    householdFileAttributesMap = new HashMap<Long, HouseholdFileAttributes>();

	    //hh_id,home_mgra,income,HVs,AVs,transponder,cdap_pattern,out_escort_choice,inb_escort_choice,jtf_choice
	    for(int row = 1; row<=householdData.getRowCount();++row){
			
	    	long hhid = (long) householdData.getValueAt(row,"hh_id");
			int home_mgra = (int)householdData.getValueAt(row,"home_mgra");
	        int income = (int) householdData.getValueAt(row,"income");
	        int automated_vehicles = (int) householdData.getValueAt(row,"AVs");
	        int human_vehicles = (int) householdData.getValueAt(row,"HVs");
	    	int autos = automated_vehicles + human_vehicles;
	        int transponder = (int) householdData.getValueAt(row,"transponder");
	        String cdap_pattern =  householdData.getStringValueAt(row,"cdap_pattern");
	        int jtf_choice = (int) householdData.getValueAt(row,"jtf_choice");
	        int out_escort_choice = (int) householdData.getValueAt(row,"out_escort_choice");
	        int inb_escort_choice = (int) householdData.getValueAt(row,"inb_escort_choice");
	        
	        
	        //	        float sampleRate = householdData.getValueAt(row,"sampleRate");
	        HouseholdFileAttributes hhAttributes = new HouseholdFileAttributes(hhid,
	        		home_mgra, income, autos, automated_vehicles, human_vehicles,transponder,cdap_pattern,
	        		jtf_choice,out_escort_choice,inb_escort_choice);
	        
	        householdFileAttributesMap.put(hhid, hhAttributes);

	    }
	}

    
    /**
     * Read the data from the Results.PersonDataFile.
     * Data is stored in HashMap personFileAttributesMap<person_id,PersonFileAttributes>
     * so that it can be retrieved quickly for a household object.
     * 
     */
	public void readPersonDataOutput(){
		
        //read person data
        String baseDir = rbMap.get(CtrampApplication.PROPERTIES_PROJECT_DIRECTORY);
        String personFile = baseDir + rbMap.get(PROPERTIES_PERSON_DATA_FILE);
        TableDataSet personData = readTableData(personFile);

        personFileAttributesMap = new HashMap<Long, PersonFileAttributes>();
        //hh_id,person_id,person_num,age,gender,type,value_of_time,activity_pattern,imf_choice,inmf_choice,
        // fp_choice,reimb_pct,tele_choice,ie_choice,timeFactorWork,timeFactorNonWork

        for(int row = 1; row<=personData.getRowCount();++row){
        	
        	//get the values for this person
        	long hhid = (long) personData.getValueAt(row, "hh_id");
        	long person_id = (long) personData.getValueAt(row,"person_id");
        	long personNumber = (long) personData.getValueAt(row,"person_num");
        	int age = (int) personData.getValueAt(row,"age");
        	
        	String genderString = personData.getStringValueAt(row,"gender");
        	int gender = (genderString.compareTo("m")==0 ? 1 : 2);
        	
        	float valueOfTime = personData.getValueAt(row,"value_of_time");
        	String activityPattern = personData.getStringValueAt(row,"activity_pattern");
        	String type = personData.getStringValueAt(row,"type");
        	int personType = getPersonType(type);
        	
        //	int occup  = (int) personData.getValueAt(row,"occp"); 
        	
        	
        	int imfChoice = (int) personData.getValueAt(row, "imf_choice");
        	int inmfChoice = (int) personData.getValueAt(row, "inmf_choice");
        	int fp_choice = (int) personData.getValueAt(row,"fp_choice");
        	float reimb_pct = personData.getValueAt(row,"reimb_pct");
        	int tele_choice = (int) personData.getValueAt(row,"tele_choice");
        	int ie_choice = (int) personData.getValueAt(row,"ie_choice");
        	float timeFactorWork =  personData.getValueAt(row,"timeFactorWork"); 
        	float timeFactorNonWork = personData.getValueAt(row,"timeFactorNonWork"); 
        	
        	//float sampleRate = personData.getValueAt(row,"sampleRate");
           
        	PersonFileAttributes personFileAttributes = new PersonFileAttributes(hhid,person_id,personNumber,age,gender,valueOfTime,
        			activityPattern,personType, imfChoice,inmfChoice,fp_choice,reimb_pct,tele_choice,
        			ie_choice, timeFactorWork, timeFactorNonWork);
        	
        	personFileAttributesMap.put(person_id,personFileAttributes);
        	
        }

	}

	/**
	 * Read both tour files.
	 * 
	 */
	public void readTourDataOutput(){
		
		String baseDir = rbMap.get(CtrampApplication.PROPERTIES_PROJECT_DIRECTORY);
		
		if(rbMap.containsKey(PROPERTIES_INDIV_TOUR_DATA_FILE)){
			String indivTourFile = rbMap.get(PROPERTIES_INDIV_TOUR_DATA_FILE);
			if(indivTourFile != null){
				if(indivTourFile.length()>0){
					individualTourAttributesMap = readTourData(baseDir+indivTourFile, false, individualTourAttributesMap);
					readIndividualTourFile = true;
				}
			}
		}
		if(rbMap.containsKey(PROPERTIES_JOINT_TOUR_DATA_FILE)){
			String jointTourFile = rbMap.get(PROPERTIES_JOINT_TOUR_DATA_FILE);
			if(jointTourFile != null){
				if(jointTourFile.length()>0){
					jointTourAttributesMap = readTourData(baseDir+jointTourFile, true, jointTourAttributesMap);
					readJointTourFile = true;
				}
			}
		}
		if(readIndividualTourFile==false){
			logger.info("No individual tour file to read in MtcModelOutputReader class");
		}
		if(readJointTourFile==false){
			logger.info("No joint tour file to read in MtcModelOutputReader class");
		}       
	}

	
    /**
     * Read the data from the Results.IndivTourDataFile or Results.JointTourDataFile.
     * Data is stored in HashMap passed into method as an argument. Method handles
     * both individual and joint data. Joint tour data is indexed by hh_id
     * so that it can be retrieved quickly for a household object. Individual tour data is
     * indexed by person_id.
     * 
     */
	public HashMap<Long, ArrayList<TourFileAttributes>> readTourData(String filename, boolean isJoint, HashMap<Long, ArrayList<TourFileAttributes>> tourFileAttributesMap ){
		
        TableDataSet tourData = readTableData(filename);

        tourFileAttributesMap = new HashMap<Long, ArrayList<TourFileAttributes>>();
        //hh_id,person_id,person_num,person_type,tour_id,tour_category,tour_purpose,
        //orig_mgra,dest_mgra,start_period,end_period,tour_mode,av_avail,tour_distance,atwork_freq,
        //num_ob_stops,num_ib_stops,valueOfTime,escort_type_out,escort_type_in,driver_num_out,driver_num_in

        for(int row = 1; row<=tourData.getRowCount();++row){
        	
    		long hh_id = (long) tourData.getValueAt(row,"hh_id");
    		long person_id = 0;
    		int person_num=0;
    		int person_type=0;
    		int escort_type_out=0;
    		int escort_type_in=0;
    		int driver_num_out=0;
    		int driver_num_in=0;
    		if(!isJoint){
    			person_id = (long) tourData.getValueAt(row,"person_id");;
    		    person_num            = (int) tourData.getValueAt(row,"person_num");            
        		person_type           = (int) tourData.getValueAt(row,"person_type");  
        		escort_type_out	 = (int) tourData.getValueAt(row,"escort_type_out");  
        		escort_type_in	 = (int) tourData.getValueAt(row,"escort_type_in");  
        		driver_num_out	 = (int) tourData.getValueAt(row,"driver_num_out");  
        		driver_num_in    = (int) tourData.getValueAt(row,"driver_num_in");  
    		}
    		int tour_id               = (int) tourData.getValueAt(row,"tour_id");               
    		String tour_category      = tourData.getStringValueAt(row,"tour_category");         
    		String tour_purpose       = tourData.getStringValueAt(row,"tour_purpose");          
    		
    		int tour_composition = 0;
    		String tour_participants = null;
    		if(isJoint){
    			tour_composition = (int) tourData.getValueAt(row,"tour_composition");
    			tour_participants = tourData.getStringValueAt(row,"tour_participants");
    		}
    		
    		int orig_mgra             = (int) tourData.getValueAt(row,"orig_mgra");             
    		int dest_mgra             = (int) tourData.getValueAt(row,"dest_mgra");             
    		int start_period          = (int) tourData.getValueAt(row,"start_period");          
    		int end_period            = (int) tourData.getValueAt(row,"end_period");            
    		int tour_mode             = (int) tourData.getValueAt(row,"tour_mode");     
    		int av_avail              = (int) tourData.getValueAt(row,"av_avail");
    		float tour_distance       = tourData.getValueAt(row,"tour_distance");               
    	//	float tour_time           = tourData.getValueAt(row,"tour_time");                   
    		int atWork_freq           = (int) tourData.getValueAt(row,"atWork_freq");           
    		int num_ob_stops          = (int) tourData.getValueAt(row,"num_ob_stops");          
    		int num_ib_stops          = (int) tourData.getValueAt(row,"num_ib_stops"); 
    		float valueOfTime         = tourData.getValueAt(row, "valueOfTime");
    	/*
    		int out_btap              = (int) tourData.getValueAt(row,"out_btap");              
    		int out_atap              = (int) tourData.getValueAt(row,"out_atap");              
    		int in_btap               = (int) tourData.getValueAt(row,"in_btap");               
    		int in_atap               = (int) tourData.getValueAt(row,"in_atap");               
    		int out_set               = (int) tourData.getValueAt(row,"out_set");               
    		int in_set                = (int) tourData.getValueAt(row,"in_set");                
//    		float sampleRate          = tourData.getValueAt(row,"sampleRate");                  
//    		int avAvailable           = (int) tourData.getValueAt(row,"avAvailable");           
 */    		float[] util = new float[modelStructure.getMaxTourModeIndex()];
    		float[] prob = new float[modelStructure.getMaxTourModeIndex()];
   		
    		TourFileAttributes tourFileAttributes = new TourFileAttributes(hh_id, person_id, person_num, person_type,
    				 tour_id,  tour_category, tour_purpose, orig_mgra,dest_mgra,
    				 start_period, end_period, tour_mode, av_avail, tour_distance, 
    				 atWork_freq,  num_ob_stops, num_ib_stops, valueOfTime,
    				 escort_type_out,escort_type_in,driver_num_out,driver_num_in,
    				 tour_composition, tour_participants,util,prob);
        	
        	//if individual tour, map key is person_id, else it is hh_id
        	long key = -1;
        	if(!isJoint)
        		key = person_id;
        	else
        		key = hh_id;
        	
        	//if the not the first tour for this person or hh, add the tour to the existing
        	//arraylist; else create a new arraylist and add the tour attributes to it,
        	//then add the arraylist to the map
        	if(tourFileAttributesMap.containsKey(key)){
        		ArrayList<TourFileAttributes> tourArray = tourFileAttributesMap.get(key);
        		tourArray.add(tourFileAttributes);
        	}else{
        		ArrayList<TourFileAttributes> tourArray = new ArrayList<TourFileAttributes>();
        		tourArray.add(tourFileAttributes);
        		tourFileAttributesMap.put(key, tourArray);
        	}
        	
        }
        
        return tourFileAttributesMap;
	}

	/**
	 * Create individual tour objects for all persons in the household object based
	 * on the data read in the individual tour file.
	 *
	 * @param household
	 */
	public void createIndividualTours(Household household){
		
		HashMap<String,Integer> purposeIndexMap = modelStructure.getPrimaryPurposeNameIndexMap();
		Person[] persons = household.getPersons();
		for(int pnum=1;pnum<persons.length;++pnum){
			Person p = persons[pnum];
			long personId = p.getPersonId();
			if(individualTourAttributesMap.containsKey(personId)){

				ArrayList<TourFileAttributes> tourAttributesArray = individualTourAttributesMap.get(personId);
				
				//store tours by type
				ArrayList<TourFileAttributes> workTours = new ArrayList<TourFileAttributes>();
				ArrayList<TourFileAttributes> universityTours = new ArrayList<TourFileAttributes>();
				ArrayList<TourFileAttributes> schoolTours = new ArrayList<TourFileAttributes>();
				ArrayList<TourFileAttributes> atWorkSubtours = new ArrayList<TourFileAttributes>();
				ArrayList<TourFileAttributes> nonMandTours = new ArrayList<TourFileAttributes>();
				
				for(int i=0;i<tourAttributesArray.size();++i){
					
					TourFileAttributes tourAttributes = tourAttributesArray.get(i);
					if(tourAttributes.tour_purpose.compareTo(modelStructure.WORK_PRIMARY_PURPOSE_NAME)==0)
						workTours.add(tourAttributes);
					else if(tourAttributes.tour_purpose.compareTo(modelStructure.SCHOOL_PRIMARY_PURPOSE_NAME)==0)
						schoolTours.add(tourAttributes);
					else if(tourAttributes.tour_purpose.compareTo(modelStructure.UNIVERSITY_PRIMARY_PURPOSE_NAME)==0)
						universityTours.add(tourAttributes);
					else if(tourAttributes.tour_purpose.compareTo(modelStructure.AT_WORK_PURPOSE_NAME)==0)
						atWorkSubtours.add(tourAttributes);
					else
						nonMandTours.add(tourAttributes);
				}
				
				//create the mandatory tours
				if(workTours.size()>0){
					p.createWorkTours(workTours.size(), 0, ModelStructure.WORK_PRIMARY_PURPOSE_NAME,
	                    ModelStructure.WORK_PRIMARY_PURPOSE_INDEX);
					ArrayList<Tour> workTourArrayList = p.getListOfWorkTours();
					for(int i=0;i<workTourArrayList.size();++i){
						Tour workTour = workTourArrayList.get(i);
						TourFileAttributes workTourAttributes = workTours.get(i);
						workTourAttributes.setModeledTourAttributes(workTour);
					}					
				}
				//create school tours
				if(schoolTours.size()>0){
					p.createSchoolTours(schoolTours.size(), 0, ModelStructure.SCHOOL_PRIMARY_PURPOSE_NAME,
		                    ModelStructure.SCHOOL_PRIMARY_PURPOSE_INDEX);
					ArrayList<Tour> schoolTourArrayList = p.getListOfSchoolTours();
					for(int i=0;i<schoolTourArrayList.size();++i){
						Tour schoolTour = schoolTourArrayList.get(i);
						TourFileAttributes schoolTourAttributes = schoolTours.get(i);
						schoolTourAttributes.setModeledTourAttributes(schoolTour);
					}
				}
				//create university tours
				if(universityTours.size()>0){
					p.createSchoolTours(universityTours.size(), 0, ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_NAME,
		                    ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_INDEX);
					ArrayList<Tour> universityTourArrayList = p.getListOfSchoolTours();
					for(int i=0;i<universityTourArrayList.size();++i){
						Tour universityTour = universityTourArrayList.get(i);
						TourFileAttributes universityTourAttributes = universityTours.get(i);
						universityTourAttributes.setModeledTourAttributes(universityTour);
					}
				}
				//create non-mandatory tours
				for(int i =0; i<nonMandTours.size(); ++i){
					TourFileAttributes tourAttributes = nonMandTours.get(i);
					p.createIndividualNonMandatoryTours(1, tourAttributes.tour_purpose);
				}
				ArrayList<Tour> nonMandTourArrayList = p.getListOfIndividualNonMandatoryTours();
				for(int i =0; i<nonMandTours.size(); ++i){
					TourFileAttributes nonMandTourAttributes = nonMandTours.get(i);
					Tour nonMandTour = nonMandTourArrayList.get(i);
					nonMandTourAttributes.setModeledTourAttributes(nonMandTour);
				}

				
				//create at-work sub-tours
				for(int i =0; i<atWorkSubtours.size(); ++i){
					TourFileAttributes tourAttributes = atWorkSubtours.get(i);
					//TODO: assuming first work tour is location of this at-work tour; write out actual work location or parent tour ID
					//TODO: assuming purpose is eat out; need to write out actual purpose
					p.createAtWorkSubtour(tourAttributes.tour_id, 0, workTours.get(0).dest_mgra,modelStructure.AT_WORK_EAT_PURPOSE_NAME);
					
					//set tour attributes
					
				}
			}
		}
		
	}
	
	
	/**
	 * Create joint tours in the household object based on data read in the joint tour file.
	 * 
	 * @param household
	 */
	public void createJointTours(Household household){

		HashMap<String,Integer> purposeIndexMap = modelStructure.getPrimaryPurposeNameIndexMap();
		
		//joint tours
		long hhid = household.getHhId();
		if(jointTourAttributesMap.containsKey(hhid)){
			ArrayList<TourFileAttributes> tourArray = jointTourAttributesMap.get(hhid);
			int numberOfJointTours = tourArray.size();
			
			//get the first joint tour
			TourFileAttributes tourAttributes = tourArray.get(0);
			String purposeString = tourAttributes.tour_purpose;
			int purpose = purposeIndexMap.get(purposeString);
            int composition = tourAttributes.tour_composition; 
            int[] tourParticipants = getTourParticipantsArray(tourAttributes.tour_participants);
            
            Tour t1 = new Tour(household,purposeString, ModelStructure.JOINT_NON_MANDATORY_CATEGORY, purpose);
            t1.setJointTourComposition(composition);
            t1.setPersonNumArray(tourParticipants);
           
            //if the household has two joint tours, get the second
         	if(numberOfJointTours==2){
				tourAttributes = tourArray.get(2);
				purposeString = tourAttributes.tour_purpose;
				purpose = purposeIndexMap.get(purposeString);
	            composition = tourAttributes.tour_composition; 
	            tourParticipants = getTourParticipantsArray(tourAttributes.tour_participants);
	            
	            Tour t2 = new Tour(household,purposeString, ModelStructure.JOINT_NON_MANDATORY_CATEGORY, purpose);
	            t2.setJointTourComposition(composition);
				t2.setPersonNumArray(tourParticipants);
				
				//set in hh object
				household.createJointTourArray(t1, t2);
	            tourAttributes.setModeledTourAttributes(t1);
	            tourAttributes.setModeledTourAttributes(t2);
		    }else{
				household.createJointTourArray(t1);
	            tourAttributes.setModeledTourAttributes(t1);
			}
		}
		
		
	}
	
// HELPER METHODS AND CLASSES
	
	/**
	 * Split the participants string around spaces and return the 
	 * integer array of participant numbers.
	 * 
	 * @param tourParticipants
	 * @return
	 */
	public int[] getTourParticipantsArray(String tourParticipants){
		
		String[] values = tourParticipants.split(" ");
	    int[] array = new int[values.length];
	    for (int i = 0; i < array.length; i++)
	       	array[i] = Integer.parseInt(values[i]);
	    return array;
	}
	
	/**
	 * Set household and person attributes for this household object. This method uses
	 * the data in the personFileAttributesMap to set the data members of the
	 * Person objects for all persons in the household.
	 * 
	 * @param hhObject
	 */
	public void setHouseholdAndPersonAttributes(Household hhObject){
		
		long hhid = (long) hhObject.getHhId();
		HouseholdFileAttributes hhAttributes = householdFileAttributesMap.get(hhid);
		hhAttributes.setHouseholdAttributes(hhObject);
		Person[] persons = hhObject.getPersons();
		for(int i=1;i<persons.length;++i){
			Person p = persons[i];
			long person_id = (long) p.getPersonId();
			if(!personFileAttributesMap.containsKey(person_id)){
				logger.error("Error: personFileAttributes map does not contain person_id "+person_id+" in household "+hhid);
				throw new RuntimeException();
			}
			PersonFileAttributes personFileAttributes = personFileAttributesMap.get(person_id);
			personFileAttributes.setPersonAttributes(p);
		}
	}
	
	/**
	 * A class to hold a household file record attributes.
	 * @author joel.freedman
	 *
	 */
	private class HouseholdFileAttributes{
	
		long hhid;
        int home_mgra;
        int income;
        int autos;
        int automated_vehicles;
        int human_vehicles;
        int transponder;
        String cdap_pattern;
        int jtf_choice;
        int out_escort_choice;
        int inb_escort_choice;
        float sampleRate;
        
        
        // hhid,home_mgra, income, autos, automated_vehicles, human_vehicles,transponder,cdap_pattern,
		// jtf_choice,out_escort_choice,inb_escort_choice
        public HouseholdFileAttributes(long hhid, int home_mgra,
        		 int income, int autos, int automated_vehicles, int human_vehicles,int transponder,
        		String cdap_pattern, int jtf_choice, int out_escort_choice, int inb_escort_choice){
        	
    		this.hhid = hhid;
            this.home_mgra = home_mgra;
            this.income = income;
            this.autos = autos;
            this.automated_vehicles = automated_vehicles;
            this.human_vehicles = human_vehicles;
            this.transponder = transponder;
            this.cdap_pattern = cdap_pattern;
            this.jtf_choice = jtf_choice;
            this.out_escort_choice = out_escort_choice;
            this.inb_escort_choice = inb_escort_choice;

            
            //            this.sampleRate = sampleRate;
        }
        
        public void setHouseholdAttributes(Household hh){
        	
        	hh.setHhMgra(home_mgra);
        	hh.setHhIncomeInDollars(income);
        	hh.setHhAutos(autos);
        	hh.setConventionalVehicles(human_vehicles);
        	hh.setAutomatedVehicles((short)automated_vehicles);
            hh.setTpChoice(transponder);
        	hh.setCoordinatedDailyActivityPatternResult(cdap_pattern);
        	hh.setJointTourFreqResult(jtf_choice, "JTF_CHOICE_STRING_UNKNOWN");
        	hh.setOutboundEscortChoice(out_escort_choice);
        	hh.setInboundEscortChoice(inb_escort_choice);
        	
        	//        	hh.setSampleRate(sampleRate);
        }
	}
	
	
	
	/**
	 * A class to hold person file attributes (read in from Results.PersonDataFile)
	 * @author joel.freedman
	 *
	 */
	private class PersonFileAttributes{
       
		long hhid;
    	long person_id;
    	long personNumber;
    	int age;
    	int gender;
    	float valueOfTime;
    	String activityPattern;
    	int personType;
//    	int occupation;
    	int imfChoice;
    	int inmfChoice;
    	int fp_choice;
    	float reimb_pct;
    	int tele_choice;
    	int ie_choice;
    	float timeFactorWork;
    	float timeFactorNonWork;
    	
    	
    	
//    	float sampleRate;

    	//hhid,person_id,personNumber,age,gender,valueOfTime,
		//activityPattern,personType, imfChoice,inmfChoice,fp_choice,reimb_pct,tele_choice,
		//ie_choice, timeFactorWork, timeFactorNonWork);
		public PersonFileAttributes(long hhid, long person_id, long personNumber, int age, int gender,float valueOfTime, 
				String activityPattern,int personType, int imfChoice,int inmfChoice,int fp_choice, 
				float reimb_pct, int tele_choice, int ie_choice, float timeFactorWork, float timeFactorNonWork){
			
			this.hhid=hhid;
			this.person_id = person_id;
			this.personNumber=personNumber;
			this.age=age;
			this.gender=gender;
			this.valueOfTime=valueOfTime;
			this.activityPattern=activityPattern;
			this.personType=personType;
//			this.occupation=occup;
			this.imfChoice=imfChoice;
			this.inmfChoice=inmfChoice;
			this.fp_choice=fp_choice;
			this.reimb_pct=reimb_pct;
			this.tele_choice=tele_choice;
			this.ie_choice=ie_choice;
			this.timeFactorWork=timeFactorWork;
			this.timeFactorNonWork=timeFactorNonWork;
//			this.sampleRate=sampleRate;
		}
		
		
		public void setPersonAttributes(Person p){
			
			p.setPersAge(age);
			p.setPersGender(gender);
			p.setValueOfTime(valueOfTime);
			p.setDailyActivityResult(activityPattern);
			p.setPersonTypeCategory(personType);
//			p.setPersPecasOccup(occupation);
			p.setImtfChoice(imfChoice);
			p.setInmtfChoice(inmfChoice);
			p.setFreeParkingAvailableResult(fp_choice);
			p.setParkingReimbursement(reimb_pct);
			p.setTelecommuteChoice((short)tele_choice);
			p.setInternalExternalTripChoiceResult(ie_choice);
			p.setTimeFactorWork(timeFactorWork);
			p.setTimeFactorNonWork(timeFactorNonWork);
			
//			p.setSampleRate(sampleRate);
			
		}
	}
	
	private class TourFileAttributes{
		
		long hh_id;
		long person_id;
		int person_num;
		int person_type;
		int tour_id;
		String tour_category;
		String tour_purpose;
		int orig_mgra;
		int dest_mgra;
		int start_period;
		int end_period;
		int tour_mode;
		float tour_distance;
//		float tour_time;
		int atWork_freq;
		int num_ob_stops;
		int num_ib_stops;
		float valueOfTime;
		int escort_type_out;
		int escort_type_in;
		int driver_num_out;
		int driver_num_in;
	/*
		int out_btap;
		int out_atap;
		int in_btap;
		int in_atap;
		int out_set;
		int in_set;
		float sampleRate;
		*/
		int avAvailable;
		float[] util;
		float[] prob;
		
		//for joint tours
		int tour_composition;
		String tour_participants;

//		/hh_id, person_id, person_num, person_type,
//		 tour_id,  tour_category, tour_purpose, orig_mgra,dest_mgra,
//		 start_period, end_period, tour_mode, av_avail, tour_distance, 
//		 atWork_freq,  num_ob_stops, num_ib_stops, valueOfTime,
//		 escort_type_out,escort_type_in,driver_num_out,driver_num_in,
//		 tour_composition, tour_participants,util,prob
		public TourFileAttributes(long hh_id, long person_id, int person_num, int person_type,
				int tour_id, String tour_category,String tour_purpose, int orig_mgra,int dest_mgra,
				int start_period,int end_period,int tour_mode, int av_avail,float tour_distance,
				int atWork_freq, int num_ob_stops,int num_ib_stops,
				float valueOfTime, int escort_type_out,int escort_type_in,int driver_num_out,
				int driver_num_in, int tour_composition,String tour_participants,float[] util, float[] prob){
			
			
			this.hh_id = hh_id;
			this.person_id = person_id;
			this.person_num = person_num;
			this.person_type = person_type;
			this.tour_id = tour_id;
			this.tour_category = tour_category;
			this.tour_purpose = tour_purpose;
			this.orig_mgra = orig_mgra;
			this.dest_mgra = dest_mgra;
			this.start_period = start_period;
			this.end_period = end_period;
			this.tour_mode = tour_mode;
			this.avAvailable=av_avail;
			this.tour_distance = tour_distance;
//			this.tour_time = tour_time;
			this.atWork_freq = atWork_freq;
			this.num_ob_stops = num_ob_stops;
			this.num_ib_stops = num_ib_stops;
/*
  			this.out_btap = out_btap;
			this.out_atap = out_atap;
			this.in_btap = in_btap;
			this.in_atap = in_atap;
			this.out_set = out_set;
			this.in_set = in_set;
			this.sampleRate = sampleRate;
*/			this.util = util;
			this.prob = prob;
			this.tour_composition = tour_composition;
			this.tour_participants = tour_participants;
			this.valueOfTime=valueOfTime;
			this.escort_type_out=escort_type_out;
			this.escort_type_in=escort_type_in;
			this.driver_num_out=driver_num_out;
			this.driver_num_in=driver_num_in;
					
		}
		
		/**
		 * Set the tour attributes up through tour destination
		 * and time-of-day choice. Tour mode choice is not
		 * known (requires running tour mode choice).
		 * 
		 * @param tour
		 */
		public void setModeledTourAttributes(Tour tour){
			
			tour.setTourOrigMgra(orig_mgra);
			tour.setTourDestMgra(dest_mgra);
			tour.setTourDepartPeriod(start_period);
			tour.setTourArrivePeriod(end_period);
			tour.setSubtourFreqChoice(atWork_freq);
//			tour.setSampleRate(sampleRate);
			tour.setUseOwnedAV(avAvailable==1 ? true : false);
		}

		
	}
	
	
	
	/**
	 * Calculate person type value based on string.
	 * @param personTypeString
	 * @return
	 */
	private int getPersonType(String personTypeString){
		
		for(int i =0;i<Person.PERSON_TYPE_NAME_ARRAY.length;++i){
			
			if(personTypeString.compareTo(Person.PERSON_TYPE_NAME_ARRAY[i])==0)
				return i+1;
			
		}
	   
		//should never be here
		return -1;
		
	}

	
	
	/**
	 * Read data into inputDataTable tabledataset.
	 * 
	 */
	private TableDataSet readTableData(String inputFile){
		
		TableDataSet tableDataSet = null;
		
		logger.info("Begin reading the data in file " + inputFile);
	    
	    try
	    {
	    	OLD_CSVFileReader csvFile = new OLD_CSVFileReader();
	    	tableDataSet = csvFile.readFile(new File(inputFile));
	    } catch (IOException e)
	    {
	    	throw new RuntimeException(e);
        }
        logger.info("End reading the data in file " + inputFile);
        
        return tableDataSet;
	}
	/**
	 * Create a file name with the iteration number appended.
	 * 
	 * @param originalFileName The original file name	
	 * @param iteration The iteration number
	 * @return The reformed file name with the iteration number appended.
	 */
    private String formFileName(String originalFileName, int iteration)
    {
        int lastDot = originalFileName.lastIndexOf('.');

        String returnString = "";
        if (lastDot > 0)
        {
            String base = originalFileName.substring(0, lastDot);
            String ext = originalFileName.substring(lastDot);
            returnString = String.format("%s_%d%s", base, iteration, ext);
        } else
        {
            returnString = String.format("%s_%d.csv", originalFileName, iteration);
        }

        logger.info("writing " + originalFileName + " file to " + returnString);

        return returnString;
    }
 
	public HashMap<Long, HouseholdFileAttributes> getHouseholdFileAttributesMap() {
		return householdFileAttributesMap;
	}

	public HashMap<Long, PersonFileAttributes> getPersonFileAttributesMap() {
		return personFileAttributesMap;
	}

	public HashMap<Long, ArrayList<TourFileAttributes>> getIndividualTourAttributesMap() {
		return individualTourAttributesMap;
	}

	public HashMap<Long, ArrayList<TourFileAttributes>> getJointTourAttributesMap() {
		return jointTourAttributesMap;
	}

	public boolean hasIndividualTourFile() {
		return readIndividualTourFile;
	}

	public boolean hasJointTourFile() {
		return readJointTourFile;
	}


}
