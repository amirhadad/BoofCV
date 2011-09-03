/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.orientation.impl;

import boofcv.alg.feature.orientation.OrientationSlidingWindow;
import boofcv.struct.image.ImageFloat32;
import georegression.metric.UtilAngle;


/**
 * <p>
 * Implementation of {@link OrientationSlidingWindow} for a specific image type.
 * </p>
 *
 * <p>
 * WARNING: Do not modify.  Automatically generated by {@link GenerateImplOrientationSlidingWindow}.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImplOrientationSlidingWindow_F32 extends OrientationSlidingWindow<ImageFloat32> {

	public ImplOrientationSlidingWindow_F32(int numAngles, double windowSize, boolean isWeighted) {
		super(numAngles, windowSize, isWeighted);
	}

	@Override
	public Class<ImageFloat32> getImageType() {
		return ImageFloat32.class;
	}

	private void computeAngles() {
		int i = 0;
		for( int y = rect.y0; y < rect.y1; y++ ) {
			int indexX = derivX.startIndex + derivX.stride*y + rect.x0;
			int indexY = derivY.startIndex + derivY.stride*y + rect.x0;

			for( int x = rect.x0; x < rect.x1; x++ , indexX++ , indexY++ ) {
				float dx = derivX.data[indexX];
				float dy = derivY.data[indexY];

				angles[i++] = Math.atan2(dy,dx);
			}
		}
	}

	@Override
	protected double computeOrientation() {
		computeAngles();

		double windowRadius = windowSize/2.0;
		int w = rect.x1-rect.x0;
		double bestScore = -1;
		double bestAngle = 0;
		double stepAngle = Math.PI*2.0/numAngles;
		int N = w*(rect.y1-rect.y0);
		for( double angle = -Math.PI; angle < Math.PI; angle += stepAngle ) {
			double dx = 0;
			double dy = 0;
			for( int i = 0; i < N; i++ ) {
				double diff = UtilAngle.dist(angle,angles[i]);
				if( diff <= windowRadius) {
					int x = rect.x0 + i % w;
					int y = rect.y0 + i / w;
					dx += derivX.get(x,y);
					dy += derivY.get(x,y);
				}
			}
			double n = dx*dx + dy*dy;
			if( n > bestScore) {
				bestAngle = Math.atan2(dy,dx);
				bestScore = n;
			}
		}

		return bestAngle;
	}

	@Override
	protected double computeWeightedOrientation(int c_x, int c_y) {
		double windowRadius = windowSize/2.0;
		int w = rect.x1-rect.x0;
		double bestScore = -1;
		double bestAngle = 0;
		double stepAngle = Math.PI*2.0/numAngles;
		int N = w*(rect.y1-rect.y0);

		for( double angle = -Math.PI; angle < Math.PI; angle += stepAngle ) {
			double dx = 0;
			double dy = 0;
			for( int i = 0; i < N; i++ ) {
				double diff = UtilAngle.dist(angle,angles[i]);
				if( diff <= windowRadius) {
					int localX = i%w;
					int localY = i/w;
					double ww = weights.get(localX,localY);
					int x = rect.x0 + i % w;
					int y = rect.y0 + i / w;
					dx += ww*derivX.get(x,y);
					dy += ww*derivY.get(x,y);
				}
			}
			double n = dx*dx + dy*dy;
			if( n > bestScore) {
				bestAngle = Math.atan2(dy,dx);
				bestScore = n;
			}
		}

		return bestAngle;
	}

}
