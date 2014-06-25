/*
 *
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package disco;

import edu.wpi.cetask.Utils;
import edu.wpi.disco.Agenda.Plugin;
import edu.wpi.disco.*;
import edu.wpi.disco.game.*;
import edu.wpi.disco.lang.Utterance;
import eu.semaine.jms.IOBase.Event;
import eu.semaine.jms.message.SEMAINEMessage;
import eu.semaine.jms.receiver.Receiver;
import eu.semaine.jms.sender.FMLSender;

import java.awt.event.*;
import java.util.*;
import java.util.logging.*;

import javax.jms.JMSException;

/**
 *
 * @author Andre
 */
public class DiscoComponent extends eu.semaine.components.Component {
    
    private FMLSender fmlSender;
    private Receiver outputAnimation;
    TenChoicesFrame gui;

    private class SemaineAgent extends Agent {
        public SemaineAgent (String s) {
           super(s);
        }
        
        @Override
        public void say (Interaction interaction, Utterance utterance) { 
           setCharacter(this);
           sendSentence(interaction.format(utterance));
        }
    };

    final SemaineAgent interviewer = new SemaineAgent("interviewer");
    final Player interviewee = new Player("interviewee");
    final SemaineAgent coach = new SemaineAgent("coach");

    // note using CoachedInteraction
    final CoachedInteraction nway = new CoachedInteraction(interviewee, interviewer, coach, null);
    final SingleInteraction interview = nway.getMain();
    final SingleInteraction coaching = nway.getCoaching();
    final GameConsole console = nway.getConsole();
   
    DiscoComponent() throws JMSException {
        super("Disco Component", false, false);
        continueToTheNextStep=true;
        this.waitingTime = 500;
        fmlSender = new FMLSender(
              "semaine.data.action.selected.function", 
              getName());
        senders.add(fmlSender);
        outputAnimation = new Receiver("semaine.callback.output.Animation");
        receivers.add(outputAnimation);

        // simplified loading
        edu.wpi.disco.Disco disco = interview.getDisco();
        disco.load("models/Interview.xml");
        disco.load("models/InterviewTop.xml");
        // start toplevel interview goal
        disco.addTop(disco.getTaskClass("Interview"));
        coaching.getDisco().load("models/Coach.xml");

        new Thread(){
            @Override
            public void run(){ nway.run(); }
        }.start();
        startGUI();
    }
   
    private void setCharacter (SemaineAgent agent) {
       // TODO: set screen character to match given agent (may
       // not require changing current character)
       System.out.println("Set character: "+agent);
    }
     
    // note synchronization on interactions now built into Disco
    
    @Override
    protected void act() {
       if (continueToTheNextStep) {
          // always "drain" coaching interaction first
          setCharacter(coach);
          boolean responded = coach.respond(coaching, false, true);
          if ( responded ) generate(coaching);
          if ( !responded || answers == null ) {
             setCharacter(interviewer);
             interviewer.respond(interview, false, true);
             generate(interview);
          }
          if (continueToTheNextStep)
             gui.setAnswers(answers);
       }
    }

    private String[] answers, formatted;   
    private List<Plugin.Item> items;

    // generate interviewee answers in given interaction
    private void generate (Interaction interaction) {
       items = interviewee.generate(interaction);
       if ( items.isEmpty() ) answers = formatted = null;
       else {
          answers = new String[items.size()];
          formatted = new String[items.size()];
          for (int i=0;i<answers.length;++i) {
             StringBuilder buffer = new StringBuilder();
             Utterance utterance = (Utterance)items.get(i).task;
             formatted[i] = utterance.occurred() ? utterance.formatTask() :
                console.getEngine().translate(utterance);
             buffer.append(Utils.capitalize(formatted[i]));
             Utils.endSentence(buffer);
             answers[i] = buffer.toString();
          }
       }
    }

    private boolean continueToTheNextStep = true;
    
    @Override
    protected void react(SEMAINEMessage m ) throws JMSException{
        if(m==null)return ;
        if(m.getDatatype().equals("callback")){
            String id = m.getContentID();
            if(id.equals(lastId)){
                String message = m.getText();
                String type_and_the_rest = message.substring(message.indexOf("type=\"")+6);
                String type = type_and_the_rest.substring(0,type_and_the_rest.indexOf("\""));
                if(type.equals("end")){
                     //gui.setAnswers(answers);
                    continueToTheNextStep=true;
                }
                else 
                    if(type.equals("dead")){
                        sendSentence(lastSentence);
                }
            }
        }
    }
    
    String lastId;
    String lastSentence;
    private void sendSentence(String sentence){
        try {
            continueToTheNextStep = false;
            String idfml = "FML_Disco_"+meta.getTime();
            lastId = idfml;
            lastSentence=sentence;
            sentence = sentence.replace("'", "&quot;");
            String fml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+
                    "<fml-apml version=\"0.1\">"+
                    "<bml:bml xmlns:bml=\"http://www.mindmakers.org/projects/BML\" id=\"bml_"+idfml+"\">"+
                    "<bml:speech id=\"disco_text\" language=\"en-GB\" text=\""+sentence+"\" voice=\"activemary\">"+
                    "<ssml:mark xmlns:ssml=\"http://www.w3.org/2001/10/synthesis\" name=\"disco_text:tm1\"/>"+
                    sentence+
                    "<ssml:mark xmlns:ssml=\"http://www.w3.org/2001/10/synthesis\" name=\"disco_text:tm2\"/>"+
                    "</bml:speech>"+
                    "</bml:bml>"+
                    "<fml:fml xmlns:fml=\"http://www.mindmakers.org/fml\" id=\""+idfml+"\"/>"+
                    "</fml-apml>";
            fmlSender.sendTextMessage(
                    fml,
                    meta.getTime(),
                    Event.single,
                    idfml,
                    meta.getTime(),
                    SEMAINEMessage.CONTENT_TYPE_UTTERANCE);
        } catch (JMSException ex) {
            Logger.getLogger(DiscoComponent.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    
    
    private void startGUI(){
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {}
        gui = new TenChoicesFrame();
        gui.noAnswer();
        for(int i=0; i<gui.buttons.length;++i){
            gui.buttons[i].addActionListener(new TenChoicesFrameListener(i));
        }
        gui.setVisible(true);
        
    }

    class TenChoicesFrameListener implements ActionListener{
        private final int choice;
        TenChoicesFrameListener(int num){
            choice=num;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
	   synchronized (nway){ // not needed? Since done and doneUtterance already synch'd on interaction
                gui.disableAllButNot(choice);
                Plugin.Item item = items.get(choice);
                interview.getDisco().getInteraction().doneUtterance((Utterance) item.task, null, formatted[choice]);
                interview.done(true, item.task, item.contributes);
            }
            
        }
    }
}
