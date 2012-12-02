/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detdesc;

import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.image.ImageFloat32;

/**
 * @author Peter Abeles
 */
public class TestWrapDetectDescribeSurf extends GenericTestsDetectDescribePoint<ImageFloat32,SurfFeature>
{

	public TestWrapDetectDescribeSurf() {
		super(true, true, ImageFloat32.class, SurfFeature.class);
	}

	@Override
	public DetectDescribePoint<ImageFloat32, SurfFeature> createDetDesc() {
		return FactoryDetectDescribe.surf(0, 1, -1, 1, 9, 4, 4, true, ImageFloat32.class);
	}
}
