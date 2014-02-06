package org.sandag.abm.reporting;

import com.pb.common.matrix.Matrix;

public class MockMatrixDao
        implements IMatrixDao
{
    public Matrix getMatrix(String matrixName, String coreName)
    {
        Matrix mtx = new Matrix(5,5);

        int[] origins = mtx.getInternalRowNumbers();
        int[] dests = mtx.getInternalColumnNumbers();
        
        
        for (int origin : origins)
        {
            for (int dest : dests)
            {
                mtx.setValueAt(origin, dest, (float) (1 + Math.random()));
            }
        }

        return mtx;
    }
}
