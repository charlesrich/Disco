/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.plugin;

import edu.wpi.cetask.*;
import edu.wpi.disco.Agenda;

/**
 * Plugin to enquire about optional tasks which have live successors 
 * (and therefore would be passed if the successors proposed first).
 */
public class AskShouldPassablePlugin extends AskShouldPlugin {
   
   @Override
   public boolean isApplicable (Plan plan) {
      Plan parent = plan.getParent();
      return super.isApplicable(plan) && 
            // defer to AskShouldPlugin for parent
            !(parent != null && super.isApplicablePlan(parent)) && 
            plan.hasLiveSuccessor();
   }
   
   public AskShouldPassablePlugin (Agenda agenda, int priority) { 
      super(agenda, priority);
   }
}
