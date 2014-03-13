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
 * Plugin to propose enumerated values for slot.  Typically used to 
 * generate things-to-say list. 
 */
public class ProposeWhatPlugin extends EnumerationPlugin {

   @Override 
   public List<Plugin.Item> apply (Plan plan) {
      if ( !(plan.getGoal() instanceof Propose.What) ) return null;
      Propose.What propose = (Propose.What) plan.getGoal();
      Task goal = propose.getGoal();
      if ( goal == null ) return null;
      String slot = propose.getSlot();
      if ( slot == null ) return null;
      Object value = propose.getValue();
      if ( value != null ) return null;
      String type = goal.getType().getSlotType(slot);
      List<Object> values = values(type);
      if ( values == null ) return null;
      List<Plugin.Item> items = new ArrayList<Plugin.Item>(values.size());
      boolean menu = getMenuProperty(type);
      for (Object e : values)
         items.add(new Plugin.Item(
               new Propose.What((Disco) plan.getGoal().engine, 
                     self(), goal, slot, e),
               plan, 
               menu ? getDisco().toString(e) : null)); 
      return items;
   }
   
   public ProposeWhatPlugin (Agenda agenda, int priority) { 
      super(agenda, priority);
   }
}
