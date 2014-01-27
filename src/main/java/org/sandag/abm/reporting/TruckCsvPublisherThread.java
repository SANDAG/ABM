package org.sandag.abm.reporting;

import java.text.DecimalFormat;
import java.util.concurrent.BlockingQueue;
import com.pb.common.matrix.Matrix;

public class TruckCsvPublisherThread
        implements Runnable
{
    private BlockingQueue<CsvRow>      queue;
    private MatrixServerWrapper        mtxSvrWrapper;
    private String                     matrixName;
    private String                     tod;
    private String[]                   cores;

    private static final DecimalFormat FORMATTER = new DecimalFormat("#.######");

    public TruckCsvPublisherThread(BlockingQueue<CsvRow> aQueue,
            MatrixServerWrapper aMtxSvrWrapper, String aMatrixName, String aTod, String[] theCores)
    {
        this.queue = aQueue;
        this.mtxSvrWrapper = aMtxSvrWrapper;
        this.matrixName = aMatrixName;
        this.tod = aTod;
        this.cores = theCores;
    }

    @Override
    public void run()
    {
        for (String core : cores)
        {
            Matrix matrix = mtxSvrWrapper.getMatrix(matrixName, core);
            int[] origins = matrix.getExternalNumbers();
            int[] destinations = matrix.getExternalColumnNumbers();

            for (int origin : origins)
            {
                for (int dest : destinations)
                {
                    float trips = matrix.getValueAt(origin, dest);
                    if (trips > 0.00001)
                    {
                        CsvRow row = new CsvRow(new String[] {String.valueOf(origin),
                                String.valueOf(dest), tod, core, FORMATTER.format(trips)});
                        try
                        {
                            queue.put(row);
                        } catch (InterruptedException e)
                        {
                            e.printStackTrace(System.err);
                        }
                    }
                }
            }
        }

    }
}
