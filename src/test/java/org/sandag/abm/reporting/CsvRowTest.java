package org.sandag.abm.reporting;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class CsvRowTest
{
    @Test
    public void testGetRow()
    {
        String[] sampleRow = new String[]{"col1", "col2", "col3", "col4"};
        CsvRow row = new CsvRow(sampleRow);
        assertEquals("col1,col2,col3,col4"+System.lineSeparator(), row.getRow());
    }
}
