/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.plugin;

import edu.wpi.cetask.*;
import edu.wpi.disco.*;
import edu.wpi.disco.Agenda.Plugin;
import edu.wpi.disco.lang.Propose;

import java.util.*;

/**
 * Plugin to propose applicable decompositions.  Used both in agent and 
 * to generate things-to-say list. Controlled by Propose.How key with 
 * default value of true for user and false for agent.
 */
public class ProposeHowPlugin extends Agenda.Plugin {
   
   @Override 
   public List<Plugin.Item> apply (Plan plan) {
      Plan contributes = plan;
      if ( plan.getGoal() instanceof Propose.How ) {
         Propose.How propose = (Propose.How) plan.getGoal();
         if ( propose.getDecomp() == null )
            plan = propose.getPlan();
      }
      if ( plan.isHow() || plan.isPrimitive() || plan.isInternal() )
         return null;
      List<DecompositionClass> decomps = plan.getDecompositions();
      if ( decomps.isEmpty() ) return null;
      List<Plugin.Item> items = new ArrayList<Plugin.Item>(decomps.size());
      for (DecompositionClass decomp : decomps)
         if ( getGenerateProperty(Propose.How.class, decomp, 
               self() ? !decomp.isInternal() : false) ) 
            items.add(new Plugin.Item(
                  new Propose.How((Disco) plan.getGoal().engine,
                                  self(), plan, decomp), contributes)); 
      return items;
   }
   
   public ProposeHowPlugin (Agenda agenda, int priority) { 
      agenda.super(priority);
   }
}
