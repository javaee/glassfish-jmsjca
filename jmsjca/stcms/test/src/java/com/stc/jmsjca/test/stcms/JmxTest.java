/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable add the following below this CDDL HEADER,
 * with the fields enclosed by brackets "[]" replaced with
 * your own identifying information: Portions Copyright
 * [year] [name of copyright owner]
 */
/*
 * $RCSfile: JmxTest.java,v $
 * $Revision: 1.3 $
 * $Date: 2007-01-21 17:52:13 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.test.stcms;

import com.stc.jmsjca.container.Container;

import javax.management.Attribute;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This tests the logging api in Tomato (should not be part of jmsjca; can be moved
 * as soon as the Container class is moved)
 *
 * Required:
 * test.server.properties = path to properties file containing server config
 * test.ear.path          = path to ear file to be tested
 *
 * @author fkieviet
 * @version $Revision: 1.3 $
 */
public class JmxTest extends StcmsEndToEnd {

    String DOMAIN1 = "com.sun.appserv:";
    String DOMAIN2 = "ias:";

    String S1 = "name=logmanager,category=runtime";
    String S2 = "type=module-log-levels,config=server-config,category=config";
    String S3 = "type=log-service,config=server-config,category=config";

    /**
     * Purpose: <br>
     * Assertion: <br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void x_test100() throws Throwable {
        a(DOMAIN2, S3, "file");
        a(DOMAIN1, S3, "file");

        a(DOMAIN1, S2, "connector");
        a(DOMAIN2, S2, "connector");
    }

    /**
     * The rules interface on LogMBean
     * name=com.sun.appserv:name=logmanager,category=runtime
     */
    public interface I1 {
        javax.management.AttributeList getLogRules() throws Exception;
        void setLogRules(javax.management.AttributeList rules) throws Exception;
        javax.management.AttributeList getLogLevels() throws Exception;
    }

    public interface I2 {
        javax.management.AttributeList getProperties() throws Exception;
        java.lang.Object getPropertyValue(java.lang.String propertyName) throws Exception;
        void setProperty(javax.management.Attribute nameAndValue) throws Exception;
    }

    public interface I3 {
        String xgetfile() throws Exception;
    }

    /**
     * Purpose: <br>
     * Assertion: <br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void x_test200() throws Throwable {
        Container c = createContainer();

        I2 o = (I2) c.getMBeanProxy(DOMAIN1 + S2, I2.class);
        javax.management.AttributeList attrs = o.getProperties();
        for (int i = 0; i < attrs.size(); i++) {
            Attribute item = (Attribute) attrs.get(i);
            System.out.println("item " + item.getName() + "=" + item.getValue());
        }

        System.out.println(o.getPropertyValue("com.stc"));
    }

    private Map toMap(javax.management.AttributeList attrs) {
        Map attrm = new HashMap();
        for (int i = 0; i < attrs.size(); i++) {
            Attribute item = (Attribute) attrs.get(i);
            attrm.put(item.getName(), item.getValue());
        }
        return attrm;
    }

    private javax.management.AttributeList toList(Map map) {
        javax.management.AttributeList ret = new javax.management.AttributeList();
        for (Iterator it = map.entrySet().iterator(); it.hasNext(); ) {

            Map.Entry o = (Map.Entry) it.next();

            ret.add(new Attribute((String) o.getKey(), o.getValue()));
        }
        return ret;
    }


    /**
     * Purpose: <br>
     * Assertion: Known attributes should be present<br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test400() throws Throwable {
        Container c = createContainer();
        I1 o = (I1) c.getMBeanProxy(DOMAIN1 + S1, I1.class);

        javax.management.AttributeList attrs = o.getLogRules();
        Map attrm = toMap(attrs);

        // Rules should contain admin, root, saaj
        assertTrue(attrm.containsKey("root"));
        assertTrue(attrm.containsKey("saaj"));
        assertTrue(attrm.containsKey("admin"));
    }

    /**
     * Purpose: <br>
     * Assertion: Attributes can be changed but not deleted<br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test410() throws Throwable {
        Container c = createContainer();
        I1 o = (I1) c.getMBeanProxy(DOMAIN1 + S1, I1.class);

        javax.management.AttributeList attrs = o.getLogRules();
        Map attrm = toMap(attrs);

        // Store org value
        String org = (String) attrm.get("saaj");
        String newl = org.equals("OFF") ? "INFO" : "OFF";

        // Change attrs
        attrm.put("saaj", newl);
        o.setLogRules(toList(attrm));

        // Read back
        attrm = toMap(o.getLogRules());
        String x = (String) attrm.get("saaj");
        assertTrue(x != null);
        assertTrue(attrm.get("saaj").equals(newl));

        // Change attrs back
        attrm.put("saaj", org);
        o.setLogRules(toList(attrm));

        // Read back
        attrm = toMap(o.getLogRules());
        assertTrue(attrm.get("saaj").equals(org));

        // Try to delete
        attrm.put("saaj", null);
        o.setLogRules(toList(attrm));

        // Read back
        attrm = toMap(o.getLogRules());
        assertTrue(attrm.get("saaj").equals(org));
    }

    /**
     * Purpose: <br>
     * Assertion: Non-attributes are set as properties<br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test420() throws Throwable {
        Container c = createContainer();
        I1 o = (I1) c.getMBeanProxy(DOMAIN1 + S1, I1.class);

        javax.management.AttributeList attrs = o.getLogRules();
        Map attrm = toMap(attrs);

        String cat = "a.b.c.d.e.f";

        // Store org value
        String org = (String) attrm.get(cat);
        String newl = "OFF".equals(org) ? "INFO" : "OFF";

        // Change attrs
        attrm.put(cat, newl);
        o.setLogRules(toList(attrm));

        // Read back
        attrm = toMap(o.getLogRules());
        String x = (String) attrm.get(cat);
        assertTrue(x != null);
        assertTrue(attrm.get(cat).equals(newl));

        // Try to delete
        attrm.put(cat, null);
        o.setLogRules(toList(attrm));

        // Read back
        attrm = toMap(o.getLogRules());
        assertTrue(attrm.get(cat) == null);

        // Try to delete twice
        attrm.put(cat, null);
        o.setLogRules(toList(attrm));

        // Read back
        attrm = toMap(o.getLogRules());
        assertTrue(attrm.get(cat) == null);

        // Change attrs back
        attrm.put(cat, org);
        o.setLogRules(toList(attrm));

        // Read back
        attrm = toMap(o.getLogRules());
        assertTrue((org == null && attrm.get(cat) == null) || attrm.get(cat).equals(org));
    }

    /**
     * Purpose: <br>
     * Assertion: Non-attributes can be deleted by omitting them<br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test430() throws Throwable {
        Container c = createContainer();
        I1 o = (I1) c.getMBeanProxy(DOMAIN1 + S1, I1.class);

        javax.management.AttributeList attrs = o.getLogRules();
        Map attrm = toMap(attrs);

        String cat = "a.b.c.d.e.f";

        // Store org value
        String org = (String) attrm.get(cat);
        String newl = "OFF".equals(org) ? "INFO" : "OFF";

        // Change attrs
        attrm.put(cat, newl);
        o.setLogRules(toList(attrm));

        // Read back
        attrm = toMap(o.getLogRules());
        String x = (String) attrm.get(cat);
        assertTrue(x != null);
        assertTrue(attrm.get(cat).equals(newl));

        // Try to delete
        attrm.remove(cat);
        o.setLogRules(toList(attrm));

        // Read back
        attrm = toMap(o.getLogRules());
        assertTrue(attrm.get(cat) == null);

        // Change attrs back
        attrm.put(cat, org);
        o.setLogRules(toList(attrm));

        // Read back
        attrm = toMap(o.getLogRules());
        assertTrue((org == null && attrm.get(cat) == null) || attrm.get(cat).equals(org));
    }

    /**
     * Purpose: <br>
     * Assertion: File should be readable<br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test500() throws Throwable {
        Container c = createContainer();
        I3 o = (I3) c.getMBeanProxy(DOMAIN1 + S3, I3.class);

        String file = o.xgetfile();
        assertTrue(file != null);
        assertTrue(new File(file).isFile());

    }

    /**
     * Purpose: <br>
     * Assertion: File should be readable<br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test510() throws Throwable {
        Container c = createContainer();
        I1 o = (I1) c.getMBeanProxy(DOMAIN1 + S1, I1.class);

        javax.management.AttributeList attrs = o.getLogLevels();
        Map attrm = toMap(attrs);

        // Rules should contain admin, root, saaj
        assertTrue(attrm.containsKey("INFO"));
        assertTrue(attrm.containsKey("FINEST"));
    }

    //    javax.management.AttributeList newRules = new javax.management.AttributeList();
//    newRules.add(new javax.management.Attribute(
//        "javax.enterprise.system.webservices.rpc", "OFF"));
//    newRules.add(new javax.management.Attribute(
//        "com.stc", "OFF"));
//
//    o.setLogRules(newRules);

    /**
     * Purpose: <br>
     * Assertion: <br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void x_test300() throws Throwable {
        Container c = createContainer();

        I2 o = (I2) c.getMBeanProxy(DOMAIN1 + S2, I2.class);
        javax.management.AttributeList attrs = o.getProperties();
        for (int i = 0; i < attrs.size(); i++) {
            Attribute item = (Attribute) attrs.get(i);
            System.out.println("item " + item.getName() + "=" + item.getValue());
        }

        System.out.println(o.getPropertyValue("com.stc"));


        o.setProperty(new Attribute("com.stc.jmsjca", "FINE"));
        assertTrue(o.getPropertyValue("com.stc.jmsjca").equals("FINE"));

        o.setProperty(new Attribute("com.stc.jmsjca", "INFO"));
        assertTrue(o.getPropertyValue("com.stc.jmsjca").equals("INFO"));


        attrs = o.getProperties();
        for (int i = 0; i < attrs.size(); i++) {
            Attribute item = (Attribute) attrs.get(i);
            System.out.println("item " + item.getName() + "=" + item.getValue());
        }
    }


    private void a(String domain, String name, String attrib) throws Throwable {
        Container c = createContainer();
        String s = (String) c.getAttribute(domain + name, attrib);
        System.out.println(domain + name + "::::" + attrib);
        System.out.println(s);
    }
}
