/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.detect.corner.impl;

import gecv.alg.detect.corner.GradientCornerIntensity;
import gecv.struct.image.ImageFloat32;


/**
 * <p>
 * Several corner detector algorithms work by computing a symmetric matrix whose elements are composed of the convolution
 * of the image's gradient squared.  This is done for X*X, X*Y, and X*X.  Once the matrix has been constructed
 * it is used to estimate how corner like the pixel under consideration is.  This class provides a generalized
 * interface for performing these calculations in an optimized manor.
 * </p>
 * 
 * <p>
 * NOTE: Image borders are not processed.  The zeros in the image border need to be taken in account when
 * extract features using algorithms such as non-max suppression.
 * </p>
 * 
 * <p>
 * DO NOT MODIFY.  Code has been automatically generated by {@link GenerateSsdCorner}.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class SsdCorner_F32 implements GradientCornerIntensity<ImageFloat32> {

	// input image gradient
	protected ImageFloat32 derivX;
	protected ImageFloat32 derivY;

	// radius of detected features
	protected int radius;

	// temporary storage for intensity derivatives summations
	private ImageFloat32 horizXX = new ImageFloat32(1,1);
	private ImageFloat32 horizXY = new ImageFloat32(1,1);
	private ImageFloat32 horizYY = new ImageFloat32(1,1);

	// temporary storage for convolution along in the vertical axis.
	private float tempXX[] = new float[1];
	private float tempXY[] = new float[1];
	private float tempYY[] = new float[1];

	// the intensity of the found features in the image
	private ImageFloat32 featureIntensity = new ImageFloat32(1,1);

	// defines the A matrix, from which the eignevalues are computed
	protected float totalXX, totalYY, totalXY;

	// used to keep track of where it is in the image
	protected int x, y;

	public SsdCorner_F32( int windowRadius) {
		this.radius = windowRadius;
	}

	public void setImageShape( int imageWidth, int imageHeight ) {
		horizXX.reshape(imageWidth,imageHeight);
		horizYY.reshape(imageWidth,imageHeight);
		horizXY.reshape(imageWidth,imageHeight);

		featureIntensity.reshape(imageWidth,imageHeight);

		if( tempXX.length < imageWidth ) {
			tempXX = new float[imageWidth];
			tempXY = new float[imageWidth];
			tempYY = new float[imageWidth];
		}
	}

	@Override
	public int getRadius() {
		return radius;
	}

	@Override
	public ImageFloat32 getIntensity() {
		return featureIntensity;
	}
	
	/**
	 * Computes the pixel's corner intensity.
	 * @return corner intensity.
	 */
	protected abstract float computeIntensity();

	@Override
	public void process(ImageFloat32 derivX, ImageFloat32 derivY) {
		if( tempXX == null ) {
			if (derivX.getWidth() != derivY.getWidth() || derivX.getHeight() != derivY.getHeight()) {
				throw new IllegalArgumentException("Input image sizes do not match");
			}
			setImageShape(derivX.getWidth(),derivX.getHeight());
		} else if (derivX.getWidth() != horizXX.getWidth() || derivX.getHeight() != horizXX.getHeight()) {
			setImageShape(derivX.getWidth(),derivX.getHeight());
		}
		this.derivX = derivX;
		this.derivY = derivY;

		horizontal();
		vertical();
	}

/**
	 * Compute the derivative sum along the x-axis while taking advantage of duplicate
	 * calculations for each window.
	 */
	private void horizontal() {
		float[] dataX = derivX.data;
		float[] dataY = derivY.data;

		float[] hXX = horizXX.data;
		float[] hXY = horizXY.data;
		float[] hYY = horizYY.data;

		final int imgHeight = derivX.getHeight();
		final int imgWidth = derivX.getWidth();

		int windowWidth = radius * 2 + 1;

		int radp1 = radius + 1;

		for (int row = 0; row < imgHeight; row++) {

			int pix = row * imgWidth;
			int end = pix + windowWidth;

			float totalXX = 0;
			float totalXY = 0;
			float totalYY = 0;

			int indexX = derivX.startIndex + row * derivX.stride;
			int indexY = derivY.startIndex + row * derivY.stride;

			for (; pix < end; pix++) {
				float dx = dataX[indexX++];
				float dy = dataY[indexY++];

				totalXX += dx * dx;
				totalXY += dx * dy;
				totalYY += dy * dy;
			}

			hXX[pix - radp1] = totalXX;
			hXY[pix - radp1] = totalXY;
			hYY[pix - radp1] = totalYY;

			end = row * imgWidth + imgWidth;
			for (; pix < end; pix++, indexX++, indexY++) {

				float dx = dataX[indexX - windowWidth];
				float dy = dataY[indexY - windowWidth];

				// saving these multiplications in an array to avoid recalculating them made
				// the algorithm about 50% slower
				totalXX -= dx * dx;
				totalXY -= dx * dy;
				totalYY -= dy * dy;

				dx = dataX[indexX];
				dy = dataY[indexY];

				totalXX += dx * dx;
				totalXY += dx * dy;
				totalYY += dy * dy;

				hXX[pix - radius] = totalXX;
				hXY[pix - radius] = totalXY;
				hYY[pix - radius] = totalYY;
			}
		}
	}

	/**
	 * Compute the derivative sum along the y-axis while taking advantage of duplicate
	 * calculations for each window and avoiding cache misses. Then compute the eigen values
	 */
	private void vertical() {
		float[] hXX = horizXX.data;
		float[] hXY = horizXY.data;
		float[] hYY = horizYY.data;
		final float[] inten = featureIntensity.data;

		final int imgHeight = horizXX.getHeight();
		final int imgWidth = horizXX.getWidth();

		final int kernelWidth = radius * 2 + 1;

		final int startX = radius;
		final int endX = imgWidth - radius;

		final int backStep = kernelWidth * imgWidth;

		for (x = startX; x < endX; x++) {
			int srcIndex = x;
			int destIndex = imgWidth * radius + x;
			totalXX = totalXY = totalYY = 0;

			int indexEnd = srcIndex + imgWidth * kernelWidth;
			for (; srcIndex < indexEnd; srcIndex += imgWidth) {
				totalXX += hXX[srcIndex];
				totalXY += hXY[srcIndex];
				totalYY += hYY[srcIndex];
			}

			tempXX[x] = totalXX;
			tempXY[x] = totalXY;
			tempYY[x] = totalYY;

			y = radius;
			// compute the eigen values
			inten[destIndex] = computeIntensity();
			destIndex += imgWidth;
			y++;
		}

		// change the order it is processed in to reduce cache misses
		for (y = radius + 1; y < imgHeight - radius; y++) {
			int srcIndex = (y + radius) * imgWidth + startX;
			int destIndex = y * imgWidth + startX;

			for (x = startX; x < endX; x++, srcIndex++, destIndex++) {
				totalXX = tempXX[x] - hXX[srcIndex - backStep];
				tempXX[x] = totalXX += hXX[srcIndex];
				totalXY = tempXY[x] - hXY[srcIndex - backStep];
				tempXY[x] = totalXY += hXY[srcIndex];
				totalYY = tempYY[x] - hYY[srcIndex - backStep];
				tempYY[x] = totalYY += hYY[srcIndex];

				inten[destIndex] = computeIntensity();
			}
		}
	}
}
