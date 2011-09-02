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

package boofcv.alg.filter.derivative;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;

import java.awt.image.BufferedImage;

/**
 * Displays detected corners in a video sequence
 *
 * @author Peter Abeles
 */
public class ImageShowImageDerivative {

	ImageSInt16 derivX;
	ImageSInt16 derivY;

	ListDisplayPanel panelX = new ListDisplayPanel();
	ListDisplayPanel panelY = new ListDisplayPanel();

	public void process(ImageUInt8 image) {

		if( derivX == null ) {
			derivX = new ImageSInt16(image.width,image.height);
			derivY = new ImageSInt16(image.width,image.height);
		}

		addDerivative(image,"Sobel",FactoryDerivative.sobel_I8());
		addDerivative(image,"Three",FactoryDerivative.three_I8());
		addDerivative(image,"Gaussian", FactoryDerivative.gaussian_I8(-1,3));

		ShowImages.showWindow(image, "Input");
		ShowImages.showWindow(panelX, "Derivative X-Axis");
		ShowImages.showWindow(panelY, "Derivative Y-Axis");
	}

	private void addDerivative( ImageUInt8 image , String name , ImageGradient<ImageUInt8, ImageSInt16> gradient ) {
		gradient.process(image,derivX,derivY);
		panelX.addImage(VisualizeImageData.standard(derivX,null),name);
		panelY.addImage(VisualizeImageData.standard(derivY,null),name);
	}

	public static void main(String args[]) {
		String fileName;

		if (args.length == 0) {
//			fileName = "evaluation/data/scale/mountain_7p1mm.jpg";
			fileName = "evaluation/data/indoors01.jpg";
		} else {
			fileName = args[0];
		}
		BufferedImage input = UtilImageIO.loadImage(fileName);
		ImageUInt8 gray = ConvertBufferedImage.convertFrom(input,(ImageUInt8)null);

		ImageShowImageDerivative display = new ImageShowImageDerivative();

		display.process(gray);
	}
}