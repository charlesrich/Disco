/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.cetask;

import com.sun.msv.verifier.jarv.TheFactoryImpl;
import edu.wpi.cetask.ScriptEngineWrapper.Compiled;
import edu.wpi.cetask.TaskClass.Grounding;
import edu.wpi.cetask.TaskModel.Init;
import org.iso_relax.verifier.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.script.*;
import javax.xml.XMLConstants;
import javax.xml.namespace.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;

/**
 * Implementation of an intepreter for ANSI/CEA-2018 Task Model Description
 * standard (see docs/CEA-2018.pdf).
 * <p>
 * NB: This implementation does <em>not</em> support multi-threaded access.
 *     See edu.wpi.disco.Interaction class for thread-safe methods.
 */
public class TaskEngine {
   
   public static String VERSION = "1.10";
   
   public static boolean VERBOSE, DEBUG, PRINT_TASK;
   
   // TODO  Allow single task model to be shared among instances of TaskEngine
   
   // for synchronization in Task.occurred(); overridden by Disco
   protected Object synchronizer = new Object(); 
   
   private String platform, deviceType;
   
   public String getDeviceType (TaskClass task) { return deviceType; }
   public String getPlatform (TaskClass task) { return platform; }

   /**
    * Create a new task engine with given default values for platform and
    * deviceType. Note that how platforms and deviceTypes are associated with
    * task classes is outside the scope of the standard, and is not addressed by
    * this implementation.
    */
   public TaskEngine (String platform, String deviceType) {
      this(); // see below
      this.platform = platform;
      this.deviceType = deviceType;
   }
   
   /**
    * Most recently created task engine.
    */
   public static TaskEngine ENGINE; 
   
   private final ScriptEngineWrapper scriptEngine;
   
   public ScriptEngineWrapper getScriptEngine () { return scriptEngine; }
   
   public TaskEngine () {
      ENGINE = this;
      scriptEngine = ScriptEngineWrapper.getScriptEngine();
      try { loadDefaultProperties(); }
      catch (IOException e) { throw new RuntimeException(e); }
      defaultProperties();
      if ( Task.compiledCloneThis != null ) {
         Task.compiledCloneThis = compile(Task.cloneThis, "compiledCloneThis");
         Task.compiledCloneSlot = compile(Task.cloneSlot, "compiledCloneSlot");
      }
      rootClass = new TaskClass(this, "**ROOT*"); // after scriptEngine initialized	
      try { 
         // load functions used in equals and hashCode into global scope
         scriptEngine.eval(
               Utils.toString(TaskEngine.class.getResourceAsStream("default.js")));
      } catch (Exception e) { getErr().println(e); }
      clear();  // after default.js loaded
   }
   
   /**
    * Return the field of given Javascript object.  See LiveConnect documentation
    * for conversion of return value from Javascript to Java.
    */
   public Object get (Object object, String field) { 
      return scriptEngine.get(object, field);
   }
   
   /**
    * Set the field of given Javascript object. See LiveConnect documentation
    * for conversion of value from Java to Javascript.
    */
   public void put (Object object, String field, Object value) {
      scriptEngine.put(object, field, value);
   }

   public boolean isDefined (Object object, String field) { 
      return scriptEngine.isDefined(object, field); 
   }
   
   protected boolean isUndefined (Object value) { // do not make public 
       return scriptEngine.isUndefined(value);
   }
   
   /**
    * Remove the field of given Javascript object.
    */
   public void remove (Object object, String field) {
      scriptEngine.remove(object, field);
   }
   
   boolean isScriptable (Object value) { return scriptEngine.isScriptable(value); }
 
   // for extensions

   protected void loadDefaultProperties () throws IOException {
      properties.load(TaskEngine.class.getResource("default.properties").openStream());               
   }
      
   protected void defaultProperties () {
      DEBUG = Utils.parseBoolean(getProperty("engine@debug"));
   }

   /**
    * Convert given value to string using <em>Javascript</em> toString method,
    * if possible.
    */
   public String toString (Object value) {
      if ( value instanceof List ) {
         StringBuilder buffer = new StringBuilder().append('[');
         boolean first = true;
         for (Object item : (List<?>) value) {
            if ( !first ) buffer.append(", ");
            first = false;
            buffer.append(toString(item));
         }
         return buffer.append(']').toString();
      }
      if ( value == null || value instanceof Number || value instanceof String ) 
         return value+"";
      try {
         return scriptEngine.isScriptable(value) ?
            (String) scriptEngine.invokeFunction("edu_wpi_cetask_toString", value) :
               value+"";
      } catch (ScriptException e) { throw new RuntimeException(e); }         
        catch (NoSuchMethodException e) { throw new RuntimeException(e); }
   }

   private long tick = 0;
   
   public long getTick () { return tick; }
   
   /**
    * Run method called by {@link #tick()} after tick incremented and before
    * {@link #clearLiveAchieved()}.
    */
   public Runnable onTick;
   
   /**
    * This method <em>must</em> be called at appropriate times to clear
    * liveness cache. 
    */
   public void tick () { 
      synchronized (synchronizer) {
         if ( tick == Long.MAX_VALUE ) {
            getErr().println("WARNING: wrapping tick back to zero.");
            tick = 0;
         } else tick++; 
         if ( onTick != null ) onTick.run();
         clearLiveAchieved(); 
      }
   }

   private Task lastOccurrence;
   private Plan lastContributes;
   
   /**
    * Return the most recently interpreted occurrence.
    * 
    * @see #getLastContributes()
    */
   public Task getLastOccurrence () { return lastOccurrence; }
   
   /**
    * Return the plan to which the most recently interpreted occurrence 
    * directly contributes, or null if it was unexplained. 
    * 
    * @see #getLastOccurrence()
    */
   public Plan getLastContributes () { return lastContributes; }
   
   /**
    * Evaluate in using default (global) context (not in any task instance)
    */
   public Object eval (String script, String where) {
      try { return scriptEngine.eval(script); }
      catch (Exception e) {
         if ( DEBUG ) getErr().println(script);
         throw newRuntimeException(e, where); } 
   }

   public Object eval (String script, Bindings bindings, String where) {
      try { return scriptEngine.eval(script, bindings); }
      catch (Exception e) {
         if ( DEBUG ) getErr().println(script);
         throw newRuntimeException(e, where); } 
   }

   static RuntimeException newRuntimeException (Exception e, String where) {
      return new RuntimeException("Evaluating "+where+"\n"+e, e);
   }
   
   public Object eval (InputStream input, String where) {
      return eval(Utils.toString(input), where);
   }

   Boolean evalBoolean (String script, String where) {
      if ( script == null || script.length() == 0 ) return null;
      try { return (Boolean) scriptEngine.eval(script); }
      catch (Exception e) { throw newRuntimeException(e, where); } 
   }
   
   Boolean evalBoolean (String script, Bindings bindings, String where) {
      if ( script == null || script.length() == 0 ) return null;
      try { return scriptEngine.evalBoolean(script, bindings); }
      catch (Exception e) { throw newRuntimeException(e, where); } 
   }
  
   Double evalDouble (String script, Bindings bindings, String where) {
      try { return scriptEngine.evalDouble(script, bindings); } 
      catch (Exception e) { throw newRuntimeException(e, where); } 
   }
   
   Long evalLong (String script, Bindings bindings, String where) {
      try { return scriptEngine.evalLong(script, bindings); } 
      catch (Exception e) { throw newRuntimeException(e, where); } 
   }
   
   /**
    * Sets given variable to given value in global scope of this engine.
    * <p>
    * WARNING: If this variable already has a value or has been initialized (by
    * 'var') using {@link #eval(String,String)} or related methods, then this
    * method will create a <em>new</em> copy of the variable whose value will be
    * independent of the original variable.
    */
   public void setGlobal (String variable, Object value) {
      Bindings bindings = scriptEngine.getBindings(ScriptContext.GLOBAL_SCOPE);
      synchronized (bindings) { bindings.put(variable, value); }
   }
   
   /**
    * Get the value of given variable in global scope of this engine.
    * See {@link #setGlobal(String,Object)} for WARNING!
    */
   public Object getGlobal (String variable) {
      Bindings bindings = scriptEngine.getBindings(ScriptContext.GLOBAL_SCOPE);
      synchronized (bindings) { return bindings.get(variable); }
   }
   
   public boolean isDefinedGlobal (String variable) {
      Bindings bindings = scriptEngine.getBindings(ScriptContext.GLOBAL_SCOPE);
      synchronized (bindings) { return bindings.containsKey(variable); }
   }
   
   public void deleteGlobal (String variable) {
      Bindings bindings = scriptEngine.getBindings(ScriptContext.GLOBAL_SCOPE);
      synchronized (bindings) { bindings.remove(variable); }
   }
   
   public Compiled compile (String script, String where) {
      if ( script.length() == 0 ) return null;
      try { return scriptEngine.compile(script); } 
      catch (ScriptException e) { 
         getErr().println("WARNING: Javascript syntax error in "+where               +"\n"+e);
         getErr().println("\'"+script+"\'");
         return null;
      }
   }
  
   public Object newObject () { 
      try { return scriptEngine.invokeFunction("Object"); }
      // should never have exception
      catch (Exception e) { throw new IllegalStateException(e); }
   }
   
   final Map<String,TaskModel> models = new HashMap<String,TaskModel>(); 
   
   // default properties for all task models
   final protected Properties properties = new Properties();
   
   public Properties getProperties () { return properties; }
   
   public String getProperty (String key) { 
      return properties.getProperty(key);
   }
   
   public boolean hasProperty (String key) {
      return properties.getProperty(key) != null;
   }
   
   public Boolean getProperty (String key, Boolean defaultValue) {
      String value = properties.getProperty(key);
      return value == null ? defaultValue : (Boolean) Utils.parseBoolean(value);
   }
   
   public void setProperty (String key, boolean value) {
      properties.put(key, Boolean.toString(value));
   }
   
   public void setProperty (String key, String value) {
      properties.put(key, value);
   }
   
   public String removeProperty (String key) {
      return (String) properties.remove(key);
   }
   
   // schemas and validation

   // note static initialization is thread-safe
   static protected final String xmlns = "http://ce.org/cea-2018";
   static private final String rng = "cea-2018.rng";
   static protected final VerifierFactory verifierFactory = new TheFactoryImpl();   
   static protected final Schema schema;

   static protected final XPathFactory xpathFactory = XPathFactory.newInstance();
   static private final XPath xpath = xpathFactory.newXPath();
   static private final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
   
   static {
      try { schema = verifierFactory.compileSchema(
                      TaskEngine.class.getResourceAsStream(rng), rng);
      } catch (Exception e) { throw new RuntimeException(e); }
      xpath.setNamespaceContext(new Context(xmlns));
      builderFactory.setNamespaceAware(true); 
   }

   protected final DocumentBuilder builder;
   protected final Handler handler = new Handler();
   { 
      synchronized (this) {
         try { builder = builderFactory.newDocumentBuilder(); } 
         catch (ParserConfigurationException e) { throw new RuntimeException(e); }
      }
     builder.setErrorHandler(handler); 
   }
   
   // note: need to make a new verifier each time because it appears
   // to remember data if processing same file again
   protected Verifier newVerifier (Schema schema) {
      try { 
         Verifier verifier = schema.newVerifier(); 
         verifier.setErrorHandler(handler); 
         return verifier; 
      } catch (VerifierConfigurationException e) { throw new RuntimeException(e); }
   }
   
   protected static class Context implements NamespaceContext {
      private final String xmlns;
      public Context (String xmlns) { this.xmlns = xmlns; }
      @Override
      public String getNamespaceURI (String prefix) { return this.xmlns; }
      @Override
      public String getPrefix (String namespace) { return XMLConstants.DEFAULT_NS_PREFIX; }
      private final Iterator<String> prefixes =
         Collections.singletonList(XMLConstants.DEFAULT_NS_PREFIX).iterator();
      @Override
      public Iterator<String> getPrefixes (String namespace) { return prefixes; } 
   }
   
   private static class Handler implements ErrorHandler { 
      @Override
      public void warning (SAXParseException e) { print(e); }
      @Override
      public void error (SAXParseException e) { print(e); }
      @Override
      public void fatalError (SAXParseException e) { print(e); }
      
      private String from;  // set before each builder or verifier call
      
      private void print (SAXParseException e) { 
         if ( e.getSystemId() != null )
               System.err.print(e.getSystemId()
                     +" ("+e.getLineNumber()+":"+e.getColumnNumber()+") ");
         else if ( from != null ) System.err.print(from+" ");
         System.err.println("Syntax Error: "+e);
      }
   }
   
   /**
    * Load, parse, validate and return task model from specified XML document.  Loads the
    * first toplevel &lt;taskModel&gt; element. Validation errors written to error output.
    * Also loads associated properties file(s), if they exist.
    * 
    * <em>NB:</em> This implementation does not support forward or circular 
    * namespace references using prefixes.  Thus task models must be loaded
    * in order such that all external references to task model namespaces refer 
    * to already loaded models.
    * 
    * Note you can also do stand-alone validation from command line as follows
    * (ignore warnings about schema):
    *   
    * <tt>java -jar lib/msv-rng.jar java/edu/wpi/cetask/cea-2018.rng models/ABC.xml</tt>
    *
    * @see #load(Document,Properties)
    * 
    * @param from string identifying url, system resource (without leading slash)
    * or file containing XML document
    * @return null if validation errors
    */
   public TaskModel load (String from) {
      handler.from = null;  // systemId non-null
      try { return load(from, Utils.toURL(from).openStream()); }
      catch (Exception e) { throw new RuntimeException(e); }
   }
   
   /**
    * Parse task model from specified XML document.  This method only parses, it does
    * not create task model.
    * 
    * @see #load(String)
    */
   public Document parse (String from) {
      handler.from = null;  // systemId non-null
      try { return parse(Utils.toURL(from).openStream(), from); }
      catch (IOException e) { throw new RuntimeException(e); }
   }
      
   /**
    * Load, parse, validate and return task model and associated properties file
    * from strings.  Used for DiscoUnity.
    * 
    * @param from name of model (for error messages)
    * @param model xml for model
    * @param properties associated properties file (or null)
    * 
    * @see #load(String)
    */
   public TaskModel load (String from, String model, String properties) {
         return load(from, 
               parse(new ByteArrayInputStream(model.getBytes()), from),
               properties == null ? null :
                  Utils.loadProperties(new ByteArrayInputStream(properties.getBytes())));
   }

   protected TaskModel load (String from, InputStream input) {
        return load(from, parse(input, from), loadProperties(from, ".properties")); 
   }
   
   protected Document parse (InputStream stream, String from) {
      Document document;
      try { document = builder.parse(stream, from); }
      catch (SAXException e) { print(e); return null; }
      catch (IOException e) { throw new RuntimeException(e); }
      try { // remove whitespace outside tags (for better printing later)
         NodeList nodeList = (NodeList) xpath.evaluate("//text()[normalize-space()='']",
               document,
               XPathConstants.NODESET);
         for (int i = 0; i < nodeList.getLength(); ++i) {
            Node node = nodeList.item(i);
            node.getParentNode().removeChild(node);
         }
      } catch (XPathExpressionException e) { throw new RuntimeException(e); }
      return document;
   }
   
   /** 
    * Load, parse, validate and return task model from specified XML document.  
    * Validation errors written to error output.
    * Also loads associated properties information, if non-null.  Note that
    * such models should be built using a "task aware" DOM builder.
    * 
    * <em>NB:</em> This implementation does not support forward or circular 
    * namespace references using prefixes.  Thus task models must be loaded
    * in order such that all external references to task model namespaces refer 
    * to already loaded models.
    *
    * @see #load(String)
    * 
    * @return null if validation errors
    */
   public TaskModel load (Document document, Properties properties) {
      return load(null, document, properties);
   }
    
   protected TaskModel load (String from, Document document, Properties properties) {
      // use about attribute to identify document in error messages when no file
      handler.from = ( from == null ? document.getDocumentElement().getAttribute("about") 
                       : null );
      String xmlns = document.getDocumentElement().getAttribute("xmlns");
      boolean valid = false;
      try { valid = verify(from, document, xmlns); }
      catch (SAXException e) { print(e); }
      URL source = null;
      if ( from != null )
         try { source = Utils.toURL(from); }
         catch (MalformedURLException e) { throw new RuntimeException(e); }
      if ( !valid ) {
         if ( from != null ) // re-validate to get line numbers in file
            try {
               // systemId may be null (e.g., for duplicate id check)
               handler.from = source.getFile(); 
               verify(handler.from, xmlns); 
            } catch (SAXException e) { print(e); }
              catch (IOException e) { throw new RuntimeException(e); }
         return null;
      }
      TaskModel model = newTaskModel(document, xpath, xmlns);
      model.setSource(source);
      // to catch global engine properties in individual model files
      if ( properties != null ) this.properties.putAll(properties);
      // create task classes
      for (Node task : model.xpathNodes("./n:task"))
         new TaskClass(task, model.xpath, model); 
      // create nested decomposition classes (after all task classes created)
      for (TaskClass task : model.getTaskClasses())
         for (Node subtasks : task.xpathNodes("./n:subtasks"))
            new DecompositionClass(subtasks, model.xpath, model, task); 
      // create toplevel decomposition classes
      for (Node subtasks : model.xpathNodes("./n:subtasks"))
         new DecompositionClass(subtasks, model.xpath, model, null); 
      clearLiveAchieved();
      // cache toplevel scripts and evaluate initialization script
      for (Node node : model.xpathNodes("./n:script")) {
         if ( model.scripts.isEmpty() ) model.scripts = new ArrayList<Grounding>(2);
         if ( Utils.parseBoolean(Description.xpath(node, model.xpath, "./@init")) ) { 
            Init script = new Init(node, model.xpath, this);
            script.setEnclosing(model);
            eval(script.getScript(), model.getNamespace()+" init");
         } else {
            Grounding script = new Grounding(node, model.xpath, this);
            script.setEnclosing(model); 
            model.scripts.add(script);
         }
      }
      // very last
      if ( callback != null ) callback.onLoad(from, model);
      return model;
   }

   private void print (SAXException e) {
      getErr().println(handler.from+": "+e); 
   }

   // following three methods broken out for extension in Disco
   
   protected boolean verify (String from, Document document, String xmlns) throws SAXException {
      if ( !xmlns.equals(TaskEngine.xmlns) )
         throw new RuntimeException(from+" xmlns is not "+TaskEngine.xmlns);
      return newVerifier(schema).verify(document);
   }
   
   protected boolean verify (String file, String xmlns) throws SAXException, IOException {
      return newVerifier(schema).verify(file);
   }
   
   protected TaskModel newTaskModel (Document document, XPath xpath, String xmlns) {
      return new TaskModel(document.getDocumentElement(), xpath, this);
   }
   
   protected Properties loadProperties (String source, String extension) {
      String filename = Utils.replaceEndsWith(source, ".xml", extension);
      Properties properties = Utils.loadProperties(filename);
      if ( VERBOSE && properties != null )
         getOut().println("    # Loaded "+ filename);
      return properties;
   }
   
   /**
    * Interface for {@link #setOnLoading(OnLoading)} callback.
    */
   public interface OnLoading {
      void onLoad (String uri, TaskModel model);
   }
   
   private OnLoading callback = null;
   
   /**
    * Set object to be called after new task model loaded.
    */
   public void setOnLoading (OnLoading callback) { this.callback = callback; }
   
   /**
    * Return task model for given namespace. If such a model has
    * not already been loaded, resolve namespace  and load.
    */
   public TaskModel getModel (String namespace) {
      TaskModel model = models.get(namespace);
      if ( model != null ) return model;
      // no way to resolve namespaces yet
      throw new IllegalArgumentException("Cannot resolve namespace: "+namespace);
   }
 
   /**
    * Return collection of task models loaded into this engine.
    */
   public Collection<TaskModel> getModels () { return Collections.unmodifiableCollection(models.values()); }

   /**
    * Return unique task class identified by given qualified name or null if none
    */
   public TaskClass resolveTaskClass (QName qname) {
      return getModel(qname.getNamespaceURI()).getTaskClass(qname.getLocalPart()); 
   }
   
   /**
    * Return the unique task class, if any, in currently loaded task models
    * which is identified by given id.
    *  
    * @return task class or null if none
    * @throws AmbiguousIdException if id in more than one model
    * @throws IllegalArgumentException if id is a decomposition class
    */
   public TaskClass getTaskClass (String id) {
      TaskModel.Member member = getMember(id);
      if ( member == null || member instanceof TaskClass ) return (TaskClass) member;
      else throw new IllegalArgumentException("Id is not task class: "+id);
   }
    
   /**
    * Return the unique decomposition class, if any, in currently loaded task models
    * which is identified by given id.
    *  
    * @return decomposition class or null if none
    * @throws AmbiguousIdException if id in more than one model
    */
   public DecompositionClass getDecompositionClass (String id) {
      TaskModel.Member member = getMember(id);
      if ( member == null || member instanceof DecompositionClass ) return (DecompositionClass) member;
      else throw new IllegalArgumentException("Id is not decomposition class: "+id);
   }
   
   private TaskModel.Member getMember (String id) {
      TaskModel.Member found = null;
      for (TaskModel model : models.values()) {
         TaskModel.Member member = model.getTaskClass(id);
         if ( member != null ) {
            if ( found == null ) found = member;
            else throw new AmbiguousIdException(id+" found in "
                  +member.getNamespace()+" and "+found.getNamespace());
         }
         member = model.getDecompositionClass(id);
         if ( member != null ) {
            if ( found == null ) found = member;
            else throw new AmbiguousIdException(id+" found in "
                  +member.getNamespace()+" and "+found.getNamespace());
         }
      }
      return found;
   }
    
   public static class AmbiguousIdException extends RuntimeException {
      public AmbiguousIdException (String message) { super(message); }
   }
   
   /**
    * Make a copy in this engine of given task instance, possibly from another 
    * engine.  Recursively copies task values in slots.
    * 
    * @throws IllegalArgumentException if task class does not exist in this engine
    */
   public Task copy (Task task) {
      TaskClass type = task.getType();
      String namespace = type.getNamespace();
      TaskModel model = models.get(namespace);
      if ( model == null ) 
         throw new IllegalArgumentException("Namespace not found: "+namespace);
      String id = type.getId();
      TaskClass thisType = model.getTaskClass(id);
      if ( thisType == null )
         throw new IllegalArgumentException("Task class not found: "+id);
      Task thisTask = thisType.newInstance();
      for (String name : type.inputNames)  
         copySlotValue(task, thisTask, name);
      for (String name : type.outputNames)
         copySlotValue(task, thisTask, name);
      return thisTask;
   }
   
   private void copySlotValue (Task from, Task to, String name) {
      Object object = from.bindings.get("$this");
      if ( isDefined(object, name) ) {
         Object javaValue = from.getSlotValue(name);
         if ( javaValue instanceof Task ) {
            to.setSlotValue(name, copy((Task) javaValue));
         } else 
            scriptEngine.put(to.bindings.get("$this"), name, scriptEngine.get(object, name));
      }
   }

   // for extension
   public String getExternalName () { return "user"; }
   public String getSystemName () { return "system"; } 
   
   private long start; 
   
   /**
    * @see #setStart(long)
    */
   public long getStart () { return start; }
   
   /**
    * Set the start time to be used for 'when' slot in verbose version of
    * {@link Task#toString()}.  If start time is greater than zero, then shows 
    * 'when' in seconds since start time, rather than UTC in milliseconds.
    * 
    * @param start - start time in milliseconds (e.g., {@link System#currentTimeMillis()})
    * 
    * @see #VERBOSE
    */
   public void setStart (long start) { this.start = start; }
   
   // toplevel plan management
   
   private Plan root;
   
   protected Plan getRoot () { return root; }
   
   private final TaskClass rootClass;
         
   public void clear () {
      root = new Plan(rootClass.newInstance()); 
      focus = null;
      if ( cacheLive != null ) clearLiveAchieved(); // called from constructor
   }
   
   public List<Plan> getTops () { return root.getChildren(); }

   /**
    * Create new instance of given task class and add a plan to 
    * achieve it to task tree at toplevel. (Convenience method for JavaScript)   
    * 
    * @return the new plan
    */
   public Plan addTop (String task) { 
      return addTop(getTaskClass(task));
   }
   
   /**
    * Create new instance of given task class and add a plan to 
    * achieve it to task tree at toplevel.   
    * 
    * @return the new plan
    */
   public Plan addTop (TaskClass task) { 
      return addTop(task.newInstance()); 
   }

   /**
    * Add plan for given task to current task tree at toplevel. 
    * 
    * @return the new plan
    */
   public Plan addTop (Task task) { 
      Plan plan = new Plan(task); 
      addTop(plan);     
      return plan;
   }
   
   /**
    * Add given plan to current task tree at toplevel.   
    */
   public void addTop (Plan plan) { 
      root.add(plan);
      if ( !plan.isDone() ) {
         plan.decomposeAll();
         if ( focus == null ) focus = plan;
      }
      Task task = plan.getGoal();
      // for convenience make every toplevel task T accessible in JavaScript as $T
      setGlobal('$'+task.getType().getId(), task.bindings.get("$this"));
   }
   
   /**
    * Remove given toplevel plan from current tree. Also sets focus to null, if
    * given plan was focus.
    */
   public void removeTop (Plan plan) { 
      root.remove(plan);
      if ( focus == plan ) focus = null;
   }
      
   /**
    * Return the toplevel plan which is the root of given plan.
    */
   public Plan getTop (Plan plan) {
      Plan parent = plan.getParent();
      if ( parent == null ) 
         throw new IllegalArgumentException("Plan with no parent: "+plan);
      return parent == root ? plan : getTop(parent);
   }

   /**  
    * Test whether given plan is at toplevel in current tree.
    */
   public boolean isTop (Plan plan) { return plan.getParent() == root; }
   
   /**
    * Call {@link Plan#decomposeAll()} on all plans in current tree.
    */
   public void decomposeAll() {
      for (Plan top : getTops()) 
         if ( !top.isDone() ) {
            // decomposition changes liveness of Propose.How
            if ( top.decomposeAll() ) clearLiveAchieved();
      }
   }
   
   // cache liveness and postconditions to speed up (clear at same time)
   
   private final Map<Plan, Boolean> cacheLive = new IdentityHashMap<Plan, Boolean>();

   Boolean isLive (Plan plan) { return cacheLive.get(plan); }
   
   boolean setLive (Plan plan, boolean live) { 
      cacheLive.put(plan, live);
      // to make sure bindings that do not depend on other bindings
      // are updated before plugins run or task executed
      if ( live ) plan.getGoal().updateBindingsTask(false);
      return live;
   }
   
   private final Map<Task, Boolean> cacheAchieved = new IdentityHashMap<Task, Boolean>();
   
   Boolean isAchieved (Task task) { return cacheAchieved.get(task); }
   
   // DESIGN NOTE: Since postconditions can return null (unknown), need to 
   //              explicitly check for keys.  Not returning Map.Entry
   //              to avoid creating new instances
   
   boolean containsAchieved (Task task) { return cacheAchieved.containsKey(task); }
   
   Boolean setAchieved (Task task, Boolean achieved) { 
      cacheAchieved.put(task, achieved); 
      return achieved;
   }
   
   void clearLive () {
      cacheLive.clear(); 
   }
   
   public void clearLiveAchieved () {
      clearLive();
      cacheAchieved.clear();
      // this is a good time to check globally for fortuitous success 
      root.checkSuccess();   
   }
   
   /**
    * Method for notifying task engine that given primitive <em>user</em> task 
    * has occurred.
    * 
    * @return matching plan in current task tree or null if none
    * 
    * @see Plan#execute()
    */
   public Plan occurred (Task occurrence) {
      // leave "success" undefined (see doc for Task.isDefinedOutputs)
      return occurred(true, occurrence, null, false);
   }
 
   // factorization of occurred below is to support extension in Disco
   
   public Plan occurred (boolean external, Task occurrence, Plan contributes, 
         boolean eval) { 
      occurrence.setExternal(external);
      // do explanation before evaluating scripts, since expectations are in
      // terms of state of world before execution
      if ( contributes == null ) contributes = explainBest(occurrence, true);
      // check for continuation before setWhen
      boolean continuation = contributes != null && contributes.isStarted();
      if ( external ) { 
         occurrence.occurred(); 
         // sic evalIf, since overridden in Utterance
         if ( eval ) occurrence.eval(contributes); 
         else occurrence.evalIf(contributes); 
      } else occurrence.execute(contributes);
      occurred(occurrence, contributes, continuation);     
      return contributes;
   }

   protected Plan occurred (Task occurrence, Plan contributes, boolean continuation) {
      interpret(occurrence, contributes, continuation);
      return contributes;
   }
   
   /**
    * Update engine state based on this occurrence.
    * 
    * @param contributes plan to which occurrence contributes (e.g., matches) or null
    * @param continuation true iff contributes plan started before this occurrence
    * @return false iff this task is unexplained
    * 
    * @see Task#interpret(Plan,boolean)
    */
   protected boolean interpret (Task occurrence, Plan contributes, boolean continuation) {
      boolean explained = occurrence.interpret(contributes, continuation);
      if ( !explained ) unexplained(occurrence);
      // checkAchieved after interpret so values propagated
      occurrence.checkAchieved();
      if ( contributes != null && contributes.getType() == occurrence.getType()) 
         // TODO is this best/only place to check success/failure?
         contributes.checkAchieved(); 
      lastOccurrence = occurrence;
      lastContributes = contributes;
      return explained;
   }
    
   protected void unexplained (Task occurrence) {
      if ( DEBUG )
         getOut().println("Unexplained occurrence: "+occurrence.format());
      occurrence.setUnexplained(true);
   }
   
   /**
    * Return compact human-readable string for given task using this engine.
    * (for Disco extension)
    */
   protected String format (Task task) {
      // to support extensions
      return task.formatTask();
   }

   // for overriding in Disco
   public PrintStream getOut () { return System.out; }
   
   // for overriding in Disco
   public PrintStream getErr () { return System.err; }

   /**
    * Return format string for given task, or null if none
    * (for Disco extension)
    */
   protected String getFormat (Task task) {  
      if ( TaskEngine.VERBOSE ) {
         String format = task.getProperty("@verbose");
         if ( format != null && format.length() > 0 ) return format;   
      }
      return task.getFormat();
   }
   
   // simple focus system
   
   private Plan focus;
   
   /**
    * Return the current focus in the task tree, or null if no focus.<br>
    * <br>
    * Note that this is <em>not</em> the discourse concept of focus (cf.
    * Collagen), but rather a much simpler "execution" focus. For example, there
    * is no focus stack. Also, the focus will not usually not be on a done plan.
    * Management of the focus is the responsibility of the application using the
    * engine (e.g., the Guide), with the exception of {@link #addTop(Plan)} and
    * {@link #removeTop(Plan)}, as documented.<br>
    * <br>
    * Note also that this is not the same as the "next" live action, since in a
    * partially ordered plan, there may be several next actions to choose from.
    * The current focus will usually have the next live actions as its children.
    * 
    * @see #setFocus(Plan)
    */
   public Plan getFocus () { return focus; }
   
   /**
    * Shift focus to the given plan or null.
    * 
    * @return true iff this is an "unnecessary focus shift" in the sense of 
    *         docs/LeshRichSidner2001_UM.pdf, i.e., we are leaving the current
    *         focus when it is started but not done.
    *         
    * @see #getFocus()        
    */
   public boolean setFocus (Plan focus) {
      if ( focus == this.focus ) return false;
      if ( focus != null && (focus == root || !focus.isAncestor(root)) )
         throw new IllegalArgumentException(focus+" is not in task tree!");
      Plan old = this.focus;
      this.focus = focus;
      return old != null 
         && old.isStarted() && !old.isDone()
         && (focus == null || !( old.isAncestor(focus) || focus.isAncestor(old)) ); 
   }
   
   /**
    * Set focus to (possibly null) parent of current focus. 
    */
   public void pop () { setFocus(focus.getParent()); }
   
   /**
    * Return first ancestor of given plan which is live or has live descendants,
    * stopping at toplevel plan containing given plan, or null if none.
    */
   public Plan newFocus (Plan plan) {
      Plan parent = plan.getParent();
      // TODO optimize this code?
      return parent == null || parent == root ? null :
            (parent.isLive() || parent.hasLiveDescendants()) ? parent : 
               newFocus(parent);
   }
 
   // simple plan recognition (without decomposition choice interpolation)
   
   /**
    * Find the plan in current task tree, if any, which best explains given
    * task (i.e., to which given task contributes).
    * 
    * @return plan which explains this task or null, if none
    * 
    * @see #explain(Task,boolean)
    */
   public Plan explainBest (Task task, boolean onlyLive) {
      List<Plan> explanations = explain(task, onlyLive);
      return explanations.isEmpty() ? null : resolveAmbiguity(explanations);
   }
   
   /**
    * Return list of plans in task tree which explain given task, searching
    * in the following categories, in order:
    * <ol>
    * <li> within current focus
    * <li> within current toplevel plan
    * <li> any toplevel plan
    * </ol>
    * If plans are found in one category, search does <em>not</em> continue to next category.
    * 
    * @param onlyLive only search live plans
    */
   public List<Plan> explain (Task task, boolean onlyLive) {
      Plan focus = getFocus(); 
      Plan top = null;
      if ( focus != null ) {
         List<Plan> explanations = focus.explain(task, onlyLive, null);
         if ( !explanations.isEmpty() ) return explanations;
         top = getTop(focus);
         explanations = top.explain(task, onlyLive, focus);
         if ( !explanations.isEmpty() ) return explanations;
      }
      return root.explain(task, onlyLive, top);
   }
   
   private Plan resolveAmbiguity (List<Plan> explanations) {
      if ( explanations.isEmpty() ) return null;
      Plan first = explanations.get(0);
      if ( explanations.size() > 1 ) ambiguous(explanations);
      return first;
   }
   
   protected void ambiguous (List<?> explanations) {
      getOut().println("Ignoring ambiguity of "+explanations.size());
      if ( DEBUG ) for (Object e : explanations) getOut().println(e);      
   }
   
   // for extension to plan recognition with interpolation of decompositions

   // package permission for TaskClass
   // note this list includes primitives, because there is no way to know
   // if they won't be decomposed by later loaded library--see getTopClasses()
   final List<TaskClass> topClasses = new ArrayList<TaskClass>(); 
   
   /**
    * Test whether this engine has plan recognition.
    */
   public boolean isRecognition () { return false; }

   /**
    * Returns unmodifiable list of non-primitive task classes which can serve as root of plan
    * recognition. By default, these are classes that do not contribute to any other task classes.
    * However, this can be overridden by @top property in library.
    * 
    * @see TaskClass#isTop()
    * @see TaskClass#setTop(boolean)
    */
   public List<TaskClass> getTopClasses () { 
      for (TaskClass task : topClasses)
         if ( task.isPrimitive() ) {
            List<TaskClass> tops = new ArrayList<TaskClass>(topClasses.size());
            for (TaskClass top : topClasses)
               if ( !top.isPrimitive() ) tops.add(top);
            return tops;
         }
      return Collections.unmodifiableList(topClasses); 
   }
}
