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

package gecv.alg.detect.interest;

import gecv.abst.detect.corner.GeneralFeatureDetector;
import gecv.abst.filter.derivative.AnyImageDerivative;
import gecv.core.image.border.FactoryImageBorder;
import gecv.core.image.border.ImageBorder_F32;
import gecv.struct.QueueCorner;
import gecv.struct.gss.ScaleSpacePyramid;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import jgrl.struct.point.Point2D_I16;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Detects scale invariant interest/corner points by computing the feature intensities across a pyramid of different scales.
 * Features which are maximums with in a local 2D neighborhood and within the local scale neighbourhood are declared to
 * be features.
 * </p>
 *
 * <p>
 * NOTE: Features are not computed for the bottom and top most layers in the pyramid.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FeaturePyramid<T extends ImageBase, D extends ImageBase> {

	// generalized feature detector.  Used to find candidate features in each scale's image
	private GeneralFeatureDetector<T,D> detector;
	private float baseThreshold;
	// feature intensity in the pyramid
	protected ImageFloat32 intensities[];
	protected int spaceIndex = 0;

	protected List<Point2D_I16> maximums[];

	// List of found feature points
	protected List<ScalePoint> foundPoints = new ArrayList<ScalePoint>();

	protected AnyImageDerivative<T,D> computeDerivative;

	// how much the feature intensity is scaled in each level
	// varies depending on feature type
	protected double scalePower;

	/**
	 * Create a feature detector.
	 *
	 * @param detector Point feature detector which is used to find candidates in each scale level
	 */
	public FeaturePyramid( GeneralFeatureDetector<T, D> detector, AnyImageDerivative<T,D> computeDerivative ,
						   double scalePower ) {
		this.detector = detector;

		this.baseThreshold = detector.getThreshold();
		this.computeDerivative = computeDerivative;
		this.scalePower = scalePower;
	}

	/**
	 * Searches for features inside the provided scale space
	 *
	 * @param ss Scale space of an image
	 */
	public void detect( ScaleSpacePyramid<T> ss ) {
		spaceIndex = 0;
		if( intensities == null ) {
			intensities = new ImageFloat32[3];
			intensities[0] = new ImageFloat32(1,1);
			intensities[1] = new ImageFloat32(1,1);
			intensities[2] = new ImageFloat32(1,1);

			maximums = new List[ 3 ];
			maximums[0] = new ArrayList<Point2D_I16>();
			maximums[1] = new ArrayList<Point2D_I16>();
			maximums[2] = new ArrayList<Point2D_I16>();
		}

		// compute feature intensity in each level
		for( int i = 0; i < ss.getNumLayers(); i++ ) {
			detectCandidateFeatures(ss.getLayer(i),ss.getScale(i));

			// find maximum in NxNx3 (local image and scale space) region
			if( i >= 2 ) {
				findLocalScaleSpaceMax(ss,i-1);
			}
		}
	}


	/**
	 * Use the feature detector to find candidate features in each level.  Only compute the needed image derivatives.
	 */
	private void detectCandidateFeatures( T image , double scale ) {
		// adjust corner intensity threshold based upon the current scale factor
		float scaleThreshold = (float)(baseThreshold/(scale*scale));
		detector.setThreshold(scaleThreshold);
		computeDerivative.setInput(image);

		D derivX = null, derivY = null;
		D derivXX = null, derivYY = null, derivXY = null;

		if( detector.getRequiresGradient() ) {
			derivX = computeDerivative.getDerivative(true);
			derivY = computeDerivative.getDerivative(false);
		}
		if( detector.getRequiresHessian() ) {
			derivXX = computeDerivative.getDerivative(true,true);
			derivYY = computeDerivative.getDerivative(false,false);
			derivXY = computeDerivative.getDerivative(true,false);
		}
		
		detector.process(image,derivX,derivY,derivXX,derivYY,derivXY);

		intensities[spaceIndex].reshape(image.width,image.height);
		intensities[spaceIndex].setTo(detector.getIntensity());

		List<Point2D_I16> m = maximums[spaceIndex];
		m.clear();
		QueueCorner q = detector.getFeatures();
		for( int i = 0; i < q.num; i++ ) {
			m.add( q.get(i).copy() );
		}

		spaceIndex++;
		if( spaceIndex >= 3 )
			spaceIndex = 0;
	}

	/**
	 * Searches the pyramid layers up and down to see if the found 2D features are also scale space maximums.
	 */
	protected void findLocalScaleSpaceMax( ScaleSpacePyramid<T> ss , int layerID ) {
		int index0 = spaceIndex;
		int index1 = (spaceIndex + 1) % 3;
		int index2 = (spaceIndex + 2) % 3;

		List<Point2D_I16> candidates = maximums[index1];
		ImageBorder_F32 inten0 = FactoryImageBorder.value(intensities[index0],0);
		ImageFloat32 inten1 = intensities[index1];
		ImageBorder_F32 inten2 = FactoryImageBorder.value(intensities[index2],0);

		float scale0 = (float)ss.getScale(layerID-1);
		float scale1 = (float)ss.getScale(layerID);
		float scale2 = (float)ss.getScale(layerID+1);

		float ss0 = (float)Math.pow(scale0,scalePower);
		float ss1 = (float)Math.pow(scale1,scalePower);
		float ss2 = (float)Math.pow(scale2,scalePower);

		for( Point2D_I16 c : candidates ) {
			float val = ss1*inten1.get(c.x,c.y);

			// find pixel location in each image's local coordinate
			int x0 = (int)(c.x*scale1/scale0);
			int y0 = (int)(c.y*scale1/scale0);

			int x2 = (int)(c.x*scale1/scale2);
			int y2 = (int)(c.y*scale1/scale2);


			if( checkMax(inten0, ss0, val, x0, y0) && checkMax(inten2, ss2, val, x2, y2) ) {
				// put features into the scale of the upper image
				foundPoints.add( new ScalePoint((int)(c.x*scale1),(int)(c.y*scale1),scale1));
			}
		}
	}

	private boolean checkMax(ImageBorder_F32 inten, float scoreAdjust, float bestScore, int c_x, int c_y) {
		boolean isMax = true;
		beginLoop:
		for( int i = c_y -1; i <= c_y+1; i++ ) {
			for( int j = c_x-1; j <= c_x+1; j++ ) {

				if( scoreAdjust*inten.get(j,i) >= bestScore ) {
					isMax = false;
					break beginLoop;
				}
			}
		}
		return isMax;
	}


	public List<ScalePoint> getInterestPoints() {
		return foundPoints;
	}
}
