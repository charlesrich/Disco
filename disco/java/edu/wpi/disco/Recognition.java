/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco;

import edu.wpi.cetask.*;
import java.io.PrintStream;
import java.util.*;

class Recognition { 
   
   private final List<Explanation> explanations = new ArrayList<Explanation>(4); 
   private final Task occurrence;
   
   List<Explanation> getExplanations () { return explanations; }
   
   private void add (Explanation explanation) {
      if ( explanation.start != null ) {
         TaskClass task = explanation.start.getType();
         for (Iterator<Explanation> i = explanations.iterator(); i.hasNext();) {
            Explanation e = i.next();
            Task current = explanation.getFocus().getGoal();
            Task old = e.getFocus().getGoal();
            // prefer specific explanation over Task.Any
            if ( current.getType() == Task.Any.CLASS ) {
               if ( old.getType() != Task.Any.CLASS ) return; // ignore current
               continue; // keep ambiguity
            } else if ( old.getType() == Task.Any.CLASS ) {
               i.remove(); // remove old
               continue; // keep current
            }
            int oldCount = old.countSlotValues(),
                currentCount = current.countSlotValues(); 
            // prefer more specific explanation with more defined slots
            if ( oldCount > currentCount ) return; // ignore current
            if ( currentCount > oldCount ) i.remove(); // remove old
            // otherwise keep current or ambiguity
         } 
         for (Explanation e : explanations) // do after above 
            // ignore ambiguity between different explanations for the same task
            // type --- this is a pretty strong oversimplification, since it
            // ignores inputs/outputs and the fact that there could validly be
            // different implementations, but it works well in practice
            if ( e.start != null && task == e.start.getType() ) return;
      }
      explanations.add(explanation);
   }

   Recognition (Task occurrence) { this.occurrence = occurrence; } 

   // Note immediate children of root node are always searched,
   // even if the root plan is done (not live), in order
   // to match trailing optional steps in current focus.  Otherwise,
   // the search does not look below non-live nodes.
   
   List<Explanation> recognize (Plan plan, Plan exclude) {
      recognizeWalk(plan, exclude);
      if ( Disco.TRACE && !explanations.isEmpty()) {
         PrintStream out = ((Disco) plan.getGoal().engine).getOut();
         out.println("Plan recognition for "+occurrence+" in "
               +plan+" (excluding "+exclude+"):");
         for (Explanation e : explanations) out.println("   "+e);
      }
      return explanations;
   }
   
   // walking existing structure
   private void recognizeWalk (Plan plan, Plan exclude) {
      // don't check isPathFrom here b/c may be procedurally added children
      if ( plan != exclude && (plan.isLive() || onStackHasLive(plan)) ) {
         Task goal = plan.getGoal();
         if ( occurrence.contributes(plan) ) 
            add(new Explanation(plan, null, null));
         for (Plan child : plan.getChildren()) {
            // prefer parent to prevent spurious ambiguity 
            if ( child.getGoal() != goal ) recognizeWalk(child, exclude);
         }
         // there could also be an interpolated explanation here
         recognizeDecomp(plan, plan.getType(),
               // speed up check for circularity, but preserve
               // order for debugging (only)
               new LinkedHashSet<DecompStep>());
      }
   }
   
   // look for trailing optionals for done tasks on stack only
   // TODO: this can be removed when semantics of optional fixed
   private boolean onStackHasLive (Plan plan) {
      for (Segment segment : ((Disco) plan.getGoal().engine).getStack())
         if ( segment.getPlan() == plan && plan.hasLiveDescendants() )
            return true;
      return false;
   }

   // interpolating based on decompositions
   private void recognizeDecomp (Plan start, TaskClass current, Set<DecompStep> path) {
      if ( !start.isDecomposed() && occurrence.isPathFrom(current) ) {
         for (DecompositionClass decomp : current.getDecompositions()) {
            for (String step : decomp.getLiveStepNames()) {
               DecompStep decompStep = new DecompStep(decomp, step);
               // check for cycles
               if ( path.contains(decompStep) ) continue;
               TaskClass stepType = decomp.getStepType(step);
               boolean extended = false;
               try {
                  if ( occurrence.contributes(stepType) ) {
                     try {
                        extended = true; path.add(decompStep);
                        Plan focus = instantiate(start, path);
                        // make sure instantiation didn't fail
                        if ( focus != null && occurrence.contributes(focus) )
                           add(new Explanation(focus, start, path));
                     } finally { start.setDecomposition(null); }
                  } 
                  // there could be another interpolated explanation here
                  if ( occurrence.isPathFrom(stepType) ) {
                     if ( !extended ) { extended = true; path.add(decompStep); }
                     recognizeDecomp(start, stepType, path);
                  }
               } finally { if ( extended ) path.remove(decompStep); }
            }
         }
      }
   }
    
   private Plan instantiate (Plan start, Set<DecompStep> path) {
      Plan current = start;
      for (DecompStep next : path) {
         current = next.instantiate(current);
         if ( current == null ) { // bindings failed
            start.setDecomposition(null);
            return null;
         }
      }
      // check for spurious ambiguity
      TaskClass type = current.getType();
      for (Plan child : current.getChildren()) {
         if ( child.getType() != type  
               && occurrence.contributes(child.getType()) 
               && occurrence.contributes(child) 
               && child.isLive() ) {
            start.setDecomposition(null);
            return null;
         }
      }
      return current;
   }
   
   static class Explanation {

      final Plan focus; // to attach occurrence 
      final Plan start; // to attach decomp (may be null)
      final private Decomposition decomp; // may be null
      final private List<DecompStep> path; // for debugging

      Explanation (Plan focus, Plan start, Set<DecompStep> path) {
         this.start = start;
         this.focus = focus;
         this.decomp = start == null ? null : start.getDecomposition();
         this.path = (path == null || !TaskEngine.DEBUG) ? null :
            new ArrayList<DecompStep>(path);
      }

      Plan getFocus () { return focus; }
      Plan getStart () { return start; }
      
      void attach () { if ( decomp != null ) start.setDecomposition(decomp); }
      
      void retract () { if ( decomp != null ) start.setDecomposition(null); }

      @Override
      public String toString () {
         return "Explanation[start="+start+",path="+path+",focus="+focus+"]";
      }
   }
   
   private static class DecompStep { 

      final private DecompositionClass decomp;
      final private String step;

      public DecompStep (DecompositionClass decomp, String step) {
         this.decomp = decomp;
         this.step = step;
      }

      public Plan instantiate (Plan current) {
         if ( current.isDecomposed() 
               || current.apply(decomp)  == null 
               // re-check applicability condition after bindings done (if not started)
               || (!current.getGoal().isStarter(current) && Utils.isFalse(decomp.isApplicable(current.getGoal()))) ) 
            return null;
         for (Plan child : current.getChildren()) {
            Task task = child.getGoal();
            if ( task instanceof Decomposition.Step 
                  && step.equals(((Decomposition.Step) task).getName()) )
               return child;
         }
         throw new IllegalStateException("Did not find step "+step);
      }

      @Override
      public boolean equals (Object o) {
         if ( !(o instanceof DecompStep) ) return false;
         DecompStep i = (DecompStep) o;
         return i.decomp.equals(decomp) && i.step.equals(step);
      }

      @Override
      public int hashCode () {
         return decomp.hashCode() ^ step.hashCode();
      }
      
      @Override
      public String toString () { return "<"+decomp+','+step+'>'; }
   }
}


