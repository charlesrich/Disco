/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.cetask;

import java.beans.Statement;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.DateFormat;
import java.util.*;

/**
 * A simple command shell for interacting with the task engine.
 * 
 * @see edu.wpi.cetask.guide.Guide
 * @see edu.wpi.cetask.guide.SpeechGuide
 */
public class Shell {

   public static void main (String[] args) {
      make(args).loop();
   }

   /**
    * Method for use in other main methods.
    */
   public static Shell make (String[] args) { 
      return new Shell(new TaskEngine(), args.length > 0 ? args[0] : null);
   }
   
   private final TaskEngine engine;
   public TaskEngine getEngine () { return engine; }
   
   protected PrintStream out = System.out, err = System.err; 
   
   public PrintStream getOut () { return out; } 
   public PrintStream getErr () { return err; }
  
   /**
    * Redirect normal system output for this shell to given stream.
    * Note this must be called before {@link #init(TaskEngine)}.
    */
   public void setOut (PrintStream stream) {
      out = stream;
      getEngine().getScriptEngine().getContext().setWriter(new PrintWriter(stream, true));
   }
   
   /**
    * Redirect error output for this shell to given stream.
    * Note this must be called before {@link #init(TaskEngine)}.
    */
   public void setErr (PrintStream stream) {
      err = stream;
      getEngine().getScriptEngine().getContext().setErrorWriter(new PrintWriter(stream, true));
   }
   
   final public File log;
   
   protected String prompt;
   
   public String getPrompt () { return prompt; }
   
   private PrintStream logStream;
   
   public PrintStream getLogStream () { return logStream; }

   protected Reader input; // current input
   protected URL source;   // current alternative input source (or null)
   
   private Reader systemIn = new StreamAsReader(System.in);

   /**
    * Read shell input from given reader instead of System.in
    */
   public void setReader (Reader reader) { 
      systemIn = reader;
      if ( source == null ) input = reader;
   }
   
   // for nested 'source' commands
   private final Stack<Reader> readers = new Stack<Reader>();
   private final Stack<URL> sources = new Stack<URL>();
   
   private static Shell shell; // see Shell.command()
  
   /**
    * @param from if non-null, change default name of log file to correspond
    *        to name of given source and immediately start reading from that source
    * 
    * @see #source(String)
    */
   protected Shell (TaskEngine engine, String from) {
      this(engine, from, null);
   }
   
   protected Shell (TaskEngine engine, String from, File log) {
      this.engine = engine;
      if ( from == null ) input = systemIn;
      else source(from);
      // allow overriding of log file name
      this.log = log != null ? log :
         new File(System.getProperty("java.io.tmpdir") + '/'
            + (source == null ? 
               Utils.getSimpleName(getClass(), false) : 
                  new File(source.getFile()).getName()) 
                  + ".test");
      shell = this;
   }
    
   /**
    * Command loop. Uses reflection to invoke commands; see methods with command
    * names.
    */
   public void loop () {
      try { 
         init(getEngine());
         while (true) processLine(); 
      } catch (Quit e) {} 
        finally { cleanup(); }
   }

   protected void processLine () throws Quit {
      try {
         if ( stepping ) {
            if ( source == null ) {
               stepping = false;
               println("# Done single stepping.");
            } else {
               char in = (char) System.in.read(); // wait for keyboard
               System.in.skip(1); // flush enter
               if ( in == 'q' ) {
                  stepping = false;
                  println("# Stopped single stepping.");
               } else if ( in == 'p' ) {
                  stepping = false;
                  try { pushSource(null); } catch (IOException e) {}
                  println("# Pause single stepping. Use 'step' command to resume.");
               }
            }
         }
         out.print(prompt);
         String line;
         while (true) {
            line = input.readLine();
            if ( onlyPrompts && line != null && !line.startsWith(prompt) ) continue;
            if ( line != null || source == null ) break;
            popSource();
         }
         if ( line == null ) return;
         if ( line.startsWith(prompt) )
            line = line.substring(prompt.length());
         getPrintStream().println(line);
         if ( !line.startsWith("#") ) // skip comment line 
            processCommand(line); 
      } catch (Quit e) { throw e; } // rethrow
        catch (Throwable e) { exception(e); }
   }
   
   protected PrintStream getPrintStream () {
      return (source == null && logStream != null) || out == logStream ? 
         logStream : out;
   }
   protected void printVersion () {
      out.print(" - TaskEngine "+TaskEngine.VERSION);
   }
  
   private boolean append;
   
   public void setAppend (PrintStream logStream) { 
      this.logStream = logStream;
      setErrOut(logStream);
      append = true;
   }
   
   protected void init (TaskEngine engine) {
      prompt = engine.getProperty("shell@prompt");
      logStream = null;
      try {
         logStream = new PrintStream(new BufferedOutputStream(
               new FileOutputStream(log, append)), true);
         if ( source == null ) {
            println("# Writing log to " +log+ "...");
            copyOutput(logStream);
         } else setErrOut(logStream);
      } catch (FileNotFoundException e) { 
         getErr().println("Cannot open log file: "+e);
      }
      long now = System.currentTimeMillis();
      out.print("    # "+
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
            .format(new Date(now)));
      printVersion(); out.println();
      engine.setStart(now);
      println(engine.getProperty("shell@welcome"));
      if ( !TaskEngine.isCompilable() )
         println("WARNING: "+engine.getScriptEngine()+
         " conditions are not compiled (will run slower).");
      if ( !TaskEngine.SCRIPTABLE)
         println("WARNING: "+engine.getScriptEngine()+
         " does not use (sun.)org.mozilla.javascript.(internal.)Scriptable and will run slower.");
   }         
   
   private void setErrOut (PrintStream stream) {
      setOut(stream); 
      // errors also go to console
      setErr(new PrintStream(new Utils.CopyOutputStream(err, stream), true));
   }

   protected void cleanup () {
      setOut(System.out); setErr(System.err);
      if ( logStream != null && !append ) logStream.close();
      deallocate();
   }
   
   protected void respond (String key) {
      println(getEngine().getProperty(key));
   }

   public static class Quit extends Throwable {};
   
   public void deallocate () {} // for SpeechGuide
   
   private Runnable onProcessCommand;

   /**
    * @param onProcessCommand hook to run after each user command is executed.  
    *        Useful, for example, for updating associated gui
    */
   public void setOnProcessCommand (Runnable onProcessCommand) { 
      this.onProcessCommand= onProcessCommand;
   }
   
   protected String command;
   
   /**
    * Process given string as if it were typed to shell prompt.
    */
   public void processCommand (String line) throws Throwable {
      line = line.trim();
      StringTokenizer tokenizer = new StringTokenizer(line);
      if ( tokenizer.hasMoreTokens() ) {
         command = tokenizer.nextToken().toLowerCase();
         if ( "quit".equals(command) ) {
            respond("shell@goodbye");
            throw new Quit();
         }
         // synchronized so that console can run for debugging alongside
         synchronized (engine.synchronizer) {
            try {
               // dispatch to command method
               new Statement(this, command, 
                     new Object[] { line.substring(command.length()).trim() }) 
               .execute();
               if ( onProcessCommand != null ) onProcessCommand.run();
            } catch (NoSuchMethodException m) {
               // do not use respond here (for shell)
               println("Invalid command. Type 'help' for commands.");
               command = null;
            } catch (InvocationTargetException t) {
               throw t.getTargetException();
            } 
            if ( !status.contains(command) ) {
               if ( command != null ) {
                  getEngine().decomposeAll();
                  system(); // system turn
                  getEngine().decomposeAll();
               }
            }
         }
      }
   }
   
   /**
    * Process given string as if it were typed to last created shell prompt.
    * (Convenient for debugging in IDE).
    */
   public static void command (String line) throws Throwable { 
      shell.processCommand(line);
   }
   
   private void copyOutput (PrintStream logStream) {
      setOut(new PrintStream(new Utils.CopyOutputStream(out, logStream), true));
      setErr(new PrintStream(new Utils.CopyOutputStream(err, logStream), true));
   }
   
   public void println (Object object) {
      out.print("    "); out.println(object);
   }
   
   public void warning (String text) {
      err.println("WARNING: "+text);
   }
    
   public static boolean THROW;

   public void exception (Throwable e) {
      if ( TaskEngine.DEBUG || THROW ) Utils.rethrow(e);
      else {
         err.println(e); 
         err.print("at "); out.println(e.getStackTrace()[0]);
      }
   }
 
   /**
    * Method for system turn.  Called once each time around command loop.
    * Intended extension point.
    */
   protected void system () {} // do nothing for now

   // allow later additions to list
   protected final List<String> status = new ArrayList<String>(
      Arrays.asList("load", "cd", "status", "eval", "clear",
                    "source", "test", "step", "verbose", "debug", "quit", "help"));
           
   // commands 
   
   public final void help (String ignore) {
      help();
      out.println("    help                - show this message");
      out.println("    (See javadoc for more information)");
   }
   
   protected void help () {
      out.println("    load <uri>          - load xml for task model");
      out.println("    status              - print current engine state");
      out.println("    clear               - delete all current tasks");
      out.println("    eval <javascript>   - evaluate inline JavaScript");
      out.println("    source <filename>   - read command input from file");
      out.println("    test <filename>     - like source but only prompt lines");
      out.println("    step (<filename>)   - single-step command input from file");
      out.println("    cd <path>           - change working directory");
      out.println("    quit                - quit program");
      out.println("    verbose [<boolean>] - turn verbose output on/off (default true)");
      out.println("    debug [<boolean>]   - turn debugging output on/off (default true)");
   }
   
   /**
    * Load, parse and validate specified task model (XML).
    * 
    * @param from string identifying url, system resource or file
    */
   public void load (String from) {
      TaskModel model = getEngine().load(from);
      if ( model != null && TaskEngine.VERBOSE )
         println("# Parsed and validated "+model);
   }
 
   /**
    * Evaluate given JavaScript code and report return value.
    */
   public void eval (String script) {
      TaskEngine engine = getEngine();
      println("# Returned '"+engine.toString(engine.eval(script, "Shell"))+"'");
      getEngine().clearLiveAchieved();
   }
   
   private boolean stepping;

   /**
    * Like 'source', but with single-stepping
    */
   public void step (String from) {
      if ( from.length() == 0 ) {
         if ( source == null && !sources.isEmpty() ) {
            println("# Resume single stepping:  <enter> = next, p<enter> = pause, q<enter> = quit");
            popSource();
            stepping = true;
         } else println("# No source to resume single stepping.");
      } else {
         stepping = true;
         println("# Start single stepping: <enter> = next, p<enter> = pause, q<enter> = quit");
         source(from);
      }
   }
   
   /**
    * Start reading user input from url, system resource or file 
    * identified by given string. 
    */
   public void source (String from) {
      try { pushSource(new File(from).toURI().toURL()); } // do not use Utils.toURL()
      catch (IOException e) {
         exception(e);
         popSource();
      }
   }
   
   protected boolean onlyPrompts;  
   
   /**
    * Like {@link #source(String)}, except skips lines that do not start with
    * prompt
    */
   public void test (String from) {
      onlyPrompts = true;
      try {
         pushSource(new File(from).toURI().toURL()); // do not use Utils.toURL() 
      } catch (IOException e) {
         exception(e);
         popSource();
      }
   }
   
   private void pushSource (URL url) throws IOException {
      if ( input != null ) {
         readers.push(input);
         sources.push(source);
      }
      source = url;
      input = source == null ? systemIn : new StreamAsReader(source.openStream());
   }
   
   private void popSource () {
      if ( onlyPrompts ) onlyPrompts = false;
      if ( sources.isEmpty() ) {
         source = null;
         input = systemIn; // switch back to user input
         if ( logStream != null ) copyOutput(logStream);
         if ( prompt != null ) out.print(prompt);
      } else {
         source = sources.pop();
         input = readers.pop();
      }
   }
   
   /**
    * Change current working directory.   Accepts relative or absolute
    * directory string.  Note for Windows, does not accept the the drive
    * prefix, thus \Users\rich, not C:\Users\rich
    */
   public void cd (String dir) {
      try {
         String separator = System.getProperty("file.separator"); 
         dir = dir.startsWith(separator) ? dir :
            System.getProperty("user.dir")+separator+dir;
         File file = new File(dir);
         if ( file.isDirectory() ) {
            dir = file.getCanonicalPath();
            System.setProperty("user.dir", dir);
            // suppress local path for testing
            if ( source == null ) println(dir);
         } else throw new IOException("Directory does not exist: "+dir);
      } catch (IOException e) { err.println(e); }
   }
   
   /**
    * Print out current task decomposition tree with status information.
    */
   public void status (String ignore) {
      Plan focus = getEngine().getFocus();
      try {
         if ( focus != null) focus.getNotes().add(Plan.FOCUS_NOTE);
         List<Plan> tops = getEngine().getTops();
         if ( !tops.isEmpty() ) {
            out.println();
            for (Plan top : tops) top.print(out);
            out.println();
         } else respond("shell@empty");
      } finally {
         if ( focus != null) focus.getNotes().remove(Plan.FOCUS_NOTE);
      }
   }
   
   /**
    * Reinitialize current task state (but keeps loaded models).
    */
   public void clear (String ignore) {
      getEngine().clear();
   }
   
   /**
    * Control verbose output.
    * 
    * @param state - "true" or "false" (empty means true)
    */
   public void verbose (String state) {
      TaskEngine.VERBOSE = state.length() == 0 || Utils.parseBoolean(state);
   }
   
   /**
    * Control debugging output.  Note initial setting is determined by
    * "engine@debug" property.
    * 
    * @param state - "true" or "false"
    */
   public void debug (String state) {
      TaskEngine.DEBUG = state.length() == 0 || Utils.parseBoolean(state);
   }
   
   // utilities for extensions
   
   /**
    * @see #processTask(String,Plan,boolean)
    */
   protected Task processTaskIf (String args, Plan focus, boolean success) {
      Task occurrence = processTask(args, focus, true);
      if ( occurrence == null ) 
         warning("Missing task argument (and no focus).");
      return occurrence;
   }
   
   /**
    * Return the task instance represented by given shell arguments.
    * 
    * JavaScript to compute all <em>declared</em> input
    * and output slot values is required (in order declared). Success slot value is
    * followed by external are optional (and last). Skipped slots can be specified
    * by '/ /'.<br>
    * <br>
    * Hint: If Javascript contains '/', use 'eval' command to set temporary
    * variable and use variable in 'done'.
    * 
    * @param args [&lt;id&gt [&lt;namespace&gt]] [ / &lt;value&gt ]*
    */
   public Task processTask (String args, Plan focus, boolean optional) {
      TaskClass type = focus == null ? null : focus.getType(); 
      StringTokenizer tokenizer = new StringTokenizer(args, "/");
      if ( tokenizer.hasMoreTokens() && !args.startsWith("/") ) {
         String namespace = null;
         StringTokenizer name = new StringTokenizer(tokenizer.nextToken());
         if ( name.hasMoreTokens() ) {
            String id = name.nextToken();
            if ( name.hasMoreTokens() ) namespace = name.nextToken();
            if ( name.hasMoreTokens() ) 
               warning("Ignoring '"+name.nextToken()+"' (and following)");
            type = namespace == null ? getEngine().getTaskClass(id) :
               getEngine().getModel(namespace).getTaskClass(id);
            if ( type == null ) 
               throw new IllegalArgumentException("Unknown task id "+id);
         }
      }
      if ( type == null ) return null;
      Task task = type.newInstance();
      // process input, output and success slot values, if any
      for (String name : type.getDeclaredInputNames()) 
         if ( !nextArg(tokenizer, task, name) ) return task;
      for (String name : type.getDeclaredOutputNames()) 
         if ( !nextArg(tokenizer, task, name) ) return task;
      if ( optional ) {
         if ( nextArg(tokenizer, task, "success") ) 
            nextArg(tokenizer, task, "external");
      }
      if ( tokenizer.hasMoreTokens() ) 
         warning("Ignoring rest of line starting at: \'"+tokenizer.nextToken()+"\'");
      return task;
   }

   private static boolean nextArg (StringTokenizer tokenizer, 
                                   Task task, String name) {
      if ( tokenizer.hasMoreTokens() ) {
         String next = tokenizer.nextToken().trim();
         if ( next.length() > 0 )
            task.setSlotValueScript(name, next, "Shell");
         return true;
      } else return false;
   }
  
   // for redirection of shell input (e.g., in Unity)
   
   public interface Reader {
      String readLine () throws IOException, Quit;
   }

   private static class StreamAsReader implements Reader {
      
      private final BufferedReader in;

      StreamAsReader (InputStream stream) {
         in = new BufferedReader(new InputStreamReader(stream));  
      }
      
      @Override
      public String readLine () throws IOException {
         return in.readLine();
      }
   }

}

