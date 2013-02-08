/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.abst.calib;

import boofcv.struct.Configuration;

/**
 * Calibration parameters for chessboard style calibration grid.
 *
 * @see boofcv.alg.feature.detect.chess.DetectChessCalibrationPoints
 *
 * @author Peter Abeles
 */
public class ConfigChessboard implements Configuration {
	/**
	 * Number of columns in square block grid.  Target dependent
	 */
	public int numCols = -1;
	/**
	 * Number of rows in square block grid.  Target dependent.
	 */
	public int numRows = -1;
	/**
	 *  Size of interest point detection region.  Typically 5
	 */
	public int nonmaxRadius = 5;

	/**
	 *  Increases or decreases the minimum allowed blob size. Try 1.0
	 */
	public double relativeSizeThreshold = 1;

	/**
	 * Threshold used to compute binary image.  If < 0 then the mean image intensity is used.
	 */
	public double binaryThreshold = -1;

	public ConfigChessboard(int numCols, int numRows) {
		this.numCols = numCols;
		this.numRows = numRows;
	}

	public ConfigChessboard(int numCols, int numRows, int nonmaxRadius,
							double relativeSizeThreshold, double binaryThreshold) {
		this.numCols = numCols;
		this.numRows = numRows;
		this.nonmaxRadius = nonmaxRadius;
		this.relativeSizeThreshold = relativeSizeThreshold;
		this.binaryThreshold = binaryThreshold;
	}

	@Override
	public void checkValidity() {
		if( numCols <= 0 || numRows <= 0 )
			throw new IllegalArgumentException("Must specify then number of rows and columns in the target");
	}
}
