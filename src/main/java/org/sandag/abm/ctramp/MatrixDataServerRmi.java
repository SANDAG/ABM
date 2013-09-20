package org.sandag.abm.ctramp;

import java.io.Serializable;
import com.pb.common.calculator.DataEntry;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixType;
import org.sandag.abm.ctramp.UtilRmi;

/**
 * @author Jim Hicks
 * 
 *         Class for managing matrix data in a remote process and accessed by UECs using RMI.
 */
public class MatrixDataServerRmi
        implements MatrixDataServerIf, Serializable
{

    // protected static Logger logger = Logger.getLogger(MatrixDataServerRmi.class);

    UtilRmi remote;
    String  connectString;

    public MatrixDataServerRmi(String hostname, int port, String className)
    {

        connectString = String.format("//%s:%d/%s", hostname, port, className);
        remote = new UtilRmi(connectString);

    }

    public void clear()
    {
        Object[] objArray = {};
        remote.method("clear", objArray);
    }

    public void writeMatrixFile(String fileName, Matrix[] m, MatrixType mt)
    {
        Object[] objArray = {fileName, m, mt};
        remote.method("writeMatrixFile", objArray);
    }

    public Matrix getMatrix(DataEntry dataEntry)
    {
        Object[] objArray = {dataEntry};
        return (Matrix) remote.method("getMatrix", objArray);
    }

    public void start32BitMatrixIoServer(MatrixType mType)
    {
        Object[] objArray = {mType};
        remote.method("start32BitMatrixIoServer", objArray);
    }

    public void start32BitMatrixIoServer(MatrixType mType, String label)
    {
        Object[] objArray = {mType, label};
        remote.method("start32BitMatrixIoServer", objArray);
    }

    public void stop32BitMatrixIoServer()
    {
        Object[] objArray = {};
        remote.method("stop32BitMatrixIoServer", objArray);
    }

    public void stop32BitMatrixIoServer(String label)
    {
        Object[] objArray = {label};
        remote.method("stop32BitMatrixIoServer", objArray);
    }

    public String testRemote(String remoteObjectName)
    {
        Object[] objArray = {remoteObjectName};
        return (String) remote.method("testRemote", objArray);
    }

    public String testRemote()
    {
        Object[] objArray = {};
        return (String) remote.method("testRemote", objArray);
    }

}