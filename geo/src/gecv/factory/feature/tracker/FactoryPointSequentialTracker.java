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

package gecv.factory.feature.tracker;

import gecv.abst.feature.detect.extract.FeatureExtractor;
import gecv.abst.feature.tracker.PointSequentialTracker;
import gecv.abst.feature.tracker.PstWrapperKltPyramid;
import gecv.abst.feature.tracker.PstWrapperSurf;
import gecv.alg.feature.associate.AssociateSurfBasic;
import gecv.alg.feature.associate.ScoreAssociateEuclideanSq;
import gecv.alg.feature.associate.ScoreAssociateTuple;
import gecv.alg.feature.describe.DescribePointSurf;
import gecv.alg.feature.detect.interest.FastHessianFeatureDetector;
import gecv.alg.feature.orientation.OrientationIntegral;
import gecv.alg.tracker.pklt.PkltManager;
import gecv.alg.tracker.pklt.PkltManagerConfig;
import gecv.alg.transform.ii.GIntegralImageOps;
import gecv.factory.feature.associate.FactoryAssociationTuple;
import gecv.factory.feature.detect.extract.FactoryFeatureFromIntensity;
import gecv.factory.feature.orientation.FactoryOrientationAlgs;
import gecv.struct.image.ImageBase;


/**
 * Factory for creating trackers which implement {@link PointSequentialTracker}.
 *
 * @author Peter Abeles
 */
public class FactoryPointSequentialTracker {

	/**
	 * Creates a tracker using KLT features/tracker.
	 *
	 * @param maxFeatures Maximum number of features it can detect/track. Try 200 initially.
	 * @param imgWidth Input image width.
	 * @param imgHeight Input image height.
	 * @param scaling Scales in the image pyramid. Recommend [1,2,4] or [2,4]
	 * @param imageType Input image type.
	 * @param derivType Image derivative  type.
	 * @return KLT based tracker.
	 */
	public static <I extends ImageBase, D extends ImageBase>
	PointSequentialTracker<I> klt( int maxFeatures , int imgWidth , int imgHeight ,
								   int scaling[] , Class<I> imageType , Class<D> derivType )
	{
		PkltManagerConfig<I, D> config =
				PkltManagerConfig.createDefault(imageType,derivType,imgWidth,imgHeight);
		config.pyramidScaling = scaling;
		config.maxFeatures = maxFeatures;
		PkltManager<I, D> trackManager =
				new PkltManager<I, D>(config);

		return new PstWrapperKltPyramid<I,D>(trackManager);
	}

	/**
	 * Creates a tracker using SURF features.
	 *
	 * @param maxMatches When features are associated with each other what is the maximum number of associations.
	 * @param detectPerScale Controls how many features can be detected.  Try a value of 200 initially.
	 * @param minSeparation How close together detected features can be.  Recommended value = 2.
	 * @param imageType Type of image the input is.
	 * @param <I> Input image type.
	 * @param <II> Integral image type.
	 * @return SURF based tracker.
	 */
	public static <I extends ImageBase,II extends ImageBase>
	PointSequentialTracker<I> surf( int maxMatches , int detectPerScale , int minSeparation ,
									Class<I> imageType )
	{
		Class<II> integralType = GIntegralImageOps.getIntegralType(imageType);

		FeatureExtractor extractor = FactoryFeatureFromIntensity.create(minSeparation,1,10,false,false,false);

		FastHessianFeatureDetector<II> detector = new FastHessianFeatureDetector<II>(extractor,detectPerScale,9,4,4);
		OrientationIntegral<II> orientation = FactoryOrientationAlgs.average_ii(3,false,integralType);
		DescribePointSurf<II> describe = new DescribePointSurf<II>();

		ScoreAssociateTuple score = new ScoreAssociateEuclideanSq();
		AssociateSurfBasic assoc = new AssociateSurfBasic(FactoryAssociationTuple.inlierError(score,maxMatches,3));

		return new PstWrapperSurf<I,II>(detector,orientation,describe,assoc,integralType);
	}
}
