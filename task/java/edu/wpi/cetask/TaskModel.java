/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.cetask;

import java.io.*;
import java.net.URL;
import java.util.*;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import org.w3c.dom.Node;
import edu.wpi.cetask.TaskClass.Grounding;

public class TaskModel extends Description {
   
   TaskModel (Node node, XPath xpath, TaskEngine engine) { 
      super(node, xpath, engine);     
   }
   
   public TaskModel (String namespace, TaskEngine engine) {
      super(null, null, namespace, engine);
   }
   
   private URL source; 
   
   public void setSource (URL source) { this.source = source; }
   
   public URL getSource () { return source; }
   
   final Map<String,TaskClass> tasks = new HashMap<String,TaskClass>();
   
   public Collection<TaskClass> getTaskClasses () { return tasks.values(); }
   
   public TaskClass getTaskClass (String id) { return tasks.get(id); }

   final Map<String,DecompositionClass> decomps = new HashMap<String,DecompositionClass>();

   public Collection<DecompositionClass> getDecompositionClasses () { return decomps.values(); }
   
   public DecompositionClass getDecompositionClass (String id) { return decomps.get(id); }
   
   List<Grounding> scripts = Collections.emptyList();
   
   public List<Grounding> getGroundingAll () { return scripts; }
   
   @Override
   public String toString () { return getNamespace(); }
   
   private final Properties properties = new Properties(engine.properties);
   
   public Properties getProperties () { return properties; }
   
   public String getProperty (String key) {
      String property = properties.getProperty(key);
      return property == null ? null : property.trim();  // remove accidental trailing spaces
   }
   
   public Integer getProperty (String key, Integer defaultValue) {
      String value = getProperty(key);
      return value == null ? defaultValue : (Integer) Integer.parseInt(value);
   }
   
   public Long getProperty (String key, Long defaultValue) {
      String value = getProperty(key);
      return value == null ? defaultValue : (Long) Long.parseLong(value);
   }
   
   public Float getProperty (String key, Float defaultValue) {
      String value = getProperty(key);
      return value == null ? defaultValue : (Float) Float.parseFloat(value);
   }
   
   public Double getProperty (String key, Double defaultValue) {
      String value = getProperty(key);
      return value == null ? defaultValue : (Double) Double.parseDouble(value);
   }
   
   public Boolean getProperty (String key, Boolean defaultValue) {
      String value = getProperty(key);
      return value == null ? defaultValue : (Boolean) Utils.parseBoolean(value);
   }
   
   public String removeProperty (String key) {
      return (String) properties.remove(key);
   }
   
   public void setProperty (String key, String value) {
      properties.setProperty(key, value);
   }
   
   public void setProperty (String key, int value) {
      setProperty(key, Integer.toString(value));
   }
   
   public void setProperty (String key, long value) {
      setProperty(key, Long.toString(value));
   }
   
   public void setProperty (String key, float value) {
      setProperty(key, Float.toString(value));
   }
   
   public void setProperty (String key, double value) {
      setProperty(key, Double.toString(value));
   }
   
   public void setProperty (String key, boolean value) {
      setProperty(key, Boolean.toString(value));
   }
   
   public void storeProperties () {
      if ( source == null ) throw new UnsupportedOperationException("Task model source is null: "+this);
      if ( !"file".equals(source.getProtocol() )) 
         throw new UnsupportedOperationException("Task model source is not file: "+source);
      try { 
         properties.store(new FileOutputStream(Utils.replaceEndsWith(source.getFile(), ".xml", ".properties")),
                          namespace);
      } catch (IOException e) { throw new RuntimeException(e); } 
   }
   
   public static class Error extends RuntimeException {
      public Error (Description description, String error) { 
         super(description+": "+error);
      }
   }
   
   private final List<String> ids = new ArrayList<String>();
   
   protected static String getPropertyId (String id) {
         return (id.startsWith("edu.wpi.disco.lang.") ?
            // spare users typing this prefix in properties files
            id.substring(19) : id).replace('$', '.');
   }
      
   protected static String parseId (Node node, XPath xpath) {
         return xpath(node, xpath, "./@id");
   }
   
   /**
    * Base class for members of task model with id's
    */
   public abstract class Member extends Description {
      
      final private String id; 
      final private QName qname;
      
      /**
       * @return the raw id string from the definition of this member.
       * 
       * @see #getQName()
       */
      public String getId () { return id; }

      /**
       * @return the qualified name of this member
       */
      public QName getQName () { return qname; }
      
      protected Member (Node node, XPath xpath) { 
         this(node, xpath, parseId(node, xpath));
      }
     
      // placeholder for creating members without XML
      protected Member (String id) {
         this(null, null, id);
      }
      
      protected Member (Node node, XPath xpath, String id) {
         super(node, xpath, TaskModel.this.getNamespace(), TaskModel.this.engine);
         this.id = id.length() > 0 ? id : "**ROOT**";
         qname = new QName(TaskModel.this.getNamespace(), id);
         ids.add(id);  
      }
      
      public TaskModel getModel () { return TaskModel.this; }   
      
      public String getPropertyId () { return TaskModel.getPropertyId(id); }
     
      public String getProperty (String key) {         
        return TaskModel.this.getProperty(getPropertyId()+key);
      }

      public Integer getProperty (String key, Integer defaultValue) {
        return TaskModel.this.getProperty(getPropertyId()+key, defaultValue);
      }
      
      public Long getProperty (String key, Long defaultValue) {
         return TaskModel.this.getProperty(getPropertyId()+key, defaultValue);
      }
      
      public Float getProperty (String key, Float defaultValue) {
         return TaskModel.this.getProperty(getPropertyId()+key, defaultValue);
      }
      
      public Double getProperty (String key, Double defaultValue) {
         return TaskModel.this.getProperty(getPropertyId()+key, defaultValue);
      }
      
      public Boolean getProperty (String key, Boolean defaultValue) {
         return TaskModel.this.getProperty(getPropertyId()+key, defaultValue);
      }

      /**
       * @return all names of all properties with @ keys that apply to this task or
       * decomposition class
       */
      public Set<String> getProperties () {
         Set<String> names = properties.stringPropertyNames();
         Iterator<String> iterator = names.iterator();
         while (iterator.hasNext()) {
            String next = iterator.next();
            // TODO should probably figure out a regex here
            int at = next.indexOf('@');
            if ( at < 0 ) { // no @
               System.err.println("Warning: Property with no @key in "+TaskModel.this+": "+next);
               iterator.remove(); 
               continue; } 
            String id = getPropertyId();
            int len = id.length();
            if ( at == len && next.startsWith(id) ) continue; // id@
            int open = next.indexOf('(');
            if ( open < 0 || open > at ) { iterator.remove(); continue; } // no (
            if ( open == len && next.startsWith(id) ) continue; // id(
            int comma = next.indexOf(',');
            if ( (comma-open) == len+1 && next.regionMatches(open+1, id, 0, len) ) continue; // ...(id,
            int close = next.indexOf(')');
            if ( close < 0 || close < open ) {
               System.err.println("Warning: Property with mismatched parens in "+TaskModel.this+": "+next);
               iterator.remove(); continue;
            }
            if ( (close-open) == len+1 && next.regionMatches(open+1, id, 0, len) ) continue; // ...(id)
            open = next.indexOf('(', open+1);
            if ( open < 0 || open > at ) { iterator.remove(); continue; } // no nested parens
            close = next.lastIndexOf(')', close);
            if ( close < 0 ) {
               System.err.println("Warning: Property with mismatched parens in "+TaskModel.this+": "+next);
               iterator.remove(); continue;
            }
            if ( (close-open) == len+1 && next.regionMatches(open+1, id, 0, len) )  continue; // ...(...(id)
            iterator.remove();
         }
         return names;
      }
     
      public void setProperty (String key, String value) {
         TaskModel.this.setProperty(getPropertyId()+key, value);
      }

      public void setProperty (String key, int value) {
         TaskModel.this.setProperty(getPropertyId()+key, value);
      }
      
      public void setProperty (String key, long value) {
         TaskModel.this.setProperty(getPropertyId()+key, value);
      }
      
      public void setProperty (String key, float value) {
         TaskModel.this.setProperty(getPropertyId()+key, value);
      }
      
      public void setProperty (String key, double value) {
         TaskModel.this.setProperty(getPropertyId()+key, value);
      }
      
      public void setProperty (String key, boolean value) {
         TaskModel.this.setProperty(getPropertyId()+key, value);
      }

      @Override
      public boolean equals (Object object) {
         return object instanceof Member
           // works across different engines
           && namespace.equals(((Member) object).getNamespace()) 
           && id.equals(((Member) object).getId());
      }
      
      @Override
      public int hashCode () { return node.hashCode(); }
      
      /**
       * A task or decomposition class is internal if its id starts with an underscore.  
       * Can be overridden with @internal property.
       * Internal types are suppressed in various places, such as history printing,
       * ttsay, etc.  Also the default key values that control plugins, such
       * as @ProposeHow are false for internal tasks, rather than true.
       */
      public boolean isInternal () { 
         return getProperty("@internal", getId().charAt(0) == '_');
      }
   }
     
   public static class Init extends Script {

      Init (Node node, XPath xpath, TaskEngine engine) {
         this(parseText(node, xpath), engine);
      }
      
      public Init (String script, TaskEngine engine) {
         super(script, null); // don't compile
      }
      
      @Override
      public TaskModel getEnclosing () { return (TaskModel) super.getEnclosing(); }      
   }
}
