package org.sandag.abm.dta.postprocessing;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.log4j.Logger;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.math.MersenneTwister;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;

import org.sandag.abm.accessibilities.AutoAndNonMotorizedSkimsCalculator;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.dta.postprocessing.dtaTrip;
import org.sandag.abm.dta.postprocessing.PostprocessModel;
import org.sandag.abm.dta.postprocessing.todDisaggregationModel;
import org.sandag.abm.modechoice.MgraDataManager;

public class detailedTODProcessing {

	private static final String PROPERTIES_DISAGGPATHTOD          = "dta.postprocessing.disaggregateTOD.path";
	private static final String PROPERTIES_DISAGGPATHNODE         = "dta.postprocessing.disaggregateNode.path";
	private static final String PROPERTIES_OUTPUTSPATH            = "dta.postprocessing.outputs.path";
	private static final String PROPERTIES_DETAILPROBABILITIES    = "dta.postprocessing.DetailedTODFile";
    private static final String PROPERTIES_NODEPROBABILITIES      = "dta.postprocessing.NodeFile";
    private static final String PROPERTIES_RANDOMSEED             = "dta.postprocessing.RandomSeed";

    private static final int                         DA_TIME_INDEX                          = 0;
    
    public static final int                             EA_SKIM_PERIOD_INDEX              = 0;
    public static final int                             AM_SKIM_PERIOD_INDEX              = 1;
    public static final int                             MD_SKIM_PERIOD_INDEX              = 2;
    public static final int                             PM_SKIM_PERIOD_INDEX              = 3;
    public static final int                             EV_SKIM_PERIOD_INDEX              = 4;

    public Matrix[] skimMatrix; 
    
    private HashMap<String,String>  rbMap;
    private HashSet<Integer>        householdTraceSet;
    private HashSet<Integer>        originTraceSet;
    private MgraDataManager         mgraManager;
    //private final AutoAndNonMotorizedSkimsCalculator autoNonMotSkims;
	private MersenneTwister random;
	private long randomSeed;
    
    private todDisaggregationModel todDisaggregationModel;
    private spatialDisaggregationModel spatialDisaggregationModel;
	
	private String disaggTODPath;
	private String disaggNodePath;
	private String outputsPath;
	
	private double sampleRate;
	private String inputFile;
	private String marketSegment;
		
	private int[] detailTODMap;
	private int[] mgraNodeMap;
	private int[] nodeMap;
	private double[] detailProbabilities;
	private double[] nodeProbabilities;
			
	private dtaTrip Trip;	
	private PrintWriter tripWriter;


    private transient Logger           logger                         = Logger.getLogger("postprocessModel");

	
	/**
	 * Default constructor.
	 */
	public detailedTODProcessing(HashMap<String,String> rbMap, double sampleRate, String inputFile, String marketSegment, HashSet<Integer> householdTraceSet, HashSet<Integer> originTraceSet, PrintWriter tripWriter){
		
		this.sampleRate = sampleRate;
		this.rbMap = rbMap;
		this.inputFile = inputFile;
		this.marketSegment = marketSegment;
		this.tripWriter = tripWriter;
		this.householdTraceSet = householdTraceSet;
		this.originTraceSet = originTraceSet;
		
		todDisaggregationModel = new todDisaggregationModel(rbMap);
		
		spatialDisaggregationModel = new spatialDisaggregationModel(rbMap);
		
		mgraManager = MgraDataManager.getInstance(rbMap);
        //autoNonMotSkims = new AutoAndNonMotorizedSkimsCalculator(rbMap);
	
		outputsPath = Util.getStringValueFromPropertyMap(rbMap, PROPERTIES_OUTPUTSPATH);

		//Read in factors and maps to aggregate time periods	
		disaggTODPath = Util.getStringValueFromPropertyMap(rbMap, PROPERTIES_DISAGGPATHTOD);
		String detailedFactorsFile = disaggTODPath + Util.getStringValueFromPropertyMap(rbMap, PROPERTIES_DETAILPROBABILITIES);

		TableDataSet DetailedData = TableDataSet.readFile(detailedFactorsFile);
		int numDetailedPeriods = DetailedData.getRowCount();
		detailProbabilities = todDisaggregationModel.getTODProbabilities(DetailedData, numDetailedPeriods, null);
		detailTODMap = todDisaggregationModel.getTODMap(DetailedData, numDetailedPeriods);
		
		disaggNodePath = Util.getStringValueFromPropertyMap(rbMap, PROPERTIES_DISAGGPATHNODE);
		String nodeFactorsFile = disaggNodePath + Util.getStringValueFromPropertyMap(rbMap, PROPERTIES_NODEPROBABILITIES);
		TableDataSet NodeData = TableDataSet.readFile(nodeFactorsFile);
		int numNodes = NodeData.getRowCount();
		nodeProbabilities = spatialDisaggregationModel.getSpatialProbabilities(NodeData, numNodes, "Probability", null);
		nodeMap = spatialDisaggregationModel.getSpatialMap(NodeData, numNodes, "NodeId");
		mgraNodeMap = spatialDisaggregationModel.getSpatialMap(NodeData, numNodes, "MGRA");
		
		randomSeed = Util.getIntegerValueFromPropertyMap(rbMap, PROPERTIES_RANDOMSEED);
		random = new MersenneTwister();
		random.setSeed(randomSeed);
		
		// read skims - AshishK
		String skimPath = Util.getStringValueFromPropertyMap(rbMap, "skims.path");
	    String skimPrefix = Util.getStringValueFromPropertyMap(rbMap, "da.no.toll.skims.prefix");
	    skimMatrix = new Matrix[ModelStructure.MODEL_PERIOD_LABELS.length];
	    
	    for(int p=0; p<ModelStructure.MODEL_PERIOD_LABELS.length; p++){
	    	String skimFileName = skimPath + skimPrefix + ModelStructure.MODEL_PERIOD_LABELS[p] + ".mtx";
	    	String matName = "*STM_" + ModelStructure.SKIM_PERIOD_STRINGS[p] + " (Skim)";
	    	skimMatrix[p] = MatrixReader.readMatrix(new File(skimFileName), matName);
	    }
		
 	}
	
		
	/**
	 * Create trip record from and disaggregate tod for detailed tod files
	 */
	public void createDetailedTODTrips(String inputFile, String marketSegment, HashMap<String,String> rbMap){
		
		TableDataSet tripRecords = TableDataSet.readFile(inputFile);
		int numRecords = tripRecords.getRowCount();
		int period = -1;
		int periodLast = -1;
		int hhidLast = -1;
		int persidLast = -1;
		int touridLast = -1;
		int modeLast = -1;
		
		double expansionFactor=1.0;
		boolean debug = false;
		boolean addSOVTrip = false;
		int tripExp=0;
		int offset = 0;
		int scheduleCount = 0;
		int tripsCount = 0;
		int [] dtaTimes;
		int [] dtaTimesPrev;
		dtaTimes = new int[10];
		dtaTimesPrev = new int[10];
		
		Arrays.fill(dtaTimesPrev, 0);
		
		logger.info("Reading "+numRecords+" trip records from disaggregate file: "+inputFile);
		
		// Create a trip record for each record in the input file
		for (int i=0; i<numRecords; ++i){
			addSOVTrip = false;
			int mode=0;
			double occ=1.0;
			expansionFactor = 1.0/sampleRate;
			double fractionalTrips = 1.0;
			// Use mode to determine number of trips to create
			if (tripRecords.containsColumn("trip_mode")){
				mode = (int) tripRecords.getValueAt(i+1, "trip_mode");
				if (mode>=16 && mode<26){
					addSOVTrip=true;
				}
			}
			
			if (tripRecords.containsColumn("tripMode")){
				mode = (int) tripRecords.getValueAt(i+1, "tripMode");
				if (mode>=16 && mode<26){
					addSOVTrip=true;
				}
			}

			
			if(mode==-99){
				continue;
			}
			
			if(marketSegment.equalsIgnoreCase("IndividualTrips")||marketSegment.equalsIgnoreCase("CrossBorderTrips")||marketSegment.equalsIgnoreCase("InternalExternalTrips")){
				if((mode>=3 && mode<=5)||mode==27){
	   				occ=2.0;
				}
				
	   			if(mode>=6 && mode<=8){
	   				if(marketSegment.equalsIgnoreCase("IndividualTrips"))
	   					occ = 3.34;
	   				else
	   					occ = 3.50;
	   			}
				
				expansionFactor = expansionFactor/occ;
				fractionalTrips = fractionalTrips/occ;
			}
			
			//Initialize trip record values
			int hhid=0;
			int persid=0;
			int tourid=0;
			int tripid=0;
			int origMGRA=0;
			int destMGRA=0;
			int origTAZ=0;
			int destTAZ=0;
			int tourDriver=-1;
			
			
			if (tripRecords.containsColumn("hh_id")){
				hhid = (int) tripRecords.getValueAt(i+1,"hh_id");
			}
			if (tripRecords.containsColumn("person_id")){
				persid = (int) tripRecords.getValueAt(i+1, "person_id");
			}
			if (tripRecords.containsColumn("tour_id")){
				tourid = (int) tripRecords.getValueAt(i+1, "tour_id");
			}
			if (tripRecords.containsColumn("tourID")){
				tourid = (int) tripRecords.getValueAt(i+1, "tourID");
			}
			if (tripRecords.containsColumn("stop_id")){
				tripid = (int) tripRecords.getValueAt(i+1, "stop_id");
			}
			if (tripRecords.containsColumn("id")){
				tripid = (int) tripRecords.getValueAt(i+1, "id");
			}
			if (tripRecords.containsColumn("tripID")){
				tripid = (int) tripRecords.getValueAt(i+1, "tripID");
			}			
			if (tripRecords.containsColumn("orig_mgra")){
				origMGRA = (int) tripRecords.getValueAt(i+1, "orig_mgra");
			}
			if (tripRecords.containsColumn("dest_mgra")){
				int parkingMGRA = (int) tripRecords.getValueAt(i+1, "parking_mgra");
				destMGRA = (int) tripRecords.getValueAt(i+1, "dest_mgra");
				if(parkingMGRA>0){
					destMGRA = parkingMGRA;	
				}	
			}
			if (tripRecords.containsColumn("originMGRA")){
				origMGRA = (int) tripRecords.getValueAt(i+1, "originMGRA");
			}
			if (tripRecords.containsColumn("destinationMGRA")){
				destMGRA = (int) tripRecords.getValueAt(i+1, "destinationMGRA");
			}
			if (tripRecords.containsColumn("originTAZ")){
				origTAZ = (int) tripRecords.getValueAt(i+1, "originTAZ");
			}else{
				origTAZ = mgraManager.getTaz(origMGRA);
			}
			if (tripRecords.containsColumn("destinationTAZ")){
				destTAZ = (int) tripRecords.getValueAt(i+1, "destinationTAZ");
			}else{
				destTAZ = mgraManager.getTaz(destMGRA);
			}
			if (tripRecords.containsColumn("arrivalMode")){
				int arriveMode = (int) tripRecords.getValueAt(i+1, "arrivalMode");
				if((occ>1 && arriveMode==5)||(mode>=16 && mode<26)){
					addSOVTrip = true;
				}
			}
			if (tripRecords.containsColumn("driver")){
				tourDriver = (int) tripRecords.getValueAt(i+1, "driver");
			}
			
			if (tripRecords.containsColumn("stop_period")){
				period = (int) tripRecords.getValueAt(i+1,"stop_period");
			}
			if (tripRecords.containsColumn("period")){
				period = (int) tripRecords.getValueAt(i+1,"period");
			}
			if (tripRecords.containsColumn("departTime")){
				period = (int) tripRecords.getValueAt(i+1,"departTime");
			}
			
			if(period==0)
				period=1;
			
			//Calculate number of trips to generate from the record (at a tour level where possible)
			
			if (tourid!=touridLast || persid!=persidLast || hhid!=hhidLast || (tourid==0 && hhid==0)){
				tripExp = (int) Math.floor(expansionFactor);
				double tripsFrac = expansionFactor - tripExp;
				double rn = random.nextDouble();
				if (rn<tripsFrac)
					tripExp += 1; 
				rn = random.nextDouble();
				if (rn>fractionalTrips)
					tripExp += 0;
			}else if(fractionalTrips<1.0 && mode==modeLast){
				double rn = random.nextDouble();
				if (rn>fractionalTrips)
					tripExp += 0;			
			}else if((fractionalTrips<1.0 && mode!=modeLast) || (fractionalTrips==1.0 && modeLast>=3 && modeLast<=8)){
				tripExp = (int) Math.floor(expansionFactor);
				double tripsFrac = expansionFactor - tripExp;
				double rn = random.nextDouble();
				if (rn<tripsFrac)
					tripExp += 1; 
				rn = random.nextDouble();
				if (rn>fractionalTrips)
					tripExp += 0;			
			}
			
			//logger.info("expansionFactor " + expansionFactor  + ", fractionalTrips " + fractionalTrips + ", tripExp " + tripExp);
			// Reset the dtaTimes array
			Arrays.fill(dtaTimes,0);
			
			// Create a number of integer trip instances based on the expansion factor		
			for (int k=0; k<tripExp; k++){
				
				Trip = new dtaTrip();
				Trip.initializeTrip();
				Trip.setHHId(hhid);
				Trip.setPersonId(persid);
				Trip.setTourId(tourid);
				Trip.setId(tripid);
				Trip.setMarketSegment(marketSegment);
				Trip.setOriginMGRA(origMGRA);
				Trip.setDestinationMGRA(destMGRA);
				Trip.setOriginTaz(origTAZ);
				Trip.setDestinationTaz(destTAZ);
				Trip.setTripMode(mode);
				Trip.setDetailedPeriod(period);
				Trip.setExpansionFactor(1.0);
				Trip.setTourDriver(tourDriver);
				
				offset = 0;
				debug = isTraceHousehold(Trip.getHHId())||isTraceOrigin(Trip.getOriginTaz());
				
				if(debug){
					logger.info("*******************************");
					logger.info("Disaggregating TOD for Trace Trip in Disaggregate TOD");
					logger.info("Market Segment = "+Trip.getMarketSegment());
					logger.info("Household = "+Trip.getHHId());
					logger.info("Person = "+Trip.getPersonId());
					logger.info("Tour = "+Trip.getTourId());
					logger.info("Trip period = "+Trip.getDetailedPeriod());
					logger.info("Period     dtaPeriod     Prob     cumProb     randomNumber");
				}

				dtaTimes[k] = todDisaggregationModel.calculateDisaggregateTOD(period, detailTODMap, detailProbabilities,debug);		
							
				// Choose nodes for the trip origin and destination					
				int origNode = spatialDisaggregationModel.selectNodeFromMGRA(Trip.getOriginMGRA(), nodeMap, mgraNodeMap, nodeProbabilities, debug);
				int destNode = spatialDisaggregationModel.selectNodeFromMGRA(Trip.getDestinationMGRA(), nodeMap, mgraNodeMap, nodeProbabilities, debug);

				Trip.setDTAPeriod(dtaTimes[k]);
				Trip.setOriginNode(origNode);
				Trip.setDestinationNode(destNode);
				tripWriter.print("\r\n");
				Trip.writeTrip(tripWriter);
				tripsCount += 1;
				
				if (addSOVTrip){
					int tollEligible=0;
					if(mode==2||mode==5||mode==8){
						tollEligible=1;
					}
					if(mode<=8||mode>=27){
						int direction = (int) tripRecords.getValueAt(i+1, "direction");
						if (direction==0){
							direction = 1;
						}else{
							direction = 0;
						}
						addSOVTrip(direction,period,dtaTimes[k],origMGRA,destMGRA,tollEligible,debug);
						tripsCount += 1;
					}else{
						int boardMGRA = -1;
						int alightMGRA = -1;
						int dir = -1;
						if(tripRecords.containsColumn("direction")){
							dir = (int) tripRecords.getValueAt(i+1,"direction");
						}else if(tripRecords.containsColumn("inbound")){
							dir = (int) tripRecords.getValueAt(i+1,"inbound");
						}
						if(tripRecords.containsColumn("boardingTAP")){
							boardMGRA = (int) tripRecords.getValueAt(i+1,"boardingTAP");
						}else if(tripRecords.containsColumn("trip_board_tap")){
							boardMGRA = (int) tripRecords.getValueAt(i+1,"trip_board_tap");
						}
						if(tripRecords.containsColumn("alightingTAP")){
							alightMGRA = (int) tripRecords.getValueAt(i+1,"alightingTAP");
						}else if(tripRecords.containsColumn("trip_alight_tap")){
							alightMGRA = (int) tripRecords.getValueAt(i+1, "trip_alight_tap");
						}
						if(dir==0){
							destMGRA = boardMGRA;
						}else{
							origMGRA = alightMGRA;
						}
						addSOVTrip(dir,period,dtaTimes[k],destMGRA,origMGRA,0,debug);
						tripsCount += 1;
					}
				
				}
			}
			hhidLast = hhid;
			persidLast = persid;
			touridLast = tourid;
			periodLast = period;
			modeLast = mode;
			dtaTimesPrev = dtaTimes.clone();
		}	
		logger.info("Market "+marketSegment+" wrote out "+tripsCount+" total trips.");
	}
	
	/**
	 * Add an SOV trip for drop-off airport trips and Drive Transit Trips
	 */
	public void addSOVTrip(int direction, int period, int dtaPer, int tripDest, int tripOrig, int toll, boolean debug){
		// **Still need to implement schedule consistency here - use skims to calculate the expected departure time
		int dtaPeriod = -1;		
		
		int origTAZ = mgraManager.getTaz(tripOrig);
		int destTAZ = mgraManager.getTaz(tripDest);
		
		int tod = 0;
		
		// Set skim matrix to the appropriate time period's SOV time skim matrix
		if (dtaPer>=1 & dtaPer<=36){
			tod = EA_SKIM_PERIOD_INDEX;
		}else if(dtaPer>36 & dtaPer<=72){
			tod = AM_SKIM_PERIOD_INDEX;
		}else if(dtaPer>72 & dtaPer<=150){
			tod = MD_SKIM_PERIOD_INDEX;
		}else if(dtaPer>150 & dtaPer<=192){
			tod = PM_SKIM_PERIOD_INDEX;
		}else{
			tod = EV_SKIM_PERIOD_INDEX;
		}

		//dtaPeriod = todDisaggregationModel.calculateDisaggregateTOD(period, detailTODMap, detailProbabilities,debug);
		
		//double[] autoSkims = autoNonMotSkims.getAutoSkims(tripOrig, tripDest, tod, false, logger);
		//double travTime = autoSkims[DA_TIME_INDEX];
		
		//changed the code to directly read the values from skim matrices - AshishK
		double travTime = skimMatrix[tod].getValueAt(origTAZ, destTAZ);
		
		int travPer = (int) Math.ceil(travTime/5.0);
		
		if (direction==1){
			dtaPeriod = dtaPer + travPer + 2;
		}else{
			dtaPeriod = dtaPer - (travPer + 2);
		}
		
		// limit the dta period between 1 and 288 - AshishK
		if(dtaPeriod < 1) {
			dtaPeriod = 1;
		}
		if(dtaPeriod > detailProbabilities.length) {
			dtaPeriod = detailProbabilities.length;
		}		
		
		int origNode = spatialDisaggregationModel.selectNodeFromMGRA(tripOrig, nodeMap, mgraNodeMap, nodeProbabilities, debug);
		int destNode = spatialDisaggregationModel.selectNodeFromMGRA(tripDest, nodeMap, mgraNodeMap, nodeProbabilities, debug);

		Trip = new dtaTrip();
		Trip.setMarketSegment(marketSegment);
		Trip.setOriginMGRA(tripOrig);
		Trip.setDestinationMGRA(tripDest);
		Trip.setOriginTaz(origTAZ);
		Trip.setDestinationTaz(destTAZ);
		Trip.setOriginNode(origNode);
		Trip.setDestinationNode(destNode);
		Trip.setDetailedPeriod(period);
		Trip.setDTAPeriod(dtaPeriod);
		Trip.setVehicleType("passengerCar");
		Trip.setVehicleOccupancy(1);
		Trip.setTollEligible(toll);
		Trip.setExpansionFactor(1.0);
		tripWriter.print("\r\n");
		Trip.writeTrip(tripWriter);	
		
	}
	        
	/**
	 * Check if this is a trace household.
	 * 
	 * @param householdId
	 * @return  True if a trace household, else false
	 */
	public boolean isTraceHousehold(int householdId){
		
		return householdTraceSet.contains(householdId);
			
	}
	
	/**
	 * Check if this is a trace origin.
	 * 
	 * @param householdId
	 * @return  True if a trace household, else false
	 */
	public boolean isTraceOrigin(int origTAZ){
		
		return originTraceSet.contains(origTAZ);
			
	}
    
}
