/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.lang;

import edu.wpi.cetask.*;
import edu.wpi.disco.Disco;

/**
 * Base class for responding to a proposal (see {@link Accept} and {@link Reject}).
 */
abstract public class Respond extends Nested {

   // for extensions
   protected Respond (Class<? extends Respond> cls, Disco disco, Decomposition decomp, 
         String step, boolean repeat) { 
      super(cls, disco, decomp, step, repeat);
   }

   // for extensions
   protected Respond (Class<? extends Respond> cls, Disco disco, Boolean external, 
         Propose proposal) { 
      super(cls, disco, external);
      if ( proposal != null ) setSlotValue("proposal", proposal);
   }
   
   public Propose getProposal () { return (Propose) getSlotValue("proposal"); }

   @Override
   protected Task getNested () { return (Task) getProposal(); }
   
   abstract protected void respond (Propose proposal, Plan contributes);
   
   @Override
   public boolean interpret (Plan contributes, boolean continuation) {
      if ( super.interpret(contributes, continuation) ) {
         // match to same Respond
         respond(((Respond) contributes.getGoal()).getProposal(), contributes);
         engine.clearLiveAchieved(); // response can change liveness and achieved
         return true;
      } 
      respond(getProposal(), contributes); // even if contributes null
      if ( contributes != null ) {
         engine.clearLiveAchieved(); // response can change liveness and achieved
         reconcileStack(contributes, continuation);
         return true;
      }
      return false;
   }
   
   @Override
   public String toHistoryString (boolean formatTask) {
      return TaskEngine.VERBOSE ? formatNestedDefault(formatTask) :
         engine.getProperty(getKey());
   }
}
