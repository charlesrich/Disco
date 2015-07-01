import javax.script.*;
import edu.wpi.cetask.*;
import edu.wpi.disco.*;
import jdk.nashorn.api.scripting.*;

public class Test {

   public static void main (String[] args) {
      Interaction i = new Interaction(
            new Agent("agent"), 
            new User("user"),
            null);
      Disco engine = i.getDisco();
      ScriptEngineWrapper script = engine.getScriptEngine();
      try {
         System.out.println(script.invokeFunction("Object"));
      } catch (Exception e) { System.out.println(e); }
      Object obj = engine.newObject();
      engine.put(obj, "foo", 7);
      //Bindings bindings = new SimpleBindings();
      //bindings.put("x", obj);
      //engine.put(obj, "foo", new Object()); /// try remove also!
      //engine.eval("x.foo = undefined", bindings, "test");
      System.out.println(ScriptObjectMirror.isUndefined(
            ((ScriptObjectMirror) obj).getMember("foo")));
      /*
      Interaction i2 = new Interaction(
            new Agent("agent"), 
            new User("user"),
            null);
      Disco engine2 = i2.getDisco();
      System.out.println(engine.getGlobal("foo"));
      System.out.println(engine2.getGlobal("foo"));
      engine.setGlobal("foo", 7);
      System.out.println(engine.getGlobal("foo"));
      System.out.println(engine2.getGlobal("foo"));
      Bindings bindings = new SimpleBindings();
      bindings.put("$this", engine.newObject());
      bindings.put("$$value", 1434917591894L);
      //engine.put(bindings.get("$this"), "x", "foo");
      System.out.println(engine.eval(
           "print($$value);$this.x = $$value; print($this.x);print($this.x.class)",
           bindings,
           "test"));
      engine.load("../task/models/ABC.xml");
      TaskClass cls = engine.getTaskClass("B");
      Task t1 = cls.newInstance();
      t1.setSlotValue("external", true);
      t1.setSlotValue("success", true);
      t1.setSlotValue("input", "x");
      t1.setSlotValue("output1", 1);
      t1.setSlotValue("output2", "y");
      //Task t2 = cls.newInstance();
      t1.done();
      System.out.println(t1.getWhen());
      /*
      System.out.println(t2.getWhen());
      t2.copySlotValues(t1);
      System.out.println(t2);
      System.out.println(t2.getWhen());
      */
      
   }

}
