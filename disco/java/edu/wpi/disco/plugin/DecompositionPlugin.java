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
 * Plugin to explore applicable decompositions to extend set of possible
 * next tasks. Only apply to focus, because it does its own recursion, and
 * not elsewhere, because  it is expensive and potentially distracting. 
 * Typically used to generate things-to-say list. 
 */
public class DecompositionPlugin extends Agenda.Plugin {
   
   private boolean suppressFormatted; // see User sets to true
   
   // keep track of recursion
   private final Stack<DecompositionClass> path = new Stack<DecompositionClass>();

   private final boolean focusOnly; 
   
   @Override 
   public List<Plugin.Item> apply (Plan plan) {
      boolean recursive = !path.isEmpty();
      if ( plan.isPrimitive()
            || (recursive ? plan.isHow() : (focusOnly && plan != getDisco().getFocus(true))) )
         return null;
      List<DecompositionClass> decomps = plan.getDecompositions();
      if ( decomps.isEmpty() ) return null;
      Map<Task,Plugin.Item> items = new LinkedHashMap<Task,Plugin.Item>();
      for (DecompositionClass decomp : decomps) {
         // allow only one loop
         if ( path.indexOf(decomp) != path.lastIndexOf(decomp) ) continue; 
         try {
            path.push(decomp);
            // decomposition of internal nodes needed to make TTSay work for D4g
            // for recursive calls need to expand even formatted decomps in order to 
            // get some tasks to include in items
            if ( decomp.isInternal() || recursive 
                  // if decomp is formatted, then it may be better to talk about it
                  // explicitly rather than via first step
                  || !suppressFormatted || decomp.getProperty("@format") == null )
               try {
                  if ( plan.isHow() ) decomp = null;  // e.g., focus
                  else plan.apply(decomp);
                  for (Plan child : plan.getChildren()) 
                     // note recursive call to agenda generation 
                     getAgenda().visit(child, items, null);
               } finally { if ( decomp != null ) plan.setDecomposition(null); }
         } finally { path.pop(); } // relies on discourse process single threaded
      }
      Collection<Plugin.Item> values = items.values();
      List<Plugin.Item> results = new ArrayList<Plugin.Item>(values.size());
      for (Plugin.Item item : values)
         // remove Propose.How's since they refer to plans
         if ( !(item.task instanceof Propose.How) ) 
            // note all items get priority of *this* plugin
            results.add(new Plugin.Item(item.task, null));
      return results;
   }

   public DecompositionPlugin (Agenda agenda, int priority, boolean suppressFormatted, boolean focusOnly) { 
      agenda.super(priority);
      this.suppressFormatted = suppressFormatted;
	this.focusOnly = focusOnly;
   }
}
