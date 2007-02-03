/*
 * The contents of this file are subject to the terms of the Common Development and Distribution License
 * (the "License"). You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the license at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each file and include the License file at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html. If applicable add the following below this
 * CDDL HEADER, with the fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [year] [name of copyright owner]
 */
/*
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.
 */

package com.stc.jmsjca.container;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author fkieviet
 * @version $Revision: 1.4 $
 */
public class EmbeddedDescriptor {
    private Archive mArchive;
    private Map mDocuments = new HashMap(); // key=path, value=Document

    /**
     * Tool function: ensures that the specified stream is closed
     *
     * @param stream to close; maybe null
     */
    public static void safeClose(OutputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception ex) {
                // ignore
            }
        }
    }

    public EmbeddedDescriptor(File orgEar, File ear) throws Exception {
        copy(orgEar, ear);
        mArchive = new Archive(ear);
    }

    public static void copy(File src, File dest) throws IOException {
        FileChannel sourceChannel = new FileInputStream(src).getChannel();
        FileChannel destinationChannel = new FileOutputStream(dest).getChannel();
        sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
        sourceChannel.close();
        destinationChannel.close();
    }

    public Document getDocument(String path) throws Exception {
        Document ret = (Document) mDocuments.get(path);
        if (ret == null) {
            byte[] payload = mArchive.fetchFile(path);
            ret = load(payload);
            mDocuments.put(path, ret);
        }
        return ret;
    }

    public void update() throws Exception {
        Map payloads = new HashMap();
        for (Iterator iter = mDocuments.entrySet().iterator();  iter.hasNext(); ) {
            Map.Entry item = (Map.Entry) iter.next();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                Format format = Format.getPrettyFormat();
                new XMLOutputter(format).output((Document) item.getValue(), out);
            } finally {
                safeClose(out);
            }

            payloads.put(item.getKey(), out.toByteArray());
        }

        mArchive.updateWithMemoryPayloads(payloads);

        mDocuments.clear();
    }

    public static Namespace J2EENS = Namespace.getNamespace(
        "http://java.sun.com/xml/ns/j2ee");

    private static Document load(byte[] payload) throws Exception {
        // Load doc
        SAXBuilder builder = new SAXBuilder();
        builder.setEntityResolver(new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId) {
                return null;
            }
        });

        return builder.build(new ByteArrayInputStream(payload));
    }

    private String getPath(Element e) {
        if (e.getParent() != null && e.getParent() instanceof Element) {
            return getPath((Element) e.getParent()) + "/" + e.getName();
        } else {
            return "/" + e.getName();
        }
    }

    public Element findElementText(Document doc, String path, String text) throws Exception {
        for (Iterator iter = doc.getRootElement().getDescendants(); iter.hasNext(); ) {
            Object item = (Object) iter.next();
            if (item instanceof Element) {
                Element e = (Element) item;
                if (text.equals(e.getText())) {
                    String thisPath = getPath(e);
                    if (thisPath.equals(path)) {
                        return e;
                    }
                }
            }
        }
        throw new Exception("Element not found; path=[" + path + "], text=[" + text + "]");
    }

    public Element up(Element e, int n) {
        Element ret = e;
        for (int i = 0; i < n; i++) {
            ret = (Element) ret.getParent();
        }
        return ret;
    }

    public Element findParentNode(String ddpath, String path, String text, int nUp) throws Exception {
        Document doc = getDocument(ddpath);
        Element ret = findElementText(doc, path, text);
        ret = up(ret, nUp);
        return ret;
    }

    public Element findElementByName(String ddpath, String elname) throws Exception {
        Document doc = getDocument(ddpath);
        for (Iterator iter = doc.getRootElement().getDescendants(); iter.hasNext(); ) {
            Object item = (Object) iter.next();
            if (item instanceof Element) {
                Element e = (Element) item;
                if (e.getName().equals(elname)) {
                    return e;
                }
            }
        }
        throw new Exception("Element not found; name=[" + elname + "]");
    }

    public Element findElementByText(String ddpath, String text) throws Exception {
        Document doc = getDocument(ddpath);
        for (Iterator iter = doc.getRootElement().getDescendants(); iter.hasNext(); ) {
            Object item = (Object) iter.next();
            if (item instanceof Element) {
                Element e = (Element) item;
                if (text.equals(e.getText())) {
                    return e;
                }
            }
        }
        throw new Exception("Element not found; text=[" + text + "]");
    }
    /**
     * For JDom iterations: only lets Element-s through.
     */
    public static class ElementFilter implements org.jdom.filter.Filter {
        private static final long serialVersionUID = -6658846670364943979L;
        public boolean matches(Object obj) {
            return obj instanceof Element;
        }
    }

    /**
     * Represents an activation configuration
     */
    public class ActivationConfig {
        private Element mRoot;
        private Map mValues;  // key=name (String); value=value (Element)

        /**
         * Constructor 
         * 
         * @param dd
         * @param ddpath
         * @param ejbname
         * @throws Exception
         */
        public ActivationConfig(String ddpath, String ejbname) throws Exception {
            // Find ejbname
            Element bean = null;
            Document doc = getDocument(ddpath);
            for (Iterator iter = doc.getRootElement().getDescendants(new ElementFilter()); iter.hasNext(); ) {
                Element e = (Element) iter.next();
                if (e.getName().equals("ejb-name") && e.getText().equals(ejbname)) {
                    bean = (Element) e.getParent();
                    break;
                }
            }
            if (bean == null) {
                throw new Exception("Element not found; name=[ejb-name, " + ejbname + "]");
            }
            
            // Find root
            for (Iterator iter = bean.getParent().getDescendants(new ElementFilter()); iter.hasNext(); ) {
                Element e = (Element) iter.next();
                if (e.getName().equals("activation-config")) {
                    mRoot = e;
                    break;
                }
            }
            if (mRoot == null) {
                throw new Exception("Element not found; name=[activation-config]");
            }
            
            mValues = new HashMap();
            
            // Find values
            for (Iterator iter = mRoot.getDescendants(new ElementFilter()); iter.hasNext(); ) {
                Element e = (Element) iter.next();
                if (e.getName().equals("activation-config-property")) {
                    Iterator subs = e.getDescendants(new ElementFilter());
                    if (!subs.hasNext()) {
                        throw new Exception("No config name");
                    }
                    Element name = (Element) subs.next();
                    if (!name.getName().equals("activation-config-property-name")) {
                        throw new Exception("Unexpected element [" + name.getName() + "] encountered");
                    }
                    if (!subs.hasNext()) {
                        throw new Exception("No config value for " + name.getText());
                    }
                    Element value = (Element) subs.next();
                    if (!value.getName().equals("activation-config-property-value")) {
                        throw new Exception("Unexpected element [" + name.getName() + "] encountered");
                    }
                    mValues.put(name.getText().toUpperCase(), value);
                }
            }
        }
        
        /**
         * Sets a configuration parameter
         * 
         * @param name
         * @param newValue
         * @throws Exception misc errors
         */
        public void setParam(String name, String newValue) throws Exception {
            Element e = (Element) mValues.get(name.toUpperCase());
            if (e == null) {
                Element configEl = new Element("activation-config-property", mRoot.getNamespace());
                Element nameEl = new Element("activation-config-property-name", mRoot.getNamespace());
                nameEl.setText(name);
                e = new Element("activation-config-property-value", mRoot.getNamespace());
                mRoot.addContent(configEl);
                configEl.addContent(nameEl);
                configEl.addContent(e);
            }
            e.setText(newValue);
        }
        
        /**
         * Creates an instance of the specified interface such that any
         * setter in this interface will cause a configuration parameter
         * to be set.
         * 
         * @param itf
         * @return instance
         */
        public Object createActivation(Class itf) {
            InvocationHandler h = new InvocationHandler() {
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (method.getName().startsWith("set")) {
                        setParam(method.getName().substring("set".length()), (String) args[0]);
                        return null;
                    } else {
                        throw new Exception("Invalid operation " + method.getName());
                    }
                }
            };
            return Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] {itf}, h);
        }
    }
    
    /**
     * Base class for XML snippets that have the following sequence:
     * 
     *   <config-property>
     *           <config-property-name>Options</config-property-name>
     *           <config-property-type>java.lang.String</config-property-type>
     *           <config-property-value></config-property-value>
     *   </config-property>
     *   
     * @author fkieviet 
     */
    public class Configurable {
        /**
         * Root element of the configurable parameters
         */
        protected Element mRoot;
        
        /**
         * All parameters
         */
        protected Map mValues = new HashMap();  // key=name (String); value=value (Element)
        
        protected void findValues() throws Exception {
            for (Iterator iter = mRoot.getDescendants(new ElementFilter()); iter.hasNext(); ) {
                Element e = (Element) iter.next();
                if (e.getParent() == mRoot && e.getName().equals("config-property")) {
                    Iterator subs = e.getDescendants(new ElementFilter());
                    // config-property-name
                    if (!subs.hasNext()) {
                        throw new Exception("No config name");
                    }
                    Element name = (Element) subs.next();
                    if (!name.getName().equals("config-property-name")) {
                        throw new Exception("Unexpected element [" + name.getName() + "] encountered");
                    }
                    
                    // config-property-type
                    if (!subs.hasNext()) {
                        throw new Exception("No config-property-type value for " + name.getText());
                    }
                    Element type = (Element) subs.next();
                    if (!type.getName().equals("config-property-type")) {
                        throw new Exception("Unexpected element [" + name.getName() + "] encountered");
                    }
                    
                    // config-property-value
                    Element value; 
                    if (!subs.hasNext()) {
                        value = new Element("config-property-value", mRoot.getNamespace());
                        value.setText("");
                    } else {
                        value = (Element) subs.next();
                        if (!value.getName().equals("config-property-value")) {
                            throw new Exception("Unexpected element [" + name.getName() + "] encountered");
                        }
                    }
                    mValues.put(name.getText().toUpperCase(), value);
                }
            }
        }

        /**
         * Sets a configuration parameter
         * 
         * @param name
         * @param newValue
         * @throws Exception misc errors
         */
        public void setParam(String name, String newValue) throws Exception {
            Element e = (Element) mValues.get(name.toUpperCase());
            if (e == null) {
                Element configEl = new Element("config-property", mRoot.getNamespace());
                Element nameEl = new Element("config-property-name", mRoot.getNamespace());
                nameEl.setText(name);
                Element typeEl = new Element("config-property-type", mRoot.getNamespace());
                typeEl.setText(String.class.getName());
                e = new Element("config-property-value", mRoot.getNamespace());
                mRoot.addContent(configEl);
                configEl.addContent(nameEl);
                configEl.addContent(e);
            }
            e.setText(newValue);
        }
        
        /**
         * @param name String
         * @return value of named parameter
         * @throws Exception on failure
         */
        public String getParam(String name) throws Exception {
            Element e = (Element) mValues.get(name.toUpperCase());
            if (e == null) {
                return null;
            } else {
                return e.getText();
            }
        }

        /**
         * Creates an instance of the specified interface such that any
         * setter in this interface will cause a configuration parameter
         * to be set.
         * 
         * @param itf
         * @return instance
         */
        public Object createSettable(Class itf) {
            InvocationHandler h = new InvocationHandler() {
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (method.getName().startsWith("set")) {
                        setParam(method.getName().substring("set".length()), (String) args[0]);
                        return null;
                    } else if (method.getName().startsWith("get")) {
                        return getParam(method.getName().substring("get".length()));
                    } else {
                        throw new Exception("Invalid operation " + method.getName());
                    }
                }
            };
            return Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] {itf}, h);
        }
    }

    /**
     * Represents an activation configuration
     */
    public class ResourceAdapter extends Configurable {
        /**
         * Constructor 
         * 
         * @param dd
         * @param ddpath
         * @param ejbname
         * @throws Exception
         */
        public ResourceAdapter(String ddpath) throws Exception {
            // Find root
            Element root = null;
            Document doc = getDocument(ddpath);
            for (Iterator iter = doc.getRootElement().getDescendants(new ElementFilter()); iter.hasNext(); ) {
                Element e = (Element) iter.next();
                if (e.getName().equals("resourceadapter")) {
                    root = (Element) e;
                    break;
                }
            }
            if (root == null) {
                throw new Exception("Element not found; name=[resourceadapter]");
            }
            
            mRoot = root;
            
            // Find values
            findValues();
        }
        
        /**
         * Creates an instance of the specified interface such that any
         * setter in this interface will cause a configuration parameter
         * to be set.
         * 
         * @param itf
         * @return instance
         */
        public Object createConnector(Class itf) {
            return createSettable(itf);
        }
    }
    
    /**
     * Represents an outbound section in ra.xml
     * 
     * @author fkieviet
     */
    public class Outbound extends Configurable {
        public Outbound(String ddpath, String itfname) throws Exception {
            // Find root
            mRoot = (Element) findElementByText(ddpath, itfname).getParent();
            if (mRoot == null) {
                throw new Exception("Element not found; name=[" + itfname + "]");
            }
            
            // Find values
            findValues();
        }
    }
}
