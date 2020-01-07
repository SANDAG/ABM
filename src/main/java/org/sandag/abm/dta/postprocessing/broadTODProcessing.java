package org.sandag.abm.dta.postprocessing;

import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.log4j.Logger;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.math.MersenneTwister;

import org.sandag.abm.ctramp.Util;
import org.sandag.abm.dta.postprocessing.dtaTrip;
import org.sandag.abm.dta.postprocessing.todDisaggregationModel;
import org.sandag.abm.dta.postprocessing.PostprocessModel;
import org.sandag.abm.dta.postprocessing.spatialDisaggregationModel;

public class broadTODProcessing {

	private static final String PROPERTIES_DISAGGPATHTOD          = "dta.postprocessing.disaggregateTOD.path";
	private static final String PROPERTIES_DISAGGPATHZONE         = "dta.postprocessing.disaggregateZone.path";
	private static final String PROPERTIES_DISAGGPATHNODE         = "dta.postprocessing.disaggregateNode.path";
    private static final String PROPERTIES_BROADTODPROBABILITIES  = "dta.postprocessing.BroadTODFile";
    private static final String PROPERTIES_ZONEPROBABILITIES      = "dta.postprocessing.ZoneFile";
    private static final String PROPERTIES_NODEPROBABILITIES      = "dta.postprocessing.NodeFile";
    private static final String PROPERTIES_RANDOMSEED             = "dta.postprocessing.RandomSeed";
    
    private HashMap<String,String> rbMap;
    private HashSet<Integer>        originTraceSet;
	private MersenneTwister random;
	private long randomSeed;
	
	private String disaggTODPath;
	private String disaggZonePath;
	private String disaggNodePath;
	
	private double sampleRate;
	private String inputFile;
	private String marketSegment;
	
	private int[] broadTODMap;
	private int[] tazMap;
	private int[] mgraTAZMap;
	private int[] mgraNodeMap;
	private int[] nodeMap;
	private double[] broadProbabilities;
	private double[] mgraProdProbabilities;
	private double[] mgraAttrProbabilities;
	private double[] nodeProbabilities;
	
    private todDisaggregationModel todDisaggregationModel;
    private spatialDisaggregationModel spatialDisaggregationModel;
	
	private dtaTrip Trip;	
	private PrintWriter tripWriter;


    private transient Logger           logger                         = Logger.getLogger("postprocessModel");

	
	/**
	 * Default constructor.
	 */
	public broadTODProcessing(HashMap<String,String> rbMap, double sampleRate, String inputFile, String marketSegment, HashSet<Integer> originTraceSet, PrintWriter tripWriter){
		
		this.rbMap = rbMap;
		this.sampleRate = sampleRate;
		this.inputFile = inputFile;
		this.marketSegment = marketSegment;
		this.tripWriter = tripWriter;
		this.originTraceSet = originTraceSet;
		
		todDisaggregationModel = new todDisaggregationModel(rbMap);
		spatialDisaggregationModel = new spatialDisaggregationModel(rbMap);
		
		randomSeed = Util.getIntegerValueFromPropertyMap(rbMap, PROPERTIES_RANDOMSEED);
		random = new MersenneTwister();
		random.setSeed(randomSeed);
								
		//Read in factors and maps to aggregate time periods
		disaggTODPath = Util.getStringValueFromPropertyMap(rbMap, PROPERTIES_DISAGGPATHTOD);
		String broadFactorsFile = disaggTODPath + Util.getStringValueFromPropertyMap(rbMap, PROPERTIES_BROADTODPROBABILITIES);
	
		TableDataSet BroadData = TableDataSet.readFile(broadFactorsFile);
		int numPeriods = BroadData.getRowCount();
		broadProbabilities = todDisaggregationModel.getTODProbabilities(BroadData, numPeriods, marketSegment);   
		broadTODMap = todDisaggregationModel.getTODMap(BroadData, numPeriods);
		
		disaggZonePath = Util.getStringValueFromPropertyMap(rbMap, PROPERTIES_DISAGGPATHZONE);
		String mgraFactorsFile = disaggZonePath + Util.getStringValueFromPropertyMap(rbMap, PROPERTIES_ZONEPROBABILITIES);
		TableDataSet MGRAData = TableDataSet.readFile(mgraFactorsFile);
		int numMGRAs = MGRAData.getRowCount();
		mgraProdProbabilities = spatialDisaggregationModel.getSpatialProbabilities(MGRAData, numMGRAs, "Prods", marketSegment);
		mgraAttrProbabilities = spatialDisaggregationModel.getSpatialProbabilities(MGRAData, numMGRAs, "Attrs", marketSegment);
		tazMap = spatialDisaggregationModel.getSpatialMap(MGRAData, numMGRAs, "taz");
		mgraTAZMap = spatialDisaggregationModel.getSpatialMap(MGRAData, numMGRAs, "mgra");
		
		disaggNodePath = Util.getStringValueFromPropertyMap(rbMap, PROPERTIES_DISAGGPATHNODE);
		String nodeFactorsFile = disaggNodePath + Util.getStringValueFromPropertyMap(rbMap, PROPERTIES_NODEPROBABILITIES);
		TableDataSet NodeData = TableDataSet.readFile(nodeFactorsFile);
		int numNodes = NodeData.getRowCount();
		nodeProbabilities = spatialDisaggregationModel.getSpatialProbabilities(NodeData, numNodes, "Probability", null);
		nodeMap = spatialDisaggregationModel.getSpatialMap(NodeData, numNodes, "NodeId");
		mgraNodeMap = spatialDisaggregationModel.getSpatialMap(NodeData, numNodes, "MGRA");
		
	}
	
		
	/**
	 * Create trip record from and disaggregate tod, mgra, and node for broad tod files
	 */
	public void createBroadTODTrips(String inputFileName,String MarketSegment,String matrixName,int broadTOD,String vehType,int occ,int toll){
			
		//Read TransCAD matrix and create a trip for each record in each cell
		File inputFile = new File(inputFileName);
		Matrix m = MatrixReader.readMatrix(inputFile,matrixName);
	    int intTrips = 0;
	    double expansionFactor=1.0;
	    int totalTrips = 0;
	    boolean debug=false;
	    
	    logger.info("*************************************");
	    logger.info("Summary info for TransCAD Matrix");
	    logger.info("Market Segment = "+MarketSegment);
	    logger.info("TNCVehicle Type = "+vehType);
	    logger.info("TNCVehicle Occupancy = "+occ);
	    logger.info("Toll Eligibility = "+toll);
	    logger.info("Number of Trips = "+m.getSum());
	    logger.info("*************************************");
	    
	    
		for (int i=1; i<=m.getRowCount();++i){
			for (int j=1; j<=m.getColumnCount();++j){
				
				double numTrips = m.getValueAt(i,j);

				if (numTrips==0.0)
					continue;

				intTrips = (int) Math.floor(numTrips);
				double fracTrips = numTrips - intTrips;
				double rn = random.nextDouble();
				// Check if a trip should be created for the fractional trip value
				if (rn<fracTrips)
					intTrips += 1;
				// Create trip records for each trip in the cell				
				for (int k=0; k<intTrips; ++k){
					Trip = new dtaTrip();
					Trip.initializeTrip();
					Trip.setExpansionFactor(expansionFactor);
					Trip.setMarketSegment(MarketSegment);
					Trip.setBroadPeriod(broadTOD);
					Trip.setOriginTaz(i);
					Trip.setDestinationTaz(j);
					Trip.setVehicleType(vehType);
					Trip.setVehicleOccupancy(occ);
					Trip.setTollEligible(toll);
					debug = isTraceOrigin(Trip.getOriginTaz());
					if (debug){
						logger.info("*******************************");
						logger.info("Disaggregating TOD for Trace Trip in Broad TOD");
						logger.info("Market Segment = "+Trip.getMarketSegment());
						logger.info("Household = "+Trip.getHHId());
						logger.info("Person = "+Trip.getPersonId());
						logger.info("Tour = "+Trip.getTourId());
						logger.info("Origin Zone = "+Trip.getOriginTaz());
						logger.info("Destination Zone = "+Trip.getDestinationTaz());
						logger.info("Trip period = "+Trip.getBroadPeriod());
						logger.info("Period     dtaPeriod     Prob     cumProb     randomNumber");						
					}
					int dtaPeriod = todDisaggregationModel.calculateDisaggregateTOD(broadTOD, broadTODMap, broadProbabilities, debug);
					Trip.setDTAPeriod(dtaPeriod);
					if (debug){
						logger.info("*******************************");
						logger.info("Choosing MGRA for Origin and Destination TAZs");
						logger.info("Market Segment = "+Trip.getMarketSegment());
						logger.info("Household = "+Trip.getHHId());
						logger.info("Person = "+Trip.getPersonId());
						logger.info("Tour = "+Trip.getTourId());
						logger.info("Origin Zone = "+Trip.getOriginTaz());
						logger.info("Destination Zone = "+Trip.getDestinationTaz());
						logger.info("TAZ    MGRA     Prob     cumProb     randomNumber");						
					}
					int origMGRA = spatialDisaggregationModel.selectMGRAfromTAZ(Trip.getOriginTaz(),mgraTAZMap,tazMap,mgraProdProbabilities,debug);
					int destMGRA = spatialDisaggregationModel.selectMGRAfromTAZ(Trip.getOriginTaz(),mgraTAZMap,tazMap,mgraAttrProbabilities,debug);
					Trip.setOriginMGRA(origMGRA);
					Trip.setDestinationMGRA(destMGRA);
					if (origMGRA==0){
						origMGRA = 50000+Trip.getOriginTaz();
					}
					if (destMGRA==0){
						destMGRA = 50000+Trip.getDestinationTaz();
					}
					int origNode = spatialDisaggregationModel.selectNodeFromMGRA(origMGRA, nodeMap, mgraNodeMap, nodeProbabilities, debug);
					int destNode = spatialDisaggregationModel.selectNodeFromMGRA(origMGRA, nodeMap, mgraNodeMap, nodeProbabilities, debug);
					Trip.setOriginNode(origNode);
					Trip.setDestinationNode(destNode);
					tripWriter.print("\r\n");
					Trip.writeTrip(tripWriter);
					
				}
				totalTrips += intTrips;
			}
		}
		logger.info("Created "+totalTrips+" trip records from matrix: "+inputFileName+", "+matrixName);	
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