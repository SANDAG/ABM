/**
 * 
 */
package org.sandag.cvm.common.skims;

import java.io.File;
import java.util.Vector;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.log4j.Logger;

import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixException;
import com.pb.common.matrix.MatrixWriter;

import ncsa.hdf.object.Dataset;
import ncsa.hdf.object.Datatype;
import ncsa.hdf.object.Group;
import ncsa.hdf.object.h5.H5CompoundDS;
import ncsa.hdf.object.h5.H5Datatype;
import ncsa.hdf.object.h5.H5File;
import ncsa.hdf.object.h5.H5Group;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;

/**
 * @author jabraham
 *
 */
public class HDF5MatrixWriter extends MatrixWriter {


	String node;
	private H5File hdf5File;
	Logger logger = Logger.getLogger(HDF5MatrixWriter.class);

	public HDF5MatrixWriter( File openedHdf5File, String node ) {
		super();
		this.node = node;
		this.hdf5File = (H5File) openedHdf5File;
	}

	/* (non-Javadoc)
	 * @see com.pb.common.matrix.MatrixWriter#writeMatrices(java.lang.String[], com.pb.common.matrix.Matrix[])
	 */
	@Override
	public void writeMatrices(String[] names, Matrix[] ms)
	throws MatrixException {

		if (names.length != ms.length){
			String msg = "Can't write matrices without names for each matrix";
			logger.fatal(msg);
			throw new MatrixException(msg);
		}

		checkZoneConcurrancy(ms);

		try {

			fillinHDF5(names, ms);

		} catch (Exception e){
			logger.error(e.getMessage());
			throw new MatrixException(e);	
		}
	}

	/*
		try {			
			f = new H5File(file.getAbsolutePath(), H5File.WRITE);
			f.open();
			fillinHDF5(names, ms, f); 
		} catch (Exception ex) {
			logger.info("Couldn't find HDF5 file to write. Will try to create it!");
			f = new H5File(file.getAbsolutePath(), H5File.CREATE);
			try {
				f.open();
				fillinHDF5(names, ms, f); 
			} catch (Exception e) {
				String msg = "Can't create HDF5 File!";
				logger.error(msg, e);
				throw new MatrixException(msg,e);				
			}
		} finally {
			try {
				f.close();
			} catch (HDF5Exception e1) {
				String msg = "Can't close HDF5 File";
				logger.error(msg, e1);
			}
		}
	 */
	/* 
		// An array of 100 'real' components
		Double[] realValues = new Double[100];
		// An array of 100 'imag' components
		Double[] imaginaryValues = new Double[100];
		int[] dimsc = new int[1];
		dimsc[0] = 100;

		int dataspace = -1;
		try {
		dataspace = H5.H5Screate_simple( 1, dimsc, null );
		 // create a compound data type to read the 'real' field of type double

		 int datatype = -1;
		 datatype = H5.H5Tcreate( HDF5Constants.H5T_COMPOUND, 8 );
		H5.H5Tinsert( datatype, "real", 0, HDF5Constants.H5T_NATIVE_DOUBLE );
		 // create the dataset with this dataspace and compound datatype
		 int dataset = -1;
		H5.H5Fopen( file, node, HDF5Constants.H5P_DEFAULT );
		H5.H5Dread( dataset, datatype, dataspace,
		 HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT,
		 realValues );
		// create a compound data type to read the 'real' field of type double

		int datatype2 = -1;
		datatype2 = H5.H5Tcreate( HDF5Constants.H5T_COMPOUND, 8 );
		H5.H5Tinsert( datatype, "imaginary", 0,
		 HDF5Constants.H5T_NATIVE_DOUBLE);

		H5.H5Dread( dataset, datatype, dataspace,
		 HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT,
		 imaginaryValues );
		} catch ( HDF5Exception ex ) {
		 logger.fatal(ex);
		 throw new RuntimeException(ex);
		} */
	//In this example, all the records are read field by field. The first component, "real", is read into an array of 100 Doubles, and the second field is read into a second array.
	/*
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) f.getRootNode();
		H5Group pgroup = (H5Group)((DefaultMutableTreeNode)f.getRootNode()).getUserObject();
		 long[] dims = {100, 50};
		 long[] chunks = {1, 50};
		 int gzip = 9;
		 String[] memberNames = {"x", "y"};

		 H5Datatype type = new H5Datatype(H5Datatype.CLASS_COMPOUND, tsize, torder, tsign)
		 Datatype[] memberDatatypes = {
		     new H5Datatype(Datatype.CLASS_INTEGER, Datatype.NATIVE, 
		                    Datatype.NATIVE, Datatype.NATIVE)
		     new H5Datatype(Datatype.CLASS_FLOAT, Datatype.NATIVE, 
		                    Datatype.NATIVE, Datatype.NATIVE));

		 int[] memberSizes = {1, 10};
		 Object data = null; // no initial data values
		 Dataset d = (H5File)file.createCompoundDS(name, pgroup, dims, null, 
		           chunks, gzip, memberNames, memberDatatypes, memberSizes, null);

	 */

	int fileID;


	private void fillinHDF5(String[] names, Matrix[] ms) {

		logger.info("Writing to HDF5 node: " + node);
		H5Group root=null;
		try {

			root= (H5Group) ((DefaultMutableTreeNode) hdf5File.getRootNode()).getUserObject();
		} catch (Exception ex) {
			logger.error(ex);
		}

		String[] fieldNames = new String[names.length+2];
		Datatype[] memberDataTypes = new H5Datatype[fieldNames.length];
		fieldNames[0] = "origin";
		memberDataTypes[0] = new H5Datatype(Datatype.CLASS_INTEGER,4,-1,-1);
		fieldNames[1] = "destination";
		memberDataTypes[1] = new H5Datatype(Datatype.CLASS_INTEGER, 4,-1,-1);
		System.arraycopy(names,0,fieldNames,2,names.length);
		for (int i=2;i<fieldNames.length;i++) {
			memberDataTypes[i] = new H5Datatype(Datatype.CLASS_FLOAT, 4, -1,-1);
		}

		int rows = ms[0].getRowCount()*ms[0].getColumnCount();
		int[] origins = new int[rows];
		int[] destinations = new int[rows];
		float[][] values = new float[names.length][rows];
		int row= 0;
		int[] externalRows = ms[0].getExternalRowNumbers();
		int[] externalCols = ms[0].getExternalColumnNumbers();
		for (int rowId=1;rowId<externalRows.length;rowId++) {
			int o = externalRows[rowId];
			for (int colId =1; colId < externalCols.length;colId++) {
				int d = externalCols[colId];
				origins[row] = o;
				destinations[row] = d;
				for (int mnum = 0; mnum < names.length; mnum++) {
					values[mnum][row] = ms[mnum].getValueAt(o, d);
				}
				row++;
			}
		}


		Vector data = new Vector();
		data.add(origins);
		data.add(destinations);
		for (float[] valueArray : values) {
			data.add(valueArray);
		}

		long[]  dims = new long[]{rows};
		int[] ranks = new int[]{1};
		int[] mDims = {10};
		Dataset dataset = null;
		try {
			dataset = hdf5File.createCompoundDS(
					node, // the name of the new dataset
					root, //- parent group where the new dataset is created.
					dims,  //the dimension size
					null, //- maximum dimension sizes of the new dataset, null if maxdims is the same as dims.
					null, // - chunk sizes of the new dataset, null if no chunking
					2, //gzip - GZIP compression level (1 to 9), 0 or negative values if no compression.
					fieldNames,// - the names of compound datatype
					memberDataTypes,// - the datatypes of the compound datatype
					//ranks,// - the ranks of the members
					null,// - the dim sizes of the members
					data// - list of data arrays written to the new dataset, null if no data is written to the new dataset.
			);
			/*
			 dset = f.createCompoundDS(
					 node, pgroup, DIMs, null, null, 9,
		             mnames, mdtypes, null, data);
			 */

			if (dataset != null){

				//data = (Vector) dataset.getData();
				logger.info("Finished writing node: "+ node);
			}
			else{
				String msg = "Data set is null";
				throw new HDF5Exception(msg);
			}
			//H5.H5Fflush(1, HDF5Constants.H5F_SCOPE_GLOBAL);
		} catch (Exception ex) {
			String msg = "Can't create HDF5 dataset";
			logger.error(ex.getMessage());
			throw new MatrixException(msg);

			//FIXME: Close f2 instance!!!

		}
	}

	/**
	 * Checks to make sure all the matrices have the same zone numbers in both rows and columns
	 * @param ms
	 * @throws MatrixException
	 */
	private void checkZoneConcurrancy(Matrix[] ms) throws MatrixException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.pb.common.matrix.MatrixWriter#writeMatrix(com.pb.common.matrix.Matrix)
	 */
	@Override
	public void writeMatrix(Matrix m) throws MatrixException {
		writeMatrix(m.getName(),m);
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.pb.common.matrix.MatrixWriter#writeMatrix(java.lang.String, com.pb.common.matrix.Matrix)
	 */
	@Override
	public void writeMatrix(String name, Matrix m) throws MatrixException {
		logger.warn("WriteMatrix in "+HDF5MatrixWriter.class.getName()+" will overwrite all matrices in HDF5 file node "+node);
		String[] names = new String[1];
		names[0] = name;
		Matrix[] ms = new Matrix[1];
		ms[0] = m;
	}

}
