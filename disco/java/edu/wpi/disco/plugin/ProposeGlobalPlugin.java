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
 * Plugin to propose enumerated values for global variable.  Typically used to 
 * generate things-to-say list. 
 */
public class ProposeGlobalPlugin extends EnumerationPlugin {
   
   @Override 
   public List<Plugin.Item> apply (Plan plan) {
      Task goal = plan.getGoal();
      if ( !(goal instanceof Propose.Global && canSelf(goal)) ) return null;
      Propose.Global propose = (Propose.Global) goal;
      String variable = propose.getVariable(),
         type = propose.getVariableType();
      Object value = propose.getValue();
      if ( variable == null || type == null || value != null ) return null;
      List<Object> values = values(type);
      if ( values == null ) return null;
      List<Plugin.Item> items = new ArrayList<Plugin.Item>(values.size());
      boolean menu = getMenuProperty(type);
      for (Object e : values)
         items.add(new Plugin.Item(
               new Propose.Global((Disco) plan.getGoal().engine, 
                     self(), variable, type, e), 
                     plan,
                     menu ? getDisco().toString(e) : null)); 
      return items;
   }

   public ProposeGlobalPlugin (Agenda agenda, int priority) { 
      super(agenda, priority);
   }
}
