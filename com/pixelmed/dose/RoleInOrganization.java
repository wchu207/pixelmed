/* Copyright (c) 2001-2025, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.ContentItem;
import com.pixelmed.dicom.ContentItemFactory;
import com.pixelmed.dicom.DicomException;

public class RoleInOrganization {
	
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dose/RoleInOrganization.java,v 1.15 2025/01/29 10:58:08 dclunie Exp $";

	private String description;
	
	private RoleInOrganization() {};
	
	private RoleInOrganization(String description) {
		this.description = description;
	};
	
	public static final RoleInOrganization PHYSICIAN = new RoleInOrganization("Physician");
	public static final RoleInOrganization TECHNOLOGIST = new RoleInOrganization("Radiologic Technologist");
	public static final RoleInOrganization RADIATION_PHYSICIST = new RoleInOrganization("Radiation Physicist");
	
	public String toString() { return description; }
	
	public static RoleInOrganization getRoleInOrganization(ContentItem parent) {
		RoleInOrganization found = null;
		ContentItem ci = parent.getNamedChild("DCM","113874");	// "Person Role in Organization"
		if (ci != null
		 && ci instanceof ContentItemFactory.CodeContentItem) {
			CodedSequenceItem conceptCode = ((ContentItemFactory.CodeContentItem)ci).getConceptCode();
			if (conceptCode != null) {
				String csd = conceptCode.getCodingSchemeDesignator();
				if (csd != null) {
					String cv = conceptCode.getCodeValue();
					if (csd.equals("DCM")) {
						if (cv != null) {
							if (cv.equals("121081")) {
								found = RoleInOrganization.PHYSICIAN;
							}
							else if (cv.equals("121083")) {
								found = RoleInOrganization.TECHNOLOGIST;
							}
							else if (cv.equals("121105")) {
								found = RoleInOrganization.RADIATION_PHYSICIST;
							}
						}
					}
					else if (csd.equals("SCT")) {
						if (cv != null) {
							if (cv.equals("309343006")) {
								found = RoleInOrganization.PHYSICIAN;
							}
							else if (cv.equals("159016003")) {
								found = RoleInOrganization.TECHNOLOGIST;
							}
						}
					}
					else if (csd.equals("SRT") || csd.equals("SNM") || csd.equals("99SDM")) {
						if (cv != null) {
							if (cv.equals("J-004E8")) {
								found = RoleInOrganization.PHYSICIAN;
							}
							else if (cv.equals("J-00187")) {
								found = RoleInOrganization.TECHNOLOGIST;
							}
						}
					}
					else if (csd.equals("UMLS")) {
						if (cv != null) {
							if (cv.equals("C2985483")) {
								found = RoleInOrganization.RADIATION_PHYSICIST;
							}
						}
					}
				}
			}
		}
		return found;
	}
	
	public static CodedSequenceItem getCodedSequenceItem(RoleInOrganization role) throws DicomException {
		CodedSequenceItem csi = null;
		if (role != null) {
			if (role.equals(RoleInOrganization.PHYSICIAN)) {
				csi = new CodedSequenceItem("309343006","SCT","Physician");
			}
			else if (role.equals(RoleInOrganization.TECHNOLOGIST)) {
				csi = new CodedSequenceItem("159016003","SCT","Radiologic Technologist");
			}
			else if (role.equals(RoleInOrganization.RADIATION_PHYSICIST)) {
				csi = new CodedSequenceItem("C2985483","UMLS","Radiation Physicist");
			}
		}
		return csi;
	}
	
	public CodedSequenceItem getCodedSequenceItem() throws DicomException {
		return getCodedSequenceItem(this);
	}
}
