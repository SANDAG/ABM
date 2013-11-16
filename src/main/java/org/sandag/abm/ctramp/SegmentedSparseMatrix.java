package org.sandag.abm.ctramp;

/**
 * The {@code SegmentedSparseMatrix} interface is used to find values in sparse matrices segmented in some manner. The details about the segmentation,
 * storage, and lookup are left to the implementation.
 *
 * @param <S>
 *        The type that the matrices are segmented against.
 */
public interface SegmentedSparseMatrix<S> {
	/**
	 * Get the value of the matrix for a specified segment and row/column ids.
	 * 
	 * @param segment
	 *        The segment.
	 *        
	 * @param rowId
	 *        The row id.
	 *        
	 * @param columnId
	 *        The column id.
	 *        
	 * @return the matrix value at <code>(rowId,columnId)</code> for {@code segment}.
	 */
	double getValue(S segment, int rowId, int columnId);
}
