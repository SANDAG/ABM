package org.sandag.abm.reporting;

import java.io.File;
import java.util.Properties;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixType;

public class TranscadMatrixDao
{
    private final String outputFolderToken = "skims.path";
    private final String matrixLocation;

    public TranscadMatrixDao(Properties properties)
    {
        matrixLocation = properties.getProperty(outputFolderToken);
    }

    public synchronized Matrix getMatrix(String matrixName, String coreName)
    {
        String matrixPath = matrixLocation + File.separator + matrixName + ".mtx";

        MatrixReader mr = MatrixReader.createReader(MatrixType.TRANSCAD, new File(matrixPath));

        return mr.readMatrix(coreName);
    }
}
