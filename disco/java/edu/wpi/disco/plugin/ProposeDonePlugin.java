/* Copyright (c) 2015 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.plugin;

import java.util.*;
import edu.wpi.cetask.*;
import edu.wpi.disco.*;
import edu.wpi.disco.Agenda.Plugin;
import edu.wpi.disco.lang.Propose;

/**
 * Plugin to propose that done and exhausted highest-level focused non-primitive task is 
 * done (postcondition may be null). Not currently used by default agent (see
 * examples/learn-do-teach/UnifiedAgent)
 */
public class ProposeDonePlugin extends DefaultPlugin {
   
   // note this must be a DefaultPlugin because the non-primitive task is no longer live
   
   @Override
   protected List<Plugin.Item> apply () {
      for (Segment segment : getDisco().getStack()) {
         Plan plan = segment.getPlan();
         if ( plan == null ) continue;
         Task goal = plan.getGoal();
         if ( getGenerateProperty(Propose.Done.class, goal)
               && !plan.isPrimitive() && plan.isDone() && plan.isExhausted() ) 
            return Collections.singletonList(
               new Plugin.Item(new Propose.Done(getDisco(), self(), goal), plan));
      }
      return null;
   }
   
   public ProposeDonePlugin (Agenda agenda, int priority) { 
      super(agenda, priority);
   }

}
