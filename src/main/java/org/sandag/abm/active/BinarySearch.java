package org.sandag.abm.active;

public class BinarySearch
{
    public static int binarySearch(double[] values, double target)
    {
        return binarySearch(values, target, 0, values.length-1);
    }
    
    public static int binarySearch(double[] values, double target, int lower, int upper)
    {   
        if ( lower <= upper ) {
            int mid = (lower + upper) / 2;
            
            switch ( Double.compare(values[mid],target) ) {
                case 0: return mid; 
                case 1: return binarySearch(values, target, lower, upper - 1);
                case -1: return binarySearch(values, target, mid + 1, upper);  
            }
        }

        if ( values[lower] >= target ) {
            return lower;
        } else {
            return lower + 1;
        }
    }
}
