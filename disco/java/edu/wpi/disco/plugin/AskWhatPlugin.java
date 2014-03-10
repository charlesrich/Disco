/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.plugin;

import edu.wpi.cetask.*;
import edu.wpi.disco.*;
import edu.wpi.disco.lang.*;

/**
 * Plugin which asks for first unknown input value of non-utterance 
 * task that cannot be done by other actor.
 */
public class AskWhatPlugin extends SingletonPlugin {
   
   // assuming single threaded
   
   private String input;
   
   @Override
   protected boolean isApplicable (Plan plan) {
      Task goal = plan.getGoal();
      if ( goal instanceof Utterance || canOther(goal) 
            || goal.isDefinedInputs() )
         return false;
      for (String input : goal.getType().getDeclaredInputNames()) 
         if ( getGenerateProperty(Ask.What.class, goal, input) &&
              !goal.isDefinedSlot(input) ) {
            this.input = input;
            return true;
         }
      return false;
   }
   
   @Override
   protected Task newTask (Disco disco, Plan plan) {
      return new Ask.What(disco, self(), plan.getGoal(), input);
   }

   public AskWhatPlugin (Agenda agenda, int priority) { 
      super(agenda, priority);
   }
}
