package org.sandag.abm.modechoice;

import com.pb.common.util.ResourceUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.Util;

public final class TapDataManager
        implements Serializable
{

    protected transient Logger    logger = Logger.getLogger(TapDataManager.class);
    private static TapDataManager instance;

    // tapID, [tapNum, lotId, ??, taz][list of values]
    private float[][][]           tapParkingInfo;

    // an array that stores parking lot use by lot ID.
    private int[]                 lotUse;

    // an array of taps
    private int[]                 taps;
    private int                   maxTap;

    public int getMaxTap()
    {
        return maxTap;
    }

    private TapDataManager(HashMap<String, String> rbMap)
    {

        System.out.println("I'm the TapDataManager");
        readTap(rbMap);
        getTapList(rbMap);
        intializeLotUse();
        printStats();
    }

    /**
     * This method should only be used after the getInstance(HashMap<String, String> rbMap) method has been called since the rbMap is needed to read
     * in all the data and populate the object. This method will return the instance that has already been populated.
     * 
     * @return instance
     * @throws RuntimeException
     */
    public static TapDataManager getInstance()
    {
        if (instance == null)
        {
            throw new RuntimeException(
                    "Must instantiate TapDataManager with the getInstance(rbMap) method first");
        } else
        {
            return instance;
        }
    }

    /**
     * This method will read in the tapParkingInfo.ptype file and store the info in a TreeMap where key equals the iTap and value equals an array of
     * [][3] elements. The TreeMap will be passed to the populateTap function which will transpose the array of [][3] elements to an array of [4][]
     * elements and attaches it to the this.tapParkingInfo[key]
     * 
     * //TODO: Test this and see if there is only a single lot associated // TODO with each tap.
     * 
     * The file has 5 columns - tap, lotId, parking type, taz, and capacity
     * 
     * @param rb
     *            - the resource bundle that lists the tap.ptype file and scenario.path.
     */
    private void readTap(HashMap<String, String> rbMap)
    {

        File tazTdzCorresFile = new File(Util.getStringValueFromPropertyMap(rbMap, "scenario.path")
                + Util.getStringValueFromPropertyMap(rbMap, "tap.ptype.file"));
        String s;
        TreeMap<Integer, List<float[]>> map = new TreeMap<Integer, List<float[]>>();
        StringTokenizer st;
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(tazTdzCorresFile));
            while ((s = br.readLine()) != null)
            {
                st = new StringTokenizer(s, " ");
                float[] tapList = new float[3];
                int key = Integer.parseInt(st.nextToken()); // tap number
                tapList[0] = Float.parseFloat(st.nextToken()); // lot id
                st.nextToken(); // skip column not used
                tapList[1] = Float.parseFloat(st.nextToken()); // taz
                tapList[2] = (Math.max(Float.parseFloat(st.nextToken()), 15)) * 2.5f; // lot
                // capacity
                if (map.get(key) == null)
                {
                    List<float[]> newList = new ArrayList<float[]>();
                    newList.add(tapList);
                    map.put(key, newList);
                } else
                {
                    map.get(key).add(tapList);
                }
            }
            br.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        populateTap(map);
    }

    /**
     * The function will get a TreeMap having with iTaps as keys and [][4] arrays. For each iTap in the TreeMap it will transpose the [][4] array
     * associated with it and attach it to the this.tapParkingInfo[key] element.
     * 
     * @param map
     *            - a TreeMap containing all the records of the tapParkingInfo.ptype file
     */
    private void populateTap(TreeMap<Integer, List<float[]>> map)
    {

        this.tapParkingInfo = new float[map.lastKey() + 1][3][];
        Iterator<Integer> iterKeys = map.keySet().iterator();
        while (iterKeys.hasNext())
        {
            int key = iterKeys.next();
            int numElem = map.get(key).size();
            for (int i = 0; i < 3; i++)
                this.tapParkingInfo[key][i] = new float[numElem];
            for (int i = 0; i < numElem; i++)
            {
                for (int j = 0; j < 3; j++)
                {
                    this.tapParkingInfo[key][j][i] = map.get(key).get(i)[j];
                }
            }
        }
    }

    // TODO: test this.
    public void intializeLotUse()
    {

        float maxLotId = 0;
        for (int i = 0; i < tapParkingInfo.length; i++)
        {
            float[] lotIds = tapParkingInfo[i][0];
            if (lotIds != null)
            {
                for (int j = 0; j < tapParkingInfo[i][0].length; j++)
                {
                    if (maxLotId < tapParkingInfo[i][0][j]) maxLotId = tapParkingInfo[i][0][j];

                }
            }
        }

        lotUse = new int[(int) maxLotId + 1];
    }

    /**
     * Set the array of tap numbers (taps[]), indexed at 1.
     * 
     * @param rb
     *            A Resourcebundle with skims.path and tap.skim.file properties.
     */
    public void getTapList(HashMap<String, String> rbMap)
    {

        // read taps from walk file
        File mgraWlkTapCorresFile = new File(Util.getStringValueFromPropertyMap(rbMap,
                "scenario.path")
                + Util.getStringValueFromPropertyMap(rbMap, "mgra.wlkacc.taps.and.distance.file"));
        ArrayList<Integer> tapList = new ArrayList<Integer>();
        String s;

        StringTokenizer st;
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(mgraWlkTapCorresFile));

            // read the first data file line containing column names
            s = br.readLine();

            // read the data records
            while ((s = br.readLine()) != null)
            {
                st = new StringTokenizer(s, ", ");
                st.nextToken();
                int tap = Integer.parseInt(st.nextToken());
                if (tap > maxTap) maxTap = tap;
                if (!tapList.contains(tap)) tapList.add(tap);
            }
            br.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        // read taps from park-and-ride file
        File tazTdzCorresFile = new File(Util.getStringValueFromPropertyMap(rbMap, "scenario.path")
                + Util.getStringValueFromPropertyMap(rbMap, "tap.ptype.file"));

        try
        {
            BufferedReader br = new BufferedReader(new FileReader(tazTdzCorresFile));
            while ((s = br.readLine()) != null)
            {
                st = new StringTokenizer(s, " ");
                int tap = Integer.parseInt(st.nextToken()); // tap number
                if (!tapList.contains(tap)) tapList.add(tap);
            }
            br.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        Collections.sort(tapList);
        // now go thru the array of ArrayLists and convert the lists to arrays and
        taps = new int[tapList.size() + 1];

        for (int i = 0; i < tapList.size(); ++i)
            taps[i + 1] = tapList.get(i);

    }

    public int getLotUse(int lotId)
    {
        return lotUse[lotId];
    }

    public void printStats()
    {
        /*
         * logger.info("Tap 561 is in zone: " + tapParkingInfo[561][1][0]); logger.info("Tap 298 lot capacity: " + tapParkingInfo[298][2][0]);
         */
    }

    public int getTazForTap(int tap)
    {
        return (int) tapParkingInfo[tap][1][0];
    }

    public static TapDataManager getInstance(HashMap<String, String> rbMap)
    {
        if (instance == null)
        {
            instance = new TapDataManager(rbMap);
            return instance;
        } else return instance;
    }

    public float[][][] getTapParkingInfo()
    {
        if (instance != null)
        {
            return this.tapParkingInfo;
        } else
        {
            throw new RuntimeException();
        }
    }

    public float getCarToStationWalkTime(int tap)
    {
        return 0.0f;
    }

    public float getEscalatorTime(int tap)
    {
        return 0.0f;
    }

    public int[] getTaps()
    {
        return taps;
    }

    public static void main(String[] args)
    {
        ResourceBundle rb = ResourceUtil.getPropertyBundle(new File(args[0]));

        TapDataManager tdm = TapDataManager.getInstance(ResourceUtil
                .changeResourceBundleIntoHashMap(rb));
        tdm.printStats();

    }

}
