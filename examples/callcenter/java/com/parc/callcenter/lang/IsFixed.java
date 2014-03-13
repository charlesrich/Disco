package com.parc.callcenter.lang;

import edu.wpi.cetask.*;
import edu.wpi.disco.Disco;
import edu.wpi.disco.lang.*;

public class IsFixed extends Utterance {
   
  public static TaskClass CLASS; // required
  
  // constructor for general use
  public IsFixed (Disco disco, Boolean external) { 
     super(IsFixed.class, disco, external);
  }
  
  // required for Class.newInstance
  public IsFixed ( Disco disco, Decomposition decomp, String name, boolean repeat) { 
     super(IsFixed.class, disco, decomp, name, repeat);
  }
 
}
