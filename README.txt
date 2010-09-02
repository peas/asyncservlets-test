Performance and scalability test for Tomcat7 and Jetty8 AsyncContext.

You need JDK6 (linking it @JAVA_HOME) and gradle (gradle.org)

You should do like
:
gradle runTomcat7 
or
gradle runJetty8

and then

gradle runGrinder

to be able to see some action from the command line:
curl http://127.0.0.1/asyncservlets-test/subscribe

To broadcast a message to every single client connected
curl http://127.0.0.1/asyncservlets-test/subscribe -danything

