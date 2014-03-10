/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.plugin;

import edu.wpi.cetask.*;
import edu.wpi.disco.*;

import java.util.*;

/**
 * Convenient base plugin for defining plugins which return a single task.
 */
public abstract class SingletonPlugin extends BasePlugin {

   @Override
   final protected List<Task> newTaskList (Disco disco, Plan plan) {
      return Collections.singletonList(newTask(disco, plan));
   }
   
   abstract protected Task newTask (Disco disco, Plan plan);
   
   public SingletonPlugin (Agenda agenda, int priority) { 
      super(agenda, priority);
   }
}
