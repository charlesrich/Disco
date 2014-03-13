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

import java.util.*;
import java.lang.Object;

/**
 * An ice block that can be on land or floating in water.
 * 
 * @author Philip Hanson <phanson@wpi.edu>
 * 
 */
public class IceBlock extends InteractableObject {
	private boolean inWater;
	private boolean pushed = false;
	private Area to;

	public IceBlock(GTLevel level, Area toArea, boolean inWater, boolean tooHeavy) {
		super(level);
		this.to = toArea;
		this.inWater = inWater;
		
		setImage(level.getImage("resources/images/IceBlock.png"));

		actions.put(0, "push");
	}

	public synchronized void pushIntoWater() {
		this.inWater = true;
		this.location.setLocation(-100, -100);
	}

	public boolean isInWater() {
		return inWater;
	}
	
	@Override
	public String toString() {
		return "an ice block";
	}

	
	@Override
	public void doAction(int index, Map<String,Object> world, NWayInteraction interaction, Actor a) {
		switch(index) {
		case 0:
			if (!pushed) {
				pushed = true;
				HashMap<String,Object> slots = new HashMap<String,Object>();
				slots.put("to", to);
				interaction.getPlayer().doAction(
						new ExecuteTaskAction(a, "urn:secrets.wpi.edu:models:IceBlocks", "PushIceBlock", slots)
						);
			}
			break;
		default:
			break;
		}
	}
}
