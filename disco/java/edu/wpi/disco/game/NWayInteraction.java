/* Copyright (c) 2010 Philip Hanson, Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.game;

import edu.wpi.cetask.*;
import edu.wpi.disco.*;
import edu.wpi.disco.Agenda.Plugin;
import edu.wpi.disco.Agenda.Plugin.Item;
import edu.wpi.disco.lang.Utterance;

import java.util.*;

/**
 * Models a conversation between multiple parties.  This thread class runs as the 
 * "AI loop" of the game.
 */
public class NWayInteraction implements Runnable {
	/**
	 * Limit for number of actions allowed in a single turn without breaking with an Utterance 
	 */
	private static final int MAX_ACTIONS = 4;
	/**
	 * Limit for number of times Actor is allowed to check an interaction in a single turn
	 * when attempting to generate tasks to execute 
	 */
	private static final int MAX_LOOPS = 3;

	protected final List<SingleInteraction> activeInteractions = new LinkedList<SingleInteraction>();
	private final List<SingleInteraction> inactiveInteractions = new LinkedList<SingleInteraction>();
	
	private final List<Actor> activeActors = new LinkedList<Actor>();
	private final List<Actor> inactiveActors = new LinkedList<Actor>();
	
   public List<SingleInteraction> getActiveInteractions () { return activeInteractions; }
   public List<Actor> getActiveActors () { return activeActors; }
   
	private final Map<String,Object> world;
	protected final Player player;
	
   public Player getPlayer () { return player; }
	
	private final List<Plugin.Item> ttSayItems = new LinkedList<Plugin.Item>();
	
	protected final GameConsole console;

	public GameConsole getConsole () { return console; }
	
	protected boolean running = true;
	private boolean paused;
	private int responses;
	private boolean doneUtterance;
	
   public boolean isPaused () { return paused; }

	public NWayInteraction (Map<String,Object> world, Player player, Actor actor, 
	                        String from, boolean paused) {
		this.world = world;
		this.player = player;
		this.paused = paused;
		
		SingleInteraction interaction = new SingleInteraction(actor, player, world, this);
		interaction.startRunning();
		
		activeActors.add(player);
		activeActors.add(actor);
		activeInteractions.add(interaction);
      this.console = new GameConsole(from, activeInteractions.get(0), this);
      interaction.setConsole(console);
		
		getNextActor(); // so that first turn goes to player

	}
	
	/**
	 * Add a new speaker to the conversation.
	 */
	public synchronized void add (Actor actor) {
		if (activeActors.contains(actor)) return;
		for (SingleInteraction i : inactiveInteractions.toArray(new SingleInteraction[] {})) {
			if ((i.getSystem().equals(actor) && activeActors.contains(i.getExternal()))
					|| (i.getExternal().equals(actor) && activeActors.contains(i.getSystem()))) {
				inactiveInteractions.remove(i);
				activeInteractions.add(i);
			}
		}
		for (Actor a : activeActors) {
			boolean found = false;
			for (SingleInteraction i : activeInteractions.toArray(new SingleInteraction[] {})) {
				if ((i.getSystem().equals(actor) && i.getExternal().equals(a))
						|| (i.getExternal().equals(actor) && i.getSystem().equals(a))) {
					found = true;
					break;
				}
			}
			if (!found) {
				// create new interaction
				SingleInteraction i = new SingleInteraction(actor, a, world, this);
				i.setConsole(console);
				i.startRunning();
				activeInteractions.add(i);
			}
		}
		if (inactiveActors.contains(actor)) inactiveActors.remove(actor);
		activeActors.add(actor);
		tick();
		generateTTSay();
	}
	
	/**
	 * Remove a speaker from the conversation.
	 * Implementation note: actually just deactivates the speaker 
	 * @throws IllegalArgumentException the given speaker is not part of the conversation
	 */
	public synchronized void remove (Actor actor) throws IllegalArgumentException {
		if (!activeActors.contains(actor)) throw new IllegalArgumentException("No such conversant");		
		// deactivate all interactions this Actor is party to
		for (SingleInteraction i : activeInteractions.toArray(new SingleInteraction[] {})) {
			if (i.getSystem().equals(actor) || i.getExternal().equals(actor)) {
				if (activeInteractions.indexOf(i) == 0) {
					// if this is the current interaction, we should rewind the loop by one
					// to make sure that the next interaction does not get skipped.
					activeInteractions.add(activeInteractions.remove(activeInteractions.size()-1));
				}
				activeInteractions.remove(i);
				inactiveInteractions.add(i);
			}
		}
		// deactivate
		activeActors.remove(actor);
		inactiveActors.add(actor);
		tick();
		generateTTSay();
	}
	
	/**
	 * Return a reference to the first actor with the given name
	 * @return <code>null</code> if not found
	 */
	public Actor get (String name) {
		for (Actor a : activeActors) {
			if (a.getName().equals(name)) {
				return a;
			}
		}
		for (Actor a : inactiveActors) {
			if (a.getName().equals(name)) {
				return a;
			}
		}
		return null;
	}
	
	/**
	 * Return a list of involved actors
	 * @param active <code>true</code> for active speakers, <code>false</code> for inactive speakers
	 */
	public Actor[] getActors (boolean active) {
	   return (active ? activeActors : inactiveActors).toArray(new Actor[] {}); 
	}
	
	/**
	 * Return a list of all sub-interactions.
	 * @param active <code>true</code> for active interactions, <code>false</code> for inactive interactions
	 */
	public SingleInteraction[] getInteractions (boolean active) {
	   return (active ? activeInteractions : inactiveInteractions).toArray(new SingleInteraction[] {});
	}
	
	/**
	 * Notify all interactions concerned that the given actor has completed a task.
	 */
	@SuppressWarnings("deprecation")
   public void broadcastOccurred (Actor a, Task occurrence, Plan contributes, boolean eval) {
		responses += 1;
		
		doneUtterance = (occurrence instanceof Utterance); // graceful end-of-turn
		
		// do broadcast by copying to all interactions actor is party to
		for (SingleInteraction i : activeInteractions) {
			if (i.getExternal().equals(a) || i.getSystem().equals(a)) {
				try {
					Disco disco = i.getDisco();
					if (occurrence.engine != disco) {
						occurrence = disco.copy(occurrence);
						contributes = null; // need to recognize in other engines
					}
					i.occurredSilent(i.getExternal().equals(a), occurrence, contributes, eval);
					eval = false; // only evaluate grounding script once
				} catch (IllegalArgumentException e) {} // ignore if no such task in that engine
			}
		}
		generateTTSay();
	}

	/**
	 * Generate things-to-say list
	 */
	private void generateTTSay () {
		List<edu.wpi.disco.Agenda.Plugin.Item> items = new LinkedList<Plugin.Item>();
		for (SingleInteraction i : activeInteractions) {
			if (i.getExternal().equals(player) || i.getSystem().equals(player)) {
				for (Plugin.Item it : player.generate(i)) {
					if (!items.contains(it)) items.add(it);
				}
			}
		}
		java.util.Collections.sort(items, new PluginPriorityComparator());
		ttSayItems.clear();
		ttSayItems.addAll(items);
		player.setTTSayItems(items);
	}
	
	/**
	 * Simple priority sort for plugin items
	 * 
	 * @author Philip Hanson <phanson@wpi.edu>
	 */
	private class PluginPriorityComparator implements java.util.Comparator<Plugin.Item> {
		@Override
		public int compare(Item o1, Item o2) {
			return o2.getPriority() - o1.getPriority();
		}
	}
	
	public List<Plugin.Item> getTTSayItems () {
		return ttSayItems;
	}
	
	/**
	 * Clear the discourse state for all interactions.
	 */
	public void clearAll () {
		for (SingleInteraction i : activeInteractions) i.clear();
		for (SingleInteraction i : inactiveInteractions) i.clear();
	}
	
	@Override
	public void run () {
	   Actor a = null;
	   SingleInteraction i = null;
	   int tries = 0;
	   while (running) {
	      a = getNextActor();
	      tries = 0;
	      responses = 0;
	      doneUtterance = false;
	      
	      do {
	         i = getNextInteraction(a);
	         if (isPaused()) {
	            console.setInteraction(i);
	            console.init(i.getDisco());
	         }
	         a.respond(i, false, false);
	         tries++;
	         // short circuit for obvious case
	         if ((responses == 0) && tries > activeActors.size() - 1) break;
	      } while (!doneUtterance && responses < MAX_ACTIONS 
	                && tries < (activeActors.size()-1)*MAX_LOOPS);

	      if (isPaused()) {
	         console.setInteraction(i);
	         console.init(i.getDisco());
	         console.respond(i);
	      }
	      else if (!player.getName().equals(a.getName())) {
	         player.respond(i, false, false);
	         generateTTSay();  // TODO find better place for this? trying to catch "fortuitous completions" of tasks
	         try { Thread.sleep(1500); } catch(InterruptedException e) { /* continue on wake */ }
	      }
	   }
	   cleanup();
	}

	/**
	 * Ends the turn of the {@link Actor} that currently holds the floor,
	 * gives the floor to a new <code>Actor</code>.
	 * @return the new <code>Actor</code>
	 */
	private Actor getNextActor () {
	   activeActors.add(activeActors.remove(0));
	   return activeActors.get(0);
	}
	
	/**
	 * Move to the next {@link SingleInteraction} that the given
	 * {@link Actor} is party to. Can be used to iterate over all
	 * <code>SingleInteraction</code>s.  
	 * @return next <code>SingleInteraction</code>
	 */
	private SingleInteraction getNextInteraction (Actor a) {
		SingleInteraction next;
		// it's not pretty, but it works.
		do {
			activeInteractions.add(activeInteractions.remove(0));  // pull off front, add to back
			next = activeInteractions.get(0);
		} while (!next.getExternal().equals(a) && !next.getSystem().equals(a));
		return next;
	}

	public void pause () {
		paused = true;
		for (SingleInteraction i : activeInteractions) {
			i.setConsole(console);
		}
		for (SingleInteraction i : inactiveInteractions) {
			i.setConsole(console);
		}
	}
	
	public void resume () {
		for (SingleInteraction i : activeInteractions) {
			i.setConsole(null);
		}
		for (SingleInteraction i : inactiveInteractions) {
			i.setConsole(null);
		}
		paused = false;
	}
	
	protected void cleanup () {
		for (SingleInteraction i : activeInteractions) {
			i.cleanup();
		}
		for (SingleInteraction i : inactiveInteractions) {
			i.cleanup();
		}
	}

	/**
	 * @return <code>true</code> iff the given conversant is active in the conversation
	 */
	public boolean isActive (Actor actor) {
		return activeActors.contains(actor);
	}
	
	/**
	 * Load the given task model file into the interaction containing the given actors.
	 * @param from Filename of task model
	 * @return <code>true</code> if some interaction loads the model
	 */
	public boolean load (Actor a, Actor b, String from) {
		boolean loaded = false;
		for(SingleInteraction i : activeInteractions) {
			if ((i.getExternal().equals(a) && i.getSystem().equals(b))
					|| (i.getExternal().equals(b) && i.getSystem().equals(a))) {
				loaded |= i.getDisco().load(from) != null;
			}
		}
		for(SingleInteraction i : inactiveInteractions) {
			if ((i.getExternal().equals(a) && i.getSystem().equals(b))
					|| (i.getExternal().equals(b) && i.getSystem().equals(a))) {
				loaded |= i.getDisco().load(from) != null;
			}
		}
		generateTTSay();
		return loaded;
	}
	
	/**
	 * Quit all interactions and exit the conversation.
	 */
	public void exit () { running = false; }

	/**
	 * Signal to start next turn if waiting
	 */
	public void wake () {
		Thread.currentThread().interrupt();
	}

	/**
	 * Clear liveness flags and postconditions on all active interactions
	 */
	public void tick () {
		for (SingleInteraction i : activeInteractions) {
			i.getDisco().tick();
		}
	}

}
