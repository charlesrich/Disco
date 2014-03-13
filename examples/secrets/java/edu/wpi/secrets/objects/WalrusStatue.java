/* Copyright (c) 2010 Philip Hanson and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.secrets.objects;

import edu.wpi.disco.Actor;
import edu.wpi.disco.game.*;
import edu.wpi.disco.game.actions.ExecuteTaskAction;
import edu.wpi.disco.game.gt.*;

import java.util.*;

public class WalrusStatue extends InteractableObject {
	private boolean searched;
	
	public WalrusStatue(GTLevel level) {
		super(level);
		actions.put(0, "search");
		
		searched = false;
		setImage(level.getImage("resources/images/WalrusStatue.png"));
	}

	@Override
	public void doAction(int index, Map<String,Object> world, NWayInteraction interaction, Actor a) {
		if (index != 0) return;
		searched = true;
		interaction.getPlayer().doAction(new ExecuteTaskAction(a, "urn:secrets.wpi.edu:models:WalrusCave", "SearchStatue", null));
	}
	
	public void search() {
		searched = true;
	}
	
	@Override
	public String toString() {
		return "a status of a walrus";
	}
	
	/* (non-Javadoc)
	 * @see edu.wpi.disco.game.gt.InteractableObject#shouldShowActions()
	 */
	@Override
	public boolean shouldShowActions() {
		return !searched && super.shouldShowActions();
	}

}
