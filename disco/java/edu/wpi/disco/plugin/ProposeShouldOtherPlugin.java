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
 * Plugin to propose other actor execute task.  
 */
public class ProposeShouldOtherPlugin extends ProposeShouldPlugin {
   
   @Override
   public boolean isApplicable (Plan plan) {
      Task goal = plan.getGoal();
      return super.isApplicable(plan)
         && !plan.isOptionalStep() // defer to AskShouldPlugin
         && goal.getExternal() != null // defer to AskWhoPlugin
         // undefined inputs ok on user-only task
         && ( (isOther(goal) && goal.isUser())
               || (canOther(goal) && goal.isDefinedInputs()));
   }

   @Override
   public Task newTask (Disco disco, Plan plan) {
      return Propose.Should.newInstance(disco, self(), plan.getGoal());
   }
   
   public ProposeShouldOtherPlugin (Agenda agenda, int priority) { 
      super(agenda, priority);
   }
}
