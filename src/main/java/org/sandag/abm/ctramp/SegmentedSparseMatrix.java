package org.sandag.abm.ctramp;

public interface SegmentedSparseMatrix<S> {
	double getValue(S segment, int rowId, int columnId);
}
