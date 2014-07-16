/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.cetask;

import java.beans.Expression;
import java.util.*;
import javax.xml.xpath.*;
import org.w3c.dom.*;
import edu.wpi.cetask.ScriptEngineWrapper.Compiled;

public class TaskClass extends TaskModel.Member {
  
   private final Precondition precondition;
   private final Postcondition postcondition; 
   
   public class Precondition extends Condition {
      
      private Precondition (String script, String where) {
         super(script, where, TaskClass.this.isStrict());
      }
      
      @Override
      protected boolean check (String slot) {
         if ( inputNames.contains(slot) ) return true;
         System.out.println("WARNING: $this."+slot+" not a valid input in "+where);
         return false;
      }
      
      @Override
      public TaskClass getType () { return (TaskClass) super.getType(); }
   }
   
   public class Postcondition extends Condition {
      
      private Postcondition (String script, String where) {
         super(script, where, TaskClass.this.isStrict());
      }
      
      @Override
      protected boolean check (String slot) {
         if ( inputNames.contains(slot) || outputNames.contains(slot) ) return true;
         System.out.println("WARNING: $this."+slot+" not a valid input or output in "+where);
         return false;
      }
       
      @Override
      public TaskClass getType () { return (TaskClass) super.getType(); }
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
   
   private final boolean sufficient;
   
   /**
    * Returns true iff postcondition is provided and is sufficient.
    */
   public boolean isSufficient () { return sufficient; }
   
   private Class<? extends Task> builtin;
   
   /**
    * A task class is builtin if its id is defined as a subclass of {@link Task}.
    * This allows overriding the behavior of the class.  
    * 
    * @see #newInstance()
    */
   public boolean isBuiltin () { return builtin != null; }
      
   final List<String> inputNames, outputNames, declaredInputNames, declaredOutputNames;
   
   private List<Script> scripts = Collections.emptyList();
   
   /**
    * Return all scripts associated with this primitive task class (for all platforms and
    * device types). 
    * 
    * @see #getScript()
    */
   public List<Script> getScripts () {
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
   public Script getScript () {
      List<Script> candidates = new ArrayList<Script>(getScripts());
      for (Iterator<Script> i = candidates.iterator(); i.hasNext();) {
         Script script = i.next();
         String platform = script.getPlatform(),
            deviceType = script.getDeviceType();
         // remove scripts not appropriate to current platform and deviceType
         if ( !( (platform == null || platform.equals(engine.getPlatform(this)))
                 && (deviceType == null || deviceType.equals(engine.getDeviceType(this))) ) )
            i.remove();
      }
      if ( candidates.isEmpty() ) { 
         // look for default script for model and then overall (must be toplevel)
         Script global = null;
         for (TaskModel model : engine.getModels()) {
            for (Script script : model.getScripts()) {
               if ( script.isInit() ) continue;
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
   
   private final List<String> primitiveTypes = Arrays.asList(new String[] {"boolean", "string", "number"});
   
   public abstract class Slot {
      
      protected final String name, type;
      protected final Class<?> java;
      
      public String getName () { return name; }
      public String getType () { return type; }
      public Class<?> getJava () { return java; }
      
      private Slot (String name) {
         this.name = name;
         // compute type
         if ( name.equals("success") || name.equals("external") )
            this.type = "boolean";
         else if ( name.equals("when") ) this.type = "Date";
         else {
            String path = "[@name=\""+name+"\"]/@type";
            String type = xpath("./n:input"+path+" | "+"./n:output"+path);
            // type attribute is optional (extension by CR)
            if ( type.length() == 0 ) {
               if ( !declaredInputNames.contains(name)
                     && !declaredOutputNames.contains(name) )
                  throw new IllegalArgumentException(name+" is not slot of "+this);
               this.type = null;
            } else this.type = type;
         }
          // compute java class for type, if any
         if ( type == null || primitiveTypes.contains(type) )
            this.java = null;
         else {
            Object java = null;
            try { java = engine.eval(type, "Slot constructor"); }
            // JavaScript constructor may not yet be defined since init script not evaluated yet
            catch (RuntimeException e) {}
            this.java = java instanceof Class ? (Class<?>) java : null;               
         }
      }
   }
   
   public class Input extends Slot {
      
      private final boolean optional;
      public boolean isOptional () { return optional; }

      private final Output modified;
      public Output getModified () { return modified; }
      
      private Input (String name) { 
         super(name);
         String modified = xpath("./n:input[@name=\""+name+"\"]/@modified");
         if ( modified.length() > 0 ) {
            if ( !declaredOutputNames.contains(modified) ) { 
               getErr().println("WARNING: Ignoring unknown modified output slot: "+modified);
               this.modified = null;
            } else if ( primitiveTypes.contains(type) || 
                  (java != null && !Cloneable.class.isAssignableFrom(java)) ){
               getErr().println("WARNING: Ignoring modified attribute of non-cloneable input slot: "+name);
               this.modified = null;
            } else this.modified = (Output) slots.get(modified);
         } else this.modified = null;  
         // cache optional
         this.optional = getProperty(name, "@optional", false);
      }
   }

   public class Output extends Slot {
      private Output (String name) { super(name); }
   }

   final List<Input> inputs, declaredInputs;
   final List<Output> outputs, declaredOutputs;
   
   private final Map<String,Slot> slots;
   
   public Slot getSlot (String name) { return slots.get(name); }
   
   private final boolean hasModifiedInputs;
   
   public boolean hasModifiedInputs () { return hasModifiedInputs; }
   
   @SuppressWarnings("unchecked")
   TaskClass (Node node, TaskModel model, XPath xpath) { 
      model.super(node, xpath);
      // cache simple name (suppress package for builtin classes) 
      String simple = getId();
      try { simple = Utils.getSimpleName(Class.forName(getId()), true); }
      catch (ClassNotFoundException e) {}
      simpleName = simple;
      String attribute = xpath("./n:postcondition/@sufficient"); 
      // default sufficient false
      sufficient = attribute.length() > 0 && Utils.parseBoolean(attribute);
      declaredOutputNames = xpathValues("./n:output/@name");
      declaredInputNames = xpathValues("./n:input/@name");
      if ( declaredInputNames.contains("success") || declaredOutputNames.contains("success") )
         throw new ReservedSlotException("success");
      if ( declaredInputNames.contains("external") || declaredOutputNames.contains("external") )
         throw new ReservedSlotException("external");
      outputNames =  new ArrayList<String>(declaredOutputNames);
      inputNames = new ArrayList<String>(declaredInputNames);
      outputNames.add("success"); 
      outputNames.add("when");
      inputNames.add("external");
      // to cache slot types
      slots = new HashMap<String,Slot>(inputNames.size()+outputNames.size());
      // create outputs first for modified
      declaredOutputs = new ArrayList<Output>(declaredOutputNames.size());     
      for (String name : declaredOutputNames) {
         if ( declaredOutputNames.indexOf(name) != declaredOutputNames.lastIndexOf(name) )
            throw new DuplicateSlotNameException(name);
         declaredOutputs.add(new Output(name));
      }
      outputs = new ArrayList<Output>(declaredOutputs);
      outputs.add(new Output("success")); 
      outputs.add(new Output("when"));
      for (Output output : outputs) slots.put(output.name, output);
      declaredInputs = new ArrayList<Input>(declaredInputNames.size());
      for (String name : declaredInputNames) { 
         if ( declaredInputNames.indexOf(name) != declaredInputNames.lastIndexOf(name) 
               || declaredOutputNames.contains(name) )
            throw new DuplicateSlotNameException(name);
         declaredInputs.add(new Input(name));
      }
      inputs = new ArrayList<Input>(declaredInputs);
      inputs.add(new Input("external"));
      boolean hasModifiedInputs = false;
      for (Input input : inputs) {
         slots.put(input.name, input);
         if ( input.modified != null ) {
            hasModifiedInputs = true;
            if ( !Utils.equals(input.type, input.modified.type) ) { // null types possible
               getErr().println("WARNING: Modified output slot of different type: "+input.name);
            }
         }
      }
      this.hasModifiedInputs = hasModifiedInputs;
      if ( declaredInputNames.contains("device") &&
            !"string".equals(getSlotType("device")) )
         throw new IllegalStateException("Device slot must be of type string in "
               +getId());
      // conditions after inputs and outputs for error checking
      String pre = xpath("./n:precondition"), post = xpath("./n:postcondition");
      precondition = pre.isEmpty() ? null : new Precondition(pre, simple+" precondition");
      postcondition = post.isEmpty() ? null : new Postcondition(post, simple+" postcondition");
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
      for (Node script : xpathNodes("./n:script")) {
         if ( scripts.isEmpty() ) scripts = new ArrayList<Script>(2);
         scripts.add(new Script(script, engine, this, xpath));
      }   
      try {  
         builtin = (Class<? extends Task>) Class.forName(getId());
         builtin.getDeclaredField("CLASS").set(null, this);
      } 
      catch (ClassNotFoundException|ClassCastException e) {}
      catch (NoSuchFieldException e) { throw new IllegalStateException(e); }
      catch (IllegalAccessException e) { throw new IllegalStateException(e); }
      if ( !getId().equals("**ROOT**") && getEngine().isRecognition() ) {
         if ( getProperty("@top", true) )
            // assume this can be top until find decomposition that uses step
            // see contributes
            getEngine().topClasses.add(this);
      }
   }
   
   private class ReservedSlotException extends RuntimeException {
      public ReservedSlotException (String name) {
         super("Attempting to redefine reserved slot "+name+" in "+xpath("./@id"));
      }
   }
   
   private class DuplicateSlotNameException extends RuntimeException {
      public DuplicateSlotNameException (String name) {
         super("Attempting to define a duplicate slot name "+name+" in "+xpath("./@id"));
      }
   }

   /**
    * Thread-safe method to create new instance of this task class.
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
    * Test whether this class can serve as root of plan recognition. Typically 
    * this is because they do not contribute to any other
    * task classes. However, this can be overridden by @top property in library.
    * 
    * @see TaskEngine#getTopClasses()
    */
   public boolean isTop () {
      return engine.topClasses.contains(this);
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
   public Input getModifiedInput (Output output) {
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
   
   private final String simpleName;
   
   @Override
   public String toString () {
      // for readability, suppress namespace for unambiguous id's
      try { 
         engine.getTaskClass(getId());
         return simpleName;
      } catch (TaskEngine.AmbiguousIdException e) { return '{'+getNamespace()+'}'+getId(); } 
   }

   // for extension to plan recognition
   
   private List<TaskClass> contributes = null;
   private Set<TaskClass> explains = Collections.emptySet();
   
   /**
    * Test whether this task class can contribute to given task class.
    * Returns false if given task class is same as this task class.
    */
   public boolean isPathFrom (TaskClass task) { 
      return task.explains.contains(this);   
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
         if ( !this.getProperty("@top", false) )
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
   
   // for use with extensions to CEA-2018
   // a greatly simplified version of DecompositionClass.Binding
   
   // TODO does not handle dependencies between bindings
   
   private final List<Binding> bindings = new ArrayList<Binding>(4);
   
   void updateBindings (Task task) {
      for (Binding binding : bindings) binding.update(task); 
   }
   
   private class Binding {

      private final String slot, value, expression, where;
      private final Compiled compiled;
      
      private Binding (String slot, String value) {
         this.slot = slot;
         this.value = value;
         where = TaskClass.this.getId() + " binding for " + slot;
         String expression = Task.makeExpression("$this", TaskClass.this, slot, value, true);
         if ( TaskEngine.isCompilable() ) { 
            compiled = engine.compile(expression, where);
            this.expression = null;
         } else { this.expression = expression; compiled = null; }
      }
      
      private void update (Task task) {
         // using ..Final methods to avoid calling Decomposition.Step.updateBindings 
         // (see Task constructor)
         if ( compiled != null ) {
            if ( !task.evalConditionFinal(compiled, task.bindings, where) )
               task.failCheck(slot, "compiled script", where);
            else task.setModified(true);
         } else {
            try {
               if ( !task.evalConditionFinal(expression, where) )
                  task.failCheck(slot, value, where);
               else task.setModified(true);
            } finally { task.bindings.remove("$$value"); }
         }
      }
      
      @Override
      public String toString () { return slot; }
   }

}

