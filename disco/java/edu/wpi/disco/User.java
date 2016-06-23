/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco;

import edu.wpi.disco.Agenda.Plugin;
import edu.wpi.disco.lang.*;
import edu.wpi.disco.plugin.*;

import java.util.List;

public class User extends Actor {
  
   public User (String name) {
      super(name);
   }
   
   @Override
   protected void init () {
      new AskWhatPlugin(agenda, 26);
      // TTSay plugins
      new RespondPlugin.Accept(agenda, 120);
      new UtterancePlugin(agenda, 100, true); // excludeAcceptShould 
      new ProposeGlobalEnumerationPlugin(agenda, 95);
      new ProposeShouldSelfPlugin(agenda, 90, false);
      new ProposeShouldOtherPlugin(agenda, 70);
      new ProposeWhoPlugin(agenda, 50);
      new ProposeWhatEnumerationPlugin(agenda, 50);
      new RejectProposeWhatPlugin(agenda, 45); // after ProposeWhatPlugin (re enumerations)
      new RespondPlugin.Reject(agenda, 30);
      new ProposeHowPlugin(agenda, 30);
      new DecompositionPlugin(agenda, 25, true, true); // suppressFormatted, focusOnly
      new ImplicitAcceptPlugin(agenda, 25);
      new ProposeShouldOptionalPlugin(agenda, 20);
      new ProposeShouldNotPlugin(agenda, 5);
      new TopsPlugin(agenda, 0, true); // interrupt (note lowest priority)
   }
   
   /**
    * Thread-safe method to generate list of items for user utterance menu (TTSay).
    * 
    * Note interaction@ok property (default true) controls whether instance of Ok added
    * when menu is either empty or consists exclusively of Propose.ShouldNot
    * utterances.  This behavior is useful in system with rigid turn-taking,
    * where the user has to select something in order to return control to the
    * agent.  Note Ok is always added if agent has something to say.
    * 
    * @see Interaction#isOk()
    */
   @Override
   public List<Plugin.Item> generate (Interaction interaction) {
      List<Plugin.Item> items = agenda.generate(interaction);
      for (Plugin.Item item : items)
         if ( !(item.task instanceof Propose.ShouldNot) ) return items;
      Disco disco = interaction.getDisco();
      if ( disco.getProperty("interaction@ok", interaction.isOk()) ||
            interaction.getSystem().agenda.generateBest(interaction) != null )
         items.add(0, Agenda.newItem(new Ok(disco, true), null));
      return items;
   }
 
   @Override // default definition of dummy user (using console)
   protected boolean synchronizedRespond (Interaction interaction, 
         boolean ok, boolean guess) {
      return false;
   }
}
            
     
