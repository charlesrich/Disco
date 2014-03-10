/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package com.parc.callcenter.plugin;

import edu.wpi.cetask.*;
import edu.wpi.disco.plugin.SingletonPlugin;
import edu.wpi.disco.*;
import edu.wpi.disco.lang.*;

/**
 * User (TTSay) plugin to allow user to report on successful
 * performance of primitive non-utterance task which only user can perform.
 */
public class ProposeSucceededPlugin extends SingletonPlugin {

   @Override
   // this method is called on all live nodes in the plan tree,
   // starting with the focus
   protected boolean isApplicable (Plan plan) {
      Task goal = plan.getGoal();
      return
        // first check flag used to inhibit this plugin for particular tasks
        // e.g., Propose.Achieved(MyTask)@generate = false
        getGenerateProperty(Propose.Succeeded.class, goal) &&
        // then check whether given (live) goal is primitive
        // non-utterance task that only user can perform 
        goal.isPrimitive()&& !(goal instanceof Utterance) && goal.isUser();
   }

   @Override
   // this method is called when isApplicable() returns true
   // the returned task is added to the menu
   public Task newTask (Disco disco, Plan plan) {
      // return the utterance that will go into the menu
      // note this could be just an instance of Say
      return new Propose.Succeeded(disco, true, plan.getGoal());
   }
   
   // See CallCenterUser constructor 
   public ProposeSucceededPlugin (Agenda agenda, int priority) {
      super(agenda, priority);
   }

}

