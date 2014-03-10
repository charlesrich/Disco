/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package com.parc.callcenter.plugin;

import java.util.*;
import com.parc.callcenter.lang.AskSucceeded;
import edu.wpi.cetask.*;
import edu.wpi.disco.*;
import edu.wpi.disco.Agenda.Plugin;
import edu.wpi.disco.lang.Propose;
import edu.wpi.disco.plugin.DefaultPlugin;

/**
 * Agent plugin to ask whether completed non-primitive task in current focus has
 * unknown success.  Note this needs to be default plugin because
 * regular plugins are only applied to live plans and and completed
 * task is no longer live.
 */
public class AskSucceededPlugin extends DefaultPlugin {
   
   @Override
   // this method is called once on each agent turn.
   protected List<Plugin.Item> apply () {
      Plugin.Item item = applyFocus(getDisco().getFocus(true));
      return item == null ? null : Collections.singletonList(item);
   }
 
   private Plugin.Item applyFocus (Plan plan) {
      // don't ask again if just asked
      if ( plan == null || plan.getType() == Propose.Succeeded.CLASS ) return null;
      Task goal = plan.getGoal();
      return (!goal.isPrimitive() 
              // check flag used to inhibit this plugin for particular tasks
              // e.g., AskAchieved(MyTask)@generate = false
              && getGenerateProperty(AskSucceeded.class, goal) 
              && plan.isDone()
              && goal.getSuccess() == null ) ?
         new Plugin.Item(new AskSucceeded(getDisco(), false, goal), plan)
         // recurse up the tree
         : applyFocus(plan.getParent());
   }
   
   // see CallCenterAgent constructor
   public AskSucceededPlugin (Agenda agenda, int priority) { 
      super(agenda, priority);
   }
}
