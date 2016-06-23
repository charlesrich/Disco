/* Copyright (c) 2010 Philip Hanson, Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.game.actions;

import java.util.*;
import edu.wpi.cetask.Task;
import edu.wpi.disco.Actor;
import edu.wpi.disco.game.SingleInteraction;

/**
 * Encapsulates creation and execution of a new task instance 
 */
public class ExecuteTaskAction extends Action {

	final private Actor actor;
	final private String namespace;
	final private String taskId;
	final private Map<String, Object> slotValues;

	public ExecuteTaskAction (Actor actor, String namespace, String taskId, 
	                         Map<String, Object> slotValues) {
		this.actor = actor;
		this.namespace = namespace;
		this.taskId = taskId;
		this.slotValues = slotValues;
	}

	@Override
	public void perform (SingleInteraction interaction) {
		// create new task instance using namespace and id
		Task task = interaction.getDisco()
			.getModel(namespace)
			.getTaskClass(taskId)
			.newInstance();
		
		// fill in all the slot values
		if (slotValues != null) {
			String key;
			Iterator<String> i = slotValues.keySet().iterator();  
			while (i.hasNext()) {
				key = i.next();
				task.setSlotValue(key, slotValues.get(key));
			}
		}
		actor.execute(task, interaction, null);
		
		// broadcast to all interactions (but do not re-execute script)
		interaction.getNWay().broadcastOccurred(actor, task, null, false);
	}

}
