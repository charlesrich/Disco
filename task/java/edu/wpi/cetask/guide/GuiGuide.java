/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.cetask.guide;

import edu.wpi.cetask.Plan;

import java.awt.event.*;

import javax.swing.*;

/**
 * Abstract class for connecting Swing GUI to Guide.  See ExampleGui (in unnamed
 * package) for example of use.
 */
public abstract class GuiGuide {
   
   final protected JFrame frame = new JFrame("CETask");
   final protected Guide guide; //may be SpeechGuide

   protected GuiGuide (Guide guide) {
      this.guide = guide;
      guide.setOnProcessCommand(new Runnable() {
         @Override
         public void run () { frame.pack(); frame.repaint(); } // update gui
      });
      frame.addWindowListener(new WindowAdapter () {
         @Override
         public void windowClosing (WindowEvent event) { 
            frame.dispose();            
            GuiGuide.this.guide.deallocate();
         }
      });
   }
   
   protected void loop () { guide.loop(); }
   
   /**
    * Call this method in ActionListener's of GUI
    */
   protected boolean processCommand (String command) {
      try { 
         System.out.println(command);  //echo
         try { guide.processCommand(command); }
         finally { System.out.print(guide.getEngine().getProperty("prompt@word")); }
      } catch (Throwable throwable) { return false;}
      return true;
    }
   
   /**
    * Useful class for a button associated with the plan for primitive 
    * or composite task instance.  Button is enabled iff plan is live.
    * Clicking on button is equivalent to executing computed command.
    */
   abstract public class PlanButton extends JButton {
      
      final protected Plan plan;

      public PlanButton (String task) { 
         super(task); 
         // assuming exactly one matching instance in current engine state
         plan =  guide.getEngine().explain(
               guide.processTask(task, null, false), false).get(0);
         addActionListener(new ActionListener() { 
            @Override
            public void actionPerformed (ActionEvent event) { 
               if ( isEnabled() ) processCommand(getCommand());
            }
         });
         setFocusable(false);
      }
      
      /**
       * Override this method to compute command.
       */
      abstract protected String getCommand ();
         
      @Override
      public boolean isEnabled () { return plan != null && plan.isLive(); }
   }

   
   /**
    * PlanButton with fixed command.
    *
    */
   public class PlanButtonCommand extends PlanButton {
      
      final private String command;
      
      public PlanButtonCommand (String task, String command) {
         super(task);
         this.command = command;
      }

      @Override
      protected String getCommand () { return command; }
   }
}
