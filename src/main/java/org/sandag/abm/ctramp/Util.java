package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;

public final class Util
        implements Serializable
{
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
            System.out.println("property file key: " + key + " = " + value
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

    public static int getIntegerValueFromPropertyMap(HashMap<String, String> rbMap, String key)
    {
        String value = rbMap.get(key);
        if (value != null)
        {
            return Integer.parseInt(value);
        } else
        {
            System.out.println("property file key: " + key
                    + " missing.  No integer value can be determined.");
            throw new RuntimeException();
        }
    }

    public static int[] getIntegerArrayFromPropertyMap(HashMap<String, String> rbMap, String key)
    {

        int[] returnArray;
        String valueList = rbMap.get(key);
        if (valueList != null)
        {

            HashSet<Integer> valueSet = new HashSet<Integer>();

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
            System.out.println("property file key: " + key
                    + " missing.  No integer value can be determined.");
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
        if (cumProbabilities[cumProbabilities.length - 1] != 1.0)
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
                System.out.println("binary search stuck in the while loop");
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
            System.out.println("entry = " + entry
                    + " is outside of allowable range of cumulative probabilities.");
            System.out.println("cumProbabilityLowerBound = " + cumProbabilityLowerBound
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
                System.out.println("binary search stuck in the while loop");
                throw new RuntimeException("binary search stuck in the while loop");
            }

        }

    }

}
