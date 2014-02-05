package org.sandag.abm.reporting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.Test;

public class CsvWriterThreadTest
{

    @Test
    public void testRun() throws Exception
    {
        File testFile = new File("writer" + new Date().getTime());

        String[] sampleRow = new String[] {"col1", "col2", "col3", "col4"};
        String[] headerRow = new String[] {"hdr1", "hdr2", "hdr3", "hdr4"};

        BlockingQueue<CsvRow> queue = new LinkedBlockingQueue<CsvRow>();

        CsvWriterThread writer = new CsvWriterThread(queue, testFile, headerRow);
        writer.setMaxBuffer(1024);
        Thread thread = new Thread(writer);

        thread.start();

        for (int i = 0; i < 30000; i++)
            queue.add(new CsvRow(sampleRow));

        queue.add(CsvWriterThread.POISON_PILL);

        thread.join();

        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(testFile));
            String headerLine = reader.readLine();
            assertEquals("hdr1,hdr2,hdr3,hdr4", headerLine);

            String rowLine = null;
            while (null != (rowLine = reader.readLine()))
                assertEquals("col1,col2,col3,col4", rowLine);
        } finally
        {
            if (null != reader) reader.close();
            if (null != testFile) assertTrue(testFile.delete());
        }
    }

}
