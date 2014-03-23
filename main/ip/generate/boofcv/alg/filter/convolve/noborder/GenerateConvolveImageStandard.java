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

package boofcv.alg.filter.convolve.noborder;

import boofcv.misc.AutoTypeImage;
import boofcv.misc.CodeGeneratorBase;

import java.io.FileNotFoundException;

/**
 * Code generator for {@link ConvolveImageStandard}.
 *
 * @author Peter Abeles
 */
public class GenerateConvolveImageStandard extends CodeGeneratorBase {
	String className = "ConvolveImageStandard";

	String kernelType;
	String inputType;
	String outputType;
	String kernelData;
	String inputData;
	String outputData;
	String sumType;
	String typeCast;
	String bitWise;
	boolean hasDivide;
	boolean hasBound;

	@Override
	public void generate()throws FileNotFoundException {
		printPreamble();
		printAllOps(AutoTypeImage.F32, AutoTypeImage.F32, false, false);
//		printAllOps(AutoTypeImage.F32, AutoTypeImage.F32, false, true);
		printAllOps(AutoTypeImage.F64, AutoTypeImage.F64, false, false);
		printAllOps(AutoTypeImage.U8, AutoTypeImage.I16, false, false);
		printAllOps(AutoTypeImage.U8, AutoTypeImage.S32, false, false);
		printAllOps(AutoTypeImage.S16,AutoTypeImage.I16,false, false);
		printAllOps(AutoTypeImage.U8,AutoTypeImage.I8,true, false);
//		printAllOps(AutoTypeImage.U8,AutoTypeImage.I8,false, true);
		printAllOps(AutoTypeImage.S16,AutoTypeImage.I16,true, false);
		printAllOps(AutoTypeImage.S32,AutoTypeImage.S32,false, false);
		printAllOps(AutoTypeImage.S32,AutoTypeImage.S32,true, false);

		out.println("}");
	}

	private void printPreamble() throws FileNotFoundException {
		setOutputFile(className);
		out.print("import boofcv.struct.convolve.*;\n" +
				"import boofcv.struct.image.*;\n");
		out.println();
		out.println();
		out.print("/**\n" +
				" * <p>\n" +
				" * Standard algorithms with no fancy optimization for convolving 1D and 2D kernels across an image.\n" +
				" * </p>\n" +
				" * \n" +
				" * <p>\n" +
				" * NOTE: This code was automatically generated using {@link "+getClass().getName()+"}.\n" +
				" * </p>\n" +
				" * \n" +
				" * @author Peter Abeles\n" +
				" */\n" +
				"@SuppressWarnings({\"ForLoopReplaceableByForEach\"})\n" +
				"public class " + className + " {\n\n");
	}

	private void printAllOps(AutoTypeImage input, AutoTypeImage output, boolean hasDivide, boolean hasBound)
	{
		boolean isInteger = input.isInteger();
		boolean is64 = input.getNumBits()==64;

		typeCast = output.getTypeCastFromSum();
		kernelType = isInteger ? "I32" : is64 ? "F64" : "F32";
		inputType = input.getSingleBandName();
		outputType = output.getSingleBandName();
		kernelData = isInteger ? "int" : is64 ? "double" : "float";
		inputData = input.getDataType();
		outputData = output.getDataType();
		sumType = isInteger ? "int" : is64 ? "double" : "float";
		bitWise = input.getBitWise();
		this.hasDivide = hasDivide;
		this.hasBound = hasBound;

		if( !hasBound ) {
			printHorizontal();
			printVertical();
		}
		printConvolve2D();
	}

	private void printHorizontal() {
		String paramDiv = hasDivide ? " , int divisor" : "";
		String totalDiv = hasDivide ? "((total+halfDivisor)/divisor)" : "total";

		out.print("\tpublic static void horizontal( Kernel1D_" + kernelType + " kernel ,\n");
		out.print("\t\t\t\t\t\t\t\t  " + inputType + " image, " + outputType + " dest" + paramDiv + " ) {\n" +
				"\t\tfinal " + inputData + "[] dataSrc = image.data;\n" +
				"\t\tfinal " + outputData + "[] dataDst = dest.data;\n" +
				"\t\tfinal " + kernelData + "[] dataKer = kernel.data;\n" +
				"\n" +
				"\t\tfinal int offset = kernel.getOffset();\n" +
				"\t\tfinal int kernelWidth = kernel.getWidth();\n");
		if( hasDivide )
			out.print("\t\tfinal int halfDivisor = divisor/2;\n");
		out.print("\n" +
				"\t\tfinal int width = image.getWidth();\n" +
				"\n" +
				"\t\tfor( int i = 0; i < image.height; i++ ) {\n" +
				"\t\t\tint indexDst = dest.startIndex + i*dest.stride+offset;\n" +
				"\t\t\tint j = image.startIndex + i*image.stride;\n" +
				"\t\t\tfinal int jEnd = j+width-(kernelWidth-1);\n" +
				"\n" +
				"\t\t\tfor( ; j < jEnd; j++ ) {\n" +
				"\t\t\t\t" + sumType + " total = 0;\n" +
				"\t\t\t\tint indexSrc = j;\n" +
				"\t\t\t\tfor( int k = 0; k < kernelWidth; k++ ) {\n" +
				"\t\t\t\t\ttotal += (dataSrc[indexSrc++] " + bitWise + ") * dataKer[k];\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\tdataDst[indexDst++] = " + typeCast + totalDiv + ";\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printVertical() {
		String paramDiv = hasDivide ? " , int divisor" : "";
		String totalDiv = hasDivide ? "((total+halfDivisor)/divisor)" : "total";

		out.print("\tpublic static void vertical( Kernel1D_" + kernelType + " kernel,\n" +
				"\t\t\t\t\t\t\t\t " + inputType + " image, " + outputType + " dest" + paramDiv +" )\n" +
				"\t{\n" +
				"\t\tfinal " + inputData + "[] dataSrc = image.data;\n" +
				"\t\tfinal " + outputData + "[] dataDst = dest.data;\n" +
				"\t\tfinal " + kernelData + "[] dataKer = kernel.data;\n" +
				"\n" +
				"\t\tfinal int offset = kernel.getOffset();\n" +
				"\t\tfinal int kernelWidth = kernel.getWidth();\n");
		if( hasDivide )
			out.print("\t\tfinal int halfDivisor = divisor/2;\n");
		out.print("\n" +
				"\t\tfinal int imgWidth = dest.getWidth();\n" +
				"\t\tfinal int imgHeight = dest.getHeight();\n" +
				"\n" +
				"\t\tfinal int yEnd = imgHeight-(kernelWidth-offset-1);\n" +
				"\n" +
				"\t\tfor( int y = offset; y < yEnd; y++ ) {\n" +
				"\t\t\tint indexDst = dest.startIndex+y*dest.stride;\n" +
				"\t\t\tint i = image.startIndex + (y-offset)*image.stride;\n" +
				"\t\t\tfinal int iEnd = i+imgWidth;\n" +
				"\n" +
				"\t\t\tfor( ; i < iEnd; i++ ) {\n" +
				"\t\t\t\t" + sumType + " total = 0;\n" +
				"\t\t\t\tint indexSrc = i;\n" +
				"\t\t\t\tfor( int k = 0; k < kernelWidth; k++ ) {\n" +
				"\t\t\t\t\ttotal += (dataSrc[indexSrc] " + bitWise + ")* dataKer[k];\n" +
				"\t\t\t\t\tindexSrc += image.stride;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\tdataDst[indexDst++] = " + typeCast + totalDiv + ";\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printConvolve2D() {

		String paramDiv = hasDivide ? ", int divisor " : "";
		String totalDiv = hasDivide ? "((total+halfDivisor)/divisor)" : "total";
		String paramBound = hasBound ? ", "+sumType+" minValue , "+sumType+" maxValue " : "";
		String performBound = "";

		if( hasBound ) {
			performBound = "\n" +
					"\t\t\t\tif( total < minValue )\n" +
					"\t\t\t\t\ttotal = minValue;\n" +
					"\t\t\t\telse if( total > maxValue )\n" +
					"\t\t\t\t\ttotal = maxValue;\n" +
					"\n";
		}

		out.print("\tpublic static void convolve( Kernel2D_" + kernelType + " kernel , " + inputType + " src , " + outputType + " dest " + paramDiv + paramBound + ")\n" +
				"\t{\n" +
				"\t\tfinal " + kernelData + "[] dataKernel = kernel.data;\n" +
				"\t\tfinal " + inputData + "[] dataSrc = src.data;\n" +
				"\t\tfinal " + outputData + "[] dataDst = dest.data;\n" +
				"\n" +
				"\t\tfinal int width = src.getWidth();\n" +
				"\t\tfinal int height = src.getHeight();\n");
		if( hasDivide )
			out.print("\t\tfinal int halfDivisor = divisor/2;\n");
		out.print("\n" +
				"\t\tint kernelRadius = kernel.width/2;\n" +
				"\n" +
				"\t\tfor( int y = kernelRadius; y < height-kernelRadius; y++ ) {\n" +
				"\t\t\tint indexDst = dest.startIndex + y*dest.stride+kernelRadius;\n" +
				"\t\t\tfor( int x = kernelRadius; x < width-kernelRadius; x++ ) {\n" +
				"\t\t\t\t" + sumType + " total = 0;\n" +
				"\t\t\t\tint indexKer = 0;\n" +
				"\t\t\t\tfor( int ki = -kernelRadius; ki <= kernelRadius; ki++ ) {\n" +
				"\t\t\t\t\tint indexSrc = src.startIndex+(y+ki)*src.stride+ x;\n" +
				"\t\t\t\t\tfor( int kj = -kernelRadius; kj <= kernelRadius; kj++ ) {\n" +
				"\t\t\t\t\t\ttotal += (dataSrc[indexSrc+kj] " + bitWise + " )* dataKernel[indexKer++];\n" +
				"\t\t\t\t\t}\n" +
				"\t\t\t\t}\n" +
				performBound +
				"\t\t\t\tdataDst[indexDst++] = " + typeCast + totalDiv + ";\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	public static void main(String args[]) throws FileNotFoundException {
		GenerateConvolveImageStandard gen = new GenerateConvolveImageStandard();
		gen.generate();
	}
}
