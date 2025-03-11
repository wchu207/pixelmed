/* Copyright (c) 2001-2025, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display.event;

import com.pixelmed.event.Event;
import com.pixelmed.event.EventContext;

/**
 * @author	dclunie
 */
public class WindowLinearCalculationChangeEvent extends Event {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/event/WindowLinearCalculationChangeEvent.java,v 1.11 2025/01/29 10:58:08 dclunie Exp $";

	private String calculation;

	/***/
	public static final String exactCalculation = "EXACT";
	/***/
	public static final String dicomCalculation = "DICOM";

	/**
	 * @param	eventContext
	 * @param	calculation
	 */
	public WindowLinearCalculationChangeEvent(EventContext eventContext,String calculation) {
		super(eventContext);
		this.calculation=calculation;
//System.err.println("WindowLinearCalculationChangeEvent() "+calculation);
	}

	/***/
	public String getCalculation() { return calculation; }

	/***/
	public boolean isExactCalculation() { return calculation.equals(exactCalculation); }

	/***/
	public boolean isDicomCalculation() { return calculation.equals(dicomCalculation); }
}

