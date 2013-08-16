package org.sandag.abm.application;

import static org.junit.Assert.*;
import org.junit.Test;

public class SandagModelStructureTest
{

    @Test
    public void testGetTripModeIsPay()
    {
        assertTrue(SandagModelStructure.getTripModeIsPay(2));
        assertTrue(SandagModelStructure.getTripModeIsPay(5));
        assertTrue(SandagModelStructure.getTripModeIsPay(8));
        
        assertFalse(SandagModelStructure.getTripModeIsPay(1));
    }

}
