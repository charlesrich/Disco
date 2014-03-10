/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.plugin;

import edu.wpi.cetask.*;
import edu.wpi.disco.*;
import edu.wpi.disco.Agenda.Plugin;

import java.util.*;

/**
 * Convenient base plugin for defining simple plugin inheritance.
 */
public abstract class BasePlugin extends Agenda.Plugin {

   @Override
   public final List<Plugin.Item> apply (Plan plan) {
      if ( !isApplicable(plan) ) return null;
      List<Task> tasks = newTaskList((Disco) plan.getGoal().engine, plan);
      if ( tasks == null ) return null;
      List<Plugin.Item> items = new ArrayList<Plugin.Item>(tasks.size());
      for (Task task : tasks) 
         items.add(new Plugin.Item(task, plan));
      return items;
   }

   abstract protected boolean isApplicable (Plan plan);
   
   abstract protected List<Task> newTaskList (Disco disco, Plan plan);
   
   public BasePlugin (Agenda agenda, int priority) { 
      agenda.super(priority);
   }
}
