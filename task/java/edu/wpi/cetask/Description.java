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
   
   static final String about = "/n:taskModel/@about";
   
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
      if ( node == null ) throw new IllegalArgumentException("Null node");
      this.node = node;
      this.xpath = xpath;
      this.engine = engine;
      this.namespace = xpath(about);
   }

   // placeholder for creating descriptions without XML
   protected Description (TaskEngine engine, String namespace) {
      this.engine = engine;
      this.namespace = namespace;
      this.node = null;
      this.xpath = null;
   }
   
   public TaskClass resolveTaskClass (String qname) {
      return engine.resolveTaskClass(parseQName(qname));
   }
   
   // convenient utilities for subclasses

   protected Object xpath (String path, QName returnType) {
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
      List<String> values = new ArrayList<String>();
      for (Node node : xpathNodes(path))
         values.add(node.getNodeValue());
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
      String prefix;
      int i = qname.indexOf(':');
      if ( i >= 0 ) {
         prefix = qname.substring(0, i);
         String ns = node.lookupNamespaceURI(prefix);
         if ( ns == null ) 
            throw new IllegalArgumentException("Unknown namespace prefix "+qname
                  +" in "+getNamespace()); 
         return new QName(ns, qname.substring(i+1), prefix); 
      } else // default namespace is from 'about', not xmlns
         return new QName(getNamespace(), qname);  
   }
   
   protected static abstract class Script extends Description {

      protected final String script;
      protected final Compiled compiled;
      protected String where;
      private Description enclosing;
      
      public String getScript () { return script; }

      public Description getEnclosing () { return enclosing; }

      void setEnclosing (Description enclosing) {
         if ( this.enclosing != null ) throw new IllegalArgumentException("Already has enclosing: "+this);
         this.enclosing = enclosing;
         where = (enclosing instanceof TaskModel.Member ? 
            Utils.getSimpleName(((TaskModel.Member) enclosing).getId()) : enclosing.getNamespace())
            +" "+Utils.getSimpleName(getClass(), true);
      }
      
      protected Script (Node node, XPath xpath, String script, TaskEngine engine) {
         this(script, engine, "compiling "+TaskModel.parseId(node, xpath));
      }
      
      protected Script (String script, TaskEngine engine) {
         this(script, engine, "compiling");
      }
      
      private Script (String script, TaskEngine engine, String where) {
         super(engine, null);
         this.script = script;
         compiled =  TaskEngine.isCompilable() ? engine.compile(script, where) : null;
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
   
   protected static abstract class Condition extends Script {
      
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
         Pattern.compile("\\$this\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*");      

      @Override
      void setEnclosing (Description enclosing) {
         super.setEnclosing(enclosing);
         if ( enclosing instanceof TaskClass ) {
            Matcher matcher = pattern.matcher(script);
            while ( matcher.find() ) {
               String slot = matcher.group().substring(6);
               if ( check(slot) ) slots.add(slot);
            }
         }
      }
      
      public Boolean evalCondition (Task task) {
         if ( strict ) {
            for (String slot : slots)
               if ( !task.isDefinedSlot(slot) ) return null;
         } 
         return task.evalCondition(script, compiled, where);
      }
   }

}