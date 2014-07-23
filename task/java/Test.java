import edu.wpi.cetask.*;

// for testing constructing model with XML

public class Test {

   public static void main (String[] args) {
      TaskEngine engine = new TaskEngine();
      TaskModel model = new TaskModel("urn:edu.wpi.cetask:models:Test", engine);
      TaskClass c = new TaskClass(model, "Test", null, null, null);
      System.out.println(c.newInstance());
   }
}
