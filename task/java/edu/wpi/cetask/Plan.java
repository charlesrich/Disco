/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.cetask;

import java.io.PrintStream;
import java.util.*;

public class Plan {
   
   private final Task goal;
   private final List<Plan> children;
   private Plan parent; // future improvement: allow multiple parents
   
   public Plan (Task goal) { this(goal, null); }
 
   public Plan (Task goal, Plan parent) {
      this(goal, parent, new ArrayList<Plan>());
   }
   
   private Plan (Task goal, Plan parent, List<Plan> children) {
      if ( goal == null ) 
         throw new NullPointerException("Goal of plan cannot be null");
      this.goal = goal;
      if ( parent != null) parent.add(this);
      this.parent = parent;
      this.children = children;
      for (Plan child : children) child.parent = this;
   }
   
   /**
    * @return the task which this plan is supposed to achieve
    */
   public Task getGoal () { return goal; }
   
   /**
    * @return the task class of this plan's goal
    */
   public TaskClass getType () { return goal.getType(); }
    
   private final List<Object> notes = new ArrayList<Object>();
   
   /**
    * Return list of notes to be added to verbose printing of this plan. This is
    * convenience feature for applications to manage by side effect.
    * 
    * @see #print(PrintStream)
    */
   public List<Object>getNotes () { return notes; }
      
   public final static String FOCUS_NOTE = "<-focus";
   
   /**
    * The following mutually exclusive breakdown of possible states of a plan
    * may be convenient for applications using Disco:
    * <ul>
    * <li>DONE - Equivalent to {@link #isDone()}.
    * 
    * <li>FAILED - Equivalent to {@link #isFailed()}.
    * 
    * <li>PENDING - This plan is live (see {@link #isLive()}) and if it is non-primitive, 
    *               has not been started (see {@link #isStarted()}).
    * 
    * <li>IN_PROGRESS - (For non-primitive tasks only) This plan has been started but is
    *                   not yet completed (see {@link #isComplete()}).
    * 
    * <li>INAPPLICABLE - This plan is not live only because the precondition of the
    *                    goal evaluated to false (i.e., all of its predecessors are done).
    *
    * <li>BLOCKED - Equivalent to {@link #isBlocked()}.
    * </ul>
    */
   public enum Status { DONE, FAILED, PENDING, IN_PROGRESS, INAPPLICABLE, BLOCKED }

   public Status getStatus () {
	   return isDone() ? Status.DONE :
		   isFailed() ? Status.FAILED :
			   // plan has not been (completely) executed
			   isLive() ? ( isStarted() ? Status.IN_PROGRESS : Status.PENDING ) :
				   isBlocked() ? Status.BLOCKED : Status.INAPPLICABLE;
   }
   
   private boolean optionalStep;
   
   /**
    * Makes this plan an optional step. Useful for procedural decompositions.  
    */
   public void setOptionalStep (boolean optionalStep) {
      this.optionalStep = optionalStep;
   }
   
   /**
    * Test whether this plan corresponds to an optional step of a decomposition.
    */
   public boolean isOptionalStep () { return optionalStep; }
   
   /**
    * Test whether this plan is optional. It is optional iff it has
    * not been accepted or started and it is either an optional step 
    * (see {@link #isOptionalStep()}) with a non-true precondition 
    * or its parent is optional.
    */
   public boolean isOptional () { 
      return !(Utils.isTrue(goal.getShould()) || isStarted()) &&
            ( (optionalStep && !Utils.isTrue(isApplicable()))
              || ( parent != null && parent.isOptional()) ); 
   }
  
   private int repeatStep;
   
   /**
    * Makes this plan the nth repeated step (maxOccurs &gt; 1).
    * Useful for procedural decompositions.  
    */
   public void setRepeatStep (int repeatStep) {
      this.repeatStep = repeatStep;
   }
   
   public int getRepeatStep () { return repeatStep; }
   
   /**
    * Test whether this plan corresponds to a repeated step of a decomposition.
    */
   public boolean isRepeatStep () { return repeatStep > 0; }
   
   public Plan getParent () { return parent; }  
            
   // whether this plan contributes to parent
   // set to false for copy of failed retry (see checkFailed)
   private boolean contributes = true;

   /**
    * Tests whether this plan contributes to its parent.
    */
   public boolean contributes () { return contributes; }
   
   /**
    * Sets whether this plan contributes to its parent.
    */
   public void setContributes (boolean contributes) { this.contributes = contributes; }
   
   /**
    * Test whether given plan can be reached from this plan by following parents.
    * Note plan is <em>not</em> its own ancestor.
    */
   public boolean isAncestor (Plan plan) {
      return plan == parent || (parent != null && parent.isAncestor(plan));
   }
   
   /**
    * Return closest ancestor of this plan that has goal
    * of given task class, or null if none.
    * 
    * @see #isAncestor(Plan)
    */
   public Plan getAncestor (TaskClass task) { 
      return parent == null ? null :
         parent.goal.getType() == task ? parent :
            parent.getAncestor(task);
   }

   /**
    * Test whether given plan can be reached from this plan by following children.
    * Note plan is <em>not</em> its own descendant.
    */
   public boolean isDescendant (Plan plan) {
      for (Plan child : children)
         if ( child == plan || child.isDescendant(plan) ) return true;
      return false;
   }
  
   /**
    * Return path from this plan to given plan iff it is a descendant; otherwise 
    * null.  Path does <em>not</em> include this plan or descendant and thus will 
    * be empty if given plan is child.
    */
   public Stack<Plan> pathToDescendant (Plan plan) {
      Stack<Plan> path = new Stack<Plan>();
      return pathToDescendant(plan, path, this) ? path : null;
   }
   
   private boolean pathToDescendant (Plan plan, Stack<Plan> path, Plan root) {
      for (Plan child : children)
         if ( child == plan || child.pathToDescendant(plan, path, root) ) {
            if ( this != root ) path.push(this);
            return true;
         }
      return false;
   }
   
   /**
    * @return unmodifiable list of children of this plan.
    */
   public List<Plan> getChildren () { return Collections.unmodifiableList(children); }
 
   /**
    * Add given plan as child of this plan.
    */
   public void add (Plan plan) { 
      if ( plan.parent != null ) 
         throw new IllegalArgumentException(
               "Plan already has a parent: "+plan.parent);
      if ( plan.goal.engine != goal.engine )
         throw new IllegalArgumentException(
               "Attempting to add goal from different engine "+ goal.engine);
      plan.parent = this;
      children.add(plan);
   }
   
   /**
    * Return child of this plan with given goal, or null if none.
    */
   public Plan getChild (Task goal) {
      for (Plan child : children)
         if ( child.goal == goal ) return child;
      return null;
   }
   
   /**
    * Remove given child of this plan.
    * 
    * @param child - must be child of this plan
    */
   public void remove (Plan child) { remove(child, null); }

   private void remove (Plan child, Collection<Plan> steps) {
      if ( !children.contains(child) )
         throw new IllegalArgumentException(child+" is not child of "+this);
      children.remove(child);
      child.parent = null;
      for (Iterator<Plan> successors = child.requiredBy.iterator(); successors.hasNext();) {
         Plan successor = successors.next();
         if ( steps == null || !steps.contains(successor) ) {
            successor.required.remove(child);
            successors.remove();
         }
      }
      for (Iterator<Plan> predecessors = child.required.iterator(); predecessors.hasNext();) {
         Plan predecessor = predecessors.next();
         if ( steps == null || !steps.contains(predecessor) ) {
            predecessor.requiredBy.remove(child);
            predecessors.remove();
         }
      }
   }
   
   // these lists usually contain siblings (common parent), but not checking
   final private List<Plan> required = new ArrayList<Plan>();
   final List<Plan> requiredBy = new ArrayList<Plan>(); // package access for Decomposition
   
   /**
    * Make this plan require given plan.
    */
   public void requires (Plan plan) {
      if ( plan == this ) 
         throw new IllegalArgumentException("Circular dependency: "+plan);
      required.add(plan);
      plan.requiredBy.add(this);
      isRequired(this, null); // check for cycles
   }
   
   /**
    * Remove the dependency of this plan on given plan.
    */
   public void unrequires (Plan plan) {
      if ( !required.remove(plan) ) throw new IllegalArgumentException("Does not require: "+plan);
      plan.requiredBy.remove(this);
   }
   
   /**
    * Test if this plan requires given plan.
    */
   public boolean isRequired (Plan plan) {
      return required.contains(plan);
   }
   
   /**
    * Modify the dependencies in parent of this plan such
    * that all siblings that require this plan will require given plan
    * instead.  Assumes given plan already a child of same parent.
    */
   public void splice (Plan plan) {
      if ( parent == null ) throw new UnsupportedOperationException("No parent: "+this);
      if ( plan.parent != parent ) throw new IllegalArgumentException("Not same parent: "+plan);
      for (Plan child : parent.children)
         if ( child.isRequired(this) ) {
            child.unrequires(this);
            child.requires(plan);
         }
      goal.engine.clearLiveAchieved();
   }

   /**
    * Test whether plan1 requires plan2 <em>transitively</em>.
    */
   public static boolean isRequired (Plan plan1, Plan plan2) {
      return isRequired(plan1, plan2, 0);
   }

   private static boolean isRequired (Plan plan1, Plan plan2, int depth) {
      if (depth > DecompositionClass.MAX_REQUIRES_DEPTH) 
         throw new RuntimeException(plan1+" requires stopped at depth "+ depth
                  +" (probably circular)");
      if ( plan1.required.contains(plan2) ) return true;
      for (Plan plan : plan1.required)
         if ( isRequired(plan, plan2, depth+1) ) return true;
      return false;
   }
   
   /**
    * Return unmodifiable list of immediate successors of this plan, i.e., 
    * the plans that require it.
    */
   public List<Plan> getSuccessors () { 
      return Collections.unmodifiableList(requiredBy); }
   
   /**
    * Return unmodifiable list of immediate predecessors of this plan, i.e., 
    * the plans it requires.  This plan may not become live until all of these
    * are done (see {@link #isDone()}) and its precondition is not false.
    */
   public List<Plan> getPredecessors() { 
      return Collections.unmodifiableList(required);
   }
   
   public boolean isOccurred () { return goal.isOccurred(); }
   
   /**
    * Tests whether the postcondition of the goal evaluated to true or
    * unknown at the appropriate time (@see {@link #isSucceeded()}).
    * This test is used to determine liveness (see {@link #getPredecessors()}).
    * Note that this plan may have "trailing" optional steps that have
    * not yet been completed.
    * 
    * @see #isExhausted()
    * @see #isFailed()
    */
   public boolean isDone () {
      return ( isOccurred() || isSucceeded() || isComplete() ) && !isFailed();
   }

   /**
    * Tests whether the postcondition of the goal evaluated to false
    * immediately after execution of this plan was completed.
    */
   public boolean isFailed () {
	   return Utils.isFalse(goal.getSuccess());
   }

   /**
    * Tests whether the postcondition of the goal evaluated to true immediately
    * after execution of this plan was completed, or for sufficient
    * postconditions, while this plan was live.  Note that for non-sufficient
    * postconditions, the fact that the postcondition evaluated to true
    * does not guarantee that the task "really" succeeded.
    */
   public boolean isSucceeded () {
	   return Utils.isTrue(goal.getSuccess());
   }

   public Boolean isApplicable () {
      synchronized (goal.bindings) {
         goal.bindings.put("$plan", this);
         return goal.isApplicable();
      }
   }
   
   // not public because has caching side effect
   // use isSucceeded()
   Boolean isAchieved () {
      synchronized (goal.bindings) {
         goal.bindings.put("$plan", this);
         return goal.isAchieved();
      }
   }

   /**
    * Tests whether this plan is ready for execution based on its predecessors being done
    * and the precondition of the goal not being false.  Note that this does <em>not</em>
    * imply that all of its inputs are defined.
    */
   public boolean isLive () {
      Boolean live = goal.engine.isLive(this); // check cached value
      if ( live != null ) return live;
      // do not call isDone() or isLive() below to avoid infinite recursion
      return goal.engine.setLive(
            this, // store cache
            !isComplete() && isLivePlan());
   }
   
   private  boolean isLivePlan () {
      return goal.getSuccess() == null && isLiveAchieved() && 
            !(getType().isSufficient() && Utils.isTrue(isAchieved())) &&
            (parent == null || parent.isLivePlan());
   }

   // isLivePlan except for achieved
   private boolean isLiveAchieved () {
      return !isOccurred() && !Utils.isFalse(goal.getShould()) 
         && !isPassed() && isRequiredDone()
         && (isStarted() || !Utils.isFalse(isApplicable()))
         && !isFailed();
   }
   
   private boolean isAchievedSufficient () {
      return isSucceeded() || 
         (getType().isSufficient() && isLiveAchieved() && Utils.isTrue(isAchieved()));
   }

   /**
    * Tests whether popping plan from the stack would constitute an
    * unnecessary focus shift (see docs/LeshRichSidner2001.pdf)
    * 
    * @return true iff popping would <em>not</em> be an unnecessary focus
    *         shift.
    */
   public boolean isPoppable () { return !isLive() || isDone(); }

   private boolean complete;
   
   /**
    * Useful for setting a non-primitive complete when there is no
    * decomposition.
    */
   public void setComplete (boolean complete) { 
      this.complete = complete;
      getGoal().engine.clearLiveAchieved();
   }
   
   /**
    * Tests whether all the non-optional steps of this plan
    * are done. Note that even if this method returns true (and this
    * plan is therefore done), it may still have optional ("trailing") children
    * that are live.  Returns false for primitive tasks.
    * 
    * @see #isDecomposed()
    * @see #isDone()
    * @see #isPassed()
    * @see #setComplete(boolean)
    */
   public boolean isComplete () {
      if ( complete ) return true;
      if ( !isDecomposed() ) return false;
      // need to ignore failed children from failed decomposition
      if ( decomp == null ) { // decomposed
         if ( decompClass != null ) return false;  // not expanded yet 
         for (Plan child : children) // procedural decomposition
            if ( !isComplete(child) ) return false;
      } else if ( !failed.contains(decomp) ) { 
         for (Plan step : decomp.getSteps())
            // check contributes to allow recovery
            if ( step.contributes() && !isComplete(step) ) return false;
      }
      return true;
   }

   private static boolean isComplete (Plan child) {
      return child.isDone() || child.isOptionalStep();
   }
   
   public boolean isPassed () {
      if ( parent != null ) {
         for (Plan sibling : parent.children)
            if ( (sibling.isRequired(this) && sibling.isStarted()) ) return true;
         if ( parent.isPassed() ) return true; // must recurse upward in tree
      }
      return false;
   }
   
   /**
    * Test whether any required plans in this plan or recursively through parent
    * are not done and therefore block this plan from becoming live.
    */
   public boolean isBlocked () {
	  return !isRequiredDone() || ( parent != null && parent.isBlocked() );
   }
   
   private boolean isRequiredDone () {
      for (Plan plan : required)
         if ( !isRepeatStep() && plan.isOptionalStep() && plan.isOptional() ) { 
            if ( !plan.isRequiredDone() ) return false; 
         } else if ( !plan.isDone() ) return false;
      return true;
   }
   
   /**
    * Test if this plan has live children.
    * 
    * @see #hasLiveDescendants()
    */
   public boolean hasLive () {
	   for (Plan child : children)
		   if ( child.isLive() ) return true;
	   return false;
   }

   /**
    * Return list of live children of this plan.
    * 
    * @see #getLiveDescendants()
    */
   public List<Plan> getLive () {
      List<Plan> live = new ArrayList<Plan>();
      for (Plan child : children)
         if ( child.isLive() ) live.add(child);
      return live;
   }
    
   /* TODO Fix handling of trailing optionals

      Currently, trailing optional steps will only be recognized if focus
      stays inside parent (see onStack() in recognizeWalk()).   Generation,
      however, uses getLiveDescendants and hasLiveDescendants, which currently 
      does look inside done tasks (which is inefficient).
      
      Changing behavior will preserve invariant of live child implies live 
      parent. (Contrapositive of this rule is what justifies not looking inside
      non-live parents.)
      
      Implementation sketch:
       -basic idea is to distinguish between two conditions: one that
        disables blocking, isBlocker() = !(complete or occurred or succeeded ?), and 
        one that disables live (done)
       -change plan printing so that prints -complete if complete
        but not done
       -correct comments such as below and at complete
       -modify and review uses of get/hasLiveDescendants, isExhausted, ?
       -note this includes plans with _only_ optionals
   */
   
   /**
    * Return list of live descendants of this plan. Note that
    * a non-live (done) plan may have live descendants due to trailing 
    * optional steps.
    *     
    * @see #hasLiveDescendants()
    * @see #getLive()
    */
   public List<Plan> getLiveDescendants () {
      return getLiveDescendants(new ArrayList<Plan>());
   }
   
   private List<Plan> getLiveDescendants (List<Plan> live) {
      for (Plan child : children) {
         child.getLiveDescendants(live);
         if ( child.isLive() ) live.add(child);
      }
      return live;
   }
 
   /**
    * Test whether this plan has any live descendants. Note that
    * a non-live (done) plan may have live descendants due to trailing 
    * optional steps.
    * 
    * @see #getLiveDescendants()
    */
   public boolean hasLiveDescendants () {
      for (Plan child : children)
         if ( child.isLive() || child.hasLiveDescendants() ) return true;
      return false;
   }
 
   /**
    * Tests whether this plan has any live successors.
    */
   public boolean hasLiveSuccessor () {
      if ( hasLiveSuccessorStep() ) return true;
      Plan parent = getParent();
      return parent != null && parent.hasLiveSuccessor(); 
   }
   
   private boolean hasLiveSuccessorStep () {
      for (Plan successor : requiredBy) 
         if ( successor.isLive() || successor.hasLiveSuccessorStep() ) return true;
      return false;
   }

   /**
    * Test whether this plan has any failed contributing children.
    */
   public boolean hasFailed () {
      for (Plan child : children)
         if ( child.contributes && child.isFailed() ) return true;
      return false;
   }
   
   private boolean started;
   
   /**
    * Useful for setting a non-primitive started when there is no
    * decomposition.
    */
   public void setStarted (boolean started) { this.started = started; }
   
   /**
    * Test whether this plan or any of its descendants which is a
    * starter {@link Task#isStarter(Plan)} have been executed.
    */
   public boolean isStarted () {
      if ( started || isOccurred() ) return true;
      for (Plan child : children)
         if ( child.goal.isStarter(this) && child.isStarted() ) return true;
      return false;
   }

   /**
    * Test whether goal of this plan is instance of primitive type.
    */
   public boolean isPrimitive () { return goal.isPrimitive(); }
   
   /**
    * Test whether goal of this plan is instance of internal type.
    */
   public boolean isInternal () { return goal.isInternal(); }
   
   /**
    * Test whether this plan has any remaining steps that could be executed now.
    * If this returns false, then {@link #hasLive()} should return true.
    * Returns true for non-live primitive task.
    * 
    * @see #isDone()
    */
   public boolean isExhausted () {
      // may be done but have live optional trailing steps
      if ( goal.getSuccess() != null || isMoot() 
            || !isRequiredDone() || Utils.isFalse(goal.getShould())
            // precondition only required before starting               
            || ( !isStarted() && Utils.isFalse(isApplicable()) ))
         return true;
      if ( isPrimitive() ) return goal.isTemporary() || !isLive();
      else if ( isDecomposed() ) {
         if ( decomp == null && decompClass != null ) return false; // not expanded yet
         for (Plan child : children) // procedural decomposition
            if ( child.contributes && !child.isExhausted() ) return false;
         return true;
      } else // non-primitive, not decomposed but may have children 
         return isDone() && !hasLive();
   }
   
   /**
    * Tests whether this plan is incomplete, but does not need to be executed
    * because all the reasons for doing it have already been achieved or are no
    * longer desired. Currently, this means that there is an achieved or
    * occurred plan looking "up" from this plan. 
    */
   public boolean isMoot () { return !isComplete() && isMooted();  }
   
   private boolean isMooted () {
      return parent != null && 
        ( parent.isOccurred() || parent.isSucceeded() || parent.isMooted());
   }
   
   /**
    * Tests whether this plan is or is expected to ever become live.  For
    * example, a plan which has been passed (required successor started) can 
    * never become live.  Used mainly for status.
    */
   public boolean isExpected () {
      return contributes &&
         !( isOccurred()  
            || goal.getSuccess() != null // already succeeded or failed
            // don't expect if sufficient postcondition currently true
            || isAchievedSufficient()
            // or optional step and precondition currently false 
            || (optionalStep && Utils.isFalse(isApplicable())) //sic not isOptional
            || isPassed()
            // trailing optionals not expected
            || (isComplete() && !hasExpected())
            ); 
   }
   
   /**
    * Test whether this plan has immediate subplan which is expected.
    * 
    * @see #isExpected()
    */
   public boolean hasExpected () {
      for (Plan child : children) if ( child.isExpected() ) return true;
      return false;
   }
   
   public List<Plan> getExpected () {
      List<Plan> expected = new ArrayList<Plan>(children.size());
      for (Plan child : children) if ( child.isExpected() ) expected.add(child);
      // sort live plans to front
      Collections.sort(expected, new Comparator<Plan>() { 
         // note this comparator imposes orderings inconsistent with equals
         @Override
         public int compare (Plan p, Plan q) { 
            return p.isLive() ? ( q.isLive() ? 0 : -1 ) :
               q.isLive() ? ( p.isLive() ? 0 : 1 ) :
                  0;
         }
      });
      return expected;
   }
 
   /**
    * Identify given task as matching this plan and copy slot values.
    */
   public void match (Task task) {
      goal.copySlotValues(task);
      goal.engine.clearLiveAchieved();
   }
   
   void checkAchieved () {
      goal.engine.clearLiveAchieved();
      if ( isStarted() && isExhausted() ) {
         // no more chances to succeed
         Boolean success = goal.checkAchieved();
         if ( decomp != null && 
               (Utils.isFalse(success) || (success == null && hasFailed())) ) 
            fail();
      }
      if ( parent != null ) parent.checkAchieved(); // recurse up tree
   }
   
   void checkSuccess () {
      // don't need to look for trailing optionals re fortuitous success
      if ( isLive() ) {
         for (Plan child : children) child.checkSuccess();
         // true postcondition makes fortuitous success not live         
      } else if ( isAchievedSufficient() ) getGoal().setSuccess(true);
   }
   
   /**
    * Indicate that this plan (including current decomposition, if any) has failed.
    * Note that plan is not automatically retried by this call.
    * 
    * @see #retry()
    */
   public void fail () {
      if ( decomp != null ) {
         if ( failed == Collections.EMPTY_LIST ) 
            failed = new ArrayList<Decomposition>();
         failed.add(decomp);
         decomp.setFailed(true);
      }
      goal.setSuccess(false);  // make sure is false
   }
   
   /**
    * If this plan has failed, and the goal is retriable and it has not already
    * been retried, then retry it; otherwise if it has failed, but is not
    * retriable,then recurse up through failed parents looking for retriable goal.
    * <p>
    * <em>Note exception:</em> if a primitive goal is retriable and its parent is retriable, then
    * prefer to retry parent.
    * <p>
    * A non-primitive is retriable by default iff it has any applicable untried
    * decompositions. A primitive is not retriable by default. Either default
    * can be overridden by the boolean 'goal@retry' property.
    * 
    * @return the retried plan or null if none.
    * @see #getRetry()
    * @see #getRetryOf()
    */
   public Plan retry () {
      // don't retry same plan twice (retry can be retried!)
      if ( retry != null ) return null;  
      if ( isFailed() ) {
         if ( isRetriable() 
               // exception noted above
               && !(isPrimitive() && parent != null && parent.isFailed() && parent.isRetriable()) ) {
            retryCopy();
            return this;
         } else return parent == null ? null : parent.retry(); 
      } else return null;
   }

   private boolean isRetriable () {
      return goal.getProperty("@retry", 
            !isPrimitive() && !getDecompositions().isEmpty());
   }
   
   private Plan retry, retryOf;  
   
   /**
    * If this plan has been retried, return the retry otherwise null.
    * 
    * @see #getRetryOf()
    * @see #retry()
    */
   public Plan getRetry () { return retry; }
   
   /**
    * If this plan is the retry of another plan, return that plan, otherwise null.
    * 
    * @see #getRetry()
    * @see #retry()
    */
   public Plan getRetryOf () { return retryOf; }
   
   /**
    * Retry this plan (usually called on failed plans).  
    */
   private void retryCopy () {
      if ( TaskEngine.VERBOSE ) getGoal().engine.getErr().println("Retrying "+goal);
      // make copy to cache failure information and move all children to it
      // and make this plan ready for retry
      retryOf = new Plan(goal.getType().newInstance(), null, 
            new ArrayList<Plan>(children));
      retryOf.retry = this;
      children.clear();
      planned = false; // since no children left
      if ( decomp != null ) {
         retryOf.decomp = decomp;
         retryOf.decomp.goal = retryOf.goal;
         decomp = null;
      }
      retryOf.contributes = false;
      retryOf.goal.copySlotValues(goal);
      if ( parent != null ) {
         retryOf.parent = parent;
         // insert failure copy in parent before this plan for printing
         // (but note not updating predecessor/successor information)
         parent.children.add(parent.children.indexOf(this), retryOf);
         parent.unFail();
      }
      // remove all output values (including success and when)
      for (String name : goal.getType().outputNames) 
         goal.removeSlotValue(name);
      // remove all input values that depend on failed decomposition
      if ( retryOf.decomp != null ) {
         for (String name : goal.getType().inputNames) 
            if ( retryOf.decomp.hasModifiedInput(name) )
               goal.removeSlotValue(name);
      }
      goal.engine.clearLiveAchieved();
   }
  
   public void unFail () {
      if ( isFailed() ) {
         goal.removeSlotValue("success");
         failed.clear();
         decomp.setFailed(false);
         if ( parent != null ) parent.unFail();
      }
   }

   /**
    * Returns list of decompositions classes applicable to this plan,
    * excluding any that have been tried and failed.  If a decomposition class has
    * been chosen, then this method returns a singleton list of that decomposition class.
    * 
    * @see Task#getDecompositions()
    * @see TaskClass#getDecompositions()
    * @see Plan#getFailed()
    */
   public List<DecompositionClass> getDecompositions () {
      if ( decompClass != null ) return Collections.singletonList(decompClass);
      List<DecompositionClass> decomps;
      synchronized (goal.bindings) {
         goal.bindings.put("$plan", this);
         decomps = goal.getDecompositions();
      }
      for (Iterator<DecompositionClass> i = decomps.iterator();
           i.hasNext();) {
         DecompositionClass next = i.next(); // only call once
         for (Decomposition fail : failed)
            if ( fail.getType().equals(next) ) {
               i.remove(); break;
            }
      }
      return decomps;
   }
   
   // either of these may be null while other is non-null; however, if both
   // non-null, they must be consistent
   private Decomposition decomp; 
   private DecompositionClass decompClass; 
   
   /**
    * Returns the decomposition class, if any, chosen for this plan.  Note
    * that even if this method returns non-null, {@link #isDecomposed()}
    * may return false, which means that decomposition has not yet been applied.
    */
   public DecompositionClass getDecompositionClass () { 
      return decomp == null ? decompClass : decomp.getType();
   }

   /**
    * Set the decomposition class to use for this plan (replacing the current
    * choice, if any).  Note that {@link #isDecomposed()} may return false
    * after this method is called (because decomposition has not yet been
    * applied).  Also note that changing the decomposition class of
    * an already started plan is not currently supported.
    */
   public void setDecompositionClass (DecompositionClass decompClass) {
      if ( getDecompositionClass() == decompClass ) return;
      if ( decompClass.getGoal() != goal.getType() )
         throw new IllegalArgumentException(decompClass+" not applicable to "+this);
      checkStarted();
      if ( this.decomp != null ) setDecompositionInternal(null);
      this.decompClass = decompClass;
      decomp = null;
   }
   
   private void checkStarted () {
      if ( isStarted() )
         throw new IllegalArgumentException(
               "Cannot change decomposition of started plan "+this);
   }
   
   /**
    * Returns the decomposition instance, if any, used to decompose this plan.
    * If this method returns non-null, then {@link #getDecompositionClass()}
    * returns the type of this instance. 
    *
    * <em>Warning:</em> Calling this method will cause the currently chosen decomposition
    * class to be applied, if it has not already been, which modifies the children of 
    * this plan.  This can cause a ConcurrentModificationException if this method 
    * is called inside a {@link #getChildren()} iterator.
    * 
    * @see #isDecomposed()
    */
   public Decomposition getDecomposition () { 
      if ( decomp == null && decompClass != null ) apply(decompClass);
      return decomp; 
   }
   
   /**
    * Apply the given decomposition to this plan (replacing the current
    * decomposition, if any).  Note that changing the decomposition of an
    * already started plan is not currently supported.
    * 
    * @see #apply(DecompositionClass)
    */
   public void setDecomposition (Decomposition decomp) {
      if ( this.decomp == decomp && decomp != null ) return;
      // TODO consider putting failed children somewhere else
      if ( failed == null ) checkStarted();
      setDecompositionInternal(decomp);
   }
   
   private void setDecompositionInternal (Decomposition decomp) {
      if ( decomp != null && decomp.getType().getGoal() != goal.getType() )
         throw new IllegalArgumentException(decomp+" not applicable to "+this);
      if ( this.decomp != null ) { // remove decomposition
         Collection<Plan> steps = this.decomp.getSteps();
         for (Plan step : steps) {
            if ( step.isStarted() ) step.contributes = false;
            else {
               remove(step, steps);
               if ( goal.engine.getFocus() == step) goal.engine.pop();
            }
         }
         this.decomp.retract();
      }
      this.decomp = decomp;
      decompClass = null;
      if ( decomp != null ) {
         // do not set last decomp back to null
         goal.getType().setLastDecomposition(decomp);
         if ( decomp.getGoal() == null ) decomp.attach(this); // test for apply
         for (Plan step : decomp.getSteps()) add(step);
      }
      checkSuccess();  // decomps can change success
   }
   
   /**
    * Decompose this plan using given decomposition class (assuming applicability
    * condition already checked).
    * 
    * @return resulting decomposition instance or null if application
    *         failed due to contradiction in bindings
    */
   public Decomposition apply (DecompositionClass type) {
      if ( type.getGoal() != goal.getType() )
         throw new IllegalArgumentException(type+" not applicable to "+this);
      if ( getDecompositionClass() == type && decomp != null ) return decomp; 
      try { setDecomposition(new Decomposition(type, this)); }
      catch (DecompositionClass.Contradiction e) { 
         if ( TaskEngine.DEBUG ) System.out.println(e);
         return null;        
      }
      return decomp;
   }       
 
   boolean applyDecompositionScript () {
      String script = getType().getDecompositionScript();
      if ( script != null ) { 
         goal.bindings.put("$plan", this);
         Object result = goal.eval(script, "Decomposition script for "+getType());
         if ( result instanceof Boolean ) {
            if (((Boolean) result)) setPlanned(true);
            return (Boolean) result;
         } else throw new RuntimeException("Decomposition script for "+getType()
         +" returned non-boolean: "+result);
      } else return false;
   }
   
   /**
    * Use this method instead of {@link Task#setSlotValue(String,Object)}
    * when the goal task is not a step in a higher-level decomposition to force
    * immediate recursive updating of the bindings in the decomposition
    * of this plan, if any.
    * <p>
    * Note this is usually only required when manipulating plans <em>outside</em>
    * of the discourse state, since otherwise updating of the bindings will
    * be triggered by the liveness checking that the agent does on every tick.
    */
   public Object setSlotValue (String name, Object value) {
      goal.setSlotValue(name, value);
      if ( decomp != null ) decomp.updateBindings(true, null, null, null);
      return value;
   }   

   /**
    * Use this method instead of {@link Task#removeSlotValue(String)} as
    * discussed in {@link #setSlotValue(String,Object)}
    */
   public void removeSlotValue (String name) {
      goal.removeSlotValue(name);
      if ( decomp != null ) decomp.updateBindings(true, null, "this", name);
   }
     
   private List<Decomposition> failed = Collections.emptyList();
   
   public List<Decomposition> getFailed () { 
      return Collections.unmodifiableList(failed); 
   }
   
   private boolean planned; // for procedural decomposition
   
   /**
    * Mark (or unmark) the current children of this plan as a decomposition even if no
    * decomposition class has been used.
    * 
    * @see #isDecomposed()
    */
   public void setPlanned (boolean planned) { 
      if ( planned && isPrimitive() )
         throw new IllegalArgumentException("Cannot plan primitive goal "+this);
      this.planned = planned; }
   
   /**
    * Test whether this plan has been procedurally decomposed.  Returns false
    * for primitive task.
    * 
    * @see #isDecomposed()
    */
   public boolean isPlanned () { return planned; }
   
   /**
    * Tests whether the children of this plan are expected to achieve the goal
    * (never true for a primitive task).  Note that even if this method returns
    * true, {@link #getDecompositionClass()} may return null (because decomposition
    * was done procedurally).  Returns false for primitive tasks.
    *
    * @see #setPlanned(boolean)
    */
   public boolean isDecomposed () { 
      return !isPrimitive() && (planned || decomp != null); 
   }

   /**
    * Tests whether decomposition chosen for this plan.  Returns true iff either
    * {@link #isDecomposed()} returns true or {@link #getDecompositionClass()} 
    * returns non-null. Returns false for primitive tasks.
    */
   public boolean isHow () { return isDecomposed() || decompClass != null; }
   
   /**
    * Recursively apply all decompositions that do not involve choices.
    * Does not decompose done plans.
    * 
    * @return true iff any decompositions applied
    */
   public boolean decomposeAll () {
      return decomposeAll(new Stack<DecompositionClass>());
   }
   
   private boolean decomposeAll (Stack<DecompositionClass> stack) {
      boolean applied = false;
      if ( !(isPrimitive() || isDecomposed() || isDone()) ) {
         // always try script first
         boolean decomposed = applyDecompositionScript();
         if ( !decomposed ) {
            // if decomposition chosen and plan is live, 
            // or only one *known* decomposition for goal type and not de-authorized, 
            // then apply it now if ap1046plicable
            DecompositionClass decomp = getDecompositionClass();
            if ( !isLive() ) decomp = null;
            if ( decomp == null ) {
               List<DecompositionClass> decomps = goal.getType().getDecompositions();
               if ( decomps.size() == 1 ) {
                  decomp = decomps.get(0);
                  if ( !decomp.getProperty("@authorized", true) ) decomp = null;
               }
            }
            if ( decomp != null )
               synchronized (goal.bindings) {
                  goal.bindings.put("$plan", this);
                  if ( !Utils.isFalse(decomp.isApplicable(goal)) ) {
                     Plan focus = goal.engine.getFocus();
                     // inhibit infinite recursion, but allow incremental expansion
                     // if live (and recursive parent started) or if focus or child 
                     // of focus 
                     if ( !stack.contains(decomp) 
                           || (focus != null && 
                           (focus == this || focus.children.contains(this))) 
                           || (isLive() && recursiveParent(decomp).isStarted()) ) 
                     { apply(decomp); applied = true; }
                  }
               }
         }
      }
      if ( decomp != null ) stack.push(decomp.getType());
      for (Plan child : children) {
         if ( child.decomposeAll(stack) ) applied = true;
      }
      if ( decomp != null ) stack.pop();
      return applied;
   }
   
   private Plan recursiveParent (DecompositionClass type) {
      return parent == null ? null // internal error
         : (parent.decomp == null || parent.decomp.getType() != type) ? 
            parent.recursiveParent(type)
         :  parent;           
   }
   /**
    * Return (possibly empty) list of plans in this plan tree which explain
    * given task (i.e., to which given task contributes).
    * Note immediate children of root node are always searched,
    * even if onlyLive is true and root tree is done (not live), in order
    * to match trailing optional steps in current focus.  Otherwise,
    * the search does not look below non-live nodes when onyLive is true.
    * 
    * @param onlyLive only return live plans 
    * @param exclude already searched (ignored if root)
    */
   public List<Plan> explain (Task task, boolean onlyLive, Plan exclude) {
      List<Plan> plans = new ArrayList<Plan>();
      // unroll recursion one level
      if ( (!onlyLive || isLive()) && goal.isMatch(task) ) plans.add(this);
      for (Plan child : children)
         child.explain(task, plans, onlyLive, exclude);
      return plans;
   }
   
   private void explain (Task task, List<Plan> plans, 
                         boolean onlyLive, Plan exclude) {
      if ( this != exclude && (!onlyLive || isLive()) ) { 
         if ( task.contributes(this) ) plans.add(this);
         for (Plan child : children) 
            child.explain(task, plans, onlyLive, exclude);
      }
   }
   
   @Override
   public String toString () {
      StringBuilder buffer = new StringBuilder().append('[');
      if ( TaskEngine.VERBOSE && parent != null && parent.decomp != null ) {
         String step = parent.decomp.findStep(this);
         if ( step != null ) buffer.append(step).append(':');
      }
      buffer.append(goal.toString()).append(']');
      return buffer.toString();
   } 
 
   public void print () { print(getGoal().engine.getOut()); }
   
   /**
    * Print a compact human-readable version of plan tree with this plan as root.
    * 
    * @see #getNotes()
    */
   public void print (PrintStream stream) { 
      print(stream, 0, true, !TaskEngine.DEBUG, null); }
   
   public void print (PrintStream stream, int indent, 
                      boolean recurse, boolean format, String formatted) {
      for (int i = indent; i-- > 0;) stream.print("   ");
      stream.print('[');
      if ( TaskEngine.VERBOSE && goal instanceof Decomposition.Step ) {
         String name = ((Decomposition.Step) goal).getName();
         if ( name != null ) { 
            stream.print(name); 
            stream.print(TaskEngine.DEBUG ? ":" : ": "); 
         }
      }
      if ( format && formatted != null ) stream.print(formatted);
      else if ( !TaskEngine.DEBUG && goal.isInternal() ) stream.print(" ");
      else {
         boolean who = false;
         if ( format && isPrimitive() && goal.isDefinedSlot("external") ) { 
            stream.print(Utils.capitalize(
                  goal.getExternal() ? goal.engine.getExternalName() :
                     goal.engine.getSystemName()));
            stream.print(" ");
            who = true;
         }
         stream.print(format ? 
            ( who ? goal.format() : Utils.capitalize(goal.format()) )
            : goal);
         DecompositionClass decomp = getDecompositionClass();
         if ( decomp != null &&
               (TaskEngine.VERBOSE 
                     // suppress unique known and unformatted decompositions
                     || goal.getType().getDecompositions().size() > 1 
                     || decomp.getProperty("@format") != null))  {
            stream.print(' '); 
            if ( format ) stream.print(decomp.format());
            else { stream.print("by "); stream.print(decomp); }
         }
      }
      stream.print(']');
      if ( TaskEngine.DEBUG ) {
         if ( optionalStep ) stream.print(" -optionalStep"); 
         if ( isOptional() ) stream.print(" -optional"); 
      } else if ( optionalStep ) stream.print(" -optional"); // sic not isOptional()
      if ( Utils.isTrue(goal.getShould()) ) stream.print(" -accepted");
      else if ( Utils.isFalse(goal.getShould()) ) stream.print(" -rejected");
      if ( isLive() ) stream.print(" -live");
      else if ( !goal.printSuccess(stream) ) {
         if ( isDone() ) stream.print(" -done"); 
         else if ( !contributes ) stream.print(" -nonContributing");
      }
      for (Object note : notes) { stream.print(' '); stream.print(note); }
      if ( recurse ) {
         stream.println();
         if ( !children.isEmpty() ) {
            indent++;
            for (Plan child : children) 
               child.print(stream, indent, recurse, format, null);
         }
      }
   }
   
 }
