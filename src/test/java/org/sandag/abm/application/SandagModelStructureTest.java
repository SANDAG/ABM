package org.sandag.abm.application;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class SandagModelStructureTest
{

    @Test
    public void testGetTripModeIsPay()
    {
        assertTrue(SandagModelStructure.getTripModeIsPay(2));
        assertTrue(SandagModelStructure.getTripModeIsPay(4));
        assertTrue(SandagModelStructure.getTripModeIsPay(6));

        assertFalse(SandagModelStructure.getTripModeIsPay(1));
    }

}
