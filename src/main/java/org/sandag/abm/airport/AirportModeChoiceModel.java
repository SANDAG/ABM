package org.sandag.abm.airport;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.BestTransitPathCalculator;
import org.sandag.abm.accessibilities.DriveTransitWalkSkimsCalculator;
import org.sandag.abm.accessibilities.WalkTransitDriveSkimsCalculator;
import org.sandag.abm.accessibilities.WalkTransitWalkSkimsCalculator;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.calculator.VariableTable;
import com.pb.common.newmodel.ChoiceModelApplication;
import com.pb.common.util.Tracer;

public class AirportModeChoiceModel
{
    private transient Logger                  logger = Logger.getLogger("airportModel");

    private TazDataManager                    tazManager;
    private MgraDataManager                   mgraManager;

    private ChoiceModelApplication[]          driveAloneModel;
    private ChoiceModelApplication[]          shared2Model;
    private ChoiceModelApplication[]          shared3Model;
    private ChoiceModelApplication            transitModel;
    private ChoiceModelApplication            accessModel;
    private ChoiceModelApplication			  mgraModel;

    private Tracer                            tracer;
    private boolean                           trace;
    private int[]                             traceOtaz;
    private int[]                             traceDtaz;
    private boolean                           seek;
    private HashMap<String, String>           rbMap;

    private BestTransitPathCalculator         bestPathUEC;
    protected WalkTransitWalkSkimsCalculator  wtw;
    protected WalkTransitDriveSkimsCalculator wtd;
    protected DriveTransitWalkSkimsCalculator dtw;

    /**
     * Constructor
     * 
     * @param propertyMap
     *            Resource properties file map.
     * @param dmuFactory
     *            Factory object for creation of airport model DMUs
     */
    public AirportModeChoiceModel(HashMap<String, String> rbMap, AirportDmuFactoryIf dmuFactory)
    {

        this.rbMap = rbMap;

        tazManager = TazDataManager.getInstance(rbMap);
        mgraManager = MgraDataManager.getInstance(rbMap);

        String uecFileDirectory = Util.getStringValueFromPropertyMap(rbMap,
                CtrampApplication.PROPERTIES_UEC_PATH);
        String airportModeUecFileName = Util.getStringValueFromPropertyMap(rbMap,
                "airport.mc.uec.file");
        airportModeUecFileName = uecFileDirectory + airportModeUecFileName;

        int dataPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "airport.mc.data.page"));
        int daPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "airport.mc.da.page"));
        int s2Page = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "airport.mc.s2.page"));
        int s3Page = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "airport.mc.s3.page"));
        int transitPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "airport.mc.transit.page"));
        int accessPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "airport.mc.accessMode.page"));
        int mgraPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap, 
        		"airport.mc.mgra.page"));

        logger.info("Creating Airport Model Mode Choice Application UECs");

        // create a DMU
        AirportModelDMU dmu = dmuFactory.getAirportModelDMU();
        
        // initialize mgra size array
        int maxMgra = mgraManager.getMaxMgra();
        dmu.setMaxMgra(maxMgra);
//        dmu.setTravelTimeArraySize();
        
        // fake choice model to get airport access MGRA input
        mgraModel = new ChoiceModelApplication(airportModeUecFileName, mgraPage, dataPage,
        		rbMap, (VariableTable) dmu);   

        // create ChoiceModelApplication objects for each airport mgra
        driveAloneModel = new ChoiceModelApplication[5];
        shared2Model = new ChoiceModelApplication[5];
        shared3Model = new ChoiceModelApplication[5];
        
        for (int i = 0; i < 5; i++){
        	// create a ChoiceModelApplication object for drive-alone mode choice
        	driveAloneModel[i] = new ChoiceModelApplication(airportModeUecFileName, daPage, dataPage,
                    rbMap, (VariableTable) dmu);
        	// create a ChoiceModelApplication object for shared 2 mode choice
        	shared2Model[i] = new ChoiceModelApplication(airportModeUecFileName, s2Page, dataPage,
                    rbMap, (VariableTable) dmu);
        	// create a ChoiceModelApplication object for shared 3+ mode choice
        	shared3Model[i] = new ChoiceModelApplication(airportModeUecFileName, s3Page, dataPage,
                    rbMap, (VariableTable) dmu);
        }
        
/*        
        // create a ChoiceModelApplication object for drive-alone mode choice
        driveAloneModel = new ChoiceModelApplication(airportModeUecFileName, daPage, dataPage,
                rbMap, (VariableTable) dmu);
        
        // create a ChoiceModelApplication object for shared 2 mode choice
        shared2Model = new ChoiceModelApplication(airportModeUecFileName, s2Page, dataPage, rbMap,
                (VariableTable) dmu);

        // create a ChoiceModelApplication object for shared 3+ mode choice
        shared3Model = new ChoiceModelApplication(airportModeUecFileName, s3Page, dataPage, rbMap,
                (VariableTable) dmu);
*/
        // create a ChoiceModelApplication object for transit mode choice
        transitModel = new ChoiceModelApplication(airportModeUecFileName, transitPage, dataPage,
                rbMap, (VariableTable) dmu);

        // create a ChoiceModelApplication object for access mode choice
        accessModel = new ChoiceModelApplication(airportModeUecFileName, accessPage, dataPage,
                rbMap, (VariableTable) dmu);

        logger.info("Finished Creating Airport Model Mode Choice Application UECs");

        // set up the tracer object
        trace = Util.getBooleanValueFromPropertyMap(rbMap, "Trace");
        traceOtaz = Util.getIntegerArrayFromPropertyMap(rbMap, "Trace.otaz");
        traceDtaz = Util.getIntegerArrayFromPropertyMap(rbMap, "Trace.dtaz");
        tracer = Tracer.getTracer();
        tracer.setTrace(trace);

        if (trace)
        {
            for (int i = 0; i < traceOtaz.length; i++)
            {
                for (int j = 0; j < traceDtaz.length; j++)
                {
                    tracer.traceZonePair(traceOtaz[i], traceDtaz[j]);
                }
            }
        }
        seek = Util.getBooleanValueFromPropertyMap(rbMap, "Seek");

    }

    /**
     * Create new transit skim calculators.
     */
    public void initializeBestPathCalculators()
    {

        logger.info("Initializing Airport Model Best Path Calculators");

        bestPathUEC = new BestTransitPathCalculator(rbMap);

        wtw = new WalkTransitWalkSkimsCalculator();
        wtw.setup(rbMap, logger, bestPathUEC);
        wtd = new WalkTransitDriveSkimsCalculator();
        wtd.setup(rbMap, logger, bestPathUEC);
        dtw = new DriveTransitWalkSkimsCalculator();
        dtw.setup(rbMap, logger, bestPathUEC);

        logger.info("Finished Initializing Airport Model Best Path Calculators");

    }
    
    public void solveModeMgra(AirportModelDMU dmu){  //question for Jim -- Do I need dmu index for this one?
    	mgraModel.computeUtilities(dmu, dmu.getDmuIndex());
    	String[] modeNames = mgraModel.getAlternativeNames();
    	int modeCount = mgraModel.getNumberOfAlternatives();
    	double[] mgraValues = mgraModel.getUtilities();
    	
    	HashMap<Integer, Integer> modeMgraMap = new HashMap<Integer, Integer>();
    	
    	for (int m = 0; m < modeCount; m++){
    		int mgraValue = (int)Math.round(mgraValues[m]);
    		modeMgraMap.put(m+1, mgraValue);
    	}
    	
    	dmu.setModeMgraMap(modeMgraMap);
    	dmu.setMgraIndexMap();
    	dmu.setTravelTimeArraySize();
    }

    /**
     * Choose airport arrival mode and trip mode for this party. Store results
     * in the party object passed as argument.
     * 
     * @param party
     *            The travel party
     * @param dmu
     *            An airport model DMU
     */
    public void chooseMode(AirportParty party, AirportModelDMU dmu)
    {

        int origMgra = party.getOriginMGRA();
        int destMgra = party.getDestinationMGRA();
        int direction = party.getDirection();
        int airportMgra = 0;
        int airportMgra_index = 0;
        int nonAirportMgra = 0;
        int accessOrigMgra = 0;
        int accessDestMgra = 0;
        int accessOrigTaz = 0;
        int accessDestTaz = 0;
        if (direction == 0){ //departure
        	nonAirportMgra = origMgra;
        } else { //arrival
        	nonAirportMgra = destMgra;
        }
        dmu.setNonAirportMgra(nonAirportMgra);
        dmu.setDirection(direction);
        int origTaz = mgraManager.getTaz(origMgra);
        int destTaz = mgraManager.getTaz(destMgra);   
        int period = party.getDepartTime();
        int skimPeriod = AirportModelStructure.getSkimPeriodIndex(period) + 1; // The
                                                                               // skims
                                                                               // are
                                                                               // stored
                                                                               // 1-based...don't
                                                                               // ask...
        boolean debug = party.getDebugChoiceModels();

        // calculate best tap pairs for this airport party
        int[][] walkTransitTapPairs = wtw.getBestTapPairs(origMgra, destMgra, skimPeriod, debug,
                logger);
        party.setBestWtwTapPairs(walkTransitTapPairs);

        // drive transit tap pairs depend on direction; departing parties use
        // drive-transit-walk, else walk-transit-drive is used.
        int[][] driveTransitTapPairs;
        if (party.getDirection() == AirportModelStructure.DEPARTURE)
        {
            driveTransitTapPairs = dtw.getBestTapPairs(origMgra, destMgra, skimPeriod, debug,
                    logger);
            party.setBestDtwTapPairs(driveTransitTapPairs);
        } else
        {
            driveTransitTapPairs = wtd.getBestTapPairs(origMgra, destMgra, skimPeriod, debug,
                    logger);
            party.setBestWtdTapPairs(driveTransitTapPairs);
        }

        // set transit skim values in DMU object
        dmu.setDmuSkimCalculators(wtw, wtd, dtw);
        boolean inbound = false;
        if (party.getDirection() == AirportModelStructure.ARRIVAL) inbound = true;

        dmu.setAirportParty(party);
        //dmu.setDmuIndexValues(party.getID(), origTaz, destTaz);  // should this be access point Taz?
        dmu.setDmuSkimAttributes(origMgra, destMgra, period, inbound, debug);

        // Solve trip mode level utilities

        //Integer[] modeArray = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17};
        //int[] losArray = {0,1,2,3};
        
        for (int mode = 1; mode <= AirportModelStructure.ACCESS_MODES; mode++){
        	airportMgra = dmu.mode_mgra_map.get(mode);
        	//dmu.setAirportMgra(airportMgra);
        	airportMgra_index = dmu.mgra_index_map.get(airportMgra);
        	
        	if (direction == 0){ //departure
            	accessOrigMgra = origMgra;
            	accessDestMgra = airportMgra;
            } else { //arrival
            	accessOrigMgra = airportMgra;
            	accessOrigMgra = destMgra;
            }
        	
        	accessOrigTaz = mgraManager.getTaz(accessOrigMgra);
        	accessDestTaz = mgraManager.getTaz(accessDestMgra);
        	
        	dmu.setDmuIndexValues(party.getID(), accessOrigTaz, accessDestTaz);  // should this be access point Taz?
        	
        	for (int los = 0; los < AirportModelStructure.LOS_TYPE; los++){
        		double travelTime = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, los);
        		if (travelTime == 0){
        			if (los == 0){
        				driveAloneModel[airportMgra_index].computeUtilities(dmu, dmu.getDmuIndex());
        				double driveAloneLogsum = driveAloneModel[airportMgra_index].getLogsum();
        				dmu.setModeTravelTime(nonAirportMgra, airportMgra, direction, los, driveAloneLogsum);
        			}
        			else if (los == 1){
        				shared2Model[airportMgra_index].computeUtilities(dmu, dmu.getDmuIndex());
        				double shared2Logsum = shared2Model[airportMgra_index].getLogsum();
        	            dmu.setModeTravelTime(nonAirportMgra, airportMgra, direction, los, shared2Logsum);
        			}
        			else if (los == 2){
        				shared3Model[airportMgra_index].computeUtilities(dmu, dmu.getDmuIndex());
        	            double shared3Logsum = shared3Model[airportMgra_index].getLogsum();
        	            dmu.setModeTravelTime(nonAirportMgra, airportMgra, direction, los, shared3Logsum);
        			}
        			else {
        				transitModel.computeUtilities(dmu, dmu.getDmuIndex());
        		        double transitLogsum = transitModel.getLogsum();
        		        dmu.setModeTravelTime(nonAirportMgra, airportMgra, direction, los, transitLogsum);
        			}
        		}
        	}
        }
/*               
        airportMgra = dmu.mode_mgra_map.get(1);
        double ParkLoc1DriveAloneLogsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 1);
        dmu.setParkLoc1DriveAloneLogsum(ParkLoc1DriveAloneLogsum);
        double ParkLoc1Shared2Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 2);
        dmu.setParkLoc1Shared2Logsum(ParkLoc1Shared2Logsum);
       	double ParkLoc1Shared3Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 3);
       	dmu.setParkLoc1Shared3Logsum(ParkLoc1Shared3Logsum);

       	airportMgra = dmu.mode_mgra_map.get(2);
       	double ParkLoc2DriveAloneLogsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 1);
       	dmu.setParkLoc2DriveAloneLogsum(ParkLoc2DriveAloneLogsum);
       	double ParkLoc2Shared2Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 2);
       	dmu.setParkLoc2Shared2Logsum(ParkLoc2Shared2Logsum);
       	double ParkLoc2Shared3Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 3);
       	dmu.setParkLoc2Shared3Logsum(ParkLoc2Shared3Logsum);
       	
       	airportMgra = dmu.mode_mgra_map.get(3);
       	double ParkLoc3DriveAloneLogsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 1);
       	dmu.setParkLoc3DriveAloneLogsum(ParkLoc3DriveAloneLogsum);
       	double ParkLoc3Shared2Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 2);
       	dmu.setParkLoc3Shared2Logsum(ParkLoc3Shared2Logsum);
       	double ParkLoc3Shared3Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 3);
       	dmu.setParkLoc3Shared3Logsum(ParkLoc3Shared3Logsum);
       	
       	airportMgra = dmu.mode_mgra_map.get(4);
       	double ParkLoc4DriveAloneLogsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 1);
       	dmu.setParkLoc4DriveAloneLogsum(ParkLoc4DriveAloneLogsum);
       	double ParkLoc4Shared2Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 2);
       	dmu.setParkLoc4Shared2Logsum(ParkLoc4Shared2Logsum);
       	double ParkLoc4Shared3Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 3);
       	dmu.setParkLoc4Shared3Logsum(ParkLoc4Shared3Logsum);
       	
       	airportMgra = dmu.mode_mgra_map.get(5);
       	double ParkLoc5DriveAloneLogsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 1);
       	dmu.setParkLoc5DriveAloneLogsum(ParkLoc5DriveAloneLogsum);
       	double ParkLoc5Shared2Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 2);
       	dmu.setParkLoc5Shared2Logsum(ParkLoc5Shared2Logsum);
       	double ParkLoc5Shared3Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 3);
       	dmu.setParkLoc5Shared3Logsum(ParkLoc5Shared3Logsum);
       	
       	airportMgra = dmu.mode_mgra_map.get(6);
       	double ParkESCShared2Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 2);
       	dmu.setParkESCShared2Logsum(ParkESCShared2Logsum);
       	double ParkESCShared3Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 3);
       	dmu.setParkESCShared3Logsum(ParkESCShared3Logsum);
       	
       	airportMgra = dmu.mode_mgra_map.get(7);
       	double RentalDriveAloneLogsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 1);
       	dmu.setRentalDriveAloneLogsum(RentalDriveAloneLogsum);
       	double RentalShared2Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 2);
       	dmu.setRentalShared2Logsum(RentalShared2Logsum);
       	double RentalShared3Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 3);
       	dmu.setRentalShared3Logsum(RentalShared3Logsum);
       	
       	airportMgra = dmu.mode_mgra_map.get(8);
       	double ShuttleVanShared2Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 2);
       	dmu.setShuttleVanShared2Logsum(ShuttleVanShared2Logsum);
       	double ShuttleVanShared3Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 3);
       	dmu.setShuttleVanShared3Logsum(ShuttleVanShared3Logsum);

       	airportMgra = dmu.mode_mgra_map.get(9);
       	double HotelCourtesyShared2Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 2);
       	dmu.setHotelCourtesyShared2Logsum(HotelCourtesyShared2Logsum);
       	double HotelCourtesyShared3Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 3);
       	dmu.setHotelCourtesyShared3Logsum(HotelCourtesyShared3Logsum);
       	
       	airportMgra = dmu.mode_mgra_map.get(10);
       	double RideHailingLoc1Shared2Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 2);
       	dmu.setRideHailingLoc1Shared2Logsum(RideHailingLoc1Shared2Logsum);
       	double RideHailingLoc1Shared3Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 3);
       	dmu.setRideHailingLoc1Shared3Logsum(RideHailingLoc1Shared3Logsum);
       	
       	airportMgra = dmu.mode_mgra_map.get(11);
       	double RideHailingLoc2Shared2Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 2);
       	dmu.setRideHailingLoc2Shared2Logsum(RideHailingLoc2Shared2Logsum);
       	double RideHailingLoc2Shared3Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 3);
       	dmu.setRideHailingLoc2Shared3Logsum(RideHailingLoc2Shared3Logsum);
       	
       	airportMgra = dmu.mode_mgra_map.get(12);
       	double TransitLogsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 4);
       	dmu.setTransitLogsum(TransitLogsum);

       	airportMgra = dmu.mode_mgra_map.get(13);
       	double CurbLoc1Shared2Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 2);
       	dmu.setCurbLoc1Shared2Logsum(CurbLoc1Shared2Logsum);
       	double CurbLoc1Shared3Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 3);
       	dmu.setCurbLoc1Shared3Logsum(CurbLoc1Shared3Logsum);
       	
       	airportMgra = dmu.mode_mgra_map.get(14);
       	double CurbLoc2Shared2Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 2);
       	dmu.setCurbLoc2Shared2Logsum(CurbLoc2Shared2Logsum);
       	double CurbLoc2Shared3Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 3);
       	dmu.setCurbLoc2Shared3Logsum(CurbLoc2Shared3Logsum);
       	
       	airportMgra = dmu.mode_mgra_map.get(15);
       	double CurbLoc3Shared2Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 2);
       	dmu.setCurbLoc3Shared2Logsum(CurbLoc3Shared2Logsum);
       	double CurbLoc3Shared3Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 3);
       	dmu.setCurbLoc3Shared3Logsum(CurbLoc3Shared3Logsum);
       	
       	airportMgra = dmu.mode_mgra_map.get(16);
       	double CurbLoc4Shared2Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 2);
       	dmu.setCurbLoc4Shared2Logsum(CurbLoc4Shared2Logsum);
       	double CurbLoc4Shared3Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 3);
       	dmu.setCurbLoc4Shared3Logsum(CurbLoc4Shared3Logsum);
       	
       	airportMgra = dmu.mode_mgra_map.get(17);
       	double CurbLoc5Shared2Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 2);
       	dmu.setCurbLoc5Shared2Logsum(CurbLoc5Shared2Logsum);
       	double CurbLoc5Shared3Logsum = dmu.getModeTravelTime(nonAirportMgra, airportMgra, direction, 3);
       	dmu.setCurbLoc5Shared3Logsum(CurbLoc5Shared3Logsum);
 */
 
 /*      	
        // if 1-person party, solve for the drive-alone and 2-person logsum
        if (party.getSize() == 1)
        {
            driveAloneModel.computeUtilities(dmu, dmu.getDmuIndex());
            double driveAloneLogsum = driveAloneModel.getLogsum();
            dmu.setDriveAloneLogsum(driveAloneLogsum);

            shared2Model.computeUtilities(dmu, dmu.getDmuIndex());
            double shared2Logsum = shared2Model.getLogsum();
            dmu.setShared2Logsum(shared2Logsum);

        } else if (party.getSize() == 2)
        { // if 2-person party solve for the
          // shared 2 and shared 3+ logsums
            shared2Model.computeUtilities(dmu, dmu.getDmuIndex());
            double shared2Logsum = shared2Model.getLogsum();
            dmu.setShared2Logsum(shared2Logsum);

            shared3Model.computeUtilities(dmu, dmu.getDmuIndex());
            double shared3Logsum = shared3Model.getLogsum();
            dmu.setShared3Logsum(shared3Logsum);

        } else
        { // if 3+ person party, solve the shared 3+ logsums
        	            
            shared3Model.computeUtilities(dmu, dmu.getDmuIndex());
            double shared3Logsum = shared3Model.getLogsum();
            dmu.setShared3Logsum(shared3Logsum);    
            
         // add debug
            if (party.getID() == 2)
            {
            	String choiceModelDescription = "";
                String decisionMakerLabel = "";
                String loggingHeader = "";
                String separator = "";
                 
            	choiceModelDescription = String.format(
                        "Airport Mode Choice Model for: Purpose=%s, OrigMGRA=%d, DestMGRA=%d",
                        party.getPurpose(), party.getOriginMGRA(), party.getDestinationMGRA());
                decisionMakerLabel = String.format("partyID=%d, partySize=%d, purpose=%s, direction=%d",
                        party.getID(), party.getSize(),
                        party.getPurpose(), party.getDirection());
                loggingHeader = String.format("%s    %s", choiceModelDescription,
                        decisionMakerLabel);
                
                logger.info(loggingHeader);
                shared3Model.logUECResults(logger);
            }
        
        }
        // always solve for the transit logsum
        transitModel.computeUtilities(dmu, dmu.getDmuIndex());
        double transitLogsum = transitModel.getLogsum();
        dmu.setTransitLogsum(transitLogsum);
*/
        // calculate access mode utility and choose access mode
        accessModel.computeUtilities(dmu, dmu.getDmuIndex());
        int accessMode = accessModel.getChoiceResult(party.getRandom());
        party.setArrivalMode((byte) accessMode);

        // choose trip mode
        int tripMode = 0;
        int occupancy = AirportModelStructure.getOccupancy(accessMode, party.getSize());
        double randomNumber = party.getRandom();

        if (accessMode != AirportModelStructure.TRANSIT)
        {
        	int chosenAirportMgra = dmu.mode_mgra_map.get(accessMode);
        	int chosenAirportMgra_index = dmu.mgra_index_map.get(chosenAirportMgra);
            if (occupancy == 1)
            {
                int choice = driveAloneModel[chosenAirportMgra_index].getChoiceResult(randomNumber);
                tripMode = choice;
            } else if (occupancy == 2)
            {
                int choice = shared2Model[chosenAirportMgra_index].getChoiceResult(randomNumber);
                tripMode = choice + 2;
            } else if (occupancy > 2)
            {
                int choice = shared3Model[chosenAirportMgra_index].getChoiceResult(randomNumber);
                tripMode = choice + 3 + 2;
            }
        } else
        {
            int choice = transitModel.getChoiceResult(randomNumber);
            if (choice <= 5) tripMode = choice + 10;
            else tripMode = choice + 15;
        }
        party.setMode((byte) tripMode);
    }

    /**
     * Choose modes for internal trips.
     * 
     * @param airportParties
     *            An array of travel parties, with destinations already chosen.
     * @param dmuFactory
     *            A DMU Factory.
     */
    public void chooseModes(AirportParty[] airportParties, AirportDmuFactoryIf dmuFactory)
    {

        AirportModelDMU dmu = dmuFactory.getAirportModelDMU();
        solveModeMgra(dmu);
        // iterate through the array, choosing mgras and setting them
        for (AirportParty party : airportParties)
        {

            int ID = party.getID();

            if ((ID <= 5) || (ID % 100) == 0)
                logger.info("Choosing mode for party " + party.getID());

            if (party.getPurpose() < AirportModelStructure.INTERNAL_PURPOSES) chooseMode(party, dmu);
            else
            {
                party.setArrivalMode((byte) -99);
                party.setMode((byte) -99);
            }
        }
    }

    /**
     * @param wtw
     *            the wtw to set
     */
    public void setWtw(WalkTransitWalkSkimsCalculator wtw)
    {
        this.wtw = wtw;
    }

    /**
     * @param wtd
     *            the wtd to set
     */
    public void setWtd(WalkTransitDriveSkimsCalculator wtd)
    {
        this.wtd = wtd;
    }

    /**
     * @param dtw
     *            the dtw to set
     */
    public void setDtw(DriveTransitWalkSkimsCalculator dtw)
    {
        this.dtw = dtw;
    }

}
