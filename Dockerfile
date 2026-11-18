# Etapa 1: Compilar con Maven
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Ejecutar con Tomcat (porque es un WAR)
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/thymeleaf-0.0.1-SNAPSHOT.war app.war
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.war"]