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
 * Plugin which proposes to execute a non-optional task with defined inputs by self
 * or someone. Typically used by agent.
 */
public class ProposeShouldSelfPlugin extends ProposeShouldPlugin {

   // to suppress "I'm going to ..." in user TTSay menu
   private final boolean suppressExternal;
   
   @Override
   protected boolean isApplicable (Plan plan) {
      Task goal = plan.getGoal();
      return super.isApplicable(plan)
           && !(goal instanceof Propose)
           && isAuthorized(plan)
           && !(suppressExternal && self() 
                 && Utils.isTrue(goal.getExternal()))
           && !plan.isOptional() 
           && goal.isDefinedInputs();
   }
   
   @Override
   protected Task newTask (Disco disco, Plan plan) {
      return Propose.Should.newInstance(disco, self(), plan.getGoal());
   }
   
   public ProposeShouldSelfPlugin (Agenda agenda, int priority, 
                                   boolean suppressExternal) {
      super(agenda, priority);
      this.suppressExternal = suppressExternal;
   }
}
