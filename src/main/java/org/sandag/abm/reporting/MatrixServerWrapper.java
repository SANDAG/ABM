package org.sandag.abm.reporting;

import java.io.File;
import org.sandag.abm.ctramp.MatrixDataServer;
import org.sandag.abm.ctramp.MatrixDataServerRmi;
import com.pb.common.calculator.DataEntry;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.matrix.Matrix;

public class MatrixServerWrapper
{
    private final String serverAddress;
    private final int    serverPort;
    private final String matrixLocation;

    public MatrixServerWrapper(String aSvrAddress, int aSvrPort, String mtxLocation)
    {
        this.serverAddress = aSvrAddress;
        this.serverPort = aSvrPort;
        this.matrixLocation = mtxLocation;
    }

    public Matrix getMatrix(String matrixName, String coreName)
    {
        String matrixPath = matrixLocation + File.separator + matrixName + ".mtx";
        DataEntry dataEntry = new DataEntry("matrix", matrixPath + " " + coreName, "transcad",
                matrixPath, coreName, "", false);
        
        MatrixDataServerIf server = new MatrixDataServerRmi(serverAddress, serverPort, MatrixDataServer.MATRIX_DATA_SERVER_NAME);
        return server.getMatrix(dataEntry);
    }
}
