/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.plugin;

import edu.wpi.cetask.*;
import edu.wpi.disco.*;
import edu.wpi.disco.lang.Ask;

import java.util.List;

/**
 * Plugin that asks about (applicable, non-rejected, non-internal) decomposition classes.
 * If there is only one such decomposition for given plan: if it is authorized then 
 * do not ask at all, otherwise, either ask about it specifically or, if its Ask.How
 * key is false, then ask general question.  If there is more than one decomposition, ask
 * specifically about the first one that has a true Ask.How key, or if there are none 
 * such, ask a general question. 
 */
public class AskHowPlugin extends SingletonPlugin {
   
   @Override
   protected boolean isApplicable (Plan plan) {
      // allowed to ask about decomps of internal task if specifically requested 
      // by true key (see below)
      if ( plan.isHow() || plan.isPrimitive()
            // if general Ask.How(goal)@generate is false, then do nothing
            || Utils.isFalse(getGenerateProperty(Ask.How.class, plan.getGoal(), (Boolean) null)) )
         return false;
      List<DecompositionClass> decomps = plan.getDecompositions();
      if ( decomps.isEmpty() ) return false;
      if ( decomps.size() == 1 ) {
         decomp = decomps.get(0);
         if ( decomp.isInternal() || decomp.getProperty("@authorized", false) ) 
            return false;
         if ( Utils.isFalse(getGenerateProperty(Ask.How.class, decomp, null)) )
            decomp = null; 
         return true;
      }
      decomp = null;
      int count = 0;
      for (DecompositionClass decomp : decomps) 
         if ( !decomp.isInternal() ) {
            // ok to ask about decomp for internal *task* if key is true
            if ( Utils.isTrue(getGenerateProperty(Ask.How.class, decomp, null)) ) {
               this.decomp = decomp;
               return true;
            }
            // but never ask general question about internal task
            if ( !plan.isInternal() && ++count > 1 ) return true;
         }
      return false;
   }
   
   protected DecompositionClass decomp;
   
   @Override
   protected Task newTask (Disco disco, Plan plan) {
      return new Ask.How(disco, self(), plan, decomp);
   }

   public AskHowPlugin (Agenda agenda, int priority) { 
      super(agenda, priority);
   }
}
