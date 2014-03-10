/* Copyright (c) 2010 Philip Hanson, Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.game.gt;

import edu.wpi.disco.game.NPC;

import java.awt.Color;

/**
 * Convenience class for working with sidekick character
 */
public class GTSidekick extends GTNPC {

	public GTSidekick (NPC npc, GTLevel level) {
		super(npc, level);
		
		setImage(level.getImage("/edu/wpi/disco/game/gt/Sidekick.png"));
		setBgColor(new Color(255, 186, 0));  // orange
		setFgColor(new Color(0, 34, 127));   // blue
	}
	
	@Override
	public String toString () {
		return getNPC().getName();
	}
}
