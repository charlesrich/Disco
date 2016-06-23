/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.cetask;

import org.w3c.dom.*;
import java.beans.Expression;
import java.util.*;
import javax.xml.xpath.*;
import edu.wpi.cetask.ScriptEngineWrapper.Compiled;

public class TaskClass extends TaskModel.Member {
  
   private final Precondition precondition;
   private final Postcondition postcondition; 
   
   // DESIGN NOTE: Pre/Postcondition, Input/Output, etc., below are *not* inner classes,
   // so that they can be constructed *before* being passed to task class constructor
   // when creating task classes without XML.
   
   public static class Precondition extends Condition {
      
      public Precondition (String script, boolean strict, TaskEngine engine) {
         super(script, strict, engine);
      }
      
      @Override
      protected boolean check (String slot) {
         if ( getEnclosing().inputNames.contains(slot) ) return true;
         getEnclosing().getErr().println("WARNING: $this."+slot+" not a valid input in "+where);
         return false;
      }
      
      @Override
      public TaskClass getEnclosing () { return (TaskClass) super.getEnclosing(); }
   }
   
   public static class Postcondition extends Condition {
      
      private final boolean sufficient;
      
      public Postcondition (String script, boolean strict, boolean sufficient, TaskEngine engine) {
         super(script, strict, engine);
         this.sufficient = sufficient;
      }
      
      @Override
      protected boolean check (String slot) {
         if ( getEnclosing().inputNames.contains(slot) || getEnclosing().outputNames.contains(slot) ) return true;
         getEnclosing().getErr().println("WARNING: $this."+slot+" not a valid input or output in "+where);
         return false;
      }
      
      @Override
      public TaskClass getEnclosing () { return (TaskClass) super.getEnclosing(); }
      
      public boolean isSufficient () { return sufficient; }
   }
   
   /**
    * Returns true iff the precondition, postcondition and applicability
    * conditions for all decompositions for this task class are strict, i.e., 
    * the condition returns null if any of the slots on which it depends is undefined.
    * <p>
    * Controlled by @strict property (default true)
    */
   public boolean isStrict () {
      return getProperty("@strict", true);
   }
   
   /**
    * @return precondition of this task class (or null if none defined)
    */
   public Precondition getPrecondition () { return precondition; }
   
   /**
    * @return postcondition of this task class (or null if none defined)
    */
   public Postcondition getPostcondition () { return postcondition; }
   
   /**
    * Returns true iff postcondition is provided and is sufficient.
    */
   public boolean isSufficient () { 
      return postcondition != null && postcondition.isSufficient();
   }
   
   private Class<? extends Task> builtin;
   
   // may be toplevel (default) grounding script
   
   public static class Grounding extends Script {

      private final Applicability applicable;
      private final String platform, deviceType, model;
      private final TaskClass task;

      public String getPlatform () { return platform; }
      public String getDeviceType () { return deviceType; }
      public String getModel () { return model; }
      
      public TaskClass getTask () {
         return getEnclosing() instanceof TaskClass ? (TaskClass) getEnclosing() : task;
      }

      Grounding (Node node, XPath xpath, TaskEngine engine) {
         this(parseText(node, xpath), 
               parseApplicable(node, xpath, engine), 
               parseTask(node, xpath, engine),
               Utils.emptyNull(xpath(node, xpath, "./@platform")),
               Utils.emptyNull(xpath(node, xpath, "./@deviceType")),
               Utils.emptyNull(xpath(node, xpath, "./@model")),
               engine);        
      }
      
      private static Applicability parseApplicable (Node node, XPath xpath, TaskEngine engine) {
         String condition = xpath(node, xpath, "./@applicable");
         return condition.isEmpty() ? null : 
            new Applicability(condition, Condition.isStrict(engine, TaskModel.parseId(node, xpath)), engine);
      }
      
      private static TaskClass parseTask (Node node, XPath xpath, TaskEngine engine) {
         String qname = xpath(node, xpath, "./@task");
         return qname.isEmpty() ? null : resolveTaskClass(node, parseAbout(node, xpath), qname, engine);
      }
      
      public Grounding (String script, TaskEngine engine) {
         this(script, null, null, null, null, null, engine);
      }
      
      public Grounding (String script, Applicability applicable, TaskClass task,
            String platform, String deviceType, String model, TaskEngine engine) {
         super(script, engine);
         this.applicable = applicable;
         if ( applicable != null ) applicable.setEnclosing(this);
         this.task = task;
         this.platform = platform;
         this.deviceType = deviceType;
         this.model = model;
      }

      @Override
      void setEnclosing (Description enclosing) {
         super.setEnclosing(enclosing);
         if ( getTask() == null && this.platform == null )
            throw new RuntimeException("Syntax Error: Toplevel script without task, platform or init=true");
      }
      
      public Boolean isApplicable (Task occurrence) {
         return applicable == null ? null : applicable.evalCondition(occurrence);
      }      
      
      public static class Applicability extends Condition {
      
         private Applicability (String script, boolean strict, TaskEngine engine) {
            super(script, strict, engine);
         }
      
         @Override
         protected boolean check (String slot) {
            Description enclosing = getEnclosing().getEnclosing();
            if ( !(enclosing instanceof TaskClass) || 
                  ((TaskClass) enclosing).inputNames.contains(slot) ) return true;
            getEnclosing().getErr().println("WARNING: $this."+slot+" not a valid input in "+where);
            return false;
         }
      
         @Override
         public Grounding getEnclosing () { return (Grounding) super.getEnclosing(); }
      }
   }
   
   /**
    * A task class is builtin if its id is defined as a subclass of {@link Task}.
    * This allows overriding the behavior of the class.  
    * 
    * @see #newInstance()
    */
   public boolean isBuiltin () { return builtin != null; }
      
   final List<String> inputNames, outputNames, declaredInputNames, declaredOutputNames;
   
   private List<Grounding> scripts = Collections.emptyList();
   
   /**
    * Return all grounding scripts associated with this primitive task class (for all platforms and
    * device types). 
    * 
    * @see #getGrounding()
    */
   public List<Grounding> getGroundingAll () {
      if ( !scripts.isEmpty() && !isPrimitive() ) {
         getErr().println("Ignoring grounding script for non-primitive: "+this);
         return Collections.emptyList();
      }
      return scripts; 
   }
   
   /**
    * Return script to ground instances of this primitive task class appropriate
    * for platform and device type of this task engine, or null if none.
    */
   public Grounding getGrounding () {
      List<Grounding> candidates = new ArrayList<Grounding>(getGroundingAll());
      for (Iterator<Grounding> i = candidates.iterator(); i.hasNext();) {
         Grounding script = i.next();
         String platform = script.getPlatform(),
            deviceType = script.getDeviceType();
         // remove scripts not appropriate to current platform and deviceType
         if ( !( (platform == null || platform.equals(engine.getPlatform(this)))
                 && (deviceType == null || deviceType.equals(engine.getDeviceType(this))) ) )
            i.remove();
      }
      if ( candidates.isEmpty() ) { 
         // look for default script for model and then overall (must be toplevel)
         Grounding global = null;
         for (TaskModel model : engine.getModels()) {
            for (Grounding script : model.getGroundingAll()) {
               TaskClass task = script.getTask();
               if ( task == null ) {
                  String ns = script.getModel();
                  // not checking for multiple global scripts
                  if ( ns == null ) global = script; 
                  else if ( ns.equals(getNamespace()) )
                     candidates.add(script);
               } else if ( equals(task) ) candidates.add(script);
            }
         }
         if ( candidates.isEmpty() && global != null ) candidates.add(global);
      }
      if ( candidates.isEmpty() ) return null;
      if ( candidates.size() > 1 ) getErr().println("Ignoring multiple scripts for "+this);
      return candidates.get(0);
   }
   
   private static final List<String> primitiveTypes = Arrays.asList(new String[] {"boolean", "string", "number"});
 
   abstract static class SlotBase extends Description implements Description.Slot {
      
      protected final String name;
      
      @Override
      public String getName () { return name; }
      
      protected SlotBase (String name, Description enclosing) {
         super(enclosing == null ? null : enclosing.engine, null);
         this.name = name;
         setEnclosing(enclosing);
      }
      
      @Override
      public Object getSlotValue (Task task) { return task.getSlotValue(name); }

      @Override
      public boolean isDefinedSlot (Task task) { return task.isDefinedSlot(name); }

      @Override
      public Object setSlotValue (Task task, Object value) { return task.setSlotValue(name, value); }

      @Override
      public void setSlotValue (Task task, Object value, boolean check) { task.setSlotValue(name, value, check); }

      @Override
      public void setSlotValueScript (Task task, String expression, String where) { task.setSlotValueScript(name, expression, where); }

      @Override
      public void deleteSlotValue (Task task) { task.removeSlotValue(name); }
      
      @Override
      public String toString () {
         return getEnclosing() == null ? name : getEnclosing().toString()+'.'+name;
      }
   }
   
   public abstract static class Slot extends SlotBase {
      
      protected final String type;
      protected final Class<?> java;
      
      @Override
      public String getType () { return type; }
      
      @Override
      public Class<?> getJava () { return java; }
      
      @Override
      public TaskClass getEnclosing () { return (TaskClass) super.getEnclosing(); }
      
      // TODO Provide copy constructors.
   
      protected Slot (String name, TaskClass enclosing) {
         super(name, enclosing);
         if ( enclosing.slots.get(name) != null ) 
            throw new DuplicateSlotNameException(name, enclosing.getId());
         enclosing.slots.put(name, this);
         // compute type
         if ( name.equals("success") || name.equals("external") )
            this.type = "boolean";
         else if ( name.equals("when") ) this.type = "number";
         else {
            String path = "[@name=\""+name+"\"]/@type";
            String type = enclosing.xpath("./n:input"+path+" | "+"./n:output"+path);
            // type attribute is optional (extension by CR)
            if ( type.length() == 0 ) {
               if ( !enclosing.declaredInputNames.contains(name)
                     && !enclosing.declaredOutputNames.contains(name) )
                  throw new IllegalArgumentException(name+" is not slot of "+this);
               this.type = null;
            } else this.type = type;
         }
          // compute java class for type, if any
         if ( type == null || primitiveTypes.contains(type) )
            this.java = null;
         else {
            Object java = null;
            try { java = enclosing.engine.eval(type, "Slot constructor"); }
            // JavaScript constructor may not yet be defined since init script not evaluated yet
            catch (RuntimeException e) {}
            this.java = java instanceof Class ? (Class<?>) java : null;               
         }
      }
    
   }
   
   public static class Input extends Slot implements Description.Input {
      
      private final boolean optional;
      
      @Override
      public boolean isOptional () { return optional; }

      private final TaskClass.Output modified;
      
      @Override
      public TaskClass.Output getModified () { return modified; }
      
      @Override
      public boolean isDeclared () { return getEnclosing().declaredInputs.contains(this); }
      
      protected Input (String name, boolean declared, TaskClass enclosing) { 
         super(name, enclosing);
         if ( declared ) enclosing.declaredInputs.add(this);
         // temporary check for node null
         String modified = enclosing.node == null ? "" : enclosing.xpath("./n:input[@name=\""+name+"\"]/@modified");
         if ( modified.length() > 0 ) {
            if ( !enclosing.declaredOutputNames.contains(modified) ) { 
               getErr().println("WARNING: Ignoring unknown modified output slot: "+modified);
               this.modified = null;
            } else if ( primitiveTypes.contains(type) || 
                  (java != null && !Cloneable.class.isAssignableFrom(java)) ){
               getErr().println("WARNING: Ignoring modified attribute of non-cloneable input slot: "+name);
               this.modified = null;
            } else this.modified = (TaskClass.Output) enclosing.slots.get(modified);
         } else this.modified = null;  
         // cache optional
         this.optional = enclosing.getProperty(name, "@optional", false);
      }
   }

   public static class Output extends Slot implements Description.Output {
      
      @Override
      public boolean isDeclared () { return getEnclosing().declaredOutputs.contains(this); }
      
      protected Output (String name, boolean declared, TaskClass enclosing) { 
         super(name, enclosing);
         if ( declared ) enclosing.declaredOutputs.add(this);
      }
   }

   final List<Input> inputs, declaredInputs;
   final List<Output> outputs, declaredOutputs;
   
   private final Map<String,Slot> slots;
   
   @Override
   public Slot getSlot (String name) { return slots.get(name); }
   
   public Collection<Slot> getSlots () { return Collections.unmodifiableCollection(slots.values()); }
   
   private final boolean hasModifiedInputs;
   
   public boolean hasModifiedInputs () { return hasModifiedInputs; }
   
   TaskClass (Node node, XPath xpath, TaskModel model) { 
      this(node, xpath, model, TaskModel.parseId(node, xpath),
            parsePrecondition(node, xpath, model.getEngine()), 
            parsePostcondition(node, xpath, model.getEngine()),
            parseGrounding(node, xpath, model.getEngine()));
   }
   
   private static Precondition parsePrecondition (Node node, XPath xpath, TaskEngine engine) {
      String condition = xpath(node, xpath, "./n:precondition");
      return condition.isEmpty() ? null : 
         new Precondition(condition, Condition.isStrict(engine,TaskModel.parseId(node, xpath)), engine);
   }
   
   private static Postcondition parsePostcondition (Node node, XPath xpath, TaskEngine engine) {
      String condition = xpath(node, xpath, "./n:postcondition");
      String sufficient = xpath(node, xpath, "./n:postcondition/@sufficient"); 
      return condition.isEmpty() ? null : 
         new Postcondition(condition, Condition.isStrict(engine, TaskModel.parseId(node, xpath)),
               sufficient.length() > 0 && Utils.parseBoolean(sufficient), engine);
   }  
   
   private static List<Grounding> parseGrounding (Node node, XPath xpath, TaskEngine engine) {
      List<Grounding> scripts = Collections.emptyList();
      for (Node script : xpathNodes(node, xpath, "./n:script")) {
         if ( scripts.isEmpty() ) scripts = new ArrayList<Grounding>(2);
         scripts.add(new Grounding(script, xpath, engine));
      }  
      return scripts;
   }
   
   /**
    * Limited, incomplete constructor for task classes without using XML.
    * Provided to support LIMSI Discolog project.
    */
   public TaskClass (TaskModel model, String id, 
         Precondition precondition, Postcondition postcondition, Grounding script) {
       this(null, null, model, id, precondition, postcondition, 
             script == null ? Collections.<Grounding>emptyList() : Collections.singletonList(script));
   }
      
   @SuppressWarnings("unchecked")
   private TaskClass (Node node, XPath xpath, TaskModel model, String id, 
         Precondition precondition, Postcondition postcondition,
         List<Grounding> scripts) { 
      model.super(node, xpath, id);
      if ( id.length() != 0 ) model.tasks.put(id, this);
      for (Grounding script : scripts) {
         if ( this.scripts.isEmpty() ) this.scripts = new ArrayList<Grounding>(2);
         this.scripts.add(script);
         script.setEnclosing(this);
      }
      if ( node != null ) { // temporary check
         declaredOutputNames = xpathValues("./n:output/@name");
         declaredInputNames = xpathValues("./n:input/@name");
      } else {
         declaredOutputNames = Collections.emptyList();
         declaredInputNames = Collections.emptyList();
      }
      outputNames =  new ArrayList<String>(declaredOutputNames);
      inputNames = new ArrayList<String>(declaredInputNames);
      outputNames.add("success"); 
      outputNames.add("when");
      inputNames.add("external");
      slots = new HashMap<String,Slot>(inputNames.size()+outputNames.size());
      // create now for error checking
      new Output("success", false, this); 
      new Output("when", false, this); 
      new Input("external", false, this); 
      // create outputs first for modified
      declaredOutputs = new ArrayList<Output>(declaredOutputNames.size());     
      for (String name : declaredOutputNames) new Output(name, true, this);
      outputs = new ArrayList<Output>(declaredOutputs);
      declaredInputs = new ArrayList<Input>(declaredInputNames.size());
      // must be added here to preserve order at end
      outputs.add((Output) slots.get("success")); 
      outputs.add((Output) slots.get("when"));
      for (String name : declaredInputNames) new Input(name, true, this);
      inputs = new ArrayList<Input>(declaredInputs);
      // must be added here to preserve order at end
      inputs.add((Input) slots.get("external"));
      boolean hasModifiedInputs = false;
      for (Input input : inputs) {
         if ( input.modified != null ) {
            hasModifiedInputs = true;
            if ( !Utils.equals(input.type, input.modified.getType()) ) { // null types possible
               getErr().println("WARNING: Modified output slot of different type: "+input.name);
            }
         }
      }
      this.hasModifiedInputs = hasModifiedInputs;
      if ( declaredInputNames.contains("device") &&
            !"string".equals(getSlotType("device")) )
         throw new IllegalStateException("Device slot must be of type string in "
               +getId());
      // process conditions after inputs for error checking
      this.precondition = precondition;
      this.postcondition = postcondition;
      if ( precondition != null ) precondition.setEnclosing(this);
      if ( postcondition != null ) postcondition.setEnclosing(this);
      if ( (precondition != null && isStrict() != precondition.isStrict())
            || (postcondition != null && isStrict() != postcondition.isStrict()) )
         getErr().println("WARNING: Inconsistent strictness specifications for: "+this);
      if ( node != null ) { // temporary check
         // cache bindings
         NodeList nodes = (NodeList) xpath("./n:binding", XPathConstants.NODESET);
         for (int i = 0; i < nodes.getLength(); i++) { // preserve order
            Node bindingNode = nodes.item(i);
            try {
               String variable = xpath.evaluate("./@slot", bindingNode); 
               if ( !variable.startsWith("$this.") )
                  throw new TaskModel.Error(this, "Invalid task binding slot "+variable);
               String slot = variable.substring(6);
               for (Binding binding : bindings)
                  if ( binding.slot == slot)
                     throw new TaskModel.Error(this, "duplicate bindings for "+variable);
               String value =  xpath.evaluate("./@value", bindingNode);
               // TODO borrow error checking from DecompositionClass,
               //      e.g, for undefined slots 
               bindings.add(new Binding(slot, value));
            } catch (XPathException e) { throw new RuntimeException(e); }
         }
         // cache nested scripts
         for (Node scriptNode : xpathNodes("./n:script")) {
            if ( scripts.isEmpty() ) scripts = new ArrayList<Grounding>(2);
            Grounding script = new Grounding(scriptNode, xpath, engine);
            script.setEnclosing(this);
            scripts.add(script);
         }  
      }
      try {  
         builtin = ((Class<? extends Task>) Class.forName(getId()));
         builtin.getDeclaredField("CLASS").set(null, this);
      } 
      catch (ClassNotFoundException|ClassCastException e) {}
      catch (NoSuchFieldException e) { throw new IllegalStateException(e); }
      catch (IllegalAccessException e) { throw new IllegalStateException(e); }
      if ( !getId().equals("**ROOT**") && getEngine().isRecognition() ) {
         if ( getProperty("@top", true) ) // sic not isTop()
            // assume this can be top until find decomposition that uses step
            // see contributes
            getEngine().topClasses.add(this);
      }
   }
   
   // for root -- see TaskEngine.clear()
   TaskClass (TaskEngine engine, String id) {
      new TaskModel(null, engine).super(null, null, "**ROOT**");
      precondition = null; postcondition = null;
      inputNames = outputNames = declaredInputNames = declaredOutputNames = Collections.emptyList();
      declaredInputs = Collections.emptyList();
      declaredOutputs = Collections.emptyList();
      scripts = null; 
      slots = new HashMap<String,Slot>();
      inputs  =  new ArrayList<Input>();
      new Input("external", false, this); 
      outputs = new ArrayList<Output>();
      new Output("success", false, this); 
      new Output("when", false, this); 
      hasModifiedInputs = false;
   }
   
   private static class DuplicateSlotNameException extends RuntimeException {
      public DuplicateSlotNameException (String name, String id) {
         super("Attempting to define a duplicate slot name "+name+" in "+id);
      }
   }

   /**
    * Thread-safe method to create new instance of this task class.
    * NB: This method must be used in JavaScript instead of directly calling
    *     builtin constructors!
    */
   public Task newInstance () { 
      try { return isBuiltin() ? newStep(null, null, false) : new Task(this, engine); }
      catch (NoSuchMethodException e) {
         // special case for ambiguous constructors (e.g., Propose.Who)
         try { return (Decomposition.Step) new Expression(builtin, "new", 
                  new Object[]{engine}).getValue();
         } catch (RuntimeException f) { throw f; }
           catch (Exception f) { throw new RuntimeException(f); }
      }
   }

   Decomposition.Step newStep (Decomposition decomp, String step, boolean repeat)
                              throws NoSuchMethodException {
      // if id corresponds to subclass of Task, then instantiate 
      // that "builtin" class instead
      try { return (Decomposition.Step) new Expression(builtin, "new", 
               new Object[]{engine, decomp, step, repeat}).getValue();
      } catch (RuntimeException|NoSuchMethodException e) { throw e; }
        catch (Exception e) { throw new RuntimeException(e); }
   }
   
   /**
    * Test whether this task class is primitive (relative to models in current
    * task engine). A task class is primitive iff there are no known
    * decomposition classes (or a decomposition script) with this task as goal.
    * Overridden by value of @primitive, if any.
    */
   public boolean isPrimitive () { 
      Boolean property = getProperty("@primitive", (Boolean) null);
      return property != null ? property :
         getDecompositions().isEmpty() && getDecompositionScript() == null;
   }

   /**
    * Force this task class to be treated as primitive or not, regardless
    * of whether decompositions are known.  Usually used to make a task
    * class non-primitive even though decompositions not (yet) known.
    * Cannot set to primitive if decompositions are known.
    */
   public void setPrimitive (boolean primitive) {
      if ( primitive && !getDecompositions().isEmpty() ) 
         throw new UnsupportedOperationException("Cannot make primitive with known decompositions: "+this);
      setProperty("@primitive", primitive);
   }
   
   /**
    * Test whether this class can serve as root of plan recognition. Typically 
    * this is because it does not contribute to any other task classes.  However,
    * this can be overridden by the @top property.  Note primitives can never be top.
    * 
    * @see TaskEngine#getTopClasses()
    * @see #setTop(boolean)
    */
   public boolean isTop () {
      return !isPrimitive() && engine.topClasses.contains(this);
   }
   
   /**
    * @see #isTop()
    */
   public void setTop (boolean top) {
      setProperty("@top", top); // see TaskModel.setProperty(String,boolean)
   }

   /**
    * Return list of input slots, including "external".  Note "device"
    * has predefined meaning, but is not automatically an input to every task.
    * 
    * @see #getDeclaredInputs()
    */
   public List<Input> getInputs () { return Collections.unmodifiableList(inputs); }
   
   /**
    * Return list of output slots, including "success" and "when".
    * 
    * @see #getDeclaredOutputs()
    */
   public List<Output> getOutputs () { return Collections.unmodifiableList(outputs); } 

   /**
    * Return list of declared input slots.
    * 
    * @see #getInputs()
    */
   public List<Input> getDeclaredInputs () { return Collections.unmodifiableList(declaredInputs); }
      
   /**
    * Return list of declared output slots.
    * 
    * @see #getInputs()
    */
   public List<Output> getDeclaredOutputs () { return Collections.unmodifiableList(declaredOutputs); } 

   /**
    * @return corresponding input for given declared output, or null if output not modified
    */
   public Input getModifiedInput (Description.Output output) {
      for (Input input : declaredInputs)
         if ( output.equals(input.modified) ) return input;
      return null;
   }
   
   /**
    * Returns a string identifying the type of given slot name for this task
    * class, or null if no restriction.
    * 
    * @return null, "string", "boolean" or "number" or the name of a constructor
    *          defined in the current ECMAScript environment, or an expression
    *          that evaluates to a Java class object.
    */
   public String getSlotType (String name) { return slots.get(name).type; }
    
   /**
    * Tests whether given string is name of a slot of this task class.
    */
   public boolean isSlot (String name) {
      return slots.containsKey(name);
   }
   
   /**
    * Return a human-readable string describing given slot of this task class.
    * 
    * @param definite use definite ("the") form; otherwise indefinite ("a")
    */
   public String formatSlot (String name, boolean definite) {
      String property = getProperty(name,  definite ? "@definite" : "@indefinite");
      if ( property != null ) return property;
      StringBuilder buffer = new StringBuilder(
            engine.getProperty(definite ? "the@word" : "a@word"));
      buffer.append(' ');
      if ( definite ) buffer.append(this).append(' ').append(name);
      else {
         String type = getSlotType(name);
         if ( type != null ) buffer.append(Utils.decapitalize(type));
      }
      return buffer.toString();
   }
   
   public String getProperty (String slot, String key) {
      return getProperty('.'+slot+key);
   }
   
   public void setProperty (String slot, String key, String value) {
      setProperty('.'+slot+key, value);
   }
   
   public Boolean getProperty (String slot, String key, Boolean defaultValue) {
      String value = getProperty(slot, key);
      return value == null ? (Boolean) defaultValue : 
         (Boolean) Utils.parseBoolean(value);
   }
   
   // cache decomposition classes
   List<DecompositionClass> decompositions = Collections.emptyList();
         
   /**
    * Return unmodifiable list of <em>known</em> decomposition classes for this 
    * task class.
    * 
    * @see Task#getDecompositions()
    * @see Plan#getDecompositions()
    */
   public List<DecompositionClass> getDecompositions () {
      return Collections.unmodifiableList(decompositions);
   }
   
   /**
    * Return decomposition class with given id for this task class or null if none.
    */
   public DecompositionClass getDecomposition (String id) {
      for (DecompositionClass decomp : getDecompositions())
         if ( decomp.getId().equals(id) ) return decomp;
      return null;
   }

   /**
    * Return the decomposition script ("procedural decomposition") for this task
    * class or null. The script is a JavaScript expression that, when evaluated,
    * returns true if it is applicable (false otherwise) and as a side effect
    * adds subplans (and constraints) to the current value of '$plan'.
    * <p>
    * Note that if there is a decomposition script for this task class, then
    * there are not allowed to be any decomposition classes with this goal. This
    * means the script will be called in {@link Plan#decomposeAll()}.
    * <p>
    * The script is stored as the value of the '@decomposition' property of this
    * task class.
    */
   public String getDecompositionScript () {
      String script = engine.getProperty(getPropertyId()+"@decomposition");
      // check for decomposition script added later
      if ( script != null && !decompositions.isEmpty() )
         throw new IllegalStateException(this+" has both a decomposition script and class(es)");
      return script;
   }
   
   // useful for CoachedInteraction
   private Decomposition lastDecomp;
   
   /**
    * Set the last decomposition used to decompose a goal of this type
    */
   public void setLastDecomposition (Decomposition decomp) { 
      lastDecomp = decomp; }
   
   /**
    * @return the decomposition last used to decompose a goal of this type
    */
   public Decomposition getLastDecomposition () { return lastDecomp; }
   
   // for extension to plan recognition
   
   private List<TaskClass> contributes = null;
   private Set<TaskClass> explains = Collections.emptySet();
   
   /**
    * Test whether this task class can contribute to given task class.
    * Returns false if given task class is same as this task class.
    */
   public boolean isPathFrom (TaskClass task) { 
      return this == Task.Any.CLASS || task.explains.contains(Task.Any.CLASS)
            || task.explains.contains(this);   
   }

   void contributes (TaskClass task) {
      if ( task != this && (contributes == null || !contributes.contains(task)) ) {
         if ( contributes == null )
            contributes = new ArrayList<TaskClass>();
         contributes.add(task);
         if ( task.explains.isEmpty() )
            task.explains = new HashSet<TaskClass>();
         task.explains.add(this);
         task.addExplains(this, explains, new ArrayList<TaskClass>());
         if ( !getProperty("@top", false) ) // sic not isTop()
            getEngine().topClasses.remove(this);
      }
   }
   
   private void addExplains (TaskClass add, Set<TaskClass> addAll, 
                             List<TaskClass> visited) {
      if ( !visited.contains(this) ) {
         visited.add(this);
         explains.add(add);
         explains.addAll(addAll);
         if ( contributes != null )
            for (TaskClass c : contributes)
               c.addExplains(add, addAll, visited);
      }
   }
   
   // for use with CEA-2018-ext
   // a greatly simplified version of DecompositionClass.Binding
   
   // TODO does not handle dependencies between bindings
   
   private final List<Binding> bindings = new ArrayList<Binding>(4);
   
   void updateBindings (Task task) {
      for (Binding binding : bindings) binding.update(task); 
   }
   
   private class Binding {
      
      //TODO improve this by refactoring and reusing code from
      //     DecompositionClass.Binding (make separate class)
      //     to take advantage of strictness and dependency-based
      //     updating

      private final String slot, value, expression, where;
      private final Compiled compiled;
      
      private Binding (String slot, String value) {
         this.slot = slot;
         this.value = value;
         where = TaskClass.this.getId() + " binding for " + slot;
         String expression = Task.makeExpression("$this", TaskClass.this, slot, value, true);
         if ( !TaskEngine.DEBUG ) { 
            compiled = engine.compile(expression, where);
            this.expression = null;
         } else { this.expression = expression; compiled = null; }
      }
      
      private void update (Task task) {
         // calling evalConditionFinal to avoid calling Decomposition.Step.updateBindings 
         // (see Task constructor)
         if ( task.evalConditionFinal(expression, compiled, task.bindings, where) )
            task.setModified(true);
         else task.failCheck(slot, value, where);
      }
      
      @Override
      public String toString () { return slot; }
   }

}

