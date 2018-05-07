package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoAndNonMotorizedSkimsCalculator;
import org.sandag.abm.accessibilities.BestTransitPathCalculator;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import org.sandag.abm.modechoice.TransitDriveAccessDMU;
import org.sandag.abm.modechoice.TransitWalkAccessDMU;

import com.pb.common.newmodel.Alternative;
import com.pb.common.newmodel.ChoiceModelApplication;
import com.pb.common.newmodel.ConcreteAlternative;
import com.pb.common.newmodel.LogitModel;
import com.pb.common.calculator.IndexValues;


public class McLogsumsCalculator implements Serializable
{

    private transient Logger                autoSkimLogger                   = Logger.getLogger(McLogsumsCalculator.class);

    public static final String              PROPERTIES_UEC_TOUR_MODE_CHOICE  = "tourModeChoice.uec.file";
    public static final String              PROPERTIES_UEC_TRIP_MODE_CHOICE  = "tripModeChoice.uec.file";


    public static final int                   WTW = 0;
    public static final int                   WTD = 1;
    public static final int                   DTW = 2;
    public static final int                   NUM_ACC_EGR = 3;
    
    public static final int                   OUT = 0;
    public static final int                   IN = 1;
    public static final int                   NUM_DIR = 2;

    private BestTransitPathCalculator          bestPathUEC;
    private double[]                           tripModeChoiceSegmentStoredProbabilities;

    
    private TazDataManager                     tazManager;
    private MgraDataManager                    mgraManager;

    private double[]                           lsWgtAvgCostM;
    private double[]                           lsWgtAvgCostD;
    private double[]                           lsWgtAvgCostH;
    private int[]                              parkingArea;
    
    private double[][]                            bestWtwTapPairsOut;
    private double[][]                            bestWtwTapPairsIn;
    private double[][]                            bestWtdTapPairsOut;
    private double[][]                            bestWtdTapPairsIn;
    private double[][]                            bestDtwTapPairsOut;
    private double[][]                            bestDtwTapPairsIn;
           
    private double[][]                            bestWtwTripTapPairs;
    private double[][]                            bestWtdTripTapPairs;
    private double[][]                            bestDtwTripTapPairs;
        
    private AutoAndNonMotorizedSkimsCalculator anm;

    private int                                setTourMcLogsumDmuAttributesTotalTime = 0;
    private int                                setTripMcLogsumDmuAttributesTotalTime = 0;


    
    public McLogsumsCalculator()
    {
        if (mgraManager == null)
            mgraManager = MgraDataManager.getInstance();    
            
        if (tazManager == null)
            tazManager = TazDataManager.getInstance();

        this.lsWgtAvgCostM = mgraManager.getLsWgtAvgCostM();
        this.lsWgtAvgCostD = mgraManager.getLsWgtAvgCostD();
        this.lsWgtAvgCostH = mgraManager.getLsWgtAvgCostH();
        this.parkingArea = mgraManager.getMgraParkAreas();
    }
    
    
    public BestTransitPathCalculator getBestTransitPathCalculator()
    {
        return bestPathUEC;
    }
    
    
    public void setupSkimCalculators(HashMap<String, String> rbMap)
    {
        bestPathUEC = new BestTransitPathCalculator(rbMap);
        anm = new AutoAndNonMotorizedSkimsCalculator(rbMap);
    }

    public void setTazDistanceSkimArrays( double[][][] storedFromTazDistanceSkims, double[][][] storedToTazDistanceSkims ) {     
        anm.setTazDistanceSkimArrays( storedFromTazDistanceSkims, storedToTazDistanceSkims );                                                                
    }                                                                                                                            
                                                                                                                                 
                                                                                                                                 
    public AutoAndNonMotorizedSkimsCalculator getAnmSkimCalculator()
    {
        return anm;
    }

    public void setTourMcDmuAttributes( TourModeChoiceDMU mcDmuObject, int origMgra, int destMgra, int departPeriod, int arrivePeriod, boolean debug )
    {
    
        setNmTourMcDmuAttributes(  mcDmuObject, origMgra, destMgra, departPeriod, arrivePeriod, debug );
        setWtwTourMcDmuAttributes( mcDmuObject, origMgra, destMgra, departPeriod, arrivePeriod, debug );        
        setDtwTourMcDmuAttributes( mcDmuObject, origMgra, destMgra, departPeriod, arrivePeriod, debug );        
        setWtdTourMcDmuAttributes( mcDmuObject, origMgra, destMgra, departPeriod, arrivePeriod, debug );        

        // set the land use data items in the DMU for the origin
        mcDmuObject.setOrigDuDen( mgraManager.getDuDenValue( origMgra ) );
        mcDmuObject.setOrigEmpDen( mgraManager.getEmpDenValue( origMgra ) );
        mcDmuObject.setOrigTotInt( mgraManager.getTotIntValue( origMgra ) );

        // set the land use data items in the DMU for the destination
        mcDmuObject.setDestDuDen( mgraManager.getDuDenValue( destMgra ) );
        mcDmuObject.setDestEmpDen( mgraManager.getEmpDenValue( destMgra ) );
        mcDmuObject.setDestTotInt( mgraManager.getTotIntValue( destMgra ) );
        
        mcDmuObject.setLsWgtAvgCostM( lsWgtAvgCostM[destMgra] );
        mcDmuObject.setLsWgtAvgCostD( lsWgtAvgCostD[destMgra] );
        mcDmuObject.setLsWgtAvgCostH( lsWgtAvgCostH[destMgra] );
       
        int tourOrigTaz = mgraManager.getTaz(origMgra);
        int tourDestTaz = mgraManager.getTaz(destMgra);
        
        mcDmuObject.setPTazTerminalTime( tazManager.getOriginTazTerminalTime(tourOrigTaz) );
        mcDmuObject.setATazTerminalTime( tazManager.getDestinationTazTerminalTime(tourDestTaz) );
        
        Person person = mcDmuObject.getPersonObject();
        
        double reimbursePct=0;
        if(person!=null) { 
        	reimbursePct = person.getParkingReimbursement();
        }
        
        mcDmuObject.setReimburseProportion( reimbursePct );
        mcDmuObject.setParkingArea(parkingArea[destMgra]);
 

    }
    
    
    public double calculateTourMcLogsum(int origMgra, int destMgra, int departPeriod, int arrivePeriod,
        ChoiceModelApplication mcModel, TourModeChoiceDMU mcDmuObject)
    {
        
        long currentTime = System.currentTimeMillis();        
        setTourMcDmuAttributes( mcDmuObject, origMgra, destMgra, departPeriod, arrivePeriod, mcDmuObject.getDmuIndexValues().getDebug() );
        setTourMcLogsumDmuAttributesTotalTime += ( System.currentTimeMillis() - currentTime );
    
        // mode choice UEC references highway skim matrices directly, so set index orig/dest to O/D TAZs.
        IndexValues mcDmuIndex = mcDmuObject.getDmuIndexValues();
        int tourOrigTaz = mgraManager.getTaz(origMgra);
        int tourDestTaz = mgraManager.getTaz(destMgra);
        mcDmuIndex.setOriginZone(tourOrigTaz);
        mcDmuIndex.setDestZone(tourDestTaz);
        mcDmuObject.setOriginMgra(origMgra);
        mcDmuObject.setDestMgra(destMgra);
    
        mcModel.computeUtilities(mcDmuObject, mcDmuIndex);
        double logsum = mcModel.getLogsum();
    
        return logsum;
        
    }

    public void setWalkTransitLogSumUnavailable( TripModeChoiceDMU tripMcDmuObject ) {
    	tripMcDmuObject.setTransitLogSum( WTW, bestPathUEC.NA );
    }
    
    public void setDriveTransitLogSumUnavailable( TripModeChoiceDMU tripMcDmuObject, boolean isInbound ) {
    	
    	// set drive transit skim attributes to unavailable
        if ( ! isInbound ) {
        	tripMcDmuObject.setTransitLogSum( DTW, bestPathUEC.NA);
        }
        else {
        	tripMcDmuObject.setTransitLogSum( WTD, bestPathUEC.NA);
        }

    }
    
    
    public double calculateTripMcLogsum(int origMgra, int destMgra, int departPeriod, ChoiceModelApplication mcModel, TripModeChoiceDMU mcDmuObject, Logger myLogger)
    {
        long currentTime = System.currentTimeMillis();
        setNmTripMcDmuAttributes(  mcDmuObject, origMgra, destMgra, departPeriod, mcDmuObject.getHouseholdObject().getDebugChoiceModels() );

        // set the land use data items in the DMU for the origin
        mcDmuObject.setOrigDuDen( mgraManager.getDuDenValue( origMgra ) );
        mcDmuObject.setOrigEmpDen( mgraManager.getEmpDenValue( origMgra ) );
        mcDmuObject.setOrigTotInt( mgraManager.getTotIntValue( origMgra ) );

        // set the land use data items in the DMU for the destination
        mcDmuObject.setDestDuDen( mgraManager.getDuDenValue( destMgra ) );
        mcDmuObject.setDestEmpDen( mgraManager.getEmpDenValue( destMgra ) );
        mcDmuObject.setDestTotInt( mgraManager.getTotIntValue( destMgra ) );
        
        // mode choice UEC references highway skim matrices directly, so set index orig/dest to O/D TAZs.
        IndexValues mcDmuIndex = mcDmuObject.getDmuIndexValues();
        mcDmuIndex.setOriginZone(mgraManager.getTaz(origMgra));
        mcDmuIndex.setDestZone(mgraManager.getTaz(destMgra));
        mcDmuObject.setOriginMgra(origMgra);
        mcDmuObject.setDestMgra(destMgra);
        
        setTripMcLogsumDmuAttributesTotalTime += ( System.currentTimeMillis() - currentTime );
        mcDmuObject.setPTazTerminalTime( tazManager.getOriginTazTerminalTime(mgraManager.getTaz(origMgra)) );
        mcDmuObject.setATazTerminalTime( tazManager.getDestinationTazTerminalTime(mgraManager.getTaz(destMgra)) );

        
        mcModel.computeUtilities(mcDmuObject, mcDmuIndex);
        double logsum = mcModel.getLogsum();
        tripModeChoiceSegmentStoredProbabilities = Arrays.copyOf( mcModel.getCumulativeProbabilities(), mcModel.getNumberOfAlternatives() );
        
        if ( mcDmuObject.getHouseholdObject().getDebugChoiceModels() )
            mcModel.logUECResults(myLogger, "Trip Mode Choice Utility Expressions for mgras: " + origMgra + " to " + destMgra + " for HHID: " + mcDmuIndex.getHHIndex() );
        
        return logsum;
        
    }

    
    /**
     * return the array of mode choice model cumulative probabilities determined while
     * computing the mode choice logsum for the trip segmen during stop location choice.
     * These probabilities arrays are stored for each sampled stop location so that when
     * the selected sample stop location is known, the mode choice can be drawn from the
     * already computed probabilities.
     *  
     * @return mode choice cumulative probabilities array
     */
    public double[] getStoredSegmentCumulativeProbabilities() {
        return tripModeChoiceSegmentStoredProbabilities;
    }

    public double[][] getBestWtwTapsOut()
    {
        return bestWtwTapPairsOut;
    }
    
    public double[][] getBestWtwTapsIn()
    {
        return bestWtwTapPairsIn;
    }
    
    public double[][] getBestWtdTapsOut()
    {
        return bestWtdTapPairsOut;
    }
    
    public double[][] getBestWtdTapsIn()
    {
        return bestWtdTapPairsIn;
    }
    
    public double[][] getBestDtwTapsOut()
    {
        return bestDtwTapPairsOut;
    }
    
    public double[][] getBestDtwTapsIn()
    {
        return bestDtwTapPairsIn;
    }
    
    public double[][] getBestWtwTripTaps()
    {
        return bestWtwTripTapPairs;
    }
    
    public double[][] getBestDtwTripTaps()
    {
        return bestDtwTripTapPairs;
    }
    
    public double[][] getBestWtdTripTaps()
    {
        return bestWtdTripTapPairs;
    }

    
    private void setNmTourMcDmuAttributes( TourModeChoiceDMU mcDmuObject, int origMgra, int destMgra, int departPeriod, int arrivePeriod, boolean loggingEnabled )
    {
        // non-motorized, outbound then inbound
        int skimPeriodIndex = ModelStructure.getSkimPeriodIndex(departPeriod);
        departPeriod = skimPeriodIndex;
        double[] nmSkimsOut = anm.getNonMotorizedSkims(origMgra, destMgra, departPeriod, loggingEnabled, autoSkimLogger);
        if (loggingEnabled)
            anm.logReturnedSkims(origMgra, destMgra, departPeriod, nmSkimsOut, "non-motorized outbound", autoSkimLogger);

        skimPeriodIndex = ModelStructure.getSkimPeriodIndex(arrivePeriod);
        arrivePeriod = skimPeriodIndex;
        double[] nmSkimsIn = anm.getNonMotorizedSkims(destMgra, origMgra, arrivePeriod, loggingEnabled, autoSkimLogger);
        if (loggingEnabled) anm.logReturnedSkims(destMgra, origMgra, arrivePeriod, nmSkimsIn, "non-motorized inbound", autoSkimLogger);
        
        int walkIndex = anm.getNmWalkTimeSkimIndex();
        mcDmuObject.setNmWalkTimeOut( nmSkimsOut[walkIndex] );
        mcDmuObject.setNmWalkTimeIn( nmSkimsIn[walkIndex] );

        int bikeIndex = anm.getNmBikeTimeSkimIndex();
        mcDmuObject.setNmBikeTimeOut( nmSkimsOut[bikeIndex] );
        mcDmuObject.setNmBikeTimeIn( nmSkimsIn[bikeIndex] );
        
    }

    private void setWtwTourMcDmuAttributes( TourModeChoiceDMU mcDmuObject, int origMgra, int destMgra, int departPeriod, int arrivePeriod, boolean loggingEnabled )
    {
        
    	//setup best path dmu variables
    	TransitWalkAccessDMU walkDmu =  new TransitWalkAccessDMU();
    	TransitDriveAccessDMU driveDmu  = new TransitDriveAccessDMU();
    	    	
    	// walk access, walk egress transit, outbound
        int skimPeriodIndexOut = ModelStructure.getSkimPeriodIndex(departPeriod);
        bestWtwTapPairsOut = bestPathUEC.getBestTapPairs(walkDmu, driveDmu, WTW, origMgra, destMgra, skimPeriodIndexOut, loggingEnabled, autoSkimLogger);
        
        if (bestWtwTapPairsOut[0] == null) {
        	mcDmuObject.setTransitLogSum( WTW, false, bestPathUEC.NA );
        } else {
        	// calculate logsum
        	
        	//set person specific variables and re-calculate best tap pair utilities
        	walkDmu.setApplicationType(bestPathUEC.APP_TYPE_TOURMC);
        	walkDmu.setIvtCoeff( (float) mcDmuObject.getIvtCoeff());
        	walkDmu.setCostCoeff( (float) mcDmuObject.getCostCoeff());
        	
        	driveDmu.setApplicationType(bestPathUEC.APP_TYPE_TOURMC);
        	driveDmu.setIvtCoeff( (float) mcDmuObject.getIvtCoeff());
        	driveDmu.setCostCoeff( (float) mcDmuObject.getCostCoeff());


        	//catch issues where the trip mode choice DMU was set up without a household or person object
        	if(mcDmuObject.getHouseholdObject()!=null){
        		walkDmu.setPersonType(mcDmuObject.getTourCategoryJoint()==1 ? walkDmu.getPersonType() : mcDmuObject.getPersonType());
        	   	driveDmu.setPersonType(mcDmuObject.getTourCategoryJoint()==1 ? driveDmu.getPersonType() : mcDmuObject.getPersonType());
          	}

        	bestWtwTapPairsOut = bestPathUEC.calcPersonSpecificUtilities(bestWtwTapPairsOut, walkDmu, driveDmu, WTW, origMgra, destMgra, skimPeriodIndexOut, loggingEnabled, autoSkimLogger);
        	double logsumOut = bestPathUEC.calcTripLogSum(bestWtwTapPairsOut, loggingEnabled, autoSkimLogger);
        	mcDmuObject.setTransitLogSum( WTW, false, logsumOut);
        }
        
        //setup best path dmu variables
    	walkDmu =  new TransitWalkAccessDMU();
    	driveDmu  = new TransitDriveAccessDMU();
        
        // walk access, walk egress transit, inbound
        int skimPeriodIndexIn = ModelStructure.getSkimPeriodIndex(arrivePeriod);
        bestWtwTapPairsIn = bestPathUEC.getBestTapPairs(walkDmu, driveDmu, WTW, destMgra, origMgra, skimPeriodIndexIn, loggingEnabled, autoSkimLogger);

        if (bestWtwTapPairsIn[0] == null) {
        	mcDmuObject.setTransitLogSum( WTW, true, bestPathUEC.NA );
        } else {
        	// calculate logsum
        	
        	//set person specific variables and re-calculate best tap pair utilities
        	walkDmu.setApplicationType(bestPathUEC.APP_TYPE_TOURMC);
        	walkDmu.setIvtCoeff( (float) mcDmuObject.getIvtCoeff());
        	walkDmu.setCostCoeff( (float) mcDmuObject.getCostCoeff());
        	
        	driveDmu.setApplicationType(bestPathUEC.APP_TYPE_TOURMC);
        	driveDmu.setIvtCoeff( (float) mcDmuObject.getIvtCoeff());
        	driveDmu.setCostCoeff( (float) mcDmuObject.getCostCoeff());


        	//catch issues where the trip mode choice DMU was set up without a household or person object
        	if(mcDmuObject.getHouseholdObject()!=null){
        		walkDmu.setPersonType(mcDmuObject.getTourCategoryJoint()==1 ? walkDmu.getPersonType() : mcDmuObject.getPersonType());
        	   	driveDmu.setPersonType(mcDmuObject.getTourCategoryJoint()==1 ? driveDmu.getPersonType() : mcDmuObject.getPersonType());
          	}

        	bestWtwTapPairsIn = bestPathUEC.calcPersonSpecificUtilities(bestWtwTapPairsIn, walkDmu, driveDmu, WTW, destMgra, origMgra, skimPeriodIndexIn, loggingEnabled, autoSkimLogger);
        	double logsumIn = bestPathUEC.calcTripLogSum(bestWtwTapPairsIn, loggingEnabled, autoSkimLogger);             
        	mcDmuObject.setTransitLogSum( WTW, true, logsumIn);
        }
    }

    private void setWtdTourMcDmuAttributes( TourModeChoiceDMU mcDmuObject, int origMgra, int destMgra, int departPeriod, int arrivePeriod, boolean loggingEnabled )
    {
        
    	//setup best path dmu variables
    	TransitWalkAccessDMU walkDmu =  new TransitWalkAccessDMU();
    	TransitDriveAccessDMU driveDmu  = new TransitDriveAccessDMU();
    	
    	// logsum for WTD outbound is never used -> set to NA
    	mcDmuObject.setTransitLogSum( WTD, false, bestPathUEC.NA );
    	/* TODO: - remove this section of code after successful testing 
    	// walk access, drive egress transit, outbound
        int skimPeriodIndexOut = ModelStructure.getSkimPeriodIndex(departPeriod);
        bestWtdTapPairsOut = bestPathUEC.getBestTapPairs(walkDmu, driveDmu, WTD, origMgra, destMgra, skimPeriodIndexOut, loggingEnabled, autoSkimLogger);
        
        if (bestWtdTapPairsOut[0] == null) {
        	mcDmuObject.setTransitLogSum( WTD, false, bestPathUEC.NA );
        } else {
        	// calculate logsum
        	
        	//set person specific variables and re-calculate best tap pair utilities
        	walkDmu.setApplicationType(bestPathUEC.APP_TYPE_TOURMC);
        	walkDmu.setTourCategoryIsJoint(mcDmuObject.getTourCategoryJoint());
        	walkDmu.setPersonType(mcDmuObject.getTourCategoryJoint()==1 ? walkDmu.personType : mcDmuObject.getPersonType());
        	walkDmu.setValueOfTime((float)mcDmuObject.getValueOfTime());
        	
        	driveDmu.setApplicationType(bestPathUEC.APP_TYPE_TOURMC);
        	driveDmu.setTourCategoryIsJoint(mcDmuObject.getTourCategoryJoint());
        	driveDmu.setPersonType(mcDmuObject.getTourCategoryJoint()==1 ? driveDmu.personType : mcDmuObject.getPersonType());
        	driveDmu.setValueOfTime((float)mcDmuObject.getValueOfTime());
        	
        	bestWtdTapPairsOut = bestPathUEC.calcPersonSpecificUtilities(bestWtdTapPairsOut, walkDmu, driveDmu, WTD, origMgra, destMgra, skimPeriodIndexOut, loggingEnabled, autoSkimLogger);
        	double logsumOut = bestPathUEC.calcTripLogSum(bestWtdTapPairsOut, loggingEnabled, autoSkimLogger);
        	mcDmuObject.setTransitLogSum( WTD, false, logsumOut);
        }
        */
    	
        // walk access, drive egress transit, inbound
        int skimPeriodIndexIn = ModelStructure.getSkimPeriodIndex(arrivePeriod);
        bestWtdTapPairsIn = bestPathUEC.getBestTapPairs(walkDmu, driveDmu, WTD, destMgra, origMgra, skimPeriodIndexIn, loggingEnabled, autoSkimLogger);

        if (bestWtdTapPairsIn[0] == null) {
        	mcDmuObject.setTransitLogSum( WTD, true, bestPathUEC.NA );
        } else {
        	// calculate logsum
        	
        	//set person specific variables and re-calculate best tap pair utilities
        	walkDmu.setApplicationType(bestPathUEC.APP_TYPE_TOURMC);
        	walkDmu.setIvtCoeff( (float) mcDmuObject.getIvtCoeff());
        	walkDmu.setCostCoeff( (float) mcDmuObject.getCostCoeff());
        	
        	driveDmu.setApplicationType(bestPathUEC.APP_TYPE_TOURMC);
        	driveDmu.setIvtCoeff( (float) mcDmuObject.getIvtCoeff());
        	driveDmu.setCostCoeff( (float) mcDmuObject.getCostCoeff());
        	
           	//catch issues where the trip mode choice DMU was set up without a household or person object
        	if(mcDmuObject.getHouseholdObject()!=null){
        		walkDmu.setPersonType(mcDmuObject.getTourCategoryJoint()==1 ? walkDmu.getPersonType() : mcDmuObject.getPersonType());
        	   	driveDmu.setPersonType(mcDmuObject.getTourCategoryJoint()==1 ? driveDmu.getPersonType() : mcDmuObject.getPersonType());
          	}
            bestWtdTapPairsIn = bestPathUEC.calcPersonSpecificUtilities(bestWtdTapPairsIn, walkDmu, driveDmu, WTD, destMgra, origMgra, skimPeriodIndexIn, loggingEnabled, autoSkimLogger);
        	double logsumIn = bestPathUEC.calcTripLogSum(bestWtdTapPairsIn, loggingEnabled, autoSkimLogger);
        	mcDmuObject.setTransitLogSum( WTD, true, logsumIn);
        }
    }

    private void setDtwTourMcDmuAttributes( TourModeChoiceDMU mcDmuObject, int origMgra, int destMgra, int departPeriod, int arrivePeriod, boolean loggingEnabled )
    {
    	//setup best path dmu variables
    	TransitWalkAccessDMU walkDmu =  new TransitWalkAccessDMU();
    	TransitDriveAccessDMU driveDmu  = new TransitDriveAccessDMU();
    	
    	// drive access, walk egress transit, outbound
        int skimPeriodIndexOut = ModelStructure.getSkimPeriodIndex(departPeriod);
        bestDtwTapPairsOut = bestPathUEC.getBestTapPairs(walkDmu, driveDmu, DTW, origMgra, destMgra, skimPeriodIndexOut, loggingEnabled, autoSkimLogger);
        
        if (bestDtwTapPairsOut[0] == null) {
        	mcDmuObject.setTransitLogSum( DTW, false, bestPathUEC.NA );
        } else {
        	// calculate logsum
        	
        	//set person specific variables and re-calculate best tap pair utilities
        	walkDmu.setApplicationType(bestPathUEC.APP_TYPE_TOURMC);
        	walkDmu.setIvtCoeff( (float) mcDmuObject.getIvtCoeff());
        	walkDmu.setCostCoeff( (float) mcDmuObject.getCostCoeff());
        	
        	driveDmu.setApplicationType(bestPathUEC.APP_TYPE_TOURMC);
        	driveDmu.setIvtCoeff( (float) mcDmuObject.getIvtCoeff());
        	driveDmu.setCostCoeff( (float) mcDmuObject.getCostCoeff());
      
        	//catch issues where the trip mode choice DMU was set up without a household or person object
        	if(mcDmuObject.getHouseholdObject()!=null){
        		walkDmu.setPersonType(mcDmuObject.getTourCategoryJoint()==1 ? walkDmu.getPersonType() : mcDmuObject.getPersonType());
        	   	driveDmu.setPersonType(mcDmuObject.getTourCategoryJoint()==1 ? driveDmu.getPersonType() : mcDmuObject.getPersonType());
          	}
      	
        	bestDtwTapPairsOut = bestPathUEC.calcPersonSpecificUtilities(bestDtwTapPairsOut, walkDmu, driveDmu, DTW, origMgra, destMgra, skimPeriodIndexOut, loggingEnabled, autoSkimLogger);
        	double logsumOut = bestPathUEC.calcTripLogSum(bestDtwTapPairsOut, loggingEnabled, autoSkimLogger);
        	mcDmuObject.setTransitLogSum( DTW, false, logsumOut);
        }
        
    	// logsum for DTW inbound is never used -> set to NA
    	mcDmuObject.setTransitLogSum( DTW, true, bestPathUEC.NA );
        
    	/* TODO: remove this section of code after successful testing 
        //setup best path dmu variables
    	walkDmu =  new TransitWalkAccessDMU();
    	driveDmu  = new TransitDriveAccessDMU();
    	
        // drive access, walk egress transit, inbound
        int skimPeriodIndexIn = ModelStructure.getSkimPeriodIndex(arrivePeriod);
        bestDtwTapPairsIn = bestPathUEC.getBestTapPairs(walkDmu, driveDmu, DTW, destMgra, origMgra, skimPeriodIndexIn, loggingEnabled, autoSkimLogger);

        if (bestDtwTapPairsIn[0] == null) {
        	mcDmuObject.setTransitLogSum( DTW, true, bestPathUEC.NA );
        } else {
        	// calculate logsum
        	
        	//set person specific variables and re-calculate best tap pair utilities
        	walkDmu.setApplicationType(bestPathUEC.APP_TYPE_TOURMC);
        	walkDmu.setTourCategoryIsJoint(mcDmuObject.getTourCategoryJoint());
        	walkDmu.setPersonType(mcDmuObject.getTourCategoryJoint()==1 ? walkDmu.personType : mcDmuObject.getPersonType());
        	walkDmu.setValueOfTime((float)mcDmuObject.getValueOfTime());
        	
        	driveDmu.setApplicationType(bestPathUEC.APP_TYPE_TOURMC);
        	driveDmu.setTourCategoryIsJoint(mcDmuObject.getTourCategoryJoint());
        	driveDmu.setPersonType(mcDmuObject.getTourCategoryJoint()==1 ? driveDmu.personType : mcDmuObject.getPersonType());
        	driveDmu.setValueOfTime((float)mcDmuObject.getValueOfTime());
        	
        	bestDtwTapPairsIn = bestPathUEC.calcPersonSpecificUtilities(bestDtwTapPairsIn, walkDmu, driveDmu, DTW, destMgra, origMgra, skimPeriodIndexIn, loggingEnabled, autoSkimLogger);
        	double logsumIn = bestPathUEC.calcTripLogSum(bestDtwTapPairsIn, loggingEnabled, autoSkimLogger);
        	mcDmuObject.setTransitLogSum( DTW, true, logsumIn);
        }
        */
    }

    public void setNmTripMcDmuAttributes( TripModeChoiceDMU tripMcDmuObject, int origMgra, int destMgra, int departPeriod, boolean loggingEnabled )
    {

        double[] nmSkims = null;
        
        // non-motorized, outbound then inbound
        int skimPeriodIndex = ModelStructure.getSkimPeriodIndex(departPeriod);
        departPeriod = skimPeriodIndex;
        nmSkims = anm.getNonMotorizedSkims(origMgra, destMgra, departPeriod, loggingEnabled, autoSkimLogger);
        if (loggingEnabled)
            anm.logReturnedSkims(origMgra, destMgra, departPeriod, nmSkims, "non-motorized trip mode choice skims", autoSkimLogger);
    
        int walkIndex = anm.getNmWalkTimeSkimIndex();
        tripMcDmuObject.setNonMotorizedWalkTime(nmSkims[walkIndex] );
    
        int bikeIndex = anm.getNmBikeTimeSkimIndex();
        tripMcDmuObject.setNonMotorizedBikeTime(nmSkims[bikeIndex] );
        
    }
    
    public void setWtwTripMcDmuAttributesForBestTapPairs( TripModeChoiceDMU tripMcDmuObject, int origMgra, int destMgra, int departPeriod, double[][] bestTapPairs, boolean loggingEnabled)
    {

        if (bestTapPairs == null) {
        	tripMcDmuObject.setTransitLogSum( WTW, bestPathUEC.NA );
            bestWtwTripTapPairs = bestTapPairs;
            return;
        }

        // calculate logsum
        int skimPeriodIndex = ModelStructure.getSkimPeriodIndex(departPeriod);
        double logsum = bestPathUEC.calcTripLogSum(bestTapPairs, loggingEnabled, autoSkimLogger);
        tripMcDmuObject.setTransitLogSum( WTW, logsum);
        bestWtwTripTapPairs = bestTapPairs;
        
    }
    
    public void setDtwTripMcDmuAttributesForBestTapPairs( TripModeChoiceDMU tripMcDmuObject, int origMgra, int destMgra, int departPeriod, double[][] bestTapPairs, boolean loggingEnabled )
    {

        if (bestTapPairs == null) {
        	tripMcDmuObject.setTransitLogSum( DTW, bestPathUEC.NA );
            bestDtwTripTapPairs = bestTapPairs;
            return;
        }
                
        // calculate logsum
        int skimPeriodIndex = ModelStructure.getSkimPeriodIndex(departPeriod);
        double logsum = bestPathUEC.calcTripLogSum(bestTapPairs, loggingEnabled, autoSkimLogger);
        tripMcDmuObject.setTransitLogSum( DTW, logsum);
        bestDtwTripTapPairs = bestTapPairs;
        
    }
    
    public void setWtdTripMcDmuAttributesForBestTapPairs( TripModeChoiceDMU tripMcDmuObject, int origMgra, int destMgra, int departPeriod, double[][] bestTapPairs, boolean loggingEnabled )
    {

        if (bestTapPairs == null) {
        	tripMcDmuObject.setTransitLogSum( WTD, bestPathUEC.NA );
            bestWtdTripTapPairs = bestTapPairs;
            return;
        }
        
        // calculate logsum
        int skimPeriodIndex = ModelStructure.getSkimPeriodIndex(departPeriod);
        double logsum = bestPathUEC.calcTripLogSum(bestTapPairs, loggingEnabled, autoSkimLogger);
        tripMcDmuObject.setTransitLogSum( WTD, logsum);
        bestWtdTripTapPairs = bestTapPairs;
        
    }
    
    public void setWtwTripMcDmuAttributes( TripModeChoiceDMU tripMcDmuObject, int origMgra, int destMgra, int departPeriod, boolean loggingEnabled )
    {
    	//setup best path dmu variables
    	TransitWalkAccessDMU walkDmu =  new TransitWalkAccessDMU();
    	TransitDriveAccessDMU driveDmu  = new TransitDriveAccessDMU();
    	
        // walk access and walk egress for transit segment
        int skimPeriodIndex = ModelStructure.getSkimPeriodIndex(departPeriod);
        
        // store best tap pairs for walk-transit-walk
        bestWtwTripTapPairs = bestPathUEC.getBestTapPairs(walkDmu, driveDmu, WTW, origMgra, destMgra, skimPeriodIndex, loggingEnabled, autoSkimLogger );

        //set person specific variables and re-calculate best tap pair utilities
    	walkDmu.setApplicationType(bestPathUEC.APP_TYPE_TRIPMC);
     	walkDmu.setIvtCoeff( (float) tripMcDmuObject.getIvtCoeff());
    	walkDmu.setCostCoeff( (float) tripMcDmuObject.getCostCoeff());
    	
    	driveDmu.setApplicationType(bestPathUEC.APP_TYPE_TRIPMC);
    	driveDmu.setIvtCoeff( (float) tripMcDmuObject.getIvtCoeff());
    	driveDmu.setCostCoeff( (float) tripMcDmuObject.getCostCoeff());
    	
    	//catch issues where the trip mode choice DMU was set up without a household or person object
    	if(tripMcDmuObject.getHouseholdObject()!=null){
    		walkDmu.setPersonType(tripMcDmuObject.getTourCategoryJoint()==1 ? walkDmu.getPersonType() : tripMcDmuObject.getPersonType());
    	   	driveDmu.setPersonType(tripMcDmuObject.getTourCategoryJoint()==1 ? driveDmu.getPersonType() : tripMcDmuObject.getPersonType());
      	}
        // calculate logsum
    	bestWtwTripTapPairs = bestPathUEC.calcPersonSpecificUtilities(bestWtwTripTapPairs, walkDmu, driveDmu, WTW, origMgra, destMgra, skimPeriodIndex, loggingEnabled, autoSkimLogger);
        double logsum = bestPathUEC.calcTripLogSum(bestWtwTripTapPairs, loggingEnabled, autoSkimLogger);
        tripMcDmuObject.setTransitLogSum( WTW, logsum);
        
    }
    
    public void setWtdTripMcDmuAttributes( TripModeChoiceDMU tripMcDmuObject, int origMgra, int destMgra, int departPeriod, boolean loggingEnabled )
    {
    	//setup best path dmu variables
    	TransitWalkAccessDMU walkDmu =  new TransitWalkAccessDMU();
    	TransitDriveAccessDMU driveDmu  = new TransitDriveAccessDMU();
    	
        // walk access, drive egress transit, outbound
        int skimPeriodIndex = ModelStructure.getSkimPeriodIndex(departPeriod);
        
        // store best tap pairs using outbound direction array
        bestWtdTripTapPairs = bestPathUEC.getBestTapPairs(walkDmu, driveDmu, WTD, origMgra, destMgra, skimPeriodIndex, loggingEnabled, autoSkimLogger);
        
        //set person specific variables and re-calculate best tap pair utilities
        walkDmu.setApplicationType(bestPathUEC.APP_TYPE_TRIPMC);
    	walkDmu.setIvtCoeff( (float) tripMcDmuObject.getIvtCoeff());
    	walkDmu.setCostCoeff( (float) tripMcDmuObject.getCostCoeff());
    	
    	driveDmu.setApplicationType(bestPathUEC.APP_TYPE_TRIPMC);
    	driveDmu.setIvtCoeff( (float) tripMcDmuObject.getIvtCoeff());
    	driveDmu.setCostCoeff( (float) tripMcDmuObject.getCostCoeff());
  
    	//catch issues where the trip mode choice DMU was set up without a household or person object
    	if(tripMcDmuObject.getHouseholdObject()!=null){
    		walkDmu.setPersonType(tripMcDmuObject.getTourCategoryJoint()==1 ? walkDmu.getPersonType() : tripMcDmuObject.getPersonType());
    	   	driveDmu.setPersonType(tripMcDmuObject.getTourCategoryJoint()==1 ? driveDmu.getPersonType() : tripMcDmuObject.getPersonType());
      	}
  
        // calculate logsum
    	bestWtdTripTapPairs = bestPathUEC.calcPersonSpecificUtilities(bestWtdTripTapPairs, walkDmu, driveDmu, WTD, origMgra, destMgra, skimPeriodIndex, loggingEnabled, autoSkimLogger);
        double logsum = bestPathUEC.calcTripLogSum(bestWtdTripTapPairs, loggingEnabled, autoSkimLogger);
        tripMcDmuObject.setTransitLogSum( WTD, logsum);
        
    }
    
    public void setDtwTripMcDmuAttributes( TripModeChoiceDMU tripMcDmuObject, int origMgra, int destMgra, int departPeriod, boolean loggingEnabled )
    {
    	//setup best path dmu variables
    	TransitWalkAccessDMU walkDmu =  new TransitWalkAccessDMU();
    	TransitDriveAccessDMU driveDmu  = new TransitDriveAccessDMU();
    	
        // drive access, walk egress transit, outbound
        int skimPeriodIndex = ModelStructure.getSkimPeriodIndex(departPeriod);
        
        // store best tap pairs using outbound direction array
        bestDtwTripTapPairs = bestPathUEC.getBestTapPairs(walkDmu, driveDmu, DTW, origMgra, destMgra, skimPeriodIndex, loggingEnabled, autoSkimLogger);
       
        //set person specific variables and re-calculate best tap pair utilities
        walkDmu.setApplicationType(bestPathUEC.APP_TYPE_TRIPMC);
    	walkDmu.setIvtCoeff( (float) tripMcDmuObject.getIvtCoeff());
    	walkDmu.setCostCoeff( (float) tripMcDmuObject.getCostCoeff());
    	
    	driveDmu.setApplicationType(bestPathUEC.APP_TYPE_TRIPMC);
    	driveDmu.setIvtCoeff( (float) tripMcDmuObject.getIvtCoeff());
    	driveDmu.setCostCoeff( (float) tripMcDmuObject.getCostCoeff());
    	
       	//catch issues where the trip mode choice DMU was set up without a household or person object
    	if(tripMcDmuObject.getHouseholdObject()!=null){
    		walkDmu.setPersonType(tripMcDmuObject.getTourCategoryJoint()==1 ? walkDmu.getPersonType() : tripMcDmuObject.getPersonType());
    	   	driveDmu.setPersonType(tripMcDmuObject.getTourCategoryJoint()==1 ? driveDmu.getPersonType() : tripMcDmuObject.getPersonType());
      	}
      // calculate logsum
    	bestDtwTripTapPairs = bestPathUEC.calcPersonSpecificUtilities(bestDtwTripTapPairs, walkDmu, driveDmu, DTW, origMgra, destMgra, skimPeriodIndex, loggingEnabled, autoSkimLogger);
        double logsum = bestPathUEC.calcTripLogSum(bestDtwTripTapPairs, loggingEnabled, autoSkimLogger);
        tripMcDmuObject.setTransitLogSum( DTW, logsum);
        
    }
    
    //select best transit path from N-path for trip
    public int chooseTripPath(float rnum, double[][] bestTapPairs, boolean myTrace, Logger myLogger) {
    	return bestPathUEC.chooseTripPath(rnum, bestTapPairs, myTrace, myLogger);
    }
    
}
