/* Copyright (c) 2010 Philip Hanson, Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.game;

import edu.wpi.disco.Agenda.Plugin.Item;
import edu.wpi.disco.*;
import edu.wpi.disco.lang.*;
import edu.wpi.disco.plugin.AskHowPlugin;

import java.util.LinkedList;

/**
 * Represents a non-player character in Disco for Games
 */
public class NPC extends Agent {
	final private UtteranceBuffer buffer = new UtteranceBuffer(15, 1); 
	private boolean floating;

	public NPC (String name) {
		super(name);
		agenda.remove(AskHowPlugin.class);
		new AskHowPlugin(agenda, 15); 
	}

	@Override
	public void say (Interaction interaction, Utterance utterance) { 
	   buffer.say(interaction.format(utterance));
	}

	public String getUtterance () {
		return buffer.getUtterance();
	}

	public boolean hasUtterance () {
		return buffer.hasUtterance();
	}
	
	@Override
	public void occurred (Interaction interaction, Item item) {
	   if (item.task instanceof Utterance) {
	      Utterance utterance = (Utterance) item.task;
	      if ( !(utterance instanceof Utterance.Text) && utterance.equals(getLastUtterance()) )
	         return;
	   }
		super.occurred(interaction, item);
	}
	
	public void setIgnoreObstacles (boolean ignore) {
		floating = ignore;
	}
	
	public boolean isIgnoringObstacles () {
		return floating;
	}
	
	private class UtteranceBuffer {

	   private long longTimeout, shortTimeout; // long and short timeouts
	   // short timeout is used when length of queue > 1
	   private long lastAdd; // time of last addition
	   private final LinkedList<String> utterances = new LinkedList<String>();

	   /**
	    * @param longTimeout
	    *            Timeout in seconds
	    * @param shortTimeout
	    *            Timeout in seconds
	    */
	   public UtteranceBuffer (long longTimeout, long shortTimeout) {
	      this.longTimeout = longTimeout * 1000;
	      this.shortTimeout = shortTimeout * 1000;
	      lastAdd = 0;
	   }

	   public boolean hasUtterance () {
	      return !utterances.isEmpty();
	   }

	   public String getUtterance () {
	      if (utterances.isEmpty()) {
	         return null;
	      }
	      long elapsedTime = System.currentTimeMillis() - lastAdd;
	      // TODO rewrite w.r.t. every utterance time
	      return ((utterances.size() > 1) && (elapsedTime > shortTimeout))
	             || (elapsedTime > longTimeout) ?
	         utterances.removeFirst() :
	         utterances.getFirst();
	   }

	   public void say (String utterance) {
	      utterances.add(utterance);
	      lastAdd = System.currentTimeMillis();
	   }

	}

}
