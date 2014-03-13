/* Copyright (c) 2010 Philip Hanson, Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.game.actions;

import edu.wpi.disco.game.SingleInteraction;

/**
 * Send the {@link edu.wpi.disco.game.NWayInteraction} into console debug mode
 */
public class PauseDebugAction extends Action {

	@Override
	public void perform (SingleInteraction interaction) {
		interaction.getNWay().pause();
		interaction.getDisco().println("Interaction paused (type 'resume' to continue)");
	}

}
