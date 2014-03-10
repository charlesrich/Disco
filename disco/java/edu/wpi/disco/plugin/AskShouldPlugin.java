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
 * Plugin to enquire about tasks which are not authorized and are 
 * either optional or unstarted toplevel or only self can perform.
 * Controlled by Ask.Should key.
 * 
 * @see AskShouldPassablePlugin
 */
public class AskShouldPlugin extends ProposeShouldPlugin {
   
   @Override
   protected boolean isApplicablePlan (Plan plan) {
      Task goal = plan.getGoal();
      Plan parent = plan.getParent();
      return super.isApplicablePlan(plan)
               && !(goal instanceof Propose)
               && goal.getShould() == null
               && (plan.isOptional() || (getDisco().isTop(plan) && !plan.isStarted())
                     || (canSelf(goal) && !canOther(goal)) )
               && !isAuthorized(plan) 
               // always defer to parent
               && !(parent != null && isApplicable(parent)); 
   }
    
   @Override
   protected Class<? extends Nested> getGenerateClass () { return Ask.Should.class; }
   
   @Override
   public Task newTask (Disco disco, Plan plan) {
      return plan.getRepeatStep() >= 1 ?
         new Ask.Should.Repeat(disco, self(), plan.getGoal()) :
            new Ask.Should(disco, self(), plan.getGoal());
   }
   
   public AskShouldPlugin (Agenda agenda, int priority) { 
      super(agenda, priority);
   }
}
