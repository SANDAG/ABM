/**
 * 
 */
package org.sandag.cvm.common.skims;

import java.io.File;

import org.apache.log4j.Logger;

import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixException;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.TranscadMatrixReader;

/**
 * Reads a matrix by name with two parts separated by a colon.  The first part is the file
 * name (without the .mtx extension), the second part is the "core" name within the file.  
 * 
 * Transcad creates matrix files, but they are three dimensional matrices, with the z dimension being
 * referred to as "cores".  This allows treating these files as a list of two dimensional matrices
 * @author johna
 *
 */
public class TranscadMatrixCollectionReader extends MatrixReader {
	
    protected static Logger logger = Logger.getLogger(TranscadMatrixCollectionReader.class);
    
    protected File directoryOfMatrices;

	/**
	 * 
	 */
	public TranscadMatrixCollectionReader(File directory) {
    	directoryOfMatrices = directory;
	}

	/* (non-Javadoc)
	 * @see com.pb.common.matrix.MatrixReader#readMatrix(java.lang.String)
	 */
	@Override
	public Matrix readMatrix(String name) throws MatrixException {
		String[] split = name.split(":");
		if (split.length>2) {
			String msg = "Matrix name "+name+" has more than 1 part";
		}
		TranscadMatrixReader r = new TranscadMatrixReader(new File(directoryOfMatrices,split[0]+".mtx"));
		if (split.length == 1) return r.readMatrix(0);
		return r.readMatrix(split[1]);
	}

	/* (non-Javadoc)
	 * @see com.pb.common.matrix.MatrixReader#readMatrix()
	 */
	@Override
	public Matrix readMatrix() throws MatrixException {
		throw new RuntimeException("Can't read Transcad Matrix without specifying file_name:matrix_name");
	}

	/* (non-Javadoc)
	 * @see com.pb.common.matrix.MatrixReader#readMatrices()
	 */
	@Override
	public Matrix[] readMatrices() throws MatrixException {
		throw new RuntimeException("Can't read Transcad Matrices without specifying the file name, this java class is to be used for an entire directory of files.");
	}

}
