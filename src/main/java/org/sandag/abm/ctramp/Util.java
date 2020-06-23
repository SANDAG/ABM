package org.sandag.abm.ctramp;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;

public final class Util
        implements Serializable
{
    private transient static Logger logger  = Logger.getLogger(Util.class);

    private Util()
    {
        // Not implemented in utility classes
    }

    public static boolean getBooleanValueFromPropertyMap(HashMap<String, String> rbMap, String key)
    {
        boolean returnValue;
        String value = rbMap.get(key);
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false"))
        {
            returnValue = Boolean.parseBoolean(value);
        } else
        {
            logger.info("property file key: " + key + " = " + value
                    + " should be either 'true' or 'false'.");
            throw new RuntimeException();
        }

        return returnValue;
    }

    public static String getStringValueFromPropertyMap(HashMap<String, String> rbMap, String key)
    {
        String returnValue = rbMap.get(key);
        if (returnValue == null) returnValue = "";

        return returnValue;
    }
    
    public static String[] getStringArrayFromPropertyMap(HashMap<String, String> rbMap, String key) {
        String[] values = getStringValueFromPropertyMap(rbMap,key).split(",");
        return values;
    }


    public static int getIntegerValueFromPropertyMap(HashMap<String, String> rbMap, String key)
    {
        String value = rbMap.get(key);
        if (value != null)
        {
            return Integer.parseInt(value);
        } else
        {
            logger.info("property file key: " + key
                    + " missing.  No integer value can be determined.");
            throw new RuntimeException();
        }
    }

    public static float getFloatValueFromPropertyMap(HashMap<String, String> rbMap, String key)
    {
        String value = rbMap.get(key);
        if (value != null)
        {
            return Float.parseFloat(value);
        } else
        {
            logger.info("property file key: " + key
                    + " missing.  No float value can be determined.");
            throw new RuntimeException();
        }
    }
    
    public static int[] getIntegerArrayFromPropertyMap(HashMap<String, String> rbMap, String key)
    {

        int[] returnArray;
        String valueList = rbMap.get(key);
        if (valueList != null)
        {

            ArrayList<Integer> valueSet = new ArrayList<Integer>();

            if (valueSet != null)
            {
                StringTokenizer valueTokenizer = new StringTokenizer(valueList, ",");
                while (valueTokenizer.hasMoreTokens())
                {
                    String listValue = valueTokenizer.nextToken();
                    int intValue = Integer.parseInt(listValue.trim());
                    valueSet.add(intValue);
                }
            }

            returnArray = new int[valueSet.size()];
            int i = 0;
            for (int v : valueSet)
                returnArray[i++] = v;

        } else
        {
            logger.info("property file key: " + key
                    + " missing.  No integer value can be determined.");
            throw new RuntimeException();
        }

        return returnArray;

    }

    public static float[] getFloatArrayFromPropertyMap(HashMap<String, String> rbMap, String key)
    {

    	float[] returnArray;
        String valueList = rbMap.get(key);
        if (valueList != null)
        {

            ArrayList<Float> valueSet = new ArrayList<Float>();

            StringTokenizer valueTokenizer = new StringTokenizer(valueList, ",");
            while (valueTokenizer.hasMoreTokens())
            {
                 String listValue = valueTokenizer.nextToken();
                 float floatValue = Float.parseFloat(listValue.trim());
                 valueSet.add(floatValue);
            }

            returnArray = new float[valueSet.size()];
            int i = 0;
            for (float v : valueSet)
                returnArray[i++] = v;

        } else
        {
            logger.info("property file key: " + key
                    + " missing.  No float value can be determined.");
            throw new RuntimeException();
        }

        return returnArray;

    }

    public static double[] getDoubleArrayFromPropertyMap(HashMap<String, String> rbMap, String key)
    {

    	double[] returnArray;
        String valueList = rbMap.get(key);
        if (valueList != null)
        {

            ArrayList<Double> valueSet = new ArrayList<Double>();

            StringTokenizer valueTokenizer = new StringTokenizer(valueList, ",");
            while (valueTokenizer.hasMoreTokens())
            {
                 String listValue = valueTokenizer.nextToken();
                 double doubleValue = Double.parseDouble(listValue.trim());
                 valueSet.add(doubleValue);
            }

            returnArray = new double[valueSet.size()];
            int i = 0;
            for (double v : valueSet)
                returnArray[i++] = v;

        } else
        {
            logger.info("property file key: " + key
                    + " missing.  No double value can be determined.");
            throw new RuntimeException();
        }

        return returnArray;

    }

   /**
     * 
     * @param cumProbabilities
     *            cumulative probabilities array
     * @param entry
     *            target to search for in array
     * @return the array index i where cumProbabilities[i] < entry and
     *         cumProbabilities[i-1] <= entry.
     */
    public static int binarySearchDouble(double[] cumProbabilities, double entry)
    {

        // lookup index for 0 <= entry < 1.0 in cumProbabilities
        // cumProbabilities values are assumed to be in range: [0,1], and
        // cumProbabilities[cumProbabilities.length-1] must equal 1.0

        // if entry is outside the allowed range, return -1
        if (entry < 0 || entry >= 1.0)
        {
            System.out.println("entry = " + entry
                    + " is outside of allowable range for cumulative distribution [0,...,1.0)");
            return -1;
        }

        // if cumProbabilities[cumProbabilities.length-1] is not equal to 1.0,
        // return -1
        double epsilon = .0000001;
        if (!(Math.abs(cumProbabilities[cumProbabilities.length - 1] - 1.0) < epsilon))
        {
            System.out.println("cumProbabilities[cumProbabilities.length-1] = "
                    + cumProbabilities[cumProbabilities.length - 1] + " must equal 1.0");
            return -1;
        }

        int hi = cumProbabilities.length;
        int lo = 0;
        int mid = (hi - lo) / 2;

        int safetyCount = 0;

        // if mid is 0,
        if (mid == 0)
        {
            if (entry < cumProbabilities[0]) return 0;
            else return 1;
        } else if (entry < cumProbabilities[mid] && entry >= cumProbabilities[mid - 1])
        {
            return mid;
        }

        while (true)
        {

            if (entry < cumProbabilities[mid])
            {
                hi = mid;
                mid = (hi + lo) / 2;
            } else
            {
                lo = mid;
                mid = (hi + lo) / 2;
            }

            // if mid is 0,
            if (mid == 0)
            {
                if (entry < cumProbabilities[0]) return 0;
                else return 1;
            } else if (entry < cumProbabilities[mid] && entry >= cumProbabilities[mid - 1])
            {
                return mid;
            }

            if (safetyCount++ > cumProbabilities.length)
            {
                logger.info("binary search stuck in the while loop");
                throw new RuntimeException("binary search stuck in the while loop");
            }

        }

    }

    /**
     * 
     * @param cumProbabilities
     *            cumulative probabilities array
     * @param numIndices
     *            are the number of probability values to consider in the
     *            cumulative probabilities array
     * @param entry
     *            target to search for in array between indices 1 and numValues.
     * @return the array index i where cumProbabilities[i] < entry and
     *         cumProbabilities[i-1] <= entry.
     */
    public static int binarySearchDouble(double cumProbabilityLowerBound,
            double[] cumProbabilities, int numIndices, double entry)
    {

        // search for 0-based index i for cumProbabilities such that
        // cumProbabilityLowerBound <= entry < cumProbabilities[0], i = 0;
        // or
        // cumProbabilities[i-1] <= entry < cumProbabilities[i], for i =
        // 1,...numIndices-1;

        // if entry is outside the allowed range, return -1
        if (entry < cumProbabilityLowerBound || entry >= cumProbabilities[numIndices - 1])
        {
            logger.info("entry = " + entry
                    + " is outside of allowable range of cumulative probabilities.");
            logger.info("cumProbabilityLowerBound = " + cumProbabilityLowerBound
                    + ", cumProbabilities[numIndices-1] = " + cumProbabilities[numIndices - 1]
                    + ", numIndices = " + numIndices);
            return -1;
        }

        int hi = numIndices;
        int lo = 0;
        int mid = (hi - lo) / 2;

        int safetyCount = 0;

        // if mid is 0,
        if (mid == 0)
        {
            if (entry < cumProbabilities[0]) return 0;
            else return 1;
        } else if (entry < cumProbabilities[mid] && entry >= cumProbabilities[mid - 1])
        {
            return mid;
        }

        while (true)
        {

            if (entry < cumProbabilities[mid])
            {
                hi = mid;
                mid = (hi + lo) / 2;
            } else
            {
                lo = mid;
                mid = (hi + lo) / 2;
            }

            // if mid is 0,
            if (mid == 0)
            {
                if (entry < cumProbabilities[0]) return 0;
                else return 1;
            } else if (entry < cumProbabilities[mid] && entry >= cumProbabilities[mid - 1])
            {
                return mid;
            }

            if (safetyCount++ > numIndices)
            {
            	logger.info("binary search stuck in the while loop");
                throw new RuntimeException("binary search stuck in the while loop");
            }

        }

    }
    
    /**
     * REad a tabledataset from a CSV file and return it.
     * 
     * @param fileName
     * @return
     */
    public static TableDataSet readTableDataSet(String fileName) {
    	
    	
        TableDataSet tableData;

        try
        {
            OLD_CSVFileReader csvFile = new OLD_CSVFileReader();
            tableData = csvFile.readFile(new File(fileName));
        } catch (IOException e)
        {
        	logger.fatal("Error trying to read table data set from csv file: "+ fileName);
            throw new RuntimeException(e);
        }

    	return tableData;
    }

}
