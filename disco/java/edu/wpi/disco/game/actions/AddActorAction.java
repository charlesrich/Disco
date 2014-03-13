/* Copyright (c) 2010 Philip Hanson, Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.game.actions;

import edu.wpi.disco.Actor;
import edu.wpi.disco.game.SingleInteraction;

/**
 * Add or reactivate an actor
 */
public class AddActorAction extends Action {
	private final Actor actor;
	
	public AddActorAction (Actor actor) { this.actor = actor; }
	
	@Override
	public void perform (SingleInteraction interaction) {
		interaction.getNWay().add(actor);
	}

}
