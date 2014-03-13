/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.lang;

import edu.wpi.cetask.*;
import edu.wpi.disco.Disco;

/**
 * Base class for Propose.Nested and Reject, both of which take nested Task 
 */
public abstract class Nested extends Utterance {
   
   // for extensions
   protected Nested (Class<? extends Nested> cls, Disco disco,
                      Decomposition decomp, String step, boolean repeat) {
      super(cls, disco, decomp, step, repeat);
   }
   
   // for extensions
   protected Nested (Class<? extends Nested> cls, Disco disco,  Boolean external) { 
      super(cls, disco, external);
   }

   protected abstract Task getNested ();
   
   @Override
   public boolean contributes (Plan plan) {
      if ( super.contributes(plan) ) return true;
      Task nested = getNested();
      return nested != null && nested.contributes(plan);
   }

   @Override
   public boolean contributes (TaskClass type) {
      if ( super.contributes(type) ) return true;
      Task nested = getNested();
      return nested != null && nested.getType() == type;
   }

   @Override
   public boolean isPathFrom (TaskClass type) { 
      if ( super.isPathFrom(type) ) return true;
      Task nested = getNested();
      return nested != null && nested.isPathFrom(type);
   }
 
   protected abstract String getKey ();
   
   @Override 
   public String formatTask () { return formatNested(); }

   protected String formatNested () {
      String format = getDisco().getFormat(this);
      return format == null ? formatNestedDefault(true) : formatTask(format, null); 
   }
   
   // TODO provide better toHistoryString methods here and subclasses
   
   protected String formatNestedDefault (boolean formatTask) {
      // default formatting
      StringBuilder buffer = new StringBuilder();
      appendKeySp(buffer, getKey());
      buffer.append(formatTask ? getNested().formatTask() : getNested().format());
      return buffer.toString();
   }
   
   @Override
   public String getProperty (String key) {
      try { 
         getProperty(new StringBuilder(), key, this, 0, null); 
         return null;
      } catch (PropertyNested p) { return p.property; }
   }
   
   @Override
   public Boolean getProperty (String key, Boolean defaultValue) {
      String value = getProperty(key);
      return value == null ? (Boolean) defaultValue : 
         (Boolean) Utils.parseBoolean(value);
   }

   public static Boolean getProperty (Class<? extends Nested> nested, Task task, 
                                      String key, Boolean defaultValue) {
      try { 
         getProperty(nested, key, task); 
         return defaultValue;
      } catch (PropertyNested p) { return Utils.parseBoolean(p.property); }
   }

   public static Boolean getProperty (Class<? extends Nested> nested, Task task, 
                                      String slot, String key, Boolean defaultValue) {
      try { 
         getProperty(nested, key, task, slot); 
         return defaultValue;
      } catch (PropertyNested p) { return Utils.parseBoolean(p.property); }
   }

   public static String getProperty (Class<? extends Nested> nested, Task task, String key,
                                     String defaultValue) {
      try { 
         getProperty(nested, key, task); 
         return defaultValue;
      } catch (PropertyNested p) { return p.property; }
   }
 
   public static void setProperty (Class<? extends Nested> nested, TaskModel.Member member,
         String key, String value) {
      member.getModel().setProperty(getId(nested)+'('+member.getPropertyId()+')'+key, value);
   }

   public static void setProperty (Class<? extends Nested> nested, TaskModel.Member member,
         String key, Boolean value) {
      setProperty(nested, member, key, value == null ? "null" : Boolean.toString(value));
   }
   
   private static void getProperty (Class<? extends Nested> nested, String key, Task task) 
                                   throws PropertyNested {
      getProperty(new StringBuilder(getId(nested)).append('('), key, task, 1, null);
   }
   
   private static void getProperty (Class<? extends Nested> nested, String key, Task task,
                                    String slot) throws PropertyNested {
      getProperty(new StringBuilder(getId(nested)).append('('), key, task, 1, slot);
   }
   
   public static String getProperty (Class<? extends Nested> nested, 
                        DecompositionClass decomp, String key) {
      StringBuilder buffer = new StringBuilder(getId(nested));
      buffer.append('(').append(decomp.getGoal()).append(',')
            .append(decomp.getPropertyId()).append(')').append(key);
      return decomp.getModel().getProperty(buffer.toString());
   }

   public static Boolean getProperty (Class<? extends Nested> nested, 
                         DecompositionClass decomp, String key, Boolean defaultValue) {
      String value = getProperty(nested, decomp, key);
      return value == null ? (Boolean) defaultValue : 
         (Boolean) Utils.parseBoolean(value);
   }
   
   private static String getId (Class<? extends Nested> nested) {
      try { return 
        ((TaskClass) nested.getDeclaredField("CLASS").get(null)).getPropertyId();
      } catch (NoSuchFieldException e) { throw new IllegalStateException(e); }
        catch (IllegalAccessException e) { throw new IllegalStateException(e); }            
   }
   
   static String getFormat (Class<? extends Nested> nested, Task task,
                            DecompositionClass decomp) {
      return Disco.processFormat(task, getProperty(nested, decomp, "@format"),
                                 decomp.getPropertyId()+"@format");
   }

   private static class PropertyNested extends Throwable {
      private final String property;
      private PropertyNested (String property) { this.property = property; }
   }
   
   @Override
   protected String getFormat () {
      String format = getPropertySlot() == null ? super.getFormat() : getProperty("@format"); 
      // allow empty string to undo format
      return format != null && format.length() > 0 ? format : null;
   }
   
   // for overriding in Ask
   protected Task getPropertyNested () { return getNested(); } 
   
   private static void getProperty (StringBuilder buffer, String key, Task task,
                                    int level, String slot) throws PropertyNested {
      buffer.append(task.getType().getPropertyId());
      if ( slot != null ) buffer.append(',').append(slot);
      int length = buffer.length();
      if ( task instanceof Nested ) { 
         // more specific properties override more general ones
         Task nested = ((Nested) task).getPropertyNested();
         if ( nested != null ) {
            buffer.append('(');
            getProperty(buffer, key, nested, level+1, task.getPropertySlot()); // recursive call
            buffer.delete(length, buffer.length()); // remove failed probes
         }
      }
      for (int i = level; i-- > 0;) buffer.append(')');
      // do not use Disco.getProperty(), since it returns key in DEBUG mode
      String value = task.getType().getModel().getProperty(buffer+key);
      if ( value != null ) throw new PropertyNested(value);
   }
 }
