/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.lang;

import edu.wpi.cetask.*;
import edu.wpi.disco.Disco;

/**
 * Builtin utterance for acknowledgement (see Disco.xml)
 */
public class Ok extends Utterance {

   public static TaskClass CLASS;
   
   // for TaskClass.newInstance
   public Ok ( Disco disco, Decomposition decomp, String name, boolean repeat) { 
      super(Ok.class, disco, decomp, name, repeat);
   }
   
   public Ok (Disco disco, Boolean external) { 
      super(Ok.class, disco, external);
   }
}
