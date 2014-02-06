package org.sandag.abm.reporting;

import com.pb.common.matrix.Matrix;

public class MockMatrixDao
        implements IMatrixDao
{
    public Matrix getMatrix(String matrixName, String coreName)
    {
        float[][] matrixArray = new float[5][5];
        
        float counter = 0f;
        
        for(float[] rows : matrixArray)
        {
            for(float cells: rows)
            {
                cells = ++counter; 
            }
        }
        
        return new Matrix(matrixArray);
    }
}
