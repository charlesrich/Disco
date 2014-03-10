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
 * TTSay plugin for utterances for which all inputs known.
 */
public class UtterancePlugin extends AuthorizedPlugin {

   private boolean excludeAcceptShould; // see User
   
   @Override
   protected boolean isApplicable (Plan plan) {
      Task goal = plan.getGoal();
      return goal instanceof Utterance && isAuthorizedDefined(plan) && 
            ( !plan.isOptional() || isAccept(plan) ) &&
            ( !excludeAcceptShould || 
              !(goal instanceof Accept && 
                    ((Accept) goal).getProposal() instanceof Propose.Should) );
   }
   
   @Override
   public Task newTask (Disco disco, Plan plan) {
      Task goal = plan.getGoal();
      // better for default formatting of 'yes' 
      return isAccept(plan) ? new Accept(getDisco(), self(), (Propose) goal) : goal;
   }

   private boolean isAccept (Plan plan) {
      Task goal = plan.getGoal();
      Plan focus = getDisco().getFocus();
      return goal instanceof Propose && focus != null &&
             focus.getGoal() instanceof Accept && 
             ((Accept) focus.getGoal()).getProposal() instanceof Propose.Should &&
             goal.equals(((Propose.Should) ((Accept) focus.getGoal()).getProposal()).getGoal());
   }
   
   public UtterancePlugin (Agenda agenda, int priority, boolean excludeAcceptShould) { 
      super(agenda, priority);
      this.excludeAcceptShould = excludeAcceptShould;
   }
}
