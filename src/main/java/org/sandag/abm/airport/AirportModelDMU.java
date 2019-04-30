package org.sandag.abm.airport;

import java.io.Serializable;
import java.util.HashMap;

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

    protected double                          walkTransitLogsum;
    protected double                          driveTransitLogsum;
    
    protected Logger                          _logger                       = null;
   
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
            default:
            	_logger.error( "method number = " + variableIndex + " not found" );
                throw new RuntimeException( "method number = " + variableIndex + " not found" );
        }
        return returnValue;
        
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
