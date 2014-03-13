/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.lang;

import java.util.List;

import edu.wpi.cetask.*;
import edu.wpi.disco.Agenda.Plugin;
import edu.wpi.disco.*;

/**
 * Builtin primitive action for representing TTSay menu presentation (see Disco.xml)
 * Only can be performed by system.
 */
public class TTSay extends Decomposition.Step {

   public static TaskClass CLASS;
   
   // for TaskClass.newStep
   public TTSay (Disco disco, Decomposition decomp, String name, boolean repeat) { 
      super(disco.getTaskClass(TTSay.class.getName()), disco, decomp, name);
   }
   
   /**
    * @param choice (null if no choice made, i.e., action failed)
    */
   public TTSay (Disco disco, List<Plugin.Item> items, Integer choice) { 
      this(disco, null, null, false);
      setSlotValue("external", false);
      if ( items != null ) setSlotValue("items", items);
      if ( choice != null ) {
         setSlotValue("choice", choice);
         setSuccess(true);
      } else setSuccess(false);
   }
}
