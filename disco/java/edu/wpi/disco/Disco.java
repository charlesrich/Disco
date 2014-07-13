/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco;

import edu.wpi.cetask.*;
import edu.wpi.disco.Agenda.Plugin;
import edu.wpi.disco.Recognition.Explanation;
import edu.wpi.disco.lang.*;

import org.iso_relax.verifier.Schema;
import org.w3c.dom.Document;
import org.xml.sax.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;

/**
 * Collaborative Discourse Manager (see papers in docs).
 *
 * NB: This implementation does <em>not</em> support multi-threaded access.
 *     See {@link Interaction} for multi-thread support.
 */
public class Disco extends TaskEngine {
   
   /**
    * Main class for running stand-alone Disco with console.
    * 
    * @param args first string (if any) is url or filename from which to read 
    *             console commands
    */
   public static void main (String[] args) {
      new Interaction(
            new Agent("agent"), 
            new User("user"),
            args.length > 0 && args[0].length() > 0 ? args[0] : null)
         .start(true); // prompt user first
   }
   
   public static String VERSION = "1.9";
   
   /**
    * To enabled tracing of Disco implementation.  Note this variable can be conveniently
    * set using eval command in console or in init script of a task model.
    */
   public static boolean TRACE;
   
   private final Interaction interaction;
   public Interaction getInteraction () { return interaction; }
   
   @Override
   public boolean isRecognition () { return true; }
   
   public Disco (Interaction interaction) {
      // for Unity version
      load("edu/wpi/disco/Disco.xml", Disco.class.getResourceAsStream("Disco.xml"));
      if ( interaction == null ) 
         throw new IllegalArgumentException("Disco must have an interaction");
      this.interaction = interaction;
      synchronizer = interaction;
      // for use in shell debugging
      setGlobal("$disco", this);
   }   
   
   private Disco () { interaction = null; } // for Validate
   
   // schemas and validation

   static private final String xmlns_ext = "http://www.cs.wpi.edu/~rich/cetask/cea-2018-ext";
   static private final String rng_ext = "cea-2018-ext.rng";
   static private final Schema schema_ext;
   
   static {
      try { schema_ext = verifierFactory.compileSchema(   
            Disco.class.getResourceAsStream(rng_ext), rng_ext); 
      } catch (RuntimeException e) { throw e; }
        catch (Exception e) { throw new RuntimeException(e); }
   }
   
   static private final XPath xpath_ext = xpathFactory.newXPath();
   static { xpath_ext.setNamespaceContext(new Context(xmlns_ext)); }

   
   @Override
   protected TaskModel load (String from, InputStream input) {
      try { 
         return load(from, builder.parse(input, from), 
                     loadProperties(from, ".properties"),
                     loadProperties(from, ".translate.properties"));
      } // error handler above has already printed info
      catch (SAXParseException e) { return null; }
      catch (SAXException e) { return null; }
      catch (IOException e) { throw new RuntimeException(e); }
   }
    
   /**
    * Load, parse, validate and return task model and associated properties and
    * translate files from strings.  Used for DiscoUnity.
    * 
    * @param from name of model (for error messages)
    * @param document xml for model 
    * @param properties associated properties
    * @param translate associated translations
    * 
    * @see #load(String)
    */
   public TaskModel load (String from, Document document, 
                          Properties properties, Properties translate) {
      TaskModel model = load(from, document, properties);
      if ( translate != null ) this.translate.putAll(translate);
      return model;
   }
     
   /**
    * Load, parse, validate and return task model and associated properties and
    * translate files from strings.  Used for DiscoUnity.
    * 
    * @param from name of model (for error messages)
    * @param model xml for model 
    * @param properties associated properties 
    * @param translate associated translations
    * 
    * @see #load(String)
    */
   public TaskModel load (String from, String model, String properties, String translate) {
      TaskModel loaded = load(from, model, properties);
      if ( translate != null )
         this.translate.putAll(
               Utils.loadProperties(new ByteArrayInputStream(translate.getBytes())));
      return loaded;
   }
   
   @Override
   protected boolean verify (String from, Document document, String xmlns) throws SAXException {
      if ( xmlns.equals(TaskEngine.xmlns) ) return newVerifier(schema).verify(document);
      if ( xmlns.equals(xmlns_ext) ) return newVerifier(schema_ext).verify(document);
      throw new RuntimeException(from+" xmlns is not "+TaskEngine.xmlns+" or "+xmlns_ext);
   }
   
   static private final String xmlns_d4g = "http://www.cs.wpi.edu/~rich/d4g";
   static private final String rng_d4g = "d4g/d4g.rng"; // path, not a resource
   static private Schema schema_d4g; // only initialized for Validate

   @Override
   protected boolean verify (String file, String xmlns) throws SAXException, IOException {
      if ( xmlns.equals(TaskEngine.xmlns) ) return newVerifier(schema).verify(file);
      if ( xmlns.equals(xmlns_ext) ) return newVerifier(schema_ext).verify(file);
      if ( schema_d4g != null && xmlns.equals(xmlns_d4g) ) 
         return newVerifier(schema_d4g).verify(file);
      throw new RuntimeException(file+" xmlns is not "+TaskEngine.xmlns+" or "+xmlns_ext
            +" or "+xmlns_d4g);
   }
   
   // for bin/validate command
   public static class Validate {
      
      public static void main (String[] args) {
         Disco disco = new Disco();
         try { schema_d4g = verifierFactory.compileSchema(rng_d4g); }
         catch (Exception e) { Utils.rethrow(e); }
         try { System.exit(disco.verify(args[0], args[1]) ? 0 : 1); }
         catch (SAXException e) { System.exit(1); } // already printed by handler
         catch (IOException e) { 
            System.err.println(e);
            System.exit(1);
         }
      }
   }

   @Override
   protected TaskModel newTaskModel (Document document, XPath xpath, String xmlns) {
      return super.newTaskModel(document, xmlns.equals(xmlns_ext) ? xpath_ext : xpath, xmlns);
   }
 
   @Override
   public TaskModel load (String from, Document document, Properties properties) {
      TaskModel model = super.load(from, document, properties);
      setNames();
      return model;
   }
   
   private void setNames () {
      String name = properties.getProperty("user@name");
      if ( name != null ) getInteraction().getExternal().setName(name);
      name = properties.getProperty("agent@name");
      if ( name != null ) getInteraction().getSystem().setName(name);
   }
   
   @Override
   protected void loadDefaultProperties () throws IOException {
      super.loadDefaultProperties();
      properties.load(Disco.class.getResource("default.properties").openStream());  
   }

   @Override
   public TaskClass resolveTaskClass (QName qname) {
      String ns = qname.getNamespaceURI(),
            id = qname.getLocalPart();
      // support task="disco:Say.Agent"
      if ( "urn:disco.wpi.edu:Disco".equals(ns) && !id.startsWith("edu.wpi.disco.lang.") )
         id = "edu.wpi.disco.lang."+id.replace('.', '$'); 
      return getModel(ns).getTaskClass(id);
   }
   
   private Stack<Segment> stack;
   public Stack<Segment> getStack () { return stack; }
   
   /**
    * Test whether discourse stack is empty
    */
   public boolean isEmpty () { return stack.size() == 1; }
   
   /**
    * Return the current segment (top of focus stack).
    */
   public Segment getSegment () { return stack.get(stack.size()-1); } 
   
   /**
    * Return current discourse segment purpose.
    */
   public Task getPurpose () { return getSegment().getPurpose(); }
     
   /**
    * Override simple task engine version of focus with discourse model using stack.
    * 
    * @return plan for top segment in stack or null if stack is empty.
    */
   @Override
   public Plan getFocus () { return getSegment().getPlan(); }
   
   /**
    * <tt>getFocus(false)</tt> is equivalent to {@link #getFocus()}.
    * 
    * @param ignoreAccept if true then substitute non-null parent of Accept
    **/
   public Plan getFocus (boolean ignoreAccept) {
      Plan focus = getFocus();
      return ( ignoreAccept && focus != null && focus.getGoal() instanceof Accept 
               && !isTop(focus) ) ? focus.getParent() : focus;
   }
   
   /**
    * Return the plan that would be focus if exhausted plans popped off stack, or null.
    * 
    * @param ignoreAccept if true then substitute non-null parent of Accept
    */
   public Plan getFocusExhausted (boolean ignoreAccept) { 
      int index = stack.size()-1;
      if ( ignoreAccept && index > 0 && stack.get(index).getPurpose() instanceof Accept )
         index--;
      while ( index > 0 && stack.get(index).getPlan().isExhausted() )
         index--;
      return stack.get(index).getPlan();
   }
   
   /**
    * Return the the closest ancestor of the current focus (or
    * the focus) that has a goal of given task class, or null if
    * none.
    */
   public Plan getPlan (TaskClass task) {
      Plan focus = getFocus();
      return focus == null ? null :
         focus.getType() == task ? focus :
            focus.getAncestor(task);
   }
   
   @Override
   @Deprecated
   public boolean setFocus (Plan focus) {
      throw new RuntimeException("Use focus stack in Disco!");
   }
   
   @Override
   @Deprecated
   public Plan newFocus (Plan plan) {
      throw new RuntimeException("Use focus stack in Disco!");
   }
   
   public void push (Plan plan) {
      if ( plan == null ) throw new IllegalArgumentException("Cannot push null plan");
      push(plan, plan.isStarted()); 
   }
   
   private void push (Plan plan, boolean continuation) {
      // never push purpose identical to current purpose
      if ( getFocus() != null && plan.getGoal() == getFocus().getGoal() ) return;
      while ( !isEmpty() && plan.getParent() != getFocus(true) // would be interruption
                         && getFocus().isPoppable() )
         pop();    
      if ( plan.getParent() == null ) addTop(plan); // toplevel plan
      if ( getFocus() != null && getFocus().getGoal() instanceof Accept ) {
         // remove implicit Accept segment, but do *not* accept proposal
         getSegment().remove();
         Segment segment = stack.pop();
         if ( TRACE ) getOut().println("Push: "+segment);
      }
      Segment segment = new Segment(plan, getSegment(), continuation); 
      stack.push(segment);
      if ( TRACE ) getOut().println("Push: "+segment);
      if ( TaskEngine.DEBUG && getSegment().isInterruption() )
         println("Interruption: "+plan);
   }

   @Override
   public void pop () {
      if ( isEmpty() ) throw new IllegalStateException("Cannot pop stack bottom");
      Segment segment = stack.pop();
      if ( TRACE ) getOut().println("Pop: "+segment);
      Plan plan = segment.getPlan();
      if ( !(plan.isDone() || plan.isFailed()) ) {
         // see reconcileStack for implicit acceptance
         if ( plan.getGoal() instanceof Accept ) segment.remove();
         else segment.stop();
      }
   }
   
   /**
    * Pop zero or more segments off the stack until the first segment
    * is "exposed" (becomes focus) whose plan is either the given plan
    * or a descendant.
    */
   public void popTo (Plan plan) { 
      for (int i = stack.size(); i-- > 1;) {
         Plan focus = getFocus();
         if ( focus == plan || focus.isAncestor(plan) ) break;
         Segment segment = stack.pop();
         if ( TRACE ) getOut().println("Pop: "+segment);
      }
   }
   
   @Override
   public void clear () {
      super.clear();
      if ( utteranceToString != null ) utteranceToString.clear();
      if ( utteranceFormat != null) utteranceFormat.clear();
      stack = new Stack<Segment>();
      stack.push(new Segment()); 
   }
   
   @Override
   public void removeTop (Plan plan) {
      // remove entire subtree from stack 
      for (int i = stack.size(); i-- > 1;) {
         if ( getTop(stack.get(i).getPlan()) == plan ) 
            stack.remove(i);
      }
      // and history
      Iterator<Object> children = stack.get(0).children();
      while ( children.hasNext() ) {
         Object top = children.next();
         if ( top instanceof Segment && ((Segment) top).getPlan() == plan )
            children.remove();
      }
      super.removeTop(plan);
   }
   
   @Override
   public Plan done (Task occurrence) { 
      return interaction.doneSilent(true, occurrence, null);
   }
   
   /* *
    * Extend simple plan recognition in task engine with discourse interpretation
    * algorithm (but still not including decomposition choice interpolation).
    */
   @Override
   public Plan done (Task occurrence, Plan contributes, boolean continuation) {
      Segment top = getSegment();
      if ( contributes == null && top.isInterruption() && top.getPlan().isExhausted() ) {
         // special case for automatically popping exhausted interruptions
         while ( !isEmpty() && getFocus().isExhausted() ) pop();
         contributes = explainBest(occurrence, true);
      }
      interpret(occurrence, contributes, continuation); // before translation
      if ( occurrence instanceof Utterance )
         putUtterance((Utterance) occurrence, translate((Utterance) occurrence));
      return contributes;
   }
   
   public void putUtterance (Utterance utterance, String formatted) {
      if ( utteranceFormat.get(utterance) == null ) // in case copied or set in menu
         // cache translation and history formatting at occurrence time
         utteranceFormat.put(utterance, 
               toHistoryString(utterance, 
                     formatted == null ? translate(utterance) : formatted,
                        true));
   }
   
   @Override
   public Task copy (Task task) {
      Task thisTask = super.copy(task);
      if ( task instanceof Utterance ) { // do not check occurred!
         // copy translation if any
         String translated = ((Disco) task.engine).utteranceFormat.get(task);
         if ( translated != null ) utteranceFormat.put((Utterance) thisTask, translated); 
      }
      return thisTask;
   }

   /**
    * Thread-safe variant of {@link 
    * edu.wpi.cetask.TaskEngine#explainBest(Task,boolean)} for Disco.
    */
   @Override
   public Plan explainBest (Task occurrence, boolean onlyLive) {
      // TODO  note onlyLive flag ignored for now 
      synchronized (interaction) { // typically used in dialogue loop
         List<Explanation> explanations = recognize(occurrence);
         Explanation explanation = resolveAmbiguity(explanations);
         if ( explanation == null ) return null;
         explanation.attach();
         Plan start = explanation.getStart();
         if ( start != null && start.getParent() == null ) addTop(start);
         return explanation.getFocus();
      }
   }
   
   protected Explanation resolveAmbiguity (List<Explanation> explanations) {
      if ( explanations.isEmpty() ) return null;
      Explanation first = explanations.get(0);
      if ( explanations.size() > 1 ) ambiguous(explanations);
      return first;
   }
   
   @Override
   @Deprecated
   public List<Plan> explain (Task task, boolean onlyLive) {
      throw new RuntimeException("Use Disco.explainBest()!");
   }
   
   private List<Explanation> recognize (Task occurrence) {
      Plan focus = getFocusExhausted(false);
      Plan top = null;
      Recognition recognition = new Recognition(occurrence);
      List<Explanation> explanations = Collections.emptyList();
      List<Plan> tried = Collections.emptyList();
      // first look in current focus
      if ( focus != null ) {
         explanations = recognition.recognize(focus, null);
         if ( !explanations.isEmpty() ) return explanations;
         if ( getFocus(true) != focus ) {
            // try ignoring implicit Accept in focus (avoid spurious ambiguity)
            explanations = recognition.recognize(getFocus(true), null);
            if ( !explanations.isEmpty() ) return explanations;
         }
         top = getTop(focus);
         if ( top != focus ) {
            // next look in toplevel plan of current focus (if different)
            explanations = recognition.recognize(top, focus);
            if ( !explanations.isEmpty() ) return explanations;
            if ( tried.isEmpty() ) tried = new ArrayList<Plan>();
            tried.add(top);
         }
      } 
      // next consider popping toplevel plan(s) on stack
      while (true) {
         if ( focus != null && top != null 
               && top.getGoal().getProperty("@poppable", false) ) {
            if ( top == focus ) pop();
            else { while ( getFocus() != top ) pop(); pop(); }
         } else break;
         focus = getFocus();
         if ( focus == null ) break;
         top = getTop(getFocus());
         explanations = recognition.recognize(top, null);
         if ( !explanations.isEmpty() ) return explanations;
         if ( tried.isEmpty() ) tried = new ArrayList<Plan>();
         tried.add(top);
      }
      // now (only) if stack empty then consider other toplevel plans
      // skipping any already tried above
      if ( focus == null ) {
         explanations = new ArrayList<Explanation>(); 
         Iterator<Plan> tops = getTops().iterator();
         while (tops.hasNext()) {
            top = tops.next();
            if ( !tried.contains(top) )
               explanations.addAll(recognition.recognize(top, null));
         }
         if ( !explanations.isEmpty() ) return explanations;
         // as last resort, consider all toplevel task types in engine
         TaskClass type = occurrence.getType();
         for (TaskClass task : getTopClasses()) 
            if ( type.isPathFrom(task) ) 
               explanations.addAll(
                     recognition.recognize(new Plan(task.newInstance()), null));
      }
      return explanations;
   }
  
   @Override
   protected boolean interpret (Task occurrence, Plan contributes, boolean continuation) {
      boolean explained = super.interpret(occurrence, contributes, continuation);
      if ( !(occurrence instanceof Utterance) ) {
         // relying here on fact that Utterance is the only subclass of Task that 
         // overrides interpret() and does its own stack management
         if ( contributes != null  ) 
            reconcileStack(occurrence, contributes, continuation);
         getSegment().add(occurrence); // add to segment regardless
      }
      return explained;
   }
   
   @Override
   protected void unexplained (Task occurrence) {
      // TODO make handling of Ok more general
      if ( occurrence instanceof Ok ) occurrence.setUnexplained(true);
      else super.unexplained(occurrence);
   }
   
   public void reconcileStack (Task occurrence, Plan contributes, boolean continuation) {
      Plan target = getTop(contributes);
      if ( !isEmpty() ) {
         // (virtually) pop interruption and poppable segments which do not 
         // have same toplevel plan as contributes
         int pop = 0;
         int max = stack.size()-1;
         Segment segment = getSegment();
         Plan focus = segment.getPlan();
         Plan top = getTop(focus);
         while ( top != target && ( segment.isInterruption() || focus.isPoppable() ) ) {
            pop++;
            if ( pop >= max) break;
            segment = stack.get(stack.size()-(pop+1));
            focus = segment.getPlan();
            top = getTop(focus);
         }
         if ( top == target ) { 
            while (pop-- > 0) pop(); // do the popping for real
         } else {
            // start new toplevel goal or interruption 
            push(target, continuation);
         }
      } else push(target, continuation);
      reconcileStack(occurrence, getFocus(), contributes, continuation);
    }
   
   private void reconcileStack (Task occurrence, Plan focus, Plan contributes, 
         boolean continuation) {
      if ( focus != contributes ) {
         Stack<Plan> path = focus.pathToDescendant(contributes);
         if ( path == null ) {
            if ( focus.getGoal() instanceof Accept ) {
               // implicitly accept proposal iff occurrence consistent with proposal
               Propose proposal = ((Accept) focus.getGoal()).getProposal();
               if ( proposal instanceof Propose.Should ) {
                  Plan parent = focus.getParent().getParent();
                  if ( parent.pathToDescendant(contributes) != null ) {
                     proposal.accept(parent, true);
                     clearLiveAchieved();
                  }
               } // TODO extend for other types of proposals
            }
            pop();
            // recursion must end since focus and contributes have same top
            reconcileStack(occurrence, getFocus(), contributes, continuation);
         } else {
            // push to contributes
            // if contributes is continuation, then so are all parents
            while ( !path.isEmpty() ) push(path.pop(), continuation);
            // suppress singleton segments
            if ( contributes.getType() != occurrence.getType()
                  || !contributes.getChildren().isEmpty() || !contributes.isDone() )
               push(contributes, continuation);
         }
      }
   }

   /**
    * Pop exhausted segments off the stack.
    */
   void popExhausted () {
      while ( !isEmpty() && getFocus().isExhausted() ) pop();
   }

   
   // temporary non-persistent implementation of user model
   private final Map<String,Object> userModel = new HashMap<String,Object>();
   
   /**
    * Retrieve object from user model associated with given key.
    * Return null if no value.
    */
   public Object getUserModel (String key) { return getUserModel(key, null); }
   
   /**
    * Retrieve object from user model associated with given key.
    * @param defaultValue returned if no value with key
    */
   public Object getUserModel (String key, Object defaultValue) {
      Object value = userModel.get(key);
      return value == null ? defaultValue : value;
   }
   /**
    * Store given object under given key in user model.
    */
   public void putUserModel (String key, Object value) { userModel.put(key, value); }

   // following four methods are useful in task model init scripts
   // note not using varargs because not supported by version of Rhino in JDK 1.6
   
   /**
    * Remove plugin(s) of given class, if any, from plugins used for generation 
    * of user of associated interaction.
    */
   public void removeUserPlugin (Class <? extends Plugin> plugin) {
      getInteraction().getExternal().getAgenda().remove(plugin);
   }
   
   /**
    * Remove plugin(s) of given class, if any, from plugins used for generation 
    * of agent of associated interaction.
    */
   public void removeAgentPlugin (Class <? extends Plugin> plugin) {
      getInteraction().getSystem().getAgenda().remove(plugin);
   }
 
   /**
    * Add plugin of given class to plugins used for generation of agent
    * of associated interaction.
    */
   public void addAgentPlugin (Class <? extends Plugin> plugin, int priority, 
         Object[] initArgs) {
      addPlugin(getInteraction().getSystem().getAgenda(), plugin, priority, initArgs);
   }         
   
   /**
    * Add plugin of given class to plugins used for generation of user
    * of associated interaction.
    */
   public void addUserPlugin (Class <? extends Plugin> plugin, int priority,
         Object[] initArgs) {
      addPlugin(getInteraction().getExternal().getAgenda(), plugin, priority, initArgs);
   }     
   
   private void addPlugin (Agenda agenda, Class <? extends Plugin> plugin, int priority, 
         Object[] initArgs) {
      int length = initArgs == null ? 0 : initArgs.length;
      Class<?>[] argTypes = new Class<?>[length+2];
      argTypes[0] = Agenda.class; argTypes[1] = Integer.TYPE;
      Object[] args;
      if ( initArgs != null ) {
         int i = 2;
         for (Object arg : initArgs) {
            Class<?> type = arg.getClass();
            argTypes[i++] = type == Boolean.class ? Boolean.TYPE : type; 
         }
         args = new Object[length+2];
         args[0] = agenda; args[1] = priority;
         System.arraycopy(initArgs, 0, args, 2, length);
      } else args = initArgs; 
      try { plugin.getDeclaredConstructor(argTypes).newInstance(args); }
      catch (Exception e) { Utils.rethrow(e); }
   }
   
   // *******************************************************************
   //     All code below here is printing-related stuff 
   // *******************************************************************
   
   public void println (Object object) { getOut().println(object); }
   
   @Override
   public PrintStream getOut () { // for debugging
      Console console = interaction == null ? null : interaction.getConsole();
      return console == null ? System.out : console.getOut();
   }
   
   @Override
   public PrintStream getErr () { // for debugging
      Console console = interaction == null ? null : interaction.getConsole();
      return console == null ? System.err : console.getErr();
   }
   
   /**
    * Print compact human-readable summary of current discourse state.
    * 
    * @return true iff it printed something
    */
   public boolean print (PrintStream stream) { 
      return print(stream, 0, stack.get(0), getSegment(), false); 
   } 
  
   /**
    * Print compact human-readable summary of complete discourse history.
    * 
    * @return true iff it printed something
    */
   public boolean history (PrintStream stream) {
      return print(stream, 0, stack.get(0), getSegment(), true); 
   }
   
   // convenience methods for use in CoachedInteraction shell
   public boolean status () { return print(getOut()); }
   public boolean history () { return history(getOut()); }
   
   private boolean print (PrintStream stream, int indent, Segment parent,
                          Segment focus, boolean history) {
      List<Plan> printedTops = new ArrayList<Plan>(); // tops already printed
      if ( !isEmpty() || history )
         print(stream, 0, stack.get(0), focus, history, printedTops); 
      boolean printed = !isEmpty() || (history && !stack.get(0).getChildren().isEmpty());
      // print additional toplevel plans, if any
      boolean first = true;
      for (Plan top : getTops()) {
         if ( !printedTops.contains(top) 
               && (history || 
                    !(top.isDone() || top.isFailed() 
                          || Utils.isFalse(top.getGoal().getShould()))) ) {
            if ( first ) stream.println();
            first = false;
            print(top, stream, 0);
            stream.println();
         }
      }
      return printed || !first;
   }

   private void print (PrintStream stream, int indent, Segment parent, 
                       Segment focus, boolean history, List<Plan> printedTops) {
      List<Plan> printedPlans = new ArrayList<Plan>(); // plans already printed
      // first print children in segment
      for (Object child : parent.getChildren()) {
         if ( child instanceof Task ) {
            if ( !history && parent.isRoot() ) continue;
            Task task = (Task) child;
            print(task, stream, indent);
            if ( !(task instanceof Utterance) ) {
               Boolean success = task.getSuccess();
               if ( Utils.isTrue(success) ) stream.print(" -succeeded");
               else if ( Utils.isFalse(success) ) stream.print(" -failed");
            }
            if ( TaskEngine.VERBOSE && task.isUnexplained() && !parent.isRoot() )
               stream.print(" -unexplained");
            stream.println();
         } else {
            Segment segment = ((Segment) child);
            if ( (segment.isInterruption() || isTop(segment.getPlan()))
                  && !(history || stack.contains(segment)) ) 
               continue;
            Plan plan = segment.getPlan();
            print(plan, stream, indent);
            printedPlans.add(plan);
            printedTops.add(getTop(plan));
            if ( segment.isContinuation() ) stream.print(" -continuation");
            if ( segment.isInterruption() ) stream.print(" -interruption");
            if ( plan.isLive() && segment.isStopped() ) stream.print(" -stopped");
            // note ignoring temporary Accept's to avoid novice confusion
            if ( segment.getPlan() == getFocus(true) && stack.contains(segment) )
               stream.print(' '+Plan.FOCUS_NOTE);
            stream.println();
            // unless history recurse on open segments only 
            if ( history || stack.contains(segment) ) 
               print(stream, indent+1, segment, focus, history, printedTops);          
         }
      }
      if ( stack.contains(parent) ) {
         Plan plan = parent.getPlan();
         // then print additional expected steps from plan
         if ( plan != null ) printExpected(plan, printedPlans, stream, indent);
      }
   }
   
   private void printExpected (Plan plan, List<Plan> printedPlans, 
         PrintStream stream, int indent) {
      for (Plan expect : plan.getExpected()) {
         if ( !printedPlans.contains(expect) ) {
            print(expect, stream, indent);
            stream.println();
            if ( TaskEngine.DEBUG )
               printExpected(expect, printedPlans, stream, indent+1);
         }
      }         
   }
   
   public void print (Task task, PrintStream stream, int indent) {
      for (int i = indent; i-- > 0;) stream.print("   ");
      if ( TaskEngine.DEBUG || TaskEngine.PRINT_TASK ) stream.print(task);
      else stream.print(toHistoryString(task));
   }
   
   private void print (Plan plan, PrintStream stream, int indent) {
      Task goal = plan.getGoal();
      plan.print(stream, indent, false, !(TaskEngine.DEBUG || TaskEngine.PRINT_TASK),
            // pre-format utterances (re translations)
            !(TaskEngine.DEBUG || TaskEngine.PRINT_TASK) && goal instanceof Utterance ?
               toHistoryString(goal, null, true) : null);
   }
   
   // to freeze toString of occurred utterances for use in debug history
   private final Map<Utterance,String> utteranceToString = new IdentityHashMap<Utterance,String>();
   
   public String getToString (Utterance utterance) { 
      return utteranceToString.get(utterance); 
   }
   
   public void putToString (Utterance utterance, String string) {
      utteranceToString.put(utterance,  string);
   }
   
   /**
    * Utility method to create human-readable string for given task for 
    * use in history.
    */
   public String toHistoryString (Task task) {
      return task instanceof Utterance && task.occurred() ?
         task.format() : // already cached
         toHistoryString(task, null, false);
   }

   private String toHistoryString (Task task, String formatted, boolean formatTask) {
      // TODO: add rules to put verb (first word) in present tense (e.g., add 's')
      StringBuilder buffer = new StringBuilder();
      if ( task instanceof Utterance ) {
         if ( formatted != null ) {
            formatted = Utils.capitalize(formatted);
            buffer.append(formatted);
            Utils.endSentence(buffer);
            buffer.insert(0, '\"').append('\"');
            buffer.insert(0, ' ');
            buffer.insert(0, getProperty("says@word"));
         } else buffer.append(((Utterance) task).toHistoryString(formatTask));
      } else {
         buffer.append(task.format());
         Utils.endSentence(buffer);
      }
      buffer.insert(0, ' ');
      buffer.insert(0, getWho(task));
      return Utils.capitalize(buffer.toString());
   }

   
   /**
    * Format given utterance as human-readable string.
    *
    * @param capitalize controls whether to capitalize string
    * @param endSentence controls whether to call {@link Utils#endSentence(StringBuilder)}
    * @param freeze controls whether to cache (freeze) the formatted string for this utterance.  This is done
    *        automatically for occurrences, but should also be used in menus if there are formatting
    *        rules with alternative choices or other properties that change.
    */
   public String formatUtterance (Utterance utterance, boolean capitalize, boolean endSentence, boolean freeze) {
      String formatted = utterance.occurred() ? translate(utterance.formatTask(), utterance) 
               : translate(utterance);
      if ( capitalize || endSentence ) {
         StringBuilder buffer = new StringBuilder(formatted);
         if ( capitalize ) Utils.capitalize(buffer);
         if ( endSentence ) Utils.endSentence(buffer);
         formatted = buffer.toString();
      }
      if ( freeze || utterance.occurred() ) putUtterance(utterance, formatted);
      return formatted;
   }
   
   /**
    * Format given utterance as default human-readable string.  Equivalent to 
    * <code>formatUtterance(utterance, true, true, false)</code>
    * 
    * @see #formatUtterance(Utterance,boolean,boolean,boolean)
    */
   public String formatUtterance (Utterance utterance) {
       return formatUtterance(utterance, true, true, false);
   }
   
   // alternative formatting options for a task
   // ...@format = ... | ... | ... 
   
   @Override
   public String getFormat (Task task) {
      return processFormat(task, 
                           super.getFormat(task), 
                           task.getType().getPropertyId()+"@format");
   }
 
   public static String processFormat (Task task, String format, String where) {
      if ( format == null ) return null;
      // note evaluation happens before alternatives so
      // that don't need to quote |'s in Javascript
      return ((Disco) task.engine).getAlternative(format, // raw format string is key 
          evalFormat(task, format, where), 
          task.occurred());   
   }
   
   private static final Pattern
      altPattern = Pattern.compile("((((\\\\\\|)|[^\\|])*)[^\\\\])\\|"),
      quotedOr = Pattern.compile("\\\\\\|");         
 
   /**
    * Utility function for sequencing through alternatives using | separators (which
    * may be quoted with backslash).  Used for task formatting strings,
    * translation tables and {@link edu.wpi.disco.lang.Say}.
    * 
    * @param key base used to store alternatives counter in user model
    * @param value string with alternatives (may have only one)
    * @param advance whether to advance count afterward
    * @return chosen alternative
    */
   public String getAlternative (String key, String value, boolean advance) {
      if ( value == null || value.indexOf('|') < 0 ) return value; // early exit
      Matcher matcher = altPattern.matcher(value);
      List<String> alts = null;
      int end = -1;
      while ( matcher.find() ) {
         if ( alts == null ) alts = new ArrayList<String>(5);
         alts.add(matcher.group(1));
         end = matcher.end(1);
      }
      if ( alts != null ) {
         if ( ++end < value.length() )
            // add trailing alternative, if any
            alts.add(value.substring(end, value.length()));
         key += "@getAlternative";
         int alt = (Integer) getUserModel(key, 0);
         value = alts.get(alt);
         // advance to next alternative
         if ( advance ) {
            alt = (alt+1) % alts.size();
            putUserModel(key, alt);
         } 
      }
      // fix quoted or's, if any
      quotedOr.matcher(value).replaceAll("\\|");
      return value;
   }
   
   private static final Pattern
      evalPattern = Pattern.compile("((\\A|\\G|[^\\\\])\\{)((((\\\\\\})|[^\\}])*)[^\\\\])\\}"),
      quotedOpen = Pattern.compile("\\\\\\{"),
      quotedClose = Pattern.compile("\\\\\\}");
   
   // TODO Can Say.Expression be eliminated using evalFormat?
   
   /**
    * Utility function for substituting { } Javascript blocks with evaluated results.
    * 
    * @param task task being formatted
    * @param format format string which may contain { } blocks
    * @param where doc string for evaluation error messages
    */
   private static String evalFormat (Task task, String value, String where) {
      if ( value == null || value.indexOf('{') < 0 ) return value; // early exit
      Matcher matcher = evalPattern.matcher(value);
      StringBuilder buffer = new StringBuilder();
      int end = 0;
      while ( matcher.find() ) {
         buffer.append(unquote(value.substring(end, matcher.start(3)-1)));
         buffer.append(task.engine.toString(task.eval(matcher.group(3), where)));
         end = matcher.end(3)+1;
      }
      if ( end < value.length() ) buffer.append(unquote(value.substring(end)));
      return buffer.toString();
   }
   
   private static String unquote (String s) {
      if ( s.indexOf('\\') < 0 ) return s; // early exit
      return quotedClose.matcher(quotedOpen.matcher(s).replaceAll("\\{")).replaceAll("\\}");
   }
   
   // translation-related code
 
   // translation table from translate.properties file
   final protected Properties translate = new Properties();

   // all utterance occurrences already formatted and translated
   private final Map<Utterance,String> utteranceFormat 
      // using IdentityHashMap to avoid expensive Task.hashCode()
      = new IdentityHashMap<Utterance,String>();
    
   @Override
   public String format (Task task) {
      if ( task instanceof Utterance && task.occurred() ) {
         // might be goal utterance
         String translated = utteranceFormat.get(task);
         return translated == null ? task.formatTask() : translated;
      } // else 
      return task.formatTask();
   }
   
   public String translate (Utterance utterance) {
      // TODO: Provide regular expression for matching against
      //       stack, e.g, "*[Goal]..."
      return translate(utterance.format(), utterance);
   }
      
   public String translate (String formatted, Utterance utterance) {  
      String key = formatted.replace(' ','_');
      String translated = translateKey(key, utterance); // anywhere key
      if ( translated == null ) {
         key = prependStack(key, false);
         translated = translateKey(key, utterance); 
      }
      if ( translated == null ) {
         key = prependStack(key, true); // popExhausted
         translated = translateKey(key, utterance);
      }
      return translated == null ? formatted : getAlternative(key, translated, true);
   }
    
   private String translateKey (String key, Utterance utterance) {
      // note evaluation happens before alternatives, so don't
      // have to quote |'s in Javascript
      return evalFormat(utterance, (String) translate.get(key), key);
   }
   
   String getTranslateKey (String utterance) { 
      return prependStack(utterance.replace(' ','_'), false);
   }
   
   private String prependStack (String key, boolean popExhausted) {
      StringBuilder buffer = new StringBuilder();
      buffer.append("[]"); // for toplevel translations
      appendStack(buffer, getSegment(), popExhausted);
      buffer.append('_');
      return buffer.append(key).toString();
   }
   
   private void appendStack (StringBuilder buffer, Segment segment,
                             boolean popExhausted) {
      if ( segment != null && segment.getPurpose() != null ) {
         if ( !segment.isInterruption() ) 
            appendStack(buffer, segment.getParent(), popExhausted); // recursion
         if ( !popExhausted || !segment.getPlan().isExhausted() )
            buffer.append('[').append(segment.getPurpose().getType().getPropertyId()).append(']');
      }
   }
   
   /**
    * Return string identifying external slot of given task
    */
   public String getWho (Task task) {
      Boolean external = task.getExternal();
      return external == null ? 
         (task.getType().isPrimitive() ? 
            getProperty("someone@word") : getProperty("we@word")) : 
            getWho(external);
   }
   
   public String getWho (boolean external) {
      return external ? getExternalName() : getSystemName();   
   }
   
   @Override
   public String getExternalName () {
      Actor user = interaction.getExternal();
      return user == null ? "user" : user.getName(); 
   }
   
   @Override
   public String getSystemName () { 
      Actor agent = interaction.getSystem();
      return agent == null ? "agent" : agent.getName(); 
   }

}
