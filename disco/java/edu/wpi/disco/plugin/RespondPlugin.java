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
 * Base class for plugins used to accept or reject the current (focus) proposal made 
 * by other actor or proposal this actor has been asked to make.  Typically used in 
 * TTSay. Note controlled by Accept and Reject keys.
 * 
 * @see RespondPlugin.Accept
 * @see RespondPlugin.Reject
 */
public abstract class RespondPlugin extends SingletonPlugin {

   @Override
   protected boolean isApplicable (Plan plan) {
      Task goal = plan.getGoal();
      return canSelf(goal) &&
         ( plan == getDisco().getFocus() && goal instanceof Respond
            && ( !(((Respond) goal).getProposal() instanceof Ask)
                 // default is not to respond to *whether* to answer questions
                 || getGenerateProperty(getKey(), 
                       ((Task) ((Respond) goal).getProposal()), false) )
          );
            
   }

   @Override
   public Task newTask (Disco disco, Plan plan) {
      Task goal = plan.getGoal();
      return newRespond(self(),
         goal instanceof Propose ? (Propose) goal : ((Respond) goal).getProposal()); 
   }

   protected abstract Class<? extends Nested> getKey ();

   protected abstract Task newRespond (boolean external, Propose proposal);

   protected RespondPlugin (Agenda agenda, int priority) { 
      super(agenda, priority);
   }
   
   /**
    * @see RespondPlugin
    */
   public static class Accept extends RespondPlugin {
      
      // acceptance of proposed ground answers to questions is handled 
      // by UtterancePlugin
      
      @Override
      protected Class<? extends Nested> getKey () { 
         return edu.wpi.disco.lang.Accept.class; 
      }
      
      @Override
      protected Task newRespond (boolean external, Propose proposal) {
         return new edu.wpi.disco.lang.Accept(getDisco(), external, proposal);
      }

      public Accept (Agenda agenda, int priority) { 
         super(agenda, priority);
      }
   }
   
   /**
    * @see RespondPlugin
    */
   public static class Reject extends RespondPlugin {
      
      @Override
      protected boolean isApplicable (Plan plan) {
         if ( super.isApplicable(plan) ) return true;
         Task goal = plan.getGoal();
         return canSelf(goal) &&
             plan == getDisco().getFocus(true) && goal instanceof Propose
             && getGenerateProperty(getKey(), goal, true) 
             // only reject ground (concrete) proposals
             && goal.isDefinedInputs();
      }
      
      @Override
      protected Class<? extends Nested> getKey () { 
         return edu.wpi.disco.lang.Reject.class; 
      }
      
      @Override
      protected Task newRespond (boolean external, Propose proposal) {
         return new edu.wpi.disco.lang.Reject(getDisco(), external, proposal);
      }

      public Reject (Agenda agenda, int priority) { 
         super(agenda, priority);
      }
   }
}

