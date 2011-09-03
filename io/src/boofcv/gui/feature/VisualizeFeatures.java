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

package boofcv.gui.feature;

import boofcv.struct.feature.ScalePoint;
import georegression.struct.point.Point2D_I32;

import java.awt.*;


/**
 * @author Peter Abeles
 */
public class VisualizeFeatures {

	public static void drawPoints( Graphics2D g2 ,
								   Color color ,
								   java.util.List<Point2D_I32> points ,
								   int radius) {

		g2.setStroke(new BasicStroke(2));

		int ro = radius+1;
		int wo = ro*2+1;
		int w = radius*2+1;
		for( Point2D_I32 p : points ) {
			g2.setColor(color);
			g2.fillOval(p.x-radius,p.y-radius,w,w);
			g2.setColor(Color.BLACK);
			g2.drawOval(p.x-ro,p.y-ro,wo,wo);
		}
	}

	public static void drawScalePoints( Graphics2D g2 , java.util.List<ScalePoint> points , double radius ) {
		g2.setColor(Color.RED);
		g2.setStroke(new BasicStroke(2));

		for( ScalePoint p : points ) {
			int r = (int)(radius*p.scale);
			int w = r*2+1;
			g2.drawOval(p.x-r,p.y-r,w,w);
		}
	}
}
