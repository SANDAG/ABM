package org.sandag.abm.reporting;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TruckCsvExporter
        extends AbstractCsvExporter
{
    private static final String   MATRIX_BASE_NAME = "Trip_" + TOD_TOKEN;
    private static final String[] CORE_NAMES       = {"lhdn", "lhdt", "mhdn", "mhdt", "hhdn",
            "hhdt"                                 };
    private static final String[] COLUMN_HEADERS   = {"ORIG", "DEST", "TOD", "CLASS", "TRIPS"};

    public TruckCsvExporter(Properties properties, TranscadMatrixDao aMatrixServerWrapper,
            String aBaseFileName)
    {
        super(properties, aMatrixServerWrapper, aBaseFileName);
    }

    @Override
    public void export() throws IOException
    {
        BlockingQueue<CsvRow> queue = new LinkedBlockingQueue<CsvRow>();

        Thread[] threads = new Thread[TOD_TOKENS.length];

        LOGGER.info("Initializing Truck Writer Thread. Output Location: "
                + getFile().getAbsoluteFile());
        CsvWriterThread writerThread = new CsvWriterThread(queue, getFile(), COLUMN_HEADERS);
        new Thread(writerThread).start();

        for (int i = 0; i < TOD_TOKENS.length; i++)
        {
            String matrixName = MATRIX_BASE_NAME.replace(TOD_TOKEN, TOD_TOKENS[i]);
            LOGGER.info("Initializing Truck Reader Thread. Matrix: " + matrixName);
            TruckCsvPublisherThread publisherThread = new TruckCsvPublisherThread(queue,
                    getMatrixDao(), matrixName, TOD_TOKENS[i], CORE_NAMES);
            threads[i] = new Thread(publisherThread);
            threads[i].start();
        }

        for (Thread thread : threads)
        {
            try
            {
                thread.join();
            } catch (InterruptedException e)
            {
                e.printStackTrace(System.err);
            }
        }

        LOGGER.info("Initializing Truck Reader Threads Complete. Issuing Poison Pill to Writer.");
        queue.add(CsvWriterThread.POISON_PILL);
    }
}
