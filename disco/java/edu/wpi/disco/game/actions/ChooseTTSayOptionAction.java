/* Copyright (c) 2010 Philip Hanson, Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.game.actions;

import edu.wpi.disco.Agenda.Plugin;
import edu.wpi.disco.game.*;

import java.util.List;

/**
 * Execute a TTSay utterance task
 */
public class ChooseTTSayOptionAction extends Action {

	final private List<Plugin.Item> ttSayList;
	final int choice;

	public ChooseTTSayOptionAction (List<Plugin.Item> ttSayOptions, int choice) {
		ttSayList = new java.util.ArrayList<Plugin.Item>(ttSayOptions);
		this.choice = choice;
	}

	@Override
	public void perform (SingleInteraction interaction) {
		interaction.choose(ttSayList, choice, 
		     ((Player) interaction.getExternal()).getTTSayList().get(choice-1));
	}

}
