/*
 * Copyright  2005 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.sandag.cvm.common.skims;

import org.sandag.cvm.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.ZipMatrixReader;

import drasys.or.util.Array;

import ncsa.hdf.object.h5.H5CompoundDS;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.HObject;
import ncsa.hdf.object.h5.H5File;

import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * A class that reads in peak auto skims and facilitates zone pair disutility calculations
 * @author John Abraham, Joel Freedman
 * 
 */
public class SomeSkims implements TravelAttributesInterface {
    protected static Logger logger = Logger.getLogger("com.pb.models.pecas");

    private ArrayList matrixList = new ArrayList();
    public Matrix[] matrices = new Matrix[0];
    private ArrayList matrixNameList = new ArrayList();

    String my1stPath;
    String my2ndPath;


    public SomeSkims() {
        my1stPath = System.getProperty("user.dir");
    }

    public SomeSkims(String firstPath, String secondPath) {
        my1stPath = firstPath;
        my2ndPath =secondPath;
    };
    
    public Matrix getMatrix(String name) {
        int place = matrixNameList.indexOf(name);
        if (place >=0) return matrices[place];
        return null;
    }

    public void addZipMatrix(String matrixName) {
        if (matrixNameList.contains(matrixName)) {
            logger.info("SomeSkims already contains matrix named "+matrixName+", not reading it in again");
        } else {
            File skim = new File(my1stPath+matrixName+".zip");
            if (!skim.exists()) skim = new File(my1stPath+matrixName+".zipMatrix");
            if(!skim.exists()) skim = new File(my1stPath+matrixName+".zmx");
            if(!skim.exists()){
                skim = new File(my2ndPath+matrixName+".zip");
                if(!skim.exists()) skim = new File(my2ndPath+matrixName+".zipMatrix");
                if(!skim.exists()) skim = new File(my2ndPath+matrixName+".zmx");
                if (!skim.exists()) {
                    logger.fatal("Could not find "+ matrixName+".zip, .zipMatrix or .zmx in either directory");
                    throw new RuntimeException("Could not find "+ matrixName+".zip, .zipMatrix or .zmx in either directory");
                }
            }
            matrixList.add(new ZipMatrixReader(skim).readMatrix());
            matrixNameList.add(matrixName);
            matrices = (Matrix[]) matrixList.toArray(matrices);
        }
        
        if(logger.isDebugEnabled()) logger.debug("finished reading zipmatrix of skims "+matrixName+" into memory");
    }
    
    public void addTableDataSetSkims(TableDataSet s, String[] fieldsToAdd, int maxZoneNumber) {
        addTableDataSetSkims(s, fieldsToAdd,  maxZoneNumber, "origin", "destination");

    }
    
    public void addMatrixFromFile(String fileName, String matrixName) {
        File f = new File(fileName);
        MatrixType type = MatrixReader.determineMatrixType(f);
        if (type == null) {
            logger.error("Can't determine matrix type for "+fileName);
        } else {
            MatrixReader r = MatrixReader.createReader(type,f);
            Matrix m = r.readMatrix();
            matrixNameList.add(matrixName);
            matrixList.add(m);
            m.setName(matrixName);
            matrices = (Matrix[]) matrixList.toArray(matrices);
        }
        
    }
    
    public void addMatrix(Matrix m, String name) {
        matrixNameList.add(name);
        matrixList.add(m);
        m.setName(name);
        matrices = (Matrix[]) matrixList.toArray(matrices);
    }
    
    public void addMatrixCSVSkims(TableDataSet s, String name) {
        int rows = s.getRowCount();
        int columns = s.getColumnCount()-1;
        if (rows!=columns) {
            logger.fatal("Trying to add CSV Matrix Skims and number of columns does not equal number of rows");
            throw new RuntimeException("Trying to add CSV Matrix Skims and number of columns does not equal number of rows");
        }
        float[][] tempArray = new float[rows][columns];
        int[] userToSequentialLookup = new int[rows+1];
        // check order of rows and columns 
        for (int check = 1;check < s.getRowCount();check++) {
            if (!(s.getColumnLabel(check+1).equals(String.valueOf((int) (s.getValueAt(check,1)))))) {
                logger.fatal("CSVMatrixSkims have columns out of order (needs to be the same as rows)");
                throw new RuntimeException("CSVMatrixSkims have columns out of order (needs to be the same as rows)");
            }
        }
        // TODO check for missing skims when using CSV format
        for (int tdsRow = 1;tdsRow <= s.getRowCount();tdsRow++) {
            userToSequentialLookup[tdsRow]=(int) s.getValueAt(tdsRow,1);
            for (int tdsCol=2;tdsCol<=s.getColumnCount();tdsCol++) {
                tempArray[tdsRow-1][tdsCol-2]=s.getValueAt(tdsRow,tdsCol);
            }
        }
        Matrix m = new Matrix(name,"",tempArray);
        matrixNameList.add(name);
        m.setExternalNumbers(userToSequentialLookup);
        this.matrixList.add(m);
        matrices = (Matrix[]) matrixList.toArray(matrices);
    }

    /** Adds a table data set of skims into the set of skims that are available 
     * 
     * @param s the table dataset of skims.  There must be a column called "origin"
     * and another column called "destination"
     * @param fieldsToAdd the names of the fields from which to create matrices from, all other fields
     * will be ignored.
     */
    public void addTableDataSetSkims(TableDataSet s, String[] fieldsToAdd, int maxZoneNumber, String originFieldName, String destinationFieldName) {
        int originField = s.checkColumnPosition(originFieldName);
        int destinationField = s.checkColumnPosition(destinationFieldName);
        int[] userToSequentialLookup = new int[maxZoneNumber];
        int[] sequentialToUserLookup = new int[maxZoneNumber];
        for (int i =0; i<userToSequentialLookup.length;i++) {
                    userToSequentialLookup[i] = -1;
        }
        int[] origins = s.getColumnAsInt(originField);
        int zonesFound = 0;
        for (int o = 0;o<origins.length;o++) {
            int sequentialOrigin = userToSequentialLookup[origins[o]];
            if (sequentialOrigin == -1) {
                sequentialOrigin = zonesFound;
                zonesFound++;
                userToSequentialLookup[origins[o]]=sequentialOrigin;
                sequentialToUserLookup[sequentialOrigin] = origins[o];
            }
        }
        int[] externalZoneNumbers = new int[zonesFound+1];
        for(int i=1;i<externalZoneNumbers.length;i++){
            externalZoneNumbers[i]=sequentialToUserLookup[i-1];
        }
        //enable garbage collection
        origins = null;
        
        int[] fieldIds = new int[fieldsToAdd.length];
        for (int mNum=0; mNum < fieldsToAdd.length; mNum++) {
            if (matrixNameList.contains(fieldsToAdd[mNum])) {
                fieldIds[mNum] = -1;
                logger.warn("SomeSkims already contains matrix named "+fieldsToAdd[mNum]+", not reading it in again");
            } else {
                fieldIds[mNum] = s.getColumnPosition(fieldsToAdd[mNum]);
                if (fieldIds[mNum]<=0) {
                    logger.fatal("No field named "+fieldsToAdd[mNum]+ " in skim TableDataSet "+s);
                }
            }
        }
        float [][][] matrixArrays = new float[fieldsToAdd.length][zonesFound][zonesFound];
        for (int row = 1;row <= s.getRowCount();row++) {
            int origin = (int) s.getValueAt(row,originField);
            int destination = (int) s.getValueAt(row,destinationField);
            for (int entry = 0;entry<fieldIds.length;entry++) {
                if (fieldIds[entry]>0) {
                    matrixArrays[entry][userToSequentialLookup[origin]][userToSequentialLookup[destination]] = s.getValueAt(row,fieldIds[entry]);
                }
            }
        }
        
        for (int matrixToBeAdded =0; matrixToBeAdded < fieldsToAdd.length; matrixToBeAdded++) {
            if (fieldIds[matrixToBeAdded]>0) {
                matrixNameList.add(fieldsToAdd[matrixToBeAdded]);
                Matrix m = new Matrix(fieldsToAdd[matrixToBeAdded],"",matrixArrays[matrixToBeAdded]);
                m.setExternalNumbers(externalZoneNumbers);
                this.matrixList.add(m);
            }
        }
        
        matrices = (Matrix[]) matrixList.toArray(matrices);
        
        logger.info("Finished reading TableDataSet skims "+s+" into memory");
    }
    

    static int selectHDFFieldByName(H5CompoundDS hdfDataset, String name) {
    	List<String> nameList = Arrays.asList(hdfDataset.getMemberNames());
    	int index = nameList.indexOf(name);
    	if (index == -1) {
    		String msg = "No field of name "+name+" in HDF5 file node";
    		logger.fatal(msg);
    		throw new RuntimeException(msg);
    	}
    	hdfDataset.selectMember(index);
    	return index;
    }

    
	static class intKeyString implements Comparable {
		int myInt;
		String myString;
		
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

	/**
	 * Adds some skims from an HDF5 file of skims into the set of skims that are available 
	 * @param hdf5File
	 * @param nodeName
	 * @param fieldsToAdd
	 * @param maxZoneNumber
	 * @param originFieldName
	 * @param destinationFieldName
	 */
	public void addHDF5Skims(File hdf5File, String nodeName, String[] fieldsToAdd, int maxZoneNumber, String originFieldName, String destinationFieldName) {
		logger.error("HDF5 Skims have not been tested yet, test SomeSkims.addHDF5Skims before using it");
		FileFormat f = null;
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
				if (theName.equals(nodeName)){
					logger.info("Found object \""+theName+"\" in HDF5File "+hdf5File+", reading skims");
					if (!(theObj instanceof H5CompoundDS)) {
						String msg = "object \""+theName+"\" in HDF5File "+hdf5File+" is not a compound dataset, can't read skims";
						logger.fatal(msg);
						throw new RuntimeException(msg);
					}
					H5CompoundDS skims = (H5CompoundDS) theObj;
					assert skims.getRank() ==1 : "Skim object in HDF5 file should only be of rank 1";
					skims.setMemberSelection(false);
					int originFieldIndex = selectHDFFieldByName(skims, originFieldName);
					assert originFieldIndex == 0 : "Origin needs to be first member in HDF skim dataset";
					int destinationFieldIndex = selectHDFFieldByName(skims, destinationFieldName);
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

					intKeyString[] skimIndices = new intKeyString[fieldsToAdd.length];
					int i = 0;
					for (String skimName : fieldsToAdd) {
						skimIndices[i] = new intKeyString(selectHDFFieldByName(skims,skimName), skimName);
						i++;
					}
					Arrays.sort(skimIndices); // important to sort them so we get them in the correct order below.

					float[][][] matrixArrays = new float[skimIndices.length][userSequentialCrossLookup[0].length][userSequentialCrossLookup[0].length];

					// now get content 100 rows at a time
					final long HOWMANY = 100;
					long[] start = skims.getStartDims();
					selected[0] = HOWMANY;
					long size = skims.getDims()[0];
					for (long beginAt = 0; beginAt <= size; beginAt += HOWMANY) {
						if (beginAt+HOWMANY >= size) // should be >=?
						{
							selected[0] = size - beginAt;
						}
						List skimData = (List) skims.read();
						assert skimData.get(0) instanceof int[] : "Skim origins are not integer in HDF skim dataset";
						origins = (int[]) skimData.get(0);
						assert skimData.get(1) instanceof int[] : "Skim destinations are not integer in HDF skim dataset";
						destinations = (int[]) skimData.get(1);
						for (int r = 0; r < origins.length; r++ ) {
							int originArrayIndex = userSequentialCrossLookup[0][origins[r]];
							int destinationArrayIndex = userSequentialCrossLookup[0][destinations[r]];
							for (int col = 0; col + 2 < skimData.size(); col ++) {
								matrixArrays[col][originArrayIndex][destinationArrayIndex] = ((float[]) skimData.get(col+2))[r];
							}
						}

					}


					int[] externalZoneNumbers = new int[userSequentialCrossLookup[0].length+1];
					for(int k=1;k<externalZoneNumbers.length;k++){
						externalZoneNumbers[i]=userSequentialCrossLookup[0][i-1];
					}

					for (int matrixToBeAdded =0; matrixToBeAdded<skimIndices.length; matrixToBeAdded++) {
						matrixNameList.add(skimIndices[matrixToBeAdded].myString);
						Matrix m = new Matrix(skimIndices[matrixToBeAdded].myString,"",matrixArrays[matrixToBeAdded]);
						m.setExternalNumbers(userSequentialCrossLookup[0]);
						this.matrixList.add(m);
					}
				}


			}
		} catch (AssertionError e) {
			logger.fatal("Assertion Error in HDF5 Skims", e);
			throw new RuntimeException(e);
		} catch (OutOfMemoryError e) {
			logger.fatal("Out of memory reading HDF5 Skims", e);
			throw new RuntimeException(e);
		} catch (Exception e) {
			logger.fatal("Exception in reading HDF5 Skims", e);
			throw new RuntimeException(e);
		}

		matrices = (Matrix[]) matrixList.toArray(matrices);

		logger.info("Finished reading HDF5 skims "+hdf5File+" into memory");
	}

	/**
	 * Builds the user to sequential lookup table and the sequential to user lookup table
	 * @param origins
	 * @param destinations
	 * @return [0] is sequentialToUser, to lookup user zone numbers, [1] is userToSequential to lookup index.
	 */
	private static int[][] buildCrossLookups(int[] origins, int[] destinations) {
		TreeSet<Integer> zoneSet = new TreeSet<Integer>();
		for (int o : origins) {
			zoneSet.add(o);
		}
		for (int d : destinations) {
			zoneSet.add(d);
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


    public int getMatrixId(String string) {
        
        return matrixNameList.indexOf(string);
    }


    /**
     * @param my1stPath The my1stPath to set.
     */
    public void setMy1stPath(String my1stPath) {
        this.my1stPath = my1stPath;
    }

    /**
     * @param my2ndPath The my2ndPath to set.
     */
    public void setMy2ndPath(String my2ndPath) {
        this.my2ndPath = my2ndPath;
    }

};
