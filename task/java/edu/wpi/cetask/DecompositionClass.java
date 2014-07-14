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
   
   // TODO check for duplicate step names
   
   // do not confuse with JavaScript bindings in Instance
   private final Map<String,Binding> bindings = new HashMap<String,Binding>();
   
   /**
    * @return the bindings of this decomposition class.
    */
   public Map<String,Binding> getBindings () {
      return Collections.unmodifiableMap(bindings);
   }
   
   private final static Pattern pattern = // to match $var.slot
      Pattern.compile("\\$\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*");      

   private final TaskClass goal;
   
   public TaskClass getGoal () { return goal; } 
   
   private final Applicability applicable;
   
   public class Applicability extends Condition {
      
      private Applicability (String script, String where) {
         super(script, where, goal.isStrict());
      }
      
      @Override
      protected boolean check (String slot) {
         if ( goal.getInputNames().contains(slot) ) return true;
         System.out.println("WARNING: $this."+slot+" not a valid goal input in "+where);
         return false;
      }
      
      @Override
      public DecompositionClass getType () { return (DecompositionClass) super.getType(); }
   }
   
   /**
    * @return applicable condition of this task class (or null if none defined)
    */
   public Applicability getApplicable () { return applicable; }
   
   public Boolean isApplicable (Task goal) {
      return applicable == null ? null : applicable.eval(goal);
   }
   
   private final boolean ordered;
   public boolean isOrdered () { return ordered; }
   
   private final List<String> stepNames; // in order of definition
   public List<String> getStepNames () { return stepNames; }    
   
   private static class Step {
      private final TaskClass type;
      private final int minOccurs, maxOccurs;
      private final List<String> required;
      
      private Step (TaskClass type, int minOccurs, int maxOccurs, List<String> required) {
         this.type = type;
         this.minOccurs = minOccurs;
         this.maxOccurs = maxOccurs;
         this.required = required;
      }
   }

   private final Map<String,Step> steps;

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
                  +" (probably circular)");
      List<String> required = getRequiredStepNames(step1);
      if ( required.contains(step2) ) return true;
      for (String step : required)
         if ( isRequired(step, step2, depth+1) ) return true;
      return false;
   }
   
   DecompositionClass (Node node, TaskModel model, TaskClass goal, XPath xpath) { 
      model.super(node, xpath);
      model.decomps.put(getId(), this);
      this.goal = goal == null ? resolveTaskClass(xpath("./@goal")) : goal;
      if ( !this.goal.getScripts().isEmpty() )
         throw new RuntimeException("Task "+this.goal+" cannot have both script and subtasks!");
      if ( this.goal.decompositions.isEmpty() ) 
         this.goal.decompositions = new ArrayList<DecompositionClass>(5);
      this.goal.decompositions.add(this);
      this.goal.getDecompositionScript(); // for error check
      // cache ordered (default true)
      String ordered = xpath("./@ordered");
      this.ordered =  ordered.length() == 0 || Utils.parseBoolean(ordered);
      stepNames = xpathValues("./n:step/@name");
      int size = stepNames.size();
      steps = new HashMap<String,Step>(size);
      List<String> previousStepNames = null;
      if ( isOrdered() ) previousStepNames = new ArrayList<String>(size);
      for (String name : stepNames) {
         if ( stepNames.indexOf(name) != stepNames.lastIndexOf(name) )
            throw new RuntimeException(
                  "Attempting to define duplicate step name "+name
                  +" in "+getId());
         List<String> required = null;
         String requires = xpath("./n:step[@name=\""+name+"\"]/@requires");
         if ( isOrdered() ) { 
            required = new ArrayList<String>(previousStepNames);
            previousStepNames.add(name);
            if ( requires.length() > 0 )
               getErr().println("WARNING: Ignoring explicit requires in ordered decomposition "+getId());
         } else if ( requires.length() > 0) {
            required = new ArrayList<String>(size-1);
            StringTokenizer tokenizer = new StringTokenizer(requires);
            while (tokenizer.hasMoreTokens()) required.add(tokenizer.nextToken());
         } 
         String minOccurs = xpath("./n:step[@name=\""+name+"\"]/@minOccurs");
         int min = minOccurs.length() == 0 ? 1 : Integer.parseInt(minOccurs);
         String maxOccurs = xpath("./n:step[@name=\""+name+"\"]/@maxOccurs");
         int max = maxOccurs.length() == 0 ? ( min == 0 ? 1 : min ) : 
                              maxOccurs.equals("unbounded") ? -1 :
                              Integer.parseInt(maxOccurs);
         if ( max < min ) {
            getErr().println("WARNING: Ignoring maxOccurs in "+getId()+" step "+name);
            max = min;
         }
         TaskClass task = resolveTaskClass(xpath("./n:step[@name=\""+name+"\"]/@task"));
         if ( task == null )
            throw new RuntimeException(
                  "Step "+name+" in "+getId()+" has unknown task type");
         steps.put(name, new Step(task, min, max, required));
      }
      // check for cycles
      for (String name : stepNames) isRequired(name, null, 0);
      if ( getEngine().isRecognition() ) 
         for (String name : stepNames) getStepType(name).contributes(this.goal);
      // analyze and store binding dependencies
      NodeList nodes = (NodeList) xpath("./n:binding", XPathConstants.NODESET);
      // TODO: Check type compatibility between slots
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
               Matcher matcher = pattern.matcher(binding.value);
               while ( matcher.find() ) {
                  String dependValue = matcher.group();
                  Binding depend = bindings.get(dependValue);
                  if ( depend == null ) {
                     if ( binding.identity == true )
                        // special case for identity w/o next level binding
                        binding.depends.add(new Binding(binding.value, null));
                     else if ( dependValue.startsWith("$this.") )
                        // special case for value expression involving $this
                        binding.depends.add(new Binding(dependValue, null));
                  } else binding.depends.add(depend);
               }
            }
         }
      } catch (Exception e) { Utils.rethrow(e); } 
      String script = xpath("./n:applicable");
      applicable = script.isEmpty() ? null : new Applicability(script, this+" applicable");
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
   
   public class Binding {

      // since these are final, ok to make public
      public final String variable, value, 
                          step, slot, identityStep, identitySlot;
      
      public final TaskClass type; // step type
      
      public final boolean identity, inputInput, outputInput;
            
      // bindings upon which this binding depends
      private final List<Binding> depends = new ArrayList<Binding>();     
      
      private final String expression, where;
      private final Compiled compiled;
      
      private Binding (String variable, Node node) 
            throws XPathExpressionException {
         this.variable = variable;  // $step.slot
         where = DecompositionClass.this + " binding for " + variable;
         StringTokenizer tokenizer = new StringTokenizer(
               variable.substring(1), ".");
         step = tokenizer.nextToken();
         if ( !(step.equals("this") || getStepNames().contains(step)) )
            throw new TaskModel.Error(DecompositionClass.this,
                  "binding slot \""+variable+
                  "\" does not refer to any step of subtasks");
         type = getTaskType(step);
         slot = tokenizer.nextToken();
         if ( !type.isSlot(slot) )
            throw new TaskModel.Error(DecompositionClass.this,
                  "binding slot \""+variable+
                  "\" does not refer to any slot of subtasks");
         if ( node == null ) { 
            value = identityStep = identitySlot = null; 
            expression = null; compiled = null; 
            identity = inputInput = outputInput = false; 
         } else {
            value = xpath.evaluate("./@value", node); // keep value for dependency analysis
            identity = pattern.matcher(value).matches();
            if ( identity ) {               
               tokenizer = new StringTokenizer(value.substring(1), ".");
               identityStep = tokenizer.nextToken();
               identitySlot = tokenizer.nextToken();
               expression = null; compiled = null;
               inputInput = value.startsWith("$this.") &&
                     !step.equals("this") &&
                     getGoal().getInputNames().contains(value.substring(6)) &&
                     getStepType(step).getInputNames().contains(slot);
               outputInput = !(step.equals("this") || identityStep.equals("this")); 
               if ( outputInput ) { 
                  // if dataflow (output to input) make sure that compatible
                  // with ordering constraints
                  if ( !isRequired(step, identityStep, 0) ) 
                     getErr().println("WARNING: "+getId()+" contains binding that needs step "+step+" to require step "+identityStep);
               }
            } else { 
               inputInput = outputInput = false;
               identityStep = identitySlot = null;
               String expression = Task.makeExpression("$$this",
                     getTaskType(step), slot, value, true);
               if ( TaskEngine.isCompilable() ) { 
                  compiled = engine.compile(expression, where);
                  this.expression = null; 
               } else { this.expression = expression; compiled = null; }
            }
         }
      }

      // Note this implements only monotonic propagation, not full
      // dependency-based constraint propagation
      private void update (Decomposition decomp, int depth, 
                           String retractedStep, String retractedSlot) {
         Task target = getTask(decomp, step);
         if ( target.occurred() ) return; // never update slot after occurrence
         if ( depth > MAX_BINDING_DEPTH )
            throw new IllegalStateException(where + " stopped at depth "+ depth
                  +" (probably circular)");
         depth++;
         if ( "external".equals(slot) && !type.isPrimitive() )
            getErr().println("WARNING: "+getId()+" ignoring external binding of non-primitive task "+variable); 
         for (Binding depend : depends) {
            // allow binding to refer to itself for "default" bindings
            if ( !equals(depend) )
               depend.update(decomp, depth, retractedStep, retractedSlot); // recursive
         }
         for (Binding depend : depends) {
            Task dependTask = getTask(decomp, depend.step); 
            if ( dependTask == null )
               throw new TaskModel.Error(DecompositionClass.this,
                     "Unknown slot "+depend.variable+" in binding for "+variable);
            if ( !equals(depend) && !dependTask.isDefinedSlot(depend.slot) ) {
               // don't try to compute binding that depends on undefined
               // variables (except for self-referring), since cannot guarantee that 
               // value expression will be safe (strict)
               if ( depend.step.equals(retractedStep) && depend.slot.equals(retractedSlot) )
                  // special case for retracted slot: propagate undefined to target
                  target.deleteSlotValue(slot);
               if ( inputInput && target.isDefinedSlot(slot) ) 
                  // special case for inIn identity only: propagate other direction
                  dependTask.copySlotValue(target, slot, depend.slot, true, false);
               return;
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
         }
      }
      
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

