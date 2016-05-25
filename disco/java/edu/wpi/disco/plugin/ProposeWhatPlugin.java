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
 * Plugin for agent which proposes first undefined input value of non-utterance
 * task based on agent's private knowledge.  
 */
public class ProposeWhatPlugin extends SingletonPlugin {
   
   // assuming single threaded
   private Input input;
   
   @Override
   protected boolean isApplicable (Plan plan) {
      Task goal = plan.getGoal();
      if ( goal instanceof Utterance || goal.isDefinedInputs() )
         return false;
      for (Input input : goal.getType().getDeclaredInputs()) 
         if ( getGenerateProperty(Propose.What.class, goal, input.getName()) &&
              !input.isDefinedSlot(goal) &&
              ((Agent) getWho()).isDefinedSlot(plan, input) ) {
            this.input = input;
            return true;
         }
      return false;
   }
   
   @Override
   protected Task newTask (Disco disco, Plan plan) {
      return new Propose.What(disco, self(), plan.getGoal(), input.getName(), 
            ((Agent) getWho()).getSlotValue(plan, input));
   }

   public ProposeWhatPlugin (Agenda agenda, int priority) { 
      super(agenda, priority);
   }
}
