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

package boofcv.alg.filter.binary;

import boofcv.core.image.FactorySingleBandImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.SingleBandImage;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageSInt32;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestBinaryImageHighOps {

	int[] patternDo = new int[]{0,0,0,0,0,0,0,0,0,0,
								0,0,0,1,1,0,1,0,0,0,
								0,0,1,1,1,1,0,0,0,0,
								0,0,1,1,5,0,0,0,1,0,
								0,0,0,1,1,0,0,1,1,0,
								0,0,0,0,0,0,0,0,1,0};

	int[] patternUp = new int[]{6,6,6,6,6,6,6,6,6,6,
								6,6,6,5,5,6,5,6,6,6,
							 	6,6,5,5,5,5,6,6,6,6,
							 	6,6,5,5,2,6,6,6,5,6,
								6,6,6,5,5,6,6,5,5,6,
								6,6,6,6,6,6,6,6,5,6};


	int width = 10;
	int height = 6;

	@Test
	public void hysteresisLabel4() {
		int total = 0;
		Method[] list = BinaryImageHighOps.class.getMethods();

		for( Method m : list ) {
			if( !m.getName().equals("hysteresisLabel4"))
				continue;

			Class<?> param[] = m.getParameterTypes();

			ImageBase inputDown = GeneralizedImageOps.createImage(param[0],width,height);
			ImageBase inputUp = GeneralizedImageOps.createImage(param[0],width,height);
			ImageSInt32 labeled = new ImageSInt32(width,height);

			SingleBandImage a = FactorySingleBandImage.wrap(inputDown);
			for( int y = 0; y < height; y++ ) {
				for( int x = 0; x < width; x++ ) {
					a.set(x,y,patternDo[y*width+x]);
				}
			}

			a = FactorySingleBandImage.wrap(inputUp);
			for( int y = 0; y < height; y++ ) {
				for( int x = 0; x < width; x++ ) {
					a.set(x,y,patternUp[y*width+x]);
				}
			}

			BoofTesting.checkSubImage(this,"performHysteresisLabel",true,m,11,inputDown,inputUp,labeled);
			total++;
		}

		assertEquals(6,total);
	}

	@Test
	public void hysteresisLabel8() {
		int total = 0;
		Method[] list = BinaryImageHighOps.class.getMethods();

		for( Method m : list ) {
			if( !m.getName().equals("hysteresisLabel8"))
				continue;

			Class<?> param[] = m.getParameterTypes();

			ImageBase inputDown = GeneralizedImageOps.createImage(param[0],width,height);
			ImageBase inputUp = GeneralizedImageOps.createImage(param[0],width,height);
			ImageSInt32 labeled = new ImageSInt32(width,height);

			SingleBandImage a = FactorySingleBandImage.wrap(inputDown);
			for( int y = 0; y < height; y++ ) {
				for( int x = 0; x < width; x++ ) {
					a.set(x,y,patternDo[y*width+x]);
				}
			}

			a = FactorySingleBandImage.wrap(inputUp);
			for( int y = 0; y < height; y++ ) {
				for( int x = 0; x < width; x++ ) {
					a.set(x,y,patternUp[y*width+x]);
				}
			}

			BoofTesting.checkSubImage(this,"performHysteresisLabel",true,m,12,inputDown,inputUp,labeled);
			total++;
		}

		assertEquals(6,total);
	}

	public void performHysteresisLabel( Method m , Integer expected , ImageBase inputDown , ImageBase inputUp , ImageSInt32 labeled )
			throws InvocationTargetException, IllegalAccessException
	{
		int numFound = (Integer)m.invoke(null,inputUp,labeled,3,5,true,null);
		assertEquals(1,numFound);
		assertEquals(expected, countNotZero(labeled),1e-4);

		numFound = (Integer)m.invoke(null,inputDown,labeled,1,4,false,null);
		assertEquals(1,numFound);
		assertEquals(expected, countNotZero(labeled),1e-4);
	}

	private int countNotZero( ImageBase image ) {
		SingleBandImage a = FactorySingleBandImage.wrap(image);

		int ret = 0;

		for( int i = 0; i < a.getHeight(); i++ ) {
			for( int j = 0; j < a.getWidth(); j++ ) {
				if( a.get(j,i).intValue() != 0 )
					ret++;
			}
		}

		return ret;
	}
}