package edu.wpi.disco.ldt;

import org.w3c.dom.*;
import javax.xml.namespace.QName;
import javax.xml.parsers.*;
import edu.wpi.disco.*;
import edu.wpi.disco.Agenda.Plugin;
import edu.wpi.disco.plugin.*;

public class UnifiedAgent extends Agent {
   
   private static Disco disco;
   
   // use this main instead of Interaction.main() for console debugging
   // also see bin/learn-do-teach bash script
   public static void main (String[] args) {
      Interaction interaction = new Interaction(new UnifiedAgent("agent"),
            new User("human"), args.length > 0 && args[0].length() > 0 ? args[0]
               : null);
      interaction.setOk(false);
      disco = interaction.getDisco();
      // make glosses agree with figures
      disco.setProperty("achieve@word", "do");
      disco.setProperty("execute@word", "do");
      interaction.start(true);
   }

   public UnifiedAgent (String name) { 
      super(name);
      // prevent asking about recipes
      getAgenda().remove(AskHowPlugin.class);
      // announce when done current highest-level non-primitive (high priority)
      new ProposeDonePlugin(agenda, 300);
   }
    
   @Override
   protected boolean synchronizedRespond (Interaction interaction, boolean ok, boolean guess, boolean retry) {
      // override default turn-taking to simply do one action if possible
      Plugin.Item item = respondIf(interaction, true, retry); // guess true
      if ( item == null ) return false;
      occurred(interaction, item, retry);
      return true;
   }
   
   // code below to convert from JavaScript to ANSI/CEA-2018 and load into Disco
   // see init script in models/Pedagogical.xml
   
   private static DocumentBuilder builder; 
   
   { 
     DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
     factory.setNamespaceAware(true); 
     try { builder = factory.newDocumentBuilder(); }
     catch (ParserConfigurationException e) { throw new RuntimeException(e); }
   }
   
   private static Document document;
   private static Element model;
   private static final String CEA_2018 = "http://ce.org/cea-2018";
   private static final String primitives = "urn:disco.wpi.edu:ldt:Primitives";
   
   public static void load () { disco.load(document, null); }
   
   public static void compileTasks (String about, String[] tasks) {
      document = builder.newDocument();
      model = document.createElementNS(CEA_2018, "taskModel");
      document.appendChild(model);
      // boiler plate
      Attr aboutAttr = document.createAttribute("about");
      aboutAttr.setValue(about);
      model.setAttributeNode(aboutAttr);
      Attr xmlns = document.createAttribute("xmlns");
      xmlns.setValue(CEA_2018);
      model.setAttributeNode(xmlns);
      Attr primitive = document.createAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:primitive");
      primitive.setValue(primitives);
      model.setAttributeNode(primitive);
      for (int i = 0; i < tasks.length; i++) {
         String id = tasks[i];
         if ( isPrimitive(id) ) continue;
         Element task = document.createElementNS(CEA_2018, "task");
         model.appendChild(task);
         Attr idAttr = document.createAttribute("id");
         idAttr.setValue(id);
         task.setAttributeNode(idAttr);
      }
   }
   
   public static void compileRecipe (String id, String goal, String[] steps) {
      Element subtasks = document.createElementNS(CEA_2018, "subtasks");
      model.appendChild(subtasks);
      Attr idAttr = document.createAttribute("id");
      idAttr.setValue(id);
      subtasks.setAttributeNode(idAttr);
      Attr goalAttr = document.createAttribute("goal");
      goalAttr.setValue(goal);
      subtasks.setAttributeNode(goalAttr);
      for (int i = 0; i < steps.length; i++) {
         Element step =  document.createElementNS(CEA_2018, "step");
         subtasks.appendChild(step);
         Attr name = document.createAttribute("name");
         name.setValue("step"+(i+1));
         step.setAttributeNode(name);
         Attr taskAttr = document.createAttribute("task");
         taskAttr.setValue((isPrimitive(steps[i]) ? "primitive:" : "")+steps[i]);
         step.setAttributeNode(taskAttr);
      }
   }  
   
   private static boolean isPrimitive (String id) {
      return disco.resolveTaskClass(new QName(primitives, id)) != null;
   }

}
