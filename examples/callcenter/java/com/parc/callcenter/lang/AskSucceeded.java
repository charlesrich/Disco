package com.parc.callcenter.lang;

import edu.wpi.cetask.*;
import edu.wpi.disco.Disco;
import edu.wpi.disco.lang.*;

public class AskSucceeded extends Ask.Nested {
   
   // all of this boiler plate with constructors is needed to fit in with
   // implementation scheme of the rest of the Propose/Ask utterance types
   
   // this is very similar to Ask.Should (will probably add an abstract
   //  class to capture shared code)
   
   // TODO: need to provide a method to extend classification logic
   // in Propose.Should.newInstance() to coerce instances of
   // Propose.Should(Propose.Succeeded) into AskSucceeded.  This is a minor
   // problem.
   
   public static TaskClass CLASS;
    
   // for TaskClass.newStep
   public AskSucceeded (Disco disco, Decomposition decomp, String step, boolean repeat) { 
      super(AskSucceeded.class, disco, decomp, step, repeat);
   }
   
   // for extensions
   protected AskSucceeded (Class<? extends AskSucceeded> cls, Disco disco,
                     Decomposition decomp, String step, boolean repeat) { 
      super(cls, disco, decomp, step, repeat);
   }

   // for extension
   protected AskSucceeded (Class<? extends AskSucceeded> cls, Disco disco,
                     Boolean external, Propose.Nested goal) {
      super(cls, disco, external, goal);
   }
   
   public AskSucceeded (Disco disco, Boolean external, Propose.Succeeded task) {
      super(AskSucceeded.class, disco, external, task);
      if ( task != null ) {
         Task nested = task.getGoal();
         if ( nested != null ) setSlotValue("goal", nested);
      }
   }
   
   public AskSucceeded (Disco disco, Boolean external, Task nested) {
      this(disco, external, null);
      setSlotValue("goal", nested);
      cache = true;
   }

   @Override
   public Propose.Succeeded getGoal () { return (Propose.Succeeded) super.getGoal(); }
   
   @Override
   protected Propose.Succeeded newGoal () {
      return new Propose.Succeeded(getDisco(), Utils.not(getExternal()), 
                                        getNestedGoal());
   }

   @Override
   // to improve default formatting
   public String formatTask () {
      String format = getDisco().getFormat(this);
      if ( format != null) return formatTask(format, null);
      StringBuffer buffer = new StringBuffer();
      buffer.append("were we successful to ").append(getNestedGoal().formatTask()).append('?');
      return buffer.toString();
   }   

}
