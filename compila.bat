set JAVA_HOME="C:\jdk-19"
mvn clean compile assembly:single
copy config.properties ./target
copy testbiblioteca.mv.db ./target
copy testbiblioteca.trace.db ./target
"C:\jdk-19\bin\java" -jar biblioteca-1.0-SNAPSHOT-jar-with-dependencies.jar
http://127.0.0.1:8080/biblioteca/src/main/webapp/login/login.html