/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.cetask;

import java.io.PrintStream;
import java.util.*;
import java.util.regex.*;
import javax.script.ScriptException;
import javax.xml.namespace.QName;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import org.w3c.dom.*;
import edu.wpi.cetask.ScriptEngineWrapper.Compiled;

/**
 * Base class for parts of a Task Model Description
 */
public abstract class Description { //TODO: temporarily public for Anahita
 
   // NB: Some (any) prefix *must* be provided for elements in all
   // XPath expressions, because of way that XPath works with XML namespaces
   
   protected final String namespace;
   public String getNamespace () { return namespace; }

   /**
    * Node in DOM
    */
   protected final Node node;
   
   private static final TransformerFactory factory = TransformerFactory.newInstance();
   
   public void print (PrintStream stream) {
      try {
         Transformer transformer = factory.newTransformer();
         transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
         transformer.setOutputProperty(OutputKeys.INDENT, "yes");
         transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
         transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
         transformer.transform(new DOMSource(node), new StreamResult(stream));
      } catch (Exception e) { Utils.rethrow("Error printing "+this, e); }
   }
   
   /**
    * For searching in DOM (includes corrected namespace context)
    */
   protected final XPath xpath;
   
   /**
    * Task engine in which DOM resides
    */
   protected final TaskEngine engine; 
   public TaskEngine getEngine () { return engine; }
   
   protected Description (Node node, XPath xpath, TaskEngine engine) {
      this(node, xpath, parseAbout(node, xpath), engine);
   }
   
   protected Description (Node node, XPath xpath, String namespace, TaskEngine engine) {
      if ( xpath != null && node == null ) throw new IllegalArgumentException("Null node");
      this.node = node;
      this.xpath = xpath;
      this.namespace = namespace;
      this.engine = engine;
   }

   protected static String parseAbout (Node node, XPath xpath) {
      return xpath(node, xpath, "/n:taskModel/@about");
   }

   // placeholder for creating descriptions without XML
   protected Description (TaskEngine engine, String namespace) {
      this.engine = engine;
      this.namespace = namespace;
      this.node = null;
      this.xpath = null;
   }
   
   // convenient utilities for subclasses

   public TaskClass resolveTaskClass (String qname) {
      return resolveTaskClass(node, namespace, qname, engine);
   }
   
   protected static TaskClass resolveTaskClass (Node node, String namespace, String qname, TaskEngine engine) {
      return engine.resolveTaskClass(parseQName(node, namespace, qname));
   }
   
   protected Object xpath (String path, QName returnType) {
      return xpath(node, xpath, path, returnType);
   }
   
   protected static Object xpath (Node node, XPath xpath, String path, QName returnType) {
      try { return xpath.evaluate(path, node, returnType); } 
      catch (XPathExpressionException e) { throw new RuntimeException(e); }
   }
   
   protected String xpath (String expression) {
      return xpath(node, xpath, expression);
   }
   
   protected static String xpath (Node node, XPath xpath, String expression) {
      try { return xpath.evaluate(expression, node); } 
      catch (XPathExpressionException e) { throw new RuntimeException(e); }
   }

   protected List<String> xpathValues (String path) {
      return xpathValues(node, xpath, path);
   }
   
   protected static List<String> xpathValues (Node node, XPath xpath, String path) {
      List<String> values = new ArrayList<String>();
      for (Node n : xpathNodes(node, xpath, path))
         values.add(n.getNodeValue());
      return values;
   }
   
   protected List<Node> xpathNodes (String path) {
      return xpathNodes(node, xpath, path);
   }
   
   protected static List<Node> xpathNodes (Node node, XPath xpath, String path) { 
      try {
         List<Node> nodes = new ArrayList<Node>();
         NodeList nodelist = (NodeList) xpath.evaluate(path, node, XPathConstants.NODESET);
         for (int i = 0; i < nodelist.getLength(); i++) // preserve order
            nodes.add(nodelist.item(i));
         return nodes;
      } catch (XPathExpressionException e) { throw new RuntimeException(e); }
   }
   
   protected QName parseQName (String qname) {
      return parseQName(node, namespace, qname);
   }
   
   protected static QName parseQName (Node node, String namespace, String qname) {
      String prefix;
      int i = qname.indexOf(':');
      if ( i >= 0 ) {
         prefix = qname.substring(0, i);
         String ns = node.lookupNamespaceURI(prefix);
         if ( ns == null ) 
            throw new IllegalArgumentException("Unknown namespace prefix "+qname
                  +" in "+namespace); 
         return new QName(ns, qname.substring(i+1), prefix); 
      } else // default namespace is from 'about', not xmlns
         return new QName(namespace, qname);  
   }
     
   protected PrintStream getOut () { return engine.getOut(); }
      
   protected PrintStream getErr () { return engine.getErr(); }
   
   private Description enclosing;
   protected String where;

   public Description getEnclosing () { return enclosing; }

   void setEnclosing (Description enclosing) {
      if ( this.enclosing != null ) throw new IllegalArgumentException("Already has enclosing: "+this);
      if ( enclosing != null ) {
         this.enclosing = enclosing;
         where = (enclosing instanceof TaskModel.Member ? 
            Utils.getSimpleName(((TaskModel.Member) enclosing).getId()) : enclosing.getNamespace())
            +" "+Utils.getSimpleName(getClass(), true);
      }
   }
   
   interface Slot {
      
      /**
       * @return the name of this slot.
       */
      String getName ();
      
      /**
       * @return the type of this slot
       * 
       * @see #getJava()
       */
      String getType ();
      
      /**
       * @return the Java class of the type of this slot, if it is a Java type
       */
      Class<?> getJava ();
      
      /**
       * @return the enclosing context of this slot, such as the task class, step
       *         or decomposition class.
       */
      Description getEnclosing ();
      
      /**
       * Test whether this slot is declared.
       */
      boolean isDeclared ();
      
      /**
       * Return value of this slot in given task.
       *
       * @see Task#getSlotValue(String)
       */
      Object getSlotValue (Task task); 

      /**
       * Test whether this slot has defined value in given task.
       * 
       * @see Task#isDefinedSlot(String)
       */
      boolean isDefinedSlot (Task task);

      /**
       * Set the value of this slot in given task to given value.
       * 
       * @see Task#setSlotValue(String,Object)
       */
      Object setSlotValue (Task task, Object value);

      /**
       * Set the value of this slot in given task to given value.
       * 
       * @see Task#setSlotValue(String,Object,boolean)
       */
      void setSlotValue (Task task, Object value, boolean check);

      /**
       * Set the value of this slot in given task to result of evaluating given JavaScript
       * expression.
       * 
       * @see Task#setSlotValueScript(String,String,String)
       */
      void setSlotValueScript (Task task, String expression, String where);

      /**
       * Make this slot undefined in given task.
       * 
       * @see Task#removeSlotValue(String)
       */
      void deleteSlotValue (Task task);
   }
   
   interface Input extends Slot {
      
      /**
       * Test whether this input is optional.
       */
      boolean isOptional (); 

      /**
       * @return the corresponding modified output, if any.
       */
      Output getModified ();
      
   }
   
   interface Output extends Slot {}
   
   public static abstract class Script extends Description {

      protected final String script;
      protected final Compiled compiled;
         
      public String getScript () { return script; }
      
      protected Script (Node node, XPath xpath, String script, TaskEngine engine) {
         this(script, engine, "compiling "+TaskModel.parseId(node, xpath));
      }
      
      protected Script (String script, TaskEngine engine) {
         this(script, engine, "compiling");
      }
      
      private Script (String script, TaskEngine engine, String where) {
         super(engine, null);
         this.script = script;
         compiled =  (engine != null && !TaskEngine.DEBUG) ? 
            engine.compile(script, where) : null;
      }
      
      /**
       * Evaluate the grounding script associated with given occurrence.
       * 
       * Note: This method is thread-safe, which means that it can be
       * safely called on other than the Disco thread (as long, of course as the
       * script does not reference $disco).  Grounding scripts typically
       * need to be executed on the application (e.g., game) thread.
       */
      public void eval (Task occurrence) {
         if ( compiled != null)  
            // assuming that $platform and $deviceType already in task.bindings
            try { compiled.eval(occurrence.bindings); }
            catch (ScriptException e) { throw TaskEngine.newRuntimeException(e, where); }
         else engine.eval(script, occurrence.bindings, where); 
      }
      
      protected static String parseText (Node node, XPath xpath) {
         return xpath(node, xpath, ".");
      }
      
      @Override
      public String toString () {
         return "<#"+Utils.getSimpleName(getClass(), true)+" "+getScript()+">";
      }
   }
   
   public static abstract class Condition extends Script {
      
      private final boolean strict;
      private final List<String> slots = new ArrayList<String>();
     
      protected Condition (String script, boolean strict, TaskEngine engine) {
         super(script, engine);
         this.strict = strict;
      }
      
      public boolean isStrict () { return strict; }
      
      protected static boolean isStrict (TaskEngine engine, String id) {
         return engine.getProperty(TaskModel.getPropertyId(id)+"@strict", true);
      }
      
      protected abstract boolean check (String slot);

      private final static Pattern pattern = // to match $this.slot
         Pattern.compile("\\$this\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*(\\.|\\)|\\s|\\Z)");      

      @Override
      void setEnclosing (Description enclosing) {
         super.setEnclosing(enclosing);
         if ( strict && 
               (enclosing instanceof TaskClass || enclosing instanceof DecompositionClass) ) {
            Matcher matcher = pattern.matcher(script);
            while ( matcher.find() ) {
               String slot = matcher.group().substring(6).trim();
               if ( slot.endsWith(".") || slot.endsWith(")") )
                  slot = slot.substring(0, slot.length()-1);
               if ( check(slot) ) slots.add(slot);
            }
         }
      }
      
      public Boolean evalCondition (Task task) {
         if ( strict ) {
            for (String slot : slots)
               if ( !task.isDefinedSlot(slot) ) return null;
         } 
         return task.evalCondition(script, compiled, task.bindings, where);
      }
   }

}