/* Copyright (c) 2001-2025, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import com.pixelmed.dicom.*;
import com.pixelmed.display.*;

import java.io.*;
import java.awt.*; 
import java.awt.color.*; 
import java.awt.image.*;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class of static methods to convert PALETTE COLOR to MONOCHROME2 images.</p>
 *
 * @author	dclunie
 */
public class ConvertColorToGrayscale {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/ConvertColorToGrayscale.java,v 1.2 2025/01/29 10:58:05 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(ConvertColorToGrayscale.class);
	
	private static final DicomDictionary dictionary = DicomDictionary.StandardDictionary;

	/**
	 * <p>Read a DICOM image input format file with a Photometric Interpretation of RGB, and from it create a DICOM image of Photometric Interpretation MONOCHROME2.</p>
	 *
	 * @param	inputFileName	the input file name
	 * @param	outputFileName	the output file name
	 */
	public ConvertColorToGrayscale(String inputFileName,String outputFileName) throws DicomException, FileNotFoundException, IOException {
		AttributeList list = new AttributeList();
		DicomInputStream in = new DicomInputStream(new BufferedInputStream(new FileInputStream(inputFileName)));
		list.read(in);
		in.close();
		
		String sopClassUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
		if (!SOPClass.isImageStorage(sopClassUID)) {
			throw new DicomException("Input file is not an image");
		}
		
		String photometricInterpretation = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.PhotometricInterpretation);
		if (!photometricInterpretation.equals("RGB")) {
			throw new DicomException("Input image is not RGB");
		}
		
		SourceImage sImg = new SourceImage(list);
				
		BufferedImage src = sImg.getBufferedImage(); 	// possibly the first of multiple
				
		if (src.getColorModel().getNumComponents() != 3) {
			throw new DicomException("Input image is not three components");
		}
		else {
			{
				int columns = src.getWidth();
				int rows = src.getHeight();
        
				SampleModel srcSampleModel = src.getSampleModel();
				WritableRaster srcRaster = src.getRaster();
				DataBuffer srcDataBuffer = srcRaster.getDataBuffer();
				int srcNumBands = srcRaster.getNumBands();

				int dstPixelsLength = rows*columns;
				byte dstPixels[] = new byte[dstPixelsLength];

				int srcPixels[] = null; // to disambiguate SampleModel.getPixels() method signature
				srcPixels = srcSampleModel.getPixels(0,0,columns,rows,srcPixels,srcDataBuffer);
				int srcPixelsLength = srcPixels.length;

				if (srcNumBands == 3 && srcPixelsLength == dstPixelsLength * 3) {
					int dstIndex=0;
					for (int srcIndex=0; srcIndex<srcPixelsLength; srcIndex+=3) {
						// rather than just picking one channel value, averages the three
						dstPixels[dstIndex++]=(byte)((srcPixels[srcIndex]+srcPixels[srcIndex+1]+srcPixels[srcIndex+2])/3);
					}
				}
				else {
					throw new DicomException("Cannot copy source pixels to destination");
				}
				
				Attribute pixelData = new OtherByteAttribute(TagFromName.PixelData);
				pixelData.setValues(dstPixels);
				list.put(pixelData);

				{ Attribute a = new CodeStringAttribute(TagFromName.PhotometricInterpretation); a.addValue("MONOCHROME2"); list.put(a); }
				{ Attribute a = new UnsignedShortAttribute(TagFromName.SamplesPerPixel); a.addValue(1); list.put(a); }
				{ Attribute a = new UnsignedShortAttribute(TagFromName.BitsStored); a.addValue(8); list.put(a); }
				{ Attribute a = new UnsignedShortAttribute(TagFromName.BitsAllocated); a.addValue(8); list.put(a); }
				{ Attribute a = new UnsignedShortAttribute(TagFromName.HighBit); a.addValue(7); list.put(a); }
				
				list.remove(dictionary.getTagFromName("PlanarConfiguration"));
				list.remove(dictionary.getTagFromName("UltrasoundColorDataPresent"));
								
				ClinicalTrialsAttributes.addContributingEquipmentSequence(list,true/*retainExistingItems*/,
					new CodedSequenceItem("109103","DCM","Modifying Equipment"),
					"PixelMed",														// Manufacturer
					"PixelMed",														// Institution Name
					"Software Development",											// Institutional Department Name
					"Bangor, PA",													// Institution Address
					null,															// Station Name
					"com.pixelmed.apps.ConvertColorPaletteToGrayscale.main()",		// Manufacturer's Model Name
					null,															// Device Serial Number
					VersionAndConstants.getBuildDate(),								// Software Version(s)
					"Converted "+photometricInterpretation+" color to 8-bit grayscale");

				list.removeMetaInformationHeaderAttributes();
				FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,"OURAETITLE");
				list.write(outputFileName,TransferSyntax.ExplicitVRLittleEndian,true,true);
			}
		}
	}
	
	/**
	 * <p>Read a DICOM image input format file with a Photometric Interpretation of RGB, and from it create a DICOM image of Photometric Interpretation MONOCHROME2.</p>
	 *
	 * @param	arg	two parameters, the inputFile, outputFile
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length == 2) {
				new ConvertColorToGrayscale(arg[0],arg[1]);
			}
			else {
				System.err.println("Error: Incorrect number of arguments");
				System.err.println("Usage: ConvertColorToGrayscale inputFile outputFile");
				System.exit(1);
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
		}
	}
}
