/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.lang;

import edu.wpi.cetask.*;
import edu.wpi.disco.Disco;

// TODO Use this to reimplement ProposeShouldNot

/**
 * Builtin utterance for rejecting a proposal.
 */
public class Reject extends Respond {

   public static TaskClass CLASS;
   
   // for TaskClass.newStep
   public Reject (Disco disco, Decomposition decomp, String step, boolean repeat) { 
      super(Reject.class, disco, decomp, step, repeat);
   }
   
   public Reject (Disco disco, Boolean external, Propose proposal) { 
      super(Reject.class, disco, external, proposal);
   }
   
   @Override
   protected void respond (Propose proposal, Plan contributes) {
      proposal.reject(contributes, false);
   }
   
   @Override
   protected String getKey () { return "reject@word"; }
}
