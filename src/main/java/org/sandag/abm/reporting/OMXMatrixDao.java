package org.sandag.abm.reporting;

import java.io.File;
import java.util.Properties;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixType;

public class OMXMatrixDao
        implements IMatrixDao
{
    private final String outputFolderToken = "skims.path";
    private final String matrixLocation;

    public OMXMatrixDao(Properties properties)
    {
        matrixLocation = properties.getProperty(outputFolderToken);
    }

    public Matrix getMatrix(String matrixName, String coreName)
    {
        String matrixPath = matrixLocation + File.separator + matrixName;

        MatrixReader mr = MatrixReader.createReader(MatrixType.OMX, new File(matrixPath));

        return mr.readMatrix(coreName);
    }
}
