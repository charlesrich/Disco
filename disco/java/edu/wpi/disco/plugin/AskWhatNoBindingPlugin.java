/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.plugin;

import java.util.*;
import edu.wpi.cetask.*;
import edu.wpi.cetask.TaskClass.Input;
import edu.wpi.disco.*;
import edu.wpi.disco.Agenda.Plugin;
import edu.wpi.disco.lang.*;

/**
 * Plugin that asks for first unknown input value of non-primitive 
 * task that has no <em>identity</em> binding (since this input will not be asked
 * about later on)
 */
public class AskWhatNoBindingPlugin extends Agenda.Plugin {
   
   @Override
   public List<Plugin.Item> apply (Plan plan) {
      Task goal = plan.getGoal();
      if ( goal.isPrimitive() || goal.isDefinedInputs() ) return null;
      DecompositionClass decomp = plan.getDecompositionClass();
      for (Input input : goal.getType().getDeclaredInputs()) {
         String name = input.getName();
         if ( getGenerateProperty(Ask.What.class, goal, name) &&
               !goal.isDefinedSlot(input) && 
               // if no decomp yet, could be waiting for input to 
               // evaluate applicability condition
               // if this is not desired behavior, inhibit with
               // @generate=false property
               (decomp == null || !decomp.hasBinding(name)) )
            return Collections.singletonList(
                new Plugin.Item(new Ask.What(getDisco(), self(), goal, name), plan));
      }
      return null;
   }

   public AskWhatNoBindingPlugin (Agenda agenda, int priority) { 
      agenda.super(priority);
   }
}
