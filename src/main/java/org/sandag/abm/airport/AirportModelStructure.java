package org.sandag.abm.airport;


public final class AirportModelStructure
{

    public static final byte     PURPOSES            = 5;
    public static final byte     RESIDENT_BUSINESS   = 0;
    public static final byte     RESIDENT_PERSONAL   = 1;
    public static final byte     VISITOR_BUSINESS    = 2;
    public static final byte     VISITOR_PERSONAL    = 3;
    public static final byte     EXTERNAL            = 4;

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

    public static final byte     ACCESS_MODES        = 9;

    public static final byte     PARK_TMNL           = 1;
    public static final byte     PARK_SANOFF         = 2;
    public static final byte     PARK_PVTOFF         = 3;
    public static final byte     PUDO_ESC            = 4;
    public static final byte     PUDO_CURB           = 5;
    public static final byte     RENTAL              = 6;
    public static final byte     TAXI                = 7;
    public static final byte     SHUTTLE_VAN         = 8;
    public static final byte     TRANSIT             = 9;
    
    public AirportModelStructure()
    {
        //Not Implemented
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
     * Get the vehicle occupancy based upon the access mode and the party size.
     * 
     * @param accessMode
     *            Access mode, 1-based, consistent with definitions above.
     * @param partySize
     *            Number of passengers in travel party
     * @return The (minimum) occupancy of the vehicle trip to/from the airport.
     */
    public static int getOccupancy(int accessMode, int partySize)
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
            case RENTAL:
                return partySize;
            case TAXI:
                return partySize + 1;
            case SHUTTLE_VAN:
                return partySize + 1;
            case TRANSIT:
                return partySize;

            default:
                throw new RuntimeException(
                        "Error:  AccessMode not found in AirportModel.AirportModelStructure");

        }
    }

}
