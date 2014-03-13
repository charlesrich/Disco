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
 * Supertype for objects that have actions associated with them
 */
public abstract class InteractableObject extends EmbodiedObject {
	private boolean showMenu;
   protected final Map<Integer,String> actions = new HashMap<Integer,String>();
	
	public InteractableObject (GTLevel level) {
		super(level);
	}

	/**
	 * @return Indexed list of actions that can be performed on this object
	 */
   public Map<Integer,String> getActions() { return actions; }
	
	/**
	 * Perform the action with the specified index (returned by {@link getActions})
	 * on the given game world and/or dialogue instance.
	 */
	public abstract void doAction (int index, Map<String,Object> world, NWayInteraction interaction, Actor a);

	public void setShowActions (boolean show) {
		showMenu = show;
	}

	/**
	 * @return <code>true</code> iff we should display a menu listing the actions from {@link getActions()}
	 */
	public boolean shouldShowActions () {
		return showMenu;
	}
}
