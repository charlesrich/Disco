/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco;

import edu.wpi.cetask.*;
import edu.wpi.cetask.TaskClass.Grounding;
import edu.wpi.cetask.TaskClass.Input;
import edu.wpi.cetask.TaskClass.Output;
import edu.wpi.disco.Agenda.Plugin;
import edu.wpi.disco.lang.*;
import java.io.*;
import java.util.*;
   
/**
 * Interactive console for developing and debugging task models.
 * <p>
 * Do <em>not</em> use this class for integrating Disco as a component into a
 * larger system (see {@link ComponentExample}).
 * <p>
 * Note: Disco is single threaded. However, to allow for running a debug console
 * alongside a real-time Disco thread, processing of a console commands
 * synchronized using the interaction object. and also Actor.respond().
 * See {@link Interaction} for thread-safe methods.
 */
public class Console extends Shell {     

   private Interaction interaction;
   
   public Interaction getInteraction () { return interaction; }
   
   public void setInteraction (Interaction interaction) {
      this.interaction = interaction;
      interaction.setConsole(this);
   }
   
   public Console (String from, Interaction interaction) { 
      this(from, interaction, null);
   }

   public Console (String from, Interaction interaction, File log) { 
      super(interaction.getDisco(), from, log); 
      setInteraction(interaction);
   }
   
   @Override
   protected void printVersion () {
      super.printVersion();
      out.print(" / Disco "+Disco.VERSION);
   }

   @Override
   public Disco getEngine () { return interaction.getDisco(); }

   /**
    * Thread-safe method for notifying console that task has occurred.
    */
   public void done (Task occurrence) { 
      synchronized (interaction) { 
         // print out all occurrences in shell
         if ( TaskEngine.DEBUG || TaskEngine.PRINT_TASK ) {
            out.print("    ");
            interaction.getDisco().print(occurrence, out, 0);
            out.println();
         } else println(getEngine().toHistoryString(occurrence));
         if ( occurrence instanceof Utterance ) 
            printTranslateKeys((Utterance) occurrence);
      }
   }
   
   private void printTranslateKeys (Utterance utterance) {
      if ( TaskEngine.VERBOSE && translateStream != null && 
            !(utterance instanceof Say) ) { 
         String formatted = utterance.formatTask(),
               translated = getEngine().translate(formatted, utterance);
         // check if utterance was not actually translated (i.e., new key for file)
         if ( formatted == translated )
            printTranslateKey(formatted);
      }
   }
   
   // automatic user turn mode (see 'next' command)
   private boolean respond = true; 
   
   /**
    * @return true if automatic user turn mode is enabled.
    * 
    * @see #next(String)
    */
   public boolean isRespond () { return respond; }
   
   public void setRespond (boolean respond) { this.respond = true; }
   
   private boolean endTurn;  

   public void respond (Interaction interaction) {
      try { 
         while (true) {
            endTurn = false;
            processLine();
            if ( endTurn ) break;
         }
      } catch (Shell.Quit e) { 
         interaction.exit(); 
         cleanup();
      }
   }
   
   @Override
   public void system () {
      if ( respond || "next".equals(command) ) endTurn = true;
   }

   @Override
   public void status (String ignore) {
      if ( !getEngine().isEmpty() ) out.println();
      if ( getEngine().print(out) ) out.println();
      if ( TaskEngine.DEBUG ) {
         out.println(getEngine().getStack());
         out.println();
      }
   }

   @Override
   protected void help () {
      // similar commands to Guide
      out.println("    (Note: $disco bound to current instance of Disco)");
      out.println("    task <id> [<namespace>] [ / <value> ]*");
      out.println("                        - propose a task");
      out.println("                          (namespace optional if unambiguous id)");
      out.println("                          (slot values optional)");
      out.println("    say [<id> [<namespace>]] [ / <value> ]*");
      out.println("                        - execute an utterance");
      out.println("                          (defaults to menu of choices)");
      out.println("    done [<id> [<namespace>]] [ / <value> ]*");
      out.println("                        - I have performed this task");
      out.println("                          (for primitive tasks only)");
      out.println("                          (defaults to current focus)");
      out.println("                          (all slot values required)");
      out.println("    execute [<id> [<namespace>]] [ / <value> ]*");
      out.println("                        - like 'done', except runs script if any");
      out.println("    next [<boolean>]    - end user console turn");
      out.println("                          (boolean turns automatic turn mode on/off)");
      super.help();
      out.println("    trace [<boolean>]   - turn tracing output on/off (default true)");
      out.println("    reset               - replace with new instance of Disco");
      out.println("    history             - print complete dialog history");
      out.println("    print <id> [<namespace>]");
      out.println("                        - print task or decomposition class XML definition");

   }

   // new commands

   /**
    * Print out complete history in compact human-readable form.
    */
   public void history (String ignore) {
      if ( !getEngine().getStack().get(0).getChildren().isEmpty()) out.println();
      if ( getEngine().history(out) ) out.println();
   }
     
   /**
    * Print out human-readable XML definition of given task or decomposition class
    * and associated properties, if any.
    */
   public void print (String args) {
      StringTokenizer name = new StringTokenizer(args);
      String namespace = null;
      if ( name.hasMoreTokens() ) {
         String id = name.nextToken();
         if ( name.hasMoreTokens() ) namespace = name.nextToken();
         if ( name.hasMoreTokens() ) 
            warning("Ignoring '"+name.nextToken()+"' (and following)");
         TaskClass task = null;
         try { task = namespace == null ? getEngine().getTaskClass(id) :
                         getEngine().getModel(namespace).getTaskClass(id);
         } catch (IllegalArgumentException e) {} // is decomposition class
         if ( task != null ) { 
            out.println(); printNamespace(task, namespace);
            out.print("  "); task.print(out); out.println();
            printProperties(task);
         } else {
            DecompositionClass decomp = namespace == null ? getEngine().getDecompositionClass(id) :
                           getEngine().getModel(namespace).getDecompositionClass(id);
            if ( decomp != null ) { 
               out.println(); printNamespace(decomp, namespace);
               out.print("  "); decomp.print(out); out.println();
               printProperties(decomp);
            } else err.println("Unknown task or decomposition class.");
         }
      } else err.println("No task or decomposition class provided.");

   }
   
   private void printNamespace (TaskModel.Member member, String namespace) {
      if ( namespace == null && getEngine().getModels().size() > 1 ) {
         out.print("  ");out.println(member.getNamespace());
      }
      
   }
   private void printProperties (TaskModel.Member member) {
      Set<String> properties = member.getProperties();
      for (String property : properties) {
         out.print("  "); out.println(property+" = "+member.getModel().getProperty(property));
      }
      boolean empty = properties.isEmpty();
      if ( member instanceof TaskClass ) {
         TaskClass task = (TaskClass) member;
         for (Input input : task.getDeclaredInputs())
            empty &= printSlotProperties(task, input.getName());
         for (Output output : task.getDeclaredOutputs())
            empty &= printSlotProperties(task, output.getName());
      }
      if ( !empty ) out.println();
   }
   
   private boolean printSlotProperties (TaskClass task, String slot) {
      String key = "."+slot+"@optional";
      Boolean value = task.getProperty(key, (Boolean) null);
      if ( value != null ) {
         out.print("  "); out.println(task.getPropertyId()+key+" = "+value);
      }
      // TODO add other slot properties here
      return value == null;
   }
   
   /**
    * Control trace output.
    * 
    * @param state - "true" or "false" (empty means true)
    */
   public void trace (String state) {
      Disco.TRACE = state.length() == 0 || Utils.parseBoolean(state);
   }
   
   { status.add("history"); status.add("trace"); status.add("reset"); status.add("print"); }

   /* *
    * Equivalent to executing Propose.Should of specified task.
    * 
    * @see Propose.Should
    */
   public void task (String args) {
      Task should = processTaskIf(args, null, false);
      if ( should != null ) 
         interaction.done(true, 
               Propose.Should.newInstance(getEngine(), true, should), null); 
   }
   
   /**
    * Set this true to cause TTSay actions to be created in history. E.g.,
    * 
    * <tt> > eval Packages.edu.wpi.disco.Console.TTSay = true </tt> 
    */
   public static boolean TTSay;
   
   /**
    * With arguments, equivalent to 'execute' command; otherwise presents
    * menu of things to say and prompts for choice.
    */
   public void say (String args) throws Quit {
      if ( args.length() > 0 ) {
         Task occurrence = done(args);
         if ( !(occurrence instanceof Utterance) )
            warning("Was not utterance!");
      } else {
         List<Plugin.Item> items = interaction.getExternal().generate(interaction);
         if ( items.isEmpty() ) { command = null; return; }
         String[] formatted = new String[items.size()];
         int i = 0;
         for (Plugin.Item item : items) 
            formatted[i] = printTTSay(++i, item);
         try {
            while (true) {
               if ( !onlyPrompts) out.print(sayPrompt);
               String line = input.readLine();
               if ( onlyPrompts ) {
                  if ( line != null && !line.startsWith(sayPrompt) ) continue;
                  out.print(sayPrompt);
               }
               if ( line == null ) { command = null; break; }
               if ( line.startsWith(sayPrompt) ) 
                  line = line.substring(sayPrompt.length());
               (source == null || out == getLogStream() ? getLogStream() : out).println(line);
               if ( line.length() == 0 ) { 
                  command = null; 
                  if (TTSay) 
                     interaction.done(false, new TTSay(getEngine(), items, null), null);
                  break; 
               }
               if ( "quit".equals(line) ) throw new Quit();
               try {
                  int choice = Integer.parseInt(line.trim());
                  interaction.choose(items, choice, formatted[choice-1]);
                  if (TTSay) 
                     interaction.done(false, new TTSay(getEngine(), items, choice-1), null);
                  break;
               } catch (NumberFormatException e) { println("Not a number!"); }
                 catch (IndexOutOfBoundsException e) { println("Number not in menu!"); }
            }
         } catch (IOException e) { err.println(e); } 
      }
   }

   protected String printTTSay (int i, Plugin.Item item) {
      StringBuilder buffer = new StringBuilder();
      buffer.append('[').append(i);
      if ( Disco.TRACE ) buffer.append(':').append(item.getPlugin());
      buffer.append("] ");
      // note item.task is not an occurrence
      printTranslateKeys((Utterance) item.task);
      String formatted = TaskEngine.DEBUG ? item.task.toString() :
         interaction.format(item, true, true);
      buffer.append(formatted);
      out.println(buffer);
      return formatted;
   }

   private void printTranslateKey (String utterance) {
      translateStream.print(getEngine().getTranslateKey(utterance));
      translateStream.println(" = ");
   }

   /**
    * Report user execution of primitive task or achievement of non-primitive
    * task. Task class and unspecified args default to current focus.
    */
   public Task done (String args) {
      Plan focus = getEngine().getFocus(true);
      Task task = processTaskIf(args, focus, true);
      if ( task != null ) {
         if ( task.isPrimitive() ) done(task, focus); 
         else done(new Propose.Succeeded(getEngine(), true, task), 
                  focus);
      }
      return task;
   }

   private Task done (Task occurrence, Plan focus) {
      if ( occurrence != null ) {
         if ( focus != null && focus.getGoal().matches(occurrence) )
            occurrence.copySlotValues(focus.getGoal());
         if ( occurrence.isDefinedInputs() ) {
            boolean external = !Utils.isFalse(occurrence.getExternal());
            if ( !external ) command = null; // keep user turn
            interaction.done(external, occurrence, null); 
         } else warning("All input values must be defined--ignored.");
      }
      return occurrence;
   }

   /**
    * End user console turn.  If there is a nested user, then it gets to
    * respond next; otherwise it is agent's turn to respond.
    * In automatic turn mode this command is not needed, since
    * every turn is a single task; otherwise this is the command that
    * signals the end of the user console turn.
    * 
    * @param respond turn automatic turn mode on/off (default on)
    */
   public void next (String respond) {
      if ( respond.length() > 0 ) {
         this.respond = Utils.parseBoolean(respond);
         if ( !this.respond ) command = null; // prevent response now
      }
   }

   /**
    * Like 'done', but first executes script associated with primitive task, if
    * any. Convenient for running simulations.
    * 
    * @see #done(String)
    */
   public void execute (String args) {
      Plan focus = getEngine().getFocus();
      Task task = processTaskIf(args, focus, true);
      if ( !task.isPrimitive() ) {
         err.println("Execute not allowed for non-primitive tasks.");
         return;
      }
      if ( !(task instanceof Utterance) ) { // see Interaction.done
         Grounding script = task.getGrounding();
         task.setExternal(true); // must be set before eval
         if ( script != null ) script.eval(task);
      }
      done(task, focus); 
   }

   private File translate;
   private PrintStream translateStream;
   protected String sayPrompt;
   
   @Override 
   public void init (TaskEngine engine) { 
      super.init(engine);
      sayPrompt = engine.getProperty("say@prompt");
      translate = new File(Utils.replaceEndsWith(
            log.getPath(), ".test", ".translate.properties"));
   }

   @Override
   public void clear (String ignore) {
      interaction.clear(); // calls clear on disco (better for game) 
   }
   
   public void reset (String ignore) {
      interaction.reset();
   }
   
   @Override      
   protected void cleanup () { 
      super.cleanup();
      if ( translateStream != null ) translateStream.close();
   }

   @Override
   public void verbose (String state) {
      super.verbose(state);
      if ( TaskEngine.VERBOSE ) {
         if ( translateStream == null ) 
            try {
               translateStream = new PrintStream(new BufferedOutputStream(
                     new FileOutputStream(translate)), true);
               out.println("    # Writing new keys to "+translate);
            } catch (FileNotFoundException e) {
               err.println("Cannot open "+translate);
            }
      }
   }
 }
