/* Copyright (c) 2001-2025, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.validate;

import com.pixelmed.dicom.*;

// JAXP packages
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.Document;

import java.io.*;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>The {@link DicomSRValidator DicomSRValidator} class is
 * for validating SR instances against the standard IOD for the corresponding SOP Class,
 * as well as validating recognized templates and context groups.</p>
 *
 * <p>The following IODs and SOP Classes are currently supported:</p>
 * <ul>
 *   <li> Basic Text SR</li>
 *   <li> Enhanced SR</li>
 *   <li> Comprehensive SR</li>
 *   <li> Comprehensive 3D SR</li>
 *   <li> Procedure Log</li>
 *   <li> Mammography CAD SR</li>
 *   <li> Key Object Selection Document</li>
 *   <li> Chest CAD SR</li>
 *   <li> X-Ray Radiation Dose SR</li>
 *   <li> Radiopharmaceutical Radiation Dose SR</li>
 *   <li> Acquisition Context SR</li>
 * </ul>
 *
 * <p>The following standard root Templates are currently supported:</p>
 * <ul>
 *   <li> TID 1500 - Measurement Report</li>
 *   <li> TID 2000 - Basic Diagnostic Imaging Report</li>
 *   <li> TID 2010 - Key Object Selection</li>
 *   <li> TID 3900 - CT/MR Cardiovascular Analysis Report</li>
 *   <li> TID 4000 - Mammography CAD Document Root</li>
 *   <li> TID 4200 - Breast Imaging Report</li>
 *   <li> TID 5100 - Vascular Ultrasound Report</li>
 *   <li> TID 5200 - Echocardiography Procedure Report</li>
 *   <li> TID 5220 - Pediatric Fetal and Congenital Cardiac Ultrasound Reports</li>
 *   <li> TID 8101 - Preclinical Small Animal Image Acquisition Context</li>
 *   <li> TID 10001 - Projection X-Ray Radiation Dose</li>
 *   <li> TID 10011 - CT Radiation Dose</li>
 *   <li> TID 10021 - Radiopharmaceutical Radiation Dose</li>
 * </ul>
 *
 * <p>In addition, some private non-standard templates are supported, such as the IHE Additional Teaching File Information template.</p>
 *
 * <p>Template recognition is either by the Root Template that is specified for an IOD and SOP Class,
 * or the presence of an encoded TemplateIdentifier Attribute Value on the Root CONTAINER Content Item.</p>
 *
 * <p>Content Item constraints checks are also performed on the permissible Value Types and Relationship Types for the IOD.</p>
 *
 * <p>The following command line options are supported by the {@link #main main} method (if used as a command line utility):</p>
 * <ul>
 *   <li> <code>describe</code> - turns on description of the details of the validation procedure step by step</li>
 *   <li> <code>donotcheckcodemeaning</code> - turns off checking code meanings against the expected values in context groups and templates</li>
 *   <li> <code>donotmatchcase</code> - turns off matching the case of code meanings when validating them against the expected values in context groups and templates</li>
 *   <li> <code>donotcheckdeprecatedcodingscheme</code> - turns off warnings about use deprecated coding schemes (e.g., SRT instead of SCT)</li>
 *   <li> <code>checkambiguoustemplate</code> - turns on warnings about ambiguous inclusion of templates (different templates match same content)</li>
 *   <li> <code>checkcontentitemorder</code> - check if content item order does not match order in templates and report error if order significant for that template or warning otherwise</li>
 *   <li> <code>checktemplateid</code> - check if encoded Template ID on CONTAINERs match expected template</li>
 *   <li> <code>donotcheckcontentitemsnotintemplate</code> - turns off warnings about content items not recognized as being in a template</li>
 * </ul>
 *
 * <p>These command line options correspond to the parameters of the {@link #validate validate(String filename,boolean,boolean,boolean,boolean,boolean,boolean,boolean)} method.</p>
 *
 * <p>The class is typically used by reading the list of attributes that comprise an object, validating them
 * and displaying the resulting string results to the user on the standard output, in a dialog
 * box or whatever. The basic implementation of the {@link #main main} method is as follows:</p>
 *
 * <pre>
 * 	AttributeList list = new AttributeList();
 * 	list.read(arg[0],null,true,true);
 * 	DicomSRValidator validator = new DicomSRValidator();
 * 	System.err.print(validator.validate(list));
 * </pre>
 *
 * @see com.pixelmed.dicom.AttributeList
 *
 * @author	dclunie
 */
public class DicomSRValidator {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/validate/DicomSRValidator.java,v 1.29 2025/02/11 19:35:51 dclunie Exp $";

	/***/
	private Transformer transformerPass1;
	private Transformer transformerPass2;
	
	private class OurURIResolver implements URIResolver {
	
		String foundItems;
		
		/**
		 * @param	href
		 * @param	base
		 */
		public Source resolve(String href,String base) throws TransformerException {
//System.err.println("OurURIResolver.resolve() href="+href+" base="+base);
			StreamSource streamSource = null;
			if (href.equals("FoundItems.xml")) {			// this is the name that is hard-wired in CheckSRContentItemsUsed.xsl
				streamSource = new StreamSource(new StringReader(foundItems));
			}
			else {
				streamSource = new StreamSource(DicomSRValidator.class.getResourceAsStream("/com/pixelmed/validate/"+href));
			}
			return streamSource;
		}
		
		public void setFoundItems(String foundItems) {
			this.foundItems = foundItems;
		}
	}
	
	OurURIResolver ourURIResolver = new OurURIResolver();
	
	/**
	 * <p>Whether or not to describe the details of the validation procedure step by step.</p>
	 *
	 * <p>Default after construction is not to.</p>
	 *
	 * @param	option		true if the steps are to be described
	 */
	public void setOptionDescribeChecking(boolean option) {
		transformerPass1.setParameter("optionDescribeChecking",option ? "T" : "F");
	}
	
	/**
	 * <p>Whether or not to check the code meanings against the expected values in context groups and templates.</p>
	 *
	 * <p>Default after construction is true, i.e., to check.</p>
	 *
	 * @param	option		true if matching is to be case sensitive
	 */
	public void setOptionCheckCodeMeaning(boolean option) {
		transformerPass1.setParameter("optionCheckCodeMeaning",option ? "T" : "F");
	}
	
	/**
	 * <p>Whether or not to match the case of code meanings when validating them against the expected values in context groups and templates.</p>
	 *
	 * <p>Default after construction is true, i.e., to be case sensitive.</p>
	 *
	 * @param	option		true if matching is to be case sensitive
	 */
	public void setOptionMatchCaseOfCodeMeaning(boolean option) {
		transformerPass1.setParameter("optionMatchCaseOfCodeMeaning",option ? "T" : "F");
	}
	
	/**
	 * <p>Whether or not to warn about the use of deprecated coding schemes.</p>
	 *
	 * <p>Default after construction is true, i.e., to check.</p>
	 *
	 * @param	option		true if checking
	 */
	public void setOptionCheckDeprecatedCodingScheme(boolean option) {
		transformerPass1.setParameter("optionCheckDeprecatedCodingScheme",option ? "T" : "F");
	}

	/**
	 * <p>Whether or not to check if encoded Template ID on CONTAINERs match expected template.</p>
	 *
	 * <p>May emit spurious warnings if template invocation is ambiguous (different templates match same content).</p>
	 *
	 * <p>The explicitly encoded Template ID is NOT used to constrain template matching.</p>
	 *
	 * <p>Default after construction is false, i.e., not to check.</p>
	 *
	 * @param	option		true if checking
	 */
	public void setOptionCheckTemplateID(boolean option) {
		transformerPass1.setParameter("optionCheckTemplateID",option ? "T" : "F");
	}
	
	/**
	 * <p>Whether or not to check and warn about ambiguous inclusion of templates (different templates match same content).</p>
	 *
	 * <p>Default after construction is false, i.e., not to check.</p>
	 *
	 * @param	option		true if checking
	 */
	public void setOptionCheckAmbiguousTemplate(boolean option) {
		transformerPass2.setParameter("optionCheckAmbiguousTemplate",option ? "T" : "F");
	}
	
	/**
	 * <p>Whether or not to check and warn about content item order not matching order in templates.</p>
	 *
	 * <p>The check reports errors if order deemed significant in template definition otherwise warnings.</p>
	 *
	 * <p>May emit spurious warnings if template invocation is ambiguous (different templates match same content) and content item order is different in any of the matching templates.</p>
	 *
	 * <p>Default after construction is false, i.e., not to check.</p>
	 *
	 * @param	option		true if checking
	 */
	public void setOptionCheckContentItemOrder(boolean option) {
		transformerPass2.setParameter("optionCheckContentItemOrder",option ? "T" : "F");
	}
	
	/**
	 * <p>Whether or not to check and warn about content items not recognized as being in a template.</p>
	 *
	 * <p>Default after construction is true, i.e., to report.</p>
	 *
	 * @param	option		true if reporting
	 */
	public void setOptionReportContentItemsNotInTemplate(boolean option) {
		transformerPass2.setParameter("optionReportContentItemsNotInTemplate",option ? "T" : "F");
	}
	
	/**
	 * <p>Create an instance of validator.</p>
	 *
	 * <p>Once created, a validator may be reused for as many validations as desired.</p>
	 *
	 * @throws	javax.xml.transform.TransformerConfigurationException
	 */
	public DicomSRValidator() throws javax.xml.transform.TransformerConfigurationException {
		TransformerFactory tf = TransformerFactory.newInstance();
		tf.setURIResolver(ourURIResolver);					// this helps us find the common rules in the jar file, and the foundItems in the second pass
		
		transformerPass1 = tf.newTransformer(new StreamSource(DicomSRValidator.class.getResourceAsStream("/com/pixelmed/validate/"+"DicomSRDescriptionsCompiled.xsl")));
		setOptionDescribeChecking(false);
		setOptionMatchCaseOfCodeMeaning(true);
		setOptionCheckTemplateID(false);
		
		transformerPass2 = tf.newTransformer(new StreamSource(DicomSRValidator.class.getResourceAsStream("/com/pixelmed/validate/"+"CheckSRContentItemsUsed.xsl")));
		setOptionCheckAmbiguousTemplate(false);
		setOptionCheckContentItemOrder(false);
		setOptionReportContentItemsNotInTemplate(true);
	}
	
	/**
	 * <p>Perform the first pass of the validation.</p>
	 *
	 * <p>Checks the document against the stylesheet and tracks which nodes matched templates.</p>
	 *
	 * @param	inputDocument		the XML representation of the DICOM SR instance to be validated
	 * @return						a string describing the results of the first pass of the validation
	 * @throws					javax.xml.parsers.ParserConfigurationException
	 * @throws					javax.xml.transform.TransformerException
	 * @throws					java.io.UnsupportedEncodingException
	 */
	protected String validateFirstPass(Document inputDocument) throws
			javax.xml.parsers.ParserConfigurationException,
			javax.xml.transform.TransformerException,
			java.io.UnsupportedEncodingException {

		Source inputSource = new DOMSource(inputDocument);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		StreamResult outputResult = new StreamResult(outputStream);
		transformerPass1.transform(inputSource,outputResult);
		
		return outputStream.toString("UTF-8");
	}
	
	/**
	 * <p>Perform the second pass of the validation.</p>
	 *
	 * <p>Checks for unused content items.</p>
	 *
	 * @param	inputDocument		the XML representation of the DICOM SR instance to be validated
	 * @param	firstOutputString	the text output of the first validation pass that contains a list of items found in the first pass
	 * @return						a string describing the results of the validation
	 * @throws					javax.xml.parsers.ParserConfigurationException
	 * @throws					javax.xml.transform.TransformerException
	 * @throws					java.io.UnsupportedEncodingException
	 * @throws					java.io.IOException
	 */
	protected String validateSecondPass(Document inputDocument,String firstOutputString) throws
			javax.xml.parsers.ParserConfigurationException,
			javax.xml.transform.TransformerException,
			java.io.UnsupportedEncodingException,
			java.io.IOException {
		
		// parse the output of the first pass and seperate the report from the list of found content items ...
		
		LineNumberReader firstOutputStringReader = new LineNumberReader(new StringReader(firstOutputString));
		StringBuffer reportOutputBuffer = new StringBuffer();
		StringBuffer foundItemsBuffer = new StringBuffer();

		foundItemsBuffer.append("<founditems>\n");
		String lineOfText;
		while ((lineOfText = firstOutputStringReader.readLine()) != null) {
			if (lineOfText.trim().startsWith("<item")) {		// inserted by CommonDicomSRValidationRules.xsl if optionEmbedMatchedLocationsInOutputWithElement set to 'item'
				foundItemsBuffer.append(lineOfText);			// with an attribute named 'location'
				foundItemsBuffer.append("\n");
			}
			else {
				reportOutputBuffer.append(lineOfText);
				reportOutputBuffer.append("\n");
			}
		}
		foundItemsBuffer.append("</founditems>\n");

		String foundItems = foundItemsBuffer.toString();
//System.err.print(foundItems);

		// now perform a second pass of the input document to report unused content items ...
		
		ourURIResolver.setFoundItems(foundItems);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		transformerPass2.transform(new DOMSource(inputDocument),new StreamResult(outputStream));
		
		String secondOutputString =  outputStream.toString("UTF-8");
//System.err.print(secondOutputString);
		reportOutputBuffer.append(secondOutputString);
		reportOutputBuffer.append("IOD validation complete\n");
		
		return reportOutputBuffer.toString();
	}

	/**
	 * <p>Validate a DICOM SR instance against the standard IOD for the appropriate SOP Class and templates.</p>
	 *
	 * @param	list							the list of attributes comprising the DICOM SR instance to be validated
	 * @param	describe						whether or not to describe the details of the validation procedure step by step
	 * @param	checkCodeMeaning				whether or not to check the code meanings against the expected values in context groups and templates
	 * @param	checkMatchingCaseOfCodeMeaning	whether or not to match the case of code meanings when validating them against the expected values in context groups and templates
	 * @param	checkDeprecatedCodingScheme		whether or not to warn about the use of deprecated coding schemes
	 * @param	checkAmbiguousTemplate			whether or not to check and warn about ambiguous inclusion of templates (different templates match same content)
	 * @param	checkContentItemOrder			whether or not to check and report content item order not matching order in templates
	 * @param	checkTemplateID					whether or not to check if encoded Template ID on CONTAINERs match expected template
	 * @param	checkContentItemsNotInTemplate	whether or not to warn about content items not recognized as being in a template
	 * @return									a string describing the results of the validation
	 * @throws	javax.xml.parsers.ParserConfigurationException
	 * @throws	javax.xml.transform.TransformerException
	 * @throws	java.io.UnsupportedEncodingException
	 * @throws	java.io.IOException
	 */
	public String validate(AttributeList list,boolean describe,boolean checkCodeMeaning,boolean checkMatchingCaseOfCodeMeaning,boolean checkDeprecatedCodingScheme,boolean checkAmbiguousTemplate,boolean checkContentItemOrder,boolean checkTemplateID,boolean checkContentItemsNotInTemplate) throws
			javax.xml.parsers.ParserConfigurationException,
			javax.xml.transform.TransformerException,
			java.io.UnsupportedEncodingException,
			java.io.IOException {
		
		setOptionDescribeChecking(describe);
		setOptionCheckCodeMeaning(checkCodeMeaning);
		setOptionMatchCaseOfCodeMeaning(checkMatchingCaseOfCodeMeaning);
		setOptionCheckDeprecatedCodingScheme(checkDeprecatedCodingScheme);
		setOptionCheckAmbiguousTemplate(checkAmbiguousTemplate);
		setOptionCheckContentItemOrder(checkContentItemOrder);
		setOptionCheckTemplateID(checkTemplateID);
		setOptionReportContentItemsNotInTemplate(checkContentItemsNotInTemplate);
		Document inputDocument = new XMLRepresentationOfStructuredReportObjectFactory().getDocument(list);
		return validateSecondPass(inputDocument,validateFirstPass(inputDocument));
	}

	/**
	 * <p>Validate a DICOM SR instance against the standard IOD for the appropriate SOP Class and templates.</p>
	 *
	 * <p>Does not describe the details of the validation procedure step by step, and does match the case of code meanings when validating them against the expected values in context groups and templates.</p>
	 *
	 * @param	list	the list of attributes comprising the DICOM SR instance to be validated
	 * @return			a string describing the results of the validation
	 * @throws	javax.xml.parsers.ParserConfigurationException
	 * @throws	javax.xml.transform.TransformerException
	 * @throws	java.io.UnsupportedEncodingException
	 * @throws	java.io.IOException
	 */
	public String validate(AttributeList list) throws
			javax.xml.parsers.ParserConfigurationException,
			javax.xml.transform.TransformerException,
			java.io.UnsupportedEncodingException,
			java.io.IOException {
		
		return validate(list,false/*describe*/,true/*checkCodeMeaning*/,true/*checkMatchingCaseOfCodeMeaning*/,true/*checkDeprecatedCodingScheme*/,false/*checkAmbiguousTemplate*/,false/*checkContentItemOrder*/,false/*checkTemplateID*/,true/*checkContentItemsNotInTemplate*/);
	}

	/**
	 * <p>Validate a DICOM SR instance against the standard IOD for the appropriate SOP Class and templates.</p>
	 *
	 * @param	filename						the DICOM SR instance to be validated
	 * @param	describe						whether or not to describe the details of the validation procedure step by step
	 * @param	checkCodeMeaning				whether or not to check the code meanings gainst the expected values in context groups and templates
	 * @param	checkMatchingCaseOfCodeMeaning	whether or not to match the case of code meanings when validating them against the expected values in context groups and templates
	 * @param	checkDeprecatedCodingScheme		whether or not to warn about the use of deprecated coding schemes
	 * @param	checkAmbiguousTemplate			whether or not to check and warn about ambiguous inclusion of templates (different templates match same content)
	 * @param	checkContentItemOrder			whether or not to check and report content item order not matching order in templates
	 * @param	checkTemplateID					whether or not to check if encoded Template ID on CONTAINERs match expected template
	 * @param	checkContentItemsNotInTemplate	whether or not to warn about content items not recognized as being in a template
	 * @return									a string describing the results of the validation
	 * @throws	javax.xml.parsers.ParserConfigurationException
	 * @throws	javax.xml.transform.TransformerException
	 * @throws	java.io.UnsupportedEncodingException
	 * @throws	java.io.IOException
	 * @throws	DicomException
	 */
	public String validate(String filename,boolean describe,boolean checkCodeMeaning,boolean checkMatchingCaseOfCodeMeaning,boolean checkDeprecatedCodingScheme,boolean checkAmbiguousTemplate,boolean checkContentItemOrder,boolean checkTemplateID,boolean checkContentItemsNotInTemplate) throws
			javax.xml.parsers.ParserConfigurationException,
			javax.xml.transform.TransformerException,
			java.io.UnsupportedEncodingException,
			java.io.IOException,
			DicomException {
		
		AttributeList list = new AttributeList();
		list.read(filename);
		return validate(list,describe,checkCodeMeaning,checkMatchingCaseOfCodeMeaning,checkDeprecatedCodingScheme,checkAmbiguousTemplate,checkContentItemOrder,checkTemplateID,checkContentItemsNotInTemplate);
	}

	/**
	 * <p>Validate a DICOM SR instance against the standard IOD for the appropriate SOP Class and templates.</p>
	 *
	 * <p>Does not describe the details of the validation procedure step by step, and does match the case of code meanings when validating them against the expected values in context groups and templates.</p>
	 *
	 * @param	filename	the DICOM SR instance to be validated
	 * @return				a string describing the results of the validation
	 * @throws	javax.xml.parsers.ParserConfigurationException
	 * @throws	javax.xml.transform.TransformerException
	 * @throws	java.io.UnsupportedEncodingException
	 * @throws	java.io.IOException
	 * @throws	DicomException
	 */
	public String validate(String filename) throws
			javax.xml.parsers.ParserConfigurationException,
			javax.xml.transform.TransformerException,
			java.io.UnsupportedEncodingException,
			java.io.IOException,
			DicomException {
		
		return validate(filename,false/*describe*/,true/*checkCodeMeaning*/,true/*checkMatchingCaseOfCodeMeaning*/,true/*checkDeprecatedCodingScheme*/,false/*checkAmbiguousTemplate*/,false/*checkContentItemOrder*/,false/*checkTemplateID*/,true/*checkContentItemsNotInTemplate*/);
	}

	/**
	 * <p>Read the DICOM file specified on the command line and validate it against the standard IOD for the appropriate storage SOP Class.</p>
	 *
	 * <p>The result of the validation is printed to the standard output.</p>
	 *
	 * @param	arg	optionally -describe, -donotcheckcodemeaning, -donotmatchcase, -donotcheckdeprecatedcodingscheme, -checkambiguoustemplate, -checkcontentitemorder, -checktemplateid, then the name of the file containing the DICOM SR instance to be validated
	 */
	public static void main(String arg[]) {
		try {
			DicomSRValidator validator = new DicomSRValidator();
			boolean describe = false;
			boolean checkCodeMeaning = true;
			boolean checkMatchingCaseOfCodeMeaning = true;
			boolean checkDeprecatedCodingScheme = true;
			boolean checkAmbiguousTemplate = false;
			boolean checkContentItemOrder = false;
			boolean checkTemplateID = false;
			boolean checkContentItemsNotInTemplate = true;
			List<String> argList = new ArrayList<String>();
			for (String a : arg) {
				String cleanArg = a.toLowerCase().trim();
				if (cleanArg.equals("-describe")) {
					describe = true;
				}
				else if (cleanArg.equals("-donotcheckcodemeaning")) {
					checkCodeMeaning = false;
				}
				else if (cleanArg.equals("-donotmatchcase")) {
					checkMatchingCaseOfCodeMeaning = false;
				}
				else if (cleanArg.equals("-donotcheckdeprecatedcodingscheme")) {
					checkDeprecatedCodingScheme = false;
				}
				else if (cleanArg.equals("-checkambiguoustemplate")) {
					checkAmbiguousTemplate = true;
				}
				else if (cleanArg.equals("-checkcontentitemorder")) {
					checkContentItemOrder = true;
				}
				else if (cleanArg.equals("-checktemplateid")) {
					checkTemplateID = true;
				}
				else if (cleanArg.equals("-donotcheckcontentitemsnotintemplate")) {
					checkContentItemsNotInTemplate = false;
				}
				else {
					argList.add(a);
				}
			}
			if (argList.size() == 1) {
				System.out.print(validator.validate(argList.get(0),describe,checkCodeMeaning,checkMatchingCaseOfCodeMeaning,checkDeprecatedCodingScheme,checkAmbiguousTemplate,checkContentItemOrder,checkTemplateID,checkContentItemsNotInTemplate));
			}
			else {
				System.err.print("Usage: java com.pixelmed.validate.DicomSRValidator [-describe] [-donotcheckcodemeaning] [-donotmatchcase] [-donotcheckdeprecatedcodingscheme] [-checkambiguoustemplate] [-checkcontentitemorder] [-checktemplateid] [-donotcheckcontentitemsnotintemplate] filename");
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
	}
}

