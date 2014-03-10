/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */

import edu.wpi.cetask.guide.*;

import java.awt.*;

import javax.swing.*;

/**
 * A very simple example of connecting a Swing GUI to a task model.
 */
public class GuiExample extends GuiGuide {
   
   public static void main (String[] args) {
      new GuiExample(Guide.make(args)).loop();
   }
  
   public GuiExample (final Guide guide) {
      super(guide);
      // initialize model and decompose toplevel goal A
      guide.load("models/ABC.xml");
      guide.load("models/ABC-Ext.xml");
      guide.task("A");
      guide.getEngine().decomposeAll();
      // textual first input to B
      final JTextField field = new JTextField("FOO");
      // clicking on buttons is equivalent to executing the task 
      // note PlanButton is not enabled unless plan is live
      JButton b = new PlanButton("B") {
         @Override
         protected String getCommand () { 
            return "execute B / \""+field.getText()+"\" / 1 / \"y\" ";
         }
      };
      JButton c = new PlanButtonCommand("C", "execute C / 2 / 1 / 3");
      // real-time display of JavaScript global state variable
      JLabel sum = new JLabel() {
         @Override
         public String getText () { 
            return "Sum = " + guide.getEngine().eval("sum", "GuiExample");
         }
      };
      // layout hacking
      Dimension size = new Dimension(300, 150);
      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();  // for centering
      frame.addNotify(); // create peer for insets
      frame.setTitle("GuiExample");
      Insets insets = frame.getInsets();
      int width = size.width + insets.left + insets.right;
      int height = size.height + insets.top + insets.bottom;
      frame.setBounds((screen.width - width)/2, (screen.height - height)/2,  // center in screen
                     width, height);
      JPanel buttons = new JPanel(new GridLayout(1,2,20,20));
      buttons.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
      Font font = new Font("Dialog", Font.BOLD, 30);
      b.setFont(font);
      c.setFont(font);
      sum.setFont(font);
      buttons.add(b);
      buttons.add(c);
      JPanel panel = new JPanel();
      panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
      panel.add(field);
      panel.add(buttons);
      panel.add(sum);
      sum.setAlignmentX(Component.CENTER_ALIGNMENT);
      frame.getContentPane().add(BorderLayout.CENTER, panel);
      frame.pack();
      frame.setVisible(true);
    }
   
}
