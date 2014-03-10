/* Copyright (c) 2010 Philip Hanson, Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.game.gt;

import edu.wpi.disco.Actor;
import edu.wpi.disco.game.NWayInteraction;

import java.util.*;

/**
 * A generic invisible trigger object. On creation the trigger is inactive and
 * must be enabled by calling {@link activate()}
 */
public abstract class Trigger extends InteractableObject {
	/** Name of action to display. Subclasses should supply value. */
	protected String triggerAction = null;
	private boolean active;
	protected int maxTrigger = 1;
	private int triggered;
	private final Map<Integer, String> actions = new HashMap<Integer, String>();

	public Trigger (GTLevel level) { super(level); }

	public void activate () {
		active = true;
	}

	public void deactivate () {
		active = false;
	}

	public boolean isActive () {
		return active;
	}

	@Override
	public Map<Integer, String> getActions( ) {
		actions.clear();
		if (triggerAction != null) actions.put(0, triggerAction);
		return actions;
	}

	@Override
	public void doAction (int index, Map<String,Object> world, NWayInteraction interaction, Actor a) {
		if ((!isActive()) || (index != 0))
			return;
		if (triggered == maxTrigger) {
			deactivate(); // automatically deactivate when trigger is "used up"
			return;
		}
		triggered++;
		trigger(world, interaction, a);
	}

	/**
	 * Code for action to be triggered
	 */
	protected abstract void trigger (Map<String,Object> world, NWayInteraction interaction, Actor a);

	@Override
	public boolean shouldShowActions () {
		return active && super.shouldShowActions();
	}

	@Override
	public boolean isWalkable () {
		return !isActive();
	}
}