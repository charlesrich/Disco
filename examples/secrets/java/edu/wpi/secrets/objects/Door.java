/* Copyright (c) 2010 Philip Hanson and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.secrets.objects;

import com.golden.gamedev.object.sprite.AdvanceSprite;

import edu.wpi.disco.Actor;
import edu.wpi.disco.game.*;
import edu.wpi.disco.game.actions.ExecuteTaskAction;
import edu.wpi.disco.game.gt.*;

import java.awt.Point;
import java.util.*;

/**
 * A door that can only be unlocked from the right side
 * 
 * @author Philip Hanson <phanson@wpi.edu>
 */
public class Door extends InteractableObject {
	private boolean open;
	private boolean doneAction;
	private final GTPlayer player;
	
	public Door(GTLevel level) {
		super(level);
		player = level.getPlayer();
		sprite = new DoorSprite();
		((DoorSprite)sprite).setImages(level.getImages("resources/images/Door.png", 2, 1));
		((DoorSprite)sprite).setStatus(0);
		actions.put(0, "open");
	}
	
	public void open() {
		open = true;
		walkable = true;
	}
	
	public boolean isOpen() {
		return open;
	}
	
	@Override
	public void update(long elapsedTime, Map<String,Object> world) {
		if ((((DoorSprite)sprite).getStatus() == 0) && open) {
			((DoorSprite)sprite).setStatus(1);
		}
		super.update(elapsedTime, world);
	}

	/* (non-Javadoc)
	 * @see edu.wpi.disco.game.gt.InteractableObject#doAction(int, edu.wpi.disco.game.World, edu.wpi.disco.game.NWayInteraction)
	 */
	@Override
	public void doAction(int index, Map<String,Object> world, NWayInteraction interaction, Actor a) {
		if ((index == 0) && !doneAction) {
			doneAction = true;
			GTPlayer player = (GTPlayer)world.get("player");
			if ((player.getPlayer().equals(a) && player.getLocation().equals(new Point(this.getLocation().x + 1, this.getLocation().y)))
					|| (!world.get(a.getName()).equals(player) && ((GTNPC)world.get(a.getName())).getLocation().equals(new Point(this.getLocation().x + 1, this.getLocation().y))))
			{
				player.getPlayer().doAction(
						new ExecuteTaskAction(a, "urn:secrets.wpi.edu:models:IceWall", "OpenDoor", null)
						);
			} else {
				// no action (is not displayed, either)
			}
		}
	}

	@SuppressWarnings("serial")
	private class DoorSprite extends AdvanceSprite {
		@Override
		protected void animationChanged(int oldStat, int oldDir, int status, int direction) {
			if ((getImages() != null) && (status < getImages().length)) setAnimationFrame(status, status);
		}
	}
	
	@Override
	public boolean shouldShowActions() {
		return super.shouldShowActions() && !isWalkable() && player.getLocation().equals(new Point(this.getLocation().x + 1, this.getLocation().y));
	}

}
