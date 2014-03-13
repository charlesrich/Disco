/* Copyright (c) 2010 Philip Hanson and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.secrets.objects;

import java.awt.Point;

/**
 * An ice floe or landmass that player and sidekick can navigate
 * 
 * @author Philip Hanson <phanson@wpi.edu>
 * 
 */
public class Area {
	String name;
	Point center;
	IceBlock accessBlock;

	public Area(String name, Point walkToLocation) {
		this.name = name;
		center = (Point) walkToLocation.clone();
	}

	public Point getWalkToLocation() {
		return (Point) center.clone();
	}

	public IceBlock getAccessBlock() {
		return accessBlock;
	}

	public void setIceBlock(IceBlock block) {
		accessBlock = block;
	}

	@Override
	public String toString() {
		return name;
	}
}
