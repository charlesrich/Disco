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
 * Plugin which asks who should do primitive task with undefined external slot
 */
public class AskWhoPlugin extends SingletonPlugin {
   
   @Override
   protected boolean isApplicable (Plan plan) {
      Task goal = plan.getGoal();
      return !(goal instanceof Utterance || goal.isInternal()) && 
             goal.isPrimitive() && goal.getExternal() == null;
   }
   
   @Override
   protected Task newTask (Disco disco, Plan plan) {
      return new Ask.Who(disco, self(), plan.getGoal());
   }

   public AskWhoPlugin (Agenda agenda, int priority) { 
      super(agenda, priority);
   }
}
