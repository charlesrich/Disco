
/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.plugin;

import edu.wpi.cetask.*;
import edu.wpi.disco.*;
import edu.wpi.disco.Agenda.Plugin;
import edu.wpi.disco.lang.*;

import java.util.*;

/**
 * Plugin which rejects the current (focus) task and all its parents on stack
 * (within current toplevel plan).  Typically used to 
 * generate things-to-say list. Note returns Propose.ShouldNot
 * or Propose.Stop depending on whether task started or not 
 * Controlled by Propose.ShouldNot and Propose.Stop keys.
 */
public class ProposeShouldNotPlugin extends DefaultPlugin {
   
   @Override 
   public List<Plugin.Item> apply () {
      Disco disco = getAgenda().getDisco();
      Stack<Segment> stack = disco.getStack();
      int size = stack.size();
      if ( size == 1 ) return null;
      Task should = null;
      Plan focus = disco.getFocus();
      if ( focus != null ) {
         Task goal = focus.getGoal();
         if ( goal instanceof Propose.Should ) 
            should = ((Propose.Should) goal).getGoal();
         else if ( goal instanceof Accept ) {
            Propose proposal = ((Accept) goal).getProposal();
            if ( proposal instanceof Ask.Should )
               should = ((Ask.Should) proposal).getNestedGoal();
            else if ( proposal instanceof Propose.Should )
               should = ((Propose.Should) proposal).getGoal();
         }
      }
      focus = disco.getFocus(true); // for heuristic
      List<Plugin.Item> items = new ArrayList<Plugin.Item>(size);
      for (int i = size; i-- > 1;) {
         Plan plan = stack.get(i).getPlan();
         boolean exhausted = plan.isExhausted();
         if ( exhausted ) continue;
         Task goal = plan.getGoal();
         boolean started = plan.isStarted();
         if ( goal == should // defer to RejectPlugin
               || (!started && !getGenerateProperty(Propose.ShouldNot.class, goal)) ) 
            continue;
         if ( !(goal instanceof Utterance) && !exhausted ) {
            if ( started &&
                  ( "heuristic".equals(getGeneratePropertyString(Propose.Stop.class, goal,
                        getGeneratePropertyString(Propose.ShouldNot.class, goal,
                              goal.isInternal() ? null : "heuristic"))) ?
                      // note goal of heuristic (default) is to reduce number of Propose.Stop's
                      // and make them occur at more natural places, i.e., where a
                      // control question is already being asked
                      !(focus.getGoal() instanceof Propose.Should
                         || focus.getGoal() instanceof Propose.How)
                    : !getGenerateProperty(Propose.Stop.class, goal,
                          getGenerateProperty(Propose.ShouldNot.class, goal)) ) )
               continue;
            items.add(
               new Plugin.Item(
                     started ? new Propose.Stop(disco, self(), goal) :
                        new Propose.ShouldNot(disco, self(), goal), 
                        null));
         }
         // don't go out of interruptions
         if ( disco.isTop(plan) ) break;
      }
      return items;
   }
   
   public ProposeShouldNotPlugin (Agenda agenda, int priority) { 
      super(agenda, priority);
   }
}
