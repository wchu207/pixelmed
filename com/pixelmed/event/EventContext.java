/* Copyright (c) 2001-2025, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.event;

import java.rmi.server.UID;

/**
 * @author	dclunie
 */
public class EventContext {

	/***/
	static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/event/EventContext.java,v 1.13 2025/01/29 10:58:08 dclunie Exp $";
	
	/***/
	private UID uid;
	
	/***/
	private String description;
	
	/**
	 */
	public EventContext(String description) {
		uid = new UID();
		this.description=description;
	}
	
	/**
	 * @param	obj
	 */
	public boolean equals(Object obj) {
		return uid.equals(obj);
	}
	
	/**
	 * @return	hash code of context
	 */
	public int hashCode() {
		return uid.hashCode();
	}

	/**
	 * @return	description of context
	 */
	public String toString() {
		return description+"("+uid.toString()+")";
	}
}
