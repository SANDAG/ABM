package org.sandag.abm.reporting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.lang.math.NumberUtils;
import org.junit.Before;
import org.junit.Test;

public class TruckCsvPublisherThreadTest
{
    private TruckCsvPublisherThread publisherThread;

    private BlockingQueue<CsvRow>   queue;
    private IMatrixDao              mtxDao;

    private final String            matrixName = "MOCK_MATRIX";
    private final String            tod        = "XX";

    private final String[]          cores      = new String[] {"lhdn", "mhdn", "hhdn"};

    @Before
    public void setUp()
    {
        queue = new LinkedBlockingQueue<CsvRow>();
        mtxDao = new MockMatrixDao();

        publisherThread = new TruckCsvPublisherThread(queue, mtxDao, matrixName, tod, cores);
    }

    @Test
    public void testRun() throws Exception
    {
        publisherThread.run();

        assertEquals(75, queue.size());

        queue.add(CsvWriterThread.POISON_PILL);

        CsvRow row = null;

        while ((row = queue.take()) != CsvWriterThread.POISON_PILL)
        {

            String[] values = row.getRow().split(",");
            assertEquals(5, values.length);

            assertTrue(NumberUtils.isNumber(values[0]));
            assertTrue(NumberUtils.isNumber(values[1]));
            assertTrue(Float.parseFloat(values[4]) > 0.00001);
        }
    }

}
