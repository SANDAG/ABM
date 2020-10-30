package org.sandag.abm.airport;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

public class AirportModelDMU
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

    private double                            driveAloneLogsum;
    private double                            shared2Logsum;
    private double                            shared3Logsum;
    private double                            transitLogsum;

    private double							  ridehailTravelDistanceLocation1;
    private double							  ridehailTravelDistanceLocation2;
    private double							  ridehailTravelTimeLocation1;
    private double							  ridehailTravelTimeLocation2;

    protected double                          walkTransitLogsum;
    protected double                          driveTransitLogsum;
    
    protected Logger                          _logger                       = null;
    
    protected double[][][][]                  travel_time;
    
    public int								  maxMgra;
    
	public HashMap<Integer, Integer>		  mode_mgra_map = new HashMap<Integer, Integer>();
    public HashMap<Integer, Integer>		  mgra_index_map = new HashMap<Integer, Integer>();
   
    protected HashMap<String, Integer> methodIndexMap;
    

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

        methodIndexMap.put("getWalkTransitLogsum", 10);
        methodIndexMap.put("getDriveTransitLogsum", 11);
        
        methodIndexMap.put("getAvAvailable", 70);
        
        methodIndexMap.put("getDriveAloneLogsum", 90);
        methodIndexMap.put("getShared2Logsum", 91);
        methodIndexMap.put("getShared3Logsum", 92);
        methodIndexMap.put("getTransitLogsum", 93);

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
        methodIndexMap.put("getTransitLogsumSAN", 48);
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
        
     }

    /**
     * Look up and return the value for the variable according to the index.
     * 
     */
    public double getValueForIndex(int variableIndex, int arrayIndex)
    {
    	
        double returnValue = -1;

        switch (variableIndex)
        {

            case 0:
                returnValue = getDirection();
                break;
            case 1:
                returnValue = getPurpose();
                break;
            case 2:
            	returnValue = getSize();
            	break;
            case 3:
            	returnValue = getIncome();
            	break;
            case 4:
            	returnValue = getDepartTime();
            	break;
            case 5:
            	returnValue = getNights();
            	break;
            case 6:
            	returnValue = getOriginMGRA();
            	break;
            case 7:
            	returnValue = getLnDestChoiceSizeTazAlt(arrayIndex);
            	break;
            case 8:
            	returnValue = getDestZipAlt(arrayIndex);
            	break;
            case 10:
            	returnValue = getWalkTransitLogsum();
                break;
            case 11:
            	returnValue = getDriveTransitLogsum();
                break;
            case 70:
            	returnValue = getAvAvailable();
            	break;
            case 90:
                returnValue = getDriveAloneLogsum();
                break;
            case 91:
            	returnValue = getShared2Logsum();
                break;
            case 92:
            	returnValue = getShared3Logsum();
                break;
            case 93:
            	returnValue = getTransitLogsum();
            	break;
            case 20:
            	returnValue = getDriveAloneLogsumParkLocation1();
                break;
            case 21:
            	returnValue = getShared2LogsumParkLocation1();
                break;
            case 22:
            	returnValue = getShared3LogsumParkLocation1();
                break;
            case 23:
            	returnValue = getDriveAloneLogsumParkLocation2();
                break;
            case 24:
            	returnValue = getShared2LogsumParkLocation2();
                break;
            case 25:
            	returnValue = getShared3LogsumParkLocation2();
                break;
            case 26:
            	returnValue = getDriveAloneLogsumParkLocation3();
                break;
            case 27:
            	returnValue = getShared2LogsumParkLocation3();
                break;
            case 28:
            	returnValue = getShared3LogsumParkLocation3();
                break;
            case 29:
            	returnValue = getDriveAloneLogsumParkLocation4();
                break;
            case 30:
            	returnValue = getShared2LogsumParkLocation4();
                break;
            case 31:
            	returnValue = getShared3LogsumParkLocation4();
                break;
            case 32:
            	returnValue = getDriveAloneLogsumParkLocation5();
                break;
            case 33:
            	returnValue = getShared2LogsumParkLocation5();
                break;
            case 34:
            	returnValue = getShared3LogsumParkLocation5();
                break;
            case 35:
            	returnValue = getShared2LogsumParkEscort();
                break;
            case 36:
            	returnValue = getShared3LogsumParkEscort();
                break;
            case 37:
            	returnValue = getDriveAloneLogsumRental();
                break;
            case 38:
            	returnValue = getShared2LogsumRental();
                break;
            case 39:
            	returnValue = getShared3LogsumRental();
                break;
            case 40:
            	returnValue = getShared2LogsumHotelOrShuttleTerminal();
                break;
            case 41:
            	returnValue = getShared3LogsumHotelOrShuttleTerminal();
                break;
            case 42:
            	returnValue = getShared2LogsumHotelOrShuttleCentralMobilityHub();
                break;
            case 43:
            	returnValue = getShared3LogsumHotelOrShuttleCentralMobilityHub();
                break;
            case 44:
            	returnValue = getShared2LogsumRidehailLocation1();
                break;
            case 45:
            	returnValue = getShared3LogsumRidehailLocation1();
                break;
            case 46:
            	returnValue = getShared2LogsumRidehailLocation2();
                break;
            case 47:
            	returnValue = getShared3LogsumRidehailLocation2();
                break;
            case 48:
            	returnValue = getTransitLogsumSAN();
                break;
            case 49:
            	returnValue = getShared2LogsumCurbLocation1();
                break;
            case 50:
            	returnValue = getShared3LogsumCurbLocation1();
                break;
            case 51:
            	returnValue = getShared2LogsumCurbLocation2();
                break;
            case 52:
            	returnValue = getShared3LogsumCurbLocation2();
                break;
            case 53:
            	returnValue = getShared2LogsumCurbLocation3();
                break;
            case 54:
            	returnValue = getShared3LogsumCurbLocation3();
                break;
            case 55:
            	returnValue = getShared2LogsumCurbLocation4();
                break;
            case 56:
            	returnValue = getShared3LogsumCurbLocation4();
                break;
            case 57:
            	returnValue = getShared2LogsumCurbLocation5();
                break;
            case 58:
            	returnValue = getShared3LogsumCurbLocation5();
                break;
            case 59:
            	returnValue = getRidehailTravelDistanceLocation1();
                break;
            case 60:
            	returnValue = getRidehailTravelDistanceLocation2();
                break;
            case 61:
            	returnValue = getRidehailTravelTimeLocation1();
                break;
            case 62:
            	returnValue = getRidehailTravelTimeLocation2();
                break;
            default:
            	_logger.error( "method number = " + variableIndex + " not found" );
                throw new RuntimeException( "method number = " + variableIndex + " not found" );
        }
        return returnValue;
        
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
		int airportMgra = mode_mgra_map.get(AirportModelStructure.RENTAL_SAN);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.DA];	
	}
    
    public double getShared2LogsumRental() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.RENTAL_SAN);
		int airportMgra_index = mgra_index_map.get(airportMgra);
		return travel_time[nonAirportMgra][airportMgra_index][direction][AirportModelStructure.SR2];	
	}
    
    public double getShared3LogsumRental() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.RENTAL_SAN);
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
    
    public double getTransitLogsumSAN() 
    {
		int airportMgra = mode_mgra_map.get(AirportModelStructure.TRANSIT_SAN);
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
    
    public int getAvAvailable() {
    	
    	if(airportParty.getAvAvailable())
    		return 1;
    	
    	return 0;
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
    public double getTransitLogsum()
    {
        return transitLogsum;
    }

    /**
     * @param transitLogsum
     *            the transitLogsum to set
     */
    public void setTransitLogsum(double transitLogsum)
    {
        this.transitLogsum = transitLogsum;
    }

    public double getWalkTransitLogsum() {
		return walkTransitLogsum;
	}

	public void setWalkTransitLogsum(double walkTransitLogsum) {
		this.walkTransitLogsum = walkTransitLogsum;
	}

	public double getDriveTransitLogsum() {
		return driveTransitLogsum;
	}

	public void setDriveTransitLogsum(double driveTransitLogsum) {
		this.driveTransitLogsum = driveTransitLogsum;
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


}
