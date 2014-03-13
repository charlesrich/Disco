/* Copyright (c) 2010 Philip Hanson, Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.game.actions;

import edu.wpi.disco.game.SingleInteraction;

import java.util.Map;

/**
 * Sets the ready flag. 
 * This action should be placed in the queue after all model loading actions.
 */
public class LevelReadyAction extends Action {
	private final Map<String,Object> world;

	public LevelReadyAction (Map<String,Object> world) {
		this.world = world;
	}

	@Override
	public void perform (SingleInteraction interaction) {
		world.put("readyFlag", Boolean.TRUE);
	}

}
