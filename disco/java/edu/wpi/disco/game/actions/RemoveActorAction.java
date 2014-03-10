/* Copyright (c) 2010 Philip Hanson, Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.game.actions;

import edu.wpi.disco.Actor;
import edu.wpi.disco.game.SingleInteraction;

/**
 * Deactivate an actor
 */
public class RemoveActorAction extends Action {
	Actor actor;
	
	public RemoveActorAction (Actor actor) {
		this.actor = actor;
	}
	
	@Override
	public void perform (SingleInteraction interaction) {
		try {
			interaction.getNWay().remove(actor);
		} catch (IllegalArgumentException e) {
			// ignore because we probably just sent duplicate actions
		}
	}

}
