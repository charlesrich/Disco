/* Copyright (c) 2010 Philip Hanson, Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.game;

import edu.wpi.cetask.TaskEngine;
import edu.wpi.disco.*;

import java.util.HashMap;

public class GameConsole extends Console {
   
   /**
    * Main class for running stand-alone Disco for Games with console.
    * 
    * @param args first string (if any) is url or filename from which to read 
    *             console commands
    */
   public static void main (String[] args) {
      new NWayInteraction(
            new HashMap<String,Object>(),  // default world object
            new Player("player"),
            new NPC("sidekick"),
            args.length > 0 && args[0].length() > 0 ? args[0] : null,
            true)
      .run();
   }
   
	private enum State { UNINITIALIZED, RUNNING, DISPOSED }
	private State state = State.UNINITIALIZED;
	
	{
		status.add("add");
		status.add("remove");
		status.add("actors");
		status.add("interactions");
	}
	
  private final NWayInteraction nway;

	public GameConsole (String from, SingleInteraction interaction, NWayInteraction nway) {
		super(from, interaction);
		this.nway = nway;
	}
	
	@Override
	public final void init (TaskEngine engine) {
		if (state == State.UNINITIALIZED) {
			super.init(engine);
			state = State.RUNNING;
		}
		internalInit(engine);
	}

	/**
	 * Initialization function. May be called many times per object.
	 */
	protected void internalInit (TaskEngine engine) {
	   prompt = getInteraction().toString() + engine.getProperty("shell@prompt");
	}
	
	@Override
	public final void cleanup () {
		if (state == State.RUNNING) {
			super.cleanup();
			internalCleanup();
			state = State.DISPOSED;
		}
	}
	
	/**
	 * Cleanup function. Will only be called once per object.
	 */
	protected void internalCleanup () {}
	
	public void add (String name) {
		if (name.length() == 0) return;
		Actor x = nway.get(name);
		if (x == null) {
			x = new NPC(name);
		}
		nway.add(x);
	}
	
	public void remove (String name) {
		if (name.length() == 0) return;
		Actor x = nway.get(name);
		if (x == null) {
			println("Cannot remove " + name + ": not part of conversation.");
			return;
		}
		nway.remove(x);
	}
	
	public void actors (String active) {
		for (Actor a : nway.getActors(active.trim().length() == 0)) {
			println("    " + a.getName());
		}
	}
	
	public void interactions (String active) {
		for (SingleInteraction i : nway.getInteractions(active.trim().length() == 0)) {
			println("    " + i.getExternal().getName() + "&" + i.getSystem().getName());
		}
	}
	
	public void resume (String ignore) {
		nway.resume();
	}
	
	@Override
	public void clear (String ignore) {
		nway.clearAll();
	}
	
	@Override
	protected void help () {
		println("    add <name>          - add named NPC to the interaction");
		println("    remove <name>       - remove named NPC from interaction");
		println("    actors [<active>]   - list all or active conversants");
		println("    interactions [<active>]   - list all or active interactions");
		println("    resume              - return to normal gameplay");
		super.help();
	}
}
