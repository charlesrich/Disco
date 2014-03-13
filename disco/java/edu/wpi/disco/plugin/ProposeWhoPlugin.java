/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.plugin;

import edu.wpi.cetask.*;
import edu.wpi.disco.*;
import edu.wpi.disco.Agenda.Plugin;
import edu.wpi.disco.lang.Propose;

import java.util.*;

/**
 * Plugin to propose who should perform primitive act.  Typically used to 
 * generate things-to-say list. 
 */
public class ProposeWhoPlugin extends Agenda.Plugin {
   
   @Override 
   public List<Plugin.Item> apply (Plan plan) {
      Task goal = plan.getGoal();
      if ( !(goal instanceof Propose.Who && canSelf(goal)) ) return null;
      Propose.Who propose = (Propose.Who) goal;
      Task task = propose.getGoal();
      Boolean value = propose.getValue();
      if ( task == null || value != null ) return null;
      List<Plugin.Item> items = new ArrayList<Plugin.Item>(2);
      Disco disco = (Disco) plan.getGoal().engine;
      items.add(newItem(disco, true, task, plan)); 
      items.add(newItem(disco, false, task, plan)); 
      return items;
   }
   
   private Plugin.Item newItem (Disco disco, boolean value, Task task, Plan plan) {
      return new Plugin.Item(new Propose.Who(disco, self(), task, value), plan); 
   }
   
   public ProposeWhoPlugin (Agenda agenda, int priority) { 
      agenda.super(priority);
   }
}
