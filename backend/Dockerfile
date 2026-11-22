FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Keep the JVM inside the 512 MB limit of free hosting plans
ENV JAVA_TOOL_OPTIONS="-Xmx320m -XX:+UseSerialGC -Xss512k"
ENTRYPOINT ["java","-jar","app.jar"]
