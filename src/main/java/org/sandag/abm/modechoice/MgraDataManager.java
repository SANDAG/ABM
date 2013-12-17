package org.sandag.abm.modechoice;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.Modes.AccessMode;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;

/**
 * This class is used for ...
 * 
 * @author Christi Willison
 * @version Sep 4, 2008
 *          <p/>
 *          Created by IntelliJ IDEA.
 * 
 *          Edited JEF May 2009
 */
public final class MgraDataManager
        implements Serializable
{

    private static MgraDataManager      instance;
    protected transient Logger          logger                                   = Logger.getLogger(MgraDataManager.class);

    public static final double          MAX_PARKING_WALK_DISTANCE                = 0.75;

    private static final int            LOG_MGRA                                 = -4502;
    private static final String         LOG_MGRA_FILE                            = LOG_MGRA
                                                                                         + "debug";

    // create Strubg variables for the 4D land use data file field names
    public static final String          MGRA_4DDENSITY_DU_DEN_FIELD              = "DUDen";
    public static final String          MGRA_4DDENSITY_EMP_DEN_FIELD             = "EmpDen";
    public static final String          MGRA_4DDENSITY_TOT_INT_FIELD             = "TotInt";

    // public static final String MGRA_FIELD_NAME = "MGRASR10";
    public static final String          MGRA_FIELD_NAME                          = "mgra";
    public static final String          MGRA_TAZ_FIELD_NAME                      = "TAZ";
    public static final String          MGRA_LUZ_FIELD_NAME                      = "luz_id";
    private static final String         MGRA_POPULATION_FIELD_NAME               = "pop";
    private static final String         MGRA_HOUSEHOLDS_FIELD_NAME               = "hh";
    private static final String         MGRA_GRADE_SCHOOL_ENROLLMENT_FIELD_NAME  = "EnrollGradeKto8";
    private static final String         MGRA_HIGH_SCHOOL_ENROLLMENT_FIELD_NAME   = "EnrollGrade9to12";
    private static final String         MGRA_UNIVERSITY_ENROLLMENT_FIELD_NAME    = "collegeEnroll";
    private static final String         MGRA_OTHER_COLLEGE_ENROLLMENT_FIELD_NAME = "otherCollegeEnroll";
    private static final String         MGRA_ADULT_SCHOOL_ENROLLMENT_FIELD_NAME  = "AdultSchEnrl";
    private static final String         MGRA_GRADE_SCHOOL_DISTRICT_FIELD_NAME    = "ech_dist";
    private static final String         MGRA_HIGH_SCHOOL_DISTRICT_FIELD_NAME     = "hch_dist";

    private static final String         PROPERTIES_PARKING_COST_OUTPUT_FILE      = "mgra.avg.cost.output.file";

    public static final String          PROPERTIES_MGRA_DATA_FILE                = "mgra.socec.file";
    private static final String         MGRA_DISTANCE_COEFF_WORK                 = "mgra.avg.cost.dist.coeff.work";
    private static final String         MGRA_DISTANCE_COEFF_OTHER                = "mgra.avg.cost.dist.coeff.other";

    public static final int             PARK_AREA_ONE                            = 1;
    private static final String         MGRA_PARKAREA_FIELD                      = "parkarea";
    private static final String         MGRA_HSTALLSOTH_FIELD                    = "hstallsoth";
    private static final String         MGRA_HSTALLSSAM_FIELD                    = "hstallssam";
    private static final String         MGRA_HPARKCOST_FIELD                     = "hparkcost";
    private static final String         MGRA_NUMFREEHRS_FIELD                    = "numfreehrs";
    private static final String         MGRA_DSTALLSOTH_FIELD                    = "dstallsoth";
    private static final String         MGRA_DSTALLSSAM_FIELD                    = "dstallssam";
    private static final String         MGRA_DPARKCOST_FIELD                     = "dparkcost";
    private static final String         MGRA_MSTALLSOTH_FIELD                    = "mstallsoth";
    private static final String         MGRA_MSTALLSSAM_FIELD                    = "mstallssam";
    private static final String         MGRA_MPARKCOST_FIELD                     = "mparkcost";

    private ArrayList<Integer>          mgras                                    = new ArrayList<Integer>();
    private int                         maxMgra;
    private int                         maxLuz;

    private int                         maxTap;
    private int                         nMgrasWithWlkTaps;
    // [mgra], [0=tapID, 1=Distance], [tap number (0-number of taps)]
    private int[][][]                   mgraWlkTapsDistArray;
    private int[]                       mgraTaz;
    private int[]                       mgraLuz;

    // An array of Hashmaps dimensioned by origin mgra, with distance in feet,
    // in a ragged
    // array (no key for mgra means no other mgras in walk distance)
    private HashMap<Integer, Integer>[] oMgraWalkDistance;

    // An array of Hashmaps dimensioned by destination mgra, with distance in
    // feet, in a ragged
    // array (no key for mgra means no other mgras in walk distance)
    private HashMap<Integer, Integer>[] dMgraWalkDistance;

    // An array dimensioned to maxMgra of ragged arrays of lists of TAPs
    // accessible by driving
    private Set<Integer>[]              driveAccessibleTaps;
    private Set<Integer>[]              walkAccessibleTaps;

    private TableDataSet                mgraTableDataSet;

    private HashMap<Integer, Integer>   mgraDataTableMgraRowMap;

    private double[]                    duDen;
    private double[]                    empDen;
    private double[]                    totInt;

    private double[]                    lsWgtAvgCostM;
    private double[]                    lsWgtAvgCostD;
    private double[]                    lsWgtAvgCostH;

    private int[]                       mgraParkArea;
    private int[]                       numfreehrs;
    private int[]                       hstallsoth;
    private int[]                       hstallssam;
    private float[]                     hparkcost;
    private int[]                       dstallsoth;
    private int[]                       dstallssam;
    private float[]                     dparkcost;
    private int[]                       mstallsoth;
    private int[]                       mstallssam;
    private float[]                     mparkcost;

    /**
     * Constructor.
     * 
     * @param rbMap
     *            A HashMap created from a resourcebundle with model properties.
     * 
     */
    private MgraDataManager(HashMap<String, String> rbMap)
    {
        System.out.println("I'm the MgraDataManager");
        readMgraTableData(rbMap);
        readMgraWlkTaps(rbMap);
        readMgraWlkDist(rbMap);

        // pre-process the list of TAPS reachable by drive access for each MGRA
        mapDriveAccessTapsToMgras(TazDataManager.getInstance(rbMap));

        // create arrays from 4ddensity fields added to MGRA table used by
        // TourModeChoice DMU methods
        process4ddensityData(rbMap);

        calculateMgraAvgParkingCosts(rbMap);

        printMgraStats();
    }

    /**
     * Get a static instance of the Mgra data manager. Creates one if it is not
     * initialized.
     * 
     * @param rb
     *            A resourcebundle with appropriate property settings.
     * 
     * @return A static instance of the MGRA data manager.
     */
    public static MgraDataManager getInstance(HashMap<String, String> rbMap)
    {
        if (instance == null)
        {
            instance = new MgraDataManager(rbMap);
            return instance;
        } else return instance;
    }

    /**
     * This method should only be used after the getInstance(ResourceBundle rb)
     * method has been called since the rb is needed to read in all the data and
     * populate the object. This method will return the instance that has
     * already been populated.
     * 
     * @return instance
     * @throws RuntimeException
     */
    public static MgraDataManager getInstance()
    {
        if (instance == null)
        {
            throw new RuntimeException(
                    "Must instantiate MgraDataManager with the getInstance(rb) method first");
        } else
        {
            return instance;
        }
    }

    /**
     * Read the walk-transit taps for mgras.
     * 
     * @param rb
     *            The resourcebundle with the scenario.path and
     *            mgra.wlkacc.taps.and.distance.file properties.
     */
    public void readMgraWlkTaps(HashMap<String, String> rbMap)
    {
        File mgraWlkTapCorresFile = new File(Util.getStringValueFromPropertyMap(rbMap,
                "scenario.path")
                + Util.getStringValueFromPropertyMap(rbMap, "mgra.wlkacc.taps.and.distance.file"));
        gotta fix this filename
        Map<Integer,Map<Integer,int[]>> mgraWlkTapList = new HashMap<>(); //mgra -> tap -> [board dist,alight dist]
        String s;
        try ( BufferedReader br = new BufferedReader(new FileReader(mgraWlkTapCorresFile)))
        { 
            // read the first data file line containing column names
            s = br.readLine();
                
            // read the data records
            while ((s = br.readLine()) != null)
            {
            	StringTokenizer st = new StringTokenizer(s, ",");
                int mgra = Integer.parseInt(st.nextToken());
                int tap = Integer.parseInt(st.nextToken());
                if (tap > maxTap) maxTap = tap;
                float boardTime = Float.parseFloat(st.nextToken());
                float alightTime = Float.parseFloat(st.nextToken());
                int boardDist = Math.round(boardTime / Constants.walkMinutesPerFoot + 0.5f);
                int alightDist = Math.round(boardTime / Constants.walkMinutesPerFoot + 0.5f);
                if (!mgraWlkTapList.containsKey(mgra))
                	mgraWlkTapList.put(mgra,new HashMap<Integer,int[]>());
                mgraWlkTapList.get(mgra).put(tap,new int[] {boardDist,alightDist});
            }
        }catch (IOException e) {
			logger.error(e);
			throw new RuntimeException(e);
		} 

        // now go thru the array of ArrayLists and convert the lists to arrays
        // and
        // store in the class variable mgraWlkTapsDistArrays.
        mgraWlkTapsDistArray = new int[maxMgra + 1][3][];
        nMgrasWithWlkTaps = mgraWlkTapList.size();
        for (int mgra : mgraWlkTapList.keySet()) {
        	Map<Integer,int[]> wlkTapList = mgraWlkTapList.get(mgra);
        	mgraWlkTapsDistArray[mgra][0] = new int[wlkTapList.size()];
        	int counter = 0;
        	for (int tap : new TreeSet<Integer>(wlkTapList.keySet())) { //get the taps in ascending order - not sure if this matters, but it is cleaner
        		int[] dists = wlkTapList.get(tap);
        		mgraWlkTapsDistArray[mgra][0][counter] = tap;
        		mgraWlkTapsDistArray[mgra][1][counter] = dists[0];
        		mgraWlkTapsDistArray[mgra][2][counter] = dists[1];
        	}
        }
    }

    /**
     * Read the walk-transit taps for mgras.
     * 
     * @param rb
     *            ResourceBundle with scenario.path and mgra.walkdistance.file
     *            property.
     */
    public void readMgraWlkDist(HashMap<String, String> rbMap)
    {
        File mgraWlkDistFile = new File(Util.getStringValueFromPropertyMap(rbMap, "scenario.path")
                + Util.getStringValueFromPropertyMap(rbMap, "mgra.walkdistance.file"));
        gotta fix this filename
        oMgraWalkDistance = new HashMap[maxMgra + 1];
        dMgraWalkDistance = new HashMap[maxMgra + 1];
        String s;
        try (BufferedReader br = new BufferedReader(new FileReader(mgraWlkDistFile)))
        {
            while ((s = br.readLine()) != null)
            {
                StringTokenizer st = new StringTokenizer(s, " ");

                // skip lines if zone, number of mgra-pairs exists in file
                if (st.countTokens() < 3) continue;

                int oMgra = Integer.parseInt(st.nextToken());
                int dMgra = Integer.parseInt(st.nextToken());
                int dist =  Math.round(Float.parseFloat(st.nextToken()) / Constants.walkMinutesPerFoot + 0.5f);

                if (oMgraWalkDistance[oMgra] == null)
                {
                    oMgraWalkDistance[oMgra] = new HashMap<Integer, Integer>();
                    oMgraWalkDistance[oMgra].put(dMgra, dist);
                } else
                {
                    oMgraWalkDistance[oMgra].put(dMgra, dist);
                }

                if (dMgraWalkDistance[dMgra] == null)
                {
                    dMgraWalkDistance[dMgra] = new HashMap<Integer, Integer>();
                    dMgraWalkDistance[dMgra].put(oMgra, dist);
                } else
                {
                    dMgraWalkDistance[dMgra].put(oMgra, dist);
                }
            }
        } catch (IOException e) {
			logger.error(e);
			throw new RuntimeException(e);
		} 

    }

    /**
     * Return an int array of mgras within walking distance of this mgra.
     * 
     * @param mgra
     *            The mgra to look up
     * @return The mgras within walking distance. Null is returned if no mgras
     *         are within walk distance.
     */
    public int[] getMgrasWithinWalkDistanceFrom(int mgra)
    {

        if (oMgraWalkDistance[mgra] == null) return null;

        Set<Integer> keySet = oMgraWalkDistance[mgra].keySet();
        int[] walkMgras = new int[keySet.size()];
        Iterator<Integer> it = keySet.iterator();
        int i = 0;
        while (it.hasNext())
        {
            walkMgras[i] = it.next();
            ++i;
        }
        return walkMgras;

    }

    /**
     * Return an int array of mgras within walking distance of this mgra.
     * 
     * @param mgra
     *            The mgra to look up
     * @return The mgras within walking distance. Null is returned if no mgras
     *         are within walk distance.
     */
    public int[] getMgrasWithinWalkDistanceTo(int mgra)
    {

        if (dMgraWalkDistance[mgra] == null) return null;

        Set<Integer> keySet = dMgraWalkDistance[mgra].keySet();
        int[] walkMgras = new int[keySet.size()];
        Iterator<Integer> it = keySet.iterator();
        int i = 0;
        while (it.hasNext())
        {
            walkMgras[i] = it.next();
            ++i;
        }
        return walkMgras;

    }

    /**
     * Return true if mgras are within walking distance of each other.
     * 
     * @param oMgra
     *            The from mgra
     * @param dMgra
     *            The to mgra
     * @return The mgras are within walking distance - true or false.
     */
    public boolean getMgrasAreWithinWalkDistance(int oMgra, int dMgra)
    {

        if (dMgraWalkDistance[dMgra] == null) return false;

        return dMgraWalkDistance[dMgra].containsKey(oMgra);

    }

    /**
     * Get the walk distance from an MGRA to a TAP.
     * 
     * @param mgra
     *            The number of the destination MGRA.
     * @param pos
     *            The position of the TAP in the MGRA array (0+)
     * @return The walk distance in feet.
     */
    public float getMgraToTapWalkBoardDist(int mgra, int pos)
    {
        return mgraWlkTapsDistArray[mgra][1][pos];
    }

    /**
     * Get the walk alight distance from a TAP to an MGRA.
     * 
     * @param mgra The number of the destination MGRA.
     * @param pos The position of the TAP in the MGRA array (0+)
     * @return The walk distance in feet.
     */
    public float getMgraToTapWalkAlightDist(int mgra, int pos)
    {
        return mgraWlkTapsDistArray[mgra][2][pos];
    }

//    /**
//     * Get the walk distance from an MGRA to a TAP.
//     * 
//     * @param mgra The number of the destination MGRA.
//     * @param pos The position of the TAP in the MGRA array (0+)
//     * @return The walk distance in feet.
//     */
//    public float getMgraToTapWalkDist(int mgra, int pos)
//    {
//        return mgraWlkTapsDistArray[mgra][1][pos]; // [1] = distance
//    }

    /**
     * Get the position of the tap in the mgra walk tap array.
     * 
     * @param mgra
     *            The mgra to lookup
     * @param tap
     *            The tap to lookup
     * @return The position of the tap in the mgra array. -1 is returned if it
     *         is an invalid tap for the mgra, or if the tap is not within
     *         walking distance.
     */
    public int getTapPosition(int mgra, int tap)
    {

        if (mgraWlkTapsDistArray[mgra] != null)
        {
            if (mgraWlkTapsDistArray[mgra][0] != null)
            {
                for (int i = 0; i < mgraWlkTapsDistArray[mgra][0].length; ++i)
                    if (mgraWlkTapsDistArray[mgra][0][i] == tap) return i;
            }
        }

        return -1;

    }

    /**
     * Get the walk board time from an MGRA to a TAP.
     * 
     * @param mgra
     *            The number of the destination MGRA.
     * @param pos
     *            The position of the TAP in the MGRA array (0+)
     * @return The walk time in minutes.
     */
    public float getMgraToTapWalkBoardTime(int mgra, int pos)
    {
        return ((float) mgraWlkTapsDistArray[mgra][1][pos]) * Constants.walkMinutesPerFoot;
    }

    /**
     * Get the walk alight time from a TAP to an MGRA.
     * 
     * @param mgra The number of the destination MGRA.
     * @param pos The position of the TAP in the MGRA array (0+)
     * @return The walk time in minutes.
     */
    public float getMgraToTapWalkAlightTime(int mgra, int pos)
    {
        return ((float) mgraWlkTapsDistArray[mgra][2][pos]) * Constants.walkMinutesPerFoot;
    }

//    /**
//     * Get the walk time from an MGRA to a TAP.
//     * 
//     * @param mgra The number of the destination MGRA.
//     * @param pos The position of the TAP in the MGRA array (0+)
//     * @return The walk time in minutes.
//     */
//    public float getMgraToTapWalkTime(int mgra, int pos)
//    {
//        return ((float) mgraWlkTapsDistArray[mgra][1][pos]) * Constants.walkMinutesPerFoot;
//    }

    /**
     * Get the walk distance from an MGRA to an MGRA. Return 0 if not within walking
     * distance.
     * 
     * @param oMgra
     *            The number of the production/origin MGRA.
     * @param dMgra
     *            The number of the attraction/destination MGRA.
     * @return The walk distance in feet.
     */
    public int getMgraToMgraWalkDistFrom(int oMgra, int dMgra)
    {

        if (oMgraWalkDistance[oMgra] == null) return 0;
        else if (oMgraWalkDistance[oMgra].containsKey(dMgra))
            return oMgraWalkDistance[oMgra].get(dMgra);

        return 0;
    }

    /**
     * Get the walk distance from an MGRA to an MGRA. Return 0 if not within
     * walking distance.
     * 
     * @param oMgra
     *            The number of the production/origin MGRA.
     * @param dMgra
     *            The number of the attraction/destination MGRA.
     * @return The walk distance in feet.
     */
    public int getMgraToMgraWalkDistTo(int oMgra, int dMgra)
    {

        if (dMgraWalkDistance[dMgra] == null) return 0;
        else if (dMgraWalkDistance[dMgra].containsKey(oMgra))
            return dMgraWalkDistance[dMgra].get(oMgra);

        return 0;
    }

    /**
     * Get the walk time from an MGRA to an MGRA. Return 0 if not within walking
     * distance.
     * 
     * @param oMgra
     *            The number of the production/origin MGRA.
     * @param dMgra
     *            The number of the attraction/destination MGRA.
     * @return The walk time in minutes.
     */
    public float getMgraToMgraWalkTime(int oMgra, int dMgra)
    {

        if (oMgraWalkDistance[oMgra] == null) return 0f;
        else if (oMgraWalkDistance[oMgra].containsKey(dMgra))
            return ((float) oMgraWalkDistance[oMgra].get(dMgra)) * Constants.walkMinutesPerFoot;

        return 0f;
    }

    /**
     * Get the bike time from an MGRA to an MGRA. Return 0 if not within walking
     * distance.
     * 
     * @param oMgra
     *            The number of the production/origin MGRA.
     * @param dMgra
     *            The number of the attraction/destination MGRA.
     * @return The bike time in minutes.
     */
    public float getMgraToMgraBikeTime(int oMgra, int dMgra)
    {

        if (oMgraWalkDistance[oMgra] == null) return 0f;
        else if (oMgraWalkDistance[oMgra].containsKey(dMgra))
            return ((float) oMgraWalkDistance[oMgra].get(dMgra)) * Constants.bikeMinutesPerFoot;

        return 0f;
    }

    /**
     * Print mgra data to the log file for debugging purposes.
     * 
     */
    public void printMgraStats()
    {
        logger.info("Number of MGRAs: " + mgras.size());
        logger.info("Max MGRA: " + maxMgra);

        // logger.info("Number of MGRAs with WalkAccessTaps: " +
        // nMgrasWithWlkTaps);
        // logger.info("Number of TAPs in MGRA 18 (should be 3): "
        // + mgraWlkTapsDistArray[18][0].length);
        // logger.info("Distance between MGRA 18 and TAP 1648 (should be 2728): "
        // + mgraWlkTapsDistArray[18][1][1]);
        // logger.info("MGRA 28435 is in what TAZ? (Should be 995)" +
        // mgraTaz[28435]);
        // logger.info("Number of mgras within walk distance of mgra 22573 (Should be 67)"
        // + getMgrasWithinWalkDistanceFrom(22573).length);

    }

    /**
     * 
     * @param mgra
     *            - the zone
     * @return the taz that the tmgra is contained in
     */
    public int getTaz(int mgra)
    {
        return mgraTaz[mgra];
    }

    /**
     * 
     * @param mgra
     *            - the zone
     * @return the luz that the mgra is contained in
     */
    public int getMgraLuz(int mgra)
    {
        return mgraLuz[mgra];
    }

    /**
     * Get the maximum LUZ.
     * 
     * @return The highest LUZ number
     */
    public int getMaxLuz()
    {
        return maxLuz;
    }

    /**
     * Get the maximum MGRA.
     * 
     * @return The highest MGRA number
     */
    public int getMaxMgra()
    {
        return maxMgra;
    }

    /**
     * Get the maximum TAP.
     * 
     * @return The highest TAP number
     */
    public int getMaxTap()
    {
        return maxTap;
    }

    /**
     * Get the ArrayList of MGRAs
     * 
     * @return ArrayList<Integer> mgras.
     */
    public ArrayList<Integer> getMgras()
    {
        return mgras;
    }

    /**
     * Get the MgraTaz correspondence array. Given an MGRA, returns its TAZ.
     * 
     * @return int[] mgraTaz correspondence array.
     */
    public int[] getMgraTaz()
    {
        return mgraTaz;
    }

    /**
     * Get the array of Taps within walk distance
     * 
     * @return The int[][][] array of Taps within walk distance of MGRAs
     */
    public int[][][] getMgraWlkTapsDistArray()
    {
        return mgraWlkTapsDistArray;
    }

    /**
     * get arrays of drive accessible TAPS for each MGRA and populate an array
     * of sets so that later one can determine, for a given mgra, if a tap is
     * contained in the set.
     * 
     * @param args
     *            TazDataManager to get TAPs with drive access from TAZs
     */
    public void mapDriveAccessTapsToMgras(TazDataManager tazDataManager)
    {

        walkAccessibleTaps = new TreeSet[maxMgra + 1];
        driveAccessibleTaps = new TreeSet[maxMgra + 1];

        for (int mgra = 1; mgra <= maxMgra; mgra++)
        {

            // get the TAZ associated with this MGRA
            int taz = getTaz(mgra);

            // store the array of walk accessible TAPS for this MGRA as a set so
            // that contains can be called on it later
            // to determine, for a given mgra, if a tap is contained in the set.
            int[] mgraSet = getMgraWlkTapsDistArray()[mgra][0];
            if (mgraSet != null)
            {
                walkAccessibleTaps[mgra] = new TreeSet<Integer>();
                for (int i = 0; i < mgraSet.length; i++)
                    walkAccessibleTaps[mgra].add(mgraSet[i]);
            }

            // store the array of drive accessible TAPS for this MGRA as a set
            // so that contains can be called on it later
            // to determine, for a given mgra, if a tap is contained in the set.
            int[] tapItems = tazDataManager.getParkRideOrKissRideTapsForZone(taz,
                    AccessMode.PARK_N_RIDE);
            driveAccessibleTaps[mgra] = new TreeSet<Integer>();
            for (int item : tapItems)
                driveAccessibleTaps[mgra].add(item);

        }

    }

    /**
     * @param mgra
     *            for which we want to know if TAP can be reached by drive
     *            access
     * @param tap
     *            for which we want to know if the mgra can reach it by drive
     *            access
     * @return true if reachable; false otherwise
     */
    public boolean getTapIsDriveAccessibleFromMgra(int mgra, int tap)
    {
        if (driveAccessibleTaps[mgra] == null) return false;
        else return driveAccessibleTaps[mgra].contains(tap);
    }

    /**
     * @param mgra
     *            for which we want to know if TAP can be reached by walk access
     * @param tap
     *            for which we want to know if the mgra can reach it by walk
     *            access
     * @return true if reachable; false otherwise
     */
    public boolean getTapIsWalkAccessibleFromMgra(int mgra, int tap)
    {
        if (walkAccessibleTaps[mgra] == null) return false;
        else return walkAccessibleTaps[mgra].contains(tap);
    }

    /**
     * return the duDen value for the mgra
     * 
     * @param mgra
     *            is the MGRA value for which the duDen value is needed
     * @return duDen[mgra]
     */
    public double getDuDenValue(int mgra)
    {
        return duDen[mgra];
    }

    /**
     * return the empDen value for the mgra
     * 
     * @param mgra
     *            is the MGRA value for which the empDen value is needed
     * @return empDen[mgra]
     */
    public double getEmpDenValue(int mgra)
    {
        return empDen[mgra];
    }

    /**
     * return the totInt value for the mgra
     * 
     * @param mgra
     *            is the MGRA value for which the totInt value is needed
     * @return totInt[mgra]
     */
    public double getTotIntValue(int mgra)
    {
        return totInt[mgra];
    }

    /**
     * Process the 4D density land use data file and store the selected fields
     * as arrays indexed by the mgra value. The data fields are in the mgra
     * TableDataSet read from the MGRA csv file.
     * 
     * @param rbMap
     *            is a HashMap for the resource bundle generated from the
     *            properties file.
     */
    public void process4ddensityData(HashMap<String, String> rbMap)
    {

        try
        {

            // allocate arrays for the land use data fields
            duDen = new double[maxMgra + 1];
            empDen = new double[maxMgra + 1];
            totInt = new double[maxMgra + 1];

            // get the data fields needed for the mode choice utilities as
            // 0-based double[]
            double[] duDenField = mgraTableDataSet.getColumnAsDouble(MGRA_4DDENSITY_DU_DEN_FIELD);
            double[] empDenField = mgraTableDataSet.getColumnAsDouble(MGRA_4DDENSITY_EMP_DEN_FIELD);
            double[] totIntField = mgraTableDataSet.getColumnAsDouble(MGRA_4DDENSITY_TOT_INT_FIELD);

            // create a HashMap to convert MGRA values to array indices for the
            // data
            // arrays above
            int mgraCol = mgraTableDataSet.getColumnPosition(MGRA_FIELD_NAME);

            for (int row = 1; row <= mgraTableDataSet.getRowCount(); row++)
            {

                int mgra = (int) mgraTableDataSet.getValueAt(row, mgraCol);
                duDen[mgra] = duDenField[row - 1];
                empDen[mgra] = empDenField[row - 1];
                totInt[mgra] = totIntField[row - 1];

            }

        } catch (Exception e)
        {
            logger.error(
                    String.format("Exception occurred processing 4ddensity data file from mgraData TableDataSet object."),
                    e);
            throw new RuntimeException();
        }

    }

    private void readMgraTableData(HashMap<String, String> rbMap)
    {

        // get the mgra data table from one of these UECs.
        String projectPath = rbMap.get(CtrampApplication.PROPERTIES_PROJECT_DIRECTORY);
        String mgraFile = rbMap.get(PROPERTIES_MGRA_DATA_FILE);
        mgraFile = projectPath + mgraFile;

        try
        {
            OLD_CSVFileReader reader = new OLD_CSVFileReader();
            mgraTableDataSet = reader.readFile(new File(mgraFile));
        } catch (IOException e)
        {
            logger.error("problem reading mgra data table for MgraDataManager.", e);
            System.exit(1);
        }

        HashMap<Integer, Integer> tazs = new HashMap<Integer, Integer>();
        HashMap<Integer, Integer> luzs = new HashMap<Integer, Integer>();

        // create a HashMap between mgra values and the corresponding row number
        // in the mgra TableDataSet.
        mgraDataTableMgraRowMap = new HashMap<Integer, Integer>();
        maxMgra = 0;
        maxLuz = 0;
        for (int i = 1; i <= mgraTableDataSet.getRowCount(); i++)
        {
            int mgra = (int) mgraTableDataSet.getValueAt(i, MGRA_FIELD_NAME);
            int taz = (int) mgraTableDataSet.getValueAt(i, MGRA_TAZ_FIELD_NAME);

            int luz = (int) mgraTableDataSet.getValueAt(i, MGRA_LUZ_FIELD_NAME);

            mgraDataTableMgraRowMap.put(mgra, i);

            if (mgra > maxMgra) maxMgra = mgra;
            mgras.add(mgra);

            tazs.put(mgra, taz);

            if (luz > 0)
            {
                if (luz > maxLuz) maxLuz = luz;
                luzs.put(mgra, luz);
            }
        }

        mgraTaz = new int[maxMgra + 1];
        for (int mgra : mgras)
            mgraTaz[mgra] = tazs.get(mgra);

        mgraLuz = new int[maxMgra + 1];
        for (int mgra : mgras)
            mgraLuz[mgra] = luzs.get(mgra);

    }

    /**
     * @param mgra
     *            for which table data is desired
     * @return population for the specified mgra.
     */
    public double getMgraPopulation(int mgra)
    {
        int row = mgraDataTableMgraRowMap.get(mgra);
        return mgraTableDataSet.getValueAt(row, MGRA_POPULATION_FIELD_NAME);
    }

    /**
     * @param mgra
     *            for which table data is desired
     * @return households for the specified mgra.
     */
    public double getMgraHouseholds(int mgra)
    {
        int row = mgraDataTableMgraRowMap.get(mgra);
        return mgraTableDataSet.getValueAt(row, MGRA_HOUSEHOLDS_FIELD_NAME);
    }

    /**
     * @param mgra
     *            for which table data is desired
     * @return grade school enrollment for the specified mgra.
     */
    public double getMgraGradeSchoolEnrollment(int mgra)
    {
        int row = mgraDataTableMgraRowMap.get(mgra);
        return mgraTableDataSet.getValueAt(row, MGRA_GRADE_SCHOOL_ENROLLMENT_FIELD_NAME);
    }

    /**
     * @param mgra
     *            for which table data is desired
     * @return high school enrollment for the specified mgra.
     */
    public double getMgraHighSchoolEnrollment(int mgra)
    {
        int row = mgraDataTableMgraRowMap.get(mgra);
        return mgraTableDataSet.getValueAt(row, MGRA_HIGH_SCHOOL_ENROLLMENT_FIELD_NAME);
    }

    /**
     * @param mgra
     *            for which table data is desired
     * @return university enrollment for the specified mgra.
     */
    public double getMgraUniversityEnrollment(int mgra)
    {
        int row = mgraDataTableMgraRowMap.get(mgra);
        return mgraTableDataSet.getValueAt(row, MGRA_UNIVERSITY_ENROLLMENT_FIELD_NAME);
    }

    /**
     * @param mgra
     *            for which table data is desired
     * @return other college enrollment for the specified mgra.
     */
    public double getMgraOtherCollegeEnrollment(int mgra)
    {
        int row = mgraDataTableMgraRowMap.get(mgra);
        return mgraTableDataSet.getValueAt(row, MGRA_OTHER_COLLEGE_ENROLLMENT_FIELD_NAME);
    }

    /**
     * @param mgra
     *            for which table data is desired
     * @return adult school enrollment for the specified mgra.
     */
    public double getMgraAdultSchoolEnrollment(int mgra)
    {
        int row = mgraDataTableMgraRowMap.get(mgra);
        return mgraTableDataSet.getValueAt(row, MGRA_ADULT_SCHOOL_ENROLLMENT_FIELD_NAME);
    }

    /**
     * @param mgra
     *            for which table data is desired
     * @return grade school district for the specified mgra.
     */
    public int getMgraGradeSchoolDistrict(int mgra)
    {
        int row = mgraDataTableMgraRowMap.get(mgra);
        return (int) mgraTableDataSet.getValueAt(row, MGRA_GRADE_SCHOOL_DISTRICT_FIELD_NAME);
    }

    /**
     * @param mgra
     *            for which table data is desired
     * @return high school district for the specified mgra.
     */
    public int getMgraHighSchoolDistrict(int mgra)
    {
        int row = mgraDataTableMgraRowMap.get(mgra);
        return (int) mgraTableDataSet.getValueAt(row, MGRA_HIGH_SCHOOL_DISTRICT_FIELD_NAME);
    }

    public HashMap<Integer, Integer> getMgraDataTableMgraRowMap()
    {
        return mgraDataTableMgraRowMap;
    }

    private void calculateMgraAvgParkingCosts(HashMap<String, String> propertyMap)
    {

        // open output file to write average parking costs for each mgra
        PrintWriter out = null;

        String projectPath = propertyMap.get(CtrampApplication.PROPERTIES_PROJECT_DIRECTORY);
        String outFile = propertyMap.get(PROPERTIES_PARKING_COST_OUTPUT_FILE);
        outFile = projectPath + outFile;

        try
        {
            out = new PrintWriter(new BufferedWriter(new FileWriter(new File(outFile))));
        } catch (IOException e)
        {
            logger.error("Exception caught trying to create file " + outFile);
            System.out.println("Exception caught trying to create file " + outFile);
            e.printStackTrace();
            throw new RuntimeException();
        }

        // write the header record
        out.println("mgra,mgraParkArea,lsWgtAvgCostM,lsWgtAvgCostD,lsWgtAvgCostH");

        // open output files for writing debug info for a specific mgra
        PrintWriter outM = null;
        PrintWriter outD = null;
        PrintWriter outH = null;

        if (LOG_MGRA > 0)
        {
            try
            {
                outM = new PrintWriter(new BufferedWriter(new FileWriter(new File(projectPath
                        + "output/" + LOG_MGRA_FILE + "M.csv"))));
                outD = new PrintWriter(new BufferedWriter(new FileWriter(new File(projectPath
                        + "output/" + LOG_MGRA_FILE + "D.csv"))));
                outH = new PrintWriter(new BufferedWriter(new FileWriter(new File(projectPath
                        + "output/" + LOG_MGRA_FILE + "H.csv"))));
            } catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        float workDistCoeff = Float.parseFloat(propertyMap.get(MGRA_DISTANCE_COEFF_WORK));
        float otherDistCoeff = Float.parseFloat(propertyMap.get(MGRA_DISTANCE_COEFF_OTHER));

        int[] mgraField = mgraTableDataSet.getColumnAsInt(MGRA_FIELD_NAME);
        int[] mgraParkAreaField = mgraTableDataSet.getColumnAsInt(MGRA_PARKAREA_FIELD);
        int[] hstallsothField = mgraTableDataSet.getColumnAsInt(MGRA_HSTALLSOTH_FIELD);
        int[] hstallssamField = mgraTableDataSet.getColumnAsInt(MGRA_HSTALLSSAM_FIELD);
        float[] hparkcostField = mgraTableDataSet.getColumnAsFloat(MGRA_HPARKCOST_FIELD);
        int[] numfreehrsField = mgraTableDataSet.getColumnAsInt(MGRA_NUMFREEHRS_FIELD);
        int[] dstallsothField = mgraTableDataSet.getColumnAsInt(MGRA_DSTALLSOTH_FIELD);
        int[] dstallssamField = mgraTableDataSet.getColumnAsInt(MGRA_DSTALLSSAM_FIELD);
        float[] dparkcostField = mgraTableDataSet.getColumnAsFloat(MGRA_DPARKCOST_FIELD);
        int[] mstallsothField = mgraTableDataSet.getColumnAsInt(MGRA_MSTALLSOTH_FIELD);
        int[] mstallssamField = mgraTableDataSet.getColumnAsInt(MGRA_MSTALLSSAM_FIELD);
        float[] mparkcostField = mgraTableDataSet.getColumnAsFloat(MGRA_MPARKCOST_FIELD);

        mgraParkArea = new int[maxMgra + 1];
        numfreehrs = new int[maxMgra + 1];
        hstallsoth = new int[maxMgra + 1];
        hstallssam = new int[maxMgra + 1];
        hparkcost = new float[maxMgra + 1];
        dstallsoth = new int[maxMgra + 1];
        dstallssam = new int[maxMgra + 1];
        dparkcost = new float[maxMgra + 1];
        mstallsoth = new int[maxMgra + 1];
        mstallssam = new int[maxMgra + 1];
        mparkcost = new float[maxMgra + 1];

        lsWgtAvgCostM = new double[maxMgra + 1];
        lsWgtAvgCostD = new double[maxMgra + 1];
        lsWgtAvgCostH = new double[maxMgra + 1];

        // loop over the number of mgra records in the TableDataSet.
        for (int k = 0; k < maxMgra; k++)
        {

            // get the mgra value for TableDataSet row k from the mgra field.
            int mgra = mgraField[k];

            mgraParkArea[mgra] = mgraParkAreaField[k];
            numfreehrs[mgra] = numfreehrsField[k];
            hstallsoth[mgra] = hstallsothField[k];
            hstallssam[mgra] = hstallssamField[k];
            hparkcost[mgra] = hparkcostField[k];
            dstallsoth[mgra] = dstallsothField[k];
            dstallssam[mgra] = dstallssamField[k];
            dparkcost[mgra] = dparkcostField[k];
            mstallsoth[mgra] = mstallsothField[k];
            mstallssam[mgra] = mstallssamField[k];
            mparkcost[mgra] = mparkcostField[k];

            // get the array of mgras within walking distance of m
            int[] walkMgras = getMgrasWithinWalkDistanceFrom(mgra);

            // park area 1.
            if (mgraParkArea[mgra] == PARK_AREA_ONE)
            {

                // calculate weighted average cost from monthly costs
                double dist = getMgraToMgraWalkDistFrom(mgra, mgra) / 5280.0;

                double numeratorM = mstallssam[mgra] * Math.exp(workDistCoeff * dist)
                        * mparkcost[mgra];
                double denominatorM = mstallssam[mgra] * Math.exp(workDistCoeff * dist);

                double numeratorD = dstallssam[mgra] * Math.exp(workDistCoeff * dist)
                        * dparkcost[mgra];
                double denominatorD = dstallssam[mgra] * Math.exp(workDistCoeff * dist);

                double discountFactor = Math.max(1 - (numfreehrs[mgra] / 4), 0);
                double numeratorH = hstallssam[mgra] * Math.exp(workDistCoeff * dist)
                        * discountFactor * hparkcost[mgra];
                double denominatorH = hstallssam[mgra] * Math.exp(workDistCoeff * dist);

                if (mgra == LOG_MGRA)
                {
                    // log the file header
                    outM.println("wMgra" + "," + "mgraParkArea" + "," + "workDistCoeff*dist" + ","
                            + "exp(workDistCoeff*dist)" + "," + "mstallsoth" + "," + "mparkcost"
                            + "," + "numeratorM" + "," + "denominatorM");
                    outD.println("wMgra" + "," + "mgraParkArea" + "," + "otherDistCoeff*dist" + ","
                            + "exp(otherDistCoeff*dist)" + "," + "dstallsoth" + "," + "dparkcost"
                            + "," + "numeratorD" + "," + "denominatorD");
                    outH.println("wMgra" + "," + "mgraParkArea" + "," + "otherDistCoeff*dist" + ","
                            + "exp(otherDistCoeff*dist)" + "," + "discountFactor" + ","
                            + "hstallsoth" + "," + "hparkcost" + "," + "numeratorH" + ","
                            + "denominatorH");

                    outM.println(mgra + "," + mgraParkArea[mgra] + "," + workDistCoeff * dist + ","
                            + Math.exp(workDistCoeff * dist) + "," + mstallsoth[mgra] + ","
                            + mparkcost[mgra] + "," + numeratorM + "," + denominatorM);
                    outD.println(mgra + "," + mgraParkArea[mgra] + "," + workDistCoeff * dist + ","
                            + Math.exp(workDistCoeff * dist) + "," + dstallsoth[mgra] + ","
                            + dparkcost[mgra] + "," + numeratorD + "," + denominatorD);
                    outH.println(mgra + "," + mgraParkArea[mgra] + "," + workDistCoeff * dist + ","
                            + Math.exp(workDistCoeff * dist) + "," + discountFactor + ","
                            + hstallsoth[mgra] + "," + hparkcost[mgra] + "," + numeratorH + ","
                            + denominatorH);
                }

                if (walkMgras != null)
                {

                    for (int wMgra : walkMgras)
                    {

                        // skip mgra if not in park area 1 or 2.
                        if (mgraParkArea[wMgra] > 2)
                        {
                            if (mgra == LOG_MGRA)
                            {
                                outM.println(wMgra + "," + mgraParkArea[wMgra]);
                                outD.println(wMgra + "," + mgraParkArea[wMgra]);
                                outH.println(wMgra + "," + mgraParkArea[wMgra]);
                            }
                            continue;
                        }

                        if (wMgra != mgra)
                        {
                            dist = getMgraToMgraWalkDistFrom(mgra, wMgra) / 5280.0;

                            if (dist > MAX_PARKING_WALK_DISTANCE)
                            {
                                if (mgra == LOG_MGRA)
                                {
                                    outM.println(wMgra + "," + mgraParkArea[wMgra]);
                                    outD.println(wMgra + "," + mgraParkArea[wMgra]);
                                    outH.println(wMgra + "," + mgraParkArea[wMgra]);
                                }
                                continue;
                            }

                            numeratorM += mstallsoth[wMgra] * Math.exp(workDistCoeff * dist)
                                    * mparkcost[wMgra];
                            denominatorM += mstallsoth[wMgra] * Math.exp(workDistCoeff * dist);

                            numeratorD += dstallsoth[wMgra] * Math.exp(otherDistCoeff * dist)
                                    * dparkcost[wMgra];
                            denominatorD += dstallsoth[wMgra] * Math.exp(otherDistCoeff * dist);

                            discountFactor = Math.max(1 - (numfreehrs[wMgra] / 4), 0);
                            numeratorH += hstallsoth[wMgra] * Math.exp(otherDistCoeff * dist)
                                    * discountFactor * hparkcost[wMgra];
                            denominatorH += hstallsoth[wMgra] * Math.exp(otherDistCoeff * dist);

                            if (mgra == LOG_MGRA)
                            {
                                outM.println(wMgra + "," + mgraParkArea[wMgra] + ","
                                        + workDistCoeff * dist + ","
                                        + Math.exp(workDistCoeff * dist) + "," + mstallsoth[wMgra]
                                        + "," + mparkcost[wMgra] + "," + numeratorM + ","
                                        + denominatorM);
                                outD.println(wMgra + "," + mgraParkArea[wMgra] + ","
                                        + otherDistCoeff * dist + ","
                                        + Math.exp(otherDistCoeff * dist) + "," + dstallsoth[wMgra]
                                        + "," + dparkcost[wMgra] + "," + numeratorD + ","
                                        + denominatorD);
                                outH.println(wMgra + "," + mgraParkArea[wMgra] + ","
                                        + otherDistCoeff * dist + ","
                                        + Math.exp(otherDistCoeff * dist) + "," + discountFactor
                                        + "," + hstallsoth[wMgra] + "," + hparkcost[wMgra] + ","
                                        + numeratorH + "," + denominatorH);
                            }

                        }

                    }

                }
                // jef: storing by mgra since they are indexed into by mgra
                lsWgtAvgCostM[mgra] = numeratorM / denominatorM;
                lsWgtAvgCostD[mgra] = numeratorD / denominatorD;
                lsWgtAvgCostH[mgra] = numeratorH / denominatorH;

            } else
            {

                lsWgtAvgCostM[mgra] = mparkcost[mgra];
                lsWgtAvgCostD[mgra] = dparkcost[mgra];
                lsWgtAvgCostH[mgra] = hparkcost[mgra];

            }

            // write the data record
            out.println(mgra + "," + mgraParkArea[mgra] + "," + lsWgtAvgCostM[mgra] + ","
                    + lsWgtAvgCostD[mgra] + "," + lsWgtAvgCostH[mgra]);
        }

        if (LOG_MGRA > 0)
        {
            outM.close();
            outD.close();
            outH.close();
        }

        out.close();

    }

    public double[] getLsWgtAvgCostM()
    {
        return lsWgtAvgCostM;
    }

    public double[] getLsWgtAvgCostD()
    {
        return lsWgtAvgCostD;
    }

    public double[] getLsWgtAvgCostH()
    {
        return lsWgtAvgCostH;
    }

    public int[] getMgraParkAreas()
    {
        return mgraParkArea;
    }

    public int[] getNumFreeHours()
    {
        return numfreehrs;
    }

    public int[] getMStallsOth()
    {
        return mstallsoth;
    }

    public int[] getMStallsSam()
    {
        return mstallssam;
    }

    public float[] getMParkCost()
    {
        return mparkcost;
    }

    public int[] getDStallsOth()
    {
        return dstallsoth;
    }

    public int[] getDStallsSam()
    {
        return dstallssam;
    }

    public float[] getDParkCost()
    {
        return dparkcost;
    }

    public int[] getHStallsOth()
    {
        return hstallsoth;
    }

    public int[] getHStallsSam()
    {
        return hstallssam;
    }

    public float[] getHParkCost()
    {
        return hparkcost;
    }

    /**
     * @param mgra
     *            for which table data is desired
     * @return high school district for the specified mgra.
     */
    public int getMgraHourlyParkingCost(int mgra)
    {
        int row = mgraDataTableMgraRowMap.get(mgra);
        return (int) mgraTableDataSet.getValueAt(row, MGRA_HPARKCOST_FIELD);
    }

    public static void main(String[] args)
    {
        ResourceBundle rb = ResourceUtil.getPropertyBundle(new File(args[0]));
        MgraDataManager mdm = MgraDataManager.getInstance(ResourceUtil
                .changeResourceBundleIntoHashMap(rb));
        mdm.printMgraStats();
    }

}
