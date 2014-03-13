/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package disco;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.JMSException;

/**
 *
 * @author Andre
 */
public class Disco {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args){
        try {
            new DiscoComponent().start();
        } catch (JMSException ex) {
            Logger.getLogger(Disco.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
