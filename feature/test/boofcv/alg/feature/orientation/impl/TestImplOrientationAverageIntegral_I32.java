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

package boofcv.alg.feature.orientation.impl;

import boofcv.alg.feature.orientation.GenericOrientationIntegralTests;
import boofcv.alg.feature.orientation.OrientationAverageIntegral;
import boofcv.struct.image.ImageSInt32;
import org.junit.Test;


/**
 * @author Peter Abeles
 */
public class TestImplOrientationAverageIntegral_I32 {
	double angleTol = 0.015;
	int r = 3;

	@Test
	public void standardUnweighted() {
		GenericOrientationIntegralTests<ImageSInt32> tests = new GenericOrientationIntegralTests<ImageSInt32>();

		OrientationAverageIntegral<ImageSInt32> alg = new ImplOrientationAverageIntegral_I32(r,false);

		tests.setup(angleTol, r*2+1 , alg,ImageSInt32.class);
		tests.performAll();
	}

	@Test
	public void standardWeighted() {
		GenericOrientationIntegralTests<ImageSInt32> tests = new GenericOrientationIntegralTests<ImageSInt32>();

		OrientationAverageIntegral<ImageSInt32> alg = new ImplOrientationAverageIntegral_I32(r,true);

		tests.setup(angleTol, r*2+1 ,alg,ImageSInt32.class);
		tests.performAll();
	}
}