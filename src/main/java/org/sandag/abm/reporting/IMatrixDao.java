package org.sandag.abm.reporting;

import com.pb.common.matrix.Matrix;

public interface IMatrixDao
{
    Matrix getMatrix(String matrixName, String coreName);
}
