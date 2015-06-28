/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco;

import edu.wpi.cetask.*;
import edu.wpi.disco.lang.Nested;
import edu.wpi.disco.plugin.DefaultPlugin;

import java.util.*;

public class Agenda {
   
   private final List<Plugin> plugins = new ArrayList<Plugin>();

   private final Actor who;
   
   private boolean onlyBest;
   private int bestPriority;
   private Plugin.Item bestItem;

   public Agenda (Actor who) { this.who = who; }
   
   public void add (Plugin plugin) {
      plugins.add(plugin); 
   }   
   
   private static final Plugin dummy = new Agenda(null).new Plugin(0) {
      @Override
      public List<Plugin.Item> apply (Plan plan) { return null; }
   };
   
   /**
    * For creating an item outside of a plugin (note priority is 0).
    */
   public static Plugin.Item newItem (Task task, Plan contributes) {
      return dummy.new Item(task, contributes);
   }

   public abstract class Plugin {
      
      final private int priority;

      protected Plugin (int priority) {
         this.priority = priority;
         Agenda.this.add(this); 
      }
      
      /**
       * Apply plugin to given plan.
       * @return task list of items or null
       */
      public abstract List<Plugin.Item> apply (Plan plan);
      
      protected Actor getWho () { return who; }
      protected boolean self () { return who == interaction.getExternal(); } 

      public Agenda getAgenda () { return Agenda.this; }
      
      public Disco getDisco () { return Agenda.this.getDisco(); }
      
      public Interaction getInteraction () { return interaction; }
      
      // for convenience in writing plugins
      
      /**
       * Calls {@link Task#getProperty(String,Boolean)} with default value of false
       * for internal tasks and true otherwise.
       */
      protected boolean getPluginProperty (Task task, String key) {
         return task.getProperty(key, !task.isInternal());
      }
      
      protected Boolean getGenerateProperty (Class<? extends Nested> nested, 
                        DecompositionClass decomp, Boolean defaultValue) {
         return Nested.getProperty(nested, decomp, "@generate", defaultValue);
      }
  
      protected boolean getGenerateProperty (Class<? extends Nested> nested, Task task) {
         return getGenerateProperty(nested, task, !task.isInternal());
      }

      protected boolean getGenerateProperty (Class<? extends Nested> nested, Task task, String slot) {
         return Nested.getProperty(nested, task, slot, "@generate", !task.isInternal());
      }

      protected Boolean getGenerateProperty (Class<? extends Nested> nested,
                                             Task task, Boolean defaultValue) {
         return Nested.getProperty(nested, task, "@generate", defaultValue);
      }
         
      protected String getGeneratePropertyString (Class<? extends Nested> nested, 
            Task task, String defaultValue) {
         return Nested.getProperty(nested, task, "@generate", defaultValue);
      }

      protected boolean isAuthorized (Plan plan) {
         return who.isAuthorized(plan, interaction);
      }
      
      protected boolean isSelf (Task task) {
         return who.isSelf(task, interaction);
      }
      
      protected boolean isOther (Task task) {
         return who.isOther(task, interaction);
      }
      
      protected boolean canSelf (Task task) {
         return who.canSelf(task, interaction);
      }
      
      protected boolean canOther (Task task) {
         return who.canOther(task, interaction);
      }
      
      @Override
      public String toString () { return Utils.getSimpleName(getClass(), true); }
      
      public class Item {

         public int getPriority () { return priority; }
         
         public Plugin getPlugin () { return Plugin.this; }
         
         public final Task task;
         
         /**
          * Optional preformatted string for use in menu.  Note that translation
          * table is still applied afterwards. 
          */
         public final String formatted; 

         /**
          * Either null or the plan to which this item's task will contribute (usually
          * the plan the plugin was applied to).  Nature of contribution depends
          * depends on the task.
          */
         public final Plan contributes;

         public Item (Task task, Plan contributes) { this(task, contributes, null); }
         
         public Item (Task task, Plan contributes, String formatted) {
            if ( task == null ) 
               throw new IllegalArgumentException("Agenda.Plugin.Item task may not be null");
            this.task = task;
            this.contributes = contributes;
            this.formatted = formatted;
         }

         /** Eliminate duplicates based on tasks only. Note this is inconsistent 
          * with "natural ordering" of set as defined by compareTo.
          * 
          * @see java.lang.Object#equals(java.lang.Object)
          */
         @Override
         public boolean equals (Object object) {
            return object instanceof Item && ((Item) object).task.equals(task);
         }

         @Override
         public int hashCode () { return task.hashCode(); }
         
         @Override
         public String toString () {
            StringBuilder buffer = new StringBuilder("[");
            buffer.append(getPlugin()).append(":").append(priority)
               .append("] ").append(task.toString());
            return buffer.toString();
         }
      }
   }
   
   private Interaction interaction;
   
   public Disco getDisco () { return interaction.getDisco(); }
   
   public Plugin getPlugin (Class<? extends Plugin> cls) {
      for (ListIterator<Plugin> i = plugins.listIterator(); i.hasNext();) {
         Plugin plugin = i.next();
         if ( plugin.getClass().isAssignableFrom(cls) )
            return plugin;
      }
      return null;
   }
      
   public boolean remove (Class <? extends Plugin> cls) {
      Plugin plugin = getPlugin(cls);
      return plugin != null ? plugins.remove(plugin) : false;
   }
   
   // TODO optimize by caching last call and checking if discourse state 
   //      has changed since
   
   /**
    * Thread-safe method to generate tasks for this agenda.
    */
   public List<Plugin.Item> generate (Interaction interaction) {
      onlyBest = false;
      // used linked map to preserve order and eliminate duplicates
      return generate(interaction, new LinkedHashMap<Task,Plugin.Item>());
   }
 
   /**
    * Thread-safe method to generate highest-priority task for this agenda.
    */
   public Plugin.Item generateBest (Interaction interaction) {
      onlyBest = true;
      bestItem = null;
      bestPriority = Integer.MIN_VALUE;
      generate(interaction, null);
      return bestItem;
   }
   
   private List<Plugin.Item> generate (Interaction interaction, Map<Task, Plugin.Item> items) {
      synchronized (interaction) { // typically used in toplevel dialogue loop
         this.interaction = interaction;
         Disco disco = interaction.getDisco();
         Stack<Segment> stack = disco.getStack();
         int pop = 0; // for virtual popping
         Plan focus = disco.getFocus(), oldFocus;
         // first visit current focus
         if ( focus != null ) visitFocus(focus, items, null);
         // "pop" implicit Accept, if any
         oldFocus = focus;
         focus = disco.getFocus(true);
         if ( focus != oldFocus ) {
            pop++;
            if ( focus != null ) visitFocus(focus, items, oldFocus); 
         }
         // then "pop" exhausted goals
         while ( isEmpty(items) && focus != null && focus.isExhausted() ) {
            pop++;
            oldFocus = focus;
            focus = stack.get(stack.size()-(pop+1)).getPlan();
            if ( focus != null ) visitFocus(focus, items, oldFocus);
         }
         if ( isEmpty(items) ) {
            if ( focus != null ) // start at top of current tree 
               visit(disco.getTop(focus), items, focus);
            else // or consider all toplevel trees 
               for (Plan top : disco.getTops()) {
                  // TODO this extra isLive test will not be needed when
                  //      handling of optionals fixed (see Plan.getLiveDescendants)
                  if ( top.isLive() ) visit(top, items, null);
               }
         }
         // finally run default plugins 
         defaultPlugins(items);
         if ( onlyBest ) return null;
         List<Plugin.Item> results = new ArrayList<Plugin.Item>(items.values());
         Collections.sort(results,
               new Comparator<Plugin.Item>() {
            @Override
            public int compare (Plugin.Item item1, Plugin.Item item2) { 
               return item1.getPriority() < item2.getPriority() ? 1 :
                  item1.getPriority() > item2.getPriority() ? -1 : 0;
            }});
         return results;
      }
   }
  
   // public for DiscoRT
   public void defaultPlugins (Map<Task,Plugin.Item> items) {
      visitPlugins(null, items);
   }

   private boolean isEmpty (Map<Task,Plugin.Item> items) {
      return onlyBest ? bestItem == null : items.isEmpty();
   }
   
   private void visitFocus (Plan plan, Map<Task,Plugin.Item> items, Plan exclude) {
      // unroll recursion one step so always looks inside of done 
      // root plan for trailing optionals 
      for (Plan child : plan.getChildren()) visit(child, items, exclude);
      // note visiting focus last means that inputs of steps of decomposition will
      // be asked about first (see AskWhatNoBindingPlugin)
      if ( plan.isLive() ) visitPlugins(plan, items);
   }
   
   // public for use in DecompositionPlugin
   public void visit (Plan plan, Map<Task,Plugin.Item> items, Plan exclude) {
      // see note in Plan re trailing optional steps for why hasLiveDescendants needed
      if ( plan != exclude && (plan.isLive() || plan.hasLiveDescendants()) ) {
         boolean optional = plan.isOptional();
         if ( optional ) visitPlugins(plan, items); // so ask about optional parent first
         // depth-first recursion
         for (Plan child : plan.getChildren()) visit(child, items, exclude); 
         if ( !optional ) visitPlugins(plan, items); // see comment in visitFocus 
      }
   }

   private void visitPlugins (Plan plan, Map<Task,Plugin.Item> items) { 
      boolean defaults = plan == null;
      for (Plugin plugin : plugins) {
         if ( (plugin instanceof DefaultPlugin) != defaults ) continue;
         if ( onlyBest && plugin.priority <= bestPriority ) continue;
         List<Plugin.Item> list = plugin.apply(plan);
         if ( list != null && !list.isEmpty() ) {
            if ( onlyBest ) {
               bestItem = list.get(0);
               bestPriority = plugin.priority;
            } else for (Plugin.Item item : list) {
               Plugin.Item old = items.get(item.task);
               if ( old == null || item.getPriority() > old.getPriority() )
                  // eliminate duplicates
                  items.put(item.task, item);
            }
         }
      }
   }
}
