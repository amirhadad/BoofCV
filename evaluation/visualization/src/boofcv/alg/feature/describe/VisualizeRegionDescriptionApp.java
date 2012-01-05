/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.describe;

import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.gui.ProcessInput;
import boofcv.gui.SelectAlgorithmImagePanel;
import boofcv.gui.feature.SelectRegionDescriptionPanel;
import boofcv.gui.feature.TupleDescPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ImageListManager;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;


/**
 *  Allows the user to select a point and show the description of the region at that point
 *
 * @author Peter Abeles
 */
public class VisualizeRegionDescriptionApp <T extends ImageSingleBand, D extends ImageSingleBand>
	extends SelectAlgorithmImagePanel implements ProcessInput, SelectRegionDescriptionPanel.Listener
{
	boolean processedImage = false;

	Class<T> imageType;
	T input;

	DescribeRegionPoint<T> describe;

	SelectRegionDescriptionPanel panel = new SelectRegionDescriptionPanel();

	TupleDescPanel tuplePanel = new TupleDescPanel();

	// most recently requested pixel description.  Used when the algorithm is changed
	Point2D_I32 targetPt;
	double targetRadius;
	double targetOrientation;

	public VisualizeRegionDescriptionApp( Class<T> imageType , Class<D> derivType  ) {
		super(1);

		this.imageType = imageType;

		addAlgorithm(0,"SURF", FactoryDescribeRegionPoint.surf(false, imageType));
		addAlgorithm(0,"BRIEF", FactoryDescribeRegionPoint.brief(16, 512, -1, 4, true, imageType));
		addAlgorithm(0,"BRIEFO", FactoryDescribeRegionPoint.brief(16, 512, -1, 4, false, imageType));
		addAlgorithm(0,"Gaussian 12", FactoryDescribeRegionPoint.gaussian12(20, imageType, derivType));
		addAlgorithm(0,"Gaussian 14", FactoryDescribeRegionPoint.steerableGaussian(20, false, imageType, derivType));
		addAlgorithm(0,"Pixel 5x5", FactoryDescribeRegionPoint.pixel(5, 5, imageType));
		addAlgorithm(0,"NCC 5x5", FactoryDescribeRegionPoint.pixelNCC(5, 5, imageType));

		panel.setListener(this);
		tuplePanel.setPreferredSize(new Dimension(100,50));
		add(tuplePanel,BorderLayout.SOUTH);
		setMainGUI(panel);
	}

	public void process( final BufferedImage image ) {
		input = ConvertBufferedImage.convertFromSingle(image, null, imageType);
		if( describe != null ) {
			describe.setImage(input);
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				panel.setBackground(image);
				panel.setPreferredSize(new Dimension(image.getWidth(),image.getHeight()));
				processedImage = true;
			}});


		doRefreshAll();
	}

	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}

	@Override
	public void refreshAll(Object[] cookies) {
		setActiveAlgorithm(0,null,cookies[0]);
	}

	@Override
	public synchronized void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		this.describe = (DescribeRegionPoint<T>)cookie;
		if( input != null ) {
			describe.setImage(input);
		}
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				updateTargetDescription();
				repaint();
			}});
	}

	@Override
	public void changeImage(String name, int index) {
		ImageListManager m = getInputManager();
		BufferedImage image = m.loadImage(index);

		process(image);
	}

	@Override
	public synchronized void descriptionChanged(Point2D_I32 pt, double radius, double orientation) {
		if( pt == null || radius < 1) {
			targetPt = null;
		} else {
			this.targetPt = pt;
			this.targetRadius = radius;
			this.targetOrientation = orientation;
		}
		updateTargetDescription();
	}

	/**
	 * Extracts the target description and updates the panel.  Should only be called from a swing thread
	 */
	private void updateTargetDescription() {
		if( targetPt != null ) {
			double scale = targetRadius/describe.getCanonicalRadius();

			TupleDesc_F64 feature = describe.process(targetPt.x,targetPt.y,targetOrientation,scale,null);
			tuplePanel.setDescription(feature);
		} else {
			tuplePanel.setDescription(null);
		}
		tuplePanel.repaint();
	}


	public static void main( String args[] ) {
		Class imageType = ImageFloat32.class;
		Class derivType = GImageDerivativeOps.getDerivativeType(imageType);

		VisualizeRegionDescriptionApp app = new VisualizeRegionDescriptionApp(imageType,derivType);

		ImageListManager manager = new ImageListManager();
		manager.add("Cave","../data/evaluation/stitch/cave_01.jpg","../data/evaluation/stitch/cave_02.jpg");
		manager.add("Kayak","../data/evaluation/stitch/kayak_02.jpg","../data/evaluation/stitch/kayak_03.jpg");
		manager.add("Forest","../data/evaluation/scale/rainforest_01.jpg","../data/evaluation/scale/rainforest_02.jpg");

		app.setPreferredSize(new Dimension(500,500));
		app.setSize(500,500);
		app.setInputManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app,"Region Descriptor Visualization");
	}
}
