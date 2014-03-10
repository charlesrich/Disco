/* Copyright (c) 2010 Philip Hanson, Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.game.actions;

import edu.wpi.disco.Actor;
import edu.wpi.disco.game.SingleInteraction;

/**
 * Load a task model into the interaction containing the given actors
 */
public class LoadModelAction extends Action {
	private final Actor a, b;
	private final String filename;

	public LoadModelAction (Actor a, Actor b, String filename) {
		this.a = a;
		this.b = b;
		this.filename = filename;
	}

	@Override
	public void perform (SingleInteraction interaction) {
		if ( !interaction.getNWay().load(a, b, filename) )
		   interaction.getDisco().println("Warning: Task model not successfully loaded "+filename);
	}

}
