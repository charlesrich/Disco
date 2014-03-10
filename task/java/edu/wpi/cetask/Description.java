/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.cetask;

import org.w3c.dom.*;

import java.io.PrintStream;
import java.util.*;

import javax.xml.namespace.QName;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;

/**
 * Base class for parts of a Task Model Description
 */
public abstract class Description { //TODO: temporarily public for Anahita
 
   // NB: Some (any) prefix *must* be provided for elements in all
   // XPath expressions, because of way that XPath works with XML namespaces
   
   static final String about = "/n:taskModel/@about";
   
   protected final String namespace;
   public String getNamespace () { return namespace; }

   /**
    * Node in DOM
    */
   protected final Node node;
   
   private static final TransformerFactory factory = TransformerFactory.newInstance();
   
   public void print (PrintStream stream) {
      try {
         Transformer transformer = factory.newTransformer();
         transformer.setOutputProperty(OutputKeys.INDENT, "yes");
         transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
         transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
         transformer.transform(new DOMSource(node), new StreamResult(stream));
      } catch (Exception e) { Utils.rethrow("Error printing "+this, e); }
   }
   
   /**
    * For searching in DOM (includes corrected namespace context)
    */
   protected final XPath xpath;
   
   /**
    * Task engine in which DOM resides
    */
   protected final TaskEngine engine; 
   public TaskEngine getEngine () { return engine; }
   
   protected Description (Node node, TaskEngine engine, XPath xpath) { 
      if ( node == null ) throw new IllegalArgumentException("Null node");
      this.node = node; 
      this.engine = engine;
      this.xpath = xpath;
      namespace = xpath(about);
   }

   public TaskClass resolveTaskClass (String qname) {
      return engine.resolveTaskClass(parseQName(qname));
   }
   
   // convenient utilities for subclasses
      
   protected Object xpath (String path, QName returnType) {
      try { return  xpath.evaluate(path, node, returnType); } 
      catch (XPathExpressionException e) { throw new RuntimeException(e); }
   }
   
   protected String xpath (String path) {
      try { return xpath.evaluate(path, node); } 
      catch (XPathExpressionException e) { throw new RuntimeException(e); }
   }
   
   protected List<String> xpathValues (String path) {
      List<String> values = new ArrayList<String>();
      for (Node node : xpathNodes(path))
         values.add(node.getNodeValue());
      return values;
   }
   
   protected List<Node> xpathNodes (String path) { 
      List<Node> nodes = new ArrayList<Node>();
      NodeList nodelist = (NodeList) xpath(path, XPathConstants.NODESET);
      for (int i = 0; i < nodelist.getLength(); i++) // preserve order
         nodes.add(nodelist.item(i));
      return nodes;
   }
   
   protected QName parseQName (String qname) {
      String prefix;
      int i = qname.indexOf(':');
      if ( i >= 0 ) {
         prefix = qname.substring(0, i);
         String ns = node.lookupNamespaceURI(prefix);
         if ( ns == null ) 
            throw new IllegalArgumentException("Unknown namespace prefix "+qname
                  +" in "+getNamespace()); 
         return new QName(ns, qname.substring(i+1), prefix); 
      } else // default namespace is from 'about', not xmlns
         return new QName(getNamespace(), qname);  
   }
 
}