package edu.wpi.cetask;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.concurrent.*;

import javax.swing.*;

/**
 * Optional window for running debug shell.
 */
public class ShellWindow extends JFrame implements AutoCloseable {
   
   private final Panel panel;
   
   // note need to call setVisible(true) after construction
   public ShellWindow (Shell shell, int width, int height, int fontSize, boolean append) {
      shell.setAppend(append);
      panel = new Panel(shell, fontSize, append);
      add(panel);
      setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      setSize(width, height);
      setTitle("CETask");  
   }
   
   public ShellWindow (Shell shell, int width, int height, int fontSize) {
      this(shell, width, height, fontSize, false);
   }

   protected void appendOutput (String text) { panel.appendOutput(text); }

   @Override
   public void close () { 
      // note shell.cleanup() called automatically via InterruptedException
      // below in panel readline
      dispose();
   }
   
   /**
    * Panel for running debug shell (for flexibility to embed in larger layouts).
    */
   public static class Panel extends JPanel implements ActionListener, FocusListener {

      private final Shell shell;
      private final JTextField inputField = new JTextField("Type command here");
      private final BlockingQueue<String> inputQueue = new LinkedBlockingQueue<String>();
      private final JTextArea outputPanel = new JTextArea();
      private final OutputStream outputStream = new OutputStream() {

         @Override
         public void write (int b) throws IOException {
            if ( b > 0 ) appendOutput(Character.toString((char)b));
         }

         @Override
         public void write(byte[] b, int off, int len) {
            appendOutput(new String(b, off, len));
         }
      };

      public Panel (Shell shell, int fontSize, boolean append) {
         this.shell = shell;
         TaskEngine engine = shell.getEngine();
         engine.setProperty("shell@prompt", "");
         setLayout(new BorderLayout());
         add(new JScrollPane(outputPanel));
         Font font = new Font( "Monospaced", Font.PLAIN, fontSize);
         outputPanel.setEditable(false);
         outputPanel.setFont(font);
         add(inputField, BorderLayout.SOUTH);
         inputField.addActionListener(this);
         inputField.addFocusListener(this);
         inputField.setFont(font);
         PrintStream print = new PrintStream(outputStream, true);
         shell.setOut(print);
         shell.setErr(print);
         shell.init(engine); // to set logStream
         if ( !append ) {
            OutputStream logOutputStream = new Utils.CopyOutputStream(shell.logStream, outputStream);
            System.setOut(new PrintStream(new Utils.CopyOutputStream(System.out, logOutputStream), true));
            System.setErr(new PrintStream(new Utils.CopyOutputStream(System.err, logOutputStream), true));
         }
         shell.setReader(new Shell.Reader() {

            @Override
            public String readLine () throws IOException, Shell.Quit {
               String input = null;
               while (input == null) {
                  try { input = inputQueue.take(); }
                  catch (InterruptedException e) { throw new Shell.Quit(); }
               }
               return input;
            }});
      }

      private void appendOutput (String text){
         outputPanel.append(text);
         outputPanel.setCaretPosition(outputPanel.getText().length());
      }

      @Override
      public void actionPerformed (ActionEvent e) {
         String text = inputField.getText();
         if ( !outputPanel.getText().endsWith(">> ") ) {
            appendOutput("  > ");
            shell.getPrintStream().print("  > ");
         }
         appendOutput(text); 
         appendOutput(System.lineSeparator());
         inputQueue.offer(text);
         inputField.setText("");
      }

      private boolean focusGained;
      
      @Override
      public void focusGained (FocusEvent e) {
         if ( !focusGained ) {
            inputField.setText(""); // remove "Type command here"
            focusGained = true;
         }
      } 
  
      @Override
      public void focusLost (FocusEvent e) {}     

   }
}
