/* Copyright (c) 2010 Philip Hanson, Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.game;

import edu.wpi.disco.Agenda.Plugin.Item;
import edu.wpi.disco.*;
import edu.wpi.disco.game.actions.Action;
import edu.wpi.disco.lang.Utterance;
import edu.wpi.disco.plugin.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Represents a player in Disco for Games
 */
public class Player extends User {

	private final BlockingQueue<Action> toDoList = new LinkedBlockingQueue<Action>();
	private final List<String> ttSay = new LinkedList<String>();

	public Player (String name) {
		super(name);
		// tweak TTSay plugins
		agenda.remove(ProposeHowPlugin.class);
      new ProposeHowPlugin(agenda, 30); 
      agenda.remove(ProposeShouldSelfPlugin.class);
      new ProposeShouldSelfPlugin(agenda, 90, true); //suppressExternal
	}

	public boolean doAction (Action a) {
		try {
			toDoList.put(a);
			return true;
		} catch (InterruptedException e) {
			// not much to do here unless we want to add another layer
			// of buffering and complicated try/try again behavior
		}
		return false;
	}

	@Override
	public boolean synchronizedRespond (Interaction interaction, 
	      boolean ok, boolean guess, boolean retry) {
		// pull actions off the list one at a time so as not to
		// accidentally trigger a ConcurrentModificationException
		Action a;
		if ((a = toDoList.poll()) != null) {
			a.perform((SingleInteraction) interaction);
			return true;
		} 
		return false;
	}

	public List<String> getTTSayList () {
		return ttSay;
	}

	public void setTTSayItems (List<Item> items) {
	   List<String> list = new ArrayList<String>(items.size());
		for (Item i : items) 
			list.add(((Disco) i.task.engine).translate((Utterance) i.task));
		// update list all at once to reduce flickering
	    ttSay.clear();
	    ttSay.addAll(list);
	}

	public boolean hasActions() {
		return !toDoList.isEmpty();
	}

}
