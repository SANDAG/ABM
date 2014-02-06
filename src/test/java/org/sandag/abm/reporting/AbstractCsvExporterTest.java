package org.sandag.abm.reporting;

import static org.junit.Assert.assertEquals;
import java.io.File;
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;

public class AbstractCsvExporterTest
{
    private AbstractCsvExporter csvExporter;
    private IMatrixDao mtxDao;
    private String baseFileName = "testCsvFile";
    
    @Before
    public void setUp()
    {
        Properties properties = new Properties();
        properties.put("report.path", ".");
     
        mtxDao = new TranscadMatrixDao(properties);

        csvExporter = new AbstractCsvExporterMock(properties, mtxDao, baseFileName);
    }
    
    @Test
    public void testGetMatrixDao()
    {
        IMatrixDao retrievedMtxDao = csvExporter.getMatrixDao();
        assertEquals(mtxDao, retrievedMtxDao);
    }

    @Test
    public void testGetFile()
    {
        File expectedFile = new File(".", baseFileName + ".csv");
        File retrievedFile = csvExporter.getFile();
        assertEquals(expectedFile.getAbsolutePath(), retrievedFile.getAbsolutePath());
    }

}
