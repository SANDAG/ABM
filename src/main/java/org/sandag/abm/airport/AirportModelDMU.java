package org.sandag.abm.airport;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.BestTransitPathCalculator;
import org.sandag.abm.accessibilities.DriveTransitWalkSkimsCalculator;
import org.sandag.abm.accessibilities.WalkTransitDriveSkimsCalculator;
import org.sandag.abm.accessibilities.WalkTransitWalkSkimsCalculator;
import org.sandag.abm.common.ConditionalDMU;
import org.sandag.abm.modechoice.Modes;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

public class AirportModelDMU
        extends ConditionalDMU
        implements Serializable, VariableTable
{
    protected IndexValues                     dmuIndex;

    private AirportParty                      airportParty;
    private double[][]                        sizeTerms;                                                                       // dimensioned
                                                                                                                                // by
                                                                                                                                // segment,
                                                                                                                                // taz
    private int[]                             zips;                                                                            // dimensioned
                                                                                                                                // by
                                                                                                                                // taz
    protected static final int                LB_ACC_TIME_INDEX             = 0;
    protected static final int                LB_EGR_TIME_INDEX             = 1;
    protected static final int                LB_AUX_TIME_INDEX             = 2;
    protected static final int                LB_LB_IVT_INDEX               = 3;
    protected static final int                LB_FWAIT_INDEX                = 4;
    protected static final int                LB_XWAIT_INDEX                = 5;
    protected static final int                LB_FARE_INDEX                 = 6;
    protected static final int                LB_XFERS_INDEX                = 7;

    protected static final int                PREM_ACC_TIME_INDEX           = 0;
    protected static final int                PREM_EGR_TIME_INDEX           = 1;
    protected static final int                PREM_AUX_TIME_INDEX           = 2;
    protected static final int                PREM_LB_IVT_INDEX             = 3;
    protected static final int                PREM_EB_IVT_INDEX             = 4;
    protected static final int                PREM_BRT_IVT_INDEX            = 5;
    protected static final int                PREM_LR_IVT_INDEX             = 6;
    protected static final int                PREM_CR_IVT_INDEX             = 7;
    protected static final int                PREM_FWAIT_INDEX              = 8;
    protected static final int                PREM_XWAIT_INDEX              = 9;
    protected static final int                PREM_FARE_INDEX               = 10;
    protected static final int                PREM_MAIN_MODE_INDEX          = 11;
    protected static final int                PREM_XFERS_INDEX              = 12;
    protected double                          nmWalkTime;
    protected double                          nmBikeTime;

    public static final int                   LB                            = 0;
    public static final int                   EB                            = 1;
    public static final int                   BRT                           = 2;
    public static final int                   LR                            = 3;
    public static final int                   CR                            = 4;
    protected static final int                NUM_LOC_PREM                  = 5;

    public static final int                   WTW                           = 0;
    public static final int                   WTD                           = 1;
    public static final int                   DTW                           = 2;
    protected static final int                NUM_ACC_EGR                   = 3;

    public static final int                   LB_IVT                        = 0;
    public static final int                   EB_IVT                        = 1;
    public static final int                   BRT_IVT                       = 2;
    public static final int                   LR_IVT                        = 3;
    public static final int                   CR_IVT                        = 4;
    public static final int                   ACC                           = 5;
    public static final int                   EGR                           = 6;
    public static final int                   AUX                           = 7;
    public static final int                   FWAIT                         = 8;
    public static final int                   XWAIT                         = 9;
    public static final int                   FARE                          = 10;
    public static final int                   XFERS                         = 11;
    protected static final int                NUM_SKIMS                     = 13;

    public static final int                   OUT                           = 0;
    public static final int                   IN                            = 1;
    protected static final int                NUM_DIR                       = 2;
    
    protected int							  NUM_A_MGRA;
    
    protected static final int				  NUM_LOS = 4;

	private int 							  nonAirportMgra;
    private int								  direction;

    // estimation file defines time periods as:
    // 1 | Early AM: 3:00 AM - 5:59 AM |
    // 2 | AM Peak: 6:00 AM - 8:59 AM |
    // 3 | Early MD: 9:00 AM - 11:59 PM |
    // 4 | Late MD: 12:00 PM - 3:29 PM |
    // 5 | PM Peak: 3:30 PM - 6:59 PM |
    // 6 | Evening: 7:00 PM - 2:59 AM |

    protected static final int                LAST_EA_INDEX                 = 3;
    protected static final int                LAST_AM_INDEX                 = 9;
    protected static final int                LAST_MD_INDEX                 = 22;
    protected static final int                LAST_PM_INDEX                 = 29;

    protected static final int                EA                            = 1;
    protected static final int                AM                            = 2;
    protected static final int                MD                            = 3;
    protected static final int                PM                            = 4;
    protected static final int                EV                            = 5;

    protected static final int                EA_D                          = 1;                                               // 5am
    protected static final int                AM_D                          = 5;                                               // 7am
    protected static final int                MD_D                          = 15;                                              // 12pm
    protected static final int                PM_D                          = 27;                                              // 6pm
    protected static final int                EV_D                          = 35;                                              // 10pm
    protected static final int[]              DEFAULT_DEPART_INDICES        = {-1, EA_D, AM_D,
            MD_D, PM_D, EV_D                                                };

    protected static final int                EA_A                          = 2;                                               // 5:30am
    protected static final int                AM_A                          = 6;                                               // 7:30am
    protected static final int                MD_A                          = 16;                                              // 12:30pm
    protected static final int                PM_A                          = 28;                                              // 6:30pm
    protected static final int                EV_A                          = 36;                                              // 10:30pm
    protected static final int[]              DEFAULT_ARRIVE_INDICES        = {-1, EA_A, AM_A,
            MD_A, PM_A, EV_A                                                };

    protected String[][]                      departArriveCombinationLabels = { {"EA", "EA"},
            {"EA", "AM"}, {"EA", "MD"}, {"EA", "PM"}, {"EA", "EV"}, {"AM", "AM"}, {"AM", "MD"},
            {"AM", "PM"}, {"AM", "EV"}, {"MD", "MD"}, {"MD", "PM"}, {"MD", "EV"}, {"PM", "PM"},
            {"PM", "EV"}, {"EV", "EV"}                                      };

    protected int[][]                         departArriveCombinations      = { {EA, EA}, {EA, AM},
            {EA, MD}, {EA, PM}, {EA, EV}, {AM, AM}, {AM, MD}, {AM, PM}, {AM, EV}, {MD, MD},
            {MD, PM}, {MD, EV}, {PM, PM}, {PM, EV}, {EV, EV}                };

    protected double[]                        lbSkims;
    protected double[]                        ebSkims;
    protected double[]                        brSkims;
    protected double[]                        lrSkims;
    protected double[]                        crSkims;

    // Transit skim calculators
    protected WalkTransitWalkSkimsCalculator  wtw;
    protected WalkTransitDriveSkimsCalculator wtd;
    protected DriveTransitWalkSkimsCalculator dtw;
    private double                            driveAloneLogsum;
    private double                            shared2Logsum;
    private double                            shared3Logsum;
    private double                            transitLogsum;
    
    private double							  ridehailTravelDistanceLocation1;
    private double							  ridehailTravelDistanceLocation2;
    private double							  ridehailTravelTimeLocation1;
    private double							  ridehailTravelTimeLocation2;

	protected double[][][]                    transitSkim                   = new double[NUM_ACC_EGR][NUM_LOC_PREM][NUM_SKIMS];

    protected Logger                          _logger                       = null;
    
    protected double[][][][]                  travel_time;
    
    public int								  maxMgra;
    
	public HashMap<Integer, Integer>		  mode_mgra_map = new HashMap<Integer, Integer>();
    public HashMap<Integer, Integer>		  mgra_index_map = new HashMap<Integer, Integer>();

    public AirportModelDMU(Logger logger)
    {
        dmuIndex = new IndexValues();
        setupMethodIndexMap();
        if (logger == null)
        {
            _logger = Logger.getLogger(AirportModelDMU.class);
        } else _logger = logger;
    }

    /**
     * Set up the method index hashmap, where the key is the getter method for a
     * data item and the value is the index.
     */
    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();
        methodIndexMap.put("getDirection", 0);
        methodIndexMap.put("getPurpose", 1);
        methodIndexMap.put("getSize", 2);
        methodIndexMap.put("getIncome", 3);
        methodIndexMap.put("getDepartTime", 4);
        methodIndexMap.put("getNights", 5);
        methodIndexMap.put("getOriginMGRA", 6);
        methodIndexMap.put("getLnDestChoiceSizeTazAlt", 7);
        methodIndexMap.put("getDestZipAlt", 8);

        methodIndexMap.put("getDriveAloneLogsum", 90);
        methodIndexMap.put("getShared2Logsum", 91);
        methodIndexMap.put("getShared3Logsum", 92);
        //methodIndexMap.put("getTransitLogsum", 93);
        
        methodIndexMap.put("getWtw_lb_LB_ivt", 100);
        methodIndexMap.put("getWtw_lb_fwait", 101);
        methodIndexMap.put("getWtw_lb_xwait", 102);
        methodIndexMap.put("getWtw_lb_AccTime", 103);
        methodIndexMap.put("getWtw_lb_EgrTime", 104);
        methodIndexMap.put("getWtw_lb_WalkAuxTime", 105);
        methodIndexMap.put("getWtw_lb_fare", 106);
        methodIndexMap.put("getWtw_lb_xfers", 107);
        methodIndexMap.put("getWtw_eb_LB_ivt", 108);
        methodIndexMap.put("getWtw_eb_EB_ivt", 109);
        methodIndexMap.put("getWtw_eb_fwait", 110);
        methodIndexMap.put("getWtw_eb_xwait", 111);
        methodIndexMap.put("getWtw_eb_AccTime", 112);
        methodIndexMap.put("getWtw_eb_EgrTime", 113);
        methodIndexMap.put("getWtw_eb_WalkAuxTime", 114);
        methodIndexMap.put("getWtw_eb_fare", 115);
        methodIndexMap.put("getWtw_eb_xfers", 116);
        methodIndexMap.put("getWtw_brt_LB_ivt", 117);
        methodIndexMap.put("getWtw_brt_EB_ivt", 118);
        methodIndexMap.put("getWtw_brt_BRT_ivt", 119);
        methodIndexMap.put("getWtw_brt_fwait", 120);
        methodIndexMap.put("getWtw_brt_xwait", 121);
        methodIndexMap.put("getWtw_brt_AccTime", 122);
        methodIndexMap.put("getWtw_brt_EgrTime", 123);
        methodIndexMap.put("getWtw_brt_WalkAuxTime", 124);
        methodIndexMap.put("getWtw_brt_fare", 125);
        methodIndexMap.put("getWtw_brt_xfers", 126);
        methodIndexMap.put("getWtw_lr_LB_ivt", 127);
        methodIndexMap.put("getWtw_lr_EB_ivt", 128);
        methodIndexMap.put("getWtw_lr_BRT_ivt", 129);
        methodIndexMap.put("getWtw_lr_LRT_ivt", 130);
        methodIndexMap.put("getWtw_lr_fwait", 131);
        methodIndexMap.put("getWtw_lr_xwait", 132);
        methodIndexMap.put("getWtw_lr_AccTime", 133);
        methodIndexMap.put("getWtw_lr_EgrTime", 134);
        methodIndexMap.put("getWtw_lr_WalkAuxTime", 135);
        methodIndexMap.put("getWtw_lr_fare", 136);
        methodIndexMap.put("getWtw_lr_xfers", 137);
        methodIndexMap.put("getWtw_cr_LB_ivt", 138);
        methodIndexMap.put("getWtw_cr_EB_ivt", 139);
        methodIndexMap.put("getWtw_cr_BRT_ivt", 140);
        methodIndexMap.put("getWtw_cr_LRT_ivt", 141);
        methodIndexMap.put("getWtw_cr_CR_ivt", 142);
        methodIndexMap.put("getWtw_cr_fwait", 143);
        methodIndexMap.put("getWtw_cr_xwait", 144);
        methodIndexMap.put("getWtw_cr_AccTime", 145);
        methodIndexMap.put("getWtw_cr_EgrTime", 146);
        methodIndexMap.put("getWtw_cr_WalkAuxTime", 147);
        methodIndexMap.put("getWtw_cr_fare", 148);
        methodIndexMap.put("getWtw_cr_xfers", 149);

        methodIndexMap.put("getDt_lb_LB_ivt", 150);
        methodIndexMap.put("getDt_lb_fwait", 151);
        methodIndexMap.put("getDt_lb_xwait", 152);
        methodIndexMap.put("getDt_lb_AccTime", 153);
        methodIndexMap.put("getDt_lb_EgrTime", 154);
        methodIndexMap.put("getDt_lb_DrvTime", 155);
        methodIndexMap.put("getDt_lb_WalkAuxTime", 156);
        methodIndexMap.put("getDt_lb_fare", 157);
        methodIndexMap.put("getDt_lb_xfers", 158);
        methodIndexMap.put("getDt_eb_LB_ivt", 159);
        methodIndexMap.put("getDt_eb_EB_ivt", 160);
        methodIndexMap.put("getDt_eb_fwait", 161);
        methodIndexMap.put("getDt_eb_xwait", 162);
        methodIndexMap.put("getDt_eb_AccTime", 163);
        methodIndexMap.put("getDt_eb_EgrTime", 164);
        methodIndexMap.put("getDt_eb_DrvTime", 165);
        methodIndexMap.put("getDt_eb_WalkAuxTime", 166);
        methodIndexMap.put("getDt_eb_fare", 167);
        methodIndexMap.put("getDt_eb_xfers", 168);
        methodIndexMap.put("getDt_brt_LB_ivt", 169);
        methodIndexMap.put("getDt_brt_EB_ivt", 170);
        methodIndexMap.put("getDt_brt_BRT_ivt", 171);
        methodIndexMap.put("getDt_brt_fwait", 172);
        methodIndexMap.put("getDt_brt_xwait", 173);
        methodIndexMap.put("getDt_brt_AccTime", 174);
        methodIndexMap.put("getDt_brt_EgrTime", 175);
        methodIndexMap.put("getDt_brt_DrvTime", 176);
        methodIndexMap.put("getDt_brt_WalkAuxTime", 177);
        methodIndexMap.put("getDt_brt_fare", 178);
        methodIndexMap.put("getDt_brt_xfers", 179);
        methodIndexMap.put("getDt_lr_LB_ivt", 180);
        methodIndexMap.put("getDt_lr_EB_ivt", 181);
        methodIndexMap.put("getDt_lr_BRT_ivt", 182);
        methodIndexMap.put("getDt_lr_LRT_ivt", 183);
        methodIndexMap.put("getDt_lr_fwait", 184);
        methodIndexMap.put("getDt_lr_xwait", 185);
        methodIndexMap.put("getDt_lr_AccTime", 186);
        methodIndexMap.put("getDt_lr_EgrTime", 187);
        methodIndexMap.put("getDt_lr_DrvTime", 188);
        methodIndexMap.put("getDt_lr_WalkAuxTime", 189);
        methodIndexMap.put("getDt_lr_fare", 190);
        methodIndexMap.put("getDt_lr_xfers", 191);
        methodIndexMap.put("getDt_cr_LB_ivt", 192);
        methodIndexMap.put("getDt_cr_EB_ivt", 193);
        methodIndexMap.put("getDt_cr_BRT_ivt", 194);
        methodIndexMap.put("getDt_cr_LRT_ivt", 195);
        methodIndexMap.put("getDt_cr_CR_ivt", 196);
        methodIndexMap.put("getDt_cr_fwait", 197);
        methodIndexMap.put("getDt_cr_xwait", 198);
        methodIndexMap.put("getDt_cr_AccTime", 199);
        methodIndexMap.put("getDt_cr_EgrTime", 200);
        methodIndexMap.put("getDt_cr_DrvTime", 201);
        methodIndexMap.put("getDt_cr_WalkAuxTime", 202);
        methodIndexMap.put("getDt_cr_fare", 203);
        methodIndexMap.put("getDt_cr_xfers", 204);
        
        methodIndexMap.put("getDriveAloneLogsumParkLocation1", 20);
        methodIndexMap.put("getShared2LogsumParkLocation1", 21);
        methodIndexMap.put("getShared3LogsumParkLocation1", 22);
        methodIndexMap.put("getDriveAloneLogsumParkLocation2", 23);
        methodIndexMap.put("getShared2LogsumParkLocation2", 24);
        methodIndexMap.put("getShared3LogsumParkLocation2", 25);
        methodIndexMap.put("getDriveAloneLogsumParkLocation3", 26);
        methodIndexMap.put("getShared2LogsumParkLocation3", 27);
        methodIndexMap.put("getShared3LogsumParkLocation3", 28);
        methodIndexMap.put("getDriveAloneLogsumParkLocation4", 29);
        methodIndexMap.put("getShared2LogsumParkLocation4", 30);
        methodIndexMap.put("getShared3LogsumParkLocation4", 31);
        methodIndexMap.put("getDriveAloneLogsumParkLocation5", 32);
        methodIndexMap.put("getShared2LogsumParkLocation5", 33);
        methodIndexMap.put("getShared3LogsumParkLocation5", 34);
        methodIndexMap.put("getShared2LogsumParkEscort", 35);
        methodIndexMap.put("getShared3LogsumParkEscort", 36);
        methodIndexMap.put("getDriveAloneLogsumRental", 37);
        methodIndexMap.put("getShared2LogsumRental", 38);
        methodIndexMap.put("getShared3LogsumRental", 39);
        methodIndexMap.put("getShared2LogsumHotelOrShuttleTerminal", 40);
        methodIndexMap.put("getShared3LogsumHotelOrShuttleTerminal", 41);
        methodIndexMap.put("getShared2LogsumHotelOrShuttleCentralMobilityHub", 42);
        methodIndexMap.put("getShared3LogsumHotelOrShuttleCentralMobilityHub", 43);
        methodIndexMap.put("getShared2LogsumRidehailLocation1", 44);
        methodIndexMap.put("getShared3LogsumRidehailLocation1", 45);
        methodIndexMap.put("getShared2LogsumRidehailLocation2", 46);
        methodIndexMap.put("getShared3LogsumRidehailLocation2", 47);
        methodIndexMap.put("getTransitLogsum", 48);
        methodIndexMap.put("getShared2LogsumCurbLocation1", 49);
        methodIndexMap.put("getShared3LogsumCurbLocation1", 50);
        methodIndexMap.put("getShared2LogsumCurbLocation2", 51);
        methodIndexMap.put("getShared3LogsumCurbLocation2", 52);
        methodIndexMap.put("getShared2LogsumCurbLocation3", 53);
        methodIndexMap.put("getShared3LogsumCurbLocation3", 54);
        methodIndexMap.put("getShared2LogsumCurbLocation4", 55);
        methodIndexMap.put("getShared3LogsumCurbLocation4", 56);
        methodIndexMap.put("getShared2LogsumCurbLocation5", 57);
        methodIndexMap.put("getShared3LogsumCurbLocation5", 58);
        methodIndexMap.put("getRidehailTravelDistanceLocation1", 59);
        methodIndexMap.put("getRidehailTravelDistanceLocation2", 60);
        methodIndexMap.put("getRidehailTravelTimeLocation1", 61);
        methodIndexMap.put("getRidehailTravelTimeLocation2", 62);
        
        CreateReverseMap();
    }

    /**
     * Look up and return the value for the variable according to the index.
     * 
     */
    public double getValueForIndex(int variableIndex, int arrayIndex)
    {
        if (variableIndex == 7) return getLnDestChoiceSizeTazAlt(arrayIndex);
        if (variableIndex == 8) return getDestZipAlt(arrayIndex);

        return getValueForIndexLookup(variableIndex, arrayIndex);
    }

    /**
     * Set the skim calculators in the DMU
     * 
     * @param myWtw
     *            the WalkTransitWalkSkimsCalculator
     * @param myWtd
     *            the WalkTransitDriveSkimsCalculator
     * @param myDtw
     *            the DriveTransitWalkSkimsCalculator
     */
    public void setDmuSkimCalculators(WalkTransitWalkSkimsCalculator myWtw,
            WalkTransitDriveSkimsCalculator myWtd, DriveTransitWalkSkimsCalculator myDtw)
    {
        wtw = myWtw;
        wtd = myWtd;
        dtw = myDtw;
    }

    public int getNonAirportMgra() {
		return nonAirportMgra;
	}

	public void setNonAirportMgra(int nonAirportMgra) {
		this.nonAirportMgra = nonAirportMgra;
	}

	public void setDirection(int direction) {
		this.direction = direction;
	}
	
	public void setMaxMgra(int maxMgra) {
		this.maxMgra = maxMgra;
	}
	
	public void setModeMgraMap(HashMap<Integer, Integer> modeMgraMap){
    	mode_mgra_map = modeMgraMap;
    }
    
    public void setMgraIndexMap(){
    	Integer[] mgraValueArray = mode_mgra_map.values().toArray(new Integer[0]);
    	
    	Set<Integer> uniqueMgraValues = new TreeSet<Integer>();
    	uniqueMgraValues.addAll(Arrays.asList(mgraValueArray));
    	
    	Integer[] uniqueMgraValueArray = uniqueMgraValues.toArray(new Integer[0]);
    	
    	for (int i = 0; i < uniqueMgraValueArray.length; i++){
    		mgra_index_map.put(uniqueMgraValueArray[i], i);
    	}
    }
    
    public void setTravelTimeArraySize(){
    	NUM_A_MGRA = mgra_index_map.size();
    	travel_time = new double[maxMgra + 1][NUM_A_MGRA][NUM_DIR][NUM_LOS];
    }

    /**
     * Set the mode travel time array value for the access/egress mode, line-haul
     * mode, LOS component
     * 
     * @param nonAirportMgra
     *            Index for nonairport mgra
     * @param airportMgra_index
     *            Index for airport Mgra
     * @param direction
     *            The index for direction
     * @param los
     *            The los type          
     * @param value
     *            The value to set in the array
     */
    protected void setModeTravelTime(int nonAirportMgra, int airportMgra_index, int direction, int los, double value)
    {
    	travel_time[nonAirportMgra][airportMgra_index][direction][los] = value;
    }

    /**
     * Get the mode travel time array value for the access/egress mode, line-haul
     * mode, LOS component
     * 
     * @param nonAirportMgra
     *            Index for nonairport mgra
     * @param airportMgra_index
     *            Index for airport Mgra
     * @param direction
     *            The index for direction
     * @param los
     *            The los type 
     * @return The travel time value
     */
    protected double getModeTravelTime(int nonAirportMgra, int airportMgra_index, int direction, int los)
    {
        return travel_time[nonAirportMgra][airportMgra_index][direction][los];
    }

    public double getDriveAloneLogsumParkLocation1() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.PARK_LOC1);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.DA];	
	}
    
    public double getShared2LogsumParkLocation1() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.PARK_LOC1);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR2];	
	}
    
    public double getShared3LogsumParkLocation1() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.PARK_LOC1);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR3];	
	}
    
    public double getDriveAloneLogsumParkLocation2() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.PARK_LOC2);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.DA];	
	}
    
    public double getShared2LogsumParkLocation2() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.PARK_LOC2);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR2];	
	}
    
    public double getShared3LogsumParkLocation2() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.PARK_LOC2);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR3];	
	}
    
    public double getDriveAloneLogsumParkLocation3() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.PARK_LOC3);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.DA];	
	}
    
    public double getShared2LogsumParkLocation3() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.PARK_LOC3);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR2];	
	}
    
    public double getShared3LogsumParkLocation3() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.PARK_LOC3);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR3];	
	}    
  
    public double getDriveAloneLogsumParkLocation4() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.PARK_LOC4);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.DA];	
	}
    
    public double getShared2LogsumParkLocation4() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.PARK_LOC4);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR2];	
	}
    
    public double getShared3LogsumParkLocation4() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.PARK_LOC4);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR3];	
	}
    
    public double getDriveAloneLogsumParkLocation5() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.PARK_LOC5);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.DA];	
	}
    
    public double getShared2LogsumParkLocation5() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.PARK_LOC5);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR2];	
	}
    
    public double getShared3LogsumParkLocation5() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.PARK_LOC5);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR3];	
	}
    
    public double getShared2LogsumParkEscort() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.PARK_ESC);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR2];	
	}
    
    public double getShared3LogsumParkEscort() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.PARK_ESC);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR3];	
	}
    
    public double getDriveAloneLogsumRental() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.RENTAL);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.DA];	
	}
    
    public double getShared2LogsumRental() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.RENTAL);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR2];	
	}
    
    public double getShared3LogsumRental() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.RENTAL);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR3];	
	}
       
    public double getShared2LogsumHotelOrShuttleTerminal() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.MGRAAlt_TERM);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR2];	
	}
    
    public double getShared3LogsumHotelOrShuttleTerminal() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.MGRAAlt_TERM);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR3];	
	}
    
    public double getShared2LogsumHotelOrShuttleCentralMobilityHub() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.MGRAAlt_CMH);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR2];	
	}
    
    public double getShared3LogsumHotelOrShuttleCentralMobilityHub() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.MGRAAlt_CMH);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR3];	
	}
    
    public double getShared2LogsumRidehailLocation1() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.RIDEHAILING_LOC1);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR2];	
	}
    
    public double getShared3LogsumRidehailLocation1() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.RIDEHAILING_LOC1);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR3];	
	}
    
    public double getShared2LogsumRidehailLocation2() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.RIDEHAILING_LOC2);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR2];	
	}
    
    public double getShared3LogsumRidehailLocation2() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.RIDEHAILING_LOC2);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR3];	
	}
    
    public double getTransitLogsum() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.TRANSIT);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.Transit];	
	}
    
    public double getShared2LogsumCurbLocation1() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.CURB_LOC1);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR2];	
	}
    
    public double getShared3LogsumCurbLocation1() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.CURB_LOC1);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR3];	
	}

    public double getShared2LogsumCurbLocation2() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.CURB_LOC2);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR2];	
	}
    
    public double getShared3LogsumCurbLocation2() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.CURB_LOC2);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR3];	
	}

    public double getShared2LogsumCurbLocation3() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.CURB_LOC3);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR2];	
	}
    
    public double getShared3LogsumCurbLocation3() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.CURB_LOC3);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR3];	
	}

    public double getShared2LogsumCurbLocation4() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.CURB_LOC4);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR2];	
	}
    
    public double getShared3LogsumCurbLocation4() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.CURB_LOC4);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR3];	
	}

    public double getShared2LogsumCurbLocation5() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.CURB_LOC5);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR2];	
	}
    
    public double getShared3LogsumCurbLocation5() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.CURB_LOC5);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR3];	
	}
    
    public void setRidehailTravelDistanceLocation1(double ridehailTravelDistance)
    {
    	ridehailTravelDistanceLocation1 = ridehailTravelDistance;
    }
    
    public void setRidehailTravelDistanceLocation2(double ridehailTravelDistance)
    {
    	ridehailTravelDistanceLocation2 = ridehailTravelDistance;
    }
    
    public void setRidehailTravelTimeLocation1(double ridehailTravelTime)
    {
    	ridehailTravelTimeLocation1 = ridehailTravelTime;
    }
    
    public void setRidehailTravelTimeLocation2(double ridehailTravelTime)
    {
    	ridehailTravelTimeLocation2 = ridehailTravelTime;
    }
    
    public double getRidehailTravelDistanceLocation1() {
		return ridehailTravelDistanceLocation1;
	}

	public double getRidehailTravelDistanceLocation2() {
		return ridehailTravelDistanceLocation2;
	}

	public double getRidehailTravelTimeLocation1() {
		return ridehailTravelTimeLocation1;
	}

	public double getRidehailTravelTimeLocation2() {
		return ridehailTravelTimeLocation2;
	}
	
    /**
     * Set the transit skim array value for the access/egress mode, line-haul
     * mode, LOS component
     * 
     * @param accEgr
     *            An indicator of whether this is wtw, wtd, or dtw
     * @param lbPrem
     *            The line-haul mode
     * @param skimIndex
     *            The index for the skim
     * @param value
     *            The value to set in the array
     */
    protected void setTransitSkim(int accEgr, int lbPrem, int skimIndex, double value)
    {
        transitSkim[accEgr][lbPrem][skimIndex] = value;
    }

    /**
     * Get the value from the transit skim matrix for the access/egress mode,
     * line-haul mode, and LOS component
     * 
     * @param accEgr
     *            An indicator of whether this is wtw, wtd, or dtw
     * @param lbPrem
     *            The line-haul mode
     * @param skimIndex
     *            The index for the skim
     * @return The skim value
     */
    protected double getTransitSkim(int accEgr, int lbPrem, int skimIndex)
    {
        return transitSkim[accEgr][lbPrem][skimIndex];
    }

    /**
     * Set the transit skim variables from the skim calculators, based upon the
     * best TAP-TAP pairs for the airport party. Note that the best TAP-TAP
     * pairs should already be set for each transit access mode (wtw, dtw, and
     * wtd).
     * 
     * @param origMgra
     *            Origin MGRA
     * @param destMgra
     *            Destination MGRA
     * @param departPeriod
     *            Travel Period
     * @param isInbound
     *            Direction flag (true if arriving party, false if departing
     *            party)
     * @param debugFlag
     *            Debug flag
     */
    public void setDmuSkimAttributes(int origMgra, int destMgra, int departPeriod,
            boolean isInbound, boolean debugFlag)
    {

        boolean debug = false;
        if (debugFlag) debug = airportParty.getDebugChoiceModels();

        int[][] bestTapPairs = null;

        bestTapPairs = airportParty.getBestWtwTapPairs();
        setWtwDmuAttributes(origMgra, destMgra, departPeriod, bestTapPairs, debug);

        // outbound (from origin to airport) sets drive-transit-walk skims
        if (!isInbound)
        {
            bestTapPairs = airportParty.getBestDtwTapPairs();
            setDtwDmuAttributes(origMgra, destMgra, departPeriod, bestTapPairs, debug);
        } else
        { // return (from airport to origin) sets walk-transit-drive
          // skims
            bestTapPairs = airportParty.getBestWtdTapPairs();
            setWtdDmuAttributes(origMgra, destMgra, departPeriod, bestTapPairs, debug);
        }

    }

    /**
     * Sets walk-transit walk skims based upon best tap pairs, origin,
     * destination, and travel period.
     * 
     * @param origMgra
     *            Origin MGRA
     * @param destMgra
     *            Destination MGRA
     * @param departPeriod
     *            Travel Period
     * @param bestTapPairs
     *            Best TAP-TAP walk-transit pairs.
     * @param loggingEnabled
     *            True to debug
     */
    protected void setWtwDmuAttributes(int origMgra, int destMgra, int departPeriod,
            int[][] bestTapPairs, boolean loggingEnabled)
    {
        if (bestTapPairs == null)
        {
            crSkims = wtw.getNullTransitSkims(Modes.getTransitModeIndex("CR"));
            lrSkims = wtw.getNullTransitSkims(Modes.getTransitModeIndex("LR"));
            brSkims = wtw.getNullTransitSkims(Modes.getTransitModeIndex("BRT"));
            ebSkims = wtw.getNullTransitSkims(Modes.getTransitModeIndex("EB"));
            lbSkims = wtw.getNullTransitSkims(Modes.getTransitModeIndex("LB"));
            return;
        }

        // walk access, walk egress transit, outbound
        int skimPeriodIndex = AirportModelStructure.getSkimPeriodIndex(departPeriod);
        departPeriod = skimPeriodIndex + 1;

        int i = Modes.getTransitModeIndex("CR");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            crSkims = wtw.getWalkTransitWalkSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            crSkims = wtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LR");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            lrSkims = wtw.getWalkTransitWalkSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            lrSkims = wtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("BRT");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            brSkims = wtw.getWalkTransitWalkSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            brSkims = wtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("EB");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            ebSkims = wtw.getWalkTransitWalkSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            ebSkims = wtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LB");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            lbSkims = wtw.getWalkTransitWalkSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            lbSkims = wtw.getNullTransitSkims(i);
        }

        setTransitSkim(WTW, LB, LB_IVT, lbSkims[LB_LB_IVT_INDEX]);
        setTransitSkim(WTW, LB, FWAIT, lbSkims[LB_FWAIT_INDEX]);
        setTransitSkim(WTW, LB, XWAIT, lbSkims[LB_XWAIT_INDEX]);
        setTransitSkim(WTW, LB, ACC, lbSkims[LB_ACC_TIME_INDEX]);
        setTransitSkim(WTW, LB, EGR, lbSkims[LB_EGR_TIME_INDEX]);
        setTransitSkim(WTW, LB, AUX, lbSkims[LB_AUX_TIME_INDEX]);
        setTransitSkim(WTW, LB, FARE, lbSkims[LB_FARE_INDEX]);
        setTransitSkim(WTW, LB, XFERS, lbSkims[LB_XFERS_INDEX]);

        setTransitSkim(WTW, EB, LB_IVT, ebSkims[PREM_LB_IVT_INDEX]);
        setTransitSkim(WTW, EB, EB_IVT, ebSkims[PREM_EB_IVT_INDEX]);
        setTransitSkim(WTW, EB, FWAIT, ebSkims[PREM_FWAIT_INDEX]);
        setTransitSkim(WTW, EB, XWAIT, ebSkims[PREM_XWAIT_INDEX]);
        setTransitSkim(WTW, EB, ACC, ebSkims[PREM_ACC_TIME_INDEX]);
        setTransitSkim(WTW, EB, EGR, ebSkims[PREM_EGR_TIME_INDEX]);
        setTransitSkim(WTW, EB, AUX, ebSkims[PREM_AUX_TIME_INDEX]);
        setTransitSkim(WTW, EB, FARE, ebSkims[PREM_FARE_INDEX]);
        setTransitSkim(WTW, EB, XFERS, ebSkims[PREM_XFERS_INDEX]);

        setTransitSkim(WTW, BRT, LB_IVT, brSkims[PREM_LB_IVT_INDEX]);
        setTransitSkim(WTW, BRT, EB_IVT, brSkims[PREM_EB_IVT_INDEX]);
        setTransitSkim(WTW, BRT, BRT_IVT, brSkims[PREM_BRT_IVT_INDEX]);
        setTransitSkim(WTW, BRT, FWAIT, brSkims[PREM_FWAIT_INDEX]);
        setTransitSkim(WTW, BRT, XWAIT, brSkims[PREM_XWAIT_INDEX]);
        setTransitSkim(WTW, BRT, ACC, brSkims[PREM_ACC_TIME_INDEX]);
        setTransitSkim(WTW, BRT, EGR, brSkims[PREM_EGR_TIME_INDEX]);
        setTransitSkim(WTW, BRT, AUX, brSkims[PREM_AUX_TIME_INDEX]);
        setTransitSkim(WTW, BRT, FARE, brSkims[PREM_FARE_INDEX]);
        setTransitSkim(WTW, BRT, XFERS, brSkims[PREM_XFERS_INDEX]);

        setTransitSkim(WTW, LR, LB_IVT, lrSkims[PREM_LB_IVT_INDEX]);
        setTransitSkim(WTW, LR, EB_IVT, lrSkims[PREM_EB_IVT_INDEX]);
        setTransitSkim(WTW, LR, BRT_IVT, lrSkims[PREM_BRT_IVT_INDEX]);
        setTransitSkim(WTW, LR, LR_IVT, lrSkims[PREM_LR_IVT_INDEX]);
        setTransitSkim(WTW, LR, FWAIT, lrSkims[PREM_FWAIT_INDEX]);
        setTransitSkim(WTW, LR, XWAIT, lrSkims[PREM_XWAIT_INDEX]);
        setTransitSkim(WTW, LR, ACC, lrSkims[PREM_ACC_TIME_INDEX]);
        setTransitSkim(WTW, LR, EGR, lrSkims[PREM_EGR_TIME_INDEX]);
        setTransitSkim(WTW, LR, AUX, lrSkims[PREM_AUX_TIME_INDEX]);
        setTransitSkim(WTW, LR, FARE, lrSkims[PREM_FARE_INDEX]);
        setTransitSkim(WTW, LR, XFERS, lrSkims[PREM_XFERS_INDEX]);

        setTransitSkim(WTW, CR, LB_IVT, crSkims[PREM_LB_IVT_INDEX]);
        setTransitSkim(WTW, CR, EB_IVT, crSkims[PREM_EB_IVT_INDEX]);
        setTransitSkim(WTW, CR, BRT_IVT, crSkims[PREM_BRT_IVT_INDEX]);
        setTransitSkim(WTW, CR, LR_IVT, crSkims[PREM_LR_IVT_INDEX]);
        setTransitSkim(WTW, CR, CR_IVT, crSkims[PREM_CR_IVT_INDEX]);
        setTransitSkim(WTW, CR, FWAIT, crSkims[PREM_FWAIT_INDEX]);
        setTransitSkim(WTW, CR, XWAIT, crSkims[PREM_XWAIT_INDEX]);
        setTransitSkim(WTW, CR, ACC, crSkims[PREM_ACC_TIME_INDEX]);
        setTransitSkim(WTW, CR, EGR, crSkims[PREM_EGR_TIME_INDEX]);
        setTransitSkim(WTW, CR, AUX, crSkims[PREM_AUX_TIME_INDEX]);
        setTransitSkim(WTW, CR, FARE, crSkims[PREM_FARE_INDEX]);
        setTransitSkim(WTW, CR, XFERS, crSkims[PREM_XFERS_INDEX]);

    }

    /**
     * Sets walk-transit-drive skims based upon best tap pairs, origin,
     * destination, and travel period.
     * 
     * @param origMgra
     *            Origin MGRA
     * @param destMgra
     *            Destination MGRA
     * @param departPeriod
     *            Travel Period
     * @param bestTapPairs
     *            Best TAP-TAP walk-transit-drive pairs.
     * @param loggingEnabled
     *            True to debug
     */
    protected void setWtdDmuAttributes(int origMgra, int destMgra, int departPeriod,
            int[][] bestTapPairs, boolean loggingEnabled)
    {

        if (bestTapPairs == null)
        {
            crSkims = wtd.getNullTransitSkims(Modes.getTransitModeIndex("CR"));
            lrSkims = wtd.getNullTransitSkims(Modes.getTransitModeIndex("LR"));
            brSkims = wtd.getNullTransitSkims(Modes.getTransitModeIndex("BRT"));
            ebSkims = wtd.getNullTransitSkims(Modes.getTransitModeIndex("EB"));
            lbSkims = wtd.getNullTransitSkims(Modes.getTransitModeIndex("LB"));
            return;
        }

        // walk access, drive egress transit, inbound
        int skimPeriodIndex = AirportModelStructure.getSkimPeriodIndex(departPeriod);
        departPeriod = skimPeriodIndex + 1;

        int i = Modes.getTransitModeIndex("CR");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findDriveTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            crSkims = wtd.getWalkTransitDriveSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            crSkims = wtd.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LR");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findDriveTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            lrSkims = wtd.getWalkTransitDriveSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            lrSkims = wtd.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("BRT");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findDriveTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            brSkims = wtd.getWalkTransitDriveSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            brSkims = wtd.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("EB");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findDriveTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            ebSkims = wtd.getWalkTransitDriveSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            ebSkims = wtd.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LB");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findDriveTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            lbSkims = wtd.getWalkTransitDriveSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            lbSkims = wtd.getNullTransitSkims(i);
        }

        setTransitSkim(WTD, LB, LB_IVT, lbSkims[LB_LB_IVT_INDEX]);
        setTransitSkim(WTD, LB, FWAIT, lbSkims[LB_FWAIT_INDEX]);
        setTransitSkim(WTD, LB, XWAIT, lbSkims[LB_XWAIT_INDEX]);
        setTransitSkim(WTD, LB, ACC, lbSkims[LB_ACC_TIME_INDEX]);
        setTransitSkim(WTD, LB, EGR, lbSkims[LB_EGR_TIME_INDEX]);
        setTransitSkim(WTD, LB, AUX, lbSkims[LB_AUX_TIME_INDEX]);
        setTransitSkim(WTD, LB, FARE, lbSkims[LB_FARE_INDEX]);
        setTransitSkim(WTD, LB, XFERS, lbSkims[LB_XFERS_INDEX]);

        setTransitSkim(WTD, EB, LB_IVT, ebSkims[PREM_LB_IVT_INDEX]);
        setTransitSkim(WTD, EB, EB_IVT, ebSkims[PREM_EB_IVT_INDEX]);
        setTransitSkim(WTD, EB, FWAIT, ebSkims[PREM_FWAIT_INDEX]);
        setTransitSkim(WTD, EB, XWAIT, ebSkims[PREM_XWAIT_INDEX]);
        setTransitSkim(WTD, EB, ACC, ebSkims[PREM_ACC_TIME_INDEX]);
        setTransitSkim(WTD, EB, EGR, ebSkims[PREM_EGR_TIME_INDEX]);
        setTransitSkim(WTD, EB, AUX, ebSkims[PREM_AUX_TIME_INDEX]);
        setTransitSkim(WTD, EB, FARE, ebSkims[PREM_FARE_INDEX]);
        setTransitSkim(WTD, EB, XFERS, ebSkims[PREM_XFERS_INDEX]);

        setTransitSkim(WTD, BRT, LB_IVT, brSkims[PREM_LB_IVT_INDEX]);
        setTransitSkim(WTD, BRT, EB_IVT, brSkims[PREM_EB_IVT_INDEX]);
        setTransitSkim(WTD, BRT, BRT_IVT, brSkims[PREM_BRT_IVT_INDEX]);
        setTransitSkim(WTD, BRT, FWAIT, brSkims[PREM_FWAIT_INDEX]);
        setTransitSkim(WTD, BRT, XWAIT, brSkims[PREM_XWAIT_INDEX]);
        setTransitSkim(WTD, BRT, ACC, brSkims[PREM_ACC_TIME_INDEX]);
        setTransitSkim(WTD, BRT, EGR, brSkims[PREM_EGR_TIME_INDEX]);
        setTransitSkim(WTD, BRT, AUX, brSkims[PREM_AUX_TIME_INDEX]);
        setTransitSkim(WTD, BRT, FARE, brSkims[PREM_FARE_INDEX]);
        setTransitSkim(WTD, BRT, XFERS, brSkims[PREM_XFERS_INDEX]);

        setTransitSkim(WTD, LR, LB_IVT, lrSkims[PREM_LB_IVT_INDEX]);
        setTransitSkim(WTD, LR, EB_IVT, lrSkims[PREM_EB_IVT_INDEX]);
        setTransitSkim(WTD, LR, BRT_IVT, lrSkims[PREM_BRT_IVT_INDEX]);
        setTransitSkim(WTD, LR, LR_IVT, lrSkims[PREM_LR_IVT_INDEX]);
        setTransitSkim(WTD, LR, FWAIT, lrSkims[PREM_FWAIT_INDEX]);
        setTransitSkim(WTD, LR, XWAIT, lrSkims[PREM_XWAIT_INDEX]);
        setTransitSkim(WTD, LR, ACC, lrSkims[PREM_ACC_TIME_INDEX]);
        setTransitSkim(WTD, LR, EGR, lrSkims[PREM_EGR_TIME_INDEX]);
        setTransitSkim(WTD, LR, AUX, lrSkims[PREM_AUX_TIME_INDEX]);
        setTransitSkim(WTD, LR, FARE, lrSkims[PREM_FARE_INDEX]);
        setTransitSkim(WTD, LR, XFERS, lrSkims[PREM_XFERS_INDEX]);

        setTransitSkim(WTD, CR, LB_IVT, crSkims[PREM_LB_IVT_INDEX]);
        setTransitSkim(WTD, CR, EB_IVT, crSkims[PREM_EB_IVT_INDEX]);
        setTransitSkim(WTD, CR, BRT_IVT, crSkims[PREM_BRT_IVT_INDEX]);
        setTransitSkim(WTD, CR, LR_IVT, crSkims[PREM_LR_IVT_INDEX]);
        setTransitSkim(WTD, CR, CR_IVT, crSkims[PREM_CR_IVT_INDEX]);
        setTransitSkim(WTD, CR, FWAIT, crSkims[PREM_FWAIT_INDEX]);
        setTransitSkim(WTD, CR, XWAIT, crSkims[PREM_XWAIT_INDEX]);
        setTransitSkim(WTD, CR, ACC, crSkims[PREM_ACC_TIME_INDEX]);
        setTransitSkim(WTD, CR, EGR, crSkims[PREM_EGR_TIME_INDEX]);
        setTransitSkim(WTD, CR, AUX, crSkims[PREM_AUX_TIME_INDEX]);
        setTransitSkim(WTD, CR, FARE, crSkims[PREM_FARE_INDEX]);
        setTransitSkim(WTD, CR, XFERS, crSkims[PREM_XFERS_INDEX]);

    }

    /**
     * Sets drive-transit-walk skims based upon best tap pairs, origin,
     * destination, and travel period.
     * 
     * @param origMgra
     *            Origin MGRA
     * @param destMgra
     *            Destination MGRA
     * @param departPeriod
     *            Travel Period
     * @param bestTapPairs
     *            Best TAP-TAP drive-transit-walk pairs.
     * @param loggingEnabled
     *            True to debug
     */
    protected void setDtwDmuAttributes(int origMgra, int destMgra, int departPeriod,
            int[][] bestTapPairs, boolean loggingEnabled)
    {

        if (bestTapPairs == null)
        {
            crSkims = dtw.getNullTransitSkims(Modes.getTransitModeIndex("CR"));
            lrSkims = dtw.getNullTransitSkims(Modes.getTransitModeIndex("LR"));
            brSkims = dtw.getNullTransitSkims(Modes.getTransitModeIndex("BRT"));
            ebSkims = dtw.getNullTransitSkims(Modes.getTransitModeIndex("EB"));
            lbSkims = dtw.getNullTransitSkims(Modes.getTransitModeIndex("LB"));
            return;
        }

        // drive access, walk egress transit, outbound
        int skimPeriodIndex = AirportModelStructure.getSkimPeriodIndex(departPeriod);
        departPeriod = skimPeriodIndex + 1;
        // bestTapPairs = dtw.getBestTapPairs(origMgra, destMgra, departPeriod,
        // loggingEnabled, logger);

        int i = Modes.getTransitModeIndex("CR");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findDriveTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            crSkims = dtw.getDriveTransitWalkSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            crSkims = dtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LR");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findDriveTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            lrSkims = dtw.getDriveTransitWalkSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            lrSkims = dtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("BRT");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findDriveTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            brSkims = dtw.getDriveTransitWalkSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            brSkims = dtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("EB");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findDriveTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            ebSkims = dtw.getDriveTransitWalkSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            ebSkims = dtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LB");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findDriveTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            lbSkims = dtw.getDriveTransitWalkSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            lbSkims = dtw.getNullTransitSkims(i);
        }

        setTransitSkim(DTW, LB, LB_IVT, lbSkims[LB_LB_IVT_INDEX]);
        setTransitSkim(DTW, LB, FWAIT, lbSkims[LB_FWAIT_INDEX]);
        setTransitSkim(DTW, LB, XWAIT, lbSkims[LB_XWAIT_INDEX]);
        setTransitSkim(DTW, LB, ACC, lbSkims[LB_ACC_TIME_INDEX]);
        setTransitSkim(DTW, LB, EGR, lbSkims[LB_EGR_TIME_INDEX]);
        setTransitSkim(DTW, LB, AUX, lbSkims[LB_AUX_TIME_INDEX]);
        setTransitSkim(DTW, LB, FARE, lbSkims[LB_FARE_INDEX]);
        setTransitSkim(DTW, LB, XFERS, lbSkims[LB_XFERS_INDEX]);

        setTransitSkim(DTW, EB, LB_IVT, ebSkims[PREM_LB_IVT_INDEX]);
        setTransitSkim(DTW, EB, EB_IVT, ebSkims[PREM_EB_IVT_INDEX]);
        setTransitSkim(DTW, EB, FWAIT, ebSkims[PREM_FWAIT_INDEX]);
        setTransitSkim(DTW, EB, XWAIT, ebSkims[PREM_XWAIT_INDEX]);
        setTransitSkim(DTW, EB, ACC, ebSkims[PREM_ACC_TIME_INDEX]);
        setTransitSkim(DTW, EB, EGR, ebSkims[PREM_EGR_TIME_INDEX]);
        setTransitSkim(DTW, EB, AUX, ebSkims[PREM_AUX_TIME_INDEX]);
        setTransitSkim(DTW, EB, FARE, ebSkims[PREM_FARE_INDEX]);
        setTransitSkim(DTW, EB, XFERS, ebSkims[PREM_XFERS_INDEX]);

        setTransitSkim(DTW, BRT, LB_IVT, brSkims[PREM_LB_IVT_INDEX]);
        setTransitSkim(DTW, BRT, EB_IVT, brSkims[PREM_EB_IVT_INDEX]);
        setTransitSkim(DTW, BRT, BRT_IVT, brSkims[PREM_BRT_IVT_INDEX]);
        setTransitSkim(DTW, BRT, FWAIT, brSkims[PREM_FWAIT_INDEX]);
        setTransitSkim(DTW, BRT, XWAIT, brSkims[PREM_XWAIT_INDEX]);
        setTransitSkim(DTW, BRT, ACC, brSkims[PREM_ACC_TIME_INDEX]);
        setTransitSkim(DTW, BRT, EGR, brSkims[PREM_EGR_TIME_INDEX]);
        setTransitSkim(DTW, BRT, AUX, brSkims[PREM_AUX_TIME_INDEX]);
        setTransitSkim(DTW, BRT, FARE, brSkims[PREM_FARE_INDEX]);
        setTransitSkim(DTW, BRT, XFERS, brSkims[PREM_XFERS_INDEX]);

        setTransitSkim(DTW, LR, LB_IVT, lrSkims[PREM_LB_IVT_INDEX]);
        setTransitSkim(DTW, LR, EB_IVT, lrSkims[PREM_EB_IVT_INDEX]);
        setTransitSkim(DTW, LR, BRT_IVT, lrSkims[PREM_BRT_IVT_INDEX]);
        setTransitSkim(DTW, LR, LR_IVT, lrSkims[PREM_LR_IVT_INDEX]);
        setTransitSkim(DTW, LR, FWAIT, lrSkims[PREM_FWAIT_INDEX]);
        setTransitSkim(DTW, LR, XWAIT, lrSkims[PREM_XWAIT_INDEX]);
        setTransitSkim(DTW, LR, ACC, lrSkims[PREM_ACC_TIME_INDEX]);
        setTransitSkim(DTW, LR, EGR, lrSkims[PREM_EGR_TIME_INDEX]);
        setTransitSkim(DTW, LR, AUX, lrSkims[PREM_AUX_TIME_INDEX]);
        setTransitSkim(DTW, LR, FARE, lrSkims[PREM_FARE_INDEX]);
        setTransitSkim(DTW, LR, XFERS, lrSkims[PREM_XFERS_INDEX]);

        setTransitSkim(DTW, CR, LB_IVT, crSkims[PREM_LB_IVT_INDEX]);
        setTransitSkim(DTW, CR, EB_IVT, crSkims[PREM_EB_IVT_INDEX]);
        setTransitSkim(DTW, CR, BRT_IVT, crSkims[PREM_BRT_IVT_INDEX]);
        setTransitSkim(DTW, CR, LR_IVT, crSkims[PREM_LR_IVT_INDEX]);
        setTransitSkim(DTW, CR, CR_IVT, crSkims[PREM_CR_IVT_INDEX]);
        setTransitSkim(DTW, CR, FWAIT, crSkims[PREM_FWAIT_INDEX]);
        setTransitSkim(DTW, CR, XWAIT, crSkims[PREM_XWAIT_INDEX]);
        setTransitSkim(DTW, CR, ACC, crSkims[PREM_ACC_TIME_INDEX]);
        setTransitSkim(DTW, CR, EGR, crSkims[PREM_EGR_TIME_INDEX]);
        setTransitSkim(DTW, CR, AUX, crSkims[PREM_AUX_TIME_INDEX]);
        setTransitSkim(DTW, CR, FARE, crSkims[PREM_FARE_INDEX]);
        setTransitSkim(DTW, CR, XFERS, crSkims[PREM_XFERS_INDEX]);

    }

    /**
     * Get travel party direction.
     * 
     * @return Travel party direction.
     */
    public int getDirection()
    {
        return airportParty.getDirection();
    }

    /**
     * Get travel party purpose.
     * 
     * @return Travel party direction.
     */
    public int getPurpose()
    {
        return airportParty.getPurpose();
    }

    /**
     * Get travel party size.
     * 
     * @return Travel party size.
     */
    public int getSize()
    {
        return airportParty.getSize();
    }

    /**
     * Get travel party income.
     * 
     * @return Travel party income.
     */
    public int getIncome()
    {
        return airportParty.getIncome();
    }

    /**
     * Get the departure time for the trip
     * 
     * @return Trip departure time.
     */
    public int getDepartTime()
    {
        return airportParty.getDepartTime();
    }

    /**
     * Get the number of nights
     * 
     * @return Travel party number of nights.
     */
    public int getNights()
    {
        return airportParty.getNights();
    }

    /**
     * Get the origin(non-airport) MGRA
     * 
     * @return Travel party origin MGRA
     */
    public int getOriginMGRA()
    {
        return airportParty.getOriginMGRA();
    }

    /**
     * Set the index values for this DMU.
     * 
     * @param id
     * @param origTaz
     * @param destTaz
     */
    public void setDmuIndexValues(int id, int origTaz, int destTaz)
    {
        dmuIndex.setHHIndex(id);
        dmuIndex.setZoneIndex(origTaz);
        dmuIndex.setOriginZone(origTaz);
        dmuIndex.setDestZone(destTaz);

        dmuIndex.setDebug(false);
        dmuIndex.setDebugLabel("");
        if (airportParty.getDebugChoiceModels())
        {
            dmuIndex.setDebug(true);
            dmuIndex.setDebugLabel("Debug Airport Model");
        }

    }

    /**
     * Get the appropriate size term for this purpose and income market.
     * 
     * @param tazNumber
     *            the number of the taz
     * @return the size term
     */
    public double getLnDestChoiceSizeTazAlt(int alt)
    {

        int purpose = getPurpose();
        int income = getIncome();

        int segment = AirportModelStructure.getDCSizeSegment(purpose, income);

        return sizeTerms[segment][alt];
    }

    /**
     * Get the destination district for this alternative.
     * 
     * @param tazNumber
     * @return number of destination district.
     */
    public int getDestZipAlt(int alt)
    {

        return zips[alt];
    }

    /**
     * Set size terms
     * 
     * @param sizeTerms
     *            A double[][] array dimensioned by segments (purp\income
     *            groups) and taz numbers
     */
    public void setSizeTerms(double[][] sizeTerms)
    {
        this.sizeTerms = sizeTerms;
    }

    /**
     * set the zip codes
     * 
     * @param zips
     *            int[] dimensioned by taz number
     */
    public void setZips(int[] zips)
    {
        this.zips = zips;
    }

    /**
     * Set the airport party object.
     * 
     * @param party
     *            The airport party.
     */
    public void setAirportParty(AirportParty party)
    {

        airportParty = party;
    }

    /**
     * @return the dmuIndex
     */
    public IndexValues getDmuIndex()
    {
        return dmuIndex;
    }

    /**
     * @return the driveAloneLogsum
     */
    public double getDriveAloneLogsum()
    {
        return driveAloneLogsum;
    }

    /**
     * @param driveAloneLogsum
     *            the driveAloneLogsum to set
     */
    public void setDriveAloneLogsum(double driveAloneLogsum)
    {
        this.driveAloneLogsum = driveAloneLogsum;
    }

    /**
     * @return the shared2Logsum
     */
    public double getShared2Logsum()
    {
        return shared2Logsum;
    }

    /**
     * @param shared2Logsum
     *            the shared2Logsum to set
     */
    public void setShared2Logsum(double shared2Logsum)
    {
        this.shared2Logsum = shared2Logsum;
    }

    /**
     * @return the shared3Logsum
     */
    public double getShared3Logsum()
    {
        return shared3Logsum;
    }

    /**
     * @param shared3Logsum
     *            the shared3Logsum to set
     */
    public void setShared3Logsum(double shared3Logsum)
    {
        this.shared3Logsum = shared3Logsum;
    }

    /**
     * @return the transitLogsum
     */
/*    public double getTransitLogsum()
    {
        return transitLogsum;
    }
*/
    /**
     * @param transitLogsum
     *            the transitLogsum to set
     */
    public void setTransitLogsum(double transitLogsum)
    {
        this.transitLogsum = transitLogsum;
    }

    public int getIndexValue(String variableName)
    {
        return methodIndexMap.get(variableName);
    }

    public int getAssignmentIndexValue(String variableName)
    {
        throw new UnsupportedOperationException();
    }

    public double getValueForIndex(int variableIndex)
    {
        throw new UnsupportedOperationException();
    }

    public void setValue(String variableName, double variableValue)
    {
        throw new UnsupportedOperationException();
    }

    public void setValue(int variableIndex, double variableValue)
    {
        throw new UnsupportedOperationException();
    }

    protected double getTransitSkimFromMethodName(String methodName) throws Exception
    {
        boolean condition = false;
        if (airportParty != null)
            condition = (airportParty.getDirection() == AirportModelStructure.DEPARTURE);
        return getTranistSkimFromMethodConditional(methodName, condition);
    }

}
