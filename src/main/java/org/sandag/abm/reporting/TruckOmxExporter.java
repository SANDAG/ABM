package org.sandag.abm.reporting;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixWriter;

public class TruckOmxExporter implements IExporter
{
    private static final String   MATRIX_BASE_NAME = "DailyDistributionMatricesTruck" + TOD_TOKEN;
    private static final String[] CORE_NAMES       = {"lhdn", "lhdt", "mhdn", "mhdt", "hhdn",
            "hhdt"                                 };
   
    private IMatrixDao      matrixDao;
    private String          reportFolder = "report.path";
    private Properties properties;
    
    protected static final Logger LOGGER       = Logger.getLogger(AbstractCsvExporter.class);
    
    
    public TruckOmxExporter(Properties properties, IMatrixDao aMatrixServerWrapper,
            String aBaseFileName)
    {
        this.matrixDao = aMatrixServerWrapper;
        this.properties = properties;
   }

    @Override
    public void export() throws IOException
    {
      
        for (int i = 0; i < TOD_TOKENS.length; i++)
        {
            String matrixName = MATRIX_BASE_NAME.replace(TOD_TOKEN, TOD_TOKENS[i]);
            
            File outMatrixFile = new File(properties.getProperty(reportFolder), matrixName+".omx");
            
            MatrixWriter matrixWriter = MatrixWriter.createWriter(MatrixType.OMX, outMatrixFile);
            Matrix[] inMatrix = new Matrix[CORE_NAMES.length];
            
            for(int j = 0; j < CORE_NAMES.length; ++j)
            	inMatrix[j] = matrixDao.getMatrix(matrixName, CORE_NAMES[j]);
            
            
            matrixWriter.writeMatrices(CORE_NAMES, inMatrix);
           
        }
    }
}
