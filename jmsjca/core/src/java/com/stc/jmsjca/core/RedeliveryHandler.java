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

package com.stc.jmsjca.core;

import com.stc.jmsjca.localization.Localizer;
import com.stc.jmsjca.util.Exc;
import com.stc.jmsjca.util.Logger;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Topic;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Is used to deal with "poisonous messages". A poison message is a message that fails 
 * to be processed time and time again, thereby stopping other messages from being 
 * processed.
 * 
 * Is invoked for each message. Maintains a cache of msgids of messages that have the
 * JMSRedelivered flag set, and keeps a count of these messages. Based on the number
 * of times a message was redelivered, a particular Action can be invoked. Actions are
 * delay, moving or deleting the message.
 * 
 * The msgid cache is not persistent, nor is it shared between multiple activations. This
 * means that if a message was seen 10 times with the redelivered flag set, and the 
 * project is undeployed, the redelivery count will be reset to zero. Also, if there are
 * multiple application servers reading from the same queue, a message may be redelivered
 * 10 times to one application server, and 10 times to the other application server, and
 * both activations will see a count of 10 instead of 20.
 * 
 * The msgid cache is limited to 5000 (check source); when this limit is reached, the 
 * oldest msgids are flushed from the cache. "Oldest" means least recently seen.
 * 
 * Specification of the actions is done through a specially formatted string. The string
 * has this format:
 *    format := entry[; entry]*
 *    entry := idx ":" action
 *    idx := number (denotes the n-th time a msg was seen)
 *    action := number (denotes delay in ms) | "delete" | "move"(args)
 *    move := "queue"|"topic" | "same" ":" destname
 *    destname :=  any string, may include "$" which will be replaced with the original
 *        destination name.
 * 
 * Examples:
 *     5:1000; 10:5000; 50:move(queue:mydlq)
 * This causes no delay up to the 5th delivery; a 1000 ms delay is invoked when the
 * message is seen the 5th, 6th, 7th, 8th, and 9th time. A 5 second delay is invoked
 * when the msg is invoked the 10th, 11th, ..., 49th time. When the msg is seen the 50th
 * time the msg is moved to a queue with the name "mydlq".
 * 
 * If the messages were received from "Queue1" and if the string was specified as
 *     5:1000; 10:5000; 50:move(queue:dlq$oops)
 * the messages would be moved to the destination "dlqQueue1oops".
 * 
 * Another example:
 *     5:1000; 10:5000
 * This causes no delay up to the 5th delivery; a 1000 ms delay is invoked when the
 * message is seen the 5th, 6th, 7th, 8th, and 9th time. A 5 second delay is invoked
 * for each time the message is seen thereafter.
 * 
 * Moving messages is done in the same transaction if the transaction is XA. Moving
 * messages is done using auto-commit if the delivery is non-XA.
 * 
 * Moving messages is done by creating a new message of the same type unless the 
 * property JMSJCA.redeliveryRedirect is set to true in which case the messages are 
 * simply redirected. In the first case, the payload of the new message is set as follows:
 * - for a ObjectMessage this will be done through getObject(), setObject(); 
 * - for a StreamMessage through readObject/writeObject, 
 * - for a BytesMessage through readBytes() and writeBytes() 
 *   (avoiding the getBodyLength() method new in JMS 1.1)
 * Copying the payload of an ObjectMessage may cause classloader problems since the 
 * context classloader is not properly set. In this case the redelivery handler should
 * be configured to redirect the message instead. 
 * The new message will have properties as follows:
 * * JMS properties
 * - JMSCorrelationID: copied
 * - JMSDestination: see above; set by JMS provider
 * - JMSExpiration: copied through the send method
 * - JMSMessageID: set by the JMS provider 
 * - JMSPriority: set by the JMS provider; propagated through the send() method
 * - JMSRedelivered: NOT copied
 * - JMSReplyTo: copied
 * - JMSTimestamp: copied into the user property field JMSJCATimestamp
 * - JMSType: copied
 * - JMSDeliveryMode: set by the JMS provider; propagated through the send() method
 * * All user defined properties: copied
 * * Additional properties:
 * - JMS_Sun-JMSJCA.RedeliveryCount: number of times the message was seen with the redelivered 
 *   flag set by JMSJCA. Will accurately reflect the total number of redelivery attempts 
 *   only if there's one instance of the inbound adapter, and the inbound adapter was 
 *   not redeployed.
 * - JMS_Sun-JMSJCA.OriginalDestinationName: name of the destination as specified in the 
 *   activation spec
 * - JMS_Sun-JMSJCA.OriginalDestinationType: either "javax.jms.Queue" or "javax.jms.Topic"
 * - JMS_Sun-JMSJCA.SubscriberName: as specified in the activation spec
 * - JMS_Sun-JMSJCA.ContextName: as specified in the activation spec
 * 
 * Invoking a delay takes place by holding the processing thread occupied, that means 
 * that while the thread is sleeping, this thread will not be used to process any other
 * messages. Undeployment interrupts threads that are delaying message delivery. If a
 * msg delay is divisible by 1000, an INFO message is written to the log indicating that
 * the thead is delaying message delivery.
 * 
 * There is a default behavior for message redelivery handling: see source.
 * 
 * Implementation notes: this class is made abstract to enhance testability.
 * 
 * Notes on Redelivery handling, HUA, and batching:
 * ================================================
 * Sync mode scenarios:
 * m1, m2, m3*
 * (* = to be dlq-ed)
 * 
 * with HUA:
 * m1, m2, m3*, R
 * (R = rollback)
 * 
 * Non XA:
 * - afterdelivery is not an issue
 * - WL bypass is not an issue
 * - move msg cannot be committed in the deliver() method but will have to be
 *   committed at the end when the commit status is known
 *   
 * XA:
 * - WL bypass commit should not be done because tx outcome not yet known
 * - move connection's xaresource may be enlisted multiple times
 * 
 * CC scenarios:
 * m1, m2, m3*, m4
 * m1, m2R, m3*, m4R
 * 
 *
 * @author fkieviet
 * @version $Revision: 1.6 $
 */
public abstract class RedeliveryHandler {
    private static Logger sLog = Logger.getLogger(RedeliveryHandler.class);
    private DeliveryStats mStats;
    private int mLookbackSize;
    private HashMap mOldMsgs;
    private HashMap mNewMsgs;
    private boolean mLoggedOnce;
    private Action[] mActions;
    private String mActionStr;
    private RAJMSActivationSpec mActivationSpec;

    private static final Localizer LOCALE = Localizer.get();
    
    /**
     * A baseclass of all actions that could happen in response to a a repeated 
     * redelivered message
     */
    public abstract static class Action {
        private int mAt;
        
        /**
         * Constructor
         * 
         * @param at at which encounter to invoke
         */
        public Action(int at) {
            if (at <= 0) {
                throw Exc.rtexc(LOCALE.x("E111: Index {0} should be > 0", Integer.toString(at)));
            }
            if (at > 5000) {
                throw Exc.rtexc(LOCALE.x("E112: Index {0} should be < 5000", Integer.toString(at)));
            }
            mAt = at;
        }
        
        /**
         * @return at which encounter to invoke
         */
        public int getAt() {
            return mAt;
        }

        /**
         * Asserts that the next value is greater than the previous value
         * 
         * @param lastAt last value
         * @return current value of lastAt
         * @throws Exception on assertion failure
         */
        public int checkLast(int lastAt) throws Exception {
            if (lastAt == mAt) {
                throw Exc.exc(LOCALE.x("E107: Duplicate entry at: {0}", Integer.toString(lastAt)));
            }
            if (lastAt >= mAt) {
                throw Exc.exc(LOCALE.x("E106: Should be properly ordered: {0} >= {1}"
                    , Integer.toString(lastAt), Integer.toString(mAt)));
            }
            return mAt;
        }
        
        /**
         * Indicates if this action has determined if the message should be delivered
         * to the endpoint, or should be acknowledged without delivering.
         * 
         * @param owner owns this action (for callbacks)
         * @param m message that was received
         * @param e count/msgid of that msg
         * @param cookie represents the connection to be used to move the msg
         * @return true if the msg should be delivered to the endpoint
         * @throws Exception on failure
         */
        public abstract boolean shouldDeliver(RedeliveryHandler owner, Message m, 
            Encounter e, Object cookie) throws Exception;
    }
    
    /**
     * No action; always at the beginning
     */
    public static class VoidAction extends Action {
        /**
         * Constructor
         */
        public VoidAction() {
            super(1);
        }
        
        /**
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return "Void";
        }

        /**
         * @see com.stc.jmsjca.core.RedeliveryHandler.Action#shouldDeliver(
         * com.stc.jmsjca.core.RedeliveryHandler, javax.jms.Message, 
         * com.stc.jmsjca.core.RedeliveryHandler.Encounter, 
         * com.stc.jmsjca.core.RedeliveryHandler.MessageMover)
         */
        public boolean shouldDeliver(RedeliveryHandler owner, Message m, Encounter e, 
            Object cookie) throws Exception {
            // Do nothing
            return true;
        }
    }

    /**
     * A delay action
     */
    public static class Delay extends Action {
        /**
         * How to recognize a delay
         */
        public static final String PATTERN = "(\\d+):\\s?(\\d+)";
        /**
         * Compiled regex pattern
         */
        public static Pattern sPattern = Pattern.compile(PATTERN);
        private long mDelay;
        private static long MAX = 5000;
        
        /**
         * Constructor
         * 
         * @param at when
         * @param delay how long (ms)
         * @throws Exception on invalid arguments
         */
        public Delay(int at, long delay) throws Exception {
            super(at);
            if (delay > MAX && delay != Integer.MAX_VALUE) {
                // Note: max_value is used for testing
                throw Exc.exc(LOCALE.x("E109: Delay of [{0}] exceeds maximum of {1} ms"
                    , Long.toString(delay), Long.toString(MAX)));
            }
            mDelay = delay;
        }
        
        /**
         * @return delay time in ms
         */
        public long getHowLong() {
            return mDelay;
        }
        
        /**
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return "At " + getAt() + ": delay for " + mDelay + " ms";
        }

        /**
         * @see com.stc.jmsjca.core.RedeliveryHandler.Action#shouldDeliver(
         * com.stc.jmsjca.core.RedeliveryHandler, javax.jms.Message, 
         * com.stc.jmsjca.core.RedeliveryHandler.Encounter, 
         * com.stc.jmsjca.core.RedeliveryHandler.MessageMover)
         */
        public boolean shouldDeliver(RedeliveryHandler owner, Message m, Encounter e, 
            Object cookie) throws Exception {
            owner.delayMessageDelivery(m, e, mDelay);
            return true;
        }
    }
    
    /**
     * Moves a msg to a different queue or topic
     */
    public static class Move extends Action {
        /**
         * How to recognize a delay
         */
        public static final String PATTERN = "(\\d+):\\s?move\\((.*)\\)";
        /**
         * Compiled version
         */
        public static Pattern sPattern = Pattern.compile(PATTERN);
        
        /**
         * The argument pattern
         */
        public static String ARGPATTERN =  "(queue|same|topic)\\s?:\\s?(.+)";
        /**
         * Compiled version
         */
        public static Pattern sArgPattern = Pattern.compile(ARGPATTERN);
        
        private String mType; // either javax.jms.Queue or javax.jms.Topic
        private String mName;
        
        /**
         * @param at when to invoke
         * @param type destination type
         * @param name destination name 
         * @param destinationType type from activation spec
         * @throws Exception on failure
         */
        public Move(int at, String type, String name, String destinationType) throws Exception {
            super(at);
            mName = name;
            if (!Queue.class.getName().equals(destinationType) && !Topic.class.getName().equals(destinationType)) {
                throw Exc.exc(LOCALE.x("E110: Invalid destination type [{0}]", destinationType));
            }
            if ("same".equals(type)) {
                mType = destinationType;
            } else if ("queue".equals(type)) {
                mType = Queue.class.getName();
            } else if ("topic".equals(type)) {
                mType = Topic.class.getName();
            } else {
                throw Exc.exc(LOCALE.x("E108: Invalid type [{0}]", type));
            }
        }
        
        /**
         * @return javax.jms.Queue or javax.jms.Topic
         */
        public String getDestinationType() {
            return mType;
        }
        
        /**
         * @return true if Queue
         */
        public boolean isQueue() {
            return mType.equals(Queue.class.getName());
        }
        
        /**
         * @return true if topic
         */
        public boolean isTopic() {
            return mType.equals(Topic.class.getName());
        }
        
        /**
         * @return destination name to use for DLQ
         */
        public String getDestinationName() {
            return mName;
        }
        
        /**
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return "At " + getAt() + ": move to " + mType + " with name [" + mName + "]";
        }

        /**
         * @see com.stc.jmsjca.core.RedeliveryHandler.Action#shouldDeliver(
         * com.stc.jmsjca.core.RedeliveryHandler, javax.jms.Message, 
         * com.stc.jmsjca.core.RedeliveryHandler.Encounter, 
         * com.stc.jmsjca.core.RedeliveryHandler.MessageMover)
         */
        public boolean shouldDeliver(RedeliveryHandler owner, Message m, Encounter e, 
            Object cookie) throws Exception {
            owner.move(m, e, isTopic(), getDestinationName(), cookie);
            return false;
        }
    }
    
    /**
     * Deletes a msg
     */
    public static class Delete extends Action {
        /**
         * How to recognize a delete
         */
        public static final String PATTERN = "(\\d+):\\s?delete";
        /**
         * Compiled
         */
        public static Pattern sPattern = Pattern.compile(PATTERN);
        
        /**
         * Constructor
         * 
         * @param at when
         * @throws Exception on illegal argument 
         */
        public Delete(int at) throws Exception {
            super(at);
        }

        /**
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return "At " + getAt() + ": delete";
        }

        /**
         * @see com.stc.jmsjca.util.RepeatedRedeliveryActionParser.Action#process(
         * com.stc.jmsjca.core.RedeliveryHandler.MessageMover, javax.jms.Message, 
         * com.stc.jmsjca.core.RedeliveryHandler.Encounter)
         */
        public boolean shouldDeliver(RedeliveryHandler owner, Message m, Encounter e, 
            Object cookie) throws Exception {
            owner.deleteMessage(m, e);
            return false;
        }
    }

    /**
     * @param actions action string
     * @return true if can be parsed properly
     */
    public static boolean checkValid(String actions) {
        try {
            parse(actions, "nothing", Queue.class.getName());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @param m message to move
     * @param e msgid/count
     * @param isTopic isTopic
     * @param destinationName to move to
     * @param cookie represents the connection to be used for moving
     * @throws Exception on failure
     */
    protected abstract void move(Message m, Encounter e, boolean isTopic,
        String destinationName, Object cookie) throws Exception;
    
    /**
     * Gives the MDB the option to shut down the connector
     * 
     * @param s reason for shutdown
     */
    protected abstract void stopConnector(String s);
        
    /**
     * Parses an action string into separate actions and performs validations. 
     * The returned action array is guaranteed to be ordered and without duplicates.
     * 
     * @param s string to be parsed
     * @param destName destination name being used (for dlq name construction)
     * @param destType type from activation spec (javax.jms.Queue or javax.jms.Topic)
     * @return array of actions
     * @throws JMSException upon parsing failure
     */
    public static Action[] parse(String s, String destName, String destType) throws JMSException {
        if (s.trim().length() == 0) {
            return new Action[] {new VoidAction() };
        }
        
        // Split the string in different actions
        String[] actions = s.split("\\s*;\\s*");
        Action[] ret = new Action[actions.length];
        
        // Go over all actions and try to parse each action
        int lastAt = 0;
        for (int i = 0; i < actions.length; i++) {
            if (sLog.isDebugEnabled()) {
                sLog.debug(("Now parsing [" + actions[i] + "]"));
            }

            try {
                Matcher m;
                boolean last = i == (actions.length - 1);
                
                // Delay
                m = Delay.sPattern.matcher(actions[i]);
                if (m.matches()) {
                    String at = m.group(1);
                    String delay = m.group(2);
                    ret[i] = new Delay(Integer.parseInt(at), Long.parseLong(delay));
                    lastAt = ret[i].checkLast(lastAt);
                    if (sLog.isDebugEnabled()) {
                        sLog.debug(ret[i]);
                    }
                    continue;
                }
                
                // Delete
                m = Delete.sPattern.matcher(actions[i]);
                if (m.matches()) {
                    String at = m.group(1);

                    if (!last) {
                        throw Exc.jmsExc(LOCALE.x("E105: Delete command should be last command"));
                    }
                    
                    ret[i] = new Delete(Integer.parseInt(at));
                    lastAt = ret[i].checkLast(lastAt);
                    if (sLog.isDebugEnabled()) {
                        sLog.debug(ret[i]);
                    }
                    continue;
                }
                
                // Move
                m = Move.sPattern.matcher(actions[i]);
                if (m.matches()) {
                    String at = m.group(1);
                    String guts = m.group(2);
                    Matcher g = Move.sArgPattern.matcher(guts);
                    if (!g.matches()) {
                        throw Exc.jmsExc(LOCALE.x("E104: Wrong arguments: should match {0}", Move.ARGPATTERN));
                    }
                    String type = g.group(1);
                    String name = g.group(2);
                    
                    if (!last) {
                        throw Exc.jmsExc(LOCALE.x("E103: Move command should be last command"));
                    }
                    
                    name = name.replaceAll("\\$", destName);
                    
                    ret[i] = new Move(Integer.parseInt(at), type, name, destType);
                    lastAt = ret[i].checkLast(lastAt);
                    if (sLog.isDebugEnabled()) {
                        sLog.debug(ret[i]);
                    }
                    continue;
                }
                
                throw Exc.jmsExc(LOCALE.x("E102: Token not recognized: {0}", actions[i]));
            } catch (Exception e) {
                throw Exc.jmsExc(LOCALE.x("E101: Could not parse [{0}]: error [{1}]"
                    + " in element number {2}: [{3}]", s, e, Integer.toString(i), actions[i]), e);
            }
        }
        
        return ret;
    }

    /**
     * Represents a msg that was seen with the redelivered flag set 
     * 
     * @author fkieviet
     */
    public class Encounter {
        private int mNEncountered;
        private int mActionCursor;
        private String mMsgid;
        private Map mStatefulRedeliveryProperties;
        private Action[] mEncActions;
        private String mEncActionString;
        
        /**
         * Constructor 
         * 
         * @param msgid jms msg id
         * @param actions actions to perform upon redelivery
         * @param actionString for readback: actions to perform on redelivery
         */
        public Encounter(String msgid, Action[] actions, String actionString) {
            mMsgid = msgid;
            mEncActions = actions;
            mEncActionString = actionString;
        }
        
        /**
         * Called when a msg with the redelivered flag is seen
         */
        public final void encounteredAgain() {
            mNEncountered++;
        }
        
        /**
         * @return number of times this msg was seen with the redelivered flag set
         */
        public final int getNEncountered() {
            return mNEncountered;
        }
        
        /**
         * @return index into action array
         */
        public final int getActionCursor() {
            return mActionCursor;
        }
        
        /**
         * Sets the index into the actions array
         * 
         * @param actionCursor index into actions array
         */
        public final void setActionCursor(int actionCursor) {
            mActionCursor = actionCursor;
        }

        /**
         * @return msgid for this encounter
         */
        public String getMsgid() {
            return mMsgid;
        }

        /**
         * @return properties that the user can set on a msg
         */
        public Map getStatefulRedeliveryProperties() {
            if (mStatefulRedeliveryProperties == null) {
                mStatefulRedeliveryProperties = new HashMap();
            }
            return mStatefulRedeliveryProperties;
        }
        
        /**
         * @return Actions
         */
        public Action[] getEncActions() {
            return mEncActions;
        }

        /**
         * Getter for encActionString
         *
         * @return String
         */
        public String getEncActionString() {
            return mEncActionString;
        }

        /**
         * Associates a new set of actions with this encounter
         *  
         * @param a actions
         * @param s actions in the form of a string for readback
         * @param newcursor index into array where the next action should take place
         */
        public void setNewActions(Action[] a, String s, int newcursor) {
            mEncActions = a;
            mEncActionString = s;
            setActionCursor(newcursor);
        }
    }

    /**
     * Constructor
     * 
     * @param spec activation spec
     * @param stats runtime statistics
     * @param lookbackSize cache size (configurable for testing)
     * @throws Exception
     */
    public RedeliveryHandler(RAJMSActivationSpec spec, DeliveryStats stats, int lookbackSize) {
        mStats = stats;
        mLookbackSize = lookbackSize;
        mActivationSpec = spec;
        mOldMsgs = new HashMap();
        mNewMsgs = new HashMap(mLookbackSize);
        
        // Setup actions
        Action[] actions = new Action[0];
        try {
            actions = parse(spec.getRedeliveryHandling(), 
                spec.getDestination(), spec.getDestinationType());
        } catch (Exception e) {
            sLog.warn(LOCALE.x("E050: Unexpected exception parsing of redelivery actions: {0}", e), e);
        }
        mActions = sanitizeActions(actions);
        mActionStr = spec.getRedeliveryHandling();
    }
    
    /**
     * Ensure that there is an action with index = 1: there are ALWAYS actions, and
     * the first action (i.e. with index = 1) ALWAYS exists.
     * 
     * @param actions actions to cleanup
     * @return valid action array
     */
    private static Action[] sanitizeActions(Action[] actions) {
        Action[] ret;
        // Ensure that there is an action with index = 1: there are ALWAYS actions, and
        // the first action (i.e. with index = 1) ALWAYS exists.
        if (actions.length == 0) {
            ret = new Action[] {new VoidAction() };
        } else if (actions[0].getAt() != 1) {
            ret = new Action[actions.length + 1];
            ret[0] = new VoidAction();
            for (int i = 0; i < actions.length; i++) {
                ret[i + 1] = actions[i];
            }
        } else {
            ret = actions;
        }
        return ret;
    }
    
    /**
     * A state handler that is used when the message is not seen before, so it may not
     * need to have an Encounter object, which is the typical case. To conserve memory
     * for the typical case, the creation of Encounters should be avoided until the point
     * that the application indeed wants to create state on the message.
     * 
     * This delegates to a Handler that represents a real Encounter.
     * 
     * This object is created if the RedeliveryFlag was set to false; hence there is no
     * Encounter and no state associated with the msg.
     * 
     * @author fkieviet
     */
    private class LazyRedeliveryState implements WMessageIn.RedeliveryStateHandler {
        private Message mMsg;
        private WMessageIn.RedeliveryStateHandler mRealHandler;
        
        public LazyRedeliveryState(Message m) {
            mMsg = m;
        }
        
        /**
         * Ensures that an Encounter object exists and that the RealHandler is set
         * 
         * @throws JMSException propagated from getJMSMessageID()
         */
        private void checkEncounter() throws JMSException {
            if (mRealHandler != null) {
                return;
            }
            String msgid = mMsg.getJMSMessageID();
            if (msgid == null) {
                throw Exc.jmsExc(LOCALE.x("E033: Cannot retain state for this message: "
                    + "the getJMSMessageID() method of this method returns null."));
            }
            mRealHandler = new ActiveRedeliveryState(getEncounter(msgid, false));
        }
        
        /**
         * @see com.stc.jmsjca.core.WMessageIn.RedeliveryStateHandler#getProperty(java.lang.String)
         */
        public String getProperty(String key) throws JMSException {
            if (Options.MessageProperties.REDELIVERY_HANDLING.equals(key)) {
                return mActionStr;
            }
            if (mRealHandler == null) {
                return null;
            } else {
                return mRealHandler.getProperty(key);
            }
        }

        /**
         * @see com.stc.jmsjca.core.WMessageIn.RedeliveryStateHandler#setProperty(java.lang.String, java.lang.String)
         */
        public boolean setProperty(String key, String value) throws JMSException {
            // It's extremely likely that setting properties involves state  
            checkEncounter();
            return mRealHandler.setProperty(key, value);
        }

        /**
         * @see com.stc.jmsjca.core.WMessageIn.RedeliveryStateHandler#getRedeliveryCount()
         */
        public int getRedeliveryCount() {
            return 0;
        }
    }
    
    /**
     * State handler for an existing Encounter
     * 
     * @author fkieviet
     */
    private class ActiveRedeliveryState implements WMessageIn.RedeliveryStateHandler {
        private Encounter mEncounter;
        
        /**
         * @param e Encounter
         */
        public ActiveRedeliveryState(Encounter e) {
            mEncounter = e;
        }
        
        /**
         * @see com.stc.jmsjca.core.WMessageIn.RedeliveryStateHandler#getProperty(java.lang.String)
         */
        public String getProperty(String key) {
            if (Options.MessageProperties.REDELIVERY_HANDLING.equals(key)) {
                return mEncounter.getEncActionString();
            } else {
                return (String) mEncounter.getStatefulRedeliveryProperties().get(key);
            }
        }

        /**
         * @see com.stc.jmsjca.core.WMessageIn.RedeliveryStateHandler#setProperty(java.lang.String, java.lang.String)
         */
        public boolean setProperty(String key, String value) throws JMSException {
            if (key.startsWith(Options.MessageProperties.USER_ROLLBACK_DATA_PREFIX)) {
                // User data
                mEncounter.getStatefulRedeliveryProperties().put(key, value);
                return true;
            } else if (key.startsWith(Options.MessageProperties.REDELIVERY_HANDLING)) {
                // Redelivery
                setActions(value, mEncounter);
                return true;
            } else if (key.startsWith(Options.MessageProperties.STOP_CONNECTOR)) {
                stopConnector(value);
                return true;
            } else {
                return false;
            }
        }

        /**
         * @see com.stc.jmsjca.core.WMessageIn.RedeliveryStateHandler#getRedeliveryCount()
         */
        public int getRedeliveryCount() {
            return mEncounter.getNEncountered();
        }
    }
    
    private void setActions(String s, Encounter enc) throws JMSException {
        Action[] a = parse(s, mActivationSpec.getDestination(), mActivationSpec.getDestinationType());
        a = sanitizeActions(a);
        
        // Calculate action cursor
        int ienc = enc.getNEncountered();
        int newcursor = 0;
        for (int cursor = 0; cursor < a.length; cursor++) {
            if (a[cursor].getAt() <= ienc) {
                newcursor = cursor;
            } else {
                break;
            }
        }

        // Update
        enc.setNewActions(a, s, newcursor);
    }
    
    /**
     * Called for each message that is received. This will take appropriate action 
     * if the message has the redelivery flag set.
     * 
     * @param cookie represents the connection to be used for moving
     * @param m message to consider
     * @return true if the message should be delivered to the endpoint; false if the
     * message just should be acknowledged.
     */
    public boolean shouldDeliver(Object cookie, Message m) {
        WMessageIn wmsg = null;
        if (m instanceof WMessageIn) {
            wmsg = (WMessageIn) m;
        }

        // Obtain msgid; ignore non-redelivered messages and failures
        String msgid;
        try {
            if (!m.getJMSRedelivered()) {
                if (wmsg != null) {
                    // Not redelivered; the application MAY set state on this msg.
                    // This is unlikely, so only create an Encounter if this is indeed
                    // done by the application.
                    wmsg.setRedeliveryState(new LazyRedeliveryState(wmsg.getDelegate()));
                }
                
                return true;
            }
            
            msgid = m.getJMSMessageID();
            if (msgid == null) {
                if (!mLoggedOnce) {
                    mLoggedOnce = true;
                    sLog.warn(LOCALE.x("E051: Message could not be checked for redelivery: msg id is "
                        + "null. For subsequent similar cases, this warning will not "
                        + "be logged."));
                }
                return true;
            }
        } catch (JMSException e) {
            if (!mLoggedOnce) {
                mLoggedOnce = true;
                sLog.warn(LOCALE.x("E052: Message could not be checked for redelivery: " 
                    + "{0}; for subsequent similar cases, this warning will not be logged.", e), e);
            }
            return true;
        }

        
        // Find previous or new encounter
        Encounter enc = getEncounter(msgid, true);
        
        if (wmsg != null) {
            // Associate the encounter with the message so that if the message gets
            // delivered to the application, the application can get data from and set
            // data to the encounter.
            wmsg.setRedeliveryState(new ActiveRedeliveryState(enc));
        }
       
        // Bump up counter
        enc.encounteredAgain();
        
        if (m instanceof Unwrappable) {
            m = (Message) ((Unwrappable) m).getWrappedObject();
        }
        
        // Undertake action
        return shouldDeliver(cookie, enc, m);
    }
    
    /**
     * Stores an exception in an encounter
     * 
     * @param e exception
     * @param m message
     */
    public void rememberException(Exception e, Message m) {
        String msgid = null;
        try {
            msgid = m.getJMSMessageID();
        } catch (JMSException ex) {
            // This failure is likely endemic for the provider and is
            // logged in shouldDeliver()
        }
        if (msgid == null) {
            // Nothing that can be done
        } else {
            // Find previous or new encounter
            Encounter enc = getEncounter(msgid, false);
            enc.getStatefulRedeliveryProperties().put(
                Options.MessageProperties.LAST_EXCEPTIONMESSAGE, e.getMessage());
            enc.getStatefulRedeliveryProperties().put(
                Options.MessageProperties.LAST_EXCEPTIONCLASS, e.getClass().getName());
            enc.getStatefulRedeliveryProperties().put(
                Options.MessageProperties.LAST_EXCEPTIONTRACE, Exc.getStackTrace(e));
        }
    }

    private synchronized Encounter getEncounter(String msgid, boolean redelivered) {
        // Check most recent msgs first (msg may be in old AND new cache)
        if (redelivered) {
            mStats.msgRedelivered();
        }
        Encounter enc = (Encounter) mNewMsgs.get(msgid);
        if (enc == null) {
            // Check old msgs
            enc = (Encounter) mOldMsgs.get(msgid);
            if (enc == null) {
                // First encounter
                enc = new Encounter(msgid, mActions, mActionStr);
                if (redelivered) {
                    mStats.msgRedeliveredFirstTime();
                }
            }
            
            // Ensure msg is in most recent cache
            mNewMsgs.put(msgid, enc);
            
            // Check for cache overflow
            if (mNewMsgs.size() > mLookbackSize) {
                // Discard the old segment, create a new segment
                mOldMsgs = mNewMsgs;
                mNewMsgs = new HashMap(mLookbackSize);
            }
        }
        return enc;
    }
    
    private boolean shouldDeliver(Object cookie, Encounter enc, Message m) {
        // Preconditions:
        // - ienc indicates the number of times this msg was seen before, i.e. 1 on the 
        //   first redelivery
        // - cursor points to action to undertake
        // Post condition: 
        // - cursor points to action to undertake on next encounter
        // - actions[cursor].getAt() == ienc
        int ienc = enc.getNEncountered();
        int cursor = enc.getActionCursor();
        
        // Action to execute:
        Action action = enc.mEncActions[cursor];
        
        // Move to next action? E.g. nextaction.at = 11, ienc = 11
        if (cursor < (enc.mEncActions.length - 1)) {
            Action next = enc.mEncActions[cursor + 1];
            if (next.getAt() == ienc) {
                enc.setActionCursor(cursor + 1);
                action = next;
            }
        }
        
        try {
            return action.shouldDeliver(this, m, enc, cookie);
        } catch (Exception e) {
            sLog.warn(LOCALE.x("E053: Message with msgid {0} was redelivered {1} times"
                    + "; the action [{2}] failed: {3}", enc.getMsgid(), 
                    Integer.toString(enc.getNEncountered()), action, e), e);
            return true;
        }
    }
    
    /**
     * Made abstract to enhance testability; to be implemented by the user of this
     * class; will be called whenever a message delivery should be delayed
     * 
     * @param m message
     * @param e encounter
     * @param delay how long to delay
     */
    protected abstract void delayMessageDelivery(Message m, Encounter e, long delay);

    /**
     * Made abstract to enhance testability; will be called when a message should be
     * deleted
     * 
     * @param m message
     * @param e encounter
     */
    protected abstract void deleteMessage(Message m, Encounter e);

}
