cd C:\Work\innovation\h2-server\bin
start java -cp h2-1.4.200.jar org.h2.tools.Server -tcp -tcpAllowOthers -ifNotExists -tcpPort 9092


cd C:\Work\innovation\ocr-server
"C:\Program Files\Java\jdk-11.0.4\bin\java.exe" -Dspring.profiles.active=test -Dmaven.multiModuleProjectDirectory=C:\Work\innovation\ocr-server -Xmx8000m "-Dmaven.home=C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2019.2.3\plugins\maven\lib\maven3" "-Dclassworlds.conf=C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2019.2.3\plugins\maven\lib\maven3\bin\m2.conf" "-Dmaven.ext.class.path=C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2019.2.3\plugins\maven\lib\maven-event-listener.jar" "-javaagent:C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2019.2.3\lib\idea_rt.jar=62422:C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2019.2.3\bin" -Dfile.encoding=UTF-8 -classpath "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2019.2.3\plugins\maven\lib\maven3\boot\plexus-classworlds-2.6.0.jar" org.codehaus.classworlds.Launcher -Didea.version2019.2.3 -T 2C -DskipTests=true org.springframework.boot:spring-boot-maven-plugin:2.1.0.RELEASE:run
