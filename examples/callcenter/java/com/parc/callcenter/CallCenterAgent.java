package com.parc.callcenter;

import com.parc.callcenter.plugin.AskSucceededPlugin;
import edu.wpi.disco.Agenda.Plugin;
import edu.wpi.disco.*;
import edu.wpi.disco.lang.Say;

public class CallCenterAgent extends Agent {

   public CallCenterAgent (String name) {
      super(name);
      // below is where you add new plugins (or remove some added by
      // superclass constructor)
      new AskSucceededPlugin(agenda, 285); // pretty high priority
   }

   private boolean once;

   // use this main instead of Interaction.main() for console debugging
   // also see bin/callcenter bash script
   public static void main (String[] args) {
      Interaction interaction = new Interaction(new CallCenterAgent("agent"),
            new CallCenterUser("caller"), args.length > 0 && args[0].length() > 0 ? args[0]
               : null);
      // note false gives agent first turn
      interaction.start(false);
   }

   @Override
   public Plugin.Item respondIf (Interaction interaction, boolean guess, boolean retry) {
      // here is where you put logic for generating agent responses that
      // do *not* depend on the plan tree (and therefore not appropriate for
      // implementation as plugins).  Note that unlike DefaultPlugin's,
      // this behavior *replaces* all of plugins.
      if ( !once ) {
         once = true;
         return Agenda.newItem(
                  new Say(interaction.getDisco(), false,
                        "Please hold for the next operator"), 
                  null);
      }
      return super.respondIf(interaction, guess, retry);
   }

}