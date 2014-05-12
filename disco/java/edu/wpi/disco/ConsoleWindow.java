package edu.wpi.disco;

import edu.wpi.cetask.ShellWindow;

import java.awt.event.*;
import java.io.File;

public class ConsoleWindow extends ShellWindow {

   private final Interaction interaction;
   
   public Interaction getInteraction () { return interaction; }
   
   public ConsoleWindow (final Interaction interaction, int width, int height, int fontSize) {
      this(interaction, width, height, fontSize, false, null);
   }

   public ConsoleWindow (final Interaction interaction, int width, int height, int fontSize,
                         boolean append, File log) {
      super(new Console(null, interaction, log), width, height, fontSize, append);
      this.interaction = interaction;
      setTitle("Disco");
      appendOutput(System.lineSeparator());
      interaction.setDaemon(true);
      addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosed (WindowEvent e) { interaction.exit(); }
      });
      interaction.start(true);
   }
   
   @Override
   public void setTitle (String title) {
      super.setTitle(title);
      if ( interaction != null ) interaction.setName(title+" ConsoleWindow");
   }
}
