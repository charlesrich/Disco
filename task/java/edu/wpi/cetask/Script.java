/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.cetask;

import edu.wpi.cetask.ScriptEngineWrapper.Compiled;

import org.w3c.dom.Node;

import javax.script.*;
import javax.xml.xpath.XPath;

public class Script extends Description {
   
   private final TaskClass task;
   
   public TaskClass getTask () { return task; }
   
   private final String script, applicable, model; 
   private final Compiled compiled, compiledApplicable;
   
   public String getModel () { return model; }

   public Boolean isApplicable (Task occurrence) {
      return occurrence.evalCondition(applicable, compiledApplicable, 
            task+" applicable");
   }

   private final boolean init;
   public boolean isInit () { return init; }
   
   Script (Node node, TaskEngine engine, TaskClass task, XPath xpath) {
      super(node, engine, xpath);
      init = Utils.parseBoolean(xpath("./@init"));
      model = Utils.emptyNull(xpath("./@model"));
      if ( task == null ) {
         String qname = xpath("./@task");
         if ( qname.length() > 0 ) task = resolveTaskClass(qname);
         if ( task == null && !isInit() && getPlatform() == null)
            throw new RuntimeException("Syntax Error: Toplevel script without task, platform or init=true");
      }
      this.task = task;
      String where = task == null ? getModel() : task.toString();
      script = getText(); 
      applicable = xpath("./@applicable"); 
      compiled = TaskEngine.isCompilable() ?
         engine.compile(getText(), where) : null;
      compiledApplicable = TaskEngine.isCompilable() ?
         engine.compile(applicable, where+" applicable") : null;
   }  
   
   public String getPlatform () {
      return Utils.emptyNull(xpath("./@platform"));
   }
   
   public String getDeviceType () {
      return Utils.emptyNull(xpath("./@deviceType"));
   }
   
   public String getText () { return xpath("."); }

   /**
    * Evaluate the grounding script associated with given occurrence.
    * 
    * Note: This method is thread-safe, which means that it can be
    * safely called on other than the Disco thread (as long, of course as the
    * script does not reference $disco).  Grounding scripts typically
    * need to be executed on the application (e.g., game) thread.
    */
   public void eval (Task occurrence) {
      String where = task.toString();
      if ( compiled != null)  
         // assuming that $platform and $deviceType already in task.bindings
         try { compiled.eval(occurrence.bindings); }
         catch (ScriptException e) { throw TaskEngine.newRuntimeException(e, where); }
      else engine.eval(script, occurrence.bindings, where); 
   }

}

