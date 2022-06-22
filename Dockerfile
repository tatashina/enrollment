FROM openjdk:17-oracle
ARG JAR_FILE=./target/enrollment-1.0-SNAPSHOT.jar
COPY ${JAR_FILE} application.jar

ENTRYPOINT ["java", "-jar", "application.jar"]

