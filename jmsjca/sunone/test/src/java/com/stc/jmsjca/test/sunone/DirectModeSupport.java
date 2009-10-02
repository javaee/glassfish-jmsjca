package com.stc.jmsjca.test.sunone;

import java.util.Properties;

import com.sun.messaging.jmq.jmsclient.runtime.BrokerInstance;
import com.sun.messaging.jmq.jmsclient.runtime.ClientRuntime;
import com.sun.messaging.jmq.jmsservice.BrokerEvent;
import com.sun.messaging.jmq.jmsservice.BrokerEventListener;

public class DirectModeSupport {

    public static BrokerInstance startEmbeddedbroker(Properties serverProperties)
        throws Exception {

        // Ideally should use the LifecycleManagedBroker APi to create an
        // embedded broker
        // This doesn't work yet, so do it the long way
        // if (lmb ==null){
        // lmb = new LifecycleManagedBroker();
        // lmb.setBrokerType(LifecycleManagedBroker.BROKER_TYPE_DIRECT);
        // lmb.setBrokerHomeDir(mServerProperties.getProperty(SunOneProvider.PROPNAME_IMQHOME));
        // lmb.start();
        // }

        ClientRuntime clientRuntime = ClientRuntime.getRuntime();

        BrokerInstance brokerInstance = clientRuntime.createBrokerInstance();

        String[] args = new String[6];
        args[0] = "-imqhome";
        String imqhome = serverProperties.getProperty(SunOneProvider.PROPNAME_IMQHOME);
        if (imqhome == null)
            throw new Exception("Property " + SunOneProvider.PROPNAME_IMQHOME
                + " not set");
        args[1] = imqhome;
        args[2] = "-port";
        // choose a unusual port to avoid conflicting with any default
        // broker that may be already running
        args[3] = "5678";
        args[4] = "-name";
        args[5] = "embeddedbroker";
        Properties props = brokerInstance.parseArgs(args);

        BrokerEventListener listener = new BrokerEventListener() {
            public void brokerEvent(BrokerEvent error) {
            }

            public boolean exitRequested(BrokerEvent event, Throwable thr) {
                return true;
            }
        };
        brokerInstance.init(props, listener);

        // now start the embedded broker
        brokerInstance.start();

        return brokerInstance;

    }

    public static void shutdownEmbeddedBroker(BrokerInstance brokerInstance) {
        brokerInstance.stop();
        brokerInstance.shutdown();
    }

}
