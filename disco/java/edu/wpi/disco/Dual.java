package edu.wpi.disco;

import java.util.concurrent.*;
import edu.wpi.cetask.*;
import edu.wpi.disco.lang.Ok;

public class Dual {
 
   /**
    * Main method for interactively running two instances of Disco, each with its own
    * default Disco agent as system and the other agent as external actor.  To run with
    * extended agents, use similar invocation with extended agent instances.
    * <p>
    * The bin/dual command calls this method.
    * <p>
    * See disco/test/dual/Console1.test, Console2.test, 
    * and Restaurant1.test, Restaurant2.test for examples of how to interact with 
    * consoles.
    * 
    * @see #Dual(Actor,Actor,boolean)
    * @see Disco.Shared
    * @see ToM
    */
   public static void main (String[] args) {  
      new Dual(new Agent("Agent1"), new Agent("Agent2"), true).start();
   }
   
   public static class Test1 { // for testing without consoles 

      // see disco/test/dual/Test1.txt
      
      public static void main (String[] args) {  
         Dual dual = new Dual(new Agent("Agent1"), new Agent("Agent2"), false);
         // libraries do not have to be identical, i.e., lots of interesting
         // issues to explore with misunderstandings :-)
         dual.interaction1.load("models/Test.xml");
         dual.interaction2.load("models/Test.xml");
         dual.interaction1.getDisco().addTop("A");
         dual.start();
      }
   }
   
   public static class Test2 { // for testing without consoles
      
      // see disco/test/dual/Test2.txt

      public static void main (String[] args) {  
         Dual dual = new Dual(new Agent("Agent1"), new Agent("Agent2"), false);
         dual.interaction1.load("models/Restaurant.xml");
         dual.interaction2.load("models/Restaurant.xml");
         // make Agent2 submissive
         dual.interaction2.getDisco().eval("dominant = false", "Dual.Test2");
         dual.start();
      }
   }
   
   /**
    * Constructor for dual interactions.
    * 
    * @param agent1 first instance (gets first turn)
    * @param agent2 second instance
    * @param console if true then run interactively; otherwise run until exhausted
    *                
    * @see #start()               
    */
   public Dual (Actor agent1, Actor agent2, boolean console) {
      this.console = console;
      Other other1 = new Other(agent2.getName());
      Other other2 = new Other(agent1.getName());
      interaction1 = new OtherInteraction(agent1, other1, console, title(agent1), other2.queue);
      interaction2 = new OtherInteraction(agent2, other2, false, title(agent2), other1.queue);
      if ( console )
         System.setProperty("java.awt.headless", "false"); // make sure can have window
      window = console ? new ConsoleWindow(interaction2, 600, 500, 14, Shell.newLog("Console2"))
         : null;
      if ( window != null ) window.setTitle(title(agent2));
   }

   /**
    * Start the dual interactions running.  If consoles were not specified, then
    * execution ends when both agents have nothing more to say or do and final histories
    * for both interactions printed to system output.
    */
   public void start () { 
      if ( window != null ) window.setVisible(true); 
      // first agent always starts
      interaction1.start(false); 
      interaction2.start(true);
   }
   
   /**
    * The two interactions created by Dual.  Useful for initialization.
    * 
    * @see Test1
    * @see Test2
    */
   public final OtherInteraction interaction1, interaction2;   
   
   // window for interaction2 (interaction1 has system console)
   private final ConsoleWindow window; 
   
   // without consoles, runs without stopping
   private final boolean console;
   
   private static String title (Actor agent) { return agent.getName()+" Interaction"; }
 
   // to signal end of turn (since null not allowed in queues)
   private final Object END = new Object(); 
   
   public class Other extends Actor {
      
      private Other (String name) { 
         super(name); 
      }
 
      private volatile boolean exhausted; // volatile for other interaction thread
      
      // queue for communication and synchronization with other agent
      private final BlockingQueue<Object> queue = new LinkedBlockingQueue<Object>();

      @Override
      protected boolean synchronizedRespond (Interaction interaction, boolean ok, boolean guess) {
         boolean responded = false;
         Object element;
         do {
            try { element = queue.take(); }
            catch (InterruptedException e) { element = END; }
            if ( element != END ) {
               Task occurrence = (Task) element;
               // note we really should be using the other interaction's value of ok
               // but it probably doesn't matter
               exhausted = !responded && occurrence instanceof Ok && ok;
               responded = true;
               try { 
                  Task copy = translate(occurrence, interaction);
                  // note plan recognition always used because contributes null
                  interaction.getActor(copy).done(copy, interaction, null); 
                  copy.setWhen(occurrence.getWhen()); // keep original timestamp
               } catch (TranslateException e) {
                  System.err.println("WARNING: Ignoring untranslatable occurrence "+occurrence);
               }
             }
         } while ( element != END ); 
         if ( !responded ) exhausted = true;
         return responded;
      }
   }
   
   public class OtherInteraction extends Interaction {
   
      private final BlockingQueue<Object> queue;
      
      private OtherInteraction (Actor agent, Actor other, boolean console, String title, 
                                BlockingQueue<Object> queue) { 
         super(agent, other, null, false, null, title);
         this.queue = queue;
         if ( console ) new Console(null, this, Shell.newLog("Console1")).init(getDisco());
      }
      
      @Override
      public synchronized Plan occurred (boolean external, Task occurrence, 
            Plan contributes, boolean eval) {
         contributes = super.occurred(external, occurrence, contributes, eval);
         if ( !externalFloor ) queue.add(occurrence);
         return contributes;
      }   
      
      @Override
      public boolean doTurn (boolean ok) {
         boolean running = super.doTurn(ok);
         if ( !externalFloor ) queue.add(END);
         boolean exhausted = !console && interaction1.isExhausted() && interaction2.isExhausted();
         if ( exhausted ) { 
            interaction1.interrupt(); // threads may be waiting on queue 
            interaction2.interrupt(); 
         }
         return running && !exhausted;
      }
      
      private boolean isExhausted () { return ((Other) getExternal()).exhausted; }

      @Override
      public void cleanup () { 
         if ( !console ) 
            synchronized (Dual.this) {
               System.out.println();
               System.out.println(getName());
               System.out.println();
               getDisco().history(System.out); // before cleared
            }
         super.cleanup();
         if ( window != null && getConsole() == null ) window.close();
      }
   }
   
   // also used in ToM
   static Task translate (Task task, Interaction interaction) throws TranslateException {
      try { 
         TaskClass type = interaction.getDisco().resolveTaskClass(task.getType().getQName()); 
         if ( type == null ) throw new TranslateException();
         Task copy = type.newInstance();
         for (TaskClass.Input input : type.getDeclaredInputs())
            input.setSlotValue(copy, translateValue(input.getSlotValue(task), interaction));
         for (TaskClass.Output output : type.getDeclaredOutputs())
            output.setSlotValue(copy, translateValue(output.getSlotValue(task), interaction));
         if ( task.isPrimitive() && task.getExternal() != null ) 
            copy.setExternal(!task.getExternal()); // reverse perspective 
         if ( task.getSuccess() != null ) copy.setSuccess(task.getSuccess());
         // 'when' slot will be reset after occurred called
         return copy;
      } catch (IllegalArgumentException e) { throw new TranslateException(e); }
   }
   
   private static Object translateValue (Object value, Interaction interaction) throws TranslateException {
      return value instanceof Task ? translate((Task) value, interaction) : value;
   }
   
  static class TranslateException extends Exception {
      private TranslateException () {}
      private TranslateException (Throwable e) { super(e); }
   }}
