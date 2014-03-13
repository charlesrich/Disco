/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.plugin;

import edu.wpi.cetask.*;
import edu.wpi.disco.*;
import edu.wpi.disco.lang.*;

/**
 * Plugin which proposes to execute a non-optional task with defined inputs by self
 * or someone. Typically used by agent.
 */
public class ProposeShouldTopPlugin extends ProposeShouldPlugin {

	@Override
	protected boolean isApplicable (Plan plan) {
		return getDisco().getTopClasses().contains(plan.getGoal().getType());
	}

	@Override
	protected Task newTask (Disco disco, Plan plan) {
		return Propose.Should.newInstance(disco, self(), plan.getGoal());
	}
   
	public ProposeShouldTopPlugin (Agenda agenda, int priority) {
		super(agenda, priority);
	}

}
