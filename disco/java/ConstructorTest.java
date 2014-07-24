import java.util.*;
import edu.wpi.cetask.*;
import edu.wpi.cetask.TaskClass.*;
import edu.wpi.cetask.DecompositionClass.*;
import edu.wpi.disco.*;

// for testing constructing model with XML
// to support LIMSI work on Discolog

public class ConstructorTest {

   public static void main (String[] args) {
      ConstructorTest test = new ConstructorTest();
      // this is an extremely simple example tree--real experiment should have
      // branching factor of three at each level
      // NB: Write an *algorithm* to generate and prune conditions in tree
      // do not handcode
      TaskClass p1 = test.newTask("p1", true, "P", "Q", "Q=true;println('p1')"),
                // note since p2 and p3 are alternative recipes for b
                // they have the same pre/postconditions
                p2 = test.newTask("p2", true, "Q", "R", "R=true;println('p2')"),
                p3 = test.newTask("p3", true, "Q", "R", "R=true;println('p3')"),
                // recursive propagation of pre/postconditions up the tree
                b = test.newTask("b", false, p2.getPrecondition().getScript(),
                      p2.getPostcondition().getScript(), null),
                a = test.newTask("a", false, p1.getPrecondition().getScript(), 
                      b.getPostcondition().getScript(), null);
      test.newRecipe("r1", b, Collections.singletonList(new Step("s1", p2)), "V");
      test.newRecipe("r2", b, Collections.singletonList(new Step("s1", p3)), "W");
      // build the non-recipe part of the tree
      Plan top = newPlan(a);
      top.add(newPlan(p1));
      top.add(newPlan(b));
      top.setPlanned(true); // needed only for non-recipe nodes
      test.disco.addTop(top);
      // prevent agent asking about toplevel goal
      test.disco.setProperty("Ask.Should(a)@generate", false);
      // initialize all world state predicates
      test.disco.eval("var P,Q,R,V=true,W=false", "init");
      // allow agent to keep executing without talking
      ((Agent) test.interaction.getSystem()).setMax(100);
      // agent starts
      test.interaction.start(false);
   }
   
   // NB: use instance of Discolog extension instead of Agent below
   private final Interaction interaction =  new Interaction(new Agent("agent"), new User("user")) {
      
      @Override
      public void run () {
         // keep running as long as agent has something to do and then stop
         while (getSystem().respond(interaction, false, false)) {}
      }
   };
   
   private final Disco disco = interaction.getDisco();
   private final TaskModel model = new TaskModel("urn:edu.wpi.cetask:models:Test", disco); 
   
   private TaskClass newTask (String id, boolean primitive, String precondition, String postcondition, String grounding) {
      if ( !primitive && grounding != null ) 
         throw new IllegalArgumentException("Non-primitive cannot have grounding script: "+id);
      TaskClass task = new TaskClass(model, id,
            new Precondition(precondition, true, disco), 
            new Postcondition(postcondition, true, true, disco), 
            grounding == null ? null : new Grounding(grounding, disco));
      task.setProperty("@primitive",  primitive);
      return task;
   }
   
   private DecompositionClass newRecipe (String id, TaskClass goal, List<Step> steps, String applicable) {
      return new DecompositionClass(model, id, goal, steps, new Applicability(applicable, true, disco));
   }
   
   private static Plan newPlan (TaskClass task) { return new Plan(task.newInstance()); }
}
