package org.sandag.abm.reporting;

import java.text.DecimalFormat;
import java.util.concurrent.BlockingQueue;
import org.apache.log4j.Logger;
import com.pb.common.matrix.Matrix;

public class TruckCsvPublisherThread
        implements Runnable
{
    private static final Logger LOGGER                      = Logger.getLogger(TruckCsvPublisherThread.class);
    
    private BlockingQueue<CsvRow>      queue;
    private IMatrixDao        mtxDao;
    private String                     matrixName;
    private String                     tod;
    private String[]                   cores;

    private final double               sizeThreshold = 0.00001;

    private static final DecimalFormat FORMATTER     = new DecimalFormat("#.######");

    public TruckCsvPublisherThread(BlockingQueue<CsvRow> aQueue,
            IMatrixDao aMtxDao, String aMatrixName, String aTod, String[] theCores)
    {
        this.queue = aQueue;
        this.mtxDao = aMtxDao;
        this.matrixName = aMatrixName;
        this.tod = aTod;
        this.cores = theCores;
    }

    @Override
    public void run()
    {
        for (String core : cores)
        {
            Matrix matrix = mtxDao.getMatrix(matrixName, core);
            try
            {
                addRowsToQueue(core, matrix);
            } catch (InterruptedException e)
            {
                LOGGER.fatal(e);
                throw new RuntimeException(e);
            }
        }

    }

    public void addRowsToQueue(String core, Matrix matrix) throws InterruptedException
    {
        for (int origin : matrix.getExternalNumbers())
        {
            for (int dest : matrix.getExternalColumnNumbers())
            {
                float trips = matrix.getValueAt(origin, dest);
                if (trips > sizeThreshold)
                {
                    CsvRow row = new CsvRow(new String[] {String.valueOf(origin),
                            String.valueOf(dest), tod, core, FORMATTER.format(trips)});
                    queue.put(row);
                }
            }
        }
    }
}
