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
 * Plugin which asks for first undefined input value of non-utterance 
 * task that cannot be done by other actor.  Used by both Agent and User.
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
      // TODO can this test be generalized for other plugins?
      Propose.What what = null;
      Task focus = getDisco().getFocus().getGoal();
      if ( focus instanceof Accept ) {
         Propose proposal = ((Accept) focus).getProposal();
         if ( proposal instanceof Propose.What ) {
            what = (Propose.What) proposal;
            if ( !(self() ? what.isSystem() : what.isUser()) ) what = null;
            else if ( what.getGoal() != goal ) what = null;
         }
      }
      for (Input input : goal.getType().getDeclaredInputs()) 
         if ( getGenerateProperty(Ask.What.class, goal, input.getName()) &&
              !input.isDefinedSlot(goal) &&
              // don't ask about slot if other actor just proposed value
              (what == null || !input.getName().equals(what.getSlot())) ) {
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
