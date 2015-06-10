package org.sandag.abm.modechoice;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
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
 * This class is used for storing the TAZ data for the mode choice model.
 * 
 * @author Christi Willison
 * @version Sep 2, 2008
 *          <p/>
 *          Created by IntelliJ IDEA.
 */
public final class TazDataManager
        implements Serializable
{

    protected transient Logger    logger = Logger.getLogger(TazDataManager.class);
    private static TazDataManager instance;
    public int[]                  tazs;
    protected int[]               tazsOneBased;

    // arrays for the MGRA and TAZ fields from the MGRA table data file.
    private int[]                 mgraTableMgras;
    private int[]                 mgraTableTazs;

    // list of TAZs in numerical order
    public TreeSet<Integer>       tazSet = new TreeSet<Integer>();
    public int                    maxTaz;

    private int                   nTazsWithMgras;
    private int[][]               tazMgraArray;
    // private int[][] tazXYCoordArray;
    private float[]               tazDestinationTerminalTime;
    private float[]               tazOriginTerminalTime;
    // private int[] tazSuperDistrict;
    // private int[] pmsa;
    // private int[] cmsa;
    // This might be a poor name for this array.
    // Please change if you know better.
    private int[]                 tazAreaType;
    // they are being read from same file but might be different -
    // [tdz][id,time,dist][tap position]
    // in the future.
    private float[][][]           tazParkNRideTaps;
    // They are floats because they store time and distance
    private float[][][]           tazKissNRideTaps;

    // added from tdz manager
    private int[]                 tazParkingType;

    /**
     * Get an array of tazs, indexed sequentially from 0
     * 
     * @return Taz array indexed from 0
     */
    public int[] getTazs()
    {
        return tazs;
    }

    /**
     * Get an array of tazs, indexed sequentially from 1
     * 
     * @return taz array indexed from 1
     */
    public int[] getTazsOneBased()
    {
        return tazsOneBased;
    }

    private TazDataManager(HashMap<String, String> rbMap)
    {
        System.out.println("I'm the TazDataManager");

        // read the MGRA data file into a TableDataSet and get the MGRA and TAZ
        // fields from it for setting TAZ correspondence.
        readMgraTableData(rbMap);
        setTazMgraCorrespondence();

        // readTazDistrictCorrespondence(rbMap);
        readTazTerminalTimeCorrespondence(rbMap);
        // readTazProductionTerminalTimeCorrespondence(rbMap);
        // readZonePMSA(rbMap);
        // readZoneCMSA(rbMap);
        // readZoneAvrZoneCorrespondence(rbMap);
        // readTAZParkingTypeCorrespondence(rbMap);
        readPnRTapsInfo(rbMap);

        printTazStats();
    }

    /**
     * This method reads in the taz.tdz file which has 2 columns. The first
     * column is the taz and the second column is corresponding tdz. The
     * correspondence will be stored in the tdz class. The only data captured
     * here is the list of TAZs.
     * 
     * This method will also set the maxTaz value.
     * 
     * @param rb
     *            the properties file that lists the taz.tdz file and the
     *            generic.path.
     */
    private void readTazs(HashMap<String, String> rbMap)
    {
        File tazTdzCorresFile = new File(Util.getStringValueFromPropertyMap(rbMap, "generic.path")
                + Util.getStringValueFromPropertyMap(rbMap, "taz.tdz.correspondence.file"));
        String s;
        int taz;
        StringTokenizer st;
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(tazTdzCorresFile));
            while ((s = br.readLine()) != null)
            {
                st = new StringTokenizer(s, " ");
                taz = Integer.parseInt(st.nextToken());
                tazSet.add(taz);
                st.nextToken();
            }
            br.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        maxTaz = tazSet.last();
        tazs = new int[tazSet.size()];
        tazsOneBased = new int[tazSet.size() + 1];
        int i = 0;
        for (Integer tazNumber : tazSet)
        {
            tazs[i] = tazNumber;
            tazsOneBased[i + 1] = tazNumber;
            ++i;
        }

    }

    /**
     * This method will set the TAZ/MGRA correspondence. Two columns from the
     * MGRA data table are used. The first column is the MGRA and the second
     * column is the TAZ. The goal of this method is to populate the
     * tazMgraArray array and the tazs treeset, plus set maxTaz.
     * 
     */
    private void setTazMgraCorrespondence()
    {

        HashMap<Integer, ArrayList<Integer>> tazMgraMap = new HashMap<Integer, ArrayList<Integer>>();

        int mgra;
        int taz;

        for (int i = 0; i < mgraTableMgras.length; i++)
        {

            mgra = mgraTableMgras[i];
            taz = mgraTableTazs[i];
            if (!tazSet.contains(taz)) tazSet.add(taz);

            maxTaz = Math.max(taz, maxTaz);

            if (!tazMgraMap.containsKey(taz))
            {
                ArrayList<Integer> tazMgraList = new ArrayList<Integer>();
                tazMgraList.add(mgra);
                tazMgraMap.put(taz, tazMgraList);
            } else
            {
                ArrayList<Integer> tazMgraList = tazMgraMap.get(taz);
                tazMgraList.add(mgra);
            }

        }

        // now go thru the array of ArrayLists and convert the lists to arrays
        // and
        // store in the class variable tazMgraArrays.
        tazMgraArray = new int[maxTaz + 1][];
        for (Iterator it = tazMgraMap.entrySet().iterator(); it.hasNext();)
        { // elements
          // in
          // the
          // array
          // of
          // arraylists
            Map.Entry entry = (Map.Entry) it.next();
            taz = (Integer) entry.getKey();
            ArrayList tazMgraList = (ArrayList) entry.getValue();
            if (tazMgraList != null)
            { // if the list isn't null
                tazMgraArray[taz] = new int[tazMgraList.size()]; // initialize
                                                                 // the class
                                                                 // variable
                for (int j = 0; j < tazMgraList.size(); j++)
                    tazMgraArray[taz][j] = (Integer) tazMgraList.get(j);
                nTazsWithMgras++;
            }
        }
        tazs = new int[tazSet.size()];

        tazsOneBased = new int[tazSet.size() + 1];
        int i = 0;
        for (Integer tazNumber : tazSet)
        {
            tazs[i] = tazNumber;
            tazsOneBased[i + 1] = tazNumber;
            ++i;
        }
    }

    /**
     * This method will initialize the class variable tazSuperDistrict. The
     * taz.district file has 2 columns, the first is the taz and the second is
     * the superdistrict
     * 
     * @param rb
     *            the resource bundle that specifies the taz.district file and
     *            the generic.path public void
     *            readTazDistrictCorrespondence(HashMap<String, String> rbMap) {
     *            tazSuperDistrict = new int[maxTaz + 1]; File tazTdzCorresFile
     *            = new File(Util.getStringValueFromPropertyMap(rbMap,
     *            "generic.path") + Util.getStringValueFromPropertyMap(rbMap,
     *            "taz.district.correspondence.file")); String s; int taz; int
     *            sd; StringTokenizer st; try { BufferedReader br = new
     *            BufferedReader(new FileReader(tazTdzCorresFile)); while ((s =
     *            br.readLine()) != null) { st = new StringTokenizer(s, " ");
     *            taz = Integer.parseInt(st.nextToken()); sd =
     *            Integer.parseInt(st.nextToken()); tazSuperDistrict[taz] = sd;
     *            } br.close(); } catch (IOException e) { e.printStackTrace(); }
     *            }
     */

    /**
     * This method will read the zone.avrzone file and store the location area
     * (0-3) for each taz.
     * 
     * 
     * @param rb
     *            - resourceBundle That specifies the zone.avrzone file and the
     *            generic.path private void
     *            readZoneAvrZoneCorrespondence(HashMap<String, String> rbMap) {
     *            File zoneAvrZoneCorresFile = new
     *            File(Util.getStringValueFromPropertyMap(rbMap, "generic.path")
     *            + Util.getStringValueFromPropertyMap(rbMap,
     *            "taz.avrzone.correspondence.file")); tazAreaType = new
     *            int[maxTaz + 1];
     * 
     *            // read the file to get the location area (0 - 3) for each TDZ
     *            String s; int taz; StringTokenizer st; int location; try {
     *            BufferedReader br = new BufferedReader(new
     *            FileReader(zoneAvrZoneCorresFile)); while ((s = br.readLine())
     *            != null) { st = new StringTokenizer(s, " "); taz =
     *            Integer.parseInt(st.nextToken()); location =
     *            Integer.parseInt(st.nextToken()); tazAreaType[taz] = location;
     *            } br.close(); } catch (IOException e) { e.printStackTrace(); }
     * 
     *            }
     */

    /**
     * This method reads in the zone.pmsa file which has 2 columns. The first
     * column is the taz and the second column is corresponding pmsa. The
     * correspondence will be stored in the pmsa list. The only data captured
     * here is the list of pmsas.
     * 
     * This method will also set the maxTaz value.
     * 
     * @param rb
     *            the properties file that lists the taz.tdz file and the
     *            generic.path private void readZonePMSA(HashMap<String, String>
     *            rbMap) {
     * 
     *            pmsa = new int[maxTaz + 1]; File zonePmsaFileName = new
     *            File(Util.getStringValueFromPropertyMap(rbMap, "generic.path")
     *            + Util.getStringValueFromPropertyMap(rbMap, "taz.pmsa.file"));
     *            String s; int taz; int tazPmsa; StringTokenizer st; try {
     *            BufferedReader br = new BufferedReader(new
     *            FileReader(zonePmsaFileName)); // BufferedReader br = new
     *            BufferedReader(new //
     *            FileReader("/Users/michalis/Documents/Fortran2Java/data/zone.pmsa"
     *            )); while ((s = br.readLine()) != null) { st = new
     *            StringTokenizer(s, " "); taz =
     *            Integer.parseInt(st.nextToken()); tazPmsa =
     *            Integer.parseInt(st.nextToken()); pmsa[taz] = tazPmsa; }
     *            br.close(); } catch (IOException e) { e.printStackTrace(); }
     * 
     *            }
     */

    /**
     * This method reads in the zone.cmsa file which has 2 columns. The first
     * column is the taz and the second column is corresponding cmsa. The
     * correspondence will be stored in the cmsa list. The only data captured
     * here is the list of cmsas.
     * 
     * This method will also set the maxTaz value.
     * 
     * @param rb
     *            the properties file that lists the zone.cmsa file and the
     *            generic.path
     * 
     *            private void readZoneCMSA(HashMap<String, String> rbMap) {
     * 
     *            cmsa = new int[maxTaz + 1]; File zoneCmsaFile = new
     *            File(Util.getStringValueFromPropertyMap(rbMap, "generic.path")
     *            + Util.getStringValueFromPropertyMap(rbMap, "taz.cmsa.file"));
     *            String s; int taz; int tazCmsa; StringTokenizer st; try {
     *            BufferedReader br = new BufferedReader(new
     *            FileReader(zoneCmsaFile)); // BufferedReader br = new
     *            BufferedReader(new //
     *            FileReader("/Users/michalis/Documents/Fortran2Java/data/zone.cmsa"
     *            )); while ((s = br.readLine()) != null) { st = new
     *            StringTokenizer(s, " "); taz =
     *            Integer.parseInt(st.nextToken()); tazCmsa =
     *            Integer.parseInt(st.nextToken()); cmsa[taz] = tazCmsa; }
     *            br.close(); } catch (IOException e) { e.printStackTrace(); }
     * 
     *            }
     * */

    /**
     * This method will read the zone.term file and store the terminal time for
     * each taz.
     * 
     * @param rb
     *            the properties file that lists the zone.term file and the
     *            scenario.path
     */
    private void readTazTerminalTimeCorrespondence(HashMap<String, String> rbMap)
    {
        File tdzTerminalTimeCorresFile = new File(Util.getStringValueFromPropertyMap(rbMap,
                "scenario.path")
                + Util.getStringValueFromPropertyMap(rbMap, "taz.terminal.time.file"));

        tazDestinationTerminalTime = new float[maxTaz + 1];
        tazOriginTerminalTime = new float[maxTaz + 1];

        // read the file to get the terminal time for each TDZ
        String s;
        int taz;
        StringTokenizer st;
        float terminalTime;
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(tdzTerminalTimeCorresFile));
            while ((s = br.readLine()) != null)
            {
                st = new StringTokenizer(s, " ");
                taz = Integer.parseInt(st.nextToken());
                terminalTime = Float.parseFloat(st.nextToken());
                tazDestinationTerminalTime[taz] = terminalTime;
                tazOriginTerminalTime[taz] = terminalTime;
            }
            br.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    /**
     * This method will read the zone.pterm file and store the production
     * terminal time for each tdz.
     * 
     * @param rb
     *            the properties file that lists the zone.pterm file and the
     *            scenario.path
     * 
     * 
     *            private void
     *            readTazProductionTerminalTimeCorrespondence(HashMap<String,
     *            String> rbMap) { File tdzProductionTerminalTimeCorresFile =
     *            new File(Util.getStringValueFromPropertyMap( rbMap,
     *            "scenario.path") + Util.getStringValueFromPropertyMap(rbMap,
     *            "taz.prod.terminal.time.file"));
     * 
     *            tazOriginTerminalTime = new float[maxTaz + 1];
     * 
     *            // read the file to get the production terminal time for each
     *            TDZ String s; int taz; StringTokenizer st; float
     *            productionTerminalTime; try { BufferedReader br = new
     *            BufferedReader(new FileReader(
     *            tdzProductionTerminalTimeCorresFile)); while ((s =
     *            br.readLine()) != null) { st = new StringTokenizer(s, " ");
     *            taz = Integer.parseInt(st.nextToken()); productionTerminalTime
     *            = Float.parseFloat(st.nextToken()); tazOriginTerminalTime[taz]
     *            = productionTerminalTime; } br.close(); } catch (IOException
     *            e) { e.printStackTrace(); }
     * 
     *            }
     */

    /**
     * This method will read the zone.park file and store the parking type for
     * each taz. Only types 2 - 5 are given. Rest assumed to be type 1.
     * 
     * @param rb
     *            the properties file that lists the taz.parkingtype.file and
     *            the scenario.path private void
     *            readTAZParkingTypeCorrespondence(HashMap<String, String>
     *            rbMap) { File tazParkingTypeCorresFile = new
     *            File(Util.getStringValueFromPropertyMap(rbMap,
     *            "scenario.path") + Util.getStringValueFromPropertyMap(rbMap,
     *            "taz.parkingtype.file"));
     * 
     *            tazParkingType = new int[maxTaz + 1];
     *            Arrays.fill(tazParkingType, 1);
     * 
     *            // read the file to get the parking type (2 - 5) for each TAZ
     *            String s; int taz; StringTokenizer st; int parkingType; try {
     *            BufferedReader br = new BufferedReader(new
     *            FileReader(tazParkingTypeCorresFile)); while ((s =
     *            br.readLine()) != null) { st = new StringTokenizer(s, " ");
     *            taz = Integer.parseInt(st.nextToken()); parkingType =
     *            Integer.parseInt(st.nextToken()); tazParkingType[taz] =
     *            parkingType; } br.close(); } catch (IOException e) {
     *            e.printStackTrace(); }
     * 
     *            }
     */

    /**
     * This method read in the access061.prp file that lists the taz and the #
     * of taps that have drive access. Then the taps are listed along with the
     * time and the distance to those taps from the taz.
     * 
     * @param rb
     *            the properties file that lists the taz.driveaccess.taps.file
     *            and the scenario.path
     */
    public void readPnRTapsInfo(HashMap<String, String> rbMap)
    {
        File tdzDATapFile = new File(Util.getStringValueFromPropertyMap(rbMap, "scenario.path")
                + Util.getStringValueFromPropertyMap(rbMap, "taz.driveaccess.taps.file"));
        tazParkNRideTaps = new float[maxTaz + 1][3][]; // tapId, time, distance
        tazKissNRideTaps = new float[maxTaz + 1][3][]; // tapId, time, distance

        String s, s1;
        StringTokenizer st, st1;
        int taz;
        int tapId;
        float tapTime;
        float tapDist;
        
        //Shove into hash at first, then decompose into float array
        HashMap< Integer, HashMap<Integer, float[] >> tazTapMap = new HashMap<Integer, HashMap<Integer, float[] >>();
        
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(tdzDATapFile));
            while ((s = br.readLine()) != null)
            {
                st = new StringTokenizer(s, ",");
                
                taz = Integer.parseInt(st.nextToken());
                tapId = Integer.parseInt(st.nextToken());
                tapTime = Float.parseFloat(st.nextToken());
                tapDist = Float.parseFloat(st.nextToken());
                
                if(tazTapMap.get(taz) != null){
                	
                	HashMap< Integer, float[] > tapVals = tazTapMap.get(taz);
                	if(tapVals.get(tapId) != null){
                		//something wrong, since there should only be unique taps for a taz
                        throw new RuntimeException("There should not be any duplicate TAPs for a TAZ");
                	}else{
                    	float[] timeDist = new float[2];
                    	timeDist[0] = tapTime;
                    	timeDist[1] = tapDist;
                    	tapVals.put(tapId, timeDist);
                	}
                	
                }else{
                	HashMap< Integer, float[] > tapVals = new HashMap<Integer, float[] >();
                	float[] timeDist = new float[2];
                	timeDist[0] = tapTime;
                	timeDist[1] = tapDist;
                	tapVals.put(tapId, timeDist);
                	tazTapMap.put(taz, tapVals);
                }
            }
            
            Iterator it = tazTapMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                taz = (int) pair.getKey();
                HashMap< Integer, float[] > tapVals = tazTapMap.get(taz);
                int nTaps = tapVals.keySet().size();
                
                tazParkNRideTaps[taz][0] = new float[nTaps];
                tazParkNRideTaps[taz][1] = new float[nTaps];
                tazParkNRideTaps[taz][2] = new float[nTaps];
                tazKissNRideTaps[taz][0] = new float[nTaps];
                tazKissNRideTaps[taz][1] = new float[nTaps];
                tazKissNRideTaps[taz][2] = new float[nTaps];
                
                Iterator it2 = tapVals.entrySet().iterator();
                int i = 0;
                while (it2.hasNext()) {
                	Map.Entry pair2 = (Map.Entry)it2.next();
                	tapId = (int) pair2.getKey();
                	float[] vals = (float[]) pair2.getValue();
                    tazParkNRideTaps[taz][0][i] = tapId;
                    tazParkNRideTaps[taz][1][i] = vals[0];
                    tazParkNRideTaps[taz][2][i] = vals[1];
                    tazKissNRideTaps[taz][0][i] = tapId;
                    tazKissNRideTaps[taz][1][i] = vals[0];
                    tazKissNRideTaps[taz][2][i] = vals[1];
                    i++;
                }
            }

            br.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    /**
     * This method will return the Area Type (Location?) for the TAZ.
     * 
     * 
     * @param taz
     *            - TAZ that AreaType is wanted for.
     * @return area type that the taz corresponds to
     */
    public int getTAZAreaType(int taz)
    {
        return tazAreaType[taz];
    }

    /**
     * Write taz data manager data to logger for debugging.
     * 
     */
    public void printTazStats()
    {

        logger.info("Number of TAZs: " + tazSet.size());
        logger.info("Max TAZ: " + maxTaz);

        logger.info("Number of TAZs with MGRAs: " + nTazsWithMgras);
        // logger.info("Number of MGRAs in TAZ 13 (should be 42): " +
        // tazMgraArray[13].length);

        // logger.info("SuperDistrict for TAZ 2282 (should be 5): " +
        // tazSuperDistrict[2282]);
        // logger.info("Location for TAZ 4605 (should be 2): " +
        // tazAreaType[42]);
        // logger.info("Location for TAZ 1 (should be 3): " + tazAreaType[1]);
        // logger.info("Parking Type for TAZ 2234 (should be 2): " +
        // tazParkingType[2234]);

        // logger.info("PMSA for taz 4179 (should be 2):  " + pmsa[4179]);
        // logger.info("CMSA for taz 4604 (should be 4):  " + cmsa[4604]);
        // logger.info("Destination Terminal time for TAZ 3773(should be 0): "
        // + tazDestinationTerminalTime[3773]);
        // logger.info("Destination Terminal time for TAZ 3876 (should be 4): "
        // + tazDestinationTerminalTime[3876]);
        // logger.info("Origin terminal time for TAZ 2067 (should be 2.0): "
        // + tazOriginTerminalTime[2067]);
        // logger.info("Origin terminal time for TAZ 3262 (should be 0.4): "
        // + tazOriginTerminalTime[3262]);

        /*
         * System.out.println(
         * "Drive Access taps for TAZ 70 (should be 259, 337 and 338): " ); for
         * (int i = 0; i<tazParkNRideTaps[70][0].length; i++) {
         * System.out.println("\t" + tazParkNRideTaps[70][0][i]); }
         */
    }

    /**
     * Get a static instance of the Taz Data Manager. One is created if it
     * doesn't exist already.
     * 
     * @param rb
     *            A resourcebundle with properties for the TazDataManager.
     * @return A static instance of this class.
     */
    public static TazDataManager getInstance(HashMap<String, String> rbMap)
    {
        if (instance == null)
        {
            instance = new TazDataManager(rbMap);
            return instance;
        } else return instance;
    }

    /**
     * This method should only be used after the getInstance(HashMap<String,
     * String> rbMap) method has been called since the rbMap is needed to read
     * in all the data and populate the object. This method will return the
     * instance that has already been populated.
     * 
     * @return instance
     * @throws RuntimeException
     */
    public static TazDataManager getInstance() throws RuntimeException
    {
        if (instance == null)
        {
            throw new RuntimeException(
                    "Must instantiate TazDataManager with the getInstance(rbMap) method first");
        } else
        {
            return instance;
        }
    }

    /**
     * Get the number of TAZs with MGRAs.
     * 
     * @return The number of TAZs with MGRAs.
     */
    public int getNTazsWithMgras()
    {
        if (instance != null)
        {
            return nTazsWithMgras;
        } else
        {
            throw new RuntimeException();
        }
    }

    public int[][] getTazMgraArray()
    {
        if (instance != null)
        {
            return tazMgraArray;
        } else
        {
            throw new RuntimeException();
        }
    }

    /**
     * Return the list of MGRAs within this TAZ.
     * 
     * @param taz
     *            The TAZ number
     * @return An array of MGRAs within the TAZ.
     */
    public int[] getMgraArray(int taz)
    {
        if (instance != null)
        {
            return tazMgraArray[taz];
        } else
        {
            throw new RuntimeException();
        }
    }

    /*
     * public int[][] getTazXYCoordArray() { if (instance != null) { return
     * tazXYCoordArray; } else { throw new RuntimeException(); } }
     * 
     * public int[] getTazSuperDistrict() { if (instance != null) { return
     * tazSuperDistrict; } else { throw new RuntimeException(); }
     * 
     * }
     * 
     * public int[] getPmsa() { if (instance != null) { return this.pmsa; } else
     * { throw new RuntimeException(); } }
     * 
     * public int[] getCmsa() { if (instance != null) { return this.cmsa; } else
     * { throw new RuntimeException(); } }
     */

    /**
     * This method will return the Parking Type for the TAZ.
     * 
     * @param taz
     *            - TAZ that Parking Type is wanted for.
     * @return Parking Type
     */
    public int getTazParkingType(int taz)
    {
        return tazParkingType[taz];
    }

    /**
     * Get the list of Park and Ride Taps for this TAZ.
     * 
     * @param Taz
     * @return An array of PNR taps for the TAZ.
     */
    public int[] getParkRideTapsForZone(int taz)
    {
        if (tazParkNRideTaps[taz][0] == null) return null;

        int[] parkTaps = new int[tazParkNRideTaps[taz][0].length];
        for (int i = 0; i < tazParkNRideTaps[taz][0].length; i++)
        {
            parkTaps[i] = (int) tazParkNRideTaps[taz][0][i];
        }
        return parkTaps;
    }

    /**
     * Get the list of Kiss and Ride Taps for this TAZ.
     * 
     * @param Taz
     * @return An array of KNR taps for the TAZ.
     */
    public int[] getKissRideTapsForZone(int taz)
    {
        if (tazKissNRideTaps[taz][0] == null) return null;
        int[] kissTaps = new int[tazKissNRideTaps[taz][0].length];
        for (int i = 0; i < tazKissNRideTaps[taz][0].length; i++)
        {
            kissTaps[i] = (int) tazKissNRideTaps[taz][0][i];
        }
        return kissTaps;
    }

    public int[] getParkRideOrKissRideTapsForZone(int taz, AccessMode aMode)
    {

        switch (aMode)
        {
            case WALK:
                return null;
            case PARK_N_RIDE:
                return getParkRideTapsForZone(taz);
            case KISS_N_RIDE:
                return getKissRideTapsForZone(taz);
            default:
                throw new RuntimeException(
                        "Error trying to get ParkRideOrKissRideTaps for unknown access mode: "
                                + aMode);
        }
    }

    /**
     * Get the position of the tap in the taz tap array.
     * 
     * @param taz
     *            The taz to lookup
     * @param tap
     *            The tap to lookup
     * @param aMode
     *            The access mode
     * @return The position of the tap in the taz array. -1 is returned if it is
     *         an invalid tap for the taz.
     */
    public int getTapPosition(int taz, int tap, AccessMode aMode)
    {

        int[] taps = getParkRideOrKissRideTapsForZone(taz, aMode);

        if (taps == null) return -1;

        for (int i = 0; i < taps.length; ++i)
            if (taps[i] == tap) return i;

        return -1;

    }

    /**
     * Get the taz to tap time in minutes.
     * 
     * @param taz
     *            Origin/Production TAZ
     * @param pos
     *            Position of the TAP in this TAZ
     * @param mode
     *            Park and Ride or Kiss and Ride
     * @return The TAZ to TAP time in minutes.
     */
    public float getTapTime(int taz, int pos, AccessMode aMode)
    {
        // only expecting this method for Park and Ride and Kiss and Ride modes.
        switch (aMode)
        {
            case PARK_N_RIDE:
                return (tazParkNRideTaps[taz][1][pos]);
            case KISS_N_RIDE:
                return (tazKissNRideTaps[taz][1][pos]);
            default:
                throw new RuntimeException(
                        "Error trying to get ParkRideOrKissRideTaps for invalid access mode: "
                                + aMode);
        }
    }

    /**
     * Get the taz to tap distance in miles.
     * 
     * @param taz
     *            Origin/Production TAZ
     * @param pos
     *            Position of the TAP in this TAZ
     * @param mode
     *            Park and Ride or Kiss and Ride
     * @return The TAZ to TAP distance in miles.
     */
    public float getTapDist(int taz, int pos, AccessMode aMode)
    {
        // only expecting this method for Park and Ride and Kiss and Ride modes.
        switch (aMode)
        {
            case PARK_N_RIDE:
                return (tazParkNRideTaps[taz][2][pos]);
            case KISS_N_RIDE:
                return (tazKissNRideTaps[taz][2][pos]);
            default:
                throw new RuntimeException(
                        "Error trying to get ParkRideOrKissRideTaps for invalid access mode: "
                                + aMode);
        }
    }

    /**
     * Returns the max TAZ value
     * 
     * @return the max TAZ value
     */
    public int getMaxTaz()
    {
        return maxTaz;
    }

    private void readMgraTableData(HashMap<String, String> rbMap)
    {

        // get the mgra data table from one of these UECs.
        String projectPath = rbMap.get(CtrampApplication.PROPERTIES_PROJECT_DIRECTORY);
        String mgraFile = rbMap.get(MgraDataManager.PROPERTIES_MGRA_DATA_FILE);
        mgraFile = projectPath + mgraFile;

        TableDataSet mgraTableDataSet = null;
        try
        {
            OLD_CSVFileReader reader = new OLD_CSVFileReader();
            mgraTableDataSet = reader.readFile(new File(mgraFile));
        } catch (IOException e)
        {
            logger.error("problem reading mgra data table for TazDataManager.", e);
            System.exit(1);
        }

        // get 0-based arrays from the specified fields in the MGRA table
        mgraTableMgras = mgraTableDataSet.getColumnAsInt(MgraDataManager.MGRA_FIELD_NAME);
        mgraTableTazs = mgraTableDataSet.getColumnAsInt(MgraDataManager.MGRA_TAZ_FIELD_NAME);

    }

    /**
     * Test an instance of the class by instantiating and reporting.
     * 
     * @param args
     *            [0] The properties file name/path.
     */
    public static void main(String[] args)
    {
        ResourceBundle rb = ResourceUtil.getPropertyBundle(new File(args[0]));

        TazDataManager tdm = TazDataManager.getInstance(ResourceUtil
                .changeResourceBundleIntoHashMap(rb));

    }

    /**
     * This method will return the Origin Terminal Time for the TDZ.
     * 
     * @param taz
     *            - TAZ that Terminal Time is wanted for.
     * @return Origin Terminal Time
     */
    public float getOriginTazTerminalTime(int taz)
    {
        return tazOriginTerminalTime[taz];
    }

    /**
     * This method will return the Destination Terminal Time for the TDZ.
     * 
     * @param taz
     *            - TAZ that Terminal Time is wanted for.
     * @return Destination Terminal Time
     */
    public float getDestinationTazTerminalTime(int taz)
    {
        return tazDestinationTerminalTime[taz];
    }

}
