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

import java.awt.Graphics2D;
import java.util.*;
import java.lang.Object;

/**
 * Shard of ice
 * 
 * @author Philip Hanson <phanson@wpi.edu>
 */
public class IceShard extends InteractableObject {

	private boolean pickedUp, doneAction;
	
	public IceShard(GTLevel level) {
		super(level);
		actions.put(0, "pick up");
		
		setImage(level.getImage("resources/images/Shard.png"));
	}

	/* (non-Javadoc)
	 * @see edu.wpi.disco.game.gt.InteractableObject#doAction(int, edu.wpi.disco.game.World, edu.wpi.disco.game.NWayInteraction, edu.wpi.disco.Actor)
	 */
	@Override
	public void doAction(int index, Map<String,Object> world, NWayInteraction interaction, Actor a) {
		if ((index != 0) || doneAction) return;
		doneAction = true;
		HashMap<String, Object> slots = new HashMap<String, Object>();
		slots.put("shard", this);
		interaction.getPlayer().doAction(new ExecuteTaskAction(a, "urn:secrets.wpi.edu:models:Shelter", "ChooseShard", slots));
		interaction.getPlayer().doAction(new ExecuteTaskAction(a, "urn:secrets.wpi.edu:models:Shelter", "PickUpShard", slots));
	}
	
	public void pickUp() {
		pickedUp = true;
	}
	
	public boolean isPickedUp() {
		return pickedUp;
	}
	
	/* (non-Javadoc)
	 * @see edu.wpi.disco.game.gt.InteractableObject#shouldShowActions()
	 */
	@Override
	public boolean shouldShowActions() {
		return !pickedUp && !doneAction && super.shouldShowActions();
	}
	
	/* (non-Javadoc)
	 * @see edu.wpi.disco.game.gt.EmbodiedObject#render(java.awt.Graphics2D)
	 */
	@Override
	public void render(Graphics2D g) {
		if (!pickedUp)
			super.render(g);
	}
	
	/* (non-Javadoc)
	 * @see edu.wpi.disco.game.gt.EmbodiedObject#isWalkable()
	 */
	@Override
	public boolean isWalkable() {
		return pickedUp;
	}

}
