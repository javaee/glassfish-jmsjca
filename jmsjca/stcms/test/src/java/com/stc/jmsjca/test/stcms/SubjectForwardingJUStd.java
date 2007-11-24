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

package com.stc.jmsjca.test.stcms;

import com.stc.jmsjca.container.Container;
import com.stc.jmsjca.container.EmbeddedDescriptor;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.security.auth.Subject;

import java.security.Principal;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Required:
 * test.server.properties = path to properties file containing server config
 * test.ear.path          = path to ear file to be tested
 *
 * Example for Eclipse:
 *     -Dtest.server.properties=../../R1/logicalhost/testsettings.properties -Dtest.ear.path=rastcms/test/rastcms-test.ear
 * with working directory
 *     ${workspace_loc:e-jmsjca/build}/..
 *
 * @author fkieviet
 * @version $Revision: 1.5 $
 */
public class SubjectForwardingJUStd extends StcmsEndToEnd {

    /**
     * A test principal
     *
     * @author fkieviet
     */
    public static class PrincipalX implements Principal {
        private String mName;
        private String mRealm;
        private String[] mRoles;

        public PrincipalX(String name, String realm, String[] roles) {
            mName = name;
            mRealm = realm;
            mRoles = roles;
        }

        public String getName() {
            return mName;
        }

        public String toString() {
            return this.getClass().getName() + ": [" + mName + "]";
        }

        public String[] getRoles() {
            return mRoles;
        }

        public String getRealm() {
            return mRealm;
        }
    }

    private static final String Name1 = "java.security.Principal:getName()";
    private static final String String1 = "java.security.Principal:toString()";
    private static final String Realm1 = "java.security.Principal:getRealm()";
    private static final String Roles1 = "java.security.Principal:getRoles()";

    private static final String Name2 = "java.security.Principal:getVouchee().getName()";
    private static final String String2 = "java.security.Principal:getVouchee().toString()";
    private static final String Realm2 = "java.security.Principal:getVouchee().getRealm()";
    private static final String Roles2 = "java.security.Principal:getVouchee().getRoles()";

    /**
     * Tests without roles
     *
     * @throws Throwable
     */
    public void testSubjNoRoles_RTS_ONLY() throws Throwable {
        Subject s = new Subject();
        s.getPrincipals().add(new PrincipalX("Lilly", "xyzrealm", new String[0]));
        Subject.doAs(s, new PrivilegedExceptionAction() {
            public Object run() throws Exception {
                Map m = getEcho();
                assertTrue(m.get(Name1).equals(mServerProperties.get("admin.user")));
                assertTrue(m.get(Realm1).equals("file"));
                assertTrue(m.get(Roles1).equals(""));
                assertTrue(m.get(String1).equals(mServerProperties.get("admin.user") + " (realm=file) on behalf of Lilly (realm=xyzrealm)"));

                assertTrue(m.get(Name2).equals("Lilly"));
                assertTrue(m.get(Realm2).equals("xyzrealm"));
                assertTrue(m.get(Roles2).equals(""));
                assertTrue(m.get(String2).equals("Lilly (realm=xyzrealm)"));
                return null;
            }
        });
    }

    /**
     * Tests with roles
     *
     * @throws Throwable
     */
    public void testSubj_RTS_ONLY() throws Throwable {
        Subject s = new Subject();
        s.getPrincipals().add(new PrincipalX("Lilly", "xyzrealm", new String[] {"a", "b", "c" }));
        Subject.doAs(s, new PrivilegedExceptionAction() {
            public Object run() throws Exception {
                Map m = getEcho();
                assertTrue(m.get(Name1).equals(mServerProperties.get("admin.user")));
                assertTrue(m.get(Realm1).equals("file"));
                assertTrue(m.get(Roles1).equals(""));
                assertTrue(m.get(String1).equals(mServerProperties.get("admin.user") + " (realm=file) on behalf of Lilly (realm=xyzrealm)"));

                assertTrue(m.get(Name2).equals("Lilly"));
                assertTrue(m.get(Realm2).equals("xyzrealm"));
                String roles = (String) m.get(Roles2);
                assertTrue(roles != null);
                assertTrue(m.get(Roles2).equals("a,b,c"));
                assertTrue(m.get(String2).equals("Lilly (realm=xyzrealm)"));
                return null;
            }
        });
    }

    /**
     * Tests without realm
     *
     * @throws Throwable
     */
    public void testSubjNoRealm_RTS_ONLY() throws Throwable {
        Subject s = new Subject();
        s.getPrincipals().add(new PrincipalX("Lilly", null, new String[] {"a", "b", "c" }));
        Subject.doAs(s, new PrivilegedExceptionAction() {
            public Object run() throws Exception {
                Map m = getEcho();
                assertTrue(m.get(Name1).equals(mServerProperties.get("admin.user")));
                assertTrue(m.get(Realm1).equals("file"));
                assertTrue(m.get(Roles1).equals(""));
                System.out.println("X: " + m.get(String1));
                assertTrue(m.get(String1).equals(mServerProperties.get("admin.user") + " (realm=file) on behalf of Lilly"));

                assertTrue(m.get(Name2).equals("Lilly"));
                assertTrue(m.get(Realm2) == null);
                String roles = (String) m.get(Roles2);
                assertTrue(roles != null);
                assertTrue(m.get(Roles2).equals("a,b,c"));
                assertTrue(m.get(String2).equals("Lilly"));
                return null;
            }
        });
    }

    /**
     * Tests without any subject set
     *
     * @throws Throwable
     */
    public void testNoSubj_RTS_ONLY() throws Throwable {
        Map m = getEcho();
	    assertTrue(m.get(Name1).equals(mServerProperties.get("admin.user")));
	    assertTrue(m.get(Realm1).equals("file"));
	    assertTrue(m.get(Roles1).equals(""));
	    assertTrue(m.get(String1).equals(mServerProperties.get("admin.user") + " (realm=file)"));

	    assertTrue(m.get(Name2) == null);
	    assertTrue(m.get(Realm2) == null);
	    assertTrue(m.get(Roles2) == null);
	    assertTrue(m.get(String2) == null);
    }

    /**
     * Gets the subject info from the server by connecting to it
     * using JMX, and using the echo function in the LogMBean
     *
     * @return map of values
     * @throws Exception
     */
    private Map getEcho() throws Exception {
        Container c = createContainer();
        Map ret = new HashMap();

	    try {
	        LogMBean o = (LogMBean) c.getMBeanProxy("com.sun.appserv:name=logmanager,category=runtime", LogMBean.class);
	        AttributeList list = o.getSubjectInfo();
	        for (Iterator iter = list.iterator(); iter.hasNext();) {
	            Attribute a = (Attribute) iter.next();
	            System.out.println(a.getName() + "=" + a.getValue());
	            ret.put(a.getName(), a.getValue());
	        }
	    } finally {
	        Container.safeClose(c);
	    }
	    return ret;
    }

    /**
     * Deploy an ear (no test on logging; just for illustration)
     *
     * @throws Throwable
     */
    public void testDeploy() throws Throwable {
        Subject s = new Subject();
        s.getPrincipals().add(new PrincipalX("Lilly", "xyzrealm", new String[0]));
        Subject.doAs(s, new PrivilegedExceptionAction() {
            public Object run() throws Exception {
                EmbeddedDescriptor dd = new EmbeddedDescriptor(mTestEarOrg, mTestEar);
                dd.findElementByText(EJBDD, "XContextName").setText("j-testQQXAXA");
                StcmsConnector cc = (StcmsConnector) dd.new ResourceAdapter(RAXML).createConnector(StcmsConnector.class);
                cc.setUserName(mServerProperties.getProperty("admin.user"));
                cc.setPassword(mServerProperties.getProperty("admin.password"));
                
                if (!getContainerID().equals("rts")) {
                    String url = "stcms://" + getJmsServerProperties().getProperty("host") + ":"
                        + getJmsServerProperties().getProperty("stcms.instance.port");
                    cc.setConnectionURL(url);
                }
                
                dd.update();

                // Deploy
                Container c = createContainer();
                try {
                    c.redeployModule(mTestEar.getAbsolutePath());
                    c.undeploy(mTestEarName);
                } finally {
                    Container.safeClose(c);
                }
                return null;
            }
        });
    }

    /**
     * The MBean interface exposed by the LogMBean
     */
    public interface LogMBean {
        List getLoggerNames() throws Exception;
        AttributeList getSubjectInfo() throws Exception;
    }

}
