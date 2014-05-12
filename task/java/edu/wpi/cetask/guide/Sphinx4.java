/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.cetask.guide;

import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.jsapi.JSGFGrammar;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.wpi.cetask.Utils;

import java.io.IOException;
import java.net.URL;

import javax.speech.recognition.*;

public class Sphinx4 implements SpeechGuide.Recognizer {
   
   private Recognizer recognizer;
   private Microphone microphone;
   private JSGFGrammar jsgf;
   private RuleGrammar rules;
   private SpeechGuide guide;
   
   Sphinx4 () {
      try {
         ConfigurationManager cm = new ConfigurationManager(
               Sphinx4.class.getResource("sphinx4.config.xml"));
         recognizer = (Recognizer) cm.lookup("recognizer");
         microphone = (Microphone) cm.lookup("microphone");
         recognizer.allocate();
         jsgf = (JSGFGrammar) cm.lookup("jsgfGrammar"); 
         rules = jsgf.getRuleGrammar();
      } catch (Exception e) { Utils.rethrow(e); }
   }
       
   // implementation of Recognizer interface

   @Override
   public void setGuide (SpeechGuide guide) { this.guide = guide; }
   
   @Override
   public String recognize () { 
      // assuming automatic endpointing and that 'closeBetweenUtterances' property 
      // of microphone component is set to "false" in config file
      microphone.clear();
      microphone.startRecording();
      Result result = recognizer.recognize();
      // TODO following causes exception on Mac (audio line running)
      // but it's absence may cause memory overflow if running long between ptt's
      // microphone.startRecording();
      return result == null ? null : result.getBestFinalResultNoFiller();
   }

   @Override
   public Object interpret (String utterance) {
      RuleParse parse = null;
      try { parse = rules.parse(utterance, null); } 
      catch (GrammarException e) { guide.exception(e); }
      if ( parse == null ) return null;
      String[] tags = parse.getTags();
      if ( tags == null ) return null;
      StringBuilder buffer = new StringBuilder();
      for (int i = 0; i < tags.length; i++) { 
         if ( i > 0 ) buffer.append(' ');
         buffer.append(tags[i].trim());
      }
      return buffer.toString();
   }
   
   @Override
   public void loadGrammar (String uri) throws IOException {
      String spec = Utils.toURL(uri).toExternalForm();
      int i = spec.lastIndexOf('/');
      jsgf.setBaseURL(new URL(spec.substring(0, i)));
      jsgf.loadJSGF(spec.substring(i+1, spec.length()-5));
      rules = jsgf.getRuleGrammar();
   }
   
   @Override
   public void deallocate () { 
      recognizer.deallocate(); 
      microphone.stopRecording();
   }

}
