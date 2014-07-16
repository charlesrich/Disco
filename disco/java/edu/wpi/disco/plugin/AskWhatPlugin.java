/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.plugin;

import edu.wpi.cetask.*;
import edu.wpi.cetask.TaskClass.Input;
import edu.wpi.disco.*;
import edu.wpi.disco.lang.*;

/**
 * Plugin which asks for first unknown input value of non-utterance 
 * task that cannot be done by other actor.
 */
public class AskWhatPlugin extends SingletonPlugin {
   
   // assuming single threaded
   
   private Input input;
   
   @Override
   protected boolean isApplicable (Plan plan) {
      Task goal = plan.getGoal();
      if ( goal instanceof Utterance || canOther(goal) 
            || goal.isDefinedInputs() )
         return false;
      for (Input input : goal.getType().getDeclaredInputs()) 
         if ( getGenerateProperty(Ask.What.class, goal, input.getName()) &&
              !goal.isDefinedSlot(input) ) {
            this.input = input;
            return true;
         }
      return false;
   }
   
   @Override
   protected Task newTask (Disco disco, Plan plan) {
      return new Ask.What(disco, self(), plan.getGoal(), input.getName());
   }

   public AskWhatPlugin (Agenda agenda, int priority) { 
      super(agenda, priority);
   }
}
