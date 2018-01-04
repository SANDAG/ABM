/*
 Travel Model Microsimulation library
 Copyright (C) 2005 PbConsult, JE Abraham and others


  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

*/


package org.sandag.cvm.common.emme2;

import java.io.File;
import java.util.*;

import com.pb.common.matrix.*;

/**
 * @author John Abraham
 *
 * A cool class created by John Abraham (c) 2003
 */
public class Emme2MatrixHashtableReader extends Emme2MatrixReader {

    private Hashtable readMatrices = new Hashtable();

	/** Reads an entire matrix from an Emme2 databank, but if it's already been
     * read once before returns the previously read one  hashtable
     *
     * @param index the short name of the matrix, eg. "mf10"
     * @return a complete matrix
     * @throws MatrixException
     */
    public Matrix readMatrix(String index) throws MatrixException {
        
        if (readMatrices.containsKey(index)) {
            return (Matrix) readMatrices.get(index);
        }
        Matrix m = super.readMatrix(index );
        readMatrices.put(index,m);
        return m;
        
    }



	/**
	 * Constructor for Emme2MatrixHashtableReader.
	 * @param file
	 */
	public Emme2MatrixHashtableReader(File file) {
		super(file);
	}

}
