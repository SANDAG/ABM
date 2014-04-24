package org.sandag.abm.active.sandag;
import java.util.*;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import org.sandag.abm.active.BinarySearch;
import static org.junit.Assert.*;
import org.junit.*;


public class BinarySearchTest
{  
    @Test
    public void testBinarySearch()
    {
        double[] values = new double[] {0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};
        assertEquals(0, BinarySearch.binarySearch(values,0.00) );
        assertEquals(0, BinarySearch.binarySearch(values,0.10) );
        assertEquals(1, BinarySearch.binarySearch(values,0.15) );
        assertEquals(1, BinarySearch.binarySearch(values,0.20) );
        assertEquals(2, BinarySearch.binarySearch(values,0.25) );
        assertEquals(2, BinarySearch.binarySearch(values,0.30) );
        assertEquals(3, BinarySearch.binarySearch(values,0.35) );
        assertEquals(3, BinarySearch.binarySearch(values,0.40) );
        assertEquals(4, BinarySearch.binarySearch(values,0.45) );
        assertEquals(4, BinarySearch.binarySearch(values,0.50) );
        assertEquals(5, BinarySearch.binarySearch(values,0.55) );
        assertEquals(5, BinarySearch.binarySearch(values,0.60) );
        assertEquals(6, BinarySearch.binarySearch(values,0.65) );
        assertEquals(6, BinarySearch.binarySearch(values,0.70) );
        assertEquals(7, BinarySearch.binarySearch(values,0.75) );
        assertEquals(7, BinarySearch.binarySearch(values,0.80) );
        assertEquals(8, BinarySearch.binarySearch(values,0.85) );
        assertEquals(8, BinarySearch.binarySearch(values,0.90) );
        assertEquals(9, BinarySearch.binarySearch(values,0.95) );
        assertEquals(9, BinarySearch.binarySearch(values,1.00) );
        
        values = new double[] {0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9};
        assertEquals(0, BinarySearch.binarySearch(values,0.00) );
        assertEquals(0, BinarySearch.binarySearch(values,0.10) );
        assertEquals(1, BinarySearch.binarySearch(values,0.15) );
        assertEquals(1, BinarySearch.binarySearch(values,0.20) );
        assertEquals(2, BinarySearch.binarySearch(values,0.25) );
        assertEquals(2, BinarySearch.binarySearch(values,0.30) );
        assertEquals(3, BinarySearch.binarySearch(values,0.35) );
        assertEquals(3, BinarySearch.binarySearch(values,0.40) );
        assertEquals(4, BinarySearch.binarySearch(values,0.45) );
        assertEquals(4, BinarySearch.binarySearch(values,0.50) );
        assertEquals(5, BinarySearch.binarySearch(values,0.55) );
        assertEquals(5, BinarySearch.binarySearch(values,0.60) );
        assertEquals(6, BinarySearch.binarySearch(values,0.65) );
        assertEquals(6, BinarySearch.binarySearch(values,0.70) );
        assertEquals(7, BinarySearch.binarySearch(values,0.75) );
        assertEquals(7, BinarySearch.binarySearch(values,0.80) );
        assertEquals(8, BinarySearch.binarySearch(values,0.85) );
        assertEquals(8, BinarySearch.binarySearch(values,0.90) );   
    }

}
