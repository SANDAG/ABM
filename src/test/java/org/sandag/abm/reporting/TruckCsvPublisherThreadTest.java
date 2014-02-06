package org.sandag.abm.reporting;

import static org.junit.Assert.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.Before;
import org.junit.Test;

public class TruckCsvPublisherThreadTest
{
    private TruckCsvPublisherThread publisherThread;

    private BlockingQueue<CsvRow>   queue;
    private IMatrixDao              mtxDao;

    private final String            matrixName = "MOCK_MATRIX";
    private final String            tod        = "XX";

    private final String[]                cores      = new String[] {"lhdn", "mhdn", "hhdn"};

    @Before
    public void setUp()
    {
        queue = new LinkedBlockingQueue<CsvRow>();
        mtxDao = new MockMatrixDao();
        
        
        publisherThread = new TruckCsvPublisherThread(queue, mtxDao, matrixName, tod, cores);
    }

//    @Test
//    public void testRun()
//    {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testAddRowsToQueue()
//    {
//        fail("Not yet implemented");
//    }

}
