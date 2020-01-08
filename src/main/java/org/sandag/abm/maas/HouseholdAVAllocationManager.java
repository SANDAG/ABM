package org.sandag.abm.maas;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.MatrixDataServerRmi;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.math.MersenneTwister;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.util.PropertyMap;

public class HouseholdAVAllocationManager {

	HashMap<Integer, Household> householdMap; //by hh_id
	private static final Logger logger = Logger.getLogger(HouseholdAVAllocationModelRunner.class);
	protected HashMap<String, String> propertyMap = null;
	protected MersenneTwister       random;
	protected ModelStructure modelStructure;
	protected int iteration;
	protected static final String ModelSeedProperty = "Model.Random.Seed";
	protected static final String DirectoryProperty = "Project.Directory";
	protected static final String HouseholdDataFileProperty = "Results.HouseholdDataFile";
	protected static final String PersonDataFileProperty    = "Results.PersonDataFile";
	protected static final String IndivTripDataFileProperty = "Results.IndivTripDataFile";
	protected static final String JointTripDataFileProperty = "Results.JointTripDataFile";
	protected static final String VEHICLETRIP_OUTPUT_FILE_PROPERTY = "Maas.AVAllocationModel.vehicletrip.output.file";
	protected static final String VEHICLETRIP_OUTPUT_MATRIX_PROPERTY = "Maas.AVAllocationModel.vehicletrip.output.matrix";
   protected HashSet<Integer>        householdTraceSet;
    public static final String        PROPERTIES_HOUSEHOLD_TRACE_LIST                         = "Debug.Trace.HouseholdIdList";
    // one file per time period
    // matrices are indexed by periods
    private Matrix[]             emptyVehicleTripMatrix;
    MgraDataManager mazManager;
    TazDataManager tazManager;

	protected static final int[] AutoModes = {1,2,3};
	protected static final int MaxAutoMode = 3;
	private long randomSeed = 198761;
	protected String vehicleTripOutputFile;

	boolean sortByPerson;
	
	HashMap<String,Integer> personTypeMap;
	String[] personTypes = {"Full-time worker","Part-time worker","University student",
			"Non-worker", "Retired","Student of driving age","Student of non-driving age",
			"Child too young for school"};

	class Household {
		
		HashMap <Integer, Person> personMap; //by person_num
		int id;
		int homeMaz;
		int income;
		int autos;
		int HVs;
		int AVs;
		ArrayList<Trip> trips; 
		ArrayList<Vehicle> autonomousVehicles;
		int seed;
		boolean debug;
		
		public void writeDebug(Logger logger, boolean logAVs) {
			
			logger.info("******** HH DEBUG **************");
			logger.info("HH ID:    "+ id);
			logger.info("Home MAZ: "+homeMaz);
			logger.info("Income:   "+income);
			logger.info("Autos:    "+ autos);
			logger.info("HVs:      "+HVs);
			logger.info("AVs:      "+AVs);
			logger.info("Seed:     "+seed);
			
			//log persons
			if(personMap.size()==0) {
				logger.info(" No persons to log");
			}else {
				Set<Integer> keySet = personMap.keySet();
				for(Integer key: keySet) {
					Person person = personMap.get(key);
					person.writeDebug(logger);
				}
			}

			//log trips
			if(trips.size()==0) {
				logger.info(" No trips to log");
			}else {
				
				for(int i=0;i<trips.size();++i) {
					Trip trip = trips.get(i);
					logger.info("***** TRIP "+i+ " DEBUG *****");
					trip.writeDebug(logger);
				}
			}
			if(autonomousVehicles.size()>0 && logAVs) {
				for(int i=0;i<autonomousVehicles.size();++i) {
					Vehicle av = autonomousVehicles.get(i);
					logger.info("***** AV "+i+ " DEBUG *****");
					av.writeDebug(logger);
				}
			
				
			}
			
		}
		
		public Household() {
			
			trips = new ArrayList<Trip>();
			personMap = new HashMap<Integer,Person>();
			autonomousVehicles = new ArrayList<Vehicle>();
			
		}

		public ArrayList<Trip> getTrips() {
			return trips;
		}
		public void setTrips(ArrayList<Trip> trips) {
			this.trips = trips;
		}
		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public int getHomeMaz() {
			return homeMaz;
		}

		public void setHomeMaz(int homeMaz) {
			this.homeMaz = homeMaz;
		}

		public int getIncome() {
			return income;
		}

		public void setIncome(int income) {
			this.income = income;
		}

		public int getAutos() {
			return autos;
		}

		public void setAutos(int autos) {
			this.autos = autos;
		}

		public int getHVs() {
			return HVs;
		}

		public void setHVs(int hVs) {
			HVs = hVs;
		}

		public int getAVs() {
			return AVs;
		}

		public void setAVs(int aVs) {
			AVs = aVs;
		}

		public ArrayList<Vehicle> getAutonomousVehicles() {
			return autonomousVehicles;
		}

		public void setAutonomousVehicles(ArrayList<Vehicle> autonomousVehicles) {
			this.autonomousVehicles = autonomousVehicles;
		}

		public int getSeed() {
			return seed;
		}

		public void setSeed(int seed) {
			this.seed = seed;
		}

		public boolean isDebug() {
			return debug;
		}

		public void setDebug(boolean debug) {
			this.debug = debug;
		}
		
		
	}
	
	class Person {
		
		int hh_id;
        int person_id;
        int person_num;
        int age;
        int gender;
        int type;
        float value_of_time;
        float reimb_pct;
        float timeFactorWork;
        float timeFactorNonWork;
        int fp_choice;
        
		public void writeDebug(Logger logger) {
			
			logger.info("******** PERSON DEBUG **************");
			logger.info("HH ID:               "+ hh_id);
			logger.info("Person ID:           "+person_id);
			logger.info("Person Num:          "+person_num);
			logger.info("Age:                 "+age);
			logger.info("Gender:              "+gender);
			logger.info("Type:                "+type);
			logger.info("Value of time:       "+value_of_time);
			logger.info("Reimb percent:       "+reimb_pct);
			logger.info("Time factor work:    "+timeFactorWork);
			logger.info("Time factor nonwork: "+timeFactorNonWork);
			logger.info("Free parking choice: "+fp_choice);
			
		}

        
		public int getHh_id() {
			return hh_id;
		}
		public void setHh_id(int hh_id) {
			this.hh_id = hh_id;
		}
		public int getPerson_id() {
			return person_id;
		}
		public void setPerson_id(int person_id) {
			this.person_id = person_id;
		}
		public int getPerson_num() {
			return person_num;
		}
		public void setPerson_num(int person_num) {
			this.person_num = person_num;
		}
		public int getAge() {
			return age;
		}
		public void setAge(int age) {
			this.age = age;
		}
		public int getGender() {
			return gender;
		}
		public void setGender(int gender) {
			this.gender = gender;
		}
		public int getType() {
			return type;
		}
		public void setType(int type) {
			this.type = type;
		}
		public float getValue_of_time() {
			return value_of_time;
		}
		public void setValue_of_time(float value_of_time) {
			this.value_of_time = value_of_time;
		}
		public float getReimb_pct() {
			return reimb_pct;
		}
		public void setReimb_pct(float reimb_pct) {
			this.reimb_pct = reimb_pct;
		}
		public float getTimeFactorWork() {
			return timeFactorWork;
		}
		public void setTimeFactorWork(float timeFactorWork) {
			this.timeFactorWork = timeFactorWork;
		}
		public float getTimeFactorNonWork() {
			return timeFactorNonWork;
		}
		public void setTimeFactorNonWork(float timeFactorNonWork) {
			this.timeFactorNonWork = timeFactorNonWork;
		}
		public int getFp_choice() {
			return fp_choice;
		}
		public void setFp_choice(int fp_choice) {
			this.fp_choice = fp_choice;
		}
	}
	
	class Trip implements Comparable<Trip>{
		
        int hh_id;
        int person_id;
        int person_num;
        int tour_id;
        int stop_id;
        int inbound;
        String tour_purpose;
        String orig_purpose;
        String dest_purpose;
        int orig_maz;
        int dest_maz;
        int parking_maz;
        int stop_period;
        int periodsUntilNextTrip;
        int trip_mode;
        int av_avail;
        int tour_mode;
        int driver_pnum;
        float valueOfTime;
        int transponder_avail;
        int num_participants; //for joint trips
        ArrayList<Person> persons;
        int veh_used; //the number of the vehicle used (1,2,3 or 0 for no AV used)
        
		public void writeDebug(Logger logger) {
			
			logger.info("******** TRIP DEBUG *******");
			logger.info("HH ID: "+ hh_id);
			logger.info("Person ID: "+person_id);
			logger.info("Person Num: "+person_num);
			logger.info("Tour ID: "+tour_id);
			logger.info("Stop ID: "+stop_id);
			logger.info("Inbound: "+inbound);
			logger.info("Tour purpose: "+tour_purpose);
			logger.info("Orig purpose: "+orig_purpose);
			logger.info("Dest purpose: "+dest_purpose);
			logger.info("Orig MAZ : "+orig_maz);
			logger.info("Dest MAZ : "+dest_maz);
		    logger.info("Parking MAZ: "+parking_maz);
		    logger.info("Stop Period: "+stop_period);
		    logger.info("Periods Until Next Trip: "+periodsUntilNextTrip);
		    logger.info("Trip mode: "+trip_mode);
		    logger.info("AV avail: "+av_avail);
		    logger.info("Tour mode: "+tour_mode);
		    logger.info("Driver pnum: "+driver_pnum);
		    logger.info("Value Of Time: "+valueOfTime);
		    logger.info("Transponder Avail: "+transponder_avail);
		    logger.info("Num participants: "+num_participants); //for joint trips
		    logger.info("Veh used: "+veh_used); //the number of the vehicle used (1,2,3 or 0 for no AV used)
		    
		}

        
        /**
         * Return true if its the same person and the same tour id and purpose
         * @param thatTrip
         * @return true or false
         */
        public boolean sameTour(Trip thatTrip) {
           	
        	if((person_id==thatTrip.getPerson_id()) &&
        			(tour_id==thatTrip.getTour_id()) &&
        			(tour_purpose.compareTo(thatTrip.getTour_purpose())==0))
        		return true;
        	return false;
       }
        
        @Override     
        public int compareTo(Trip thatTrip) {   
        	
        	if(sortByPerson) {
        		
        		if(person_id<thatTrip.getPerson_id())
        			return -1;
        		else if(person_id>thatTrip.getPerson_id())
        			return 1;
        		else if(person_id==thatTrip.getPerson_id()) {
        			if(stop_period<thatTrip.getStop_period())
        				return -1;
        			else if(stop_period>thatTrip.getStop_period())
        				return 1;
        			else if(stop_period==thatTrip.getStop_period()) {
        				if(tour_purpose.compareTo("Work")==0 && thatTrip.getTour_purpose().compareTo("Work-Based")==0) {
            				if(inbound==0)
            					return -1;
            				else
            					return 1;
            			}else if(tour_purpose.compareTo("Work-based")==0 && thatTrip.getTour_purpose().compareTo("Work")==0) {
            				if(thatTrip.getInbound()==0)
            					return 1;
            				else 
            					return -1;
            			}
        			}
        			
        		}
        		return 0;
        	}
        	
        	
        	//if its the same person and the same tour, use the stop ID
        	if(sameTour(thatTrip)){
        		if(inbound<thatTrip.getInbound())
        			return -1;
        		else if(inbound>thatTrip.getInbound())
        			return 1;
        		else if(stop_id<thatTrip.getStop_id())
        			return -1;
        		else
        			return 1;
        	}
        	
        	//its not the same tour
        	if(stop_period<thatTrip.getStop_period())
        		return -1;
        	else if(stop_period>thatTrip.getStop_period())
        		return 1;
        	else if(stop_period==thatTrip.getStop_period()) { //its the same stop period
        		if((person_id==thatTrip.getPerson_id())) { //same person
        			if(tour_purpose.compareTo("Work")==0 && thatTrip.getTour_purpose().compareTo("Work-Based")==0) {
        				if(inbound==0)
        					return -1;
        				else
        					return 1;
        			}else if(tour_purpose.compareTo("Work-based")==0 && thatTrip.getTour_purpose().compareTo("Work")==0) {
        				if(thatTrip.getInbound()==0)
        					return 1;
        				else 
        					return -1;
        			}
        			
        		}
        		if(periodsUntilNextTrip<thatTrip.getPeriodsUntilNextTrip())
        			return -1;
        		else if(periodsUntilNextTrip>thatTrip.getPeriodsUntilNextTrip())
        			return 1;
        	}
        	
        	return 0;
        }
       
		public int getHh_id() {
			return hh_id;
		}
		public void setHh_id(int hh_id) {
			this.hh_id = hh_id;
		}
		public int getPerson_id() {
			return person_id;
		}
		public void setPerson_id(int person_id) {
			this.person_id = person_id;
		}
		public int getPerson_num() {
			return person_num;
		}
		public void setPerson_num(int person_num) {
			this.person_num = person_num;
		}
		public int getTour_id() {
			return tour_id;
		}
		public void setTour_id(int tour_id) {
			this.tour_id = tour_id;
		}
		public int getStop_id() {
			return stop_id;
		}
		public void setStop_id(int stop_id) {
			this.stop_id = stop_id;
		}
		public int getInbound() {
			return inbound;
		}
		public void setInbound(int inbound) {
			this.inbound = inbound;
		}
		public String getTour_purpose() {
			return tour_purpose;
		}
		public void setTour_purpose(String tour_purpose) {
			this.tour_purpose = tour_purpose;
		}
		public String getOrig_purpose() {
			return orig_purpose;
		}
		public void setOrig_purpose(String orig_purpose) {
			this.orig_purpose = orig_purpose;
		}
		public String getDest_purpose() {
			return dest_purpose;
		}
		public void setDest_purpose(String dest_purpose) {
			this.dest_purpose = dest_purpose;
		}
		public int getOrig_maz() {
			return orig_maz;
		}
		public void setOrig_maz(int orig_maz) {
			this.orig_maz = orig_maz;
		}
		public int getDest_maz() {
			return dest_maz;
		}
		public void setDest_maz(int dest_maz) {
			this.dest_maz = dest_maz;
		}
		public int getParking_maz() {
			return parking_maz;
		}
		public void setParking_maz(int parking_maz) {
			this.parking_maz = parking_maz;
		}
		public int getStop_period() {
			return stop_period;
		}
		public void setStop_period(int stop_period) {
			this.stop_period = stop_period;
		}
		public int getTrip_mode() {
			return trip_mode;
		}
		public void setTrip_mode(int trip_mode) {
			this.trip_mode = trip_mode;
		}
		public int getAv_avail() {
			return av_avail;
		}
		public void setAv_avail(int av_avail) {
			this.av_avail = av_avail;
		}
		public int getTour_mode() {
			return tour_mode;
		}
		public void setTour_mode(int tour_mode) {
			this.tour_mode = tour_mode;
		}
		public int getDriver_pnum() {
			return driver_pnum;
		}
		public void setDriver_pnum(int driver_pnum) {
			this.driver_pnum = driver_pnum;
		}
		public float getValueOfTime() {
			return valueOfTime;
		}
		public void setValueOfTime(float valueOfTime) {
			this.valueOfTime = valueOfTime;
		}
		public int getTransponder_avail() {
			return transponder_avail;
		}
		public void setTransponder_avail(int transponder_avail) {
			this.transponder_avail = transponder_avail;
		}
		public int getNum_participants() {
			return num_participants;
		}
		public void setNum_participants(int num_participants) {
			this.num_participants = num_participants;
		}
		public ArrayList<Person> getPersons() {
			return persons;
		}
		public void setPersons(ArrayList<Person> persons) {
			this.persons = persons;
		}

		public int getPeriodsUntilNextTrip() {
			return periodsUntilNextTrip;
		}

		public void setPeriodsUntilNextTrip(int periodsUntilNextTrip) {
			this.periodsUntilNextTrip = periodsUntilNextTrip;
		}

		public int getVeh_used() {
			return veh_used;
		}

		public void setVeh_used(int veh_used) {
			this.veh_used = veh_used;
		}

	}
	
	public class VehicleTrip{
		
		int origMaz;
		int destMaz;
		int period;
		int occupants;
		boolean originIsHome;
		boolean destinationIsHome;
		boolean originIsRemoteParking;
		boolean destinationIsRemoteParking;
		int parkingChoiceAtDestination;
		
		Trip tripServed;
		
		public void writeDebug(Logger logger) {
			
			logger.info("*** VEHICLE TRIP DEBUG ***");
			logger.info("Orig MAZ: "+origMaz);
			logger.info("Dest MAZ: "+destMaz);
			logger.info("Period: "+period);
			logger.info("Occupants: "+occupants);
			logger.info("Orig is home: "+originIsHome);
			logger.info("Dest is home: "+destinationIsHome);
			logger.info("Orig is remote park: "+originIsRemoteParking);
			logger.info("Dest is remote park: "+destinationIsRemoteParking);
			logger.info("Parking choice at dest: "+parkingChoiceAtDestination);

		}
		
		public VehicleTrip() {
		}
		
		public int getOrigMaz() {
			return origMaz;
		}

		public void setOrigMaz(int origMaz) {
			this.origMaz = origMaz;
		}

		public int getDestMaz() {
			return destMaz;
		}

		public void setDestMaz(int destMaz) {
			this.destMaz = destMaz;
		}

		public int getPeriod() {
			return period;
		}

		public void setPeriod(int period) {
			this.period = period;
		}

		public int getOccupants() {
			return occupants;
		}

		public void setOccupants(int occupants) {
			this.occupants = occupants;
		}

		public boolean isOriginIsHome() {
			return originIsHome;
		}

		public void setOriginIsHome(boolean originIsHome) {
			this.originIsHome = originIsHome;
		}

		public boolean isDestinationIsHome() {
			return destinationIsHome;
		}

		public void setDestinationIsHome(boolean destinationIsHome) {
			this.destinationIsHome = destinationIsHome;
		}

		public boolean isOriginIsRemoteParking() {
			return originIsRemoteParking;
		}

		public void setOriginIsRemoteParking(boolean originIsRemoteParking) {
			this.originIsRemoteParking = originIsRemoteParking;
		}

		public boolean isDestinationIsRemoteParking() {
			return destinationIsRemoteParking;
		}

		public void setDestinationIsRemoteParking(boolean destinationIsRemoteParking) {
			this.destinationIsRemoteParking = destinationIsRemoteParking;
		}

		public int getParkingChoiceAtDestination() {
			return parkingChoiceAtDestination;
		}

		public void setParkingChoiceAtDestination(int parkingChoiceAtDestination) {
			this.parkingChoiceAtDestination = parkingChoiceAtDestination;
		}

		public Trip getTripServed() {
			return tripServed;
		}

		public void setTripServed(Trip tripServed) {
			this.tripServed = tripServed;
		}
		
	}
	
	 /**
	  * This method writes AV vehicle trips to the output file.
	 * 
	 */
	public void writeVehicleTrips(float sampleRate){
		
		logger.info("Writing AV trips to file " + vehicleTripOutputFile);
        PrintWriter printWriter = null;
        try
        {
        	printWriter = new PrintWriter(new BufferedWriter(new FileWriter(vehicleTripOutputFile)));
        } catch (IOException e)
        {
            logger.fatal("Could not open file " + vehicleTripOutputFile + " for writing\n");
            throw new RuntimeException();
        }
        
       printHeader(printWriter);
       Set<Integer> keySet = householdMap.keySet();
       for(Integer key: keySet) {
    	   Household hh = householdMap.get(key);
    	   printVehicleTrips(printWriter,hh, sampleRate);
    	   printWriter.flush();
       }
       
       printWriter.close();
        
	}

	
	public void printHeader(PrintWriter writer) {
		
		writer.println("hh_id,veh_id,vehicleTrip_id,origMaz,destMaz,period,occupants,"
				+ "originIsHome,destinationIsHome,originIsRemoteParking,destinationIsRemoteParking,"
				+ "parkingChoiceAtDestination,"
				+ "person_id,person_num,tour_id,stop_id,inbound,tour_purpose,orig_purpose,dest_purpose,"
				+ "orig_maz,dest_maz,stop_period,periodsUntilNextTrip,trip_mode");
		
	}
	
	/**
	 * Write output to the printwriter.
	 * 
	 * @param writer
	 * @param hh
	 */
	public void printVehicleTrips(PrintWriter writer, Household hh, float sampleRate) {
		
		int hhid=hh.getId();
		ArrayList<Vehicle> vehicles = hh.getAutonomousVehicles();
		if(vehicles==null)
			return;
		for(int i=0;i<vehicles.size();++i) {
		
			Vehicle vehicle = vehicles.get(i);
			
			ArrayList<VehicleTrip> vehicleTrips = vehicle.getVehicleTrips();
			
			if(vehicleTrips==null)
				continue;
			
			if(vehicleTrips.size()==0)
				continue;
			
			for(int j=0;j<vehicleTrips.size();++j) {
				
				VehicleTrip vehicleTrip = vehicleTrips.get(j);
				

				writer.print(hhid +"," + (i+1) + "," + (j+1) + ","
						+ vehicleTrip.getOrigMaz() + "," + vehicleTrip.getDestMaz() +","
						+ vehicleTrip.getPeriod() + "," + vehicleTrip.getOccupants() +","
						+ (vehicleTrip.isOriginIsHome() ? 1: 0) + ","
						+ (vehicleTrip.isDestinationIsHome() ? 1 : 0 ) + ","
						+ (vehicleTrip.isOriginIsRemoteParking() ? 1 : 0) +"," 
						+ (vehicleTrip.isDestinationIsRemoteParking() ? 1 : 0) +","
						+ vehicleTrip.getParkingChoiceAtDestination() + ",");
				
				Trip trip = vehicleTrip.getTripServed();
				if(trip!=null) {
					
					writer.print( trip.getPerson_id() + "," +
							trip.getPerson_num() + "," +
							trip.getTour_id() + "," +
							trip.getStop_id() + "," +
							trip.getInbound() + "," + 
							trip.getTour_purpose() + "," +
							trip.getOrig_purpose() + "," +
							trip.getDest_purpose() + "," +
							trip.getOrig_maz() + "," +
							trip.getDest_maz() + "," +
							trip.getStop_period() + "," +
							trip.getPeriodsUntilNextTrip() + "," +
							trip.getTrip_mode());
				}else {
					writer.print( 0 + "," +
							0 + "," +
							0+ "," +
							0 + "," +
							0 + "," + 
							"null" + "," +
							"null" + "," +
							"null" + "," +
							0 + "," +
							0 + "," +
							0 + "," +
							0 + "," +
							0);
				}
				writer.print("\n");
				
				//save the trip if its an empty trip in the matrix
				if(vehicleTrip.getOccupants()==0) {
					int period = vehicleTrip.getPeriod();
					int skimPeriod = modelStructure.getModelPeriodIndex(period);
					int originTaz = mazManager.getTaz(vehicleTrip.getOrigMaz());
					int destinationTaz = mazManager.getTaz(vehicleTrip.getDestMaz());
					
					float existingTrips = emptyVehicleTripMatrix[skimPeriod].getValueAt(originTaz, destinationTaz);
					emptyVehicleTripMatrix[skimPeriod].setValueAt(originTaz, destinationTaz, (existingTrips+1) * (1/sampleRate));
				}
						
			}
			
		}
		
	}
	public class Vehicle{
		
		int maz;
		boolean isHome;
		int withPersonId;
		int periodAvailable;
		ArrayList<VehicleTrip> vehicleTrips;
		
		public Vehicle() {
			
			vehicleTrips = new ArrayList<VehicleTrip>();
		}
		
		public void writeDebug(Logger logger) {
			
			logger.info("*** Vehicle debug **");
			logger.info("MAZ:               "+maz);
			logger.info("Is home:           "+isHome);
			logger.info("Period available:  "+periodAvailable);
			
			if(vehicleTrips.size()>0) {
				for(int i =0;i<vehicleTrips.size();++i) {
					VehicleTrip vehTrip = vehicleTrips.get(i);
					logger.info("***** VEHICLE TRIP "+i+" DEBUG ******");
					vehTrip.writeDebug(logger);
				}
			}
		}

		
		
		/**
		 * Create a new vehicle trip and add it to the vehicleTrip ArrayList.
		 * Also return it.
		 * 
		 * @param destMaz
		 * @param period
		 * @param occupants
		 * @param destinationIsHome
		 * @param tripServed
		 * @return
		 */
		public VehicleTrip addNewVehicleTrip(int destMaz,int period,int occupants,boolean destinationIsHome,Trip tripServed){
			
			VehicleTrip vehicleTrip = new VehicleTrip();
			vehicleTrip.setDestMaz(destMaz);
			vehicleTrip.setPeriod(period);
			vehicleTrip.setOccupants(occupants);
			vehicleTrip.setTripServed(tripServed);
			vehicleTrip.setDestinationIsHome(destinationIsHome);
			
			if(vehicleTrips.size()>0) {
				VehicleTrip lastTrip = vehicleTrips.get(vehicleTrips.size()-1);
				vehicleTrip.setOrigMaz(lastTrip.getDestMaz());
				vehicleTrip.setOriginIsHome(lastTrip.isDestinationIsHome());
			}else {
				vehicleTrip.setOriginIsHome(true);
			}
			
			vehicleTrips.add(vehicleTrip);
			
			return vehicleTrip;
		}
		
		public VehicleTrip createNewVehicleTrip() {
			return new VehicleTrip();
		}
		
		public int getMaz() {
			return maz;
		}
		public void setMaz(int maz) {
			this.maz = maz;
		}
		public boolean isHome() {
			return isHome;
		}
		public void setHome(boolean isHome) {
			this.isHome = isHome;
		}
		public int getPeriodAvailable() {
			return periodAvailable;
		}
		public void setPeriodAvailable(int periodAvailable) {
			this.periodAvailable = periodAvailable;
		}

		public ArrayList<VehicleTrip> getVehicleTrips() {
			return vehicleTrips;
		}

		public void setVehicleTrips(ArrayList<VehicleTrip> vehicleTrips) {
			this.vehicleTrips = vehicleTrips;
		}

		public int getWithPersonId() {
			return withPersonId;
		}

		public void setWithPersonId(int withPersonId) {
			this.withPersonId = withPersonId;
		}
	
	}
	
	public HouseholdAVAllocationManager(HashMap<String, String> propertyMap, int iteration,MgraDataManager mazManager,TazDataManager tazManager){
    	this.iteration = iteration;
    	this.propertyMap = propertyMap;
    	this.tazManager = tazManager;
    	this.mazManager = mazManager;
    	modelStructure = new SandagModelStructure();
		
	}
	
	
	public void setup() {
		
		random = new MersenneTwister();
		random.setSeed(randomSeed);
	    String directory = Util.getStringValueFromPropertyMap(propertyMap, "Project.Directory");
	    vehicleTripOutputFile = directory + Util.getStringValueFromPropertyMap(propertyMap, VEHICLETRIP_OUTPUT_FILE_PROPERTY);
		
		householdMap = new HashMap<Integer, Household>();

	    personTypeMap = new HashMap<String,Integer>();
	    
	    for(int i = 0;i<personTypes.length;++i) {
	    	
	    	personTypeMap.put(personTypes[i],new Integer(i+1));
	    }
	    
	    //initialize the matrices for writing trips
        int maxTaz = tazManager.getMaxTaz();
        int[] tazIndex = new int[maxTaz + 1];

        // assume zone numbers are sequential
        for (int i = 1; i < tazIndex.length; ++i)
            tazIndex[i] = i;
        
        emptyVehicleTripMatrix = new Matrix[modelStructure.SKIM_PERIOD_INDICES.length];

	    for(int i =0;i<modelStructure.SKIM_PERIOD_INDICES.length;++i) {
	    	emptyVehicleTripMatrix[i] = new Matrix("EmptyAV_" + modelStructure.getModelPeriodLabel(i), "", maxTaz, maxTaz);
	    	emptyVehicleTripMatrix[i].setExternalNumbers(tazIndex);
	    	
	    }
	}
	
	public void readInputFiles() {
		
        logger.info("Start reading data");
		logger.info("...Reading households");

		readHouseholds();

		logger.info("...Reading persons");

		readPersons();
        String directory = Util.getStringValueFromPropertyMap(propertyMap, DirectoryProperty);

        String indivTripFile = directory
                + Util.getStringValueFromPropertyMap(propertyMap, IndivTripDataFileProperty);
        indivTripFile = insertIterationNumber(indivTripFile,iteration);
     
		logger.info("...Reading individual trips");

        readTrips(indivTripFile,false);
        
        String jointTripFile = directory
                + Util.getStringValueFromPropertyMap(propertyMap, JointTripDataFileProperty);
        jointTripFile = insertIterationNumber(jointTripFile,iteration);

		logger.info("...Reading joint trips");

        readTrips(jointTripFile,true);
        
        logger.info("Completed reading data");
        
        dropHouseholdsWithoutAVs();
        dropNonAVTrips();
        sortTrips();
        
        //trace results
        logTraceHouseholds();		
	}

	public void logTraceHouseholds() {
		
		Set<Integer> keySet = householdMap.keySet();
		
		for(Integer key: keySet) {
			
			Household hh = householdMap.get(key);
			if(hh.isDebug()) {
				
				logger.info("***********************************************");
				logger.info("AV allocation model trace (After reading) for household "+hh.getId());
				logger.info("");
				hh.writeDebug(logger, false);
				logger.info("***********************************************");
				
			}
		}
		
		
	}
	
	/*
	 * Drop households from the map that don't have AVs
	 */
	public void dropHouseholdsWithoutAVs() {
		
		logger.info("Dropping non-AV households");

		Set<Integer> keys = householdMap.keySet();
		ArrayList<Integer> hhIdsToRemove = new ArrayList<Integer>();
		
		for(Integer key: keys) {
			
			Household hh = householdMap.get(key);
			if(hh.getAVs()<=0) {
				hhIdsToRemove.add(key);
			}
		}
		if(hhIdsToRemove.size()>0) {
			for(Integer hhId : hhIdsToRemove) {
				householdMap.remove(hhId);
			}
		}
		logger.info("Completed dropping non-AV households");
		
	}
	
	/*
	 * Drop trips from the map that aren't auto trips with AVs
	 */
	public void dropNonAVTrips() {
		
		logger.info("Dropping non-AV trips from households");
		Set<Integer> keys = householdMap.keySet();
		
		for(Integer key: keys) {
			
			Household hh = householdMap.get(key);
			ArrayList<Trip> trips = hh.getTrips();
			if(trips.size()==0)
				continue;
		
		    Iterator<Trip> itr = trips.iterator(); 
		    while (itr.hasNext()){
		    	Trip trip = itr.next(); 
		             
		    	if(trip.getAv_avail()==0)
		    		itr.remove();
		    	
		    	if(trip.trip_mode>MaxAutoMode)
		    		itr.remove();
		    }
		}
		logger.info("Completed dropping non-AV trips from households");
		
	}

	
	public void sortTrips() {
		
		Set<Integer> keys = householdMap.keySet();
		for(Integer key: keys) {
			
			Household hh = householdMap.get(key);
			ArrayList<Trip> trips = hh.getTrips();
			if(trips.size()==0)
				continue;
	
			sortByPerson=true;
			Collections.sort(trips);
			
			//first calculate time before next AV trip made by same person
			for(int i = 0 ; i< trips.size();++i) {
				Trip trip = trips.get(i);
				if(i<(trips.size()-1)) {
					Trip nextTrip = trips.get(i+1);
					if(trip.getPerson_id()==nextTrip.getPerson_id()) {
						int periods = nextTrip.getStop_period()-trip.getStop_period();
						trip.setPeriodsUntilNextTrip(periods);
					}else
						trip.setPeriodsUntilNextTrip(99);//last trip of the day
				}else
					trip.setPeriodsUntilNextTrip(99); //last trip of the household
				
			}
			sortByPerson=false;

			Collections.sort(trips);
		
		}

	}
	
	
	public void readHouseholds() {
		

		setDebugHhIdsFromHashmap();
		
        String directory = Util.getStringValueFromPropertyMap(propertyMap, DirectoryProperty);
        String householdFile = directory
                + Util.getStringValueFromPropertyMap(propertyMap, HouseholdDataFileProperty);
        householdFile = insertIterationNumber(householdFile,iteration);
        
        //get the household table and fill up the householdMap with households.
        TableDataSet householdDataSet = readTableData(householdFile);
        
        for(int row=1;row<=householdDataSet.getRowCount();++row) {
        	
			int seed = Math.abs(random.nextInt());

        	//read data
        	int hhId = (int) householdDataSet.getValueAt(row, "hh_id");
        	int hhMgra = (int) householdDataSet.getValueAt(row,"home_mgra");
        	int income = (int) householdDataSet.getValueAt(row,"income");
        	int autos = (int) householdDataSet.getValueAt(row,"autos");
        	int HVs = (int) householdDataSet.getValueAt(row,"HVs");
        	int AVs = (int) householdDataSet.getValueAt(row,"AVs");
        	
        	//create household object
        	Household hh = new Household();
        	hh.setId(hhId);
        	hh.setHomeMaz(hhMgra);
        	hh.setIncome(income);
        	hh.setAutos(autos);
        	hh.setHVs(HVs);
        	hh.setAVs(AVs);
        	hh.setSeed(seed);
        	
        	if(householdTraceSet.contains(hhId))
        		hh.setDebug(true);
        	else
        		hh.setDebug(false);
        	
        	//generate a set of vehicles and store in h
        	if(AVs>0) {
        		for(int i=0;i<AVs;++i) {
        			Vehicle AV = new Vehicle();
        			AV.setPeriodAvailable(0);
        			AV.setHome(true);
        			AV.setMaz(hhMgra);
        			ArrayList<Vehicle> vehicles = hh.getAutonomousVehicles();
        			vehicles.add(AV);
        		}
        	}
        	
        	//put hh in map
        	householdMap.put(hhId, hh);
        			
        }

		
	}
	
	public void readPersons() {
		
		
        String directory = Util.getStringValueFromPropertyMap(propertyMap, DirectoryProperty);
        String personFile = directory
                + Util.getStringValueFromPropertyMap(propertyMap, PersonDataFileProperty);
        personFile = insertIterationNumber(personFile,iteration);
        
        //get the household table and fill up the householdMap with households.
        TableDataSet personDataSet = readTableData(personFile);

        for(int row=1;row<=personDataSet.getRowCount();++row) {

		
        	int hh_id = (int) personDataSet.getValueAt(row,"hh_id");
        	int person_id = (int) personDataSet.getValueAt(row,"person_id");
        	int person_num = (int) personDataSet.getValueAt(row,"person_num");
        	int age = (int) personDataSet.getValueAt(row,"age");
        	String gender = personDataSet.getStringValueAt(row,"gender");
        	String type = personDataSet.getStringValueAt(row,"type");
        	float value_of_time =  personDataSet.getValueAt(row,"value_of_time");
        	float reimb_pct =  personDataSet.getValueAt(row,"reimb_pct");
        	int parkingChoice = (int) personDataSet.getValueAt(row, "fp_choice");
        	float timeFactorWork =  personDataSet.getValueAt(row,"timeFactorWork");
        	float timeFactorNonWork =  personDataSet.getValueAt(row,"timeFactorNonWork");
        
        	Person person = new Person();
        	person.setHh_id(hh_id);
        	person.setPerson_id(person_id);
        	person.setPerson_num(person_num);
        	person.setAge(age);
        	person.setGender(gender.compareToIgnoreCase("m")==0 ? 1 : 2);
        	person.setType(personTypeMap.get(type));
        	person.setValue_of_time(value_of_time);
        	person.setReimb_pct(reimb_pct);
        	person.setFp_choice(parkingChoice);
        	person.setTimeFactorWork(timeFactorWork);
        	person.setTimeFactorNonWork(timeFactorNonWork);
        	
        	if(householdMap.containsKey(hh_id)) {
        		Household hh = householdMap.get(hh_id);
        		hh.personMap.put(person_num,person);
        	}else {
        		logger.fatal("Error: No household ID "+hh_id+" in householdMap. Cannot add person object");
        		throw new RuntimeException();
        	}
        	
        }

	}
	
	public void readTrips(String filename, boolean isJoint) {
		
		
        //get the household table and fill up the householdMap with households.
        TableDataSet tripDataSet = readTableData(filename);

        for(int row=1;row<=tripDataSet.getRowCount();++row) {

	
        	int hh_id = (int) tripDataSet.getValueAt(row, "hh_id");
        	int person_id=-9;
        	int person_num=-9;
        	int num_participants = 1;
        	int driver_pnum=-9;
        	if(!isJoint) {
        		person_id = (int) tripDataSet.getValueAt(row,"person_id");
        		person_num = (int) tripDataSet.getValueAt(row,"person_num");
               	driver_pnum = (int) tripDataSet.getValueAt(row,"driver_pnum");
            }else {
         		num_participants = (int) tripDataSet.getValueAt(row,"num_participants");
           		person_id = hh_id*100+num_participants;
        	}
        	int tour_id = (int) tripDataSet.getValueAt(row,"tour_id");
        	int stop_id = (int) tripDataSet.getValueAt(row,"stop_id");
        	int inbound = (int) tripDataSet.getValueAt(row,"inbound");
        	String tour_purpose =  tripDataSet.getStringValueAt(row,"tour_purpose");
        	String orig_purpose = tripDataSet.getStringValueAt(row,"orig_purpose");
        	String dest_purpose = tripDataSet.getStringValueAt(row,"dest_purpose");
        	int orig_maz= (int) tripDataSet.getValueAt(row,"orig_mgra");
        	int dest_maz= (int) tripDataSet.getValueAt(row,"dest_mgra");
        	int parking_maz = (int) tripDataSet.getValueAt(row,"parking_mgra");
        	int stop_period =  (int) tripDataSet.getValueAt(row,"stop_period");
        	int trip_mode =  (int) tripDataSet.getValueAt(row,"trip_mode");
        	int av_avail = (int) tripDataSet.getValueAt(row,"av_avail");
        	int tour_mode = (int) tripDataSet.getValueAt(row,"tour_mode");
        	float valueOfTime = tripDataSet.getValueAt(row,"valueOfTime");
        	int transponder_avail = (int) tripDataSet.getValueAt(row,"transponder_avail");
        	
        	Trip trip = new Trip();
        	trip.setHh_id(hh_id);
        	trip.setPerson_id(person_id);
        	trip.setPerson_num(person_num);
        	trip.setTour_id(tour_id);
        	trip.setStop_id(stop_id);
        	trip.setInbound(inbound);
        	trip.setTour_purpose(tour_purpose);
        	trip.setOrig_purpose(orig_purpose);
        	trip.setDest_purpose(dest_purpose);
        	trip.setOrig_maz(orig_maz);
        	trip.setDest_maz(dest_maz);
        	trip.setParking_maz(parking_maz);
        	trip.setStop_period(stop_period);
        	trip.setTrip_mode(trip_mode);
        	trip.setAv_avail(av_avail);
        	trip.setTour_mode(tour_mode);
        	trip.setDriver_pnum(driver_pnum);
        	trip.setValueOfTime(valueOfTime);
        	trip.setTransponder_avail(transponder_avail);
        	trip.setNum_participants(num_participants);
        	
        	if(householdMap.containsKey(hh_id)){
        		Household hh = householdMap.get(hh_id);
        		
        		//following code only handles individual trips right now
        		//TODO: Revise trip file to include participants, and modify code to 
        		//add all participants.
        		if(person_num!=-99) {
            		HashMap<Integer,Person> personMap = hh.personMap;
            		Person person = personMap.get(person_num);
            		ArrayList<Person> personsOnTrip = trip.getPersons();
            		if(personsOnTrip==null)
            			personsOnTrip = new ArrayList<Person>();
            		personsOnTrip.add(person);
        		}
        		hh.trips.add(trip);
        	}else {
        		logger.fatal("Error: No household ID "+hh_id+" in householdMap. Cannot add trip object");
        		throw new RuntimeException();

        	}
        	
        }

        
	
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
	 * A simple helper function to insert the iteration number into the file name.
	 * 
	 * @param filename The input file name (ex: inputFile.csv)
	 * @param iteration The iteration number (ex: 3)
	 * @return The new string (ex: inputFile_3.csv)
	 */
	private String insertIterationNumber(String filename, int iteration){
		
		String newFileName = filename.replace(".csv", "_"+new Integer(iteration).toString()+".csv");
		return newFileName;
	}

	public HashMap<Integer, Household> getHouseholdMap() {
		return householdMap;
	}
	
    public void setDebugHhIdsFromHashmap()
    {

        householdTraceSet = new HashSet<Integer>();

        // get the household ids for which debug info is required
        String householdTraceStringList = propertyMap.get(PROPERTIES_HOUSEHOLD_TRACE_LIST);

        if (householdTraceStringList != null)
        {
            StringTokenizer householdTokenizer = new StringTokenizer(householdTraceStringList, ",");
            while (householdTokenizer.hasMoreTokens())
            {
                String listValue = householdTokenizer.nextToken();
                int idValue = Integer.parseInt(listValue.trim());
                householdTraceSet.add(idValue);
            }
        }

    }

    /**
     * Get the output trip table file names from the properties file, and write
     * trip tables for all modes for the given time period.
     * 
     * @param period
     *            Time period, which will be used to find the period time string
     *            to append to each trip table matrix file
     */
    public void writeTripTable(MatrixDataServerRmi ms)
    {

        String directory = Util.getStringValueFromPropertyMap(propertyMap, "scenario.path");
        String matrixTypeName = Util.getStringValueFromPropertyMap(propertyMap, "Results.MatrixType");
        MatrixType mt = MatrixType.lookUpMatrixType(matrixTypeName);

        String fileName = directory + Util.getStringValueFromPropertyMap(propertyMap, VEHICLETRIP_OUTPUT_MATRIX_PROPERTY) + ".omx";
       	try{
	    	//Delete the file if it exists
	    	File f = new File(fileName);
       	    if(f.exists()){
       	       	logger.info("Deleting existing trip file: "+fileName);
       	       	f.delete();
       	    }

        	if (ms != null) 
        		ms.writeMatrixFile(fileName, emptyVehicleTripMatrix, mt);
        	else 
        		writeMatrixFile(fileName, emptyVehicleTripMatrix);
       		} catch (Exception e){
       			logger.error("exception caught writing " + mt.toString() + " matrix file = "
                       + fileName, e);
        		throw new RuntimeException();
       		}

    }
    /**
     * Utility method to write a set of matrices to disk.
     * 
     * @param fileName
     *            The file name to write to.
     * @param m
     *            An array of matrices
     */
    private void writeMatrixFile(String fileName, Matrix[] m)
    {

        // auto trips
        MatrixWriter writer = MatrixWriter.createWriter(fileName);
        String[] names = new String[m.length];

        for (int i = 0; i < m.length; i++)
        {
            names[i] = m[i].getName();
            logger.info(m[i].getName() + " has " + m[i].getRowCount() + " rows, "
                    + m[i].getColumnCount() + " cols, and a total of " + m[i].getSum());
        }

        writer.writeMatrices(names, m);
    }


	
}
