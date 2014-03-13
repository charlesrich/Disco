/* Copyright (c) 2010 Philip Hanson, Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.game.gt;

import edu.wpi.disco.game.NPC;

import java.awt.*;

public class GTNPC extends EmbodiedObject implements ICarry {
	private final NPC npc;
	private Point destination;
	private InteractableObject carried;
	
	/**
	 * Background and foreground colors for NPC dialogue 
	 */
	private Color bgColor, fgColor;

	public GTNPC (NPC npc, GTLevel level) {
		super(level);
		this.npc = npc;
		bgColor = Color.WHITE;
		fgColor = Color.BLACK;
		movable = true;
		carried = null;
		
		setTileSize(20);
	}

	public NPC getNPC () {
		return npc;
	}

	public void walkTo (Point to) {
		destination = (Point) to.clone();
	}
	
	public void setDestination (Point p) {
		destination = p;
	}
	
	public Point getDestination () {
		return destination;
	}
	
	public void setBgColor (Color color) {
		bgColor = color;
	}
	
	public Color getBgColor () {
		return bgColor;
	}
	
	public void setFgColor( Color color) {
		fgColor = color;
	}
	
	public Color getFgColor () {
		return fgColor;
	}

	@Override
	public InteractableObject dropCarriedObject () {
		InteractableObject tmp = carried;
		carried = null;
		return tmp;
	}

	@Override
	public InteractableObject getCarriedObject () {
		return carried;
	}

	@Override
	public boolean isCarrying () {
		return (carried != null);
	}

	@Override
	public void pickUp (InteractableObject o) {
		carried = o;
	}
}
