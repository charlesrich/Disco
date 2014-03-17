/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco;

import edu.wpi.cetask.*;

import java.util.*;

public class Segment {
   
   private Segment parent;
   public Segment getParent () { return parent; }
   
   final private List<Object> children = new ArrayList<Object>();
   
   /**
    * @return unmodifiable list of children of this segment.
    * Children are either segments or task occurrences
    */
   public List<Object> getChildren () { return Collections.unmodifiableList(children); }
 
   protected Iterator<Object> children () { return children.iterator(); }
   
   public void add (Task task) {
      if ( task == null ) throw new IllegalArgumentException("Null task not allowed!");
      children.add(task); 
   }
   
   private Plan plan;
   
   public Plan getPlan () { return plan; }
   
   // see Disco.retry
   void setPlan (Plan plan) { this.plan = plan; }
  
   public Task getPurpose () { return plan == null ? null : plan.getGoal(); }

   // plan may later be detached from parent due to to failure, but that
   // should not make it an interruption
   private boolean interruption; // not final due to remove method  
   
   public boolean isInterruption () { return interruption; }
   
   private boolean stopped;
   
   public boolean isStopped () { return stopped; }
   
   private final boolean continuation;
   
   public boolean isContinuation () { return continuation; }
   
   public void stop () { 
      if ( isRoot() ) throw new RuntimeException("Cannot stop root segment!");
      stopped = true; 
   }
    
   Segment () { // for root only
      this.plan = null;
      this.parent = null;
      interruption = false;
      continuation = false;
   } 
   
   public boolean isRoot () { return parent == null; }
   
   public Segment (Plan plan, Segment parent, boolean continuation) {
      if ( plan == null || parent == null )
         throw new IllegalArgumentException("Segment must have non-null plan and parent!");
      this.plan = plan;
      this.parent = parent;
      parent.children.add(this);
      interruption =  !parent.isRoot() && plan.getParent() != parent.getPlan();
      // check for continuation needs to happen before, e.g., occurrence added to plan
      this.continuation = continuation;
   }
   
   // used for removing implicit Accept goals, see Disco.reconcileStack()
   void remove () {
      int index = parent.children.indexOf(this);
      parent.children.remove(this);
      Plan planParent = plan.getParent();
      planParent.remove(plan);
      for (Object child : children) {
         parent.children.add(index++, child); 
         if (child instanceof Segment) {
            Segment segment = (Segment) child;
            segment.parent = parent;
            segment.interruption = interruption;
            Plan plan = segment.getPlan();
            this.plan.remove(plan);
            planParent.add(plan); // TODO preserve order of plan children?
         }
      }
      parent = null;
   }
   
   @Override
   public String toString () { return "<Segment "+getPurpose()+'>'; }
}
