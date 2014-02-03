/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.segmentation.ms;

import boofcv.alg.segmentation.ComputeRegionMeanColor;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageSInt32;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_B;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * Finds regions which are too small and merges them with a neighbor that is the most similar to it and connected.
 * The process is repeated until there are no more regions below the size threshold.  How similar two neighbors are
 * is determined using each region's average color.  Connectivity is determined using a 4-connect rule.
 *
 * @author Peter Abeles
 */
// TODO support 4 and 8 connect
public class MergeSmallRegions<T extends ImageBase> extends RegionMergeTree {

	// minimum allowed size of a region, inclusive
	protected int minimumSize;

	// Computes the color of each region
	protected ComputeRegionMeanColor<T> computeColor;

	// List which indicates ia segment is to be pruned based on its ID
	protected GrowQueue_B segmentPruneFlag = new GrowQueue_B();

	// Conversion between segment ID and prune ID
	protected GrowQueue_I32 segmentToPruneID = new GrowQueue_I32();

	// Used to mark pixels as not being a member of any region
	protected FastQueue<Node> pruneGraph = new FastQueue<Node>(Node.class,true);

	/**
	 * Constructor
	 *
	 * @param minimumSize Minimum number of pixels a region must have for it to not be pruned.
	 * @param computeColor Computes the color of each region
	 */
	public MergeSmallRegions(int minimumSize, ComputeRegionMeanColor<T> computeColor) {
		this.minimumSize = minimumSize;
		this.computeColor = computeColor;
	}

	public void setMinimumSize(int minimumSize) {
		this.minimumSize = minimumSize;
	}

	/**
	 * Merges together smaller regions.  Segmented image, region member count, and region color are all updated.
	 *
	 * @param image Input image.  Used to compute color of each region
	 * @param pixelToRegion (input/output) Segmented image with the ID of each region.  Modified.
	 * @param regionMemberCount (input/output) Number of members in each region  Modified.
	 * @param regionColor (Output) Storage for colors of each region. Will contains the color of each region on output.
	 */
	public void process( T image,
						 ImageSInt32 pixelToRegion ,
						 GrowQueue_I32 regionMemberCount,
						 FastQueue<float[]> regionColor ) {

		// iterate until no more regions need to be merged together
		while( true ) {

			// Update the color of each region
			regionColor.resize(regionMemberCount.size);
			computeColor.process(image, pixelToRegion, regionMemberCount, regionColor);

			initializeMerge(regionMemberCount.size);

			// Create a list of regions which are to be pruned
			if( !setupPruneList(regionMemberCount) )
				break;

			// Scan the image and create a list of regions which the pruned regions connect to
			findAdjacentRegions(pixelToRegion);

			// Select the closest match to merge into
			for( int i = 0; i < pruneGraph.size; i++ ) {
				selectMerge(i,regionColor);
			}

			// Do the usual merge stuff
			performMerge(pixelToRegion,regionMemberCount);
		}
	}

	/**
	 * Identifies which regions are to be pruned based on their member counts. Then sets up
	 * data structures for graph and converting segment ID to prune ID.
	 *
	 * @return true If elements need to be pruned and false if not.
	 */
	protected boolean setupPruneList(GrowQueue_I32 regionMemberCount) {
		segmentPruneFlag.resize(regionMemberCount.size);
		pruneGraph.reset();
		segmentToPruneID.resize(regionMemberCount.size);
		for( int i = 0; i < regionMemberCount.size; i++ ) {
			if( regionMemberCount.get(i) < minimumSize ) {
				segmentToPruneID.set(i, pruneGraph.size());
				Node n = pruneGraph.grow();
				n.init(i);
				segmentPruneFlag.set(i, true);
			} else {
				segmentPruneFlag.set(i, false);
			}
		}

		return pruneGraph.size() != 0;
	}

	/**
	 * Go through each pixel in the image and examine its neighbors according to a 4-connect rule.  If one of
	 * the pixels is in a region that is to be pruned mark them as neighbors. The image is traversed such that
	 * the number of comparisons is minimized.
	 */
	protected void findAdjacentRegions(ImageSInt32 pixelToRegion) {
		// -------- Do the inner pixels first
		for( int y = 0; y < pixelToRegion.height-1; y++ ) {
			int indexImg = pixelToRegion.startIndex + pixelToRegion.stride*y;
			for( int x = 0; x < pixelToRegion.width-1; x++ , indexImg++ ) {

				int regionA = pixelToRegion.data[indexImg];
				// x + 1 , y
				int regionB = pixelToRegion.data[indexImg+1];
				// x , y + 1
				int regionC = pixelToRegion.data[indexImg+pixelToRegion.stride];

				boolean pruneA = segmentPruneFlag.data[regionA];

				if( regionA != regionB ) {
					boolean pruneB = segmentPruneFlag.data[regionB];

					if( pruneA ) {
						Node n = pruneGraph.get(segmentToPruneID.get(regionA));
						n.connect(regionB);
					}
					if( pruneB ) {
						Node n = pruneGraph.get(segmentToPruneID.get(regionB));
						n.connect(regionA);
					}
				}

				if( regionA != regionC ) {
					boolean pruneC = segmentPruneFlag.data[regionC];

					if( pruneA ) {
						Node n = pruneGraph.get(segmentToPruneID.get(regionA));
						n.connect(regionC);
					}
					if( pruneC ) {
						Node n = pruneGraph.get(segmentToPruneID.get(regionC));
						n.connect(regionA);
					}
				}
			}
		}

		// -------- Do the same for the right edge
		for( int y = 0; y < pixelToRegion.height-1; y++ ) {
			int indexImg = pixelToRegion.startIndex + pixelToRegion.stride*y +pixelToRegion.width-1;

				int regionA = pixelToRegion.data[indexImg];
				// x , y + 1
				int regionC = pixelToRegion.data[indexImg+pixelToRegion.stride];

				boolean pruneA = segmentPruneFlag.data[regionA];

				if( regionA != regionC ) {
					boolean pruneC = segmentPruneFlag.data[regionC];

					if( pruneA ) {
						Node n = pruneGraph.get(segmentToPruneID.get(regionA));
						n.connect(regionC);
					}
					if( pruneC ) {
						Node n = pruneGraph.get(segmentToPruneID.get(regionC));
						n.connect(regionA);
					}
				}
			}

		// -------- Do the same for the bottom edge
		for( int x = 0; x < pixelToRegion.width-1; x++ ) {
			int indexImg = pixelToRegion.startIndex + pixelToRegion.stride*(pixelToRegion.height-1) + x;

			int regionA = pixelToRegion.data[indexImg];
			// x + 1 , y
			int regionB = pixelToRegion.data[indexImg+1];

			boolean pruneA = segmentPruneFlag.data[regionA];

			if( regionA != regionB ) {
				boolean pruneB = segmentPruneFlag.data[regionB];

				if( pruneA ) {
					Node n = pruneGraph.get(segmentToPruneID.get(regionA));
					n.connect(regionB);
				}
				if( pruneB ) {
					Node n = pruneGraph.get(segmentToPruneID.get(regionB));
					n.connect(regionA);
				}
			}
		}
	}

	/**
	 * Examine edges for the specified node and select node which it is the best match for it to merge with
	 *
	 * @param pruneId The prune Id of the segment which is to be merged into another segment
	 * @param regionColor List of region colors
	 */
	protected void selectMerge( int pruneId , FastQueue<float[]> regionColor ) {
		// Grab information on the region which is being pruned
		Node n = pruneGraph.get(pruneId);
		float[] targetColor = regionColor.get(n.segment);

		// segment ID and distance away from the most similar neighbor
		int bestId = -1;
		float bestDistance = Float.MAX_VALUE;

		// Examine all the segments it is connected to to see which one it is most similar too
		for( int i = 0; i < n.edges.size; i++ ) {
			int segment = n.edges.get(i);
			float[] neighborColor = regionColor.get(segment);
			float d = SegmentMeanShiftSearch.distanceSq(targetColor, neighborColor);

			if( d < bestDistance ) {
				bestDistance = d;
				bestId = segment;
			}
		}

		if( bestId == -1 )
			throw new RuntimeException("No neighbors?  Something went really wrong.");

		markMerge(n.segment, bestId);
	}

	/**
	 * Node in a graph.  Specifies which segments are adjacent to a segment which is to be pruned.
	 */
	public static class Node
	{
		public int segment;
		// List of segments this segment is connected to
		public GrowQueue_I32 edges = new GrowQueue_I32();

		public void init( int segment ) {
			this.segment = segment;
			edges.reset();
		}

		public void connect(int segment) {
			if( isConnected(segment))
				return;

			this.edges.add(segment);
		}

		public boolean isConnected( int segment ) {
			for( int i = 0; i < edges.size; i++ ) {
				if( edges.data[i] == segment)
					return true;
			}
			return false;
		}
	}
}
