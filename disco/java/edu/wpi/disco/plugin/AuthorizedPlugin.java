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
 * Plugin used to execute authorized task for which all inputs known.
 */
public class AuthorizedPlugin extends SingletonPlugin {

   @Override
   protected boolean isApplicable (Plan plan) {
      if ( !isAuthorizedDefined(plan) ) return false;
      Task goal = plan.getGoal();
      if ( goal instanceof Accept ) {
         Propose proposal = ((Accept) goal).getProposal();
         if ( proposal instanceof Propose.Should ) {
            goal = ((Propose.Should) proposal).getGoal();
            if ( getWho().isAuthorized(goal, getInteraction()) && goal.isDefinedInputs() )
               // don't accept proposal to do action if can just do it
               return false;
         }
      } // else
      return true;
   }

   protected boolean isAuthorizedDefined (Plan plan) {
      return isAuthorized(plan) && plan.getGoal().isDefinedInputs();
   }
   
   @Override
   public Task newTask (Disco disco, Plan plan) { return plan.getGoal(); }

   public AuthorizedPlugin (Agenda agenda, int priority) { 
      super(agenda, priority);
   }

}
