/*
 Travel Model Microsimulation library
 Copyright (C) 2005 John Abraham jabraham@ucalgary.ca and others


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

package org.sandag.cvm.activityTravel;

import org.sandag.cvm.common.emme2.MatrixCacheReader;
import com.pb.common.matrix.Emme2MatrixReader;
import com.pb.common.matrix.MatrixReader;

/**
 * @author John Abraham
 *
 * A cool class created by John Abraham (c) 2003
 */
public interface AlternativeUsesMatrices extends CodedAlternative {
    
    void addCoefficient(String index1, String index2, String matrix, double coefficient) throws CoefficientFormatError;
    

	/**
	 * Method readMatrices.
	 * @param matrixCacheReader
	 */
	void readMatrices(MatrixCacheReader matrixCacheReader);

}
