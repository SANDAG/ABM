package org.sandag.cvm.common.skims;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;

import ncsa.hdf.object.CompoundDS;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.HObject;
import ncsa.hdf.object.h5.H5File;

import org.apache.log4j.Logger;

import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixException;
import com.pb.common.matrix.MatrixReader;


public class HDF5MatrixReader extends MatrixReader {

	public static void main(String args[]) {
		String[] skimsToGet = new String[] {"Light_Off","Light_AM","Light_Mid","Light_PM","Medium_Off",
				"Medium_AM","Medium_Mid","Medium_PM","Heavy_Off","Heavy_AM","Heavy_Mid","Heavy_PM"};
		String[] nodes = new String[] {"cvm"};
		HDF5MatrixReader r = new HDF5MatrixReader(new File("/ProjectWork/CSTDM2009 105073x4/Technical/Skims and OD/skims.h5"), nodes, skimsToGet);

//		r.testMatrixFile();
		r.readMatrices();
	}

	static Logger logger = Logger.getLogger(HDF5MatrixReader.class);

	File hdf5File;

	private List nodeNames;

	private String[] initialMatrixNames;

	public HDF5MatrixReader(File hdf5File, String[] nodeNames, String[] matrixNames) {
		this.nodeNames = Arrays.asList(nodeNames);
		this.hdf5File = hdf5File;
		this.initialMatrixNames = matrixNames;
	}
	
	public HDF5MatrixReader(File file, String node) {
		throw new RuntimeException("Not implemented yet");
		//FIXME implement read all skims from node
	}

	/**
	 * Builds the user to sequential lookup table and the sequential to user lookup table
	 * @param origins
	 * @param destinations
	 * @return [0] is sequentialToUser, to lookup user zone numbers, [1] is userToSequential to lookup index.
	 */
	private static int[][] buildCrossLookups(int[] origins, int[] destinations) {
		TreeSet<Integer> zoneSet = new TreeSet<Integer>();
		int mod = 0;
		for (int o : origins) {
			zoneSet.add(o);
			if (++mod % 250000 == 0) logger.info("   Processed line "+mod+" origin "+o);
		}
		mod = 0;
		for (int d : destinations) {
			zoneSet.add(d);
			if (++mod % 250000 == 0) logger.info("   Processed line "+mod+" destination "+d);
		}
		int maxZone = Collections.max(zoneSet);
		int[][] userSequentialCrossLookup= new int[2][];
		int[] sequentialToUserLookup = new int[zoneSet.size()];
		userSequentialCrossLookup[0] = sequentialToUserLookup;
		int[] userToSequentialLookup = new int[maxZone+1];
		userSequentialCrossLookup[1] = userToSequentialLookup;
		int z=0;
		for (int z1 : userToSequentialLookup) {
			userToSequentialLookup[z++] = -1;
		}
		z=0;
		for (int z2 : zoneSet) {
			sequentialToUserLookup[z] = z2;
			userToSequentialLookup[z2] = z++;
		}
		return userSequentialCrossLookup;
	}


    /**
     * Check if the dataset contains a column with the correct name
     * If it does return the index number, else return -1
     * Also if it does contain the column, select the column for retrieval
     * @param hdfDataset the dataset to be checked
     * @param name the name of the column to be checked and marked for retrieval
     * @return the index of the column, -1 if the column was not present
     */
    static int selectHDFFieldByName(CompoundDS hdfDataset, String name) {
    	List<String> nameList = Arrays.asList(hdfDataset.getMemberNames());
    	int index = nameList.indexOf(name);
    	if (index != -1) {
    		hdfDataset.selectMember(index);
    	}
    	return index;
    }

    
	static class intKeyString implements Comparable {
		int myInt;
		String myString;
		boolean found = false;
		
		intKeyString(int i, String s) {
			myInt = i;
			myString = s;
		}

		@Override
		public int compareTo(Object o) {
			int otherInt = ((intKeyString) o).myInt;
			if (otherInt > myInt) return -1;
			if (otherInt < myInt) return 1;
			if (otherInt == myInt) return 0;
			assert false;
			return 0;
		}
	}


	@Override
	public Matrix[] readMatrices() throws MatrixException {
		return readMatrices(initialMatrixNames);
	}
	
	public Matrix[] readMatrices(String[] matrixNames) {
		ArrayList<Matrix> matrixList = new ArrayList<Matrix>();
		FileFormat f = null;

		intKeyString[] skimIndices = new intKeyString[matrixNames.length];
		int j=0;
		for (String skimName : matrixNames) {
			skimIndices[j] = new intKeyString(-1, skimName);
			j++;
		}

		try {
			f = new H5File(hdf5File.getAbsolutePath(), H5File.READ);
			f.open();
			DefaultMutableTreeNode theRoot = (DefaultMutableTreeNode) f.getRootNode();
			if (theRoot == null) {
				String msg= "Null root in HDF5 skim file "+hdf5File;
				logger.fatal(msg);
				throw new RuntimeException(msg);
			}


			Enumeration local_enum = ((DefaultMutableTreeNode) theRoot).breadthFirstEnumeration();
			while (local_enum.hasMoreElements()) {
				DefaultMutableTreeNode theNode = (DefaultMutableTreeNode) local_enum.nextElement();
				HObject theObj = (HObject) theNode.getUserObject();
				String theName = theObj.getName();
				if (nodeNames.contains(theName)){
					logger.info("Found object \""+theName+"\" in HDF5File "+hdf5File+", reading skims");
					if (!(theObj instanceof CompoundDS)) {
						String msg = "object \""+theName+"\" in HDF5File "+hdf5File+" is not a compound dataset, can't read skims";
						logger.fatal(msg);
						throw new RuntimeException(msg);
					}
					theObj.getMetadata();
					CompoundDS skims = (CompoundDS) theObj;
					assert skims.getRank() ==1 : "Skim object in HDF5 file should only be of rank 1";
					skims.setMemberSelection(false);
					int originFieldIndex = selectHDFFieldByName(skims, "origin");
					assert originFieldIndex == 0 : "Origin needs to be first member in HDF skim dataset";
					int destinationFieldIndex = selectHDFFieldByName(skims, "destination");
					assert destinationFieldIndex == 1 : "Destination needs to be second member in HDF skim dataset";

					// get origin and destination zones, get all rows from file but no content yet.
					long[] selected = skims.getSelectedDims();
					logger.info("getting zone numbers from "+selected[0]+" rows of origins and destinations");

					List odNumbers = (List) skims.read();
					assert odNumbers.get(0) instanceof int[] : "Skim origins are not integer in HDF skim dataset";
					int[] origins = (int[]) odNumbers.get(0);
					assert odNumbers.get(1) instanceof int[] : "Skim destinations are not integer in HDF skim dataset";
					int[] destinations = (int[]) odNumbers.get(1);
					int[][] userSequentialCrossLookup = buildCrossLookups(
							origins, destinations);

					odNumbers = null; // forget it so we can collect the memory with garbage collection

					int count = 0;
					for (intKeyString identifier : skimIndices) {
						int index = selectHDFFieldByName(skims,identifier.myString);
						if (index >=0 ) {
							// found
							if (identifier.found) {
								String msg = "found "+identifier.myString+" in more than one node in skim file, not sure which one to use";
								logger.fatal(msg);
								throw new RuntimeException(msg);
							} else {
								logger.info("Reading "+identifier.myString+" from node "+theName);
								identifier.myInt = index;
								identifier.found = true;
								count ++;
							}
						} else {
							identifier.myInt = -1;
						}
					}
					Arrays.sort(skimIndices); // important to sort them so we get them in the correct order below, -1 should be first;
					
					if (count ==0) { 
						logger.warn("No relevant skims in node "+theName+" skipping");
					} else {
	
						// allocate the storage for the arrays based on the number of skims and the number of zones
						float[][][] matrixArrays = new float[count][userSequentialCrossLookup[0].length][userSequentialCrossLookup[0].length];
	
						// now get content 100 rows at a time
						final long HOWMANY = 250000;
						long[] start = skims.getStartDims();
						selected[0] = HOWMANY;
						long size = skims.getDims()[0];
						boolean verbose = false;
						int startingCol = 0;
						while (skimIndices[startingCol].myInt<0) startingCol++;
						assert skimIndices.length-startingCol == count;
						
						for (long beginAt = 0; beginAt <= size; beginAt += HOWMANY) {
							logger.info("Processing line "+beginAt+" from skims");
							start[0] = beginAt;
							if (beginAt+HOWMANY >= size) // should be >=?
							{
								//verbose = true;
								selected[0] = size - beginAt;
							}
							List skimData = (List) skims.read();
							assert skimData.size() == count+2;
							assert skimData.get(0) instanceof int[] : "Skim origins are not integer in HDF skim dataset";
							origins = (int[]) skimData.get(0);
							assert skimData.get(1) instanceof int[] : "Skim destinations are not integer in HDF skim dataset";
							destinations = (int[]) skimData.get(1);
							for (int r = 0; r < origins.length; r++ ) {
								int originArrayIndex = userSequentialCrossLookup[1][origins[r]];
								int destinationArrayIndex = userSequentialCrossLookup[1][destinations[r]];
								if (verbose) System.out.println("processing origin "+origins[r]+","+destinations[r]+" (index "+originArrayIndex+","+destinationArrayIndex);
								for (int col = 0; col + 2 < skimData.size(); col ++) {
									matrixArrays[col][originArrayIndex][destinationArrayIndex] = ((float[]) skimData.get(col+2))[r];
								}
							}
							verbose = false;
	
						}
	
	
						int[] externalZoneNumbers = new int[userSequentialCrossLookup[0].length+1];
						for(int k=1;k<externalZoneNumbers.length;k++){
							externalZoneNumbers[k]=userSequentialCrossLookup[0][k-1];
						}
						
	
						for (int matrixToBeAdded =0; matrixToBeAdded<count; matrixToBeAdded++) {
							Matrix m = new Matrix(skimIndices[matrixToBeAdded+startingCol].myString,"",matrixArrays[matrixToBeAdded]);
							m.setExternalNumbers(externalZoneNumbers);
							matrixList.add(m);
						}
					}
				}


			}
		} catch (AssertionError e) {
			logger.fatal("Assertion Error in HDF5 Skims", e);
			throw new MatrixException(e);
		} catch (OutOfMemoryError e) {
			logger.fatal("Out of memory reading HDF5 Skims", e);
			throw new MatrixException(e);
		} catch (Exception e) {
			logger.fatal("Exception in reading HDF5 Skims", e);
			throw new MatrixException(e);
		} finally {
			try {
				f.close();
			} catch (Exception e) {
				logger.warn("Can't close HDF5 file");
			}
		}
		
		for (intKeyString s : skimIndices) {
			if (!s.found) {
				logger.fatal("Couldn't read skim \""+s.myString+"\" from skim file");
				throw new MatrixException("Coulnd't read skim "+s.myString+" from skim file");
			}
		}

		logger.info("Finished reading HDF5 skims "+hdf5File+" into memory");
		Matrix[] returnMatrix = new Matrix[matrixList.size()];
		return (Matrix[]) matrixList.toArray(returnMatrix);
	}

	public void testMatrixFile() throws MatrixException {
		FileFormat f = null;
		try {
			f = new H5File(hdf5File.getAbsolutePath(), H5File.READ);
			f.open();
			DefaultMutableTreeNode theRoot = (DefaultMutableTreeNode) f.getRootNode();

			if (theRoot == null)
				throw new RuntimeException("Root is null in HDF5 file");

			// go through all items
			Enumeration local_enum = ((DefaultMutableTreeNode) theRoot).breadthFirstEnumeration();

			while (local_enum.hasMoreElements()) {
				DefaultMutableTreeNode theNode = (DefaultMutableTreeNode) local_enum.nextElement();
				HObject theObj = (HObject) theNode.getUserObject();
				logger.info("theObj is "+theObj.getFullName()+" of type "+theObj.getClass());
				List l  = theObj.getMetadata();
				StringBuffer str = new StringBuffer("  metadata:");
				for (Object j : l) {
					str.append(j+" ");
				}
				logger.info(str);
				
				if (theObj instanceof CompoundDS) {
					CompoundDS skims = (CompoundDS) theObj;
					String[] members = skims.getMemberNames();
					logger.info("members:");
					for (String m : members) logger.info("   "+m);
					
					skims.setMemberSelection(true); // get all columns
					
					 int rank = skims.getRank(); // number of dimension of the dataset
					 long[] dims = skims.getDims(); // the dimension sizes of the dataset
					 long[] selected = skims.getSelectedDims(); // the selected size of the dataet
					 long[] start = skims.getStartDims(); // the off set of the selection
					 long[] stride = skims.getStride(); // the stride of the dataset
					 int[] selectedIndex = skims.getSelectedIndex(); // the selected dimensions for display

					 selected[0] = 20; // only get 20 rows
					 
					 List dataFromHDF5 = (List) skims.read();
					 int i =0;
					 for (Object data : dataFromHDF5) {
						 if (data instanceof int[]) {
							 int[] intarray = (int[]) data;
							 StringBuffer str2 = new StringBuffer(skims.getMemberNames()[i]);
							 i++;
							 for (int j : intarray) {
								 str2.append(j+",");
							 }
							 logger.info(str2);
						 } else {
							 float[] darray = (float[]) data;
							 StringBuffer str2 = new StringBuffer(skims.getMemberNames()[i]);
							 i++;
							 for (double j : darray) {
								 str2.append(j+",");
							 }
							 logger.info(str2);
						 }
					 }
				}
				
			}
			f.close();

		} catch (Exception e) {
			// FIXME Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public Matrix readMatrix(String name) throws MatrixException {
		String[] names = new String[1];
		names[0] = name;
		Matrix[] matrices = readMatrices(names);
		assert matrices.length == 1;
		return matrices[0];
	}

	@Override
	public Matrix readMatrix() throws MatrixException {
		String msg = "Can't read a matrix without specifying a name";
		logger.fatal(msg);
		throw new RuntimeException(msg);
	}

}
