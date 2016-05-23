/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.lang;

import edu.wpi.cetask.*;
import edu.wpi.disco.Disco;

/**
 * Builtin utterance similar to {@link Say} for mentioning a goal. This is for
 * typical use with other application-specific information that is being
 * provided by other methods in order to shift the focus
 * if the goal is recognized.  
 * <p>
 * Note that mentioned task only is printed in DEBUG mode.
 */
public class Mention extends Nested implements Utterance.Text {

   public static TaskClass CLASS;

   // for TaskClass.newStep
   public Mention (Disco disco, Decomposition decomp, String step, boolean repeat) { 
      super(Mention.class, disco, decomp, step, repeat);
   }

   public Mention (Disco disco, Boolean external, Task goal, String text) { 
      super(Mention.class, disco, external);
      if ( goal != null ) setSlotValue("goal", goal);
      if ( text != null ) setSlotValue("text", text);
   }

   public Task getGoal () { return (Task) getSlotValue("goal"); }

   @Override
   public String getText () { return (String) getSlotValue("text"); }

   @Override
   protected Task getNested () { return getGoal(); }

   @Override
   protected String getKey () { return "mention@word"; }

   @Override
   public String formatTask () { return Utterance.Text.formatTask(this); }
   
   @Override
   public String toHistoryString (boolean formatTask) { 
      return Utterance.Text.toHistoryString(this, formatTask); 
   }

}
