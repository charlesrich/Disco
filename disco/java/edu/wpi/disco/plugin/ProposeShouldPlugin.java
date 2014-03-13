/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.plugin;

import edu.wpi.cetask.*;
import edu.wpi.disco.Agenda;
import edu.wpi.disco.lang.*;

/**
 * Base plugin for generating Propose.Should and subclasses.  Prevents proposing utterances
 * other than nested propose (ask).
 */
abstract public class ProposeShouldPlugin extends SingletonPlugin {

   @Override
   protected boolean isApplicable (Plan plan) {
      return isApplicablePlan(plan) 
            && getGenerateProperty(getGenerateClass(), plan.getGoal()); 
   }

   protected boolean isApplicablePlan (Plan plan) {
      Task goal = plan.getGoal();
      return (!(goal instanceof Utterance) || goal instanceof Propose);
   }
   
   protected Class<? extends Nested> getGenerateClass () { return Propose.Should.class; }
   
   public ProposeShouldPlugin (Agenda agenda, int priority) { 
      super(agenda, priority);
   }
}
