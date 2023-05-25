package org.sandag.abm.airport;

public final class AirportModelStructure
{

    public static final byte     PURPOSES_CBX        = 5;
    public static final byte     PURPOSES_SAN        = 6;
    public static final byte     RESIDENT_BUSINESS   = 0;
    public static final byte     RESIDENT_PERSONAL   = 1;
    public static final byte     VISITOR_BUSINESS    = 2;
    public static final byte     VISITOR_PERSONAL    = 3;
    public static final byte     EXTERNAL            = 4;
    public static final byte	 EMPLOYEE			 = 5;

    public static final byte     INTERNAL_PURPOSES   = 4;

    public static final byte     DEPARTURE           = 0;
    public static final byte     ARRIVAL             = 1;

    public static final byte     INCOME_SEGMENTS     = 8;
    public static final byte     DC_SIZE_SEGMENTS    = INCOME_SEGMENTS * 2 + 2;

    public static final int      AM                  = 0;
    public static final int      PM                  = 1;
    public static final int      OP                  = 2;
    public static final int[]    SKIM_PERIODS        = {AM, PM, OP};
    public static final String[] SKIM_PERIOD_STRINGS = {"AM", "PM", "OP"};
    public static final int      UPPER_EA            = 3;
    public static final int      UPPER_AM            = 9;
    public static final int      UPPER_MD            = 22;
    public static final int      UPPER_PM            = 29;
    public static final String[] MODEL_PERIOD_LABELS = {"EA", "AM", "MD", "PM", "EV"};

    public static final byte     ACCESS_MODES_CBX        = 11;
    
    public static final int      ACCESS_MODES_SAN        = 17;

    public static final byte     PARK_TMNL           = 1;
    public static final byte     PARK_SANOFF         = 2;
    public static final byte     PARK_PVTOFF         = 3;
    public static final byte     PUDO_ESC            = 4;
    public static final byte     PUDO_CURB           = 5;
    public static final byte     RENTAL_CBX              = 6;
    public static final byte     TAXI                = 7;
    public static final byte     TNC_SINGLE          = 8;
    public static final byte     TNC_SHARED          = 9;
    public static final byte     SHUTTLE_VAN_CBX         = 10;
    public static final byte     TRANSIT_CBX             = 11;
    
    public static final int     PARK_LOC1           = 1;
    public static final int     PARK_LOC2           = 2;
    public static final int     PARK_LOC3           = 3;
    public static final int     PARK_LOC4		    = 4;
    public static final int     PARK_LOC5		    = 5;
    public static final int     PARK_ESC		    = 6;
    public static final int     RENTAL_SAN              = 7;
    public static final int     SHUTTLE_VAN_SAN         = 8;
    public static final int     HOTEL_COURTESY      = 9;
    public static final int     RIDEHAILING_LOC1    = 10;
    public static final int     RIDEHAILING_LOC2    = 11;
    public static final int     TRANSIT_SAN             = 12;
    public static final int     CURB_LOC1           = 13;
    public static final int     CURB_LOC2           = 14;
    public static final int     CURB_LOC3           = 15;
    public static final int     CURB_LOC4           = 16;
    public static final int     CURB_LOC5           = 17;
    
    public static final int     MGRAAlt_TERM         = 8;
    public static final int     MGRAAlt_CMH         = 9;
    
    public static final int     LOS_TYPE        = 4;
    
    public static final int     DA        		= 0;
    public static final int     SR2        		= 1;
    public static final int     SR3        		= 2;
    public static final int     Transit         = 3;
    
    public static final int     rideModeLRT_index     = 3;
    
    public static final int     employeePark_MGRA_index = 1;
    public static final int     employeePark_stall_index = 2;
    public static final int     employeePark_terminalpct_index = 3;
    public static final int     employeePark_publictransitpct_index = 4;    
    
    public static final int     airport_travel_party_trip_leg_1 = 1;
    public static final int     airport_travel_party_trip_leg_2 = 2;
    
    //reallocate the trip modes from the access choice model to ones that the trip table and other code can read, consistent with
    //resident models.
    public static final byte     REALLOCATE_WLKTRN     = 6; //walk access
    public static final byte     REALLOCATE_KNRPERTRN  = 8; //knr-personal tNCVehicle
    public static final byte     REALLOCATE_KNRTNCTRN  = 9; //knr-TNC
    public static final byte     REALLOCATE_TAXI       = 10; 
    public static final byte     REALLOCATE_TNCSINGLE  = 11; 
    public static final byte     REALLOCATE_TNCSHARED  = 12; 
  
    private AirportModelStructure()
    {
    }

    /**
     * Calculate and return the destination choice size term segment
     * 
     * @param purpose
     * @param income
     * @return The dc size term segment, currently 0-17, where: 0-7 are 8 income
     *         groups for RES_BUS 8-15 are 8 income groups for RES_PER 16 is
     *         VIS_BUS 17 is VIS_PER
     */
    public static int getDCSizeSegment(int purpose, int income)
    {

        int segment = -1;

        // size terms for resident trips are dimensioned by income
        if (purpose < 2)
        {
            segment = purpose * INCOME_SEGMENTS + income;
        } else
        {
            segment = 2 * INCOME_SEGMENTS + purpose - 2;
        }
        return segment;

    }

    /**
     * Calculate the purpose from the dc size segment.
     * 
     * @param segment
     *            The dc size segment (0-17)
     * @return The purpose
     */
    public static int getPurposeFromDCSizeSegment(int segment)
    {

        int purpose = -1;

        if (segment < INCOME_SEGMENTS)
        {
            purpose = 0;
        } else if (segment < (AirportModelStructure.INCOME_SEGMENTS * 2))
        {
            purpose = 1;
        } else if (segment == (AirportModelStructure.INCOME_SEGMENTS * 2)) purpose = 2;
        else purpose = 3;

        return purpose;
    }

    /**
     * Calculate the income from the dc size segment.
     * 
     * @param segment
     *            The dc size segment (0-17)
     * @return The income (defaults to 3 if not a resident purpose)
     */
    public static int getIncomeFromDCSizeSegment(int segment)
    {

        int income = 3;

        if (segment < AirportModelStructure.INCOME_SEGMENTS)
        {
            income = (byte) segment;
        } else if (segment < (AirportModelStructure.INCOME_SEGMENTS * 2))
            income = ((byte) (segment - AirportModelStructure.INCOME_SEGMENTS));

        return income;
    }

    /**
     * return the Skim period index 0=am, 1=pm, 2=off-peak
     */
    public static int getSkimPeriodIndex(int departPeriod)
    {

        int skimPeriodIndex = 0;

        if (departPeriod <= UPPER_EA) skimPeriodIndex = OP;
        else if (departPeriod <= UPPER_AM) skimPeriodIndex = AM;
        else if (departPeriod <= UPPER_MD) skimPeriodIndex = OP;
        else if (departPeriod <= UPPER_PM) skimPeriodIndex = PM;
        else skimPeriodIndex = OP;

        return skimPeriodIndex;

    }

    /**
     * return the Model period index 0=EA, 1=AM, 2=MD, 3=PM, 4=EV
     */
    public static int getModelPeriodIndex(int departPeriod)
    {

        int modelPeriodIndex = 0;

        if (departPeriod <= UPPER_EA) modelPeriodIndex = 0;
        else if (departPeriod <= UPPER_AM) modelPeriodIndex = 1;
        else if (departPeriod <= UPPER_MD) modelPeriodIndex = 2;
        else if (departPeriod <= UPPER_PM) modelPeriodIndex = 3;
        else modelPeriodIndex = 4;

        return modelPeriodIndex;

    }

    public static String getModelPeriodLabel(int period)
    {
        return MODEL_PERIOD_LABELS[period];
    }

    public static int getNumberModelPeriods()
    {
        return MODEL_PERIOD_LABELS.length;
    }

    public static String getSkimMatrixPeriodString(int period)
    {
        int index = getSkimPeriodIndex(period);
        return SKIM_PERIOD_STRINGS[index];
    }

    /**
     * Get the tNCVehicle occupancy based upon the access mode and the party size.
     * 
     * @param accessMode
     *            Access mode, 1-based, consistent with definitions above.
     * @param partySize
     *            Number of passengers in travel party
     * @return The (minimum) occupancy of the tNCVehicle trip to/from the airport.
     */
    public static int getOccupancy_cbx(int accessMode, int partySize)
    {

        switch (accessMode)
        {
            case PARK_TMNL:
                return partySize;
            case PARK_SANOFF:
                return partySize;
            case PARK_PVTOFF:
                return partySize;
            case PUDO_ESC:
                return partySize + 1;
            case PUDO_CURB:
                return partySize + 1;
            case RENTAL_CBX:
                return partySize;
            case TAXI:
                return partySize + 1;
            case TNC_SINGLE:
                return partySize + 1;
            case TNC_SHARED:
                return partySize + 1;
            case SHUTTLE_VAN_CBX:
                return partySize + 1;
            case TRANSIT_CBX:
                return partySize;

            default:
                throw new RuntimeException(
                        "Error:  AccessMode not found in AirportModel.AirportModelStructure");

        }
    }
    
    public static int getOccupancy_san(int accessMode, int partySize)
    {

        switch (accessMode)
        {
        	case PARK_LOC1:
        		return partySize;
        	case PARK_LOC2:
        		return partySize;
        	case PARK_LOC3:
        		return partySize;
        	case PARK_LOC4:
        		return partySize;
        	case PARK_LOC5:
        		return partySize;
        	case PARK_ESC:
        		return partySize + 1;
        	case RENTAL_SAN:
        		return partySize;
        	case SHUTTLE_VAN_SAN:
        		return partySize + 1;
        	case HOTEL_COURTESY:
        		return partySize + 1;
        	case RIDEHAILING_LOC1:
        		return partySize + 1;
        	case RIDEHAILING_LOC2:
        		return partySize + 1;
        	case TRANSIT_SAN:
        		return partySize;
        	case CURB_LOC1:
        		return partySize + 1;
        	case CURB_LOC2:
        		return partySize + 1;
        	case CURB_LOC3:
        		return partySize + 1;
        	case CURB_LOC4:
        		return partySize + 1;
        	case CURB_LOC5:
        		return partySize + 1;

            default:
                throw new RuntimeException(
                        "Error:  AccessMode not found in AirportModel.AirportModelStructure");

        }
    }
    
    public static boolean taxiTncMode_cbx(int accessMode) {
    	
    	switch (accessMode) {
    		case TAXI:
    			return true;
    		case TNC_SINGLE:
    			return true;
    		case TNC_SHARED:
    			return true;
    	}
    	
    	return false;
    }
    
    public static boolean taxiTncMode_san(int accessMode) {
    	
    	switch (accessMode) {
    		case RIDEHAILING_LOC1:
    			return true;
    		case RIDEHAILING_LOC2:
    			return true;
    	}
    	
    	return false;
    }
}
