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

/**
 * @author John Abraham
 *
 * A cool class created by John Abraham (c) 2003
 */
public class CoefficientFormatError extends Exception {

	/**
	 * Constructor for CoefficientFormatError.
	 */
	public CoefficientFormatError() {
		super();
	}

	/**
	 * Constructor for CoefficientFormatError.
	 * @param message
	 */
	public CoefficientFormatError(String message) {
		super(message);
	}

	/**
	 * Constructor for CoefficientFormatError.
	 * @param message
	 * @param cause
	 */
	public CoefficientFormatError(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructor for CoefficientFormatError.
	 * @param cause
	 */
	public CoefficientFormatError(Throwable cause) {
		super(cause);
	}

}
