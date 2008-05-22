test_readme.txt
---------------

This  readme file  is  to  help run  JMS-Grid  test  suite against Weblogic  and
Websphere application servers.


JMS-Grid JMSJCA for Weblogic 9.2 and 10.0
-----------------------------------------
Run tests  for  JMS-Grid  JMSJCA in  Weblogic 9.2  and 10.0   version
application servers.

1) install and start 5.2 JMS-Grid using all default settings

2) install and start Weblogic 9.2/10.0 using all default settings

3) change properties values in build.properties
testcontainer.id            = wl
testjcaids.wl               = wave
testcontainer.properties.wl = ${module.root}/wave/meta/wlcontainer.properties
settings.path		    = ${module.root}/wave/meta/wlcontainer.properties
wl.install.lib.path         = WLS install lib path
(comment out settings.path STC_ROOT/RTS/build.properties)

4) run envTestWLS.bat to switch jdk1.5.0_12 and over

5) run tests from command line
ant install
ant do-test

6) run tests in eclipse, add below VM properties in eclipse
-Dtest.container.id=wl
-Dtest.server.properties=STC_ROOT/jmsjca/wave/meta/wlcontainer.properties
-Dtest.container.properties=STC_ROOT/jmsjca/wave/meta/wlcontainer.properties
-Dtest.ear.path=STC_ROOT/BUILD/Modules/jmsjca/rawave/test/ratest-test.ear
-Dwave.url=tcp://localhost:50607


JMS-Grid JMSJCA for Webshpere 6.0 and 6.1
-----------------------------------------
Run  tests for  JMS-Grid JMSJCA  in Webshpere  6.0 and  6.1 version  application
servers. The test clients run in IBM JVM.

1) install and start Websphere 6.0/6.1

2) change properties values in build.properties
testcontainer.id                 = was
testjcaids.was                   = wave
testcontainer.properties.was     = ${module.root}/wave/meta/wascontainer.properties
settings.path                    = ${module.root}/wave/meta/wascontainer.properties
was.install.lib.path             = WAS install lib path

4) run envTestWAS.bat to switch IBM JDK 1.4.2 with Websphere 6.0/6.1

5) configure Websphere 6.0/6.1 application servers
5.1) create JMS provider
- specify JMSGridProvider
- specify External initial context factory
com.spirit.directory.SpiritDirectoryContextFactory
- specify External provider URL
tcp://localhost:50607

5.2) create connectionfactory resource
- specify queuefact
- specify eis/javax.jms.QueueConnectionFactory
- specify connectionfactories/xaqueueconnectionfactory
- specify topicfact
- specify eis/javax.jms.TopicConnectionFactory
- specify connectionfactories/xatopicconnectionfactory

5.3) create queue resource
- specify Queue1
- specify eis/javax.jms.Queue
- specify queues/Queue1
 
5.4) create listening port
- specify JMSGridProviderPort
- specify Connection factory JNDI name
eis/javax.jms.QueueConnectionFactory
- specify Destination JNDI name
eis/javax.jms.Queue

6) run tests from command line
ant install
ant do-test

7) run tests in eclipse, add below VM properties in eclipse
-Dtest.container.id=was
-Dtest.server.properties=STC_ROOT/jmsjca/wave/meta/wascontainer.properties
-Dtest.container.properties=STC_ROOT/jmsjca/wave/meta/wascontainer.properties
-Dtest.ear.path=STC_ROOT/BUILD/Modules/jmsjca/rawave/test/ratest-test.ear
-Dwave.url=tcp://localhost:50607

Notes on running JMS-Grid
-------------------------
-JMS-Grid runs on default port 50607.
-Enable JMS-Grid Management Console, install Tomcat 6.x version.
-Run Tomcat on default port 8080. 
-Access to http://localhost:8080/jmxConsole using Administrator/STC as username/password.


Notes on running Weblogic 9.1/9.2/10.0 application servers
----------------------------------------------------------
-Weblogic runs on default port 7001.
-Access to http://localhost:7001/console using weblogic/weblogic as username/password.

Notes on running Websphere 6.0/6.1 application servers
-Websphere runs on default port 9060.
-Access to http://localhost:9060/ibm/console/ using empty username/password .
 
 
 