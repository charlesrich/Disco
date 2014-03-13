/* Copyright (c) 2010 Philip Hanson, Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.game;

import edu.wpi.cetask.*;
import edu.wpi.disco.*;

import java.util.Map;

public class SingleInteraction extends Interaction {
   
	private final NWayInteraction nway;
	
	public SingleInteraction (Actor system, Actor external, Map<String,Object> world, NWayInteraction nway) {
		super(system, external);
		this.nway = nway;
		getDisco().setGlobal("world", world);
	}
	
	@Override
	public void done (boolean external, Task occurrence, Plan contributes) {
		nway.broadcastDone(external ? getExternal() : getSystem(), occurrence, contributes);
	}
	
	/**
	 * @return <code>true</code> iff the interaction should continue 
	 */
	public boolean isRunning () { return running; }
	
	/**
	 * Alter the running state
	 */
	public void startRunning () {	running = true; }
	
	/**
	 * @return nway interaction to which this interaction belongs
	 */
	public NWayInteraction getNWay () { return nway; }
	
	@Override
	public void exit () { nway.exit(); }
	
	@Override
	public String toString () {
		return getExternal().getName() + "&" + getSystem().getName();
	}
}
