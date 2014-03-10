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
 * Plugin to include Propose.Should and Propose.ShouldNot 
 * for optional steps in user menu.
 */
public class ProposeShouldOptionalPlugin extends Agenda.Plugin {
     
   @Override 
   public List<Plugin.Item> apply (Plan plan) {
      Task goal = plan.getGoal();
      Propose.Should should = isApplicable(plan, Propose.Should.class) ?
         (plan.getRepeatStep() >= 1 ?
            new Propose.Should.Repeat(getDisco(), self(), goal) :
            Propose.Should.newInstance(getDisco(), self(), goal))
         : null;
      Propose.ShouldNot shouldNot = isApplicable(plan, Propose.ShouldNot.class) ?
         new Propose.ShouldNot(getDisco(), self(), goal)
         : null;   
      List<Plugin.Item> items = null;
      if ( !(should == null && shouldNot == null) ) { 
         items = new ArrayList<Plugin.Item>(2);
         if ( should != null ) items.add(new Plugin.Item(should, plan));
         if ( shouldNot != null ) items.add(new Plugin.Item(shouldNot, plan));
      }
      return items;
   }

   private boolean isApplicable (Plan plan, Class<? extends Nested> generate) {
      Task goal = plan.getGoal();
      Plan parent = plan.getParent();
      return getGenerateProperty(generate, goal)
               && !(goal instanceof Utterance) && !goal.isInternal()
               && !(goal instanceof Propose)
               && (plan.isOptional() )
               // defer to parent
               && !(parent != null && isApplicable(parent, generate)); 
   }
    
   public ProposeShouldOptionalPlugin (Agenda agenda, int priority) { 
      agenda.super(priority);
   }
}
