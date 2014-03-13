package com.parc.callcenter;

import com.parc.callcenter.plugin.*;
import edu.wpi.disco.User;

public class CallCenterUser extends User {

   public CallCenterUser (String name) {
      super(name);
      // below is where you add new plugins (or remove some added by
      // superclass constructor)
      new ProposeSucceededPlugin(agenda, 3); // low priority so low on menu
   }

}
