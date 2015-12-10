/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.cetask;

import java.util.*;
import java.util.regex.*;
import javax.xml.xpath.*;
import org.w3c.dom.*;
import edu.wpi.cetask.ScriptEngineWrapper.Compiled;

public class DecompositionClass extends TaskModel.Member {  

   // do not confuse with JavaScript bindings in Instance
   private final Map<String,Binding> bindings = new HashMap<String,Binding>();
   
   /**
    * @return list of declared bindings of this decomposition class.
    */
   public List<Binding> getDeclaredBindings () { 
      return Collections.unmodifiableList(getBindings(null, true));
   }
   
   /**
    * @return list of bindings for slots of given step (or "this" for goal) of 
    * this decomposition class.
    */
   public List<Binding> getBindings (String step) { return getBindings(step, false); }
   
   private List<Binding> getBindings (String step, boolean declared) {
      List<Binding> result = new ArrayList<Binding>();
      for (Binding binding : bindings.values())
         if ( binding.value != null && (declared || binding.step.equals(step)) ) 
            result.add(binding);
      return result;
   }
   
   private final TaskClass goal;
   
   public TaskClass getGoal () { return goal; } 
   
   private final Applicability applicable;
   
   public static class Applicability extends Condition {
      
      public Applicability (String script, boolean strict, TaskEngine engine) {
         super(script, strict, engine);
      }
      
      @Override
      protected boolean check (String slot) {
         if ( getEnclosing().getGoal().inputNames.contains(slot) ) return true;
         System.out.println("WARNING: $this."+slot+" not a valid goal input in "+where);
         return false;
      }
       
      @Override
      public DecompositionClass getEnclosing () { return (DecompositionClass) super.getEnclosing(); }
   }
   
   /**
    * @return applicable condition of this decomposition class (or null if none defined)
    */
   public Applicability getApplicable () { return applicable; }
   
   public Boolean isApplicable (Task goal) {
      return applicable == null ? null : applicable.evalCondition(goal);
   }
   
   private final boolean ordered;
   public boolean isOrdered () { return ordered; }
   
   private final List<String> stepNames; // in order of definition
   public List<String> getStepNames () { return stepNames; }    
   
   private abstract static class Slot extends TaskClass.SlotBase {
       
      protected final TaskClass.Slot slot;
      
      protected Slot (TaskClass task, String name, Description enclosing) {
         super(name, null);
         this.slot = task.getSlot(name);         
         if ( slot == null ) throw new IllegalArgumentException("No slot in "+task+" named "+name);
         if ( this instanceof Input && !(slot instanceof Input) ) 
            throw new IllegalArgumentException("No input slot in "+task+" named "+name);
         else if ( this instanceof Output && !(slot instanceof Output) ) 
            throw new IllegalArgumentException("No output slot in "+task+" named "+name);
         setEnclosing(enclosing);
      }
      
      protected Slot (TaskClass task, TaskClass.Slot slot, Description enclosing) {
         super(slot.getName(), null);
         if ( task != slot.getEnclosing() ) throw new IllegalArgumentException(slot+" is not a slot of "+task);
         this.slot = slot;
         setEnclosing(enclosing);
      }
      
      @Override
      public String getType () { return slot.getType(); }

      @Override
      public Class<?> getJava () { return slot.getJava(); }

      @Override
      public boolean isDeclared () { return slot.isDeclared(); }
      
      protected List<DecompositionClass.Slot> fromSlots = Collections.emptyList(),
            toSlots = Collections.emptyList();
   }
    
   /* TODO  addBinding(Decomposition.Slot slot, String value) */

   private void addBinding (DecompositionClass.Slot from, DecompositionClass.Slot to) {
      if ( from.toSlots.isEmpty() ) from.toSlots = new ArrayList<Slot>(3);
      if ( to.fromSlots.isEmpty() ) to.fromSlots = new ArrayList<Slot>(3);
      from.toSlots.add(to);
      to.fromSlots.add(from);
   }
 
   private abstract static class GoalSlot extends DecompositionClass.Slot {
      
      protected GoalSlot (String name, DecompositionClass enclosing) {
         super(enclosing.getGoal(), name, enclosing);
      }

      protected GoalSlot (TaskClass.Slot slot, DecompositionClass enclosing) { 
         super(enclosing.getGoal(), slot, enclosing); 
      }
      
      @Override
      public DecompositionClass getEnclosing () {
         return (DecompositionClass) super.getEnclosing();
      }
   }
  
   public static class Input extends GoalSlot implements Description.Input {
      
      public Input (String name, DecompositionClass enclosing) {
         super(name, enclosing);
          if ( !enclosing.getGoal().inputNames.contains(name) )
            throw new IllegalArgumentException(name+" is not an input of "+enclosing.getGoal());
      }

      public Input (TaskClass.Input input, DecompositionClass enclosing) { super(input, enclosing); }

      @Override
      public boolean isOptional () { return ((Input) slot).isOptional(); }

      @Override
      public Output getModified () { return ((Input) slot).getModified(); }
      
      /**
       * @return slots in the definition of this decomposition class that this goal input
       *         has dataflow to (usually inputs of steps, but may also be an output of goal)
       */
      public List<DecompositionClass.Slot> to () { return toSlots; }
   }
   
   public static class Output extends GoalSlot implements Description.Output {
      
      public Output (String name, DecompositionClass enclosing) {
         super(name, enclosing);
         if ( !enclosing.getGoal().outputNames.contains(name) )
            throw new IllegalArgumentException(name+" is not an output of "+enclosing.getGoal());
      }

      public Output (TaskClass.Output output, DecompositionClass enclosing) { super(output, enclosing); }
      
      /**
       * @return slots in the definition of this decomposition class that this goal output
       *         has dataflow from (usually outputs of steps, but may also be an input of goal)
       */
      public List<DecompositionClass.Slot> from () { return toSlots; }
   }
    
   private final Map<String,GoalSlot> slots;
   
   @Override
   public GoalSlot getSlot (String name) { return slots.get(name); }
   
   /**
    * @return the goal slots for this decomposition class
    */
   public Collection<GoalSlot> getSlots () { return Collections.unmodifiableCollection(slots.values()); }

   private final List<Input> inputs;
   private final List<Output> outputs;
   
   public List<Input> getInputs () { return Collections.unmodifiableList(inputs); }
   public List<Output> getOutputs ()  { return Collections.unmodifiableList(outputs); }
   
   public static class Step extends Description {
      
      private final String name;
      private final TaskClass type;
      private final int minOccurs, maxOccurs;
      private final List<String> required;
      
      public String getName () { return name; }
      public TaskClass getType () { return type; }
      public int getMinOccurs () { return minOccurs; }
      public int getMaxOccurs () { return maxOccurs; }
      public List<String> getRequired() { return required; }
          
      private final Map<String,Step.Slot> slots;
      private final List<Input> inputs;
      private final List<Output> outputs;
      
      public List<Input> getInputs () { return Collections.unmodifiableList(inputs); }
      public List<Output> getOutputs () { return Collections.unmodifiableList(outputs); }
   
      public Step.Slot getSlot (String name) { return slots.get(name); }
      
      public Collection<Step.Slot> getSlots () { return Collections.unmodifiableCollection(slots.values()); }
      
      // TODO provide copy constructor
      public Step (String name, TaskClass type, int minOccurs, int maxOccurs, 
            List<String> required) {
         this(name, type, minOccurs, maxOccurs, required, null);
      }
      
      // TODO make required be list of Step's
      public Step (String name, TaskClass type, int minOccurs, int maxOccurs, 
            List<String> required, DecompositionClass enclosing) {
         super(null, null);
         this.name = name;
         this.type = type;
         this.minOccurs = minOccurs;
         this.maxOccurs = maxOccurs;
         this.required = required;
         setEnclosing(enclosing);
         slots = new HashMap<String,Slot>(type.inputNames.size()+type.outputNames.size());
         inputs = type.inputNames.isEmpty() ? Collections.<Input>emptyList() :
            new ArrayList<Input>(type.inputNames.size());
         outputs = type.outputNames.isEmpty() ? Collections.<Output>emptyList() :
            new ArrayList<Output>(type.outputNames.size());
         for (TaskClass.Slot slot : type.getSlots()) 
            slots.put(slot.getName(), 
                  slot instanceof TaskClass.Input ? new Input(slot.getName(), this) :
                       new Output(slot.getName(), this));
      }
      
      @Override
      public DecompositionClass getEnclosing () { 
         return (DecompositionClass) super.getEnclosing(); 
      }
      
      public Step (String name, TaskClass type) {
         this(name, type, 1, 1, null, null);
      }
      
      @Override
      public String toString () { 
         return getEnclosing() == null ? name : getEnclosing().toString()+'.'+name; 
      }
      
      private abstract static class Slot extends DecompositionClass.Slot {

         protected Slot (String name, Step enclosing) {
            this(enclosing.getType().getSlot(name), enclosing);
         }

         protected Slot (TaskClass.Slot slot, Step enclosing) { 
            super(enclosing.getType(), slot, enclosing); }

         @Override
         public Step getEnclosing () {
            return (Step) super.getEnclosing();
         }
      }
      
      public class Input extends Slot implements Description.Input {
         
         public Input (String name, Step enclosing) {
            super(name, enclosing);
            if ( !enclosing.getType().inputNames.contains(name) )
            throw new IllegalArgumentException(name+" is not an input of "+enclosing.getType());
         }

         public Input (TaskClass.Input input, Step enclosing) { super(input, enclosing); } 
         
         @Override
         public boolean isOptional () { return ((Input) slot).isOptional(); }

         @Override
         public Output getModified () { return ((Input) slot).getModified(); }
         
         /**
          * @return slots in the definition of this decomposition class that this step input
          *         has dataflow from (usually outputs of steps, but may also be an input of goal)
          */
         public List<DecompositionClass.Slot> from () { return fromSlots; }
      }
      
      public class Output extends Slot implements Description.Output {
         
         public Output (String name, Step enclosing) { 
            super(name, enclosing);
            if ( !enclosing.getType().outputNames.contains(name) )
               throw new IllegalArgumentException(name+" is not an output of "+enclosing.getType());
         }
         
         public Output (TaskClass.Output output, Step enclosing) { super(output, enclosing); }

           
         /**
          * @return slots in the definition of this decomposition class that this step output
          *         has dataflow to (usually inputs of steps, but may also be an output of goal)
          */
         public List<DecompositionClass.Slot> to () { return toSlots; }
      }
   }

   private final Map<String,Step> steps;
   
   public Step getStep (String name) { return steps.get(name); }
   
   /**
    * Return type of given step.
    */
   public TaskClass getStepType (String name) { return steps.get(name).type; }
   
   /**
    * Test whether named step in this decomposition class is optional.
    */
   public boolean isOptionalStep (String name) {
      // note this pertains to the _first_ step of repeated steps only
      return steps.get(name).minOccurs < 1;
   }

   /**
    * Return the maximum number of times named step in this decomposition
    * class may be repeated, or -1 if unbounded.
    */
   public int getMaxOccursStep (String name) { return steps.get(name).maxOccurs; }
   
    /**
    * Return the minimum number of times named step in this decomposition
    * class must occur.
    */
   public int getMinOccursStep (String name) { return steps.get(name).minOccurs; }
   
   public List<String> getRequiredStepNames (String name) {
      List<String> required = steps.get(name).required;
      if ( required == null ) return Collections.emptyList(); else return required;
   }
   
   /**
    * Maximum allowed depth of dependency chain in bindings (default 10). Used
    * to protect against cyclic dependencies.
    */
   public static int MAX_REQUIRES_DEPTH = 25;
   
   /**
    * Test whether step1 requires step2 <em>transitively</em>.
    */
   private boolean isRequired (String step1, String step2, int depth) {
      if (depth > MAX_REQUIRES_DEPTH) 
         throw new RuntimeException(getId()+" requires stopped at depth "+ depth
                  +" (probably circular requires)");
      List<String> required = getRequiredStepNames(step1);
      if ( required.contains(step2) ) return true;
      for (String step : required)
         if ( isRequired(step, step2, depth+1) ) return true;
      return false;
   }

   DecompositionClass (Node node, XPath xpath, TaskModel model, TaskClass goal) { 
      this(node, xpath, model, 
            TaskModel.parseId(node, xpath), 
            goal == null ? parseGoal(node, xpath, model.getEngine()) : goal,
            parseSteps(node, xpath, model.getEngine()),
            parseApplicable(node, xpath, model.getEngine()),
            parseOrdered(node, xpath));
      for (Step step : steps.values()) step.setEnclosing(this);
   }
   
   private static TaskClass parseGoal (Node node, XPath xpath, TaskEngine engine) {
      String qname = xpath(node, xpath, "./@goal");
      return qname.isEmpty() ? null : resolveTaskClass(node, parseAbout(node, xpath), qname, engine);
   }
   
   private static List<Step> parseSteps (Node node, XPath xpath, TaskEngine engine) {
      String id = TaskModel.parseId(node, xpath);
      List<String> stepNames = xpathValues(node, xpath, "./n:step/@name");
      int size = stepNames.size();
      List<Step> steps = new ArrayList<Step>(size);
      List<String> previousStepNames = null;
      boolean ordered = parseOrdered(node, xpath);
      if ( ordered ) previousStepNames = new ArrayList<String>(size);
      for (String name : stepNames) {
         if ( stepNames.indexOf(name) != stepNames.lastIndexOf(name) )
            throw new RuntimeException(
                  "Attempting to define duplicate step name "+name+" in "+id);
         List<String> required = null;
         String requires = xpath(node, xpath, "./n:step[@name=\""+name+"\"]/@requires");
         if ( ordered ) { 
            required = new ArrayList<String>(previousStepNames);
            previousStepNames.add(name);
            if ( requires.length() > 0 )
               engine.getErr().println("WARNING: Ignoring explicit requires in ordered decomposition "+id);
         } else if ( requires.length() > 0) {
            required = new ArrayList<String>(size-1);
            StringTokenizer tokenizer = new StringTokenizer(requires);
            while (tokenizer.hasMoreTokens()) required.add(tokenizer.nextToken());
         } 
         String minOccurs = xpath(node, xpath, "./n:step[@name=\""+name+"\"]/@minOccurs");
         int min = minOccurs.length() == 0 ? 1 : Integer.parseInt(minOccurs);
         String maxOccurs = xpath(node, xpath, "./n:step[@name=\""+name+"\"]/@maxOccurs");
         int max = maxOccurs.length() == 0 ? ( min == 0 ? 1 : min ) : 
                              maxOccurs.equals("unbounded") ? -1 :
                              Integer.parseInt(maxOccurs);
         if ( max >= 0 && max < min ) {
            engine.getErr().println("WARNING: Ignoring maxOccurs in "+id+" step "+name);
            max = min;
         }
         String task = xpath(node, xpath, "./n:step[@name=\""+name+"\"]/@task");
         TaskClass taskClass = task.isEmpty()? Task.Any.CLASS :
            resolveTaskClass(node, parseAbout(node, xpath), task, engine);
         if ( taskClass == null )
            throw new RuntimeException("Step "+name+" in "+id+" has unknown task type: "+task);
         steps.add(new Step(name, taskClass, min, max, required));
      }
      return steps;
   }
   
   private static Applicability parseApplicable (Node node, XPath xpath, TaskEngine engine) {
      String script = xpath(node, xpath, "./n:applicable");
      return script.isEmpty() ? null : 
         new Applicability(script, Condition.isStrict(engine, TaskModel.parseId(node, xpath)), engine);
   }
   
   private static boolean parseOrdered (Node node, XPath xpath) {
      String ordered = xpath(node, xpath, "./@ordered");
      return ordered.length() == 0 || Utils.parseBoolean(ordered);
   }
      
   /**
    * Limited, incomplete constructor for decomposition classes without using XML.
    * Provided to support LIMSI Discolog project.
    * 
    * Note if there are ordering constraints ('requires') between steps, they must be specified in Step objects.
    */
   public DecompositionClass (TaskModel model, String id, TaskClass goal, List<Step> steps, Applicability applicable) { 
      this(null, null, model, id, goal, steps, applicable, false);
   }
   
   private DecompositionClass (Node node, XPath xpath, TaskModel model, 
         String id, TaskClass goal, List<Step> steps, Applicability applicable, boolean ordered) { 
      model.super(node, xpath, id);
      model.decomps.put(id, this);
      this.goal = goal;
      slots = new HashMap<String,GoalSlot>(goal.inputNames.size()+goal.outputNames.size());
      for (TaskClass.Slot slot : goal.getSlots()) 
         slots.put(slot.getName(), 
               slot instanceof TaskClass.Input ? new Input(slot.getName(), this) :
                    new Output(slot.getName(), this));
      inputs = goal.inputNames.isEmpty() ? Collections.<Input>emptyList() :
            new ArrayList<Input>(goal.inputNames.size());
      outputs = goal.outputNames.isEmpty() ? Collections.<Output>emptyList() :
            new ArrayList<Output>(goal.outputNames.size());
      this.steps = new HashMap<String,Step>(steps.size());
      this.applicable = applicable;
      if ( applicable != null ) applicable.setEnclosing(this);
      this.ordered = ordered;
      stepNames = new ArrayList<String>(steps.size());
      for (Step step : steps) { 
         stepNames.add(step.name);
         this.steps.put(step.name, step);
      }
      if ( !this.goal.getGroundingAll().isEmpty() )
         throw new RuntimeException("Task "+this.goal+" cannot have both grounding script and subtasks!");
      if ( this.goal.decompositions.isEmpty() ) 
         this.goal.decompositions = new ArrayList<DecompositionClass>(5);
      this.goal.decompositions.add(this);
      this.goal.getDecompositionScript(); // for error check
      // check for cycles
      for (String name : stepNames) isRequired(name, null, 0);
      if ( getEngine().isRecognition() ) 
         for (String name : stepNames) getStepType(name).contributes(this.goal);
      if ( node != null ) { // temporary check
         // analyze and store binding dependencies
         // TODO: Check type compatibility between slots
         NodeList nodes = (NodeList) xpath("./n:binding", XPathConstants.NODESET);
         try { 
            synchronized (bindings) {
               for (int i = 0; i < nodes.getLength(); i++) { // preserve order
                  Node bindingNode = nodes.item(i);
                  String variable = xpath.evaluate("./@slot", bindingNode); 
                  if ( bindings.get(variable) != null )
                     throw new TaskModel.Error(this,
                           "duplicate bindings for "+variable);
                  bindings.put(variable, new Binding(variable, bindingNode));
               }
               for (Binding binding : bindings.values()) {
                  // TODO Should do proper JavaScript tokenizing here instead of just
                  // looking for occurrences of $step.slot pattern, e.g., to not get fooled by constants
                  // inside quotations. (Use of this pattern for identity binding detection is ok.)
                  Matcher matcher = pattern.matcher(binding.value);
                  while ( matcher.find() ) {
                     String dependVariable = matcher.group();
                     Binding depend = bindings.get(dependVariable);
                     if ( depend == null ) {
                        if ( binding.identity == true )
                           // no declared binding for this slot, so create dummy one for propagation
                           addDepend(binding, new Binding(binding.value, null));
                        else if ( dependVariable.startsWith("$this.") )
                           // special case for value expression involving $this
                           addDepend(binding, new Binding(dependVariable, null));
                     } else addDepend(binding, depend);
                  }
               }
            }
         } catch (Exception e) { Utils.rethrow(e); } 
      }
   }
   
   private static void addDepend (Binding binding, Binding depend) {
      String variable = binding.variable;
      for (Binding current : binding.depends) 
         if ( current.variable.equals(variable) ) return; // eliminate duplicates
      binding.depends.add(depend);
   }
   
   /**
    * Test whether this decomposition class has been rejected for given task;
    */
   public boolean isRejected (Task task) { 
      return task.getRejected().contains(this);
   }
   
   List<String> liveStepNames;
   
   /**
    * @return list of step names which are live when decomposition
    *         first instantiated (for plan recognition extension).
    */
   public List<String> getLiveStepNames () { return liveStepNames; }
      
   void updateBindings (Decomposition decomp, String onlyStep, String retractedStep, String retractedSlot) {
      for (Binding binding : bindings.values()) 
         // identity binding cannot have other dependencies
         if ( onlyStep == null || (onlyStep.equals(binding.step) && !binding.identity) )
            binding.update(decomp, 0, retractedStep, retractedSlot);
   }
      
   /**
    * Tests whether given input of goal is used in any <em>identity</em> bindings of
    * this decomposition class.
    */
   public boolean hasBinding (String input) {
      String variable = "$this."+input;
      for (Binding binding : bindings.values())
         for (Binding depend : binding.depends)
            if ( depend.variable.equals(variable) ) return true;
      return false;
   }
    
   @Override
   public String toString () { return getId(); }
   
   public String format () {
      String format = getProperty("@format");
      return format != null ? format : engine.getProperty("by@word")+' '+getId();
   }

   /**
    * Maximum allowed depth of dependency chain in bindings (default 10). Used
    * to protect against cyclic dependencies.
    */
   public static int MAX_BINDING_DEPTH = 10; 
   
   /**
    * The first four of these are identity bindings. Note that INPUT_OUTPUT is an uncommonly
    * used "through" binding only for goal.
    */
   public static enum BindingType { INPUT_INPUT, OUTPUT_INPUT, OUTPUT_OUTPUT, INPUT_OUTPUT, 
                                    NON_IDENTITY }
   private final static Pattern pattern = // to match $var.slot
         Pattern.compile("\\$\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*");      
         
   public class Binding {
      
      // since these are final, ok to make public
      
      public final String variable, value, step, slot, identityStep, identitySlot;
      public final BindingType type;
      public final boolean identity;
      public final TaskClass stepType; 
      public final Slot from, to;
            
      // bindings upon which this binding depends
      private final List<Binding> depends = new ArrayList<Binding>(); 
      
      /**
       * @return list of bindings upon which the value attribute of this binding depends.
       * Note this can include undeclared bindings with null values.
       */
      public List<Binding> getDepends () { 
         return Collections.unmodifiableList(depends);
      }
      
      private final String expression, where;
      private final Compiled compiled;
      
      private Binding (String variable, Node node) 
            throws XPathExpressionException {
         this.variable = variable;  // $step.slot
         where = DecompositionClass.this + " binding for " + variable;
         StringTokenizer tokenizer = new StringTokenizer(
               variable.substring(1), ".");
         step = tokenizer.nextToken();
         boolean inputInput, outputInput;
         if ( !(step.equals("this") || getStepNames().contains(step)) )
            throw new TaskModel.Error(DecompositionClass.this,
                  "binding slot \""+variable+
                  "\" does not refer to any step of subtasks");
         stepType = getTaskType(step);
         slot = tokenizer.nextToken();
         if ( !stepType.isSlot(slot) )
            throw new TaskModel.Error(DecompositionClass.this,
                  "binding slot \""+variable+
                  "\" does not refer to any slot of subtasks");
         if ( node == null ) { 
            value = identityStep = identitySlot = null; 
            expression = null; compiled = null; 
            identity = inputInput = outputInput = false; 
         } else {
            value = xpath.evaluate("./@value", node); // keep value for dependency analysis
            boolean matches = pattern.matcher(value).matches();
            if ( matches ) {               
               expression = null; compiled = null;
               tokenizer = new StringTokenizer(value.substring(1), ".");
               identityStep = tokenizer.nextToken();
               if ( !identityStep.equals("this") && getStepType(identityStep) == Task.Any.CLASS ) { 
                  identity = inputInput = outputInput = false;  // non-identity
                  identitySlot = null;
               } else {
                  identity = matches;
                  identitySlot = tokenizer.nextToken();
                  inputInput = value.startsWith("$this.") &&
                        !step.equals("this") &&
                        getGoal().inputNames.contains(value.substring(6)) &&
                        getStepType(step).inputNames.contains(slot);
                  outputInput = !(step.equals("this") || identityStep.equals("this")); 
                  if ( outputInput ) { 
                     // make sure compatible with ordering constraints
                     if ( !isRequired(step, identityStep, 0) ) 
                        getErr().println("WARNING: "+getId()+" contains binding that needs step "+step+" to require step "+identityStep);
                  }
               }
            } else { 
               identity = inputInput = outputInput = false;
               identityStep = identitySlot = null;
               if ( !TaskEngine.DEBUG ) { 
                  compiled = engine.compile(
                        Task.makeExpression("$$this", getTaskType(step), slot, value, true),
                        where);
                  this.expression = null; 
               } else { this.expression = value; compiled = null; }
            }
         }
         type = inputInput ? BindingType.INPUT_INPUT : 
            outputInput ? BindingType.OUTPUT_INPUT : 
               identity ?
                  ("this".equals(identityStep) ? BindingType.INPUT_OUTPUT : BindingType.OUTPUT_OUTPUT) :
                  BindingType.NON_IDENTITY;
         switch (type) {
            case INPUT_INPUT:
               from = getSlot(identitySlot); 
               to = getStep(step).getSlot(slot);
               break;
            case OUTPUT_INPUT:
               from = getStep(identityStep).getSlot(identitySlot);
               to = getStep(step).getSlot(slot);
               break;
            case INPUT_OUTPUT:
               from = getSlot(identitySlot);
               to = getSlot(slot);
               break;
            case OUTPUT_OUTPUT:
               from = getStep(identityStep).getSlot(identitySlot);
               to = getSlot(slot);
               break;
            default: // NON_IDENTITY
               from = null;
               to = step.equals("this") ? getSlot(slot) : getStep(step).getSlot(slot);
         }
         if ( type != BindingType.NON_IDENTITY ) addBinding(from, to);
      }

      /* DESIGN NOTE: Self-referring bindings for default values and inverse bindings
      
         As a special case, a binding that refers to its slot attribute in its
         value attribute is allowed without a circularity warning in order to 
         support two useful features:
         
         (1) Default values, e.g.,
         
             <binding slot="$this.slot1" value="$this.slot1 == undefined ? 5 : undefined"/>
             
         (2) Inverse bindings, e.g.,
         
             <binding slot="$step1.slot1" value="f($this.input)"/>
             <binding slot="$this.input" value="g($step1.slot1)"/>
             
         The second binding above uses the inverse function g to propagate
         a value "up" from plan recognition.  This is taking one step closer 
         to full constraint propagation.
      */
      
      // Note this implements only monotonic propagation, not full
      // dependency-based constraint propagation
      private void update (Decomposition decomp, int depth, 
                           String retractedStep, String retractedSlot) {
         Task target = getTask(decomp, step); 
         if ( target.isOccurred() ) return; // never update slot after occurrence
         if ( depth > MAX_BINDING_DEPTH )
            throw new IllegalStateException(where + " stopped at depth "+ depth
                  +" (probably circular bindings)");
         depth++;
         if ( "external".equals(slot) && !stepType.isPrimitive() )
            getErr().println("WARNING: "+getId()+" ignoring external binding of non-primitive task "+variable); 
         boolean self = false;
         for (Binding depend : depends) 
            if ( equals(depend) ) { self = true; break; }
         // if binding refers to itself, we have a default value or inverse binding
         // so we ignore dependencies to avoid circularity
         if ( !self ) 
            for (Binding depend : depends) 
               depend.update(decomp, depth, retractedStep, retractedSlot); // recursive
         for (Binding depend : depends) {
            Task dependTask = getTask(decomp, depend.step); 
            if ( dependTask == null )
               throw new TaskModel.Error(DecompositionClass.this,
                     "Unknown slot "+depend.variable+" in binding for "+variable);
            if ( !equals(depend) && !dependTask.isDefinedSlot(depend.slot) ) {
               // don't try to compute binding that depends on undefined
               // variables (except for self-referring), since cannot guarantee that 
               // value expression will be safe (strict)
               if ( depend.step.equals(retractedStep) && depend.slot.equals(retractedSlot) ) {
                  // special case for retracted slot: propagate undefined to target
                  target.removeSlotValue(slot);
                  updateBindings(decomp, target, step, slot);
               }
               if ( type == BindingType.INPUT_INPUT && target.isDefinedSlot(slot)
                     // don't undo retraction in progress
                     && !depend.step.equals(retractedStep) && !depend.slot.equals(retractedSlot) ) { 
                  // special case for input-input identity only: propagate value other direction 
                  dependTask.copySlotValue(target, slot, depend.slot, true, false);
                  updateBindings(decomp, dependTask, null, null);
               }
               return; // NB return here
            } else if ( type == BindingType.INPUT_INPUT && step.equals(retractedStep) && slot.equals(retractedSlot) ) {
               // special case for input-input identity only: propagate retraction other direction
               decomp.getGoal().removeSlotValue(depend.slot);
               updateBindings(decomp, decomp.getGoal(), depend.step, depend.slot);
            } else if ( !TaskEngine.DEBUG // allow looking at values for debugging 
                  && identity && target.isDefinedSlot(slot)  
                  && !Utils.equals(target.getSlotValue(slot), 
                        dependTask.getSlotValue(depend.slot)))
               throw new Contradiction(
                     this+"=\""+engine.toString(target.getSlotValue(slot))+"\" and "+
                           depend+"=\""+engine.toString(dependTask.getSlotValue(depend.slot))+
                           "\" in "+DecompositionClass.this);
         }
         if ( value != null ) {
            if ( identity) 
               target.copySlotValue(getTask(decomp, identityStep), identitySlot, slot,
                     true, false); // onlyDefined true, i.e., do not propagate undefined
            else if ( compiled != null )
               target.setSlotValueScript(slot, compiled, where, decomp.bindings);
            else target.setSlotValueScript(slot, expression, where, decomp.bindings);
            updateBindings(decomp, target, null, null);
         }
      }
      
      private void updateBindings (Decomposition decomp, Task task, String retractedStep, String retractedSlot) {
         if ( !(task instanceof Decomposition.Step) ) // if not already done 
            decomp.updateBindings(true, null, retractedStep, retractedSlot);
      }
      
      // NB: if step is "this" may not return instance of Step
      // which is reason for updateBindings method above
      private Task getTask (Decomposition decomp, String step) {
         return step.equals("this") ? decomp.getGoal() :
            decomp.getStep(step).getGoal(); 
      }
     
      private TaskClass getTaskType (String step) {
         return step.equals("this") ? getGoal() : getStepType(step);
      }
      
      @Override
      public int hashCode () {
         final int prime = 31;
         int result = 1;
         result = prime * result + getOuterType().hashCode();
         result = prime * result + ((slot == null) ? 0 : slot.hashCode());
         result = prime * result + ((step == null) ? 0 : step.hashCode());
         return result;
      }

      @Override
      public boolean equals (Object obj) {
         if ( this == obj ) return true;
         if ( obj == null ) return false;
         if ( getClass() != obj.getClass() ) return false;
         Binding other = (Binding) obj;
         if ( !getOuterType().equals(other.getOuterType()) ) return false;
         if ( slot == null ) {
            if ( other.slot != null )
               return false;
         } else if ( !slot.equals(other.slot) )
            return false;
         if ( step == null ) {
            if ( other.step != null )
               return false;
         } else if ( !step.equals(other.step) )
            return false;
         return true;
      }

      @Override
      public String toString () { return variable; }
      
      private DecompositionClass getOuterType () {
         return DecompositionClass.this;
      }      
   }

   public class Contradiction extends RuntimeException {
       public Contradiction (String error) { 
          super(DecompositionClass.this+": Contradiction involving "+error);
       }
    }
  
}

