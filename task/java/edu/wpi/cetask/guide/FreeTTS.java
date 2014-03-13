/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.cetask.guide;

import com.sun.speech.freetts.*;

public class FreeTTS implements SpeechGuide.Generator {
   
   private final Voice voice = VoiceManager.getInstance().getVoice("kevin16"); 
   
   FreeTTS () { voice.allocate(); }

   @Override
   public void generate (String text) {
      voice.speak(text);
   }
}
