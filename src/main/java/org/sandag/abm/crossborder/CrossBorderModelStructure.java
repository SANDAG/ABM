package org.sandag.abm.crossborder;

import org.sandag.abm.application.SandagModelStructure;

public class CrossBorderModelStructure
        extends SandagModelStructure
{

    public static final byte     NUMBER_CROSSBORDER_PURPOSES = 6;
    public static final byte     WORK                        = 0;
    public static final byte     SCHOOL                      = 1;
    public static final byte     CARGO                       = 2;
    public static final byte     SHOP                        = 3;
    public static final byte     VISIT                       = 4;
    public static final byte     OTHER                       = 5;

    public static final String[] CROSSBORDER_PURPOSES        = {"WORK", "SCHOOL", "CARGO", "SHOP",
            "VISIT", "OTHER"                                 };

    public static final byte     DEPARTURE                   = 0;
    public static final byte     ARRIVAL                     = 1;

    public static final int      AM                          = 0;
    public static final int      PM                          = 1;
    public static final int      OP                          = 2;
    public static final int[]    SKIM_PERIODS                = {AM, PM, OP};
    public static final String[] SKIM_PERIOD_STRINGS         = {"AM", "PM", "OP"};
    public static final int      UPPER_EA                    = 3;
    public static final int      UPPER_AM                    = 9;
    public static final int      UPPER_MD                    = 22;
    public static final int      UPPER_PM                    = 29;
    public static final String[] MODEL_PERIOD_LABELS         = {"EA", "AM", "MD", "PM", "EV"};

    public static final byte     TOUR_MODES                  = 4;

    public static final byte     DRIVEALONE                  = 1;
    public static final byte     SHARED2                     = 2;
    public static final byte     SHARED3                     = 3;
    public static final byte     WALK                        = 4;

    // note that time periods start at 1 and go to 40
    public static final byte     TIME_PERIODS                = 40;

    /**
     * Calculate and return the destination choice size term segment
     * 
     * @param purpose
     * @return Right now, just the purpose is returned.
     */
    public static int getDCSizeSegment(int purpose)
    {

        return purpose;

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

        return segment;
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

}
