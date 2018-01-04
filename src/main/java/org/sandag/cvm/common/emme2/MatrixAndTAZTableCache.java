package org.sandag.cvm.common.emme2;

import java.util.Arrays;

import org.apache.log4j.Logger;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.ColumnVector;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixException;
import com.pb.common.matrix.MatrixReader;

public class MatrixAndTAZTableCache extends MatrixCacheReader {
	
	static Logger logger = Logger.getLogger(MatrixAndTAZTableCache.class);

	private TableDataSet tazData;
	private int[] externalNumberForColumns = new int[] {1};
	
	/**
	 * Variable for recording how big our matrices are
	 */
	private int dim2MatrixSize;
	/**
	 * @return size of most recently read 2D matrix
	 */
	public int getDim2MatrixSize() {
		return dim2MatrixSize;
	}

	/**
	 * @return external numbers of most recently read 2D matrix
	 */
	public int[] getDim2MatrixExternalNumbers() {
		return dim2MatrixExternalNumbers;
	}

	private int[] dim2MatrixExternalNumbers;

	public MatrixAndTAZTableCache(MatrixReader baseReader, TableDataSet tazData) {
		super(baseReader);
		this.tazData = tazData;
	}

	@Override
	public synchronized Matrix readMatrix(String index) throws MatrixException {
		Matrix m = null;
		// first see if it's already read
        if (readMatrices.containsKey(index)) {
            return (Matrix) readMatrices.get(index);
        }
        // next check the zonal properties
		try {
			int column = tazData.getColumnPosition(index);
			if (column <0) {
				String msg = "Can't find matrix "+index+" in TAZData "+tazData;
				throw new MatrixException(msg);
			}
			int[] externalNumbers = tazData.getColumnAsInt(tazData.checkColumnPosition("TAZ"));
			float[] values = tazData.getColumnAsFloat(column);
			ColumnVector cv = new ColumnVector(values);
			cv.setExternalNumbersZeroBased(externalNumbers, externalNumberForColumns);
			cv.setName(index);
			readMatrices.put(index,cv);
			m = cv;
		} catch (MatrixException e) {
			// finally as a last resort check the 2D skims, but they should already be read and in the cache
			try {
				m = super.readMatrix(index);
				if (m.getRowCount() != m.getColumnCount()) logger.info("Nonsquare matrix "+index);
				else {
					dim2MatrixExternalNumbers = m.getExternalNumbers();
					dim2MatrixSize = m.getColumnCount();
				}
			} catch (MatrixException e2) {
				logger.fatal("Can't find matrix "+index+" in TAZData nor in skims");
				throw new MatrixException("Can't find matrix "+index+" in TAZData nor in skims", e2);
			}
		}
		return m;
	}

}
