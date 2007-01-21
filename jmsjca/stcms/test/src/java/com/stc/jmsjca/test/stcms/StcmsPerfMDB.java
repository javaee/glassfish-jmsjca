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
 * $RCSfile: StcmsPerfMDB.java,v $
 * $Revision: 1.3 $
 * $Date: 2007-01-21 17:52:15 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.test.stcms;

import com.stc.jmsjca.util.Logger;

import javax.ejb.EJBException;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * The MDB
 *
 * @author fkieviet
 * @version $Revision: 1.3 $
 */
public class StcmsPerfMDB implements MessageDrivenBean, MessageListener {
    private InitialContext mCtx;
    private Executor mExecutor;

    static final Logger sLog = Logger.getLogger(StcmsPerfMDB.class);

    public static abstract class Executor {
        public void init(InitialContext ctx) throws Exception {}
        public abstract void onMessage(Message m, InitialContext ctx) throws Exception;

        public void safeClose(Connection conn) {
            if (conn != null) {
                try {
                    conn.close();
                } catch (JMSException ignore) {
                }
            }
        }
    }

    /**
     * Default constructor. Creates a bean. Required by EJB spec.
     */
    public StcmsPerfMDB() {
        sLog.info("Perf MDB constructor");
    }

    /**
     * Sets the context for the bean.
     * @param mdc the message-driven-bean context.
     */
    public void setMessageDrivenContext(MessageDrivenContext mdc) {
        sLog.info("In StcmsPerfMDB.setMessageDrivenContext()");
    }

    /**
     * Creates a bean. Required by EJB spec.
     */
    public void ejbCreate() {
        sLog.info("In StcmsPerfMDB.ejbCreate()");
        try {
            mCtx = new InitialContext();
        } catch (NamingException ex) {
            sLog.fatal(ex, ex);
        }

        try {
            String fname = (String) mCtx.lookup("java:comp/env/Test");
            sLog.info("Looking up class name " + fname);
            Class c = Class.forName(fname);
            mExecutor = (Executor) c.newInstance();
            mExecutor.init(mCtx);
        } catch (Exception ex) {
            sLog.fatal(ex, ex);
        }
    }

    /**
     * Removes the bean. Required by EJB spec.
     */
    public void ejbRemove() {
        sLog.info("In StcmsPerfMDB.remove()");
    }

    /**
     * Called by the RA
     *
     * @param message incoming message.
     */
    public void onMessage(javax.jms.Message message) {
        try {
            mExecutor.onMessage(message, mCtx);
        } catch (Exception e) {
            sLog.error(".onMessage() encountered an exception: " + e, e);
            throw new EJBException(
                "SimpleMessageBean.onMessage() encountered an exception: " + e, e);
        }
    }
}
