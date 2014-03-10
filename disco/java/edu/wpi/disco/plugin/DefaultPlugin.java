/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.plugin;

import edu.wpi.cetask.Plan;
import edu.wpi.disco.*;
import edu.wpi.disco.Agenda.Plugin;

import java.util.List;

/**
 * Base class for plugins that are not related to any plan.
 */
public abstract class DefaultPlugin extends Agenda.Plugin {

   @Override
   public final List<Plugin.Item> apply (Plan plan) {
      if ( plan != null )
         throw new IllegalArgumentException("DefaultPlugin may not be given plan: "
               +plan);
      return apply(); 
   }

   protected abstract List<Plugin.Item> apply ();
   
   public DefaultPlugin (Agenda agenda, int priority) { 
      agenda.super(priority);
   }
}
