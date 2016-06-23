/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.plugin;

import edu.wpi.disco.Agenda;

import java.util.*;

import javax.script.*;

/**
 * Abstract class to propose enumerated values.  Typically used to 
 * generate things-to-say list. 
 * 
 * @see ProposeGlobalEnumerationPlugin
 * @see ProposeWhatEnumerationPlugin
 */
public abstract class EnumerationPlugin extends Agenda.Plugin {
   
   private final Bindings bindings = new SimpleBindings(); 
   
   protected List<Object> values (String type) {
      if ( "string".equals(type) || "number".equals(type) )
         return null;
      List<Object> list = new ArrayList<Object>();
      synchronized (bindings) {
         bindings.put("$$list", list);
         // see helper functions in cetask default.xml      
         if ( "boolean".equals(type) ) {
            getAgenda().getDisco().eval("edu.wpi.disco_helper.booleanValues($$list)",
                  bindings, "EnumerationPlugin");
            return list;
         }
         return getAgenda().getDisco().eval(
               new StringBuilder("edu.wpi.disco_helper.values(").append(type)
               .append(",$$list)").toString(), 
               bindings, "EnumerationPlugin") == null ? null : list;
      }
   }

   protected boolean getMenuProperty (String type) {
      return getDisco().getProperty(type+"@menu", true);
   }
   
   public EnumerationPlugin (Agenda agenda, int priority) { 
      agenda.super(priority);
   }
}
