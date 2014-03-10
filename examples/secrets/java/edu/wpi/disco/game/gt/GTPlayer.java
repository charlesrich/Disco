/* Copyright (c) 2010 Philip Hanson, Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.game.gt;

import edu.wpi.disco.game.Player;

/**
 * Represents the player in Golden T
 */
public class GTPlayer extends EmbodiedObject implements ICarry {
	private final Player player;
	
	private InteractableObject carried;

	public GTPlayer (Player player, GTLevel level) {
		super(level);
		this.player = player;
		movable = true;
		carried = null;
		
		setTileSize(20);
		setImage(level.getImage("/edu/wpi/disco/game/gt/Player.png"));
	}

	public Player getPlayer () {
		return player;
	}
	
	@Override
	public String toString () {
		return getPlayer().getName();
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
