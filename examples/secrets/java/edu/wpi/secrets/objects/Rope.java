/* Copyright (c) 2010 Philip Hanson and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.secrets.objects;

import edu.wpi.disco.Actor;
import edu.wpi.disco.game.*;
import edu.wpi.disco.game.actions.ExecuteTaskAction;
import edu.wpi.disco.game.gt.*;

import java.awt.Color;
import java.util.*;

/**
 * Rope for Ice Blocks level to pull player across
 * 
 * @author Philip Hanson <phanson@wpi.edu>
 */
public class Rope extends InteractableObject {
	private boolean thrown = false;
	private boolean actionPerformed = false;
	
	public Rope(GTLevel level) {
		super(level);
		
		Color mask = null;
		if (level.bsLoader != null) {
			mask = level.bsLoader.getMaskColor();
			level.setMaskColor(Color.WHITE);
		}
		setImage(level.getImage("resources/images/Rope.png", true));
		if (level.bsLoader != null) {
			level.setMaskColor(mask);
		}

		actions.put(0, "throw rope");
	}
	
	public void setThrown(boolean thrown) {
		this.thrown = thrown;
	}
	
	public boolean isThrown() {
		return thrown;
	}

	/* (non-Javadoc)
	 * @see edu.wpi.disco.game.gt.InteractableObject#doAction(int, edu.wpi.disco.game.World, edu.wpi.disco.game.NWayInteraction, edu.wpi.disco.Actor)
	 */
	@Override
	public void doAction(int index, Map<String,Object> world, NWayInteraction interaction, Actor a) {
		if ((index == 0) && !actionPerformed) {
			actionPerformed = true;
			interaction.getPlayer().doAction(
					new ExecuteTaskAction(a, "urn:secrets.wpi.edu:models:IceBlocks", "ThrowRope", null)
					);
		}
	}
	
	@Override
	public boolean shouldShowActions() {
		return false;
	}
}
