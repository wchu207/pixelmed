/* Copyright (c) 2001=2024, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.validate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.LineNumberReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ExtractContextGroupsWithTransitiveClosure {

	// see long discussion at "https://stackoverflow.com/questions/1102891/how-to-check-if-a-string-is-numeric-in-java"
	public static boolean isNonEmptyNumericArray(String s) {
		if (s == null) {
			return false;
		}
		else {
			for (char c : s.toCharArray()) {
				if (c < '0' || c > '9') {
					return false;
				}
			}
    	}
		return true;
    }

	protected class CodedConcept {
		String csd;
		String cv;
		String cm;
		
		// equal if same csd and cv regardless of cm (including for subclasses unless overridden)
		
		public boolean equals(Object o) {
			boolean match = false;
			if (o instanceof CodedConcept) {
				match = csd.equals(((CodedConcept)o).csd) && cv.equals(((CodedConcept)o).cv);
			}
			return match;
		}
		
		public int hashCode() {
			return csd.hashCode()+cv.hashCode();
		}
		
		public CodedConcept(String csd,String cv,String cm) {
			this.csd = csd;
			this.cv = cv;
			this.cm = cm;
		}

		public String toString() {
			StringBuffer buf = new StringBuffer();
			buf.append("(");
			buf.append(cv);
			buf.append(",");
			buf.append(csd);
			buf.append(",\"");
			buf.append(cm);
			buf.append("\")");
			return buf.toString();
		}
	}

	protected class CodedConceptInContextGroup extends CodedConcept {
		String sct;
		String umlscui;
		//String propertyTypeCIDForCategory;	// only for CID 7150
		
		public CodedConceptInContextGroup(String csd,String cv,String cm) {
			super(csd,cv,cm);
			this.sct = null;
			this.umlscui = null;
			//this.propertyTypeCIDForCategory = null;
		}
		
		public CodedConceptInContextGroup(String csd,String cv,String cm,String sct,String umlscui) {
			super(csd,cv,cm);
			this.sct = sct;
			this.umlscui = umlscui;
			//this.propertyTypeCIDForCategory = null;
		}
		
		public String toString() {
			StringBuffer buf = new StringBuffer();
			buf.append("(");
			buf.append(cv);
			buf.append(",");
			buf.append(csd);
			buf.append(",\"");
			buf.append(cm);
			buf.append("\")");
			if (sct != null) {
				buf.append("\t sct = ");
				buf.append(sct);
			}
			if (umlscui != null) {
				buf.append("\t umlscui = ");
				buf.append(umlscui);
			}
			//if (propertyTypeCIDForCategory != null) {
			//	buf.append("\t propertyTypeCIDForCategory = ");
			//	buf.append(propertyTypeCIDForCategory);
			//}
			return buf.toString();
		}
	}
	
	// used as key for SortedMap since wanted to control sort order numerically if possible rather than using String (which is final and not extensible)
	protected class ContextGroupIdentifier implements Comparable {
		protected String cid;
		
		protected ContextGroupIdentifier(String cid) {
			this.cid=cid;
		}
		
		// sort numerically if possible
		public int compareTo(Object o) {
			int result = -1;
			if (this.equals(o)) {
				result = 0;
			}
			else if (o instanceof ContextGroupIdentifier) {
				// see long discussion about exceptions and checking is numeric at "https://stackoverflow.com/questions/1102891/how-to-check-if-a-string-is-numeric-in-java"
				try {
					int us = Integer.parseInt(cid);
					int them = Integer.parseInt(((ContextGroupIdentifier)o).cid);
					result = (us == them) ? 0 : ((us < them) ? -1 : 1);
//System.err.println("Numeric comparison of "+us+" and "+them+" results in "+result);
				}
				catch (NumberFormatException e) {
					result = cid.compareTo(((ContextGroupIdentifier)o).cid);
//System.err.println("string comparison of "+cid+" and "+((ContextGroupIdentifier)o).cid+" results in "+result);
				}
			}
			return result;
		}
	}
	
	protected class ContextGroup implements Comparable {
		String cid;
		String name;
		String version;
		String uid;
		String keyword;
		String extensible;
		String fhirkeyword;
		Set<String> includedCIDs = new HashSet<String>();
		Set<CodedConceptInContextGroup> codedConcepts = new HashSet<CodedConceptInContextGroup>();
		ContextGroup transitiveClosure;
		
		// not actually used since using Maps and not Sets
		// sort numerically if possible
		public int compareTo(Object o) {
			int result = -1;
			if (this.equals(o)) {
				result = 0;
			}
			else if (o instanceof ContextGroup) {
				// see long discussion about exceptions and checking is numeric at "https://stackoverflow.com/questions/1102891/how-to-check-if-a-string-is-numeric-in-java"
				try {
					int us = Integer.parseInt(cid);
					int them = Integer.parseInt(((ContextGroup)o).cid);
					result = (us == them) ? 0 : ((us < them) ? -1 : 1);
//System.err.println("Numeric comparison of "+us+" and "+them+" results in "+result);
				}
				catch (NumberFormatException e) {
					result = cid.compareTo(((ContextGroup)o).cid);
//System.err.println("string comparison of "+cid+" and "+((ContextGroup)o).cid+" results in "+result);
				}
			}
			return result;
		}
		
		public ContextGroup(String cid,String name,String version,String uid,String keyword,String extensible,String fhirkeyword) {
			this.cid = cid;
			this.name = name;
			this.version = version;
			this.uid = uid;
			this.keyword = keyword;
			this.extensible = extensible;
			this.fhirkeyword = fhirkeyword;
		}
		
		public String toString() {
			StringBuffer buf = new StringBuffer();
			buf.append("CID ");
			buf.append(cid);
			buf.append(" ");
			buf.append(name);
			buf.append(" ");
			buf.append(version);
			buf.append(" ");
			buf.append(uid);
			buf.append(" ");
			buf.append(keyword);
			buf.append(" ");
			buf.append(extensible);
			buf.append(" ");
			buf.append(fhirkeyword);
			buf.append("\n");
			for (String includedCID : includedCIDs) {
				buf.append("\tInclude ");
				buf.append(includedCID);
				//buf.append(" ");
				//ContextGroup cg = originalContextGroupsByCID.get(includedCID);		// bummer ... don't want to access this variable ... if we need it make separate map of CID to name
				//buf.append(cg == null ? "" : cg.name);
				buf.append("\n");
			}
			for (CodedConceptInContextGroup codedConcept : codedConcepts) {
				buf.append("\t");
				buf.append(codedConcept);
				buf.append("\n");
			}
			return buf.toString();
		}
		
		public ContextGroup getTransitiveClosure(Map<ContextGroupIdentifier,ContextGroup> contextGroupsByCID) {
			if (transitiveClosure == null) {
				transitiveClosure = new ContextGroup(cid,name,version,uid,keyword,extensible,fhirkeyword);
				transitiveClosure.codedConcepts = new HashSet<CodedConceptInContextGroup>();
				transitiveClosure.codedConcepts.addAll(codedConcepts);
				for (String includedCID : includedCIDs) {
					ContextGroup includedCG = contextGroupsByCID.get(new ContextGroupIdentifier(includedCID));
					if (includedCG != null) {
						ContextGroup closure = includedCG.getTransitiveClosure(contextGroupsByCID);
						if (closure != null) {
							transitiveClosure.codedConcepts.addAll(closure.codedConcepts);	// should check for possible loop
						}
					}
					else {
						System.err.println("Error: Cannot find CID "+includedCID+" to include in CID "+cid);
					}
				}
			}
//System.err.println(transitiveClosure);
			return transitiveClosure;
		}
	}
	
	protected void readContextGroupsFile(String filename,Map<ContextGroupIdentifier,ContextGroup> contextGroupsByCID) throws IOException, ParserConfigurationException, SAXException, Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.parse(new File(filename));
		
		Element root = document.getDocumentElement();
		if (root.getTagName().equals("definecontextgroups")) {
			NodeList cgNodes = root.getChildNodes();
			for (int i=0; i<cgNodes.getLength(); ++i) {
				Node cgNode = cgNodes.item(i);
				if (cgNode.getNodeType() == Node.ELEMENT_NODE && ((Element)cgNode).getTagName().equals("definecontextgroup")) {
					String cid = "";
					String name = "";
					String version = "";
					String uid = "";
					String keyword = "";
					String extensible = "";
					String fhirkeyword = "";
					{
						NamedNodeMap attributes = cgNode.getAttributes();
						if (attributes != null) {
							{
								Node attribute = attributes.getNamedItem("cid");
								if (attribute != null) {
									cid = attribute.getTextContent();
								}
							}
							{
								Node attribute = attributes.getNamedItem("name");
								if (attribute != null) {
									name = attribute.getTextContent();
								}
							}
							{
								Node attribute = attributes.getNamedItem("version");
								if (attribute != null) {
									version = attribute.getTextContent();
								}
							}
							{
								Node attribute = attributes.getNamedItem("uid");
								if (attribute != null) {
									uid = attribute.getTextContent();
								}
							}
							{
								Node attribute = attributes.getNamedItem("keyword");
								if (attribute != null) {
									keyword = attribute.getTextContent();
								}
							}
							{
								Node attribute = attributes.getNamedItem("extensible");
								if (attribute != null) {
									extensible = attribute.getTextContent();
								}
							}
							{
								Node attribute = attributes.getNamedItem("fhirkeyword");
								if (attribute != null) {
									fhirkeyword = attribute.getTextContent();
								}
							}
						}
					}
//System.err.println("Have CID "+cid+" "+name);
					ContextGroup cg= new ContextGroup(cid,name,version,uid,keyword,extensible,fhirkeyword);
					contextGroupsByCID.put(new ContextGroupIdentifier(cid),cg);
					NodeList cgContentNodes = cgNode.getChildNodes();
					for (int j=0; j<cgContentNodes.getLength(); ++j) {
						Node cgContentNode = cgContentNodes.item(j);
						if (cgContentNode.getNodeType() == Node.ELEMENT_NODE) {
//System.err.println("Have cgContentNode ELEMENT "+((Element)cgContentNode).getTagName());
							if (((Element)cgContentNode).getTagName().equals("include")) {
								String includedCID = "";
								{
									NamedNodeMap attributes = cgContentNode.getAttributes();
									if (attributes != null) {
										{
											Node attribute = attributes.getNamedItem("cid");
											if (attribute != null) {
												includedCID = attribute.getTextContent();
											}
										}
									}
								}
//System.err.println("Have includedCID "+includedCID+" "+name);
								cg.includedCIDs.add(includedCID);
							}
							else if (((Element)cgContentNode).getTagName().equals("contextgroupcode")) {
								String csd = "";
								String cv = "";
								String cm = "";
								String sct = "";
								String umlscui = "";
								String propertyTypeCIDForCategory = null;
								{
									NamedNodeMap attributes = cgContentNode.getAttributes();
									if (attributes != null) {
										{
											Node attribute = attributes.getNamedItem("csd");
											if (attribute != null) {
												csd = attribute.getTextContent();
											}
										}
										{
											Node attribute = attributes.getNamedItem("cv");
											if (attribute != null) {
												cv = attribute.getTextContent();
											}
										}
										{
											Node attribute = attributes.getNamedItem("cm");
											if (attribute != null) {
												cm = attribute.getTextContent();
											}
										}
										{
											Node attribute = attributes.getNamedItem("sct");
											if (attribute != null) {
												sct = attribute.getTextContent();
											}
										}
										{
											Node attribute = attributes.getNamedItem("umlscui");
											if (attribute != null) {
												umlscui = attribute.getTextContent();
											}
										}
										{
											Node attribute = attributes.getNamedItem("propertyTypeCIDForCategory");
											if (attribute != null) {
												propertyTypeCIDForCategory = attribute.getTextContent();
											}
										}
									}
								}
								CodedConceptInContextGroup codedConcept = new CodedConceptInContextGroup(csd,cv,cm,sct,umlscui);
								cg.codedConcepts.add(codedConcept);
								//if (propertyTypeCIDForCategory != null) {
								//	codedConcept.propertyTypeCIDForCategory = propertyTypeCIDForCategory;
								//}
								//String key = codedConcept.csd+"_"+codedConcept.cv;
								//allCodedConceptsEncounteredInContextGroupsByCSDAndCV.add(key,codedConcept);
							}
						}
					}
				}
			}
		}
		else {
			throw new Exception("Expected definecontextgroups element got "+root.getTagName());
		}
		
//		for (ContextGroup cg : contextGroupsByCID.values()) {
//System.err.println(cg);
//		}
	}
	
	protected void createNamedAttributeAndAppendToElement(Document document,Node element,String attributeName,String attributeValue) {
		Attr attr = document.createAttribute(attributeName);
		attr.setValue(attributeValue);
		element.getAttributes().setNamedItem(attr);
	}
	
	protected void writeContextGroupsFile(String outputfile,List<String> wanted,Map<ContextGroupIdentifier,ContextGroup> contextGroupsByCID) throws IOException, ParserConfigurationException, TransformerConfigurationException, TransformerException {

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(false);
		DocumentBuilder db = dbf.newDocumentBuilder();

		Document document = db.newDocument();

		Node definecontextgroupsElement = document.createElement("definecontextgroups");
		//createNamedAttributeAndAppendToElement(document,definecontextgroupsElement,"xml:lang","en-US");
		createNamedAttributeAndAppendToElement(document,definecontextgroupsElement,"xmlns:xsi","http://www.w3.org/2001/XMLSchema-instance");
		createNamedAttributeAndAppendToElement(document,definecontextgroupsElement,"xsi:noNamespaceSchemaLocation","http://www.pixelmed.com/schemas/contextgroups.xsd");

		for (ContextGroup cg : contextGroupsByCID.values()) {
			if (wanted.contains(cg.cid)) {
				Node definecontextgroupElement = document.createElement("definecontextgroup");
				//createNamedAttributeAndAppendToElement(document,definecontextgroupElement,"xml:lang","en-US");
				createNamedAttributeAndAppendToElement(document,definecontextgroupElement,"cid",cg.cid);
				createNamedAttributeAndAppendToElement(document,definecontextgroupElement,"name",cg.keyword);				// don't need actual name with punctuation
				createNamedAttributeAndAppendToElement(document,definecontextgroupElement,"extensible",cg.extensible);
				createNamedAttributeAndAppendToElement(document,definecontextgroupElement,"version",cg.version);
				//createNamedAttributeAndAppendToElement(document,definecontextgroupElement,"uid",cg.uid);

				for (CodedConceptInContextGroup concept : cg.codedConcepts) {
					{
						Node contextgroupcodeElement = document.createElement("contextgroupcode");

						createNamedAttributeAndAppendToElement(document,contextgroupcodeElement,"csd",concept.csd);
						createNamedAttributeAndAppendToElement(document,contextgroupcodeElement,"cv",concept.cv);
						createNamedAttributeAndAppendToElement(document,contextgroupcodeElement,"cm",concept.cm);

						definecontextgroupElement.appendChild(contextgroupcodeElement);
					}
					// ?? if SCT, need to look up SRT equivalent from Annex O and if not already in context group (because PS3.16 lists it in SRT column) add as well ???? :(
				}

				definecontextgroupsElement.appendChild(definecontextgroupElement);
			}
		}

		document.appendChild(definecontextgroupsElement);

		DOMSource source = new DOMSource(document);
		StreamResult result = new StreamResult(new FileOutputStream(outputfile));
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		Properties outputProperties = new Properties();
		outputProperties.setProperty(OutputKeys.METHOD,"xml");
		outputProperties.setProperty(OutputKeys.INDENT,"yes");
		outputProperties.setProperty("{http://xml.apache.org/xslt}indent-amount", "4");  // https://stackoverflow.com/questions/1384802/java-how-to-indent-xml-generated-by-transformer
		outputProperties.setProperty(OutputKeys.ENCODING,"UTF-8");	// the default anyway
		transformer.setOutputProperties(outputProperties);
		transformer.transform(source,result);
	}
	
	protected List<String> readContextGroupsWantedFile(String wantedfile) throws IOException {
		// flat list of the form "nnnn" where nnnn is the cg.cid (e.g. "100" without the surrounding quotes, and doesn't need to be numeric)
		List<String> wanted = new ArrayList<String>();
		LineNumberReader r = new LineNumberReader(new BufferedReader(new FileReader(wantedfile)));
		String line;
		while ((line=r.readLine()) != null) {
			if (line.length() > 0) {
				wanted.add(line);
			}
		}
		return wanted;
	}
	
	protected void performTransitiveClosure(Map<ContextGroupIdentifier,ContextGroup> sourceContextGroupsByCID,Map<ContextGroupIdentifier,ContextGroup> targetContextGroupsByCID) {
		for (ContextGroup cg : sourceContextGroupsByCID.values()) {
			ContextGroup closedContextGroup = cg.getTransitiveClosure(sourceContextGroupsByCID);
			targetContextGroupsByCID.put(new ContextGroupIdentifier(cg.cid),closedContextGroup);
		}
	}

	protected SortedMap<ContextGroupIdentifier,ContextGroup> originalContextGroupsByCID = new TreeMap<ContextGroupIdentifier,ContextGroup>();
	protected SortedMap<ContextGroupIdentifier,ContextGroup> closedContextGroupsByCID = new TreeMap<ContextGroupIdentifier,ContextGroup>();

	public ExtractContextGroupsWithTransitiveClosure(
			String standardcontextgroupsinputfile,
			String extendedcontextgroupsinputfile,
			String contextgroupswantedinputfile,
			String outputfile
			) throws IOException, ParserConfigurationException, SAXException, Exception {
			
		readContextGroupsFile(standardcontextgroupsinputfile,originalContextGroupsByCID);
		readContextGroupsFile(extendedcontextgroupsinputfile,originalContextGroupsByCID);
		performTransitiveClosure(originalContextGroupsByCID,closedContextGroupsByCID);
		List<String> wanted = readContextGroupsWantedFile(contextgroupswantedinputfile);	// needs to contain both standard and extended context groups
		writeContextGroupsFile(outputfile,wanted,closedContextGroupsByCID);
	}
	
	public static void main(String arg[]) {
		try {
			new ExtractContextGroupsWithTransitiveClosure(arg[0],arg[1],arg[2],arg[3]);
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

}
