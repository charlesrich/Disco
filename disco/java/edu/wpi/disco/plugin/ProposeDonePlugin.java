/* Copyright (c) 2015 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.plugin;

import java.util.*;
import edu.wpi.cetask.*;
import edu.wpi.disco.*;
import edu.wpi.disco.Agenda.Plugin;
import edu.wpi.disco.lang.Propose;

/**
 * Plugin to propose that currently focused non-primitive task is complete
 * (postcondition may be null). Not currently used by default agent (see
 * examples/learn-do-teach/UnifiedAgent)
 */
public class ProposeDonePlugin extends DefaultPlugin {
   
   // TODO go through stack starting at bottom and return first appropriate element, if any
   
   // note this must be a DefaultPlugin because the non-primitive task is no longer live
   
   @Override
   protected List<Plugin.Item> apply () {
      Plan focus = getDisco().getFocus(true);
      return ( focus != null && !focus.isPrimitive() && focus.isComplete() 
               && getGenerateProperty(Propose.Done.class, focus.getGoal()) ) ?
         Collections.singletonList(
               new Plugin.Item(new Propose.Done(getDisco(), self(), focus.getGoal()), focus))
         : null;
   }
   
   public ProposeDonePlugin (Agenda agenda, int priority) { 
      super(agenda, priority);
   }

}
