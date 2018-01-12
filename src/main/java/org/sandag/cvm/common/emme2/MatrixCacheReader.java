package org.sandag.cvm.common.emme2;

import java.util.Hashtable;

import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixException;
import com.pb.common.matrix.MatrixReader;

public class MatrixCacheReader {
	
	MatrixReader baseReader;

	public MatrixCacheReader(MatrixReader baseReader) {
		this.baseReader = baseReader;
//		Matrix[] ms = baseReader.readMatrices();
//		for (Matrix m : ms) {
//			readMatrices.put(m.getName(), m);
//		}
	}

    Hashtable readMatrices = new Hashtable();

	/** Reads an entire matrix from a reader, but if it's already been
     * read once before returns the previously read one 
     *
     * @param index the short name of the matrix, eg. "mf10"
     * @return a complete matrix
     * @throws MatrixException
     */
    public synchronized Matrix readMatrix(String index) throws MatrixException {
        
        if (readMatrices.containsKey(index)) {
            return (Matrix) readMatrices.get(index);
        }
        Matrix m = baseReader.readMatrix(index );
        readMatrices.put(index,m);
        return m;
        
    }

	public MatrixReader getActualReader() {
		return baseReader;
	}

}
