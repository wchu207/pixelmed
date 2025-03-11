/* Copyright (c) 2001-2025, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

public interface BasicOffsetTableAccessor {
	/**
	 * <p>Get the 32 bit array Basic Offset Table.</p>
	 *
	 * @return					the values as an array of int, or null if none
=	 */
	public int[] getBasicOffsetTable();
	
	/**
	 * <p>Set the 32 bit array Basic Offset Table.</p>
	 *
	 * @param	basicOffsetTable
	 */
	public void setBasicOffsetTable(int[] basicOffsetTable);
}

