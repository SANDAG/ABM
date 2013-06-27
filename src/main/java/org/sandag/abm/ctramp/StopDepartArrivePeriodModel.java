package org.sandag.abm.ctramp;

import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import org.sandag.abm.ctramp.ModelStructure;
import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 * This class will be used for determining the number of stops on individual
 * mandatory, individual non-mandatory and joint tours.
 * 
 * @author Christi Willison
 * @version Nov 4, 2008
 *          <p/>
 *          Created by IntelliJ IDEA.
 */
public class StopDepartArrivePeriodModel implements Serializable
{

    private transient Logger            logger                                    = Logger.getLogger(StopDepartArrivePeriodModel.class);

    private static final String         PROPERTIES_STOP_TOD_LOOKUP_FILE           = "stop.depart.arrive.proportions";

    // define names used in lookup file
    private static final String         TOUR_PURPOSE_INDEX_COLUMN_HEADING         = "tourpurp";
    private static final String         HALF_TOUR_DIRECTION_COLUMN_HEADING        = "isInbound";
    private static final String         TOUR_TOD_PERIOD_HEADING                   = "interval";
    private static final String         TRIP_NUMBER_COLUMN_HEADING                = "trip";
    private static final String         INTERVAL_1_PROPORTION_COLUMN_HEADING      = "p1";

    private static final int            NUM_DIRECTIONS                            = 2;
    private static final int            NUM_TRIPS                                 = 4;


    private double[][][][][]            proportions;

    private ModelStructure              modelStructure;

    
    /**
     * Constructor
     * @param propertyMap - properties HashMap
     * @param modelStructure - model definitions helper class
     */
    public StopDepartArrivePeriodModel(HashMap<String, String> propertyMap, ModelStructure modelStructure)
    {
        this.modelStructure = modelStructure;
        setupModels(propertyMap);
    }

    private void setupModels(HashMap<String, String> propertyMap)
    {

        logger.info(String.format("setting up stop depart/arrive choice model."));

        String uecPath = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String propsFile = uecPath + propertyMap.get(PROPERTIES_STOP_TOD_LOOKUP_FILE);

        // read the stop purpose lookup table data and populate the maps used to assign stop purposes
        readLookupProportions(propsFile);

    }

    private void readLookupProportions(String propsLookupFilename)
    {

        // read the stop purpose proportions into a TableDataSet
        TableDataSet propsLookupTable = null;
        String fileName = "";
        try {
            OLD_CSVFileReader reader = new OLD_CSVFileReader();
            propsLookupTable = reader.readFile(new File(propsLookupFilename));
        }
        catch (Exception e) {
            logger.error( String.format( "Exception occurred reading stop purpose lookup proportions file: %s.", fileName ), e );
            throw new RuntimeException();
        }

        
        // allocate an array for storing proportions arrays.
        int lastInterval = modelStructure.getTimePeriodIndexForTime( ModelStructure.LAST_TOD_INTERVAL_HOUR );
        proportions = new double[ModelStructure.NUM_PRIMARY_PURPOSES+1][NUM_DIRECTIONS][lastInterval+1][NUM_TRIPS+1][lastInterval+1];


        
        // fields in lookup file are:
        // tourpurp isInbound interval trip p1-p40 (alternative interval proportions)

        // populate the outProportionsMaps and inProportionsMaps arrays of maps from data in the TableDataSet.
        // when stops are generated, they can lookup the proportions for stop depart or arrive interval determined
        // by tour purpose, outbound/inbound direction and interval of previous trip. From these proportions,
        // a stop tod interval can be drawn.

        // loop over rows in the TableDataSet
        for (int i = 0; i < propsLookupTable.getRowCount(); i++) {

            // get the tour primary purpose index (1-10)
            int tourPrimaryPurposeIndex = (int) propsLookupTable.getValueAt(i + 1, TOUR_PURPOSE_INDEX_COLUMN_HEADING);

            // get the half tour direction (0 for outbound or 1 for inbound)
            int direction = (int) propsLookupTable.getValueAt(i + 1, HALF_TOUR_DIRECTION_COLUMN_HEADING);

            // get the tod interval (1-40)
            int todInterval = (int) propsLookupTable.getValueAt(i + 1, TOUR_TOD_PERIOD_HEADING);

            // get the trip number (1-4)
            int tripNumber = (int) propsLookupTable.getValueAt(i + 1, TRIP_NUMBER_COLUMN_HEADING);

            // get the index of the first alternative TOD interval proportion.
            int firstPropColumn = propsLookupTable.getColumnPosition(INTERVAL_1_PROPORTION_COLUMN_HEADING);

            // starting at this column, read the proportions for all TOD interval proportions.
            // Create the array of proportions for this table record.
            for (int j = 1; j <= lastInterval; j++)
                proportions[tourPrimaryPurposeIndex][direction][todInterval][tripNumber][j] = propsLookupTable.getValueAt(i + 1, firstPropColumn + j-1);

        }

    }

    public double[] getStopTodIntervalProportions( int tourPrimaryPurposeIndex, int direction, int prevTripTodInterval, int tripNumber ) {        
        return proportions[tourPrimaryPurposeIndex][direction][prevTripTodInterval][tripNumber];
    }

}
