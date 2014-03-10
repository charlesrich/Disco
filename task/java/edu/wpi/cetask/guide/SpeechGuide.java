/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.cetask.guide;

import edu.wpi.cetask.*;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;

public class SpeechGuide extends Guide {
   
   private boolean mute;
   
   public static void main (String[] args) {
      make(args).loop();
   }

   public static SpeechGuide make (String[] args) { // for use in other main's
      VERSION += " - Speech";
      System.setProperty( "freetts.voices", 
            "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory" );
      Recognizer sphinx = new Sphinx4();
      SpeechGuide guide = new SpeechGuide(
            new TaskEngine(),
            args.length > 0 ? args[0] : null,
         sphinx, new FreeTTS());
      sphinx.setGuide(guide);
      if ( args.length > 0 ) guide.mute = true;
      return guide;
   }
      
   /**
    * Simple wrapper for speech recognition system
    */
   public interface Recognizer {
      
      void setGuide (SpeechGuide guide);
      
      /**
       * Turn on microphone for one utterance (automatic endpointing)
       * @return recognized string
       */
      String recognize ();

      /**
       * @param utterance - natural language string
       * @return semantics of utterance 
       */
      Object interpret (String utterance);
      
      /**
       * Load and start using specified grammar (replaces current grammar)
       */
      void loadGrammar (String uri) throws IOException;
      
      /**
       * Free up threads, etc., so can exit cleanly 
       */
      void deallocate ();
   }
   
   /**
    * Simple wrapper for text-to-speech system
    */
   public interface Generator {
      
      /**
       * Generate speech for given text
       */
      void generate (String text);
   }
   
   final protected Recognizer recognizer;
   final protected Generator generator;
   
   protected SpeechGuide (TaskEngine engine, String filename, 
         Recognizer recognizer, Generator generator) {
      super(engine, filename);
      this.recognizer = recognizer;
      this.generator = generator;
      engine.setOnLoading(new TaskEngine.OnLoading () {
         @Override
         public void onLoad (String uri, TaskModel model) {
            // TODO internationalize with resource bundles
            uri = Utils.replaceEndsWith(uri, ".xml", ".gram");
            try { 
               SpeechGuide.this.recognizer.loadGrammar(uri); 
               if ( TaskEngine.VERBOSE ) 
                  println("# Loaded speech grammar "+uri);
            } catch (IOException e) {} // ignore if no grammar
         }
      });
      // make control key push-to-talk
      // note this only works if there is a window with focus
      KeyboardFocusManager.getCurrentKeyboardFocusManager()
        .addKeyEventDispatcher(new KeyEventDispatcher () {
           @Override
         public boolean dispatchKeyEvent (KeyEvent event) {
              if ( event.getID() ==  KeyEvent.KEY_PRESSED
                    && event.getKeyCode() == KeyEvent.VK_CONTROL)
		  // note ptt is called in separate thread, so there will be
		  // no command loop prompt (and command loop should not be
		  // used simultaneously)
                 try { ptt(null); }
                 catch (Throwable e) { Utils.rethrow(e); }
              return false;
           }
        });
   }
   
   @Override
   public void deallocate () {
      recognizer.deallocate();
      // dirty force exit because some part of speech stuff hangs
      System.exit(0);
   }
   
   @Override
   protected void println (Object object) {
      super.println(object);
      if ( !mute ) {
         String string = object.toString();
         if ( string.charAt(0) != '#' )
            generator.generate(object.toString());
      }
   }

   // two new commands

   public void ptt (String ignore) throws Throwable { 
      String utterance = recognizer.recognize();
      if ( utterance == null || utterance.length() == 0 ) respond("repeat@word");
      else {
         super.println("say "+utterance);
         say(utterance);
      }
   }
   
   public void say (String utterance) throws Throwable {
      // for this guide, semantics is simply a command string
      String command = (String) recognizer.interpret(utterance);
      if ( command == null ) respond("rephrase@word");
      else {
         if ( TaskEngine.DEBUG ) println("# "+command);
         processCommand(command);
      }
   }
   
   @Override
   protected void help () {
      super.help();
      System.out.println("    ptt                 - push to talk [control key]");
      System.out.println("    say <utterance>     - natural language text input");
   }
}
