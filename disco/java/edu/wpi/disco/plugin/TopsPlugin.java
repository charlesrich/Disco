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
 * Plugin to propose non-primitive toplevel plans. If interrupt argument is false, then only
 * proposes when stack is empty; otherwise can propose interruptions unless 'interrupt'
 * property is false.
 * Typically used to generate things-to-say list. 
 */
public class TopsPlugin extends DefaultPlugin {

   private boolean interrupt;
   
   public void setInterrupt (boolean interrupt) { this.interrupt = interrupt; }
   
   @Override 
   public List<Plugin.Item> apply () {
      Disco disco = getAgenda().getDisco();
      Plan focus = disco.getFocusExhausted(true);
      if ( focus != null && !interrupt ) return null;
      List<TaskClass> tops = disco.getTopClasses();
      // current toplevel plan, if any
      TaskClass top = focus == null ? null :
         disco.getTop(focus).getGoal().getType(); 
      List<Plugin.Item> items = new ArrayList<Plugin.Item>(tops.size());
      Stack<Segment> stack = disco.getStack();
      int size = stack.size()-1; // ignore focus (already checking top)
      for (TaskClass type : tops) {
         if ( top != type && !type.isPrimitive() && !type.isInternal()
               && type.getProperty("@interrupt", true) ) {
            Task instance = null;
            for (int i = size; i-- > 1;) { // ignore root node
               Task purpose = stack.get(i).getPurpose();
               if ( purpose.getType() == type ) {
                  instance = purpose; // don't make new instance
                  break;
               }
            }
            // TODO cache anonymous instances of toplevel tasks?
            if ( instance == null ) instance = type.newInstance();
            // filter out inappropriate instances
            if ( Utils.isFalse(instance.isApplicable()) || 
                  (type.isSufficient() && Utils.isTrue(instance.isAchieved())) )
               continue;
            items.add(
               new Plugin.Item(Propose.Should.newInstance(disco, self(), instance), null));
         }
      }
      return items;
   }
   
   public TopsPlugin (Agenda agenda, int priority, boolean interrupt) { 
      super(agenda, priority);
      this.interrupt = interrupt;
   }
}
