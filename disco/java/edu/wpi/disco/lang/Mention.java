/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.lang;

import edu.wpi.cetask.*;
import edu.wpi.disco.Disco;

/**
 * Builtin utterance for mentioning a goal.   This is for typical use with other
 * application-specific information that is being provided by other methods, in order
 * to shift the focus if the goal is recognized.
 */
public class Mention extends Nested {

      public static TaskClass CLASS;
      
      // for TaskClass.newStep
      public Mention (Disco disco, Decomposition decomp, String step, boolean repeat) { 
         super(Mention.class, disco, decomp, step, repeat);
      }

      public Mention (Disco disco, Boolean external, Task goal) { 
         super(Mention.class, disco, external);
         if ( goal != null ) setSlotValue("goal", goal);
      }
      
      public Task getGoal () { return (Task) getSlotValue("goal"); }
      
      @Override
      protected Task getNested () { return getGoal(); }
      
      @Override
      public boolean interpret (Plan contributes, boolean continuation) { 
         if ( Utterance.interpret(this, contributes, continuation) ) 
            // matches to same Mention
            contributes = contributes.getParent();
         else if ( contributes != null ) 
            reconcileStack(contributes, continuation);
         getDisco().getSegment().add(this);
         return contributes != null;
      }
      
      @Override
      protected String getKey () { return "mention@word"; }
}
