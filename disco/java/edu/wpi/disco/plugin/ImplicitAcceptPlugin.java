/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.plugin;

import edu.wpi.cetask.*;
import edu.wpi.disco.*;
import edu.wpi.disco.Agenda.Plugin;
import edu.wpi.disco.lang.*;

import java.util.*;

/**
 * Plugin to generate behaviors based on implicit acceptance of current
 * proposal.  Only applied to Accept in current focus and below.  Used for
 * TTsay.
 * 
 * TODO Consider something similar for rejection?
 */
public class ImplicitAcceptPlugin extends Agenda.Plugin {
   
   // modeled on DecompositionPlugin
   
   @Override 
   public List<Plugin.Item> apply (Plan plan) {
      if ( !(plan == getDisco().getFocus() && plan.getGoal() instanceof Accept
             && !(((Accept) plan.getGoal()).getProposal() instanceof Ask)) ) 
         return null;
      Propose propose = ((Accept) plan.getGoal()).getProposal();
      Plan contributes = plan.getParent();
      Map<Task,Plugin.Item> items = new LinkedHashMap<Task,Plugin.Item>();
      try {
         propose.accept(contributes, true);
         getDisco().clearLiveAchieved();
         // note recursive call to agenda generation starting at top, excluding this 
         getAgenda().visitNotOnlyBest(getDisco().getTop(plan), items, plan);
      } finally { 
         propose.reject(contributes, true);
         getDisco().clearLiveAchieved();
      }
      Collection<Plugin.Item> values = items.values();
      List<Plugin.Item> results = new ArrayList<Plugin.Item>(values.size());
      // note all items get priority of *this* plugin
      for (Plugin.Item item : values) results.add(new Plugin.Item(item.task, item.contributes));
      return results;
   }
   
   public ImplicitAcceptPlugin (Agenda agenda, int priority) { 
      agenda.super(priority);
   }
}
