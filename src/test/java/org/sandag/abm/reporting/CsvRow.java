package org.sandag.abm.reporting;

public class CsvRow
{
    private final String row;
    
    public CsvRow(String[] values)
    {
        StringBuilder sb = new StringBuilder(32);
        sb.append(values[0]);
        for (int i = 1; i < values.length; i++)
            sb.append(',').append(values[i]);
        sb.append(System.lineSeparator());

        row = sb.toString();
    }
    
    public String getRow()
    {
        return row;
    }
}
