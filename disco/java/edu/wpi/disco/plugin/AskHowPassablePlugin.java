/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.plugin;

import edu.wpi.cetask.Plan;
import edu.wpi.disco.Agenda;

/**
 * Plugin to capture common dialogue authoring pattern in which there are several
 * options including doing nothing.  This is represented using an internal optional
 * task with a decomposition and @generate flag for each option.  Note that task
 * is internal so that there is no Ask.Should generated and option remains unaccepted
 * while alternative decompositions are entertained (so successor stays live).
 * 
 * Similar to {@link AskShouldPassablePlugin} in that plugin needs to have higher
 * priority than other plugins, since the optional step would be passed if 
 * steps of any live successors proposed or executed first.
 */
public class AskHowPassablePlugin extends AskHowPlugin {
   
   @Override
   public boolean isApplicable (Plan plan) {
      return plan.isInternal() && plan.hasLiveSuccessor() && super.isApplicable(plan)
            && decomp != null;
   }
   
   public AskHowPassablePlugin (Agenda agenda, int priority) { 
      super(agenda, priority);
   }
}
