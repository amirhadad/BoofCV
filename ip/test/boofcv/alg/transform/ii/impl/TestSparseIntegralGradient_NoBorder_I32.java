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

package boofcv.alg.transform.ii.impl;

import boofcv.alg.filter.derivative.GeneralSparseGradientTests;
import boofcv.alg.transform.ii.DerivativeIntegralImage;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.alg.transform.ii.IntegralKernel;
import boofcv.struct.deriv.GradientValue;
import boofcv.struct.image.ImageSInt32;
import org.junit.Test;


/**
 * @author Peter Abeles
 */
public class TestSparseIntegralGradient_NoBorder_I32 extends GeneralSparseGradientTests<ImageSInt32,ImageSInt32,GradientValue>
{
	final static int size = 5;
	SparseIntegralGradient_NoBorder_I32 alg;

	public TestSparseIntegralGradient_NoBorder_I32() {
		super(ImageSInt32.class, ImageSInt32.class,size/2+1);

		alg = new SparseIntegralGradient_NoBorder_I32(size/2);
	}

	@Test
	public void allStandard() {
		allTests(false);
	}

	@Override
	protected void imageGradient(ImageSInt32 input, ImageSInt32 derivX, ImageSInt32 derivY) {
		IntegralKernel kernelX = DerivativeIntegralImage.kernelDerivX(size);
		IntegralKernel kernelY = DerivativeIntegralImage.kernelDerivY(size);

		GIntegralImageOps.convolve(input,kernelX,derivX);
		GIntegralImageOps.convolve(input,kernelY,derivY);
	}

	@Override
	protected GradientValue sparseGradient(ImageSInt32 input, int x, int y) {
		alg.setImage(input);
		return alg.compute(x,y);
	}
}