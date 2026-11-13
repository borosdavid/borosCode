FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY inventory inventory
COPY imaging imaging
RUN mvn -B -ntp package
FROM eclipse-temurin:21-jre
ARG MODULE
COPY --from=build /src/${MODULE}/target/${MODULE}-1.0.0.jar /app.jar
ENTRYPOINT ["java","-Xmx256m","-Djava.awt.headless=true","-jar","/app.jar"]
